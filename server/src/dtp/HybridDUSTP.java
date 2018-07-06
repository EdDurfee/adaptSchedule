package dtp;

import interactionFramework.MappedPack;
import interval.Interval;
import interval.IntervalSet;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import stp.TemporalDifference;
import stp.Timepoint;
import util.Node;




public class HybridDUSTP implements DisjunctiveTemporalProblem {

	// Array lists of integers representing component numbers are associated with DTrees that 
	// should be used in case of each placement of the sporadic event. I.e. component 3 refers to
	// a specific placement of the sporadic event. 
	HashMap<ArrayList<Integer>, Node<ArrayList<SimpleDTP>>> hybrid;
	boolean DC = true;
	
	
	public HybridDUSTP(ArrayList<DUSTP> nonDCComps){
		//a function to generate hybrid solutions from a list of nonDC component DUSTPs
		//won't this possibly generate a list of HybridDUSTPs? 
	}
	
	public HybridDUSTP(ArrayList<DUSTP> nonDC, ArrayList<DUSTP> DCcomps){
		//need a function that can do combinations of DC/nonDC hybrid answers
	}
	
	/**
	 * Constructor to use with clone function.
	 * 
	 */
	public HybridDUSTP(HashMap<ArrayList<Integer>, Node<ArrayList<SimpleDTP>>> hyb){
		this.hybrid = hyb;
	}
	
	public boolean DCForwardPassHybrid(){
		//TODO: proofread this again.
		ArrayList<ArrayList<Integer>> compLists = new ArrayList<ArrayList<Integer>>();
		compLists.addAll(hybrid.keySet());
		ArrayList<SimpleEntry<Integer,Integer>> branchPoints = new ArrayList<SimpleEntry<Integer,Integer>>();
		for(ArrayList<Integer> bunch : compLists){
			Collections.sort(bunch);
			int last = bunch.size()-1;
			branchPoints.add(new SimpleEntry<Integer,Integer>(bunch.get(0), bunch.get(last)));
		}
		System.out.println("branch points, first lb should be zero: " + branchPoints);
		
		
		//the curr nodes here are mantained within the hybrids structure. 
		// first we get out of the root and into the first leftchild.
		advanceDownHybridTree();
		int curr_component = 0;
		int curr_depth = 0; //should this be 1?
		//what is the currNode here though. are we like.. advancing through the components? 
		// what goes in this while loop? UPDATE me
		while(hybrid.values().iterator().next() != null){
			
			//if we're at an idlenode, we want to continue. 
			// all nodes should be idle nodes if they have a sporadic event. 
			// we're assuming that all trees in the hashmap are at the same level
			if(hybrid.values().iterator().next().getNodeData().get(0).idleNode){
				advanceDownHybridTree();
				//curr_component++;
				curr_depth++;
				continue;
			}
			
			if(curr_depth > branchPoints.get(curr_component).getValue()){
				// if we've advanced past the first list of numbers. 
				curr_component++;
			}
			
			//ok now we need a list of valid comps that we get the intervals from
			// we always propagate the decisions from component i to components i+1...n
			// but no other component impacts the tightening that we're doing here (i don't think)
			
			ArrayList<SimpleDTP> validComps = new ArrayList<SimpleDTP>();
			// now we just get these from the current component... onwards?
			for(int j = curr_component; j < branchPoints.size(); j++){
				// get all future comps too
				validComps.addAll(hybrid.get(j).getNodeData());
			}
			
			String act_name = validComps.get(0).se_act;
			
			ArrayList<IntervalSet> iss = new ArrayList<IntervalSet>();
			for(SimpleDTP comp : validComps){
				iss.add(comp.getInterval("zero", act_name + "_S").inverse()); //TODO: CHECK! might not need inverse here
			
			}
			IntervalSet intersection = new IntervalSet();
			//compute intersection of the interval.
			//System.out.println("Interval set is: " +iss);
			if(iss.size() == 1){
				intersection = iss.get(0);
			}else{
				intersection = iss.get(0).intersection(iss.get(1));
			
				for(int i = 1; i < iss.size(); i++){
					intersection = intersection.intersection(iss.get(i));
				}
			}
			//System.out.println("INTERSECTION: " + intersection);
			
			//return false if there is no intersection. Otherwise add a constraint to every component w the new EST 
			// what does intersect return if there is no intersection? empty interval.
			if(intersection.isNull()){
				//System.out.println("NO INTERSECTION between " + iss);
				return false;
			}
			
			//next we tighten.
			int lb = (int) intersection.getLowerBound();

			for(SimpleDTP comp : validComps){
				//System.out.println("COMPONENT Before updating: ");
				//Generics.printDTP(comp);
				Timepoint act_start = comp.getTimepoint(act_name + "_S");
				Timepoint zero = comp.getTimepoint("zero");
				TemporalDifference td1 = new TemporalDifference(zero, act_start, -lb);
				//TemporalDifference td2 = new TemporalDifference(act_start, zero, lb);
				DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
				//DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
				comp.addAdditionalConstraint(dtc1);
				//comp.addAdditionalConstraint(dtc2);
				comp.updateInternalData();
				comp.enumerateSolutions(0);
				comp.simplifyMinNetIntervals();
				//System.out.println("COMPONENT after updating: ");
				//Generics.printDTP(comp);
			}
			//should we check here for problems w zero solutions? it would just be a sanity check. shouldn't
			 // actually happen since we picked an interval in the intersection. 
			advanceDownHybridTree();
			curr_depth++;
			
		}
		
		return true;
	}
	
