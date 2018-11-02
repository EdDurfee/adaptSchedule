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
 * This class provides an interaction framework that allows users to step through a schedule.
 * The displayed information and permitted options are restricted such that only the currently permissible options are provided
 *
 */
public class InteractionStageGUI implements Runnable {
	private boolean textUI = false;
	
	//private static String problemFile = "stn1.xml"; yices
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
	private static int currentAgent;  //currentAgent = [0,numAgents], currentAgent = numAgents means to control the system clock (i.e. all agents)
	private static Scanner cin = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
	private static final int DEBUG = -1;
	private static ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> > previousDTPs;
	private static int revision = 0;
	private static int skip_time = -1;
	//need a data structure to keep track of activities currently being performed and their intervals (needed for concurrent activity ability of system)
	private static ArrayList<SimpleEntry<String, Interval>> ongoingActs = new ArrayList<SimpleEntry<String,Interval>>();
	
	private HashSet<String> confirmedActSet = new HashSet<String>();
	private HashSet<String> currentActSet = new HashSet<String>();
	
	public LinkedBlockingQueue<JSONObject> toClientAgent0Queue    = new LinkedBlockingQueue<JSONObject>();
	public LinkedBlockingQueue<JSONObject> toClientAgent1Queue    = new LinkedBlockingQueue<JSONObject>();
	public LinkedBlockingQueue<JSONObject> fromClientQueue  = new LinkedBlockingQueue<JSONObject>();
	
	// list of simpleEntry that contains <actName, actEndTime> and holds all of the activities currently being performed by each agent
	public ArrayList<SimpleEntry<String, Integer>> agent0CurrentConfirmedActs;
	public ArrayList<SimpleEntry<String, Integer>> agent1CurrentConfirmedActs;
	
	public boolean processingStartup = false;
	public boolean processingTentativeAct = false;
	public boolean processingConfirmedAct = false;
	public boolean processingModify = false;
	public boolean processingAdd = false;
	public boolean processingAdvSysTime = false;
//	public String  processingAgentNum     = "";
	public boolean readyForNextReq = true;
//	Integer idleTime = 0; // the smallest idle time confirmed by an agent since the last clock advance
	String agentNum = "";
	String actName = "";
	String actDur = "";
	JSONObject inJSON;
	
	ArrayList< HashMap< String, ArrayList< String > > > oldPlotData;
	
	
	// run is activated as a new thread inside of AppServerGUI.java
	@SuppressWarnings("unused")
	public void run(){
		
		// wait a second for the http server to get running first (not essential but the output looks better)
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			System.out.println("Failed to wait");
		}
			
		System.out.println("new server instanced");

		// Allow a user on the server to select which problem file they want to demo
		System.out.println("\nEnter number of problem file you would like to use.");
		System.out.println("0 - toyExampleDTP.xml");
		System.out.println("1 - multiagentSampleProblem_simp.xml");
		System.out.println("2 - multiagentSampleProblem.xml");
		System.out.println("3 - toyexample.xml");
		System.out.println("4 - drew_test.xml");
		System.out.println("5 - DUTPtoyexampleNoSE.xml");
		System.out.println("6 - toyExampleEd.xml");
		System.out.println("7 - parentSampleProblem.xml");
		System.out.println("8 - sampleProb2.xml");
		
		Integer problemNum = Integer.valueOf(cin.next());
		
		switch ( problemNum ) {
		case 0: problemFile = "toyExampleDTP.xml"; break;
		case 1: problemFile = "multiagentSampleProblem_simp.xml"; break;
		case 2: problemFile = "multiagentSampleProblem.xml"; break;
		case 3: problemFile = "toyexample.xml"; break;
		case 4: problemFile = "drew_test.xml"; break;
		case 5: problemFile = "DUTPtoyexampleNoSE.xml"; break;
		case 6: problemFile = "toyExampleEd.xml"; break;
		case 7: problemFile = "parentSampleProblem.xml"; break;
		case 8: problemFile = "sampleProb2.xml"; break;
		default:
			System.out.println("Illegal problem number entered. Restart server to try again.");
		}
		
		// load in the selected file and generate the DTP from it
		File tmpDir = new File(problemFile);
		if (!tmpDir.exists()) {
			System.out.println("Failed to load file. Double check that file is in project directory.");
			return;
		}
		
		// this is a vector of vectors that contains that sequence of confirmed activities and their details
		ArrayList< HashMap< String, String >> actHistory = new ArrayList<HashMap<String,String>>();
		
		// this is queue where server can place requests that it wants to process later
		LinkedBlockingQueue<JSONObject> internalRequestQueue    = new LinkedBlockingQueue<JSONObject>();

		// this is where JSON requests from clients will be put
		JSONObject jsonIN = new JSONObject();

		// hardcoded to 2 agents. Will still work with 1 agent though
		agent0CurrentConfirmedActs = new ArrayList<SimpleEntry<String, Integer>>(0);
		agent1CurrentConfirmedActs = new ArrayList<SimpleEntry<String, Integer>>(0);


		
		
		// set up dtp
		dtp = new ProblemLoader().loadDTPFromFile(problemFile);
		System.out.println(problemFile + " loaded succesfully.\n\n"); // file loaded correctly

