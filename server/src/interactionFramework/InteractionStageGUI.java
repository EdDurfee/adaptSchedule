package interactionFramework;

import interval.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import stp.TemporalDifference;
import stp.Timepoint;
import util.MultiNode;
import util.Node;


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
	
	//private static String problemFile = "stn1.xml";
	private static boolean PREEMPTIVE = false;
	private static boolean CONCURRENT = false;
	private static boolean SPORADIC = false;
	private static boolean SPORADIC_OCCURRED = false;
	private static int SP_DUR = 1;
	private static int MAX_LET = 1440; // End of the day's schedule
	//private static String problemFile = "MABreakfastSplit.xml";
	private static String problemFile = "multiagentSampleProblem_simp.xml";
	//private static String problemFile = "multiagentSampleProblemRev.xml";
	// Drew - If you can see this then it has been committed
	//private static String problemFile = "toyexample.xml";
	//private static String problemFile = "DTPtoyexample.xml";
	//private static String problemFile = "DUTPtoyexampleNotDC.xml";
	//private static String problemFile = "DUTPtoyexampleDC.xml";
	//private static String problemFile = "DUTPtoyexampleNoSE.xml";
	private static final Interval zeroInterval = new Interval(0,0);
	private static DisjunctiveTemporalProblem dtp;
	private static DisjunctiveTemporalProblem prevDTP = null;
	private static DisjunctiveTemporalProblem initialDTP;
	private static List<Integer> systemTime; //systemTime[i] is the current system time for agent i
	private static List<Integer> prevSystemTime;
	private static String last_activity = "";
	private static int numAgents;
	private static int currentAgent = 0;  //currentAgent = [0,numAgents], currentAgent = numAgents means to control the system clock (i.e. all agents)
	private static Scanner cin = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
	private static final int DEBUG = -1;
	private static ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> > previousDTPs;
	private static int revision = 0;
	private static int skip_time = -1;
	//need a data structure to keep track of activities currently being performed and their intervals
	private static ArrayList<SimpleEntry<String, Interval>> ongoingActs = new ArrayList<SimpleEntry<String,Interval>>();
	
	private HashSet<String> confirmedActSet = new HashSet<String>();
	private HashSet<String> currentActSet = new HashSet<String>();
	
	public LinkedBlockingQueue<JSONObject> toClientQueue    = new LinkedBlockingQueue<JSONObject>();
	public LinkedBlockingQueue<JSONObject> fromClientQueue  = new LinkedBlockingQueue<JSONObject>();
	
	public int thisServInstNum = 0;
	public boolean processingStartup = false;
	public boolean processingTentativeAct = false;
	public boolean processingConfirmedAct = false;
