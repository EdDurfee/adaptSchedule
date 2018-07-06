package interactionFramework;

import interval.Interval;
import interval.IntervalSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.AbstractMap.SimpleEntry;

import stp.TemporalDifference;
import stp.Timepoint;

import dtp.DisjunctiveTemporalConstraint;
import dtp.DisjunctiveTemporalProblem;
import dtp.ExhaustiveDTP;
import dtp.SimpleDTP;

public class InteractionStageExhaustive {
	private static String problemFile = "prelim.xml";
	//private static String problemFile = "stnExhaustive.xml";
	private static final Interval zeroInterval = new Interval(0,0);
	private static DisjunctiveTemporalProblem dtp;
	//private static DisjunctiveTemporalProblem prevDTP = null;
	private static DisjunctiveTemporalProblem initialDTP;
	private static List<Integer> systemTime; //systemTime[i] is the current system time for agent i
	private static List<Integer> prevSystemTime;
	private static boolean WORLD = true;
	private static boolean WorldActed = false;
	private static int numAgents;
	private static int currentAgent = 0;  //currentAgent = [0,numAgents], currentAgent = numAgents means to control the system clock (i.e. all agents)
	private static Scanner cin = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
	private static final int DEBUG =-1;
	private static Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>> previousDTPs = new Stack<SimpleEntry<Integer, DisjunctiveTemporalProblem>>();
	
	public static void main(String[] args){
		DisjunctiveTemporalProblem dtp1 = new ProblemLoader().loadDTPFromFile(problemFile);
		
		//DisjunctiveTemporalProblem dtp1 = testFramework.readFromFile("/home/lynngarrett/maDTPRepo/MaDTP/test_med2/dtp2.data");
		//ArrayList<ArrayList<DisjunctiveTemporalConstraint>> EC = makeExhaustiveConstraintHM(dtp);
		//testFramework.checkValidityExhaustive(dtp);
		//dtp = testFramework.createExhaustive(dtp1);
		System.out.println("Problem loaded");
		//dtp = ((ExhaustiveDTP) dtp).getComponentDTP(1);
		Iterator<DisjunctiveTemporalProblem> it = ((ExhaustiveDTP)dtp).getDTPs().iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			System.out.println(((SimpleDTP) curr).checkValidity());
		}
		//dtp.enumerateSolutions(0);
		//dtp.simplifyMinNetIntervals();
		//dtp = new ExhaustiveDTP(dtp, EC);
		//System.out.println("Updated to be ExhaustiveDTP");
		initialDTP = dtp.clone();
		numAgents = dtp.getNumAgents();
		systemTime = new ArrayList<Integer>(numAgents);
		prevSystemTime = new ArrayList<Integer>(numAgents);
		for(int i = 0; i < numAgents; i++) systemTime.add(0); prevSystemTime.add(0);
		dtp.printConstraints(Generics.getLogStream());

		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();

