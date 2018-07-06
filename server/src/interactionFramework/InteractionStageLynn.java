package interactionFramework;

import interval.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;


import dtp.*;

/**
 * 
 * This class provides an interaction framework that allows users to step through a schedule.
 * The displayed information and permitted options are restricted such that only the currently permissible options are provided
 *
 */
public class InteractionStageLynn {
	private static String problemFile = "multiagentSampleProblem.xml";

	//private static String problemFile = "parentSampleProblem.xml";
	private static final Interval zeroInterval = new Interval(0,0);
	private static DisjunctiveTemporalProblem dtp;
	private static DisjunctiveTemporalProblem prevDTP = null;
	private static List<Integer> systemTime; //systemTime[i] is the current system time for agent i
	private static int numAgents;
	private static int currentAgent = 0;  //currentAgent = [0,numAgents], currentAgent = numAgents means to control the system clock (i.e. all agents)
	private static Scanner cin = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
	private static final int DEBUG = 0;

	public static void main(String[] args){
		dtp = new ProblemLoader().loadDTPFromFile(problemFile);
		System.out.println("Problem loaded");
		numAgents = dtp.getNumAgents();
		systemTime = new ArrayList<Integer>(numAgents);
		for(int i = 0; i < numAgents; i++) systemTime.add(0);
		dtp.printConstraints(Generics.getLogStream());
		if(numAgents > 1){
			getAgentSelection();
		}
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		boolean flag = true;
		while(flag){
			try{	
				if(DEBUG == 0){
					//if(prevDTP == null) Generics.printDTP(dtp);
					//else dtp.printDeltas(dtp.rankDeltas(prevDTP, 5));
					System.out.println(dtp.getMaxSlack());
					System.out.println("Rigidity " + dtp.getRigidity());
					System.out.println("Flexibility " + dtp.getTotalFlexibility());
					dtp.printSelectionStack();
				}
				if(DEBUG >= 2) dtp.printSelectionStack();
				if(DEBUG >= 1){
					System.out.println("SystemTime: "+systemTime.toString());
					Generics.printDTP(dtp);
				}
				System.out.println("\nCurrent Time: "+Generics.toTimeFormat(getSystemTime()));
				int minTime = dtp.getMinTime();
				if(minTime > 24*60){
					System.out.println("End of day.");
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
				
				prevDTP = dtp.clone();

				//Prompt user for their activity selection				
				List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime());
				List<Integer> maxSlack = dtp.getMaxSlack();
				if(maxSlack.size() == 1 && maxSlack.get(0) > 0) activities.get(0).add("idle");
				else for(int i = 0; i < maxSlack.size(); i++){
					//agent can only idle if it has available slack and needs to currently make a selection
					if(maxSlack.get(i) > 0 && getSystemTime(i) == getSystemTime()) activities.get(i).add("agent"+i+"Idle"); 
				}
				if(numAgents > 1 && currentAgent != numAgents)
					System.out.println("(A)dvanced selection, (U)ndo last selection, (S)witch agent, or select activity for agent "+ currentAgent+" to perform: "+activities.get(0).toString());
				else if(numAgents > 1){
					System.out.println("(A)dvanced selection, (U)ndo last selection, (S)witch agent, or select activity to perform: ");
					for(int i = 0; i < activities.size(); i++){
						System.out.println("Agent "+i+": "+activities.get(i).toString());
					}
				}
				else
					System.out.println("(A)dvanced selection, (U)ndo last selection, or select activity to perform: "+activities.get(0).toString());
				String str = cin.next();
				if(str.equalsIgnoreCase("A")){
					advancedSelection();
					continue;
				}
				else if(str.equalsIgnoreCase("s") && numAgents > 1){
					getAgentSelection();
					continue;
				}
				//TODO: look at selection stack stuff w.r.t. multiple agents and switching between them
				else if(str.equalsIgnoreCase("U")){
					System.out.println("Undoing last selection.");
					System.out.println("with SystemTime " + -getSystemTime());
					incrementSystemTime(-dtp.popSelection(-getSystemTime()));
					System.out.println("Resetting time to "+Generics.toTimeFormat(getSystemTime()));
					continue;
				}
				else if(!Generics.concat(activities).contains(str)){
					System.out.println("Unexpected response \""+str+"\"");
					continue;
				}
				int time;
				if((str.equals("idle") && currentAgent != numAgents) || str.matches("^agent\\d+Idle$")){
					if(currentAgent == numAgents){
						int idleIdx = Integer.parseInt(str.substring(5, str.length()-4));
						dtp.setCurrentAgent(idleIdx);
						currentAgent = idleIdx;
						getAndPerformIdle(str, maxSlack.get(idleIdx));
						dtp.setCurrentAgent(numAgents);
						currentAgent = numAgents;
					}
					else{
						getAndPerformIdle(str, maxSlack.get(0));
					}
				}
				else{ //performing an activity
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
					dtp.executeAndAdvance(-getSystemTime(), str+"_S",-(getSystemTime()+time),str+"_E",true, time, true);
					incrementSystemTime(time, agent);
					dtp.simplifyMinNetIntervals();
				}
			}
			catch(Exception e){
				System.err.println("Error.  Please try again.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()));
				System.err.flush();
			}
		}
		cin.close();
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
			System.out.println("Enter source activity of new constraint.  Activities are: "+activities.toString());
			String source = getTimepoint(activities,true);
			System.out.println("Enter destination activity of new constraint.  Activities are: "+activities.toString());
			String dest = getTimepoint(activities,true);
			getIntervalSetAndAdd(source,dest,getSystemTime(),"");
		}
		
		//add new activity
		else if(str.equalsIgnoreCase("N")){ 
			System.out.println("Option not yet implemented");
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
	private static int getSystemTime(){
		return getSystemTime(currentAgent);
	}
	
	private static int getSystemTime(int agent){
		if(agent == numAgents){
			return Collections.min(systemTime);
		}
		return systemTime.get(agent);
	}
	
	private static void incrementSystemTime(int val){
		setSystemTime(val+getSystemTime());
	}
	
	private static void incrementSystemTime(int val, int agent){
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
	
	private static void advanceSystemTimeTo(int val){
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
}