		// initialize variables and the DTP
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);	
		dtp.simplifyMinNetIntervals();
		
		
		numAgents = dtp.getNumAgents();
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		previousDTPs = new ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> >(numAgents);
		for(int i = 0; i < numAgents; i++) previousDTPs.add( new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>() );
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		//dtp.printConstraints(Generics.getLogStream());
		
		// if > 1 agents, put the system into 'clock-mode' which allows it to interact with all agent schedules, otherwise keep it at currentAgent = 0
		currentAgent = 0;
		if(numAgents > 1){
			currentAgent = numAgents;
		}
		dtp.setCurrentAgent(currentAgent);
		
		
		// MAX_LET == the largest of the latest end times of all activities
		MAX_LET = (int) Math.ceil(getMaxLatestEndTime(dtp));
		// Drew: Trying to temp patch a bug
		MAX_LET = 1440;
		
		initialDTP = dtp.clone();
		
		oldPlotData = new ArrayList< HashMap< String, ArrayList< String > > >();
		
		
		
		
		
		
		
		
		
		
		
		
		/* TEMP TEST
		 * TODO: Test this then remove
		 * The idea here is to allow a user to add an activity through the server but to do it before any of the typical looping and
		 * before they get a chance to select activities. This is theoretically the simplest place to add a new activity, and if it can be
		 * first done here, it should be easier to expand out to the future case 
		 */
	
/*
		// THE APPROACH BELOW ATTEMPTS TO HANDLE DELETING/ADDING ACT BY MODIFYING AN XML
		File modifiedXML = null;
		String xmlModString = "";
		try {
			File originalXML = new File(problemFile);
			String newXMLFileName = "tempModifiedXML.xml";
			modifiedXML = new File(newXMLFileName);
			modifiedXML.delete();
			Files.copy(originalXML.toPath(), modifiedXML.toPath());
			
		} catch (Exception e) {
			System.err.println("Error is creating new modified XML file.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()) + "\n");
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
		xmlModString = XMLParser.removeSpecificActivity(xmlModString, "breakfast");
		
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
*/		
		
/*		
 * 		// THE APPROACH BELOW ATTEMPTS TO HANDLE ADDING ACT BY ADDING AND MODYFING TIMEPOINTS
 * 
		String addActName = "TestAddAct";
		int newMinDurs_add = 30;   //new ArrayList<String>(); newMinDurs_add.add("00:30");
		int newMaxDurs_add = 240;  //new ArrayList<String>(); newMinDurs_add.add("02:00");
		int newEST_add 			= 0000;
		int newLST_add 			= 540;
		int newEET_add 			= 30;
		int newLET_add 			= 660;


		DisjunctiveTemporalProblem beforeAddDTP = dtp.clone();
		
		
		//dtp = new ProblemLoader().loadDTPFromFile(problemFile);
		System.out.println(problemFile + " loaded succesfully.\n\n"); // file loaded correctly

		

		
		
		// code chunk below is from inside simple dtp when initializing
				// So, ignoring constraints, it appears all that needs to be done to create a dtp is add timepoints
				// Can this be replaced by the addTimepoint() method?
				//     ^^ addTimepoint does all of below 
				//		^^ SO -> YES
		Timepoint start = new Timepoint(addActName+"_S",0); //Drew: add timepoint to agent 0
		Timepoint end = new Timepoint(addActName+"_E",0);
		dtp.addTimepoint(start);   // Drew: zero timepoint is already added in the initialization
		dtp.addTimepoint(end);
		initialDTP.addTimepoint(start);
		initialDTP.addTimepoint(end);
		
		
		// next question: What are the constraints that need to be added?
				// if constraints are ignored, just need timepoints that have actname_S and actname_E
				// however, including constraints is necessary to set est, let, eet, lst, and min/max duration
		
		// get duration bounds
		ArrayList<TemporalDifference> new_minDurs = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> new_maxDurs = new ArrayList<TemporalDifference>();
		int minDuration = newMinDurs_add;
		int maxDuration = newMaxDurs_add;
		new_minDurs.add(new TemporalDifference(start,end,-minDuration)); // Drew: Should minDuration be negative or positive coming into this line
		new_maxDurs.add(new TemporalDifference(end,start,maxDuration));
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(new_minDurs);
		tdVec.add(new_maxDurs);
		dtp.addAdditionalConstraints( DisjunctiveTemporalConstraint.crossProduct(tdVec) ); // Drew: Here we are adding 'additionalConstraints' as opposed to 'tempConstraints'
		initialDTP.addAdditionalConstraints( DisjunctiveTemporalConstraint.crossProduct(tdVec) );																				// does it matter?
		
		// get avail bounds
		//default values of 0 for earliest start-/end- times, infinite for latest start-/end-times
		ArrayList<TemporalDifference> eStart = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> lStart = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> eEnd = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> lEnd = new ArrayList<TemporalDifference>();
			
		Timepoint zero = dtp.getTimepoint("zero");
		int est = newEST_add;
		int lst = newLST_add;
		int eet = newEET_add;
		int let = newLET_add;
		eStart.add(new TemporalDifference(zero,start,-est));
		lStart.add(new TemporalDifference(start,zero,lst));
		eEnd.add(new TemporalDifference(zero,end,-eet));
		lEnd.add(new TemporalDifference(end,zero,let));

		tdVec.clear();
		tdVec.add(eStart);
		tdVec.add(lStart);
		tdVec.add(eEnd);
		tdVec.add(lEnd);
		dtp.addAdditionalConstraints( DisjunctiveTemporalConstraint.crossProduct(tdVec) );
		initialDTP.addAdditionalConstraints( DisjunctiveTemporalConstraint.crossProduct(tdVec) );
		
		
		
		// reconfigure the DTP with new timepoints and constraints
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);	
		dtp.simplifyMinNetIntervals();
		initialDTP.updateInternalData();
		initialDTP.enumerateSolutions(0);	
		initialDTP.simplifyMinNetIntervals(); // initialDTP also needs to be updated with added activities, although it will maintain its state of no confirmed acitivities
		
*/		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*
		 * Begin loop
		 * Every cycle through this loop represents processing of 1 JSON request from a client
		 * Request is 'received' part way through the loop
		 * 		Code above receiving new request is focused on processing last request and updating the DTPs
		 * 		Code below receiving new request is focused on parsing the request and deciding how to react to it in the above code
		 */
		while(true){
			try{

				// System is currently not handling concurrent activities
				CONCURRENT = false;
				
				// Optional debug output
				if(DEBUG >= 2) dtp.printSelectionStack();
				if(DEBUG >= 1){
					Generics.printDTP(dtp);
					System.out.println("Calls to solver" +dtp.getCallsToSolver());
				} if (DEBUG == 0){ // Print Deltas mode
					if(prevDTP == null) Generics.printDTP(dtp);
					else dtp.printDeltas(dtp.getDeltas(prevDTP));
				}

				
				// gets universal minTime for all agents
				// minTime == time of next decision point, AKA earliest EST of remaining activities
				int minTime = dtp.getMinTime(); 
				minTime = Math.min(minTime, MAX_LET); // should prevent minTime from going to infinity?

				// if you have moved past the latest LET, exit the loop and end the server
				if(getSystemTime() >= MAX_LET){
					return;
				}
				// if the next decision point is in the past, there has been a mistake in managing time of system and DTPs
				else if(minTime < getSystemTime()){
					System.out.println("Error: System time out of sync, expect a failure.");	
				}
				
				// get list of current activity choices and all incomplete activites for all agents
				List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime()); // returns the activities
				List<List<String>> allactNames = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime());
				
				// get list of max slack (max idle time) for each agent at its corresponding index
				List<Integer> maxSlack = dtp.getMaxSlack();
				
				// update ongoing activities data struct by removing completed activities
				ArrayList<Integer> toRemove = new ArrayList<Integer>();
				for(int i = 0; i < ongoingActs.size(); i++){
					SimpleEntry<String, Interval> pair = ongoingActs.get(i);
					Interval curr = pair.getValue();
					int time = getSystemTime();
					
					// if the system time is greater than or equal to the end time of this activity, then it is no longer ongoing
					if(time >= curr.getUpperBound()) {
						toRemove.add(i);
					}
					
					if (i > ongoingActs.size()) {
						System.out.println("Error: Concurrent loop error");
					}
				}
				
				// need to reverse the order of toRemove so that highest indices are removed first, thereby preserving the location of lower later-removed indices
				Collections.reverse(toRemove);
				
				// remove completed activities
				for(int j : toRemove) {
					if (ongoingActs.size() <= j) {
						System.out.println("Error: Concurrent activities error");
					}
					
					ongoingActs.remove(j);
				}
				
				// decide whether to add 'idle' as an activity option for each agent
				// agent can only idle if it has available slack and needs to currently make a selection
				if(maxSlack.size() == 1 && maxSlack.get(0) > 0 && getSystemTime() == dtp.getMinTime()) {
					activities.get(0).add("idle"); // if only 1 agent
				} else for(int agent = 0; agent < maxSlack.size(); agent++){
					if ( maxSlack.get(agent) > 0 && getSystemTime(agent) == getSystemTime()) {
						if (agent == 0 && agent0CurrentConfirmedActs.size() == 0) activities.get(agent).add("idle"); //agent"+agent+"Idle"); 
						if (agent == 1 && agent1CurrentConfirmedActs.size() == 0) activities.get(agent).add("idle"); //agent"+agent+"Idle"); 
					}
				}
				
				// create 2d vector of min and max durations of next possible activities
				// first index is agent num, second index is act ind that coordinates to activities vector
				ArrayList<ArrayList<String>> nextActsMinDur = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) nextActsMinDur.add(new ArrayList<String>());
				ArrayList<ArrayList<String>> nextActsMaxDur = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) nextActsMaxDur.add(new ArrayList<String>());
				// keep a list of activity names that have zero duration and need to be auto performed
