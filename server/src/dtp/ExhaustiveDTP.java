package dtp;

import interactionFramework.Generics;
import interactionFramework.MappedPack;
import interactionFramework.Viz;
import interval.Interval;
import interval.IntervalSet;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import stp.TemporalDifference;
import stp.Timepoint;

public class ExhaustiveDTP implements DisjunctiveTemporalProblem, java.io.Serializable {
	
	/**
	 * NOTE: THIS CLASS IS DEFUNCT, but is kept around for posterity.
	 */
	private static final long serialVersionUID = -703835962637634949L;
	public HashSet<DisjunctiveTemporalProblem> dtps;
	ArrayList<ArrayList<DisjunctiveTemporalConstraint>> exhaustiveConstraint;
	public DisjunctiveTemporalProblem originalDTP;
	
	public ExhaustiveDTP(){
		this.dtps = new HashSet<DisjunctiveTemporalProblem>();
	}
	
	//constructor written mostly for cloning
	public ExhaustiveDTP(HashSet<DisjunctiveTemporalProblem> dtps){
		this.dtps = dtps;
	}
	
	public ExhaustiveDTP(DisjunctiveTemporalProblem orig, ArrayList<ArrayList<DisjunctiveTemporalConstraint>> exhaus, HashSet<DisjunctiveTemporalProblem> dtps){
		this.originalDTP = orig;
		this.exhaustiveConstraint = exhaus;
		this.dtps = dtps;
		//int count = 0;
		//System.out.println("OriginalDTP has this many tps "+ originalDTP.getTimepoints().size());
		//Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		//while(it.hasNext()){
			//DisjunctiveTemporalProblem curr = it.next();
			//System.out.println(count);
			//for(DisjunctiveTemporalConstraint dtc: exhaustiveConstraint.get(count)){
				//System.out.println(dtc.toString());
			//}
			//for(int i = 0; i < 5; i++) curr.addAdditionalConstraint(exhaustiveConstraint.get(count).get(i));
			//curr.addAdditionalConstraints(exhaustiveConstraint.get(count));
			//System.out.println("DTP "+ count +" has this many tps "+ numtp);
			//count++;
		//}
		this.updateInternalData();
	}
	
	public HashSet<DisjunctiveTemporalProblem> getDTPs(){
		return this.dtps;
	}
	
//	public ExhaustiveDTP(DisjunctiveTemporalProblem dtp, ArrayList<ArrayList<DisjunctiveTemporalConstraint>> cons){
//		this.dtps = new HashSet<DisjunctiveTemporalProblem>();
//		// something like a for loop that adds one component of the "Exhaustive constraint" cons to a clone of the dtp then adds it to the HashSet. 
//		
//		for(ArrayList<DisjunctiveTemporalConstraint> c : cons){
//			DisjunctiveTemporalProblem newDTP = dtp.clone();
//			newDTP.addAdditionalConstraints(c);// is this what we want? add it to additional? does it need to be in temp? volitional? 
//			//TODO add addVolitionalConstraints() function to DTP interface and implement? 
//			getContingentTimepointsAndAdd(newDTP, c);
//			newDTP.updateInternalData();
//			//Viz.printDTPDiagram(newDTP, 300);
//			dtps.add(newDTP);
//			
//		}
//		//Viz.printDTPDiagram(dtp, 300);
//		this.originalDTP = dtp;
//		this.exhaustiveConstraint = cons;
//	}
	
	private void getContingentTimepointsAndAdd(DisjunctiveTemporalProblem dtp, ArrayList<DisjunctiveTemporalConstraint> dtcs){
		Set<Timepoint> tps = dtp.getTimepoints();
		Set<Timepoint> tpsAdded = new HashSet<Timepoint>();
		for(DisjunctiveTemporalConstraint dtc : dtcs){
			for(TemporalDifference td : dtc.getTemporalDifferences()){
				if(tps.contains(td.source) || tpsAdded.contains(td.source)); 
				else {
					//System.out.println("adding td source " +td.source.getName());
					dtp.addContingentTimepoint(td.source);
					tpsAdded.add(td.source);
				}
				if(tps.contains(td.destination)|| tpsAdded.contains(td.destination)); 
				else {
					//System.out.println("adding td dest " + td.destination.getName());
					dtp.addContingentTimepoint(td.destination);
					tpsAdded.add(td.destination);
				}
				tps = dtp.getTimepoints();
			}
		}
	}
	
	
	
