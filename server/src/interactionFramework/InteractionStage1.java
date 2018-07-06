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

import stp.TemporalDifference;
import stp.Timepoint;
import util.MultiNode;
import util.Node;


import dtp.*;

/**
 * 
 * This class provides an interaction framework that allows users to step through a schedule.
 * The displayed information and permitted options are restricted such that only the currently permissible options are provided
 *
 */
// Durfee comment just to test SVN within Eclipse
public class InteractionStage1 {
	//private static String problemFile = "stn1.xml";
	private static boolean PREEMPTIVE = false;
	private static boolean CONCURRENT = false;
	private static boolean SPORADIC = false;
	private static boolean SPORADIC_OCCURRED = false;
	private static int SP_DUR = 20;
	//private static String problemFile = "MABreakfastSplit.xml";
	private static String problemFile = "multiagentSampleProblem.xml";
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
	public static void main(String[] args){
		
		
		dtp = new ProblemLoader().loadDTPFromFile(problemFile);
		System.out.println("Problem loaded");
		
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);		
		dtp.simplifyMinNetIntervals();
		
		
		initialDTP = dtp.clone();
		numAgents = dtp.getNumAgents();
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		//dtp.printConstraints(Generics.getLogStream());
		if(numAgents > 1){
			getAgentSelection();
		}
		
		Generics.printDTP(dtp);
		initialDTP = dtp.clone();
		