	public boolean DCBackwardPassHybrid(){
		ArrayList<ArrayList<Integer>> compLists = new ArrayList<ArrayList<Integer>>();
		compLists.addAll(hybrid.keySet());
		ArrayList<SimpleEntry<Integer,Integer>> branchPoints = new ArrayList<SimpleEntry<Integer,Integer>>();
		for(ArrayList<Integer> bunch : compLists){
			Collections.sort(bunch);
			int last = bunch.size()-1;
			branchPoints.add(new SimpleEntry<Integer,Integer>(bunch.get(0), bunch.get(last)));
		}
		
		//first advance all DUSTPs to the leftmost child
		boolean advance = true;
		int curr_depth = 0;
		while(advance){
			advance = advanceDownHybridTree();
			curr_depth++;
		} //this should get us so that all nodes are at their leftmost child.
		int curr_component = branchPoints.size();
		
		while(hybrid.values().iterator().hasNext()){
			
			if(hybrid.values().iterator().next().getNodeData().get(0).idleNode){
				advanceUpHybridTree();
				curr_depth--;
				continue;
			}
			
			
			if(curr_depth < branchPoints.get(curr_component).getValue()){
				// if we've advanced past the last list of numbers. 
				curr_component--;
			}
			
			
			ArrayList<SimpleDTP> validComps = new ArrayList<SimpleDTP>();
			for(int j = curr_component; j < branchPoints.size(); j++){
				validComps.addAll(hybrid.get(j).getNodeData());
			}
			String act_name = validComps.get(0).se_act;
			
			ArrayList<IntervalSet> iss = new ArrayList<IntervalSet>();
			for(SimpleDTP comp : validComps){
				iss.add(comp.getInterval("zero", act_name+"_E").inverse());
			}
			
			IntervalSet intersection = new IntervalSet();
			
			if(iss.size() == 1){
				intersection = iss.get(0);
			}else{
				intersection = iss.get(0).intersection(iss.get(1));
				
				for(int i = 1; i < iss.size(); i++){
					intersection = intersection.intersection(iss.get(i));
				}
			}
			
			int ub = (int) intersection.getUpperBound();
			for(SimpleDTP comp : validComps){
				//System.out.println("COMPONENT before updating: ");
				//Generics.printDTP(comp);
				Timepoint act_st = comp.getTimepoint(act_name + "_S");
				Timepoint zero = comp.getTimepoint("zero");
				TemporalDifference td1 = new TemporalDifference(act_st, zero, ub);
				DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
				comp.addAdditionalConstraint(dtc1);
				comp.updateInternalData();
				comp.enumerateSolutions(0);
				comp.simplifyMinNetIntervals();
				//System.out.println("COMPONENT after updating: ");
				//Generics.printDTP(comp);
			}
			
			advanceUpHybridTree();
			curr_depth--;
		}
		
		return true;
	}
	
