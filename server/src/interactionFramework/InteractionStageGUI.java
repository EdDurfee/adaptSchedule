package interactionFramework;

import interval.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.StandardCopyOption.*;
import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import stp.TemporalDifference;
import stp.Timepoint;
import util.MultiNode;
import util.Node;
import util.XMLParser;
import dtp.*;

import util.AppServer;

/**
 * 
 *   This class provides an interaction framework that allows client-side users to step through an
 *   xml-defined schedule. It acts as the interface between back-end (server) code and front end (client)
 *   systems. It is capable of loading up an xml-defined schedule, interacting with the schedule, modifying
 *   the schedule, and sending/receiving information from the client.
 *
 */
public class InteractionStageGUI implements Runnable {
	
	// Define constants
	private static boolean PREEMPTIVE = false;
	private static boolean CONCURRENT = false;
	private static boolean SPORADIC = false;
	private static boolean SPORADIC_OCCURRED = false;
	private static int SP_DUR = 1;
	private static int MAX_LET = 1440; // End of the day's schedule
	private static String problemFile = "";
	private static final Interval zeroInterval = new Interval(0,0);
	private static DisjunctiveTemporalProblem dtp;
	private static DisjunctiveTemporalProblem prevDTP = null;
	private static DisjunctiveTemporalProblem initialDTP;
	private static List<Integer> systemTime; //systemTime[i] is the current system time for agent i
	private static List<Integer> prevSystemTime;
	private static String last_activity = "";
	private static int numAgents;
	private static int currentAgent;
	private static Scanner cin = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
	private static final int DEBUG = -1;
	private static ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> > previousDTPs;
	private static int revision = 0;
	private static int skip_time = -1;
	
	// ongoingActs is used to keep track of activities currently being performed and their intervals
	// This will be necessary for implementing concurrent activities ability of system
	private static ArrayList<SimpleEntry<String, Interval>> ongoingActs = 
			  											new ArrayList<SimpleEntry<String,Interval>>();
	
	private HashSet<String> confirmedActSet = new HashSet<String>();
	private HashSet<String> currentActSet = new HashSet<String>();
	
	public LinkedBlockingQueue<JSONObject> toClientAgent0Queue    = new LinkedBlockingQueue<JSONObject>();
	public LinkedBlockingQueue<JSONObject> toClientAgent1Queue    = new LinkedBlockingQueue<JSONObject>();
	public LinkedBlockingQueue<JSONObject> fromClientQueue  = new LinkedBlockingQueue<JSONObject>();
	
	// List of simpleEntry that contains <actName, actEndTime>
	// Holds all of the activities currently being performed by each agent
	public ArrayList<SimpleEntry<String, Integer>> agent0CurrentConfirmedActs;
	public ArrayList<SimpleEntry<String, Integer>> agent1CurrentConfirmedActs;
	
	public boolean processingStartup = false;
	public boolean processingTentativeAct = false;
	public boolean processingConfirmedAct = false;
	public boolean processingModify = false;
	public boolean processingAdd = false;
	public boolean processingAdvSysTime = false;
	public boolean readyForNextReq = true;
	String agentNum = "";
	String actName = "";
	String actDur = "";
	JSONObject inJSON;
	
	
	/* 
	 *   This function is able to be called as a new thread, which enables the server to handle
	 * separate threads to monitor for communication from clients
	 *   The function contains the main logic for handling the interaction between many clients and a single
	 * server backend managing a single xml-defined schedule.
	 */
	public void run(){
		
		// wait a second for the http server to get running first (not essential but the output looks better)
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			System.err.println("Failed to wait");
		}
		
		System.out.println("new server instanced");

		// Allow a user on the server console to select which problem file they want to demo
		// These are 9 files of interest at the time of writing. Additional files can be accessed by
		//  changing the xml file names inside of the switch( problemNum ) statement
		System.out.println("\nEnter number of problem file you would like to use.");
		System.out.println("0 - twoAgentProblem.xml");
		System.out.println("1 - multiagentSampleProblem_simp.xml");
		System.out.println("2 - multiagentSampleProblem.xml");
		System.out.println("3 - toyexample.xml");
		System.out.println("4 - drew_test.xml");
		System.out.println("5 - DUTPtoyexampleNoSE.xml");
		System.out.println("6 - toyExampleEd.xml");
		System.out.println("7 - parentSampleProblem.xml");
		System.out.println("8 - singleAgentProb.xml");
		
		Integer problemNum = Integer.valueOf(cin.next());
		
		switch ( problemNum ) {
		case 0: problemFile = "twoAgentProblem.xml"; break;
		case 1: problemFile = "multiagentSampleProblem_simp.xml"; break;
		case 2: problemFile = "multiagentSampleProblem.xml"; break;
		case 3: problemFile = "toyexample.xml"; break;
		case 4: problemFile = "drew_test.xml"; break;
		case 5: problemFile = "DUTPtoyexampleNoSE.xml"; break;
		case 6: problemFile = "toyExampleEd.xml"; break;
		case 7: problemFile = "parentSampleProblem.xml"; break;
		case 8: problemFile = "singleAgentProb.xml"; break;
		default:
			System.out.println("Illegal problem number entered. Restart server to try again.");
		}
		
		// load in the selected file and generate the initial DTP from it
		File tmpDir = new File(problemFile);
		if (!tmpDir.exists()) {
			System.out.println("Failed to load file. Double check that file is in project directory.");
			return;
		}
		
		// Define schedule-specific variables here
		
		// this is a vector of vectors that contains that sequence of confirmed activities and their details
		ArrayList< HashMap< String, String >> actHistory = new ArrayList<HashMap<String,String>>();
		
		// this is queue where server can place requests that it wants to process later
		LinkedBlockingQueue<JSONObject> internalRequestQueue    = new LinkedBlockingQueue<JSONObject>();

		// this is where JSON requests from clients will be put
		JSONObject jsonIN = new JSONObject();

		// The variables below, and a few other places in this codebase are hardcoded to work with
		// either 1 or 2 agents. Changes will be required to use additional agents.
		
		// list of activities confirmed by each individual agent
		agent0CurrentConfirmedActs = new ArrayList<SimpleEntry<String, Integer>>(0);
		agent1CurrentConfirmedActs = new ArrayList<SimpleEntry<String, Integer>>(0);


		// copy master XML into tempModifiedXML.xml that can be safely used and modified by this
		//  instance of the server without altering master file
		try {
			File originalXML = new File(problemFile);
			String newXMLFileName = "tempModifiedXML.xml";
			File modifiedXML = new File(newXMLFileName);
			modifiedXML.delete();
			Files.copy(originalXML.toPath(), modifiedXML.toPath());
		} catch (IOException e) {
			System.err.println("Error in copying of XML to temp file.\n"+e.toString()+"\n");
			System.err.flush();
		}
		
		
		// load in DTP
		dtp = new ProblemLoader().loadDTPFromFile(problemFile);
		System.out.println(problemFile + " loaded succesfully.\n\n");