		boolean flag = true;
		while(flag){
			try{	
				//dtp.getDTPBoundaries();
				CONCURRENT = false;
				if(DEBUG >= 2) dtp.printSelectionStack();
				if(DEBUG >= 1){
					System.out.println("SystemTime: "+systemTime.toString());
					Generics.printDTP(dtp);
					//System.out.println("Calls to solver" +dtp.getCallsToSolver());
				} if (DEBUG == 0){ // Print Deltas mode
					if(prevDTP == null) Generics.printDTP(dtp);
					else dtp.printDeltas(dtp.getDeltas(prevDTP));
				}
				System.out.println("\nCurrent Time: "+Generics.toTimeFormat(getSystemTime()));
				//Viz.printDTPDiagram(dtp, initialDTP, getSystemTime(),1);
				int minTime = dtp.getMinTime();
				if(minTime > 24*60){
					System.out.println("End of day.");
					Generics.printDTP(prevDTP);
					Generics.printDTP(dtp);
					return;
				}
				else if(minTime > getSystemTime()){
					System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
					int temp = minTime - getSystemTime();
					advanceSystemTimeTo(minTime);
					dtp.advanceToTime(-getSystemTime(), temp, false);
					dtp.simplifyMinNetIntervals();
					continue;
				}
				else if(minTime < getSystemTime()){
					
					throw new Error("minTime("+minTime+") < systemTime("+systemTime+")");		
				}

				//Prompt user for their activity selection				
				List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime());
				List<Integer> maxSlack = dtp.getMaxSlack();
				
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
				
				//capability for observing and updating problem when SEs are present
				if(SPORADIC && !SPORADIC_OCCURRED){
					processSporadicEvent(last_activity);
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
				System.out.println(ongoing_str);
				if(numAgents > 1 && currentAgent != numAgents)
					System.out.println("Select activity for agent " + currentAgent+" to perform: " + activities.get(0).toString() + " unless a (S)poradic event occurred, or (O)utput schedule information, (U)ndo last selection, (C)hange agent, or (A)dvanced selection.");
				else if(numAgents > 1){
					System.out.println("(A)dvanced selection, (U)ndo last selection, (S)witch agent, (O)utput schedule information, or select activity to perform: ");
					for(int i = 0; i < activities.size(); i++){
						System.out.println("Agent "+i+": "+activities.get(i).toString());
					}
				}
				else {
					if(maxSlack.get(0) >= 20 && !PREEMPTIVE){
						System.out.println("Select activity to perform: "+activities.get(0).toString() + " or (A)dvanced selection, (U)ndo last selection, (O)utput schedule information, (P)erform preemptive activity, or (E)mergency idle.");
					}else{
					System.out.println("Select activity to perform: "+activities.get(0).toString() + " or (A)dvanced selection, (U)ndo last selection, (O)utput schedule information, or (E)mergency Idle.");
				
					}
				}
				//dtp.checkBookends(20);
				String str = cin.next();
				if(str.equalsIgnoreCase("A")){
					advancedSelection();
					//TODO: this isn't quite right because there's a way to not make any changes in advanced selection.
					continue;
				}
				else if(str.equalsIgnoreCase("c") && numAgents > 1){
					getAgentSelection();
					continue;
				}
				//TODO: look at selection stack stuff w.r.t. multiple agents and switching between them
				else if(str.equalsIgnoreCase("U")){
					if(previousDTPs.empty()) {
						System.out.println("The schedule has not yet been modified.\n");
						continue;
					}
					System.out.println("Undoing last selection.");
					
					SimpleEntry<Integer, DisjunctiveTemporalProblem> ent = previousDTPs.pop();
					
					setSystemTime(ent.getKey());
					System.out.println("Resetting time to "+Generics.toTimeFormat(getSystemTime()));
									//dtp = dtpClone(prevDTP, getSystemTime());
					dtp = ent.getValue();
					
					continue;
				}
				else if(str.equalsIgnoreCase("o")){
					System.out.println("Output (C)urrent DTP, (V)isualize DTP, (B)ookends between activities or (D)eltas between current and previous DTP?");
					String option = cin.next();
					printRequestedOutput(option);
					continue;
				}
				
				
				
				else if(str.equalsIgnoreCase("P")){
					//perform preemptive activity. 
					// in theory this would "get rid of" the option of an emergent activity coming up, but this is only applicable 
					// in the exhaustiveDTP case.
					System.out.println("The first bookend in your day is toCampus which must be completed from 9:00 to 9:30. Would you like to perform a preemptive activity now? (Y/N)");
					str = cin.next();
					performPreemptive(str);
					continue;
				}

				else if(str.equalsIgnoreCase("E")){
					// perform emergency idle
					//needs to not break the interaction even if it breaks the schedule.
					getAndPerformEmergencyIdle(str);
					continue;
				}
				
				else if(str.equalsIgnoreCase("S")){
					System.out.println("When did the sporadic event begin?");
					String option = cin.next();
					int start = Generics.fromTimeFormat(option);
					int end = start + 20;
					String etime = Generics.toTimeFormat(end);
					int end_act = end+5;
					String eact = Generics.toTimeFormat(end_act);
					System.out.println("Inserting sporadic event from " + option + " to " + etime + "\n");
					System.out.println("Current time is: " + eact);
					
					System.out.println("Select activity for agent " + currentAgent+" to perform: [dressing, orgTimeM] or (O)utput schedule information, (U)ndo last selection, (C)hange agent, or (A)dvanced selection.");
					continue;
				}
				
				else if(!Generics.concat(activities).contains(str)){
					System.out.println("Unexpected response \""+str+"\"");
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
						getAndPerformIdle(str, maxSlack.get(idleIdx));
						dtp.setCurrentAgent(numAgents);
						currentAgent = numAgents;
					}
					else{
						last_activity = "idle";
						getAndPerformIdle(str, maxSlack.get(0));
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
					System.out.println("Option will advance the clock to " + Generics.toTimeFormat(getSystemTime() + skip_time));
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
					if(interval.totalSize() != 1){
						boolean durationFlag = true;
						do{
							System.out.println("How long to perform "+str+"? Possible Options "+interval.toString());
							time = Generics.fromTimeFormat(cin.next());
							if(!interval.intersect(time)){
								System.out.println("Unexpected response \""+str+"\"");
								continue;
							}
							
							IntervalSet endTime = dtp.getInterval("zero", str+"_E");
							int idle = (int) (-endTime.getUpperBound() - getSystemTime() - time);
							if(idle > 0){	
								System.out.println("Option will force immediate idle for "+Generics.toTimeFormat(idle)+". Continue (y/n)?");
								String temp = cin.next();
								if(temp.equalsIgnoreCase("y")){
									incrementSystemTime(idle, agent);
									System.out.println("Inserting idle for "+Generics.toTimeFormat(idle)+".");
									System.out.println("CurrentTime: "+Generics.toTimeFormat(getSystemTime()));
									durationFlag = false;
								}
								else if(!temp.equalsIgnoreCase("n")){
									System.out.println("Unexpected response "+temp+"\n");
								}
							}
							else{
								durationFlag = false;
							}
						}while(durationFlag);
						System.out.println("Performing "+str+" for "+Generics.toTimeFormat(time));
					}	
					else{
						System.out.println("Performing "+str+" for "+interval.toString());
						time = (int) interval.getLowerBound();
					}
					// before executeandadvance we want to add the activity we're performing to our list. 
					
					Interval curr_int = new Interval(getSystemTime(), getSystemTime() + time);
					ongoingActs.add(new SimpleEntry<String,Interval>(str, curr_int));
					//System.out.println(ongoingActs);
					dtp.executeAndAdvance(-getSystemTime(), str+"_S",-(getSystemTime()+time),str+"_E",true, time, true);
					
					//Here we want to check to see if there are any concurrent activities
					if(CONCURRENT){
						activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -prevSystemTime.get(agent));
						System.out.println("Concurrent activities are: " + activities);
						System.out.println("Would you like to perform an activity concurrently? Available activities are: " + activities.get(agent));
						dtp.executeAndAdvance(-prevSystemTime.get(agent), "test_S", -(prevSystemTime.get(agent) + 15), "test_E", true, 15, true);
						
					}
					
					dtp.simplifyMinNetIntervals();
					int new_time =  dtp.getMinTime() - getSystemTime();
					incrementSystemTime(new_time, agent);

				}
			}
			catch(Exception e){
				System.err.println("Error.  Please try again.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()));
				System.err.flush();
			}
		}
		cin.close();
	}
	
	private static void processSporadicEvent(String last_act) {
		if(last_act.length() == 0){
			System.out.println("No activities have been performed yet...");
			return;
		}
		System.out.println("Did a sporadic event occur during the previous activity or since the last check in time? (y/n)");
		String resp = cin.next();
		
		if(resp.equalsIgnoreCase("y")){
			SPORADIC_OCCURRED = true;
			System.out.println("At what time did the sporadic event begin? ");
			String time = cin.next();
			int se_start = Generics.fromTimeFormat(time);
			int end_time = se_start;
			int start_time = prevSystemTime.get(0);
			int dur = end_time - start_time;
			int last_act_dur = getSystemTime() - prevSystemTime.get(0);
			System.out.println("Inserting sporadic event in orgTimeM beginning at " + time + "\n" );
			System.out.println("Current time: 6:50.\n");
			int start_time2 = end_time + SP_DUR;
			int end_time2 = start_time2 + (last_act_dur - dur);
			int dur2 = end_time2 - start_time2;
			//change the problem to reflect that the sporadic event occurred at time se_start.
			// this should split last_activity into two portions. 
			
			//save additional constraints and fixed timepoints from the previous DTP
				// we want to grab the problem before last_act has actually been performed
				// otherwise I think things will break...
			Collection<DisjunctiveTemporalConstraint> constrToSave = prevDTP.getAdditionalConstraints();
			ArrayList<Timepoint> fixedToSave = prevDTP.getFixedTimepoints();
			//advance the new DTP to the previous time
			// perform last_act_part1, insert 20 min idle (or add in new event to XML file?)
			// perform last_act_part2, and also advance systemTime.
			
			//DisjunctiveTemporalProblem split_dtp = new ProblemLoader().loadDTPFromFile(problemFile);
			DisjunctiveTemporalProblem split_dtp = initialDTP.clone();
			//rename activity A start and end.
			Timepoint orig_s = split_dtp.getTimepoint(last_act + "_S");
			Timepoint orig_e = split_dtp.getTimepoint(last_act + "_E");
			split_dtp.updateTimepointName(orig_s, last_act+"1_S");
			split_dtp.updateTimepointName(orig_e, last_act+"1_E");
			orig_s.changeName(last_act + "1_S");
			orig_e.changeName(last_act + "1_E");
			
			//add in a new activity for last_act2
			Timepoint new_s = new Timepoint(last_act+"2_S",1);
			Timepoint new_e = new Timepoint(last_act+"2_E", 1);
			split_dtp.addTimepoint(new_s);
			split_dtp.addTimepoint(new_e);
			
			//first remove old duration constraint on last_act
			split_dtp.removeDurationConstraint(last_act);
			System.out.println("AFTER REMOVING DURATIONS:");
			System.out.println(split_dtp.getTempConstraints());
			//update constraint set to change all of the originalConstraints to respect new TP names.
			for(DisjunctiveTemporalConstraint dtc : split_dtp.getTempConstraints()){
				for(TemporalDifference td: dtc.getTemporalDifferences()){
					//if(td.source.getName().equals(last_act + "1_S")) td.updateSource(orig_s);
					if(td.source.getName().equals(last_act + "1_E")) td.updateSource(new_e);
					//if(td.destination.getName().equals(last_act + "1_S")) td.updateDestination(orig_s);
					if(td.destination.getName().equals(last_act + "1_E")) td.updateDestination(new_e);
					
					
				}
				//System.out.println("dtc is now: " + dtc.toString());
			}
			
			
			// do we need to update the additional constraints we saved to get rid of the A timepoints in the same way??
			// the code for this is here. may need to be commented out.
			for(DisjunctiveTemporalConstraint dtc : constrToSave){
				for(TemporalDifference td: dtc.getTemporalDifferences()){
					if(td.source.getName().equals(last_act + "_S")) td.updateSource(orig_s);
					if(td.source.getName().equals(last_act + "_E")) td.updateSource(new_e);
					if(td.destination.getName().equals(last_act + "_S")) td.updateDestination(orig_s);
					if(td.destination.getName().equals(last_act + "_E")) td.updateDestination(new_e);
				}
			}
			
			//add duration constraints for the new split activities
			split_dtp.addDurationConstraint(orig_s, orig_e, dur);
			split_dtp.addDurationConstraint(new_s, new_e, dur2);
			//split_dtp.addOrderingConstraint(orig_e.getName(), new_s.getName(), 0, Integer.MAX_VALUE);
			
			//what happens if wetry to solve this new problem as is
			split_dtp.enumerateSolutions(0);
			split_dtp.simplifyMinNetIntervals();
			System.out.println("DTP with modifications before adding in old constraints");
			Generics.printDTP(split_dtp);
			
			// now we need to bring the updated DTP back to the systemTime before last_act
			split_dtp.addAdditionalConstraints(constrToSave);
			split_dtp.enumerateSolutions(prevSystemTime.get(0));
			split_dtp.simplifyMinNetIntervals();
			System.out.println("DTP BEFORE performing the split activities");
			Generics.printDTP(split_dtp);

			// remove null constraints from additional constraints...
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			for(int i = 0; i < split_dtp.getAdditionalConstraints().size(); i++){
				DisjunctiveTemporalConstraint dtc = split_dtp.getAdditionalConstraints().get(i);
				if(dtc == null) toRemove.add(i);
			}
			System.out.println("Removing dtcs: " + toRemove);
			for(int i : toRemove) split_dtp.getAdditionalConstraints().remove(i);
			
			split_dtp.addFixedTimepoints(fixedToSave);
			dtp = split_dtp.clone();
			setSystemTime(prevSystemTime.get(0));
			// now we need to perform last_act1 
/**
			split_dtp.executeAndAdvance(-start_time, orig_s.getName(), -end_time, orig_e.getName(), true, dur, true);
			
			dtp.enumerateSolutions(end_time);
			dtp.simplifyMinNetIntervals();
			System.out.println("after performing first part of last_act");
			Generics.printDTP(split_dtp);
			
			split_dtp.advanceToTime(end_time, SP_DUR, true); // insert sporadic activity 
			// perform last_act2 now that we've inserted idle.
			
			split_dtp.executeAndAdvance(start_time2, new_s.getName(), end_time2, new_e.getName(), true, dur2, true);
			split_dtp.simplifyMinNetIntervals();
			incrementSystemTime(SP_DUR); //increment system time by duration of SE
			
			dtp = split_dtp.clone();
			System.out.println("AFTER performing SPLITTING");
			Generics.printDTP(split_dtp);
		**/
			return;
		}else if(resp.equalsIgnoreCase("n")){
			return;
		}
		
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
			System.out.println("Performing Preemptive activity for 20 minutes.");
			PREEMPTIVE = true;
			incrementSystemTime(20, currentAgent);
			dtp.advanceToTime(-getSystemTime(), 20, true);
			dtp.simplifyMinNetIntervals();
		
			System.out.println("You have prevented an emergency activity from happening before your morning class.");
		} else if(str.equalsIgnoreCase("n")){
			System.out.println("You have chosen not to perform a preemptive activity at this time.");
			return;
		}
	}

	private static void getAndPerformIdle(String str, int maxSlack){
		System.out.println("How long to idle? Possible Options {[0:00, "+Generics.toTimeFormat(maxSlack)+"]}");
		int time = Generics.fromTimeFormat(cin.next());
		if(time < 0 || time > maxSlack){
			System.out.println("Unexpected response \""+str+"\"");
			return;
		}
		incrementSystemTime(time, currentAgent);
		dtp.advanceToTime(-getSystemTime(), time, true);
		dtp.simplifyMinNetIntervals();
	}
	
	private static void getAndPerformEmergencyIdle(String str){
		System.out.println("How long to idle? Format for input is H:MM");
		int time = Generics.fromTimeFormat(cin.next());
		if(time < 0){
			System.out.println("Unexpected response \""+str+"\"");
			return;
		}
		List<String> uftps = dtp.getUnFixedTimepoints();
		System.out.println(uftps);
		System.out.println("incrementing system time by " + time);
		incrementSystemTime(time, currentAgent);
		System.out.println("advancing to time");
		dtp.advanceToTime(-getSystemTime(), time, true);
		System.out.println("simplifying min net intervals");
		dtp.simplifyMinNetIntervals();
		if (dtp.getMinTime() > 24*60){
			uftps = dtp.getUnFixedTimepoints();
			//for(Timepoint tp : uftps) System.out.println(tp.getName());
		}
	}
	
	private static void printRequestedOutput(String option){
		if(option.equalsIgnoreCase("D")){
			if(prevDTP == null) System.out.println("You have not yet made a choice or have just undone a choice.");
			else dtp.printDeltas(dtp.getDeltas(previousDTPs.peek().getValue()));
		}else if(option.equalsIgnoreCase("C")){
			Generics.printDTP(dtp);
		} else if(option.equalsIgnoreCase("V")){
			Viz.printDTPDiagram(dtp, initialDTP, getSystemTime(),1);
		}else if(option.equalsIgnoreCase("B")){
			dtp.checkBookends(20);
		}
		
		else{
			System.out.println("Unexpected response \"" + option + "\"");
		}
		return;
	}
	
	private static void advancedSelection(){
		//TODO: some of these option are causing errors in the idle time calculations, needs to be fixed
		System.out.println("Select from advanced options: (G)et interval, tighten activity (D)uration, tighten activity (A)vailability, add new inter-activity (C)onstraint, or add (N)ew activity");
		String str = cin.next();
		
		//get interval between two timepoints
		if(str.equalsIgnoreCase("G")){
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime()));
			activities.add("zero");
			System.out.println("Enter first activity to find interval from: "+ activities.toString());
			String tp1 = getTimepoint(activities,true);
			System.out.println("Enter second activity to find interval from: "+ activities.toString());
			String tp2 = getTimepoint(activities,true);
			System.out.println("Interval from "+tp1+" to "+tp2+" is "+dtp.getInterval(tp1, tp2));
			return;
		}
		
		//tighten activity duration
		else if(str.equalsIgnoreCase("D")){ 
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARDUR, -getSystemTime()));
			if(activities.isEmpty()){
				System.out.println("No activities with variable duration.  Please make another selection.");
				return;
			}
			prevDTP = dtp.clone();
			previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
			System.out.println("Select activity to tighten duration for.  Current activities with variable duration: "+activities.toString());
			str = getTimepoint(activities,false);
			IntervalSet interval = dtp.getInterval(str+"_S", str+"_E").inverse();
			getIntervalSetAndAdd(str+"_S",str+"_E",getSystemTime(),"Current duration for "+str+" is: "+interval.toString()+".");
		}
		
		//tighten activity availability
		else if(str.equalsIgnoreCase("A")){ 
			Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.VARAVAIL, -getSystemTime()));
			if(activities.isEmpty()){
				System.out.println("No activities with variable availability.  Please make another selection.");
				return;
			}
			prevDTP = dtp.clone();
			previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
			System.out.println("Select activity to tighten availability for.  Current activities with variable availability: "+activities.toString());
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
				System.out.println("No activities found.  Please try again.");
				return;
			}
			//prevDTP = dtpClone(dtp, getSystemTime());
			prevDTP = dtp.clone();
			previousDTPs.push(new SimpleEntry<Integer, DisjunctiveTemporalProblem>(getSystemTime(), prevDTP));
			System.out.println("Enter source activity of new constraint.  Activities are: "+activities.toString());
			String source = getTimepoint(activities,true);
			System.out.println("Enter destination activity of new constraint.  Activities are: "+activities.toString());
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
			System.out.println("Unexpected response \""+str+"\"");
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
		System.out.println(msg+"\nEnter new interval set or (n) to leave unchanged. Format {[interval1]v[interval2]v...} with no whitespace.");
		String avail = cin.next();
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
		String tp = cin.next();
		while(!a.contains(tp)){
			System.out.println("Unexpected response \""+tp+"\"");
			tp = cin.next();
		}
		if(tp.equals("zero")) return tp; 
		boolean flag = getStartEnd;
		if(flag){
			System.out.println("From (s)tart or (e)nd time of "+tp+"?");
			String str = cin.next();
			while(flag){
				flag = false;
				if(str.equalsIgnoreCase("S")) tp += "_S";
				else if(str.equalsIgnoreCase("E")) tp += "_E";
				else{
					System.out.println("Unexpected response \""+str+"\"");
					str = cin.next();
					flag = true;
				}
			}
		}
		return tp;
	}
	
	private static void getAgentSelection(){
		do{
			System.out.println("Enter agent to control (0-"+(numAgents-1)+") or "+numAgents+" to control system clock:");
			currentAgent = cin.nextInt();					
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
}