		boolean flag = true;
		while(flag){
			try{
				System.out.println("\nCurrent Time: "+Generics.toTimeFormat(getSystemTime()));
				int minTime = dtp.getMinTime();
				//System.out.println(minTime);
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
				
				//Prompt the world for activity selection
				if(WORLD && !WorldActed){
					//TODO: fix this hackiness. maybe add this function to the interface as well?
					HashMap<String, DisjunctiveTemporalProblem> contingentActMap = ((ExhaustiveDTP) dtp).getContingentActivitiesMap(-getSystemTime());
					if(contingentActMap.keySet().size() > 0) {
						System.out.println("Presenting contingent activities to the world");
						System.out.println("Select activity to perform: "+contingentActMap.keySet().toString()+" or (P)ass" );
						WORLD = false;
						String str = cin.next();
						if(str.equalsIgnoreCase("P")){
							//TODO: this needs to send us into the else block below...
							WORLD = false;
							passContingentActivity(contingentActMap);
						}else if(!contingentActMap.keySet().contains(str)){
							System.out.println("Unexpected response \""+ str + "\"");
						}else{
							// otherwise we perform the contingent activity
							WorldActed = true; // the world only gets to act once. 
							performContingentActivity(str, contingentActMap);
							continue;
							
						}
					}else{
						System.out.println("nothing for the world to do");
						WORLD = false;
						continue;
					}
				}else{
					// prompt user for activity selection
					System.out.println("Preparing to prompt the user for selection");
					List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -getSystemTime());
					List<Integer> maxSlack = dtp.getMaxSlack();
					if(maxSlack.size() == 1 && maxSlack.get(0) > 0) activities.get(0).add("idle");
					//TODO: add in for loop for multiple agents, see line 95 of IS1
					System.out.println("Select activity to perform: "+activities.get(0).toString());
					String str = cin.next();
					if(str.equals("idle")) getAndPerformIdle(str, maxSlack.get(0));
					else performActivity(str);
					WORLD = true;
					continue;
				}
				
			}catch(Exception e){
				System.err.println("Error.  Please try again.\n"+e.toString()+"\n"+Arrays.toString(e.getStackTrace()));
				System.err.flush();
			}
		}
		
		
	}
	
	//TODO: was copying over this set of functions directly the right thing to do?? 
	private static int getSystemTime(){
		return getSystemTime(currentAgent);
	}
	
	static void passContingentActivity(HashMap<String, DisjunctiveTemporalProblem> contingentActMap){
		// if we pass on a contingent activity, then the DTP that it came from (they came from) needs to be marked as invalid
		for(DisjunctiveTemporalProblem dtp : contingentActMap.values()){
			dtp.updateValidity(0);
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
	
	private static void performActivity(String str){
		//TODO: this is going to need to be changed for it we're working with a contingent activity........ 
		IntervalSet interval = dtp.getInterval(str+"_S", str+"_E").inverse().subtract(zeroInterval);
		int time;
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
	
	static void performContingentActivity(String str, HashMap<String, DisjunctiveTemporalProblem> activityMap){
		IntervalSet interval = dtp.getInterval(str+"_S", str+"_E").inverse().subtract(zeroInterval);
		int time;
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
		DisjunctiveTemporalProblem currDTP = activityMap.get(str);
		// if we perform a contingent activity, all of the DTPs that the contingent timepoint is not a part of
			// should all be changed to invalid I think. 
		//FIXME: is it possible that we might just be able to like.. add in an automatic idle into all the other
			//DTPs when a contingent activity is performed?? 
		dtp.updateValidity(currDTP, 0);
		//TODO: should this now just be dtp. instead of currDTP. since we just updated validity?
		currDTP.executeAndAdvance(-getSystemTime(), str+"_S",-(getSystemTime()+time),str+"_E",true, time, true);
		incrementSystemTime(time, agent);
		currDTP.simplifyMinNetIntervals();	
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
	private static ArrayList<ArrayList<DisjunctiveTemporalConstraint>> makeExhaustiveConstraintHM(DisjunctiveTemporalProblem dtp){
		ArrayList<ArrayList<DisjunctiveTemporalConstraint>> dtcs = new ArrayList<ArrayList<DisjunctiveTemporalConstraint>>();
		int duration = 20;
		Timepoint h_s = new Timepoint("health_S",1);
		Timepoint h_e = new Timepoint("health_E",1);
		Timepoint zero = dtp.getTimepoint("zero");
		//duration constraints
		TemporalDifference tdmin = new TemporalDifference(h_s, h_e, -duration);
		TemporalDifference tdmax = new TemporalDifference(h_e, h_s, duration);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		Collection<DisjunctiveTemporalConstraint> dtcsToAdd = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		
		ArrayList<TemporalDifference> eStart = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> lStart = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> eEnd = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> lEnd = new ArrayList<TemporalDifference>();
		
		List<String> acts = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, 0).get(0);
		int numTimepoints = dtp.getTimepoints().size(); 
		int numActivities = (numTimepoints - 1) / 2;
		
		for(int i = 0; i < numActivities; i++) dtcs.add(new ArrayList<DisjunctiveTemporalConstraint>());
		System.out.println("number of activities is "+numActivities);
		System.out.println("Length of dtcs is " + dtcs.size());
		
		for(ArrayList<DisjunctiveTemporalConstraint> elm : dtcs){
			elm.addAll(dtcsToAdd);
		}
		for(int i = 0; i < numActivities; i++){
			String currAct = acts.get(i);
			System.out.println("adding health after activity "+ currAct);
			Timepoint curr = dtp.getTimepoint(currAct+"_E");;
			ArrayList<DisjunctiveTemporalConstraint> currDTC = dtcs.get(i);
			// the earliest start time for HM activity is the earliest endtime of current activity
			int est = dtp.getEarliestEndTime(curr);
			int lst = dtp.getLatestEndTime(curr);
			eStart.add(new TemporalDifference(zero, h_s, -est));
			lStart.add(new TemporalDifference(h_s, zero, lst)); 
			eEnd.add(new TemporalDifference(zero, h_e, -(est + duration)));
			lEnd.add(new TemporalDifference(h_e, zero, lst + duration));
			tdVec.add(eStart); tdVec.add(lStart); tdVec.add(eEnd); tdVec.add(lEnd);
			dtcsToAdd.clear();
			dtcsToAdd = DisjunctiveTemporalConstraint.crossProduct(tdVec);
			currDTC.addAll(dtcsToAdd);
			//TODO: do I need to add in ordering constraints and nonconcurrency constraints?? 
			//ordering I think yes..
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(new TemporalDifference(curr, h_s, 0));
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(new TemporalDifference(h_s, curr, 0));
			currDTC.add(dtc2); currDTC.add(dtc1);
		}
		return dtcs;
	}
}