//				ArrayList<ArrayList<String>> zeroDurActsToPerform = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) nextActsMaxDur.add(new ArrayList<String>());
				for (int agent = 0; agent < numAgents; agent++) {
					for (int act = 0; act < activities.get(agent).size(); act++) {
						if (activities.get(agent).get(act).equals("idle")) {
							nextActsMinDur.get(agent).add("5"); // min idle time is no less than 5 minutes
							nextActsMaxDur.get(agent).add(String.valueOf(maxSlack.get(agent)));
							
							continue;
						}

						IntervalSet interval = dtp.getInterval(activities.get(agent).get(act)+"_S", activities.get(agent).get(act)+"_E").inverse().subtract(zeroInterval);
						nextActsMinDur.get(agent).add(String.valueOf( (int) interval.getLowerBound()));
						nextActsMaxDur.get(agent).add(String.valueOf( (int) interval.getUpperBound()));

						// if this activity has zero max duration, force it to be performed and back to top
						if (nextActsMaxDur.get(agent).get(nextActsMaxDur.get(agent).size() - 1) == "0") {
							//zeroDurActsToPerform.get(agent).add(activities.get(agent).get(act));
							int dur = 0;
							dtp.executeAndAdvance(-getSystemTime(), activities.get(agent).get(act)+"_S",-getSystemTime(), activities.get(agent).get(act)+"_E",true, dur, true);
							dtp.simplifyMinNetIntervals();
						}
						
					}
				}		
				
				// create 2d vector of min and max durations of all remaining activities
				// allactNames should never include idle
				// first index is agent num, second index is act ind that coordinates to activities vector
				ArrayList<ArrayList<String>> actMinDurs = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) actMinDurs.add(new ArrayList<String>());
				ArrayList<ArrayList<String>> actMaxDurs = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) actMaxDurs.add(new ArrayList<String>());
				for (int agent = 0; agent < numAgents; agent++) {
					for (int act = 0; act < allactNames.get(agent).size(); act++) {
						IntervalSet interval = dtp.getInterval(allactNames.get(agent).get(act)+"_S", allactNames.get(agent).get(act)+"_E").inverse().subtract(zeroInterval);
						actMinDurs.get(agent).add(String.valueOf( (int) interval.getLowerBound()));
						actMaxDurs.get(agent).add(String.valueOf( (int) interval.getUpperBound()));
					}
				}
				
				
				// depending on the request type of last request, communicate system changes to clients
				if (processingConfirmedAct || processingTentativeAct || processingModify || processingAdd || processingAdvSysTime || processingStartup) {
					
					// if this is not a tentative activity selection, send new activity options to each client
					if (!processingTentativeAct) {
						sendCurrentChoicesToAllClients(activities, nextActsMinDur, nextActsMaxDur);
						processingModify = false;
						processingAdd = false;
						processingAdvSysTime = false;
					}
					
					// if processing a confirmed action, need to update gantts of all clients to reflect permanent dependent changes
					if (processingConfirmedAct) {
						sendGanttToAgent("ALL", allactNames);
						processingConfirmedAct = false;
					}
					
					// if tentative action or startup, only send new gantt to agent of request
					else if (processingTentativeAct || processingStartup) {
						sendGanttToAgent(agentNum, allactNames);
						processingStartup = false;
					}
					
					// if this is a tentative activity, undo the activity (because it was artificially confirmed inside the system to generate new info)
					if (processingTentativeAct == true) {
						removeTentAct(agentNum);
						
						// need to remove undone act from list of ongoing acts
						for (int i = 0; i < ongoingActs.size(); i++) {
							if (ongoingActs.get(i).getKey() == actName) {
								ongoingActs.remove(i);
								break;
							}
						}
						
						processingTentativeAct = false;
						continue; // need to go back to top to get a full undo / removal of tentative activity
					}
				}
			
				
				// in order to enable system to reset and catch up properly, pull activities out of internalRequestQueue
				//  this queue will be populated with activities that were already completed on previous systems
				if (internalRequestQueue.size() > 0) {
					inJSON = internalRequestQueue.poll();
				} else {
					// wait for the client to send a POST request
					inJSON = getNextRequest();
				}
				
				
				agentNum = String.valueOf(inJSON.get("agentNum"));
				actName = String.valueOf(inJSON.get("activityName"));
				actDur = String.valueOf(inJSON.get("activityDuration"));
				
				// depending on the type of request received (AKA the infoType), process the request
				switch ( (String) inJSON.get("infoType")) {

					case "startup":
						processingStartup = true;
						break;
						
					// if it is a confirmed activity or a tentative activity the system needs to treat it the same and internally 'perform' the activity
					case "confirmActivity":
					case "tentativeActivity":
						
						// save current state of DTP to enable undoing later
						prevDTP = dtp.clone();
						previousDTPs.get(Integer.valueOf(agentNum)).push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
						
						// if activity requested is an idle, adjust the clock of the dtp idling and add idle to the current confirmed activities if necessary
						if (actName.equals("idle")) {
							getAndPerformIdle(minTime-getSystemTime(), maxSlack.get(Integer.valueOf(agentNum)), Integer.valueOf(actDur.substring(3)), Integer.valueOf(agentNum));
							int idleTime = Integer.valueOf(actDur.substring(3));

							if ( ((String) inJSON.get("infoType")).equals("confirmActivity") ) {
								if (agentNum.equals("0")) agent0CurrentConfirmedActs.add(new SimpleEntry("idle", getSystemTime() + idleTime)); // minutes + minutes ?
								if (agentNum.equals("1")) agent1CurrentConfirmedActs.add(new SimpleEntry("idle", getSystemTime() + idleTime));
							}
						}
						// if activity is not an idle, process it internal to the corresponding DTP
						else {
							
							IntervalSet endTime = dtp.getInterval("zero", actName+"_E"); // endTime = EET
							int time = Generics.fromTimeFormat( actDur ); // actDur always comes in minutes
							
							// check if given duration is valid (within max duration constraints)
							IntervalSet interval = dtp.getInterval(actName+"_S", actName+"_E").inverse().subtract(zeroInterval);
							if(!interval.intersect(time)){
								System.out.println("Unexpected duration response \""+actName+"\"");
								break;
							}
							
							// if the given duration + systemTime does not get you past EET, you need to force idle for a bit
							int idle = (int) (-endTime.getUpperBound() - getSystemTime() - time);
							if(idle > 0){	
								System.out.println("Forcing immediate idle for " + String.valueOf(idle));
							} else {
								idle = 0;
							}
							
							
							// interval from current time to end of selected duration
							Interval curr_int = new Interval(getSystemTime() + idle, getSystemTime() + idle + time);
							ongoingActs.add(new SimpleEntry<String,Interval>(actName, curr_int));
							
							// if a confirmed activity, add the activity to the corresponding list of confirmedActs
							// also add this act info to the history list
							if ( ((String) inJSON.get("infoType")).equals("confirmActivity") ) {
								if (agentNum.equals("0")) agent0CurrentConfirmedActs.add(new SimpleEntry(actName, getSystemTime() + idle + time)); // minutes + minutes ?
								if (agentNum.equals("1")) agent1CurrentConfirmedActs.add(new SimpleEntry(actName, getSystemTime() + idle + time));
							
								HashMap<String,String> thisAct = new HashMap<String,String>();
								thisAct.put("agentNum",          agentNum);
								thisAct.put("activityName",      actName);
								thisAct.put("activityDuration",  actDur);
								
								actHistory.add(thisAct);
							}
							
							// if there are any zero duration activities to perform, automatically perform them
							/*for (int a = 0; a < zeroDurActsToPerform.size(); a++) {
								for (int act = 0; act < zeroDurActsToPerform.get(a).size(); act++) {
									int dur = 0;
									dtp.executeAndAdvance(-getSystemTime(), zeroDurActsToPerform.get(a).get(act)+"_S",-getSystemTime(), zeroDurActsToPerform.get(a).get(act)+"_E",true, dur, true);
									dtp.simplifyMinNetIntervals();
								}
							}*/
							
							// internal to the corresponding dtp, perform the activity and adjust the internal clock
							dtp.executeAndAdvance(-( getSystemTime() + idle), actName+"_S",-(getSystemTime() + idle + time),actName+"_E",true, time, true);
							dtp.simplifyMinNetIntervals();
						}
						
						if ( ((String) inJSON.get("infoType")).equals("tentativeActivity")) { processingTentativeAct = true; }
						if ( ((String) inJSON.get("infoType")).equals("confirmActivity"))   { processingConfirmedAct = true; }
						
						break;
						
					// this will be sent by the client when it wants to populate the 'modify activity' screen
					case "requestActDetails":
						
						JSONObject actDetails = new JSONObject();
						
						// get the list of activities that are viable to have their duration tightened and availability tightened
						ArrayList<String> modifiableDurActs   = (ArrayList<String>) Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARDUR, -getSystemTime()));
						ArrayList<String> modifiableAvailActs = (ArrayList<String>) Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARAVAIL, -getSystemTime()));
						
						ArrayList<String> minDurs = new ArrayList<String>();
						ArrayList<String> maxDurs = new ArrayList<String>();
						String EST = "";
						String LST = "";
						String EET = "";
						String LET = "";
						
						// actname will be blank when client only is looking for modifiable lists
						// if not black, get information on the activity (availability & duration)
						if (!actName.equals("")) {
							IntervalSet currentDuration = dtp.getInterval(actName+"_S", actName+"_E").inverse(); // this interval will be [minDur,maxDur]
							IntervalSet startAvail  = dtp.getInterval("zero", actName+"_S").inverse(); // this interval will be [EST,LST]
							IntervalSet endAvail    = dtp.getInterval("zero", actName+"_E").inverse(); // this interval will be [EET,LET]
							
							minDurs.add( String.valueOf( currentDuration.getLowerBound() ) );
							maxDurs.add( String.valueOf( currentDuration.getUpperBound() ) );
							EST = String.valueOf( startAvail.getLowerBound() );
							LST = String.valueOf( startAvail.getUpperBound() );
							EET = String.valueOf( endAvail.getLowerBound()   );
							LET = String.valueOf( endAvail.getUpperBound()   );
						}

						// send the lists of modifiable activities along with details of the specific activity (if specified)
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
						
					// client will send this when the user has entered new (tighter) duration / availability for an activity
					// the system is not capable of checking that the user modifications are legal => they can only tighten constraints
					case "modifyActivity":
						
						prevDTP = dtp.clone();
						SimpleEntry<Integer, DisjunctiveTemporalProblem> tempSE_mod = new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP);
						Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> tempStack_mod = new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>();
						tempStack_mod.push(tempSE_mod);
						previousDTPs.add( tempStack_mod );
						
						JSONObject actJSON_mod = (JSONObject) inJSON.get("actDetails");
						
						List<String> newMinDurs_mod  = (ArrayList<String>) actJSON_mod.get("minDurs"); // format:  hh:mm
						List<String> newMaxDurs_mod  = (ArrayList<String>) actJSON_mod.get("maxDurs"); // format:  hh:mm
						String newEST_mod 			 = String.valueOf(actJSON_mod.get("EST")); // format:  hh:mm
						String newLST_mod 			 = String.valueOf(actJSON_mod.get("LST")); // format:  hh:mm
						String newEET_mod 			 = String.valueOf(actJSON_mod.get("EET")); // format:  hh:mm
						String newLET_mod 			 = String.valueOf(actJSON_mod.get("LET")); // format:  hh:mm
						
						// Duration
						AddIntervalSet(actName+"_S", actName+"_E", getSystemTime(), Generics.stringToInterval( "[" + newMinDurs_mod.get(0) + "," + newMaxDurs_mod.get(0) + "]" ) );
						
						// if no value submitted for these, use default max value
						if (newEST_mod.equals("")) {newEST_mod = "00:00";}
						if (newLST_mod.equals("")) {newLST_mod = "24:00";}
						if (newEET_mod.equals("")) {newEET_mod = "00:00";}
						if (newLET_mod.equals("")) {newLET_mod = "24:00";}
						
						// EST and LST
						AddIntervalSet("zero", actName+"_S", getSystemTime(), Generics.stringToInterval( "[" + newEST_mod + "," + newLST_mod + "]" ) );
						// EET and LET
						AddIntervalSet("zero", actName+"_E", getSystemTime(), Generics.stringToInterval( "[" + newEET_mod + "," + newLET_mod + "]" ) );
						
						processingModify = true;
						break;
					
						
						// client will send this when the user has entered new (tighter) duration / availability for an activity
						// the system is not capable of checking that the user modifications are legal => they can only tighten constraints
						case "addActivity":
							
							prevDTP = dtp.clone();
							SimpleEntry<Integer, DisjunctiveTemporalProblem> tempSE_add = new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP);
							Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> tempStack_add = new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>();
							tempStack_add.push(tempSE_add);
							previousDTPs.add( tempStack_add );
							
							JSONObject actJSON_add = (JSONObject) inJSON.get("actDetails");
							