		// initialize variables and the DTP
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);	
		dtp.simplifyMinNetIntervals();
		numAgents = dtp.getNumAgents();
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		previousDTPs = new ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> >(numAgents);
		for(int i = 0; i < numAgents; i++) previousDTPs.add(
				                           new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>() );
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		
		// if > 1 agents, put the system into 'clock-mode' which allows it to interact with all 
		//  agent schedules, otherwise keep it at currentAgent = 0
		currentAgent = 0;
		if(numAgents > 1){
			currentAgent = numAgents;
		}
		dtp.setCurrentAgent(currentAgent);
		
		// MAX_LET is the largest of the latest end times (LET) of all activities
		MAX_LET = (int) Math.ceil(getMaxLatestEndTime(dtp));
		// TODO: Manually set MAX_LET to end of day (24 hours * 60 mins) to avoid bug
		MAX_LET = 1440;
		
		initialDTP = dtp.clone();
		
		
		
		/*
		 * This is the main loop that handles requests from clients and interacts with the schedule
		 *  accordingly. It is capable of handeling requests from multiple (2 max for now) clients.
		 * Every cycle through this loop represents processing of 1 JSON request from a client
		 * Request is 'received' part way through the loop
		 * 		Code above receiving new request is focused on processing last request and updating the DTPs.
		 * 		Code below receiving new request is focused on parsing the request and deciding how to react
		 *       to it in the above code
		 */
		while(true){
			try{

				// System is currently capable of handling concurrent activities
				CONCURRENT = false;
				
				// Optional debug output
				if(DEBUG >= 2) {
					dtp.printSelectionStack();
				} if(DEBUG >= 1){
					Generics.printDTP(dtp);
					System.out.println("Calls to solver" +dtp.getCallsToSolver());
				} if (DEBUG == 0){ // Print Deltas mode
					if(prevDTP == null) Generics.printDTP(dtp);
					else dtp.printDeltas(dtp.getDeltas(prevDTP));
				}

				
				// gets universal minTime for all agents
				// minTime is the time of next decision point, AKA earliest EST of remaining activities
				int minTime = dtp.getMinTime(); 
				
				// TODO: do not allow a minTime greater than MAX_LET (represents end of the day is reached)
				minTime = Math.min(minTime, MAX_LET);

				// if you have moved past the latest LET, exit the loop and end the server
				if(getSystemTime() >= MAX_LET){
					return;
				}
				
				// if the next decision point is in the past, there has been a mistake in managing time of
				//  system and DTPs
				else if(minTime < getSystemTime()){
					System.err.println("Error: System time out of sync, expect a failure.");	
				}
				
				// get list of current activity choices for all agents (first dimension is agent number)
				List<List<String>> activities = dtp.getActivities(
										DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime());
				
				// get list of all non-completed activities for all agents (first dimension is agent number)
				List<List<String>> allActNames = dtp.getActivities(
										DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime());
				
				// get list of max slack (max idle time) for each agent at its corresponding index
				List<Integer> maxSlack = dtp.getMaxSlack();
				
				// remove completed activities from ongoing activities
				ArrayList<Integer> toRemove = new ArrayList<Integer>();
				for(int i = 0; i < ongoingActs.size(); i++){
					SimpleEntry<String, Interval> pair = ongoingActs.get(i);
					Interval curr = pair.getValue();
					int time = getSystemTime();
					
					// if the system time is >= to the end time of this activity, it is no longer ongoing
					if(time >= curr.getUpperBound()) {
						toRemove.add(i);
					}
					// if the index goes beyond list size, there is a bug with the concurrency
					if (i > ongoingActs.size()) {
						System.err.println("Error: Concurrent loop error in ongoing acts update");
					}
				}
				
				// need to reverse the order of toRemove so that highest indices are removed first, 
				//  thereby preserving the location of lower later-removed indices
				Collections.reverse(toRemove);
				
				// remove completed activities
				for(int j : toRemove) {
					// if the index goes beyond list size, there is a bug with the concurrency
					if (ongoingActs.size() <= j) {
						System.err.println("Error: Concurrent loop error in ongoing acts update");
					}
					ongoingActs.remove(j);
				}
				
				// decide whether to add 'idle' as an activity option for each agent
				// agent can only idle if it has available slack and needs to currently make a selection
				// if only 1 agent and conditions are met
				if(maxSlack.size() == 1 && maxSlack.get(0) > 0 && getSystemTime() == dtp.getMinTime()) {
					activities.get(0).add("idle");
				// else need to check conditions for each  individual agent
				} else for(int agent = 0; agent < maxSlack.size(); agent++){
					if ( maxSlack.get(agent) > 0 && getSystemTime(agent) == getSystemTime()) {
						if (agent == 0 && agent0CurrentConfirmedActs.size() == 0)
							activities.get(agent).add("idle");
						if (agent == 1 && agent1CurrentConfirmedActs.size() == 0)
							activities.get(agent).add("idle");
					}
				}
				
				// create 2d vector of min and max durations of activities that can be performed next
				// first dim is agent num, second dim is act idx corresponding to activities array
				
				// initialize both lists with an empty list for each agent
				ArrayList<ArrayList<String>> nextActsMinDur = new ArrayList<ArrayList<String>>();
				for (int i=0;i<numAgents;i++) nextActsMinDur.add(new ArrayList<String>());
				ArrayList<ArrayList<String>> nextActsMaxDur = new ArrayList<ArrayList<String>>();
				for (int i=0;i<numAgents;i++) nextActsMaxDur.add(new ArrayList<String>());
				
				// populate min/max duration vectors
				for (int agent = 0; agent < numAgents; agent++) {
					for (int act = 0; act < activities.get(agent).size(); act++) {
						// if agent can idle. If so, update min/max duration lists
						if (activities.get(agent).get(act).equals("idle")) {
							nextActsMinDur.get(agent).add("5"); // min idle time is no less than 5 minutes
							nextActsMaxDur.get(agent).add(String.valueOf(maxSlack.get(agent)));
							continue;
						}

						// append to min/max duration lists
						IntervalSet interval = dtp.getInterval(activities.get(agent).get(act)+"_S",
								     activities.get(agent).get(act)+"_E").inverse().subtract(zeroInterval);
						nextActsMinDur.get(agent).add(String.valueOf( (int) interval.getLowerBound()));
						nextActsMaxDur.get(agent).add(String.valueOf( (int) interval.getUpperBound()));

						// if this activity has zero max duration, force it to be performed immediately
						if (nextActsMaxDur.get(agent).get(nextActsMaxDur.get(agent).size() - 1) == "0") {
							int dur = 0;
							dtp.executeAndAdvance(-getSystemTime(), activities.get(agent).get(act)+"_S",
									-getSystemTime(), activities.get(agent).get(act)+"_E",true, dur, true);
							dtp.simplifyMinNetIntervals();
						}
						
					}
				}		
				
				// create 2d vector of min and max durations of all remaining activities
				// allActNames should never include idle
				// first dim is agent num, second dim is act idx corresponding to allActNames array
				
				// initialize both lists with an empty list for each agent
				ArrayList<ArrayList<String>> actMinDurs = new ArrayList<ArrayList<String>>();
				for (int i=0;i<numAgents;i++) actMinDurs.add(new ArrayList<String>());
				ArrayList<ArrayList<String>> actMaxDurs = new ArrayList<ArrayList<String>>();
				for (int i=0;i<numAgents;i++) actMaxDurs.add(new ArrayList<String>());
				
				// populate min/max duration vectors
				for (int agent = 0; agent < numAgents; agent++) {
					for (int act = 0; act < allActNames.get(agent).size(); act++) {
						// append to min/max duration lists
						IntervalSet interval = dtp.getInterval(allActNames.get(agent).get(act)+"_S",
								    allActNames.get(agent).get(act)+"_E").inverse().subtract(zeroInterval);
						actMinDurs.get(agent).add(String.valueOf( (int) interval.getLowerBound()));
						actMaxDurs.get(agent).add(String.valueOf( (int) interval.getUpperBound()));
					}
				}
				
				
				// depending on the request type of last request, communicate system changes to clients
				if (processingConfirmedAct || processingTentativeAct || processingModify || processingAdd
															|| processingAdvSysTime || processingStartup) {
					
					// if not a tentative activity selection, send new activity options to each client
					if (!processingTentativeAct) {
						sendCurrentChoicesToAllClients(activities, nextActsMinDur, nextActsMaxDur,
								                                                              allActNames);
						processingModify = false;
						processingAdd = false;
						processingAdvSysTime = false;
					}
					
					// if processing a confirmed action, need to update gantts of all clients to reflect
					//  permanent inter-dependent changes
					if (processingConfirmedAct) {
						sendGanttToAgent("ALL", allActNames);
						processingConfirmedAct = false;
					}
					
					// if tentative action or startup, only send new gantt to agent of request
					else if (processingTentativeAct || processingStartup) {
						sendGanttToAgent(agentNum, allActNames);
						processingStartup = false;
					}
					
					// if this is a tentative activity, undo the activity (because it was artificially
					//  confirmed inside the system to generate temporary new info)
					if (processingTentativeAct == true) {
						removeTentAct(agentNum);
						
						// need to manually remove undone act from list of ongoing acts
						for (int i = 0; i < ongoingActs.size(); i++) {
							if (ongoingActs.get(i).getKey() == actName) {
								ongoingActs.remove(i);
								break;
							}
						}
						processingTentativeAct = false;
						
						// need to go back to top to get a full undo / removal of tentative activity
						continue;
					}
				}
				
				
				/* 
				 * At this point, whatever request was previously being handled has 100% finished processing
				 * and can be replaced by the next request
				 */
			
				
				
				// if not currently recovering from a reset, process requests received from clients
				if (internalRequestQueue.size() == 0) {
					// wait for the client to send a POST request
					inJSON = getNextRequest();
				}
				// If system is currently recovering from a reset (which may be brought on by adding,
				//  removing, or modifying an activity), pull activities out of internalRequestQueue. This
				//  queue will be populated with activities that were previously completed on the system
				//  before the reset and need to be re-processed.
				else {
					inJSON = internalRequestQueue.poll();
				} 
				
				
				/* 
				 * At this point, a new request has been recieved (agnostic of its source) and can be
				 * processed without consideration for other (previous or coming) requests
				 */
				
				
				// Parse variables from the request JSON
				agentNum = String.valueOf(inJSON.get("agentNum"));
				actName = String.valueOf(inJSON.get("activityName"));
				actDur = String.valueOf(inJSON.get("activityDuration"));
				
				// process the request depending on the type of request received (AKA the infoType)
				switch ( (String) inJSON.get("infoType")) {

					// A client has just been activated and connected to the server
					case "startup":
						processingStartup = true;
						break;
						
					// A user has selected or confirmed an activity
					case "confirmActivity":
					case "tentativeActivity":
						
						// if it is a confirmed activity or a tentative activity the system needs to treat
						//  it the same and internally 'perform' the activity
						
						// save current state of DTP to enable undoing later
						prevDTP = dtp.clone();
						previousDTPs.get(Integer.valueOf(agentNum)).push(new SimpleEntry<Integer,
								                    DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
						
						// if activity requested is an idle, adjust the clock of the dtp and add idle to the
						//  current confirmed activities if necessary
						if (actName.equals("idle")) {
							getAndPerformIdle(0, maxSlack.get(Integer.valueOf(agentNum)), 
								   Integer.valueOf(actDur.substring(3)), Integer.valueOf(agentNum), minTime);
							
							int idleTime = Integer.valueOf(actDur.substring(3));

							// if this is a confirmed activity, add it to the respective agents list of all
							//  confirmed activities
							if ( ((String) inJSON.get("infoType")).equals("confirmActivity") ) {
								if (agentNum.equals("0")) agent0CurrentConfirmedActs.add(
										       		  new SimpleEntry("idle", getSystemTime() + idleTime));
								if (agentNum.equals("1")) agent1CurrentConfirmedActs.add(
													  new SimpleEntry("idle", getSystemTime() + idleTime));
							}
						}
						
						// if activity is not an idle, process it internal to the corresponding DTP
						else {
							
							// set endTime as the EET
							IntervalSet endTime = dtp.getInterval("zero", actName+"_E");
							
							// activity duration in minutes
							int time = Generics.fromTimeFormat( actDur );
							
							// check if given duration is valid (within max duration constraints)
							IntervalSet interval = dtp.getInterval(actName+"_S",
									                        actName+"_E").inverse().subtract(zeroInterval);
							if(!interval.intersect(time)){
								System.err.println("Unexpected activity duration for \""+actName+"\"");
								break;
							}
							
							// if the given duration + systemTime does not reach past EET, the system needs
							//  to force an idle until it reaches EET
							int idle = (int) (-endTime.getUpperBound() - getSystemTime() - time);
							if(idle > 0){
								if (DEBUG >= 1) {
									System.out.println("Forcing immediate idle for " + String.valueOf(idle));
								}
							} else {
								idle = 0;
							}
							
							// interval from current time to end of selected duration
							Interval curr_int = new Interval(getSystemTime() + idle,
									                                        getSystemTime() + idle + time);
							ongoingActs.add(new SimpleEntry<String,Interval>(actName, curr_int));
							
							// if a confirmed activity, add the activity to the corresponding list
							if ( ((String) inJSON.get("infoType")).equals("confirmActivity") ) {
								if (agentNum.equals("0")) agent0CurrentConfirmedActs.add(
												  new SimpleEntry(actName, getSystemTime() + idle + time));
								if (agentNum.equals("1")) agent1CurrentConfirmedActs.add(
												  new SimpleEntry(actName, getSystemTime() + idle + time));
							}
							
							// internal to the corresponding dtp, perform the activity + adjust the clock
							dtp.executeAndAdvance(-( getSystemTime() + idle), actName+"_S",
										   -(getSystemTime() + idle + time),actName+"_E",true, time, true);
							dtp.simplifyMinNetIntervals();
						}
						
						// if a confirmed activity, add this act info to the history list
						if ( ((String) inJSON.get("infoType")).equals("confirmActivity") ) {
							
							HashMap<String,String> thisAct = new HashMap<String,String>();
							thisAct.put("agentNum",          agentNum);
							thisAct.put("activityName",      actName);
							thisAct.put("activityDuration",  actDur);
							
							actHistory.add(thisAct);
						}
						
						// Set request type flag for processing at the top of the loop
						if ( ((String) inJSON.get("infoType")).equals("tentativeActivity")) {
							processingTentativeAct = true;
						}
						if ( ((String) inJSON.get("infoType")).equals("confirmActivity"))   {
							processingConfirmedAct = true;
						}
						
						break;
						
					// a user has selected an activity in the modify activity screen
					case "requestActDetails":
						
						JSONObject actDetails = new JSONObject();
						
						// get the list of activities that are viable to have their duration tightened and availability tightened
						ArrayList<String> modifiableDurActs   = (ArrayList<String>) Generics.concat(
										dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARDUR,
										-getSystemTime()));
						ArrayList<String> modifiableAvailActs = (ArrayList<String>) Generics.concat(
								      dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARAVAIL,
								      -getSystemTime()));
						
						ArrayList<String> minDurs = new ArrayList<String>();
						ArrayList<String> maxDurs = new ArrayList<String>();
						String EST = "";
						String LST = "";
						String EET = "";
						String LET = "";
						
						// actname will be blank when client has not yet selected specefic activity to mod
						// if not blank, get information on the activity (availability & duration)
						if (!actName.equals("")) {
							// this interval will be [minDur,maxDur]
							IntervalSet currentDuration = dtp.getInterval(actName+"_S", actName+"_E")
																								.inverse();
							// this interval will be [EST,LST]
							IntervalSet startAvail  = dtp.getInterval("zero", actName+"_S").inverse();
							// this interval will be [EET,LET]
							IntervalSet endAvail    = dtp.getInterval("zero", actName+"_E").inverse();
							
							minDurs.add( String.valueOf( currentDuration.getLowerBound() ) );
							maxDurs.add( String.valueOf( currentDuration.getUpperBound() ) );
							EST = String.valueOf( startAvail.getLowerBound() );
							LST = String.valueOf( startAvail.getUpperBound() );
							EET = String.valueOf( endAvail.getLowerBound()   );
							LET = String.valueOf( endAvail.getUpperBound()   );
						}

						// send the lists of modifiable activities
						//  along with details of the specific activity (if specified)
						actDetails.put("actName", actName);
						actDetails.put("modifiableDurActs", modifiableDurActs);
						actDetails.put("modifiableAvailActs", modifiableAvailActs);
						actDetails.put("minDurs", minDurs);
						actDetails.put("maxDurs", maxDurs);
						actDetails.put("EST", EST);
						actDetails.put("LST", LST);
						actDetails.put("EET", EET);
						actDetails.put("LET", LET);
						
						sendJSONToClient(agentNum,
								"activityDetails", // infoType
								"", // startTime
								"", // lastActivity
								"", // clearToConfirm
								new ArrayList<String>(), // nextActivities
								new ArrayList<String>(), // nextActsMinDur
								new ArrayList<String>(), // nextActsMaxDur
								new ArrayList<String>(), // actNames
								new ArrayList<String>(), // actIDs
								new ArrayList<String>(), // actMinDurs
								new ArrayList<String>(), // actMaxDurs
								new ArrayList<String>(), // actESTs
								new ArrayList<String>(), // actLETs
								new ArrayList<String>(), // actRestricts
								"",						 // otherWeakRestrictCount
								"",						 // otherStrongRestrictCount
								"",						 // currentTime
								"",						 // imgStr
								actDetails,				 // activity details
								new ArrayList<String>()  // debugInfo
						);
						
						break;
						
					// user has entered new (tighter) duration / availability for an activity
					case "modifyActivity":
						
						prevDTP = dtp.clone();
						SimpleEntry<Integer, DisjunctiveTemporalProblem> tempSE_mod =
							 new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP);
						Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> tempStack_mod =
									          new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>();
						tempStack_mod.push(tempSE_mod);
						previousDTPs.add( tempStack_mod );
						
						JSONObject actJSON_mod = (JSONObject) inJSON.get("actDetails");
						
						// all times below are in format:  hh:mm
						List<String> newMinDurs_mod  = (ArrayList<String>) actJSON_mod.get("minDurs");
						List<String> newMaxDurs_mod  = (ArrayList<String>) actJSON_mod.get("maxDurs");
						String newEST_mod 			 = String.valueOf(actJSON_mod.get("EST"));
						String newLST_mod 			 = String.valueOf(actJSON_mod.get("LST"));
						String newEET_mod 			 = String.valueOf(actJSON_mod.get("EET"));
						String newLET_mod 			 = String.valueOf(actJSON_mod.get("LET"));
						
						// Duration
						AddIntervalSet(actName+"_S", actName+"_E", getSystemTime(),Generics.stringToInterval(
								         "[" + newMinDurs_mod.get(0) + "," + newMaxDurs_mod.get(0) + "]" ) );
						
						// if no value submitted for these, use default max value
						if (newEST_mod.equals("")) {newEST_mod = "00:00";}
						if (newLST_mod.equals("")) {newLST_mod = "24:00";}
						if (newEET_mod.equals("")) {newEET_mod = "00:00";}
						if (newLET_mod.equals("")) {newLET_mod = "24:00";}
						
						// EST and LST
						AddIntervalSet("zero", actName+"_S", getSystemTime(), Generics.stringToInterval(
															 "[" + newEST_mod + "," + newLST_mod + "]" ) );
						// EET and LET
						AddIntervalSet("zero", actName+"_E", getSystemTime(), Generics.stringToInterval(
															 "[" + newEET_mod + "," + newLET_mod + "]" ) );
						
						processingModify = true;
						break;
					
						
						// client will send this when the user has entered a new activity
						case "addActivity":
							
							// Save current dtp in case new addition breaks system
							prevDTP = dtp.clone();
							SimpleEntry<Integer, DisjunctiveTemporalProblem> tempSE_add = new 
								SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP);
							Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> tempStack_add = new
									             Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>();
							tempStack_add.push(tempSE_add);
							previousDTPs.add( tempStack_add );
							
							JSONObject actJSON_add = (JSONObject) inJSON.get("actDetails");
							
							String name    = (String) actJSON_add.get("actName");

							// times below have format:  mmmm
							String est     = (String) actJSON_add.get("EST");
							String lst     = (String) actJSON_add.get("LST");
                            String eet     = (String) actJSON_add.get("EET");
                            String let     = (String) actJSON_add.get("LET");
                            
                            // if no value submitted for these, use default max value
    						if (est.equals("")) {est = "0000";}
    						if (lst.equals("")) {lst = "1440";}
    						if (eet.equals("")) {eet = "0000";}
    						if (let.equals("")) {let = "1440";}
                            
                            // TODO: replace hardcode solution to sub-dtp selection problem
    						// This will on work with the current system split of morning / evening 
                            String dtpIdx = "";
                            if (Integer.parseInt(est) < 720) {dtpIdx = "0";} 
                            else {dtpIdx = "1";}
                            
                            // durations come as lists to handle multi ranges in future improvements
                            String minDur  = ((ArrayList<String>) actJSON_add.get("minDurs")).get(0);
                            String maxDur  = ((ArrayList<String>) actJSON_add.get("maxDurs")).get(0);
                            
                            // Assume all constraints are ordering constraints
                            // Sources are preceeding activities and dest are succeeding activities
                            ArrayList<String> precConstraints = (ArrayList<String>) actJSON_add.get("constraintSource");
                            ArrayList<String> succConstraints = (ArrayList<String>) actJSON_add.get("constraintDest");
							
							Boolean addSuccess = addActToXML(Integer.parseInt(agentNum), dtpIdx, name, est, lst, eet, let, minDur, maxDur, precConstraints, succConstraints);
							
							// if the add succeeded, set up history to automatically repeat
							// else if add failed, the dtp will be unmodified so no need to reexecute history
							if (addSuccess == true) {
								processingAdd = true;
								
								// populate internalRequestQueue with all activities that have already been
								// completed, the items in the internalRequestQueue will then be processed
								// by the main loop later
								for (HashMap<String,String> h : actHistory) {
									JSONObject temp = createInternalJSON(h);
									internalRequestQueue.add(temp);
								}
								
								// reset the history so it can be populated naturally again
								actHistory.clear();
							}
							
							// automatically add request to advance the system time and update client display
							// regardless of success of addition
							JSONObject tempAddJSON = new JSONObject();
							tempAddJSON.put("infoType", "advSysTime");
							internalRequestQueue.add(tempAddJSON);
							
							break;
						
					// user has requested to delete an activity that has not yet been performed
					case "deleteActivity":
						
						// populate internalRequestQueue with all activities that have already been
						// completed, the items in the internalRequestQueue will then be processed
						// by the main loop later
						for (HashMap<String,String> h : actHistory) {
							JSONObject temp = createInternalJSON(h);
							internalRequestQueue.add(temp);
						}

						// reset the history so it can be populated naturally again
						actHistory.clear();
						
						// automatically add request to advance the system time and update client display
						JSONObject tempDelJSON = new JSONObject();
						tempDelJSON.put("infoType", "advSysTime");
						internalRequestQueue.add(tempDelJSON);
						
						
						// remove the selected activity from the system
						deleteActFromXML(actName);
						
						break;
					
					// user has requested for system clocks to advance to next decision point
					// this is a temporary system to enable demo without literally waiting for time to pass
					case "advSysTime":
						
						// advance and update the dtp
						int temp = minTime - getSystemTime();
						advanceSystemTimeTo(minTime);
						dtp.advanceToTime(-getSystemTime(), temp, false);
						dtp.simplifyMinNetIntervals();
						
						// check each current activity being performed for each agent. If the activity ends
						//  at or before the new currentTime (minTime), remove it from list
						for (int i = 0; i < agent0CurrentConfirmedActs.size(); i++) {
							if ( agent0CurrentConfirmedActs.get(i).getValue() <= minTime ) {
								agent0CurrentConfirmedActs.remove(i);
							}
						}
						for (int i = 0; i < agent1CurrentConfirmedActs.size(); i++) {
							if ( agent1CurrentConfirmedActs.get(i).getValue() <= minTime ) {
								agent1CurrentConfirmedActs.remove(i);
							}
						}
						
						// after advancing the clock, send updated gantt chart to all clients
						sendGanttToAgent("ALL", allActNames);
						
						processingAdvSysTime = true;
						
						break;
						
					default:
						System.out.println( "Illegal infoType found: " + (String) inJSON.get("infoType") );
						break;
					
				}
				
			} catch (Exception e) {
				System.err.println("Error in request processing.  Please try again.\n"+e.toString()+"\n"
																	  +Arrays.toString(e.getStackTrace()));
				System.err.flush();
			}
		}
	}
	
	
	// Removes the last 'performed' activity from the dtp
	// This should be called after every tentative action gantt sent to the client to revert dtp
	//  back to only confirmed acts
	private static void removeTentAct(String agentNumber) {
		if(previousDTPs.get( Integer.valueOf(agentNumber)).empty() ) {
			System.out.println("The schedule has not yet been modified.\n");
			return;
		}
		System.out.println("removing tentative selection.");
		
		// retrieve previous saved dtp
		SimpleEntry<Integer, DisjunctiveTemporalProblem> ent = previousDTPs.get( Integer.valueOf(agentNumber)).pop();
		
		// set system time and system dtp beack to previous dtp values
		setSystemTime(ent.getKey());
		System.out.println("Resetting time to "+Generics.toTimeFormat(getSystemTime()));
		dtp = ent.getValue();
	
	}
	
	
