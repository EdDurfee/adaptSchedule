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
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
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
public class InteractionStageDrew implements Runnable {
	private boolean textUI = false;
	
	//private static String problemFile = "stn1.xml";
	private static boolean PREEMPTIVE = false;
	private static boolean CONCURRENT = false;
	private static boolean SPORADIC = false;
	private static boolean SPORADIC_OCCURRED = false;
	private static int SP_DUR = 1;
	private static int MAX_LET = 1440; // End of the day's schedule
	//private static String problemFile = "MABreakfastSplit.xml";
	private static String problemFile = "multiagentSampleProblem.xml";
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
	private static Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> previousDTPs = new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>();
	private static int revision = 0;
	private static int skip_time = -1;
	//need a data structure to keep track of activities currently being performed and their intervals
	private static ArrayList<SimpleEntry<String, Interval>> ongoingActs = new ArrayList<SimpleEntry<String,Interval>>();
	
	
	@SuppressWarnings("unused")
	public void run(){
		
		JSONObject jsonIN = new JSONObject();
		System.out.println("new server instanced");
		
		
		printToClient("New session created. (Enter HELP at any point for advanced system control options)\n");
		printToClient("Which problem would you like to run?");
		printToClient("(multiagentSampleProblem, DTPtoyexample, toyExampleEd, etc.)");
		jsonIN = getNextRequest();
		problemFile = (String) jsonIN.get("value");
		File tmpDir = new File(problemFile + ".xml");
		if (!tmpDir.exists()) {
			printToClient("Failed to load file. Double check name?");
			return;
		}
		dtp = new ProblemLoader().loadDTPFromFile(problemFile + ".xml");
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
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		//dtp.printConstraints(Generics.getLogStream());
		if(numAgents > 1){
			getAgentSelection();
		}
		
		
		//Generics.printDTP(dtp);
		initialDTP = dtp.clone();
		
		
		
		boolean flag = true;
		while(flag){
			try{
				//dtp.getDTPBoundaries();
				CONCURRENT = false;
				if(DEBUG >= 2) dtp.printSelectionStack();
				if(DEBUG >= 1){
//					System.out.println("SystemTime: "+systemTime.toString());
					printToClient("SystemTime: "+systemTime.toString());
					Generics.printDTP(dtp);
					//System.out.println("Calls to solver" +dtp.getCallsToSolver());
				} if (DEBUG == 0){ // Print Deltas mode
					if(prevDTP == null) Generics.printDTP(dtp);
					else dtp.printDeltas(dtp.getDeltas(prevDTP));
				}
//				System.out.println("\nCurrent Time: "+Generics.toTimeFormat(getSystemTime()));
				printToClient("\nCurrent Time: "+Generics.toTimeFormat(getSystemTime()));
				//Viz.printDTPDiagram(dtp, initialDTP, getSystemTime(),1);
				int minTime = dtp.getMinTime();
//				System.out.println("MinTime is " + minTime);
				minTime = Math.min(minTime, MAX_LET);
//				System.out.println("MinTime is " + minTime);
//				System.out.println("Type anything to continue.");
//				String resp3 = cin.next();
				//ED: I revised the below so that it does NOT automatically move forward until the next
				// required activity.  That breaks at end of day, when it could offer chance to idle (and
				// express that a sporadic activity has occurred)
				if(getSystemTime() >= MAX_LET){
//					System.out.println("End of day.");
					printToClient("End of day.");
//					Generics.printDTP(prevDTP);
//					Generics.printDTP(dtp);
					flag = false;
					return;
				}
				else if(minTime > getSystemTime() && minTime < MAX_LET){
//					System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
					printToClient("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
					int temp = minTime - getSystemTime();
					advanceSystemTimeTo(minTime);
					dtp.advanceToTime(-getSystemTime(), temp, false);
					dtp.simplifyMinNetIntervals();
					continue;
				}
				else if(minTime < getSystemTime()){
					System.out.println("Should be error? for minTime thing");
					//throw new Error("minTime("+minTime+") < systemTime("+systemTime+")");		
				}

				Class cls = dtp.getClass();
//			    System.out.println("The type of the DTP is: " + cls.getName());
			    
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
				
				//Prompt user for their activity selection	
//				System.out.println("About to get activities. Type in anything to continue.");
//				String resp1 = cin.next();
				List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime());
				List<Integer> maxSlack = dtp.getMaxSlack();
//				System.out.println("Got activities. Type in anything to continue.");
//				String resp2 = cin.next();
				
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
				
				String ongoing_str = "";
				if(ongoingActs.size() > 0){
					ongoing_str = "Ongoing activities are: ";
					for (SimpleEntry<String,Interval> pair : ongoingActs){
						String temp = "";
						temp = temp + pair.getKey() + " ending at " + Generics.toTimeFormat((int) pair.getValue().getUpperBound());
						ongoing_str = ongoing_str + " " + temp +";";
					}
				}
				
				
				if(maxSlack.size() == 1 && maxSlack.get(0) > 0) activities.get(0).add("idle");
				else for(int i = 0; i < maxSlack.size(); i++){
					//agent can only idle if it has available slack and needs to currently make a selection
					if(maxSlack.get(i) > 0 && getSystemTime(i) == getSystemTime()) activities.get(i).add("agent"+i+"Idle"); 
				}
				
				// we want to add an option to skip to the end of the earliest ending activity in the ongoing acts list.
				if(ongoingActs.size() > 0){
					// get the earliest end time of the activities
					int eet = getEarliestEnd(ongoingActs);
					skip_time = eet - getSystemTime();
					activities.get(0).add("skip");
				}
				//System.out.println("Select activity for agent " + currentAgent+" to perform: " + activities.get(0).toString() + " unless a (S)poradic event occurred, or (O)utput schedule information, (U)ndo last selection, (C)hange agent, or (A)dvanced selection.");
//				System.out.println(ongoing_str);
				printToClient(ongoing_str);
				if(numAgents > 1 && currentAgent != numAgents) {
					if (textUI) printToClient("Select activity for agent " + currentAgent+" to perform: " + activities.get(0).toString() + " or (O)utput schedule information, (U)ndo last selection, (C)hange agent, or (A)dvanced selection.");
					else {
					printToClient("Select activity for agent " + currentAgent+" to perform:\n" + listAgentActivities(activities,0));
					//printToClient("Or select a schedule option:\n(A)dvanced selection\n(U)ndo last selection\n(C)hange agent\n(O)utput schedule information");
					//printToClient("Or select a schedule option:\n(U)ndo last selection\n(V)isualize schedule");
					}
				} else if(numAgents > 1){
					if (textUI) printToClient("(A)dvanced selection, (U)ndo last selection, (S)witch agent, (O)utput schedule information, or select activity to perform: ");
					//printToClient("Or select a schedule option:\n");
					//printToClient("(A)dvanced selection\n(U)ndo last selection\n(S)witch agent\n(O)utput schedule information\nOr select activity to perform:\n");
					else printToClient("Select activity to perform:");
					for(int i = 0; i < activities.size(); i++){
//						System.out.println("Agent "+i+": "+activities.get(i).toString());
						printToClient("Agent "+i+": "+listAgentActivities(activities,i).toString());
					}
					//if (textUI == false) printToClient("Or select a schedule option:\n(U)ndo last selection\n(V)isualize schedule");
				}
				else {
					if(maxSlack.get(0) >= 20 && !PREEMPTIVE){
						if (textUI) printToClient("Select activity to perform: "+activities.get(0).toString() + " or (A)dvanced selection, (U)ndo last selection, (O)utput schedule information, (P)erform preemptive activity, or (E)mergency idle.");
						else {
							printToClient("Select activity to perform:\n"+listAgentActivities(activities,0));
	//						printToClient("Or select a schedule option:\n(A)dvanced selection\n(U)ndo last selection\n(O)utput schedule information\n(P)erform preemptive activity\n(E)mergency idle");
							//printToClient("Or select a schedule option:\n(U)ndo last selection\n(V)isualize schedule\n(P)erform preemptive activity");
						}
					}else{
						if (textUI) printToClient("Select activity to perform: "+activities.get(0).toString() + " or (A)dvanced selection, (U)ndo last selection, (O)utput schedule information, or (E)mergency Idle.");
						else {
							printToClient("Select activity to perform:\n"+listAgentActivities(activities,0));
	//						printToClient("Or select a schedule option:\n(A)dvanced selection\n(U)ndo last selection\n(O)utput schedule information\n(E)mergency Idle");
							//printToClient("Or select a schedule option:\n(U)ndo last selection\n(V)isualize schedule");
						}
					}
				}
				
				jsonIN = getNextRequest();
				String str = (String) jsonIN.get("value");
				
				
//				//TODO: look at selection stack stuff w.r.t. multiple agents and switching between them
//				else if(str.equalsIgnoreCase("U")){
				if(str.equalsIgnoreCase("U")){
					if(previousDTPs.empty()) {
//						System.out.println("The schedule has not yet been modified.\n");
						printToClient("The schedule has not yet been modified.\n");
						continue;
					}
//					System.out.println("Undoing last selection.");
					printToClient("Undoing last selection.");
					
					SimpleEntry<Integer, DisjunctiveTemporalProblem> ent = previousDTPs.pop();
					
					setSystemTime(ent.getKey());
//					System.out.println("Resetting time to "+Generics.toTimeFormat(getSystemTime()));
									//dtp = dtpClone(prevDTP, getSystemTime());
					printToClient("Resetting time to "+Generics.toTimeFormat(getSystemTime()));
					dtp = ent.getValue();
					
					continue;
				}
				else if(str.equalsIgnoreCase("V")){
//					System.out.println("Output (C)urrent DTP, (V)isualize DTP, (B)ookends between activities or (D)eltas between current and previous DTP?");
//					printToClient("Output (C)urrent DTP\n(V)isualize DTP\n(B)ookends between activities\n(D)eltas between current and previous DTP");
//					/*String option = cin.next();*/ // Commented out by Drew
//					jsonIN = getNextRequest();
//					String option = (String) jsonIN.get("value");
//					printRequestedOutput(option);
					printRequestedOutput("V");
					continue;
				}
				
				
				
//				else if(str.equalsIgnoreCase("P")){
//					//perform preemptive activity. 
//					// in theory this would "get rid of" the option of an emergent activity coming up, but this is only applicable 
//					// in the exhaustiveDTP case.
////					System.out.println("The first bookend in your day is toCampus which must be completed from 9:00 to 9:30. Would you like to perform a preemptive activity now? (Y/N)");
//					printToClient("The first bookend in your day is toCampus which must be completed from 9:00 to 9:30. Would you like to perform a preemptive activity now? (Y/N)");
//					/*str = cin.next();*/ // commented out by Drew
//					jsonIN = getNextRequest();
//					str = (String) jsonIN.get("value");
//					performPreemptive(str);
//					continue;
//				}
//
//				else if(str.equalsIgnoreCase("E")){
//					// perform emergency idle
//					//needs to not break the interaction even if it breaks the schedule.
//					getAndPerformEmergencyIdle(str);
//					continue;
//				}
				
//				else if(str.equalsIgnoreCase("S")){
//					System.out.println("When did the sporadic event begin?");
//					String option = cin.next();
//					int start = Generics.fromTimeFormat(option);
//					int end = start + 20;
//					String etime = Generics.toTimeFormat(end);
//					int end_act = end+5;
//   				String eact = Generics.toTimeFormat(end_act);
//					System.out.println("Inserting sporadic event from " + option + " to " + etime + "\n");
//					System.out.println("Current time is: " + eact);
//					
//					System.out.println("Select activity for agent " + currentAgent+" to perform: [dressing, orgTimeM] or (O)utput schedule information, (U)ndo last selection, (C)hange agent, or (A)dvanced selection.");
//					continue;
//				}
				
				else if(!Generics.concat(activities).contains(str)){
//					System.out.println("Unsupported response \""+str+"\"");
					printToClient("Unsupported response \""+str+"\"");
					continue;
				}
				int time;
				if((str.equals("idle") && currentAgent != numAgents) || str.matches("^agent\\d+Idle$")){
					//prevDTP = dtpClone(dtp, getSystemTime());
					prevDTP = dtp.clone();
					int t = getSystemTime();
					previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(t, prevDTP));
					prevSystemTime.clear();
					for(Integer i: systemTime) prevSystemTime.add(i);
					
					if(currentAgent == numAgents){
						int idleIdx = Integer.parseInt(str.substring(5, str.length()-4));
						dtp.setCurrentAgent(idleIdx);
						currentAgent = idleIdx;
						last_activity = "idle";
						getAndPerformIdle(str, minTime - getSystemTime(), maxSlack.get(idleIdx));
						dtp.setCurrentAgent(numAgents);
						currentAgent = numAgents;
					}
					else{
						last_activity = "idle";
						getAndPerformIdle(str, minTime-getSystemTime(), maxSlack.get(0));
					}
					continue;
				}
				if(str.equals("skip")){
					prevDTP = dtp.clone();
					int t = getSystemTime();
					previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(t, prevDTP));
					prevSystemTime.clear();
					for(Integer i: systemTime) prevSystemTime.add(i);
					last_activity = "skip";
//					System.out.println("Option will advance the clock to " + Generics.toTimeFormat(getSystemTime() + skip_time));
					printToClient("Option will advance the clock to " + Generics.toTimeFormat(getSystemTime() + skip_time));
					incrementSystemTime(skip_time, currentAgent);
					dtp.advanceToTime(-getSystemTime(), skip_time, true);
					dtp.simplifyMinNetIntervals();
							
				}
				else{ //performing an activity
					last_activity = str;
					prevDTP = dtp.clone();
					previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
					prevSystemTime.clear();
					for(Integer i: systemTime) prevSystemTime.add(i);
					//TODO: make sure that this ^^ handles the second case below where it asks if you're cool w the immediate idle.
					IntervalSet interval = dtp.getInterval(str+"_S", str+"_E").inverse().subtract(zeroInterval);
					int agent = dtp.getAgent(str+"_S");
//					System.out.println("Interval totalSize is "+interval.totalSize());
//					printToClient("Interval totalSize is "+interval.totalSize());
					if(interval.totalSize() != 1){
						boolean durationFlag = true;
						do{
							printToClient("How long to perform "+str+"? Possible Options "+interval.toString());
							/*time = Generics.fromTimeFormat(cin.next());*/ // Commented out by Drew
							jsonIN = getNextRequest();
							time = Generics.fromTimeFormat( (String) jsonIN.get("value") );
							if(!interval.intersect(time)){
								printToClient("Unexpected response \""+str+"\"");
								continue;
							}
							
							IntervalSet endTime = dtp.getInterval("zero", str+"_E");
							int idle = (int) (-endTime.getUpperBound() - getSystemTime() - time);
							if(idle > 0){	
								printToClient("Option will force immediate idle for "+Generics.toTimeFormat(idle)+". Continue (y/n)?");
								/*String temp = cin.next();*/ // Commented out by Drew
								jsonIN = getNextRequest();
								String temp = (String) jsonIN.get("value");
								if(temp.equalsIgnoreCase("y")){
									incrementSystemTime(idle, agent);
//									System.out.println("Inserting idle for "+Generics.toTimeFormat(idle)+".");
//									System.out.println("CurrentTime: "+Generics.toTimeFormat(getSystemTime()));
									durationFlag = false;
								}
								else if(!temp.equalsIgnoreCase("n")){
									printToClient("Unexpected response "+temp+"\n");
								}
							}
							else{
								durationFlag = false;
							}
						}while(durationFlag);
						printToClient("Performing "+str+" for "+Generics.toTimeFormat(time));
					}	
					else{
						printToClient("Performing "+str+" for "+interval.toString());
						time = (int) interval.getLowerBound();
					}
					// before executeandadvance we want to add the activity we're performing to our list. 
					
					Interval curr_int = new Interval(getSystemTime(), getSystemTime() + time);
					ongoingActs.add(new SimpleEntry<String,Interval>(str, curr_int));
					//System.out.println(ongoingActs);
//					System.out.println("Starting executeAndAdvance");
//					System.out.println("Type anything to continue.");
					//String resp = cin.next();
					dtp.executeAndAdvance(-getSystemTime(), str+"_S",-(getSystemTime()+time),str+"_E",true, time, true);
//					System.out.println("Finished executeAndAdvance");					
//					System.out.println("System time is: " + getSystemTime());
					//Here we want to check to see if there are any concurrent activities
					if(CONCURRENT){
						activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -prevSystemTime.get(agent));
						printToClient("Concurrent activities are: " + activities);
						printToClient("Would you like to perform an activity concurrently? Available activities are: " + listAgentActivities(activities,agent));
						dtp.executeAndAdvance(-prevSystemTime.get(agent), "test_S", -(prevSystemTime.get(agent) + 15), "test_E", true, 15, true);
						
					}
					
//					System.out.println("SytemTime is " + getSystemTime() + " and MinTime is " + dtp.getMinTime());
					dtp.simplifyMinNetIntervals();
//					int new_time =  dtp.getMinTime() - getSystemTime();
					// The above will move the systemTime to the end of day after the last activity, rather than
					// leaving the possibility that there could be something after the last required activity, like
					// processing a sporadic event, or simply choosing to idle out the rest of the day
					int new_time =  time;
					incrementSystemTime(new_time, agent);
//					System.out.println("System time is: " + getSystemTime());

				}
			}
			catch(Exception e){
				System.err.println("Error.  Please try again.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()));
				System.err.flush();
			}
		}
		cin.close();
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
	
	private static void processSporadicEvent(String last_act) {
		if(last_act.length() == 0){
			printToClient("No activities have been performed yet...");
			return;
		} else if ((DUTP.SporadicEventLT < prevSystemTime.get(currentAgent)) || (DUTP.SporadicEventET >= systemTime.get(currentAgent))) {
			printToClient("Sporadic event couldn't have happened since last check in time.");
			return;
		}
		printToClient("Did a sporadic event occur during the previous activity or since the last check in time? (y/n)");
		/*String resp = cin.next();*/ // Commented out by Drew
		JSONObject jsonIN = getNextRequest();
		String resp = (String) jsonIN.get("value");
		
		if(resp.equalsIgnoreCase("y")){
			SPORADIC_OCCURRED = true;
			printToClient("At what time did the sporadic event happen? ");
			/*String time = cin.next();*/ // Commented out by Drew
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

	private static void getAndPerformIdle(String str, int minIdle, int maxIdle){
		minIdle = Math.max(minIdle,  0);
		maxIdle = Math.min(maxIdle, MAX_LET - getSystemTime());
		printToClient("How long to idle? Possible range {["+Generics.toTimeFormat(minIdle)+", "+Generics.toTimeFormat(maxIdle)+"]}");
		JSONObject jsonIN = getNextRequest();
		String temp = (String) jsonIN.get("value");
		int time = Generics.fromTimeFormat(temp);
		/*int time = Generics.fromTimeFormat(cin.next());*/ // Commented out by Drew
		if(time < 0 || time > maxIdle){
			printToClient("Unexpected response \""+str+"\"");
			return;
		}
		incrementSystemTime(time, currentAgent);
		dtp.advanceToTime(-getSystemTime(), time, true);
		dtp.simplifyMinNetIntervals();
	}
	
	private static void getAndPerformEmergencyIdle(String str){
		printToClient("How long to idle? Format for input is H:MM");
		JSONObject jsonIN = getNextRequest();
		String temp = (String) jsonIN.get("value");
		int time = Generics.fromTimeFormat(temp);
		/*int time = Generics.fromTimeFormat(cin.next());*/ // commented out by Drew
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
	
	private static void advancedSelection(){
		//TODO: some of these option are causing errors in the idle time calculations, needs to be fixed
		printToClient("Select from advanced options:\n(G)et interval\ntighten activity (D)uration\ntighten activity (A)vailability\nadd new inter-activity (C)onstraint\nadd (N)ew activity");
		JSONObject jsonIN = getNextRequest();
		String str = (String) jsonIN.get("value");
		/*String str = cin.next();*/ // Commented out by Drew
		
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
			/**
			for(String tpOne : tpNames){
					
					//String tpOne = tpNames.get(w);
					if(tpOne.equals(new_act)) continue;
					dtp.addNonconcurrentConstraint(tpOne, new_act, id);
					//System.out.println("adding nonconcurrency constraint between "+tpOne+" "+new_act);
				
			}
			**/
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
	private static void getIntervalSetAndAdd(String tpS, String tpE, int time, String msg){
		printToClient(msg+"\nEnter new interval set or (n) to leave unchanged. Format {[interval1]v[interval2]v...} with no whitespace.");
		JSONObject jsonIN = getNextRequest();
		String avail = (String) jsonIN.get("value");
		/*String avail = cin.next();*/ // Commented out by Drew
		if(!avail.equalsIgnoreCase("N")){
			IntervalSet is = Generics.stringToInterval(avail);
			dtp.addAdditionalConstraint(tpS, tpE, is, time, true, true);	
			dtp.simplifyMinNetIntervals();
		}		
	}
	
	/**
	 * ui helper code to prompt a user to select a timepoint from a
	 * @param a
	 * @param getStartEnd toggles whether to prompt user to specify between start/end timepoint
	 * @return
	 */
	private static String getTimepoint(Collection<String> a, boolean getStartEnd){
		JSONObject jsonIN = getNextRequest();
		String tp = (String) jsonIN.get("value");
		/*String tp = cin.next();*/ // Commented out by Drew
		while(!a.contains(tp)){
			printToClient("Unexpected response \""+tp+"\"");
			jsonIN = getNextRequest();
			tp = (String) jsonIN.get("value");
			/*tp = cin.next();*/ // Commented out by Drew
		}
		if(tp.equals("zero")) return tp; 
		boolean flag = getStartEnd;
		if(flag){
			printToClient("From (s)tart or (e)nd time of "+tp+"?");
			jsonIN = getNextRequest();
			String str = (String) jsonIN.get("value");
			/*String str = cin.next();*/ // Commented out by Drew
			while(flag){
				flag = false;
				if(str.equalsIgnoreCase("S")) tp += "_S";
				else if(str.equalsIgnoreCase("E")) tp += "_E";
				else{
					printToClient("Unexpected response \""+str+"\"");
					jsonIN = getNextRequest();
					str = (String) jsonIN.get("value");
					/*str = cin.next();*/ // Commented out by Drew
					flag = true;
				}
			}
		}
		return tp;
	}
	
	private static void getAgentSelection(){
		do{
			printToClient("Enter agent to control (0-"+(numAgents-1)+") or "+numAgents+" to control system clock:");
			JSONObject jsonIN = getNextRequest();
			currentAgent = Integer.parseInt( (String) jsonIN.get("value"));
			/*currentAgent = cin.nextInt();	*/ // Commented out by Drew				
		}while(currentAgent < 0 || currentAgent > numAgents);
		dtp.setCurrentAgent(currentAgent);
	}
	
	
	
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
	 * This class sends a JSON message to the client to print out for the user to see
	 * Author: Drew
	 * For now, it will save a JSON file that acts as a queue containing the things
	 * that the server wants to print. Will be flushed (erased) once the client sends a
	 * GET request and gets it
	 */
	private static void printToClient(String msg) {
		File tmpDir = new File("JSON_to_client.json");
		FileWriter fout;
		try {
			if (tmpDir.exists()) { // if the file already exists, read in JSON then edit it / add to it
			
				JSONParser parser = new JSONParser();
				Object fileIN = parser.parse(new FileReader("JSON_to_client.json"));
				JSONObject jsonIN = (JSONObject) fileIN;
				if (!tmpDir.delete()) { // delete the JSON file after receiving it
					System.out.println("Error: Failed to delete to_client file when writing.");
					// if you fail to delete this, do not send anything
					return;
				}
				
				msg = (String)jsonIN.get("toPrint") + '\n' + msg;
				
			}
			
			fout = new FileWriter("JSON_to_client.json");

			
			// JSON will have object {toPrint : someMessage}
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("toPrint", msg);
	
			System.out.println("Server output: " + msg);
			fout.write(jsonObj.toJSONString()); // write the JSON to a string to the file
			
			
			fout.close();
			
		} catch (Exception e) {
			System.err.println("Error while writing to_client file");
			System.err.flush();
		}
	}

	/*
	 * Drew: We need a method that acts in replacement of the stdin
	 * It will need to hold up the main thread until the client sends it information
	 * For now, we will ignore the possibility of needing to ping the server and re-request information
	 * This should only be called when the program cannot do anything until it hears from the client
	 */
	private static JSONObject getNextRequest() {
		while (true) {
			JSONObject jsonIN = new JSONObject();
			try {
				// the server thread will be constantly running in the background looking for
				// requests from the client
				// Once it receives one, it will save the JSON as a file called JSON_from_client.json
				// wait until the server gets a response from the client before continuing
				File tmpDir = new File("JSON_from_client.json");
				while( !tmpDir.exists() ) {
					Thread.sleep(100);
					tmpDir = new File("JSON_from_client.json");
				}
				if (tmpDir.length() == 0) {
					tmpDir.delete();
					continue;
				}
				
				JSONParser parser = new JSONParser();
				Object fileIN = parser.parse(new FileReader("JSON_from_client.json"));
				if (!tmpDir.delete()) { // delete the JSON file after receiving it
					System.out.println("Error: Failed to delete file.");
				}
				
				jsonIN = (JSONObject) fileIN;
				
			} catch (Exception e) {
				System.err.println("Error while waiting in getNextRequest()");
				System.err.println(e);
				System.err.flush();
			}
			
			if (!jsonIN.isEmpty()) {
				if (jsonIN.get("value").equals("RESTART")) {Thread.currentThread().interrupt();}
				System.out.println("User input: " + (String)jsonIN.get("value"));
				return jsonIN;
			}
		}
		
	}
	
}