//							List<String> newMinDurs_add  = (ArrayList<String>) actJSON_add.get("minDurs"); // format:  hh:mm
//							List<String> newMaxDurs_add  = (ArrayList<String>) actJSON_add.get("maxDurs"); // format:  hh:mm
//							String newEST_add 			 = String.valueOf(actJSON_add.get("EST")); // format:  hh:mm
//							String newLST_add 			 = String.valueOf(actJSON_add.get("LST")); // format:  hh:mm
//							String newEET_add 			 = String.valueOf(actJSON_add.get("EET")); // format:  hh:mm
//							String newLET_add 			 = String.valueOf(actJSON_add.get("LET")); // format:  hh:mm
							
							// Duration
//							AddIntervalSet(actName+"_S", actName+"_E", getSystemTime(), Generics.stringToInterval( "[" + newMinDurs_add.get(0) + "," + newMaxDurs_add.get(0) + "]" ) );
							
							// if no value submitted for these, use default max value
//							if (newEST_add.equals("")) {newEST_add = "00:00";}
//							if (newLST_add.equals("")) {newLST_add = "24:00";}
//							if (newEET_add.equals("")) {newEET_add = "00:00";}
//							if (newLET_add.equals("")) {newLET_add = "24:00";}
							
//							// EST and LST
//							AddIntervalSet("zero", actName+"_S", getSystemTime(), Generics.stringToInterval( "[" + newEST_add + "," + newLST_add + "]" ) );
//							// EET and LET
//							AddIntervalSet("zero", actName+"_E", getSystemTime(), Generics.stringToInterval( "[" + newEET_add + "," + newLET_add + "]" ) );
							
							processingAdd = true;
							
							break;
						
					// delete an activity that has not yet been performed
					// populate internalRequestQueue with all activities that have already been completed
					//  the items in the internalRequestQueue will be processed by the main loop
					case "deleteActivity":
						
						// set up historical sequence of events that led up to current time in system before deletion
						for (HashMap<String,String> h : actHistory) {
							JSONObject temp = createInternalJSON(h);
							internalRequestQueue.add(temp);
						}
						actHistory.clear();
						
						
						// remove the selected activity from the system
						deleteActFromXML(actName);
						
						
						break;
					
					// temporary demo system to allow a client to trigger an advancing of time to the next decision point (minTime)
					case "advSysTime":
						int temp = minTime - getSystemTime();
						advanceSystemTimeTo(minTime);
						dtp.advanceToTime(-getSystemTime(), temp, false);
						dtp.simplifyMinNetIntervals();
						
						// check each current activity being performed for the agent. If the activity ends at or before the new currentTime (minTime), remove it from list
						for (int i = 0; i < agent0CurrentConfirmedActs.size(); i++) {
							if ( agent0CurrentConfirmedActs.get(i).getValue() <= minTime ) agent0CurrentConfirmedActs.remove(i);
						}
						for (int i = 0; i < agent1CurrentConfirmedActs.size(); i++) {
							if ( agent1CurrentConfirmedActs.get(i).getValue() <= minTime ) agent1CurrentConfirmedActs.remove(i);
						}
						
						// after advancing the clock all clients will need an updated gantt chart
						sendGanttToAgent("ALL", allactNames);
						
						processingAdvSysTime = true;
						
					default:
						break;
					
				}
				
			} catch (Exception e) {
				System.err.println("Error in request processing.  Please try again.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()));
				System.err.flush();
			}
		}
	}
	
	
	// Removes the last 'performed' activity from the dtp
	// this should be called after every tentative action gantt sent to the client to revert dtp back to only confirmed acts
	private static void removeTentAct(String agentNumber) {
		if(previousDTPs.get( Integer.valueOf(agentNumber)).empty() ) {
			System.out.println("The schedule has not yet been modified.\n");
			return;
		}
		System.out.println("removing tentative selection.");
		
		SimpleEntry<Integer, DisjunctiveTemporalProblem> ent = previousDTPs.get( Integer.valueOf(agentNumber)).pop();
		
		setSystemTime(ent.getKey());
		System.out.println("Resetting time to "+Generics.toTimeFormat(getSystemTime()));
		dtp = ent.getValue();
	
	}
	
	
	private static boolean disjunctivelyUncertainProblem(DisjunctiveTemporalProblem dtp) {
		if ((dtp instanceof DUTP) || (dtp instanceof DUSTP)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private static double getMaxLatestEndTime(DisjunctiveTemporalProblem dtp) {
		double let = MAX_LET;
		ArrayList<Interval> minmaxArray = dtp.getDTPBoundaries();
		for (Interval interv : minmaxArray) {
			// Drew: Max seems more appropriate here. Otherwise this ends early, right?
			let = Math.min(let, interv.getUpperBound());
			//let = Math.max(let, interv.getUpperBound());
		}
		return let;
		}

	
	public static boolean requiredActivity(String actname) {
		return (!(actname.equals("skip")) || (actname.equals("idle")));
	}

	private static int getEarliestEnd(
			ArrayList<SimpleEntry<String, Interval>> ongoing) {
		int min = Integer.MAX_VALUE;
		for(SimpleEntry<String,Interval> pair: ongoing){
			int end = (int) pair.getValue().getUpperBound();
			if(end < min) min = end;
		}
		//System.out.println("Would skip to time " + Generics.toTimeFormat(min));
		return min;
	}


	private static void getAndPerformIdle(int minIdle, int maxIdle, int time, int subDTPNum){
		if(time < 0 || time > maxIdle+1){
			System.out.println("Unexpected idle time response \""+Integer.toString(time)+"\"");
			return;
		}
		/// incrementSystemTime(time, currentAgent);
		/// dtp.advanceToTime(-getSystemTime(), time, true);
		
		dtp.advanceSubDTPToTime(-(getSystemTime()+time), time, true, subDTPNum);
		
		dtp.simplifyMinNetIntervals();
	}

/*
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
	private static void advancedSelection(){
		//TODO: some of these option are causing errors in the idle time calculations, needs to be fixed
		printToClient("Select from advanced options:\n(G)et interval\ntighten activity (D)uration\ntighten activity (A)vailability\nadd new inter-activity (C)onstraint\nadd (N)ew activity");
		JSONObject jsonIN = getNextRequest();
		String str = (String) jsonIN.get("value");
		
		//get interval between two timepoints
		if(str.equalsIgnoreCase("G")){
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime()));
			activities.add("zero");
			printToClient("Enter first activity to find interval from: "+ activities.toString());
			String tp1 = getTimepoint(activities,true);
			printToClient("Enter second activity to find interval from: "+ activities.toString());
			String tp2 = getTimepoint(activities,true);
			printToClient("Interval from "+tp1+" to "+tp2+" is "+dtp.getInterval(tp1, tp2));
			return;
		}
		
		//tighten activity duration
		else if(str.equalsIgnoreCase("D")){ 
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARDUR, -getSystemTime()));
			if(activities.isEmpty()){
				printToClient("No activities with variable duration.  Please make another selection.");
				return;
			}
			prevDTP = dtp.clone();
			previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
			printToClient("Select activity to tighten duration for.  Current activities with variable duration: "+activities.toString());
			str = getTimepoint(activities,false);
			IntervalSet interval = dtp.getInterval(str+"_S", str+"_E").inverse();
			getIntervalSetAndAdd(str+"_S",str+"_E",getSystemTime(),"Current duration for "+str+" is: "+interval.toString()+".");
		}
		
		//tighten activity availability
		else if(str.equalsIgnoreCase("A")){ 
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARAVAIL, -getSystemTime()));
			if(activities.isEmpty()){
				printToClient("No activities with variable availability.  Please make another selection.");
				return;
			}
			prevDTP = dtp.clone();
			previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
			printToClient("Select activity to tighten availability for.  Current activities with variable availability: "+activities.toString());
			str = getTimepoint(activities,false);
			IntervalSet start = dtp.getInterval("zero", str+"_S").inverse();
			if(start.totalSize() > 1){
				getIntervalSetAndAdd("zero",str+"_S",getSystemTime(),"Current availability for starting "+str+" is: "+start.toString()+".");
			}
			IntervalSet end = dtp.getInterval("zero", str+"_E").inverse();
			if(end.totalSize() > 1){
				getIntervalSetAndAdd("zero",str+"_E",getSystemTime(),"Current availability for ending "+str+" is: "+end.toString()+".");
			}
		}
		
		//add new interactivity constraint
		else if(str.equalsIgnoreCase("C")){ 
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime()));
			if(activities.isEmpty()){
				printToClient("No activities found.  Please try again.");
				return;
			}
			//prevDTP = dtpClone(dtp, getSystemTime());
			prevDTP = dtp.clone();
			previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
			printToClient("Enter source activity of new constraint.  Activities are: "+activities.toString());
			String source = getTimepoint(activities,true);
			printToClient("Enter destination activity of new constraint.  Activities are: "+activities.toString());
			String dest = getTimepoint(activities,true);
			getIntervalSetAndAdd(source,dest,getSystemTime(),"");
		}
		
		//add new activity
		else if(str.equalsIgnoreCase("N")){ 

			prevDTP = dtp.clone();
			
			Collection<DisjunctiveTemporalConstraint> constrToSave = dtp.getAdditionalConstraints();
			ArrayList<Timepoint> fixedToSave = dtp.getFixedTimepoints();
			SimpleEntry<String, Integer> output = addNewActivity();
			String new_act = output.getKey();
			dtp = new ProblemLoader().loadDTPFromFile(problemFile);
			int id = output.getValue();
			//System.out.println("timepoint added to SUBDTP "+id);
			//add in nonconcurrency constraints (make this so the user can choose??
			Collection<String> tpNames = dtp.getActivityNames(id);
//			
//			for(String tpOne : tpNames){
//					
//					//String tpOne = tpNames.get(w);
//					if(tpOne.equals(new_act)) continue;
//					dtp.addNonconcurrentConstraint(tpOne, new_act, id);
//					//System.out.println("adding nonconcurrency constraint between "+tpOne+" "+new_act);
//				
//			}
			//((SimpleDTP) dtp).addNonconcurrentConstraint("wakeup", new_act);
			//((SimpleDTP) dtp).addOrderingConstraint("wakeup", new_act,0,Integer.MAX_VALUE);
			//((SimpleDTP) dtp).addOrderingConstraint(new_act, "toSchool",0,Integer.MAX_VALUE);
			
			//dtp.enumerateSolutions(0); // right now this and the line below are error checking. ultimately, i don't think they're needed
			//dtp.simplifyMinNetIntervals();
			
			
			dtp.addAdditionalConstraints(constrToSave);
			//System.out.println("readded additional constraints");
			//dtp.addFixedTimepoints(fixedToSave);
			//System.out.println("readded fixed timepoints");
			dtp.enumerateSolutions(getSystemTime());
			dtp.simplifyMinNetIntervals();
			dtp.addFixedTimepoints(fixedToSave);
			//System.out.println("readded fixed timepoints");
			return;
		}
		else{
			printToClient("Unexpected response \""+str+"\"");
			return;
	
		}
		dtp.enumerateSolutions(getSystemTime());
		dtp.simplifyMinNetIntervals();
	
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
	
//	/*
//	 * This class sends a JSON message to the client to print out for the user to see
//	 * Author: Drew
//	 * For now, it will save a JSON file that acts as a queue containing the things
//	 * that the server wants to print. Will be flushed (erased) once the client sends a
//	 * GET request and gets it
//	 */
//	private static void printToClient(String msg) {
//		File tmpDir = new File("JSON_to_client.json");
//		FileWriter fout;
//		try {
//			if (tmpDir.exists()) { // if the file already exists, read in JSON then edit it / add to it
//			
//				JSONParser parser = new JSONParser();
//				Object fileIN = parser.parse(new FileReader("JSON_to_client.json"));
//				JSONObject jsonIN = (JSONObject) fileIN;
//				if (!tmpDir.delete()) { // delete the JSON file after receiving it
//					System.out.println("Error: Failed to delete to_client file when writing.");
//					// if you fail to delete this, do not send anything
//					return;
//				}
//				
//				msg = (String)jsonIN.get("toPrint") + '\n' + msg;
//				
//			}
//			
//			fout = new FileWriter("JSON_to_client.json");
//
//			
//			// JSON will have object {toPrint : someMessage}
//			JSONObject jsonObj = new JSONObject();
//			jsonObj.put("toPrint", msg);
//	
//			System.out.println("Server output: " + msg);
//			fout.write(jsonObj.toJSONString()); // write the JSON to a string to the file
//			
//			
//			fout.close();
//			
//		} catch (Exception e) {
//			
//			System.err.flush();
//		}
//	}
	
	/*
	 * This function deletes the activity from the XML file so that it will be deleted from the user schedule
	 * It also modifies the global dtp and refreshes it
	 */
	private void deleteActFromXML(String delActName) {
		
		File modifiedXML = null;
		String xmlModString = "";
		try {
			File originalXML = new File(problemFile);
			String newXMLFileName = "tempModifiedXML.xml";
			modifiedXML = new File(newXMLFileName);
			modifiedXML.delete();
			Files.copy(originalXML.toPath(), modifiedXML.toPath());
			
		} catch (Exception e) {
			System.err.println("Error is creating new modified XML file.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()) + "\n");
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
	
	
	private void sendCurrentChoicesToAllClients(List<List<String>> acts, ArrayList<ArrayList<String>> minDurs, ArrayList<ArrayList<String>> maxDurs) {
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
		
		// wait until gantt is made, saved, and loaded into system
//		ganttDir = new File("forClient_image.png");
//		String imageString = "";
		//try {
//			while(!ganttDir.exists()) { // while it does not exist
//				Thread.sleep(100);
//				ganttDir = new File("forClient_image.png");
//			}
			
			// once gantt is made and loaded, convert to base64 string
//			FileInputStream fis = new FileInputStream(ganttDir);
//			byte byteArray[] = new byte[(int)ganttDir.length()];
//			fis.read(byteArray);
//			imageString = Base64.encodeBase64String(byteArray);
			
			// clean up stream and delete png after use
//			fis.close();
//			ganttDir.delete();
			
			
			// get count of partially constricted and significantly constricted activities
			// go through each agent, then each activity and check the activity ratio
			double ratioThreshold = 0.66;
			ArrayList< Integer > weakRestrictCounts = new ArrayList(numAgents);
			ArrayList< Integer > strongRestrictCounts = new ArrayList(numAgents);
			int totalWeakCount = 0;
			int totalStrongCount = 0;
			
			// if this is the first choice (no oldPlotData), then fill the restrictCount arrays with 0's
//			if (oldPlotData.size() == 0) {
//				for (int i = 0; i < Integer.valueOf(numAgents); i++) {
//					weakRestrictCounts.add(0);
//					strongRestrictCounts.add(0);
//				}
//			}
//			
//			for (int a = 0; a < oldPlotData.size(); a++) {
//				weakRestrictCounts.add(0);
//				strongRestrictCounts.add(0);
//				for (int i = 0; i < oldPlotData.get(a).get("actRestricts").size(); i++) {
//					double newRatio =  Double.parseDouble( plotData.get(a).get("actRestricts").get(i) );
//					double oldRatio =  Double.parseDouble( oldPlotData.get(a).get("actRestricts").get(i) );
//					double ratioOfRatios = newRatio / oldRatio;
//					
//					// if there has been a change over the threshold, increment strong restriction count
//					if (ratioOfRatios < 0.7) {
//						strongRestrictCounts.set(a, strongRestrictCounts.get(a)+1);
//						totalStrongCount++;
//					} // if there has been a change less than the threshold, increment weak restriction count
//					else if (ratioOfRatios < 1.0) {
//						weakRestrictCounts.set(a, weakRestrictCounts.get(a)+1);
//						totalWeakCount++;
//					}
//				};
//			}
			
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
				

//				System.out.println("totalStrongCount: " + String.valueOf(totalStrongCount));
//				System.out.println("totalWeakCount: " + String.valueOf(totalWeakCount));
//				System.out.println("this agent strong count: " + String.valueOf(strongRestrictCounts.get(Integer.valueOf(agentNum))) );
			}
			
			oldPlotData = plotData;
			
//		} catch (Exception e) {
//			System.err.println("sendGanttToAgent Error: "+e.toString());
//			System.err.flush();
//		}
		
		// delete the gantt image after it has been sent
//		ganttDir.delete();
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