//	private static boolean disjunctivelyUncertainProblem(DisjunctiveTemporalProblem dtp) {
//		if ((dtp instanceof DUTP) || (dtp instanceof DUSTP)) {
//			return true;
//		}
//		else {
//			return false;
//		}
//	}
	
	private static double getMaxLatestEndTime(DisjunctiveTemporalProblem dtp) {
		double let = MAX_LET;
		ArrayList<Interval> minmaxArray = dtp.getDTPBoundaries();
		for (Interval interv : minmaxArray) {
			let = Math.min(let, interv.getUpperBound());
		}
		return let;
	}

	
	public static boolean requiredActivity(String actname) {
		return (!(actname.equals("skip")) || (actname.equals("idle")));
	}

//	private static int getEarliestEnd(
//			ArrayList<SimpleEntry<String, Interval>> ongoing) {
//		int min = Integer.MAX_VALUE;
//		for(SimpleEntry<String,Interval> pair: ongoing){
//			int end = (int) pair.getValue().getUpperBound();
//			if(end < min) min = end;
//		}
//		//System.out.println("Would skip to time " + Generics.toTimeFormat(min));
//		return min;
//	}


	private static void getAndPerformIdle(int minIdle, int maxIdle, int time, int subDTPNum, int minTime){
		if(time < 0 || time > maxIdle+1){
			System.out.println("Unexpected idle time response \""+Integer.toString(time)+"\"");
			return;
		}
		
		dtp.advanceSubDTPToTime(-(minTime+time), time, true, subDTPNum);
		
		
		dtp.simplifyMinNetIntervals();
	}