//	public String  processingAgentNum     = "";
	public boolean readyForNextReq = true;
	String agentNum = "";
	String actName = "";
	String actDur = "";
	JSONObject inJSON;
	
	
	public InteractionStageGUI(int servNum) {
		thisServInstNum = servNum;
	}
	
	
	
	@SuppressWarnings("unused")
	public void run(){

		System.out.println("new server instanced");
		
///		removed the problem input for the GUI version
//		printToClient("New session created. (Enter HELP at any point for advanced system control options)\n");
//		printToClient("Which problem would you like to run?");
//		printToClient("(multiagentSampleProblem, DTPtoyexample, toyExampleEd, etc.)");

		File tmpDir = new File(problemFile);
		if (!tmpDir.exists()) {
			System.out.println("Failed to load file. Double check file?");
			return;
		}
		dtp = new ProblemLoader().loadDTPFromFile(problemFile);
		System.out.println("Problem loaded"); // file loaded correctly

		
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);	
		dtp.simplifyMinNetIntervals();
		
		MAX_LET = (int) Math.ceil(getMaxLatestEndTime(dtp));
		// Drew - Trying to temp quick patch a bug
		MAX_LET = 1440;
				
		initialDTP = dtp.clone();
		numAgents = dtp.getNumAgents();
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		previousDTPs = new ArrayList< Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> >(numAgents);
		for(int i = 0; i < numAgents; i++) previousDTPs.add( new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>() );
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		//dtp.printConstraints(Generics.getLogStream());
		if(numAgents > 1){
//			getAgentSelection();
			currentAgent = 2;
			dtp.setCurrentAgent(currentAgent);
		}
		
		
		//Generics.printDTP(dtp);
		initialDTP = dtp.clone();
		
		JSONObject jsonIN = new JSONObject();
		
		
		boolean flag = true;
		while(flag){
			try{
				//dtp.getDTPBoundaries();
				CONCURRENT = false;
				if(DEBUG >= 2) dtp.printSelectionStack();
				if(DEBUG >= 1){
					Generics.printDTP(dtp);
					System.out.println("Calls to solver" +dtp.getCallsToSolver());
				} if (DEBUG == 0){ // Print Deltas mode
					if(prevDTP == null) Generics.printDTP(dtp);
					else dtp.printDeltas(dtp.getDeltas(prevDTP));
				}

				int minTime = dtp.getMinTime(); // gets universal minTime for all agents
				minTime = Math.min(minTime, MAX_LET);

				//ED: I revised the below so that it does NOT automatically move forward until the next
				// required activity.  That breaks at end of day, when it could offer chance to idle (and
				// express that a sporadic activity has occurred)
				if(getSystemTime() >= MAX_LET){
					flag = false;
					return;
				}
				else if(minTime > getSystemTime() && minTime < MAX_LET){
					int temp = minTime - getSystemTime();
					advanceSystemTimeTo(minTime);
					dtp.advanceToTime(-getSystemTime(), temp, false);
					dtp.simplifyMinNetIntervals();
					continue;
				}
				else if(minTime < getSystemTime()){
					System.out.println("Should be error for minTime thing. Ignoring in this version.");	
				}

				Class cls = dtp.getClass();
			    
				/*
				//capability for observing and updating problem when SEs are present
				//TODO: This enumerates the known cases of sporadic TP.  Is there a more extensible approach?
				if((disjunctivelyUncertainProblem(dtp)) && !SPORADIC_OCCURRED){
					processSporadicEvent(last_activity);
					if(SPORADIC_OCCURRED) {
						// If we are here, then we just processed the sporadic event
						// go back to the start of this loop to recompute the system time, etc.
						continue;
					}
				}
				*/
				
				//Prompt user for their activity selection
				List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime()); // returns the activities of only the currentAgent
				List<List<String>> allRemActivities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime());
				List<Integer> maxSlack = dtp.getMaxSlack(); // list of max slack for each agent at its corresponding index
				
				//Update ongoing activities data struct
				ArrayList<Integer> toRemove = new ArrayList<Integer>();
				for(int i = 0; i < ongoingActs.size(); i++){
					SimpleEntry<String, Interval> pair = ongoingActs.get(i);
					Interval curr = pair.getValue();
					int time = getSystemTime();
					//System.out.println("current interval: " + curr.getUpperBound() + " time: " + time);
					if(time >= curr.getUpperBound()) toRemove.add(i);
				}
				
				for(int j : toRemove) ongoingActs.remove(j);
				//System.out.println("after removal: " + ongoingActs);
				
				if(maxSlack.size() == 1 && maxSlack.get(0) > 0) activities.get(0).add("idle"); // if only 1 agent
				else for(int agent = 0; agent < maxSlack.size(); agent++){
					//agent can only idle if it has available slack and needs to currently make a selection
					if(maxSlack.get(agent) > 0 && getSystemTime(agent) == getSystemTime()) activities.get(agent).add("idle"); //agent"+agent+"Idle"); 
				}
				
				/*
				// we want to add an option to skip to the end of the earliest ending activity in the ongoing acts list.
				if(ongoingActs.size() > 0){
					// get the earliest end time of the activities
					int eet = getEarliestEnd(ongoingActs);
					skip_time = eet - getSystemTime();
					activities.get(0).add("skip");
				}
				*/
				
				// create 2d vector of min and max durations of next possible activities
				// first index is agent num, second index is act ind that coordinates to activities vector
				ArrayList<ArrayList<String>> nextActsMinDur = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) nextActsMinDur.add(new ArrayList<String>());
				ArrayList<ArrayList<String>> nextActsMaxDur = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) nextActsMaxDur.add(new ArrayList<String>());
				
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
					}
				}		
						
				// create 2d vector of min and max durations of all remaining activities
				// allRemActivities should never include idle
				// first index is agent num, second index is act ind that coordinates to activities vector
				ArrayList<ArrayList<String>> remMinDurs = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) remMinDurs.add(new ArrayList<String>());
				ArrayList<ArrayList<String>> remMaxDurs = new ArrayList<ArrayList<String>>(); for (int i=0;i<numAgents;i++) remMaxDurs.add(new ArrayList<String>());

				for (int agent = 0; agent < numAgents; agent++) {
					for (int act = 0; act < allRemActivities.get(agent).size(); act++) {
						IntervalSet interval = dtp.getInterval(allRemActivities.get(agent).get(act)+"_S", allRemActivities.get(agent).get(act)+"_E").inverse().subtract(zeroInterval);
						remMinDurs.get(agent).add(String.valueOf( (int) interval.getLowerBound()));
						remMaxDurs.get(agent).add(String.valueOf( (int) interval.getUpperBound()));
					}
				}
				
				
				
				if (processingConfirmedAct || processingTentativeAct || processingStartup) {
					
					// if this is not a tentative activity selection (AKA it is a confirmedActivity)
					if (processingConfirmedAct || processingStartup) {
						
						// inform the client of all activity options, including idle
						sendJSONToClient(
								"currentChoices", // infoType
								"TODO", // startTime
								"", // lastActivity
								activities.get(Integer.valueOf(agentNum)), // nextActivities of currentAgent
								nextActsMinDur.get(Integer.valueOf(agentNum)), // nextActsMinDur
								nextActsMaxDur.get(Integer.valueOf(agentNum)), // nextActsMaxDur
								new ArrayList<String>(), // remActivities
								new ArrayList<String>(), // remMinDurs
								new ArrayList<String>(), // remMaxDurs
								new ArrayList<String>(), // remMinStarts
								new ArrayList<String>(), // remMaxEnds
								new ArrayList<String>()  // debugInfo
								);
						
						// done processing confirmedAct
						processingConfirmedAct = false;
						processingStartup = false;
//						processingAgentNum = "";
					}
					
					// create visual png of DTP with current state
	//					Viz.printDTPDiagram(dtp, initialDTP, getSystemTime(),1);
					Viz.createAndSaveDTPDiagram(dtp, initialDTP, getSystemTime(),1);
					
					// prep client for gantt image
					// actual image will be sent on next get request
					sendJSONToClient(
							"ganttImage", // infoType
							"", // startTime
							"", // lastActivity
							new ArrayList<String>(), // nextActivities
							new ArrayList<String>(), // nextActsMinDur
							new ArrayList<String>(), // nextActsMaxDur
							new ArrayList<String>(), // remActivities
							new ArrayList<String>(), // remMinDurs
							new ArrayList<String>(), // remMaxDurs
							new ArrayList<String>(), // remMinStarts
							new ArrayList<String>(), // remMaxEnds
							new ArrayList<String>()  // debugInfo
							);
					
					// if this is a tentative activity
					if (processingTentativeAct == true) {
						removeTentAct(agentNum);
						processingTentativeAct = false;
//						processingAgentNum = "";
						continue; // need to go back to top to get a full undo / removal of tentative activity
					}
				}
			

				// wait for the client to send a POST request
				inJSON = getNextRequest();
				
				agentNum = String.valueOf(inJSON.get("agentNum"));
				actName = String.valueOf(inJSON.get("activityName"));
				actDur = String.valueOf(inJSON.get("activityDuration"));
				
				
				switch ( (String) inJSON.get("infoType")) {

					case "startup":
						processingStartup = true;
						break;
						
					case "confirmActivity":
					case "tentativeActivity":
						prevDTP = dtp.clone();
						previousDTPs.get(Integer.valueOf(agentNum)).push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
						
						
						if (actName.equals("idle")) {
							getAndPerformIdle(minTime-getSystemTime(), maxSlack.get(Integer.valueOf(agentNum)), Integer.valueOf(actDur.substring(3)));
							
						} else {
							
							IntervalSet endTime = dtp.getInterval("zero", actName+"_E");
							int time = Generics.fromTimeFormat( actDur ); // actDur always comes in minutes
							
							IntervalSet interval = dtp.getInterval(actName+"_S", actName+"_E").inverse().subtract(zeroInterval);
							if(!interval.intersect(time)){
								System.out.println("Unexpected duration response \""+actName+"\"");
								break;
							}
							
							int idle = (int) (-endTime.getUpperBound() - getSystemTime() - time);
							if(idle > 0){	
								// force immediate idle for "+Generics.toTimeFormat(idle)+"
								incrementSystemTime(idle, Integer.valueOf(agentNum));
							}

							Interval curr_int = new Interval(getSystemTime(), getSystemTime() + time);
							ongoingActs.add(new SimpleEntry<String,Interval>(actName, curr_int));
							
							dtp.executeAndAdvance(-getSystemTime(), actName+"_S",-(getSystemTime()+time),actName+"_E",true, time, true);
							
							dtp.simplifyMinNetIntervals();
							// The above will move the systemTime to the end of day after the last activity, rather than
							// leaving the possibility that there could be something after the last required activity, like
							// processing a sporadic event, or simply choosing to idle out the rest of the day
							int new_time =  time;
							incrementSystemTime(new_time, Integer.valueOf(agentNum));
						}
						
						if ( ((String) inJSON.get("infoType")).equals("tentativeActivity")) { processingTentativeAct = true; }
						if ( ((String) inJSON.get("infoType")).equals("confirmActivity"))   { processingConfirmedAct = true; }
						
						break;
						
					
					default:
						break;
					
				}
				
			} catch (Exception e) {
				System.err.println("Error.  Please try again.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()));
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
	
/*
	private static void processSporadicEvent(String last_act) {
		if(last_act.length() == 0){
			printToClient("No activities have been performed yet...");
			return;
		} else if ((DUTP.SporadicEventLT < prevSystemTime.get(currentAgent)) || (DUTP.SporadicEventET >= systemTime.get(currentAgent))) {
			printToClient("Sporadic event couldn't have happened since last check in time.");
			return;
		}
		printToClient("Did a sporadic event occur during the previous activity or since the last check in time? (y/n)");
		JSONObject jsonIN = getNextRequest();
		String resp = (String) jsonIN.get("value");
		
		if(resp.equalsIgnoreCase("y")){
			SPORADIC_OCCURRED = true;
			printToClient("At what time did the sporadic event happen? ");
			jsonIN = getNextRequest();
			String time = (String) jsonIN.get("value");
			if(true) {
				String sename = DUTP.SPORADICNAME;
				int se_time = Generics.fromTimeFormat(time);
				printToClient("Inserting sporadic activity at end of previous activity, for duration " + DUTP.DUR);
				dtp.executeAndAdvance(-getSystemTime(), sename+"_S", -(getSystemTime()+SP_DUR), sename+"_E",true, DUTP.DUR, true);
				advanceSystemTimeTo(getSystemTime()+DUTP.DUR);
				dtp.advanceToTime(-getSystemTime(), DUTP.DUR, false);
				dtp.simplifyMinNetIntervals();
				return;
				}
			

			// ED: The below is old code from Lynn, intended to split the ongoing activity and insert the
			//  sporadic activity into its place, kinda - some stuff was hardwired kindof funny.  So, not
			//  doing this for now, and just always pasting the sporadic activity at the end of the activity
			//  the sporadic event occurred within
			/// Drew: ^^ I deleted all the code this comment references to in a clean up attempt, as I was unlikely to reimplement it.

		}else {  //if(resp.equalsIgnoreCase("n")){
			// ED: This had simply returned, but that doesn't seem right.
			// If it didn't occur, it seems like we need to branch down the leftside of the DTrees for
			// each of the DUSTPs, to rule out the iSTPs where the SE would have occurred during the
			// most recent activity, as long as that activity was a required activity (not idle)

			// For each of the DUTP's DUSTPs, we know the sporadic event did NOT occur, so we want
			// to skip over it, removing the iSTP where the sporadic occurred during the previous activity
			// if there was a previous activity (other than idle)
			if (requiredActivity(last_activity)) {
				printToClient("No sporadic and a required activity.");
				dtp.advanceDownTree();
//				System.out.println("Advancing done. Type in anything to continue.");
//				String resp1 = cin.next();
			}
			return;
		}
		
	}
*/
	
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

/*
	private static void performPreemptive(String str) {
		if(str.equalsIgnoreCase("y")){
			printToClient("Performing Preemptive activity for 20 minutes.");
			PREEMPTIVE = true;
			incrementSystemTime(20, currentAgent);
			dtp.advanceToTime(-getSystemTime(), 20, true);
			dtp.simplifyMinNetIntervals();
		
			printToClient("You have prevented an emergency activity from happening before your morning class.");
		} else if(str.equalsIgnoreCase("n")){
			printToClient("You have chosen not to perform a preemptive activity at this time.");
			return;
		}
	}
*/

	private static void getAndPerformIdle(int minIdle, int maxIdle, int time){
		if(time < 0 || time > maxIdle+1){
			System.out.println("Unexpected idle time response \""+Integer.toString(time)+"\"");
			return;
		}
		incrementSystemTime(time, currentAgent);
		dtp.advanceToTime(-getSystemTime(), time, true);
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
	private static void printRequestedOutput(String option){
		if(option.equalsIgnoreCase("D")){
			if(prevDTP == null) printToClient("You have not yet made a choice or have just undone a choice.");
			else dtp.printDeltas(dtp.getDeltas(previousDTPs.peek().getValue()));
		}else if(option.equalsIgnoreCase("C")){
			Generics.printDTP(dtp);
		} else if(option.equalsIgnoreCase("V")){
			Viz.printDTPDiagram(dtp, initialDTP, getSystemTime(),1);
		}else if(option.equalsIgnoreCase("B")){
			dtp.checkBookends(20);
		}
		
		else{
			printToClient("Unexpected response \"" + option + "\"");
		}
		return;
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
	 * ui helper code to prompt a user to input an intervalset
	 * also generates a disjunctivetemporalconstraint from that intervalset and adds it to the dtp
	 * @param tpS
	 * @param tpE
	 * @param time
	 * @param msg
	 */
/*	private static void getIntervalSetAndAdd(String tpS, String tpE, int time, String msg){
		printToClient(msg+"\nEnter new interval set or (n) to leave unchanged. Format {[interval1]v[interval2]v...} with no whitespace.");
		JSONObject jsonIN = getNextRequest();
		String avail = (String) jsonIN.get("value");
		String avail = cin.next(); // Commented out by Drew
		if(!avail.equalsIgnoreCase("N")){
			IntervalSet is = Generics.stringToInterval(avail);
			dtp.addAdditionalConstraint(tpS, tpE, is, time, true, true);	
			dtp.simplifyMinNetIntervals();
		}		
	}
*/
	
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
	
	@SuppressWarnings("unchecked")
	private void sendJSONToClient(
			String infoType, String startTime, String lastActivity, List<String> nextActivities,
			List<String> nextActsMinDur, List<String> nextActsMaxDur, List<String> remActivities,
			List<String> remMinDurs, List<String> remMaxDurs, List<String> remMinStarts,
			List<String> remMaxEnds, List<String> debugInfo) {
		
		System.out.println("Server #" + String.valueOf(thisServInstNum) + " Sending '"+infoType+"' JSON to client");
		
		JSONObject outJSON = new JSONObject();
		outJSON.put("infoType", infoType);
		outJSON.put("startTime", startTime);
		outJSON.put("lastActivity", lastActivity);
		outJSON.put("nextActivities", nextActivities);
		outJSON.put("nextActsMinDur", nextActsMinDur);
		outJSON.put("nextActsMaxDur", nextActsMaxDur);
		outJSON.put("remActivities", remActivities);
		outJSON.put("remMinDurs", remMinDurs);
		outJSON.put("remMaxDurs", remMaxDurs);
		outJSON.put("remMinStarts", remMinStarts);
		outJSON.put("remMaxEnds", remMaxEnds);
		outJSON.put("debugInfo", debugInfo);
		
		try {
			toClientQueue.put(outJSON);
		} catch (Exception e) {
			System.err.println("Error while adding to toClientQueue");
			System.err.flush();
		}
		
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
					break;			
				}
			}
		} catch (Exception e) {
			System.err.println("Error while waiting in getNextRequest()");
			System.err.println(e);
			System.err.flush();
		}
		return jsonIN;
			
//			try {
//				// the server thread will be constantly running in the background looking for
//				// requests from the client
//				// Once it receives one, it will save the JSON as a file called JSON_from_client.json
//				// wait until the server gets a response from the client before continuing
//				File tmpDir = new File("JSON_from_client.json");
//				while( !tmpDir.exists() ) {
//					Thread.sleep(100);
//					tmpDir = new File("JSON_from_client.json");
//				}
//				if (tmpDir.length() == 0) {
//					tmpDir.delete();
//					continue;
//				}
//				
//				JSONParser parser = new JSONParser();
//				Object fileIN = parser.parse(new FileReader("JSON_from_client.json"));
//				if (!tmpDir.delete()) { // delete the JSON file after receiving it
//					System.out.println("Error: Failed to delete file.");
//				}
//				
//				jsonIN = (JSONObject) fileIN;
//				
//			} catch (Exception e) {
//				System.err.println("Error while waiting in getNextRequest()");
//				System.err.println(e);
//				System.err.flush();
//			}
//			
//			if (!jsonIN.isEmpty()) {
//				// if (jsonIN.get("value").equals("RESTART")) {Thread.currentThread().interrupt();} // no longer needed with GUI interface
//				System.out.println("User command type: " + (String)jsonIN.get("infoType"));
//				return jsonIN;
//			}
//		}
		
	}
	
}