	private boolean advanceDownHybridTree(){
		boolean ret = true;
		for(Node<ArrayList<SimpleDTP>> tree : hybrid.values()){
			if(tree.getLeftChild() != null){
				tree = tree.getLeftChild();
			}else{
				ret = false;
			}
		}
		return ret;
	}
	
	private boolean advanceUpHybridTree(){
		boolean ret = true;
		for(Node<ArrayList<SimpleDTP>> tree : hybrid.values()){
			if(tree.getParent() != null){
				tree = tree.getParent();
			}else{
				ret = false;
			}
		}
		return ret;
	}
	
	
	public boolean checkDC(){
		boolean fwd = DCForwardPassHybrid();
		if(!fwd) return false;
		boolean bkwd = DCBackwardPassHybrid();
		
		if(fwd && bkwd) DC = true;
		return fwd && bkwd;
	}
	
	
	@Override
	public void enumerateSolutions(int scheduleTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean nextSolution(int scheduleTime) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void enumerateInfluences(int scheduleTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean nextInfluence(int scheduleTime) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void establishMinimality(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean solveNext(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void advanceToTime(int time, int deltaTime, boolean pushSelection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void advanceToTime(int time, boolean resolve, int deltaTime,
			boolean pushSelection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, int deltaTime, boolean pushSelection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeAndAdvance(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAdditionalConstraint(String tpS, String tpE,
			IntervalSet dtc, int time, boolean resolve, boolean pushSelection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAdditionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addVolitionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addVolitionalConstraint(DisjunctiveTemporalConstraint cons) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean fixZeroValIntervals() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void simplifyMinNetIntervals() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMinTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getMinTimeArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getMaxSlack() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntervalSet getInterval(String tp1, String tp2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Timepoint> getTimepoints() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentAgent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setCurrentAgent(int agent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getAgent(String tpS) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void softReset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearIncomingConstraints() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addIncomingConstraint(DisjunctiveTemporalConstraint cons) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addIncomingConstraints(
			Collection<DisjunctiveTemporalConstraint> constraints) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setIncomingConstraints(
			ArrayList<DisjunctiveTemporalConstraint> constraints) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int popSelection(int time, boolean resolve) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<List<String>> getActivities(ActivityFinder af, int time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printFixedIntervals(PrintStream out, int time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean existsFixedTimepoint() {
		// TODO Auto-generated method stub
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
	public Stack<DisjunctiveTemporalConstraint> getAdditionalConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTimepoint(Timepoint tp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addContingentTimepoint(Timepoint source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getValidity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateValidity(int val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> getContingentActivities(int time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateValidity(DisjunctiveTemporalProblem currDTP, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateInternalData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<Timepoint> getFixedTimepoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getMinSlack(int scheduleTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCallsToSolver() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<String> getUnFixedTimepoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int checkBookends(int duration) {
		// TODO Auto-generated method stub
		return 0;
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
	public ArrayList<DisjunctiveTemporalConstraint> getSpecificTempConstraints(
			Timepoint tp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getTempConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<MappedPack> getRelatedInputPairs(String crucial) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addNewTimepoint(Timepoint tp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<Interval> getDTPBoundaries() {
		// TODO Auto-generated method stub
		return null;
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
	
	public HybridDUSTP clone(){
		// save any additional values in fields you add! 
		return new HybridDUSTP(this.hybrid);
	}

}