/*
 * This function was implemented for the text-based UI but has not yet been incorporated into the GUI system
	private static void getAndPerformEmergencyIdle(String str){
		printToClient("How long to idle? Format for input is H:MM");
		JSONObject jsonIN = getNextRequest();
		String temp = (String) jsonIN.get("value");
		int time = Generics.fromTimeFormat(temp);
		if(time < 0){
			printToClient("Unexpected response \""+str+"\"");
			return;
		}
		List<String> uftps = dtp.getUnFixedTimepoints();
		printToClient( uftps.toString() );
		printToClient("incrementing system time by " + time);
		incrementSystemTime(time, currentAgent);
		printToClient("advancing to time");
		dtp.advanceToTime(-getSystemTime(), time, true);
		printToClient("simplifying min net intervals");
		dtp.simplifyMinNetIntervals();
		if (dtp.getMinTime() > 24*60){
			uftps = dtp.getUnFixedTimepoints();
			//for(Timepoint tp : uftps) System.out.println(tp.getName());
		}
	}
*/
	
	/*
	 * The following set of get-/set-/increment- SystemTime function should be used to interface the system clock rather than directly accessing things 
	 */	
	public static int getSystemTime(){
		return getSystemTime(currentAgent);
	}
	
	static int getSystemTime(int agent){
		if(agent == numAgents){
			return Collections.min(systemTime);
		}
		return systemTime.get(agent);
	}
	
	private static void incrementSystemTime(int val){
		setSystemTime(val+getSystemTime());
	}
	
	static void incrementSystemTime(int val, int agent){
		setSystemTime(val+getSystemTime(agent), agent);
	}
	
	private static void setSystemTime(int val){
		setSystemTime(val, currentAgent);
	}
	
	private static void setSystemTime(int val, int agent){
		if(agent == numAgents){
			for(int i = 0; i < systemTime.size(); i++){
				systemTime.set(i, val);
			}
		}
		else
			systemTime.set(agent, val);
	}
	
	static void advanceSystemTimeTo(int val){
		advanceSystemTimeTo(val,currentAgent);
	}
	
	private static void advanceSystemTimeTo(int val, int agent){
		if(agent == numAgents){
			for(int i = 0; i < systemTime.size(); i++){
				systemTime.set(i, Math.max(val,getSystemTime(i)));
			}
		}
		else
			systemTime.set(agent, val);
	}
	
	
	/**
	 * oldName: getIntervalSetAndAdd()
	 * ui helper code to prompt a user to input an intervalset
	 * also generates a disjunctivetemporalconstraint from that intervalset and adds it to the dtp
	 * @param tpS
	 * @param tpE
	 * @param time
	 * @param msg
	 */
	private static void AddIntervalSet(String tpS, String tpE, int time, IntervalSet newIS){
//		printToClient(msg+"\nEnter new interval set or (n) to leave unchanged. Format {[interval1]v[interval2]v...} with no whitespace.");
//		JSONObject jsonIN = getNextRequest();
//		String avail = (String) jsonIN.get("value");
//		String avail = cin.next(); // Commented out by Drew
//		if(!avail.equalsIgnoreCase("N")){
//			IntervalSet is = Generics.stringToInterval(avail);
		
		dtp.addAdditionalConstraint(tpS, tpE, newIS, time, true, true);	
		dtp.simplifyMinNetIntervals();
	}

	
	/**
	 * ui helper code to prompt a user to select a timepoint from a
	 * @param a
	 * @param getStartEnd toggles whether to prompt user to specify between start/end timepoint
	 * @return
	 */