	@Override
	public void enumerateSolutions(int scheduleTime) {
		originalDTP.enumerateSolutions(scheduleTime);
		//System.out.println("finished enumeratingSolutions originalDTP");
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) {
				curr.enumerateSolutions(scheduleTime);
			}
		}
//		DisjunctiveTemporalProblem curr = it.next();
//		curr.updateInternalData();
//		curr.enumerateSolutions(scheduleTime);

	}

	@Override
	public boolean nextSolution(int scheduleTime) {
		originalDTP.nextSolution(scheduleTime);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		boolean r = false;
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) r = curr.nextSolution(scheduleTime);
			res.add(r);
		}
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void enumerateInfluences(int scheduleTime) {
		originalDTP.enumerateInfluences(scheduleTime);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.enumerateInfluences(scheduleTime);
		}
	}

	@Override
	public boolean nextInfluence(int scheduleTime) {
		originalDTP.nextInfluence(scheduleTime);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		boolean r = false;
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) r = curr.nextInfluence(scheduleTime);
			res.add(r);
		}
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void establishMinimality(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		originalDTP.establishMinimality(timepointsToConsider, scheduleTime);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.establishMinimality(timepointsToConsider, scheduleTime);
		}
	}

	@Override
	public boolean solveNext(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		originalDTP.solveNext(timepointsToConsider, scheduleTime);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		boolean r = false;
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.solveNext(timepointsToConsider, scheduleTime);
			res.add(r);
		}
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void advanceToTime(int time, int deltaTime, boolean pushSelection) {
		originalDTP.advanceToTime(time, deltaTime, pushSelection);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.advanceToTime(time, deltaTime, pushSelection);
		}
	}

	@Override
	public void advanceToTime(int time, boolean resolve, int deltaTime,
			boolean pushSelection) {
		originalDTP.advanceToTime(time, resolve, deltaTime, pushSelection);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.advanceToTime(time, resolve, deltaTime, pushSelection);
		}

	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, int deltaTime, boolean pushSelection) {
		originalDTP.tightenTimepoint(timeStart, tp1, timeEnd, tp2, deltaTime, pushSelection);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.tightenTimepoint(timeStart, tp1, timeEnd, tp2, deltaTime, pushSelection);
		}

	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		originalDTP.tightenTimepoint(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.tightenTimepoint(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		}

	}

	@Override
	public void executeAndAdvance(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		originalDTP.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		}
	}

	@Override
	public void addAdditionalConstraint(String tpS, String tpE,
			IntervalSet dtc, int time, boolean resolve, boolean pushSelection) {
		originalDTP.addAdditionalConstraint(tpS, tpE, dtc, time, resolve, pushSelection);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addAdditionalConstraint(tpS, tpE, dtc, time, resolve, pushSelection);
		}

	}

	@Override
	public void addAdditionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		originalDTP.addAdditionalConstraints(col);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addAdditionalConstraints(col);
		}

	}

	@Override
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons) {
		originalDTP.addAdditionalConstraint(cons);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addAdditionalConstraint(cons);
		}

	}

	@Override
	public boolean fixZeroValIntervals() {
		originalDTP.fixZeroValIntervals();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		boolean r = false;
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) r=curr.fixZeroValIntervals();
			res.add(r);
		}
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void simplifyMinNetIntervals() {
		originalDTP.simplifyMinNetIntervals();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.simplifyMinNetIntervals();
		}

	}

	@Override
	public int getMinTime() {
	// should this return the max of the min times for the component DTPs??
		ArrayList<Integer> minTimes = new ArrayList<Integer>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) {
			//	System.out.println(curr.getMinTime());
				minTimes.add(curr.getMinTime());
			}
		}
		if(originalDTP.getValidity() == 1) minTimes.add(originalDTP.getMinTime());
	//	System.out.println(originalDTP.getMinTime());
	//	System.out.println("MIN TIMES: "+minTimes);
		return Collections.min(minTimes);
	}

	@Override
	public int[] getMinTimeArray() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public List<Integer> getMaxSlack(int time){
		
		ArrayList<Integer> maxSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1){
				int slack = curr.getMaxSlack().get(0);
				System.out.println("maxSlack of one component is: " + slack);
				List<List<String>> actBefore = curr.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -time);
				List<List<String>> actAfter = curr.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -(time+slack));
				if(actBefore.get(0).equals(actAfter.get(0))) {
					//System.out.println("actbefore : "+actBefore);
					//System.out.println("actafter : "+actAfter);
					maxSlacks.add(slack);
				}
				else maxSlacks.add(0);
			}
		}
		maxSlacks.add(originalDTP.getMaxSlack().get(0));
		//System.out.println("maxslack: "+maxSlacks);
		
		ret.add(Collections.min(maxSlacks));
		return ret;
	}
	
	@Override
	public List<Integer> getMaxSlack() {
		ArrayList<Integer> maxSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1){
				//Generics.printDTP(curr);
				maxSlacks.add(curr.getMaxSlack().get(0));
			}
		}
		maxSlacks.add(originalDTP.getMaxSlack().get(0));
		//Generics.printDTP(originalDTP);
		//System.out.println("maxslack: "+maxSlacks);
		//System.out.println("max slacks are: " + maxSlacks);
		ret.add(Collections.min(maxSlacks));
		return ret;
	}

	@Override
	public IntervalSet getInterval(String tp1, String tp2) {
		// TODO we need to get the.... intersection of the intervals from all of the component DTPs. Pretty sure. 
		IntervalSet ret = new IntervalSet();
		ArrayList<IntervalSet> coll = new ArrayList<IntervalSet>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			IntervalSet is = curr.getInterval(tp1, tp2);
			coll.add(is);
		}
		ret = coll.get(0).intersection(coll.get(1));
		for(int i = 1; i < coll.size(); i++){
			ret = ret.intersection(coll.get(i));
		}
		return ret;
	} 

	@Override
	public Set<Timepoint> getTimepoints() {
		return originalDTP.getTimepoints();
	}

	@Override
	public ArrayList<Timepoint> getInterfaceTimepoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timepoint getTimepoint(String tpS) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(String tp1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getNumSolutions() {
		ArrayList<Integer> solns = new ArrayList<Integer>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			solns.add(curr.getNumSolutions());
		}
		
		return Collections.min(solns);
	}

	@Override
	public long getTotalFlexibility() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRigidity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumAgents() {
		return originalDTP.getNumAgents();
	}

	@Override
	public int getCurrentAgent() {
		return originalDTP.getCurrentAgent();
	}

	@Override
	public void setCurrentAgent(int agent) {
		originalDTP.setCurrentAgent(agent);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.setCurrentAgent(agent);
		}

	}

	@Override
	public int getAgent(String tpS) {
		// leaving this one this way because it might be a contingent time point... though this still isn't handling that right
		//TODO: fix this to actually find the agent for the given timepoint if its contingent nad maybe only in one DTP
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		if(it.hasNext()){
			return it.next().getAgent(tpS);
		}else{
			return -1; // this is an error really
		}
	}

	@Override
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void softReset() {
		originalDTP.softReset();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.softReset();
		}
	}

	@Override
	public void clearIncomingConstraints() {
		originalDTP.clearIncomingConstraints();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.clearIncomingConstraints();
		}

	}

	@Override
	public void addIncomingConstraint(DisjunctiveTemporalConstraint cons) {
		originalDTP.addIncomingConstraint(cons);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addIncomingConstraint(cons);
		}

	}

	@Override
	public void addIncomingConstraints(
			Collection<DisjunctiveTemporalConstraint> constraints) {
		originalDTP.addIncomingConstraints(constraints);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addIncomingConstraints(constraints);
		}

	}

	@Override
	public void setIncomingConstraints(
			ArrayList<DisjunctiveTemporalConstraint> constraints) {
		originalDTP.setIncomingConstraints(constraints);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.setIncomingConstraints(constraints);
		}

	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> computeSummarizingConstraints(
			ArrayList<Timepoint> timepointsToDecouple) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> computeDecouplingConstraints(
			ArrayList<Timepoint> timepointsToDecouple) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addInterfaceTimepoint(Timepoint tp) {
		originalDTP.addInterfaceTimepoint(tp);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addInterfaceTimepoint(tp);
		}

	}

	@Override
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
		originalDTP.addInterfaceTimepoints(timepoints);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addInterfaceTimepoints(timepoints);
		}

	}

	@Override
	public void printTimepointIntervals(PrintStream out) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printTimepointIntervals(PrintStream out, int time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printConstraints(PrintStream out) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printSelectionStack() {
		// TODO Auto-generated method stub

	}

	@Override
	public int popSelection(int time) {
		originalDTP.popSelection(time);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.popSelection(time);
		}
		return 0; 
		//TODO: fix this. it should return the amount the system time was changed... 
	}

	@Override
	public int popSelection(int time, boolean resolve) {
		originalDTP.popSelection(time, resolve);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			it.next().popSelection(time, resolve);
		}
		return 0;
		//TODO: same as function above. 
	}

	@Override
	public List<List<String>> getActivities(ActivityFinder af, int time) {
		//TODO: make sure this doesn't include contingent activities!
		// actually it shouldn't bc they won't be available for all the component DTPs
		String out_str = "";
		List<List<String>> result = new LinkedList<>();
		List<List<String>> ret = new LinkedList<>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){ // accumulate a list of all possible activities.
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1){
				ArrayList<String> activities = new ArrayList<String>();
				//System.out.println(curr.getActivities(af, time).get(0));
				if(curr.getActivities(af, time).get(0).size() == 0){
					//System.out.println("NO VALID ACTIVITIES");
					Generics.printDTP(curr);
				}
				if(curr.getActivities(af, time).get(0).contains("health")){
					// we want to prevent this. can we just make it invalid?
					curr.updateValidity(0);
					continue;
				}
				activities.addAll(curr.getActivities(af, time).get(0));
				result.add(activities);
				out_str += ((SimpleDTP) curr).getID() + " : "+ activities;
				//System.out.println(result);
			}
		}
		//System.out.println(result);
		ret.add(Generics.intersect(result));
		return ret;
	}
	
	//FIXME: why in the world did i write this function.
	// I think now (update) that i'm going to reqrite it to make all the dtps invalid except 
	// for the one that is given as an argument
	public void updateValidity(DisjunctiveTemporalProblem dtp, int val){
		originalDTP.updateValidity(0);
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem dtp2 = it.next();
			 if(!dtp.equals(dtp2)){
				dtp2.updateValidity(val);
			}
		}
	}
	

	@Override
	public void printFixedIntervals(PrintStream out, int time) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean existsFixedTimepoint() {
		originalDTP.existsFixedTimepoint();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		boolean r = false;
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) r = curr.existsFixedTimepoint();
			res.add(r);
		}
		for(boolean elm : res){
			if(elm == true) return true;
		}
		return false;
	}

	@Override
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP,
			int threshold) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP,
			DeltaFinder df) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP,
			double relThreshold) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Delta> rankDeltas(DisjunctiveTemporalProblem prevDTP,
			int rankLim) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Delta> rankRelativeDeltas(
			DisjunctiveTemporalProblem prevDTP, int rankLim) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printDeltas(ArrayList<Delta> deltas) {
		// TODO Auto-generated method stub

	}


	@Override
	public Stack<DisjunctiveTemporalConstraint> getAdditionalConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

	public DisjunctiveTemporalProblem clone(){
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		HashSet<DisjunctiveTemporalProblem> hs = new HashSet<DisjunctiveTemporalProblem>();
		while(it.hasNext()){
			
			hs.add(it.next().clone());
		}
		ArrayList<ArrayList<DisjunctiveTemporalConstraint>> newEC = new ArrayList<ArrayList<DisjunctiveTemporalConstraint>>();
		ArrayList<DisjunctiveTemporalConstraint> temp = new ArrayList<DisjunctiveTemporalConstraint>();
		for(ArrayList<DisjunctiveTemporalConstraint> c : exhaustiveConstraint){
			for(DisjunctiveTemporalConstraint dtc : c){
				temp.add(dtc.clone());
			}
			newEC.add(temp);
			temp.clear();
		}
		ExhaustiveDTP newDTPs = new ExhaustiveDTP(hs);
		newDTPs.originalDTP = originalDTP.clone();
		newDTPs.exhaustiveConstraint = newEC;
		return newDTPs;
	}

	@Override
	public void addTimepoint(Timepoint tp) {
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1) curr.addTimepoint(tp);
		}
	}
	

	@Override
	public void addContingentTimepoint(Timepoint source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	//is the validity of the WHOLE exhaustive DTP, the or over the validities? 
	public int getValidity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateValidity(int val) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	public HashMap<String, DisjunctiveTemporalProblem> getContingentActivitiesMap(int time){
		HashMap<String, DisjunctiveTemporalProblem> map = new HashMap<String, DisjunctiveTemporalProblem>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			List<String> acts = curr.getContingentActivities(time);
			//System.out.println("Found contingent acts at time " + time +" : "+acts);
			if(acts.size() == 0) continue;
			for(String elm : acts) map.put(elm, curr);
		}
		return map;
	}

	@Override
	public List<String> getContingentActivities(int time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLatestEndTime(Timepoint tp) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getEarliestEndTime(Timepoint tp) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateInternalData() {
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			it.next().updateInternalData();
		}
		originalDTP.updateInternalData();
		
	}

	@Override
	public void addVolitionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addVolitionalConstraint(DisjunctiveTemporalConstraint cons) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<Timepoint> getFixedTimepoints() {
		// TODO Auto-generated method stub
		return originalDTP.getFixedTimepoints();
	}

	@Override
	public List<Integer> getMinSlack(int scheduleTime) {
		ArrayList<Integer> minSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			if(curr.getValidity() == 1){
				minSlacks.add(curr.getMinSlack(scheduleTime).get(0));
			}
		}
		minSlacks.add(originalDTP.getMinSlack(scheduleTime).get(0));
		
		ret.add(Collections.min(minSlacks));
		return ret;
	}

	@Override
	public int getCallsToSolver() {
		int total = 0;
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			total+= it.next().getCallsToSolver();
		}
		return total;
	}

	@Override
	public List<String> getUnFixedTimepoints() {
		List<String> unfixed = new ArrayList<String>();
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalProblem curr = it.next();
			List<String> tps = curr.getUnFixedTimepoints();
			for(String t : tps){
				if(!unfixed.contains(t)) unfixed.add(t);
			}
		}
		return unfixed;
	}

	@Override
	/** This returns zero because there are no bookends in an Exhaustive DTP.
	 * 
	 */
	public int checkBookends(int duration) {
		return 0;
	}
	
	public DisjunctiveTemporalProblem getComponentDTP(int i){
		Iterator<DisjunctiveTemporalProblem> it = dtps.iterator();
		for(int j = 0; j < i; j++){
			it.next();
		}
		return it.next();
	}

	@Override
	public double getOriginalUpperBound(Timepoint tp) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getOriginalLowerBound(Timepoint tp) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getSpecificTempConstraints(Timepoint tp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getTempConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<MappedPack> getRelatedInputPairs(String crucial) {
		// TODO Auto-generated method stub
		System.out.println("in getRelatedInputPairs in ExhaustiveDTP.java...shouldn't be here");
		return null;
	}

	@Override
	public void addNewTimepoint(Timepoint tp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<Interval> getDTPBoundaries() {
		// TODO Auto-generated method stub
		return new ArrayList<Interval>();
	}

	@Override
	public void addNonconcurrentConstraint(String source, String dest, int agent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<String> getActivityNames(int agent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFixedTimepoints(Collection<Timepoint> tps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTimepointName(Timepoint tp, String new_name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addDurationConstraint(Timepoint start, Timepoint end,
			int duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addOrderingConstraint(String source, String dest,
			int min_duration, int max_duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDurationConstraint(String tpName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> getComponentSTPs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void advanceDownTree() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SimpleEntry<Integer, Integer> getMinSlackInterval(int scheduleTime) {
		// TODO Auto-generated method stub
		return null;
	}

}