/*	private static String getTimepoint(Collection<String> a, boolean getStartEnd){
		JSONObject jsonIN = getNextRequest();
		String tp = (String) jsonIN.get("value");
//		String tp = cin.next(); // Commented out by Drew
		while(!a.contains(tp)){
			printToClient("Unexpected response \""+tp+"\"");
			jsonIN = getNextRequest();
			tp = (String) jsonIN.get("value");
//			tp = cin.next(); // Commented out by Drew
		}
		if(tp.equals("zero")) return tp; 
		boolean flag = getStartEnd;
		if(flag){
			printToClient("From (s)tart or (e)nd time of "+tp+"?");
			jsonIN = getNextRequest();
			String str = (String) jsonIN.get("value");
//			String str = cin.next(); // Commented out by Drew
			while(flag){
				flag = false;
				if(str.equalsIgnoreCase("S")) tp += "_S";
				else if(str.equalsIgnoreCase("E")) tp += "_E";
				else{
					printToClient("Unexpected response \""+str+"\"");
					jsonIN = getNextRequest();
					str = (String) jsonIN.get("value");
//					str = cin.next(); // Commented out by Drew
					flag = true;
				}
			}
		}
		return tp;
	}
*/
	
	/*private static void setAgentSelection(String newAgent) {
		if (Integer.valueOf(newAgent) != currentAgent) {
			currentAgent = Integer.valueOf(newAgent);
		}
		if (currentAgent < 0 || currentAgent > numAgents) {
			System.err.println("ERROR: Invalid agent selection");
			System.exit(1);
		}
		dtp.setCurrentAgent(currentAgent);
	}*/
	
	
	
	//returns a simple entry with the name of the new activity and the subDTP or agentDTP it was added to.
	private static SimpleEntry<String, Integer> addNewActivity(){
		Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime()));
		ArrayList<Interval> seps = dtp.getDTPBoundaries();
		SimpleEntry<Integer, String> output = XMLMaker.makeNewActivityAndConstraintString(activities, seps);
		String out_string = output.getValue();
		int dtp_id = output.getKey();
		String act_name = XMLMaker.activity;
		revision++;
		BufferedReader br = null;
		BufferedWriter out_file = null;
		Path src = FileSystems.getDefault().getPath(problemFile);
		String dest_fname = "rev" + revision + problemFile;
		File f = new File("/home/lynngarrett/research/maDTPRepo/MaDTP/branches/Lynn/trunk/" + dest_fname);
		File f_in = new File(problemFile);
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			out_file = new BufferedWriter(new FileWriter(f));
			br = new BufferedReader(new FileReader(f_in));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		boolean inserted = false;
		String line = null;
		try {
			while((line = br.readLine()) != null){
				// check to see if the line is the ending of an activity tag
				// if it is, we want to insert our new activity here. 
				if(!inserted && line.contains("</activity>")){
					out_file.write(line+ "\n");
					out_file.write(out_string);
					inserted = true;
					
					continue;
				}
				out_file.write(line + "\n");
				
			}
			
		out_file.close();
		br.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//System.out.println(out_string);
		problemFile = dest_fname;
		return new SimpleEntry<String, Integer>(act_name, dtp_id);

	}
	
	/*
	 * Return String of activity names seperated by newlines
	 */
	private static String listAgentActivities(List<List<String>> acts, int agentNum) {
		String outStr = "";
		for (int a = 0; a < acts.get(agentNum).size(); a++) {
			outStr += acts.get(agentNum).get(a) + "\n"; // the name of the activity
		}
		return outStr;
	}


	/*
	 * This function adds the activity into the XML file
	 * It also modifies the global dtp and refreshes it
	 * Return true/false bsased on if add failed or not yices
	 */
	private Boolean addActToXML(int agent, String dtpIdx, String name, String est, String lst,
			                              String eet, String let, String minDur, String maxDur,
			                              ArrayList<String> precConstraints,
			                              ArrayList<String> succConstraints) {
		
		
		File modifiedXML = null;
		String xmlModString = "";
		String prevXMLModString = "";
		try {
//			File originalXML = new File(problemFile);
			String newXMLFileName = "tempModifiedXML.xml";
			modifiedXML = new File(newXMLFileName);
//			modifiedXML.delete();
//			Files.copy(originalXML.toPath(), modifiedXML.toPath());
			
		} catch (Exception e) {
			System.err.println("Error is creating new modified XML file in addActivity.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()) + "\n");
			System.err.flush();
		}
		
		try{
			Scanner scan = new Scanner(modifiedXML);
			xmlModString = scan.useDelimiter("\\Z").next();
			scan.close();
		}
		catch(IOException e){
			System.err.println(e.getMessage()+"\n"+e.getStackTrace().toString());
		}
		
		// save the xml string from before the modification
		prevXMLModString = xmlModString;
		
		// add this activity to the modified xml file string
		xmlModString = XMLParser.addActivity(xmlModString, agent, dtpIdx, name, est, lst, eet, let, minDur,
																 maxDur, precConstraints, succConstraints);
		
		// put new xml string into file
		try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter("tempModifiedXML.xml"));
		    writer.write(xmlModString);
		    writer.close();
		} catch(IOException e){
			System.err.println(e.getMessage()+"\n"+e.getStackTrace().toString());
		}
		
		// save old state of the system, in case new system fails
		DisjunctiveTemporalProblem beforeModDTP = dtp.clone();
		
		
		// load this xml string to a dtp and put it in main dtp variable
		// set up dtp
		dtp = new ProblemLoader().loadDTPFromFile("tempModifiedXML.xml");
		System.out.println("tempModifiedXML.xml" + " loaded succesfully.\n\n"); // file loaded correctly

		// initialize variables and the DTP
		// if these initialization functions throw an exception, assume added activity was illegal and revert back to previous
		try {
			dtp.updateInternalData();
			dtp.enumerateSolutions(0);	
			dtp.simplifyMinNetIntervals();
		} catch (Exception e) {
			
			System.out.println("Attempted to add illegal activity to dtp. Rejecting addition and reverting to previous dtp.");
			
			// revert the dtp to before changes
			dtp = beforeModDTP.clone();
			
			// revert the xml file to before changes
			try {
			    BufferedWriter writer = new BufferedWriter(new FileWriter("tempModifiedXML.xml"));
			    writer.write(prevXMLModString);
			    writer.close();
			} catch(IOException p){
				System.err.println("Failure when reverting XML" + p.getMessage()+"\n"+p.getStackTrace().toString());
			}
			
			return false; // return that this add failed
		}
		
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		previousDTPs = new ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> >(numAgents);
		for(int i = 0; i < numAgents; i++) previousDTPs.add( new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>() );
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		
		dtp.setCurrentAgent(currentAgent);

		// for Add act, need to change original dtp to match new set of activities (before re-performing any acts)
		initialDTP = dtp.clone();

		return true; // return that this add succeeded
		
	}
	
	
	/*
	 * This function deletes the activity from the XML file so that it will be deleted from the user schedule
	 * It also modifies the global dtp and refreshes it
	 */
	private void deleteActFromXML(String delActName) {
		
		File modifiedXML = null;
		String xmlModString = "";
		try {
//			File originalXML = new File(problemFile);
			String newXMLFileName = "tempModifiedXML.xml";
			modifiedXML = new File(newXMLFileName);
//			modifiedXML.delete();
//			Files.copy(originalXML.toPath(), modifiedXML.toPath());
			
		} catch (Exception e) {
			System.err.println("Error is creating new modified XML file in deleteActivity.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()) + "\n");
			System.err.flush();
		}
		
		try{
			Scanner scan = new Scanner(modifiedXML);
			xmlModString = scan.useDelimiter("\\Z").next();
			scan.close();
		}
		catch(IOException e){
			System.err.println(e.getMessage()+"\n"+e.getStackTrace().toString());
		}
		
		// remove this activity from the modified xml file string
		xmlModString = XMLParser.removeSpecificActivity(xmlModString, delActName);
		
		// put new xml string into file
		try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter("tempModifiedXML.xml"));
		    writer.write(xmlModString);
		    writer.close();
		} catch(IOException e){
			System.err.println(e.getMessage()+"\n"+e.getStackTrace().toString());
		}
		
		// save old state of the system, in case new system fails
		DisjunctiveTemporalProblem beforeModDTP = dtp.clone();
		
		
		// load this xml string to a dtp and put it in main dtp variable
		// set up dtp
		dtp = new ProblemLoader().loadDTPFromFile("tempModifiedXML.xml");
		System.out.println("tempModifiedXML.xml" + " loaded succesfully.\n\n"); // file loaded correctly

		// initialize variables and the DTP
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);	
		dtp.simplifyMinNetIntervals();
		
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		previousDTPs = new ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> >(numAgents);
		for(int i = 0; i < numAgents; i++) previousDTPs.add( new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>() );
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		
		dtp.setCurrentAgent(currentAgent);

	}
	
	
	@SuppressWarnings("unchecked")
	private void sendJSONToClient( String toAgentNum,
			String infoType, String startTime, String lastActivity, String clearToConfirm, List<String> nextActivities,
			List<String> nextActsMinDur, List<String> nextActsMaxDur, List<String> actNames, List<String> actIDs,
			List<String> actMinDurs, List<String> actMaxDurs, List<String> actESTs,
			List<String> actLETs, List<String> actRestricts, String otherWeakRestrictCount, String otherStrongRestrictCount,
			String currentTime, String strImg, JSONObject actDetails, List<String> debugInfo) {
		
		System.out.println("- Server sending '"+infoType+"' JSON to client #" + toAgentNum);
//		System.out.println(actDetails);
		
		JSONObject outJSON = new JSONObject();
		outJSON.put("infoType", infoType);
		outJSON.put("startTime", startTime);
		outJSON.put("lastActivity", lastActivity);
		outJSON.put("clearToConfirm", clearToConfirm);
		outJSON.put("nextActivities", nextActivities);
		outJSON.put("nextActsMinDur", nextActsMinDur);
		outJSON.put("nextActsMaxDur", nextActsMaxDur);
		outJSON.put("actNames", actNames);
		outJSON.put("actIDs", actIDs);
		outJSON.put("actMinDurs", actMinDurs);
		outJSON.put("actMaxDurs", actMaxDurs);
		outJSON.put("actESTs", actESTs);
		outJSON.put("actLETs", actLETs);
		outJSON.put("actRestricts", actRestricts);
		outJSON.put("otherWeakRestrictCount", otherWeakRestrictCount);
		outJSON.put("otherStrongRestrictCount", otherStrongRestrictCount);
		outJSON.put("currentTime", currentTime);
		outJSON.put("strImg", strImg);
		outJSON.put("actDetails", actDetails);
		outJSON.put("debugInfo", debugInfo);
		
		try {
			if (toAgentNum.equals("0")) {
				toClientAgent0Queue.put(outJSON);
			} else if (toAgentNum.equals("1")) {
				toClientAgent1Queue.put(outJSON);
			}
		} catch (Exception e) {
			System.err.println("Error while adding to toClientQueue");
			System.err.flush();
		}
		
	}
	
	
	private void sendCurrentChoicesToAllClients(List<List<String>> acts, ArrayList<ArrayList<String>> minDurs, ArrayList<ArrayList<String>> maxDurs, 
			                                        List<List<String>> actNames) {
		int lowestAgentReadyToConfirm = 99999;
		String clearToConfirm = "true";
		
		// send the current activity choices for every agent
		for (int a = 0; a < numAgents; a++) {
			
			// If multiple agents are at a decision point, require the lowest numbered agent to make a decision before allowing higher agents to confirm acts
			if (acts.get(a).size() > 0 && a < lowestAgentReadyToConfirm) {
				lowestAgentReadyToConfirm = a;
			}
			if (lowestAgentReadyToConfirm < a) {
				clearToConfirm = "false";
			}
			
			sendJSONToClient(
					String.valueOf(a),	// agentNum
					"currentChoices", // infoType
					"TODO", // startTime
					"", // lastActivity
					clearToConfirm,
					acts.get(a), 	 // nextActivities
					minDurs.get(a), // nextActsMinDur
					maxDurs.get(a), // nextActsMaxDur
					actNames.get(a), // actNames
					new ArrayList<String>(), // actIDs
					new ArrayList<String>(), // actMinDurs
					new ArrayList<String>(), // actMaxDurs
					new ArrayList<String>(), // actESTs
					new ArrayList<String>(), // actLETs
					new ArrayList<String>(), // actRestricts
					"",						 // otherWeakRestrictCount
					"",						 // otherStrongRestrictCount
					"",						 // currentTime
					"",						 // strImg
					new JSONObject(),
					new ArrayList<String>()  // debugInfo
			);
		}
	}
	
	
	/*
	 * This class creates and sends a gantt chart of the current system dtp to a specified (or all) client(s)
	 * Author: Drew
	 * Gantt is sent as a 64byte encoded String
	 */
	private void sendGanttToAgent(String agentNum, List<List<String>> remActs) {
	
		// delete any outdated gantt images
//		File ganttDir = new File("forClient_image.png");
//		ganttDir.delete();
		
		// create visual png of DTP with current state
//		Viz.createAndSaveDTPDiagram(dtp, initialDTP, getSystemTime(),1);
		
		
		// retrieve all of the data needed to create a plot from the timepoints inside viz
		ArrayList< HashMap< String, ArrayList< String > > > plotData = Viz.retrievePlotData( dtp, initialDTP, getSystemTime() );
		for (int i = 0; i < plotData.size(); i++) {plotData.get(i).get("currentTime").add( String.valueOf(getSystemTime()) );} // add system current time to each agent
		
			
			// get count of partially constricted and significantly constricted activities
			// go through each agent, then each activity and check the activity ratio
			double ratioThreshold = 0.66;
			ArrayList< Integer > weakRestrictCounts = new ArrayList(numAgents);
			ArrayList< Integer > strongRestrictCounts = new ArrayList(numAgents);
			int totalWeakCount = 0;
			int totalStrongCount = 0;
			
			
			// send JSON file with infoType and image as encoded String
			// if agentNum is set to "ALL", send the JSON to all clients/agents
			if ( agentNum.equals("ALL") ) {
				for (int a = 0; a < numAgents; a++) {
					sendJSONToClient(String.valueOf(a),
							"ganttImage", // infoType
							"", // startTime
							"", // lastActivity
							"", // clearToConfirm
							new ArrayList<String>(), // nextActivities
							new ArrayList<String>(), // nextActsMinDur
							new ArrayList<String>(), // nextActsMaxDur
							plotData.get(a).get("actNames"),   // actNames
							plotData.get(a).get("actIDs"),		// actIDs
							plotData.get(a).get("actMinDurs"), // actMinDurs
							plotData.get(a).get("actMaxDurs"), // actMaxDurs
							plotData.get(a).get("actESTs"),    // actESTs
							plotData.get(a).get("actLETs"),    // actLETs
							plotData.get(a).get("actRestricts"), // actRestricts
							"", // String.valueOf( totalWeakCount - weakRestrictCounts.get(a) ),     // otherWeakRestrictCount
							"", // String.valueOf( totalStrongCount - strongRestrictCounts.get(a) ), // otherStrongRestrictCount
							plotData.get(a).get("currentTime").get(0),			 // currentTime
							"",   // imgStr
//							imageString,			    // imgStr
							new JSONObject(),
							new ArrayList<String>()  // debugInfo
					);

				}
			
			// otherwise only send to specified client
			} else if(Integer.valueOf(agentNum) >= 0) {
				sendJSONToClient(agentNum,
						"ganttImage", // infoType
						"", // startTime
						"", // lastActivity
						"", // clearToConfirm
						new ArrayList<String>(), // nextActivities
						new ArrayList<String>(), // nextActsMinDur
						new ArrayList<String>(), // nextActsMaxDur
						plotData.get(Integer.valueOf(agentNum)).get("actNames"),   // actNames
						plotData.get(Integer.valueOf(agentNum)).get("actIDs"),		// actIDs
						plotData.get(Integer.valueOf(agentNum)).get("actMinDurs"), // actMinDurs
						plotData.get(Integer.valueOf(agentNum)).get("actMaxDurs"), // actMaxDurs
						plotData.get(Integer.valueOf(agentNum)).get("actESTs"),    // actESTs
						plotData.get(Integer.valueOf(agentNum)).get("actLETs"),    // actLETs
						plotData.get(Integer.valueOf(agentNum)).get("actRestricts"), // actRestricts
						"", //String.valueOf( totalWeakCount - weakRestrictCounts.get(Integer.valueOf(agentNum)) ),     // otherWeakRestrictCount
						"", //String.valueOf( totalStrongCount - strongRestrictCounts.get(Integer.valueOf(agentNum)) ), // otherStrongRestrictCount
						plotData.get(Integer.valueOf(agentNum)).get("currentTime").get(0),			 // currentTime
						"",   // imgStr
//						imageString,			 // imgStr
						new JSONObject(),
						new ArrayList<String>()  // debugInfo
				);
				
			}
			
	}
	
	private JSONObject createInternalJSON(HashMap<String, String> actHistDetails) {
		JSONObject tempJSON = new JSONObject();

		tempJSON.put("agentNum", actHistDetails.get("agentNum"));
		tempJSON.put("activityName", actHistDetails.get("activityName"));
		tempJSON.put("activityDuration", actHistDetails.get("activityDuration"));
		tempJSON.put("infoType", "confirmActivity");
		
		return tempJSON;
	}

	/*
	 * Drew: We need a method that acts in replacement of the stdin
	 * It will need to hold up the main thread until the client sends it information
	 * For now, we will ignore the possibility of needing to ping the server and re-request information
	 * This should only be called when the program cannot do anything until it hears from the client
	 */
	private JSONObject getNextRequest() {
		JSONObject jsonIN = new JSONObject();
		try {
			while (true) {
				Thread.sleep(100);
				if (fromClientQueue.size() > 0) {
					jsonIN = fromClientQueue.poll();
					System.out.println("- Server recieved '" + (String) jsonIN.get("infoType") + "' from client #" + (String) jsonIN.get("agentNum"));
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Error while waiting in getNextRequest()");
			System.err.println(e);
			System.err.flush();
		}
		return jsonIN;
	}
	
}

