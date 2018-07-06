package dtp;

import interactionFramework.Generics;
import interactionFramework.MappedPack;
import interval.Interval;
import interval.IntervalSet;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import stp.TemporalDifference;
import stp.Timepoint;
import util.Node;

public class DUSTP implements DisjunctiveTemporalProblem{
	/**
	 * This class provides the implementation of a Disjunctively-Uncertain Temporal Problem in which the underlying temporal problem in an STP. 
	 * Note that this class does not do any error checking regarding the input underlying problem, but unexpected behavior will occur if given a DTP instead of an STP due to this class relying on the assumption of a strict ordering of activities in the underlying temporal problem. 

	 */
	public static  String SPORADICNAME = "Sporadic";
	public static  int DUR = 20; //duration of the sporadic event
	public static  int SporadicIntervalLength = 600; //length of scheduling period for sporadic interval -- DEPRECATED, replaced by interval below
	public static  int SporadicEventET = 0;  //earliest time of sporadic event
	public static  int SporadicEventLT = 1440;  //latest time of sporadic event (NOTE: sporadic activity could happen later, depending on precendence)
	public static  int MaxSporadicActivityEndTimeAfterSporadicEvent = 1440;  //Even though the SE can't happen after the SporadiceEventLT, the activity to address it can, depending on precedence
	public SimpleDTP stp = null; // underlying stp problem
	public ArrayList<SimpleDTP> componentSTPs = new ArrayList<SimpleDTP>(); //component STPs representing the various placements of the sporadic activity
	public Node<ArrayList<SimpleDTP>> DTree = null; //DPTree structure used for solving DUSTPs
	public Node<ArrayList<SimpleDTP>> currentNode = null; // keeps track of where we are during execution 
	public int VALIDITY = 1;
	public int precedence = -1;
	public boolean DC = false; //toggled once this has been checked, if true
	public ArrayList<Integer> se_times = new ArrayList<Integer>(); //structure to hold the problematic placements of the sporadic event 
																	// in the case of hybrid solutions. Contains indices referring to components 
																	// in the componentSTPs list.
	
	public HashMap<SimpleEntry<String,String>, Integer> precMap = new HashMap<SimpleEntry<String, String>, Integer>();
	
	private int iters = 0;
	
	public DUSTP(SimpleDTP underlying){
		this.stp = underlying;
		//generate the components based on representation assumptions
		componentSTPs = populateISTPs();
		//System.out.println("component STPs generated");
		DTree = generateDCTree();
		//System.out.println("DC tree generated");
		currentNode = DTree;
	}
	
	/*
	 * This constructor should be used when the sporadic event interacting with required activities always has the same 
	 * precedence level. The precedence map will be populated with the same value everywhere.
	 * @param underlying the underlying stp problem
	 * @param prec can be 0, 1, or 2, representing the precedence relation between SA and required activities
	 */
	public DUSTP(SimpleDTP underlying, int prec){
		this.stp = underlying;
		this.precedence = prec;
		populatePrecedenceMap(prec);
		//System.out.println("PrecMap populated, next populating ISTPs");
		componentSTPs = populateISTPs();
		DTree = generateDCTree();
		currentNode = DTree;
		printDTree(currentNode,1);
	}
	/*
	 * This constructor should be used when the sporadic event interacting with required activities always has the same 
	 * precedence level. The precedence map will be populated with the same value everywhere.
	 * @param underlying the underlying stp problem
	 * @param prec can be 0, 1, or 2, representing the precedence relation between SA and required activities
	 * This constructor also includes a timepoint for exactly when the sporadic event is modeled to occur
	 */
	public DUSTP(SimpleDTP underlying, int prec, boolean many){
		this.stp = underlying;
		this.precedence = prec;
		populatePrecedenceMap(prec);
		//System.out.println("PrecMap populated, next populating ISTPs");
		componentSTPs = populateISTPsMany();
		DTree = generateDCTree();
		currentNode = DTree;
		printDTree(currentNode,1);
	}
	
	/**
	 * Constructor that is used when we want the expanded version of the structure, without the use of equivalence classes.
	 * @param underlying, underlying stp problem
	 * @param map, the precedence map, already populated with the appropriate values
	 * @param many, a boolean value, always true.
	 */
	public DUSTP(SimpleDTP underlying, HashMap<SimpleEntry<String,String>, Integer> map, boolean many){
		this.stp = underlying;
		this.precMap = map;
		componentSTPs = populateISTPsMany();
//		cleanupCompSTPs();
		//generate Dtree
		DTree = generateDCTree();
		currentNode = DTree;
		//set current Node
	}
	
	public DUSTP(SimpleDTP underlying, ArrayList<SimpleDTP> cstps){
		this.stp = underlying;
		this.componentSTPs = cstps;
		DTree = generateDCTree();
		currentNode = DTree;
	}
	
	public DUSTP(ArrayList<SimpleDTP> iSTPs){
		this.stp = null;
		this.componentSTPs = null;
		DTree = regenerateDCTree(iSTPs);
		currentNode = DTree;
	}
		
	public DUSTP(SimpleDTP underlying, ArrayList<SimpleDTP> cstps, int prec){
		this.stp = underlying;
		this.componentSTPs = cstps;
		this.precedence = prec;
		populatePrecedenceMap(prec);
		DTree = generateDCTree();
		currentNode = DTree;
		
	}
	
	public DUSTP(SimpleDTP underlying, ArrayList<SimpleDTP> cstps, int prec, HashMap<SimpleEntry<String,String>, Integer> map){
		this.stp = underlying;
		this.componentSTPs = cstps;
		this.precedence = prec;
		this.precMap = map;
		DTree = generateDCTree();
		currentNode = DTree;
		
	}
	
	// constructor used for cloning purposes.
	public DUSTP(SimpleDTP underlying, ArrayList<SimpleDTP> cstps, int prec, HashMap<SimpleEntry<String,String>, Integer> map, ArrayList<Integer> newTimes){
		this.stp = underlying;
		this.componentSTPs = cstps;
		this.precedence = prec;
		this.precMap = map;	
		DTree = generateDCTree();
		currentNode = DTree;
		
		se_times = newTimes;
	}
	
	public DUSTP(SimpleDTP underlying, int prec, HashMap<SimpleEntry<String,String>, Integer> map){
		this.stp = underlying;
		this.precedence = prec;
		this.precMap = map;
		this.componentSTPs = populateISTPs();
		DTree = generateDCTree();
		currentNode = DTree;
		
		
	}
	
	
	/**
	 * Populates a precedence map with every value for the relation being prec.
	 * @param prec the value for the precedence. We expect prec to be 0,1,or 2.
	 */
	private void populatePrecedenceMap(int prec){
//		System.out.println("Populating Precedence map.");
		if(!(prec < 0)){
			//then we populate with all values of this integer. 
			ArrayList<String> acts = this.getActivityNames(0);
			ArrayList<String> sps = new ArrayList<String>();
			sps.add(SPORADICNAME);
			//iterate over both of these lists computing all combinations (not permutations)
			for(int i = 0; i < acts.size(); i++){
				for(int j = 0; j < sps.size(); j++){
					SimpleEntry<String, String> pair = new SimpleEntry<String,String>(acts.get(i), sps.get(j));
					//System.out.println("Adding pair to precMap " + pair);
					this.precMap.put(pair, prec);
				}
			}
		}
	}
	
	public void skipOverSporadicNotDone() {
		currentNode = currentNode.getLeftChild();
	}
	
	public void printDTree(Node<ArrayList<SimpleDTP>> node, int level) {
		System.out.println("LEVEL: " + level);
		printTreeNode(node);
		int nextLevel = level + 1;
		if(!(node.getLeftChild()==null)) {
			System.out.println("LEFT BRANCH FROM " + level + " to " + nextLevel);
			printDTree(node.getLeftChild(), nextLevel);}
		if (!(node.getRightChild()==null)) {
			System.out.println("RIGHT BRANCH FROM " + level + " TO " + nextLevel);
			printDTree(node.getRightChild(), level + 1);
		}
	}
	
	public void printTreeNode(Node<ArrayList<SimpleDTP>> node) {
		for(SimpleDTP stp : node.getNodeData()){
			Generics.printDTP(stp);		
		}

	}
	
	@Override
	public void enumerateSolutions(int scheduleTime) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.enumerateSolutions(scheduleTime);
		}
		
	}

	@Override
	public boolean nextSolution(int scheduleTime) {
	
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			r = comp.nextSolution(scheduleTime);
			res.add(r);
		}
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void enumerateInfluences(int scheduleTime) {
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.enumerateInfluences(scheduleTime);
		}
		
	}

	@Override
	public boolean nextInfluence(int scheduleTime) {
		//boolean orig = stp.nextInfluence(scheduleTime);
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			r = comp.nextInfluence(scheduleTime);
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
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.establishMinimality(timepointsToConsider, scheduleTime);
		}
	}

	@Override
	public boolean solveNext(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			r = comp.solveNext(timepointsToConsider, scheduleTime);
			res.add(r);
		}

		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void advanceToTime(int time, int deltaTime, boolean pushSelection) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.advanceToTime(time, deltaTime, pushSelection);
		}
	}

	@Override
	public void advanceToTime(int time, boolean resolve, int deltaTime,
			boolean pushSelection) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.advanceToTime(time, resolve, deltaTime, pushSelection);
		}
		
	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, int deltaTime, boolean pushSelection) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.tightenTimepoint(timeStart, tp1, timeEnd, tp2, deltaTime, pushSelection);
		}
		
	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.tightenTimepoint(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		}
		
	}

	@Override
	public void executeAndAdvance(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		
//		System.out.println("DUSTP executeAndAdvance number of validComponents is: " + validComps.size());

		if(tp1.equals(SPORADICNAME+"_S")){
			//then we need to switch to the right branch and execute the sporadic event 
			currentNode = currentNode.getRightChild();
			SimpleDTP right = currentNode.getNodeData().get(0);
			
			validComps = currentNode.getNodeData();
			for(SimpleDTP comp : validComps){
				comp.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
			}
		}
		else{ //we're performing a regular activity and then updating the currentNode
			for(SimpleDTP comp : validComps){
				comp.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
				
			}
			if(currentNode.getLeftChild() != null){
//				System.out.println("DUSTP executeAndAdvance Branching to leftchild");
//				System.out.println("DUSTP executeAndAdvance number of validComponents in leftchild: " + currentNode.getLeftChild().getNodeData().size());
				// there are two ways of progressing down: either based on skipping over all of the possible Sporadic Event times that have gone
				//  by, if those have been enumerated, or to just go to the next decision point.
				// We can tell that the SE times have been enumerated if there is a timepoint with the sporadicevent name _T, which is
				//  added when we are doing the "Many" option
				if (validComps.get(0).timepointExists(SPORADICNAME+"_T")) {
					currentNode = currentNode.getLeftChild();
					System.out.println("DUSTP executeAndAdvance brancing to leftchild.  Should do more?");
					System.out.println("Leftchild is ");
					System.out.println(currentNode);
				}
				else {
					currentNode = currentNode.getLeftChild();
				}
			}
		}
//		printDTree(currentNode,1);
//		System.out.println("Executed and Advanced to time: " + timeEnd);
	}

	@Override
	public void addAdditionalConstraint(String tpS, String tpE,
			IntervalSet dtc, int time, boolean resolve, boolean pushSelection) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addAdditionalConstraint(tpS, tpE, dtc, time, resolve, pushSelection);
		}
		
	}

	@Override
	public void addAdditionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addAdditionalConstraints(col);
		}

	}

	@Override
	public void addVolitionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addVolitionalConstraints(col);
		}

	}

	@Override
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addAdditionalConstraint(cons);
		}
	}

	@Override
	public void addVolitionalConstraint(DisjunctiveTemporalConstraint cons) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addVolitionalConstraint(cons);
		}
		
	}

	@Override
	public boolean fixZeroValIntervals() {
		
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			r = comp.fixZeroValIntervals();
			res.add(r);
		}	

		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void simplifyMinNetIntervals() {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.simplifyMinNetIntervals();
		}

	}

	@Override
	public int getMinTime() {

		ArrayList<Integer> minTimes = new ArrayList<Integer>();
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			minTimes.add(comp.getMinTime());
		}

		return Collections.min(minTimes);           //WARNING: ED changed from max to min.  Side-effects??
	}

	@Override
	public int[] getMinTimeArray() {
		// returns an array that contains the min times for all valid components. 
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		int[] result = new int[validComps.size()];
		for(int i = 0; i < validComps.size(); i++) result[i] = validComps.get(i).getMinTime();
		
		return result;
	}

	@Override
	public List<Integer> getMaxSlack() {
		ArrayList<Integer> maxSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			maxSlacks.add(comp.getMaxSlack().get(0));
		}

		ret.add(Collections.min(maxSlacks));
		return ret;
	}
	
	public List<Integer> getMaxSlack(int time) {
		ArrayList<Integer> maxSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		
		Iterator<SimpleDTP> it = componentSTPs.iterator();
		while(it.hasNext()){
			SimpleDTP curr = it.next();
			if(curr.getValidity() == 1){
				int slack = curr.getMaxSlack().get(0);
				
				List<List<String>> actBefore = curr.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -time);
				List<List<String>> actAfter = curr.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -(time+slack));
				if(actBefore.get(0).equals(actAfter.get(0))) {
					maxSlacks.add(slack);
				}
				else maxSlacks.add(0);
			}
		}
		maxSlacks.add(stp.getMaxSlack().get(0));
		ret.add(Collections.min(maxSlacks));
		return ret;
	}

	@Override
	public IntervalSet getInterval(String tp1, String tp2) {
		IntervalSet ret = new IntervalSet();
		ArrayList<IntervalSet> coll = new ArrayList<IntervalSet>();
		
		
		Iterator<SimpleDTP> it = currentNode.getNodeData().iterator();
		while(it.hasNext()){
			SimpleDTP curr = it.next();

			IntervalSet is = curr.getInterval(tp1, tp2);
			coll.add(is);

		}
		if(coll.size() == 1) ret = coll.get(0);
		else{
			ret = coll.get(0).intersection(coll.get(1));
			for(int i = 1; i < coll.size(); i++){
				ret = ret.intersection(coll.get(i));
			}
		}
		return ret;
		
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Timepoint> getTimepoints() {
		return stp.getTimepoints();
	}

	@Override
	public ArrayList<Timepoint> getInterfaceTimepoints() {
		return stp.getInterfaceTimepoints();
	}

	@Override
	public Timepoint getTimepoint(String tpS) {
		return stp.getTimepoint(tpS);
	}

	@Override
	public boolean contains(String tp1) {
		return stp.contains(tp1);
	}

	@Override
	public int getNumSolutions() {
		ArrayList<Integer> solns = new ArrayList<Integer>();
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			solns.add(comp.getNumSolutions());
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
		return stp.getNumAgents();
	}

	@Override
	public int getCurrentAgent() {
		return stp.getCurrentAgent();
	}

	@Override
	public void setCurrentAgent(int agent) {
		stp.setCurrentAgent(agent);
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.setCurrentAgent(agent);
		}
	
	}

	@Override
	public int getAgent(String tpS) {
		// TODO Auto-generated method stub
		//right now we're only working with single agent problems.
		return 0;
	}

	@Override
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void softReset() {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.softReset();
		}
           
	}

	@Override
	public void clearIncomingConstraints() {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.clearIncomingConstraints();
		}

	}

	@Override
	public void addIncomingConstraint(DisjunctiveTemporalConstraint cons) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addIncomingConstraint(cons);
		}
		
	}

	@Override
	public void addIncomingConstraints(
			Collection<DisjunctiveTemporalConstraint> constraints) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addIncomingConstraints(constraints);
		}

	}

	@Override
	public void setIncomingConstraints(
			ArrayList<DisjunctiveTemporalConstraint> constraints) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.setIncomingConstraints(constraints);
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
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addInterfaceTimepoint(tp);
		}
		
	}

	@Override
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addInterfaceTimepoints(timepoints);
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
		List<List<String>> result = new LinkedList<>();
		List<List<String>> ret = new LinkedList<>();
		
//		System.out.println("The number of STPs in currentNode is " + currentNode.getNodeData().size());
//		System.out.println("Activity finder is " + af + " and time is " + time);
		result = getActivitiesFromSet(currentNode.getNodeData(), af, time);
//		System.out.println("Getting DUSTP activities yields " + result);
		if(result == null){
			ret.add(new ArrayList<String>());
		}else{
			ret.add(result.get(0));
		}
		return ret;
		
	}

	@Override
	public void printFixedIntervals(PrintStream out, int time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean existsFixedTimepoint() {
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			r = comp.existsFixedTimepoint();
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
		return currentNode.getNodeData().get(0).getAdditionalConstraints();
	}


	@Override
	public void addTimepoint(Timepoint tp) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addTimepoint(tp);
		}
		
	}

	@Override
	public void addContingentTimepoint(Timepoint source) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addContingentTimepoint(source);
		}
	}

	@Override
	public int getValidity() {
		return VALIDITY;
	}

	@Override
	public void updateValidity(int val) {
		this.VALIDITY = val;
		
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
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.updateInternalData();
		}
	}

	@Override
	public ArrayList<Timepoint> getFixedTimepoints() {
		return currentNode.getNodeData().get(0).getFixedTimepoints();
	}

	@Override
	public List<Integer> getMinSlack(int scheduleTime) {
		ArrayList<Integer> minSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			int slack = comp.getMinSlack(scheduleTime).get(0);
			minSlacks.add(slack);
		}

		ret.add(Collections.min(minSlacks));
		return ret;
	}

	@Override
	public int getCallsToSolver() {
		int total = 0;
		//we want to count up the calls to solver at the root. 
		ArrayList<SimpleDTP> allComps = DTree.getNodeData();
		for(SimpleDTP comp : allComps){
			total += comp.getCallsToSolver();
		}
		
		return total;
	}
	
	

	@Override
	public List<String> getUnFixedTimepoints() {

		return currentNode.getNodeData().get(0).getUnFixedTimepoints();
		
	}

	@Override
	//we assume there are no bookends if we're working with DC problems
	//FIXME: this will need to be changed if we want to start doing preemptive stuff
	public int checkBookends(int duration) {
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
		return currentNode.getNodeData().get(0).getDTPBoundaries();
	}

	@Override
	public void addNonconcurrentConstraint(String source, String dest, int agent) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addNonconcurrentConstraint(source, dest, agent);
		}
	}

	@Override
	public ArrayList<String> getActivityNames(int agent) {
		// I think this is what we want. The only way that the other components differ is
		// that they contain an activity for the sporadic event. 
		ArrayList<String> names = new ArrayList<String>();
		names.addAll(stp.getActivityNames());
		return names;
	}

	@Override
	public void addFixedTimepoints(Collection<Timepoint> tps) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addFixedTimepoints(tps);
		}
	}

	@Override

	public void updateTimepointName(Timepoint tp, String new_name) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.updateTimepointName(tp, new_name);
		}
		
	}

	@Override
	public void addDurationConstraint(Timepoint start, Timepoint end,
			int duration) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addDurationConstraint(start, end, duration);
		}
		
	}

	@Override
	public void addOrderingConstraint(String source, String dest,
			int min_duration, int max_duration) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.addOrderingConstraint(source, dest, min_duration, max_duration);
		}

	}

	@Override
	public void removeDurationConstraint(String tpName) {
		
		ArrayList<SimpleDTP> validComps = currentNode.getNodeData();
		for(SimpleDTP comp : validComps){
			comp.removeDurationConstraint(tpName);
		}

	}

	@Override
	//this means something different for this data structure than the others...
	
	public SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> getComponentSTPs() {
		return null;
	}
	
	//this method is no longer used. Does not take precedence into account when generating. 
	// this assumes that the sporadic activity follows any required activity that SE interrupts. 
	public ArrayList<SimpleDTP> populateComponentSTPsOLD(){
		ArrayList<SimpleDTP> stps = new ArrayList<SimpleDTP>();
		SimpleDTP base = stp.clone();
		base.updateInternalData();
		base.enumerateSolutions(0);
		base.simplifyMinNetIntervals();
		ArrayList<String> actOrder = getActivityOrdering();
//		System.out.println("Activity ordering is: " + actOrder);
		
		
		Timepoint s_s = new Timepoint(SPORADICNAME + "_S", 1);
		Timepoint s_e = new Timepoint(SPORADICNAME + "_E", 1);
		Timepoint zero = base.getTimepoint("zero");
		base.addContingentTimepoint(s_s);
		base.addContingentTimepoint(s_e);
		
		TemporalDifference tdmin = new TemporalDifference(s_s, s_e, -DUR);
		TemporalDifference tdmax = new TemporalDifference(s_e, s_s, DUR);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		
		TemporalDifference td1 = new TemporalDifference(s_e, zero, SporadicEventLT);
		TemporalDifference td2 = new TemporalDifference(zero, s_s, SporadicEventET);
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
	
		dtcs.add(dtc1); dtcs.add(dtc2);
		
		base.addAdditionalConstraints(dtcs); 
		
		
		
		//System.out.println("ACT order is: " + actOrder);
		for(int i = 0; i < actOrder.size(); i++){
			String actName = actOrder.get(i);
//			//System.out.println("Working with activity: " + actName);
			
			//we need one component with the sporadic event glued to the front 
			SimpleDTP newstp = base.clone();
			newstp.addOrderingConstraint(SPORADICNAME, actName, 0, 0);
			if(i == 0) {
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				stps.add(newstp);
				
			}else{
			//SE also needs to come after previous activity. how we enforce non-concurrency
				//without introducing disjunctive constraints
			newstp.addOrderingConstraint(actOrder.get(i-1), SPORADICNAME, 0, Integer.MAX_VALUE);
			newstp.updateInternalData();
			newstp.enumerateSolutions(0);
			newstp.simplifyMinNetIntervals();
			stps.add(newstp);
			}
			
			
			//CASE1: sporadic event follows activity 
			//also need the component where the sporadic event comes directly after
			if(precedence == 0){
				//System.out.println("Sporadic event follows");
				newstp = base.clone();
				newstp.addOrderingConstraint(actName, SPORADICNAME, 0, 0);
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				stps.add(newstp);
			}else if(precedence == 1){
				//System.out.println("sporadic event interrupts");
				//CASE2: sporadic event interrupts activity. 
				newstp = base.clone();
				
				newstp.updateDurationConstraintsValue(actName, DUR);
				//synchronize sporadic event end time with end time of actName
				Timepoint curr_act_end = newstp.getTimepoint(actName + "_E");
				TemporalDifference tdSync1 = new TemporalDifference(s_e, curr_act_end, 0);
				TemporalDifference tdSync2 = new TemporalDifference(curr_act_end, s_e, 0);
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync1));
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync2));
				
				// SE still comes before the next activity
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				stps.add(newstp);
			}else{
				System.out.println("PRECEDENCE is not set properly!!");
			}
			
			if(i == actOrder.size() - 1){
				//for the last component, we want to glue the SE to the end of the day also
				newstp = base.clone();
				newstp.addOrderingConstraint(actOrder.get(i), SPORADICNAME, 0, Integer.MAX_VALUE);
				td1 = new TemporalDifference(s_e, zero, SporadicEventLT);
				td2 = new TemporalDifference(zero, s_e, -SporadicEventLT);
				dtc1 = new DisjunctiveTemporalConstraint(td1);
				dtc2 = new DisjunctiveTemporalConstraint(td2);
				newstp.addAdditionalConstraint(dtc2);
				newstp.addAdditionalConstraint(dtc1);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				stps.add(newstp);
			}
		}
		
		return stps;
	}
	
	/*
	 * Note that in determining whether a sporadic event happened during an interval (like during an activity)
	 * discrete time is treated as:
	 * If the event happens at exactly the same time as the start-time of the interval, then it happens during the interval
	 * If the event happens at exactly the same time as the end-time of the interval, then it happens *AFTER* the interval
	 */
	
	public boolean sporadicEventCanOccurBeforeActivityStarts(String actname) {
//		System.out.print("Can sporadic activity occur before "+actname);
		if(stp.getLatestStartTime(stp.getTimepoint(actname+"_S")) > SporadicEventET) {
//			System.out.println("  YES");
			return true;
		} else {
//			System.out.println("  NO");
			return false;
		}
	}

	public boolean sporadicEventCanOccurAfterActivityEnds(String actname) {
//		System.out.print("Can sporadic activity occur after "+actname);
		if(stp.getEarliestEndTime(stp.getTimepoint(actname+"_E")) <= SporadicEventLT) {
//			System.out.println("  YES");
			return true;
		} else {
//			System.out.println("  NO");
			return false;
		}
	}
	public boolean sporadicEventCanOccurDuringActivity(String actname) {
//		System.out.print("Can sporadic activity occur during "+actname);
		if(SporadicEventET < stp.getLatestEndTime(stp.getTimepoint(actname+"_E")) &&
				SporadicEventLT >= stp.getEarliestStartTime(stp.getTimepoint(actname+"_S"))) {
//			if(Collections.max(stp.getMaxSlack()) >= SporadicEventET) {
//			System.out.println("  YES");
				return true;
			} else {
//				System.out.println("  NO");
				return false;
			}
		}

	public boolean sporadicEventCanOccurBetweenActivities(String actname, String prevactname) {
//		System.out.print("Can sporadic activity occur after prev activity and before "+actname);
		if(SporadicEventET < stp.getLatestStartTime(stp.getTimepoint(actname+"_S")) &&
				SporadicEventLT >= stp.getEarliestEndTime(stp.getTimepoint(prevactname+"_E"))) {
//			System.out.println("  YES");
				return true;
			} else {
//				System.out.println("  NO");
				return false;
			}
		}
	
	public ArrayList<SimpleDTP> populateISTPs(){
		ArrayList<SimpleDTP> stps = new ArrayList<SimpleDTP>();
		//set up the base STP we'll be working with. add the sporadic event
		// along with the constraints needed to include it within the bounds of the problem
		
		SimpleDTP base = stp.clone();
		base.updateInternalData();
		base.enumerateSolutions(0);
		base.simplifyMinNetIntervals();
		ArrayList<String> actOrder = getActivityOrdering();
//		System.out.println("Activity ordering is: " + actOrder);
		
		
		Timepoint s_s = new Timepoint(SPORADICNAME + "_S", 1);
		Timepoint s_e = new Timepoint(SPORADICNAME + "_E", 1);
		Timepoint zero = base.getTimepoint("zero");
		base.addContingentTimepoint(s_s);
		base.addContingentTimepoint(s_e);
		
		TemporalDifference tdmin = new TemporalDifference(s_s, s_e, -DUR);
		TemporalDifference tdmax = new TemporalDifference(s_e, s_s, DUR);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		
		TemporalDifference td1 = new TemporalDifference(s_e, zero, SporadicEventLT+DUR+MaxSporadicActivityEndTimeAfterSporadicEvent);
		TemporalDifference td2 = new TemporalDifference(zero, s_s, -SporadicEventET);
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
	
		dtcs.add(dtc1); dtcs.add(dtc2);
		
		base.addAdditionalConstraints(dtcs); 
		
		//now for each activity in the list, we add in component iSTPs
		// STILL NEED TO DECIDE WHAT TO DO ABOUT FIRST AND LAST GIVEN SPECIFIC PREC RELATIONS
		
		for(int i = 0; i < actOrder.size(); i++){
			String actName = actOrder.get(i);
//			System.out.println("working with activity " + actName);
			SimpleDTP newstp = base.clone();
			
			//System.out.println("SPORADICNAME is " + SPORADICNAME);

			int prec = precMap.get(new SimpleEntry<String, String>(actName, SPORADICNAME));
						
			//first component has the sporadic event glued to the front.
			//  unless it cannot happen before the first activity must start
//			if(sporadicEventCanOccurBeforeActivityStarts(actName)) {
				if(i == 0 && sporadicEventCanOccurBeforeActivityStarts(actName)){
					//before the first activity
					if(prec == 2){
						stps.addAll(getPrec2Comps("", actName, newstp));
					}else{
						newstp.addOrderingConstraint(SPORADICNAME, actName, 0, Integer.MAX_VALUE); //ED: changed last arg from 0 to max_value
						newstp.updateInternalData();
						newstp.enumerateSolutions(0);
						newstp.simplifyMinNetIntervals();
						newstp.setIdleNodeTrue();
						//Generics.printDTP(newstp);
						stps.add(newstp);
					}
				}else if (i != 0 && sporadicEventCanOccurBetweenActivities(actName, actOrder.get(i-1))){
					//if we've already completed an activity, SE needs to come after it
					// pretty sure this is adding the component that says "SE occurs between prev activity and current one"
				
					if(prec == 2){
						stps.addAll(getPrec2Comps(actOrder.get(i-1), actName, newstp));
					}
				
					newstp = base.clone();
					newstp.addOrderingConstraint(SPORADICNAME, actName, 0, Integer.MAX_VALUE); //ED: Changed last arg from 0 to max_value since doesn't have to occur immediately before
					newstp.addOrderingConstraint(actOrder.get(i-1), SPORADICNAME, 0, Integer.MAX_VALUE);
					// Not enough to just assert the SporadicActivity comes between them.  
					// Previous activity needs to finish at or before latest time SE could happen
					// And next activity needs to start strictly after earliest time SE could happen
					Timepoint prev_act_end = newstp.getTimepoint(actOrder.get(i-1) + "_E");
					TemporalDifference tdfinish = new TemporalDifference(prev_act_end, zero, SporadicEventLT);
					newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdfinish));
					Timepoint curr_act_start = newstp.getTimepoint(actName + "_S");
					TemporalDifference tdstart = new TemporalDifference(zero, curr_act_start, -(SporadicEventET+1));
					newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdstart));
					newstp.updateInternalData();
					newstp.enumerateSolutions(0);
					newstp.simplifyMinNetIntervals();
					newstp.setIdleNodeTrue();
					stps.add(newstp);
				}

			//next we do what happens based on precedence and how SA interacts with activity. 
			if(sporadicEventCanOccurDuringActivity(actName)) {
			  if(prec == 1){
				//the sporadic activity takes precedence over the req act
				// SA interrupts then
				newstp = base.clone();
				newstp.updateDurationConstraintsValue(actName, DUR);
				Timepoint curr_act_end = newstp.getTimepoint(actName + "_E");
				TemporalDifference tdSync1 = new TemporalDifference(s_e, curr_act_end, 0);
				TemporalDifference tdSync2 = new TemporalDifference(curr_act_end, s_e, 0);
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync1));
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync2));
				
				//need to make sure that this activity is associated with this component
				newstp.setSEAct(actName);
				
				// SE still comes before the next activity
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				stps.add(newstp);
			}else if(prec == 0){
				//sporadic event follows (equal precedence)

				newstp = base.clone();
				newstp.addOrderingConstraint(actName, SPORADICNAME, 0, 0); //ED: This last arg should be 0, since sporadic activity follows right after
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				// Not enough to just assert the SporadicActivity comes immediately after activity SE happened during and before next activity  
				// Activity that SE happens during must start at or before the latest time SE could happen
				//  and must end strictly after the earliest time the SE could happen
				Timepoint curr_act_start = newstp.getTimepoint(actName + "_S");
				TemporalDifference tdstartoverlap = new TemporalDifference(curr_act_start, zero, SporadicEventLT);
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdstartoverlap));
				Timepoint curr_act_end = newstp.getTimepoint(actName + "_E");
				TemporalDifference tdfinishoverlap = new TemporalDifference(zero, curr_act_end, -(SporadicEventET+1));
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdfinishoverlap));

				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setSEAct(actName); //associate this activity with this component.
				//Generics.printDTP(newstp);
				stps.add(newstp);

			}else if (prec == 2){
				System.out.println("Precedence is 2");
				//so now we need to say that if the sporadic event occurs during C, sporadic activity follows
				newstp = base.clone();
				newstp.addOrderingConstraint(actName, SPORADICNAME, 0, 0);
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setSEAct(actName);
				//Generics.printDTP(newstp);
				stps.add(newstp);			
			}else{
				System.out.println("PRECEDENCE is not set properly!!");	
			}}
			//Finally, if this was the last activity, then need to glue the sporadic activity in at the end
			if(i == (actOrder.size()-1) && sporadicEventCanOccurAfterActivityEnds(actName)){
				//after the last activity
				// precedence shouldn't matter since this is after the last activity?
				//if(prec == 2){
					//stps.addAll(getPrec2Comps("", actName, newstp));
				//}else{
					newstp = base.clone();
					newstp.addOrderingConstraint(actName, SPORADICNAME, 0, 0);
					newstp.updateInternalData();
					newstp.enumerateSolutions(0);
					newstp.simplifyMinNetIntervals();
					newstp.setIdleNodeTrue();
					//Generics.printDTP(newstp);
					stps.add(newstp);
				//}
			}
		}
		return stps;
	}
	
	
	public ArrayList<SimpleDTP> populateISTPsMany(){
		ArrayList<SimpleDTP> manySTPs = new ArrayList<SimpleDTP>();
		ArrayList<SimpleDTP> returnedISTPs = new ArrayList<SimpleDTP>();
		SimpleDTP mergedSTP;
// Here is the basic idea.  We step through each of the possible times the SE could occur.
// For each such time, we call populateISTPs to enumerate the possible placements of the sporadic activity.
// Then we merge those STPs together into a single STP and add that to our collection of manySTPs
// If this is done right, we'll have as many STPs in manySTPs as there are timepoints when the sporadic event can occur.
// Rationale about merging: For a specific placement of the SE, it could occur potentially during any of a number
//  of possible required activities or discretionary activities, depending on the USER's choices of when
//  discretionary time is taken.  Since it is the user's choice, we can offer the union of the times, rather
//  than an intersection.
		int initialSporadicEventET = SporadicEventET;
		int initialSporadicEventLT = SporadicEventLT;
		
		for(int i=initialSporadicEventET; i<=initialSporadicEventLT; i++) {
			SporadicEventET = i;
			SporadicEventLT = i;
			returnedISTPs = populateISTPs();
			mergedSTP = returnedISTPs.get(0).mergeSTPs(returnedISTPs);
			// Now add in the sporadic event timepoint and its setting
			
			Timepoint seTimepoint1 = new Timepoint(SPORADICNAME + "_T1",1);
			mergedSTP.addContingentTimepoint(seTimepoint1);
			Timepoint zero = mergedSTP.getTimepoint("zero");
			TemporalDifference SEend = new TemporalDifference(seTimepoint1, zero, i);
			mergedSTP.addAdditionalConstraint(new DisjunctiveTemporalConstraint(SEend));
			TemporalDifference SEstart = new TemporalDifference(zero, seTimepoint1, -i);
			mergedSTP.addAdditionalConstraint(new DisjunctiveTemporalConstraint(SEstart));
			// This repeat is really ugly.  Other parts of the code assume that, other than the zero timepoint
			//  all timepoints come in pairs (start and end of an activity)
			//  So to stay consistent we're doing that here too, but the sporadic event (as opposed to sporadic activity)
			//  is by definition instantaneous, so a pairing is unnecessary - but easier for now...
			Timepoint seTimepoint2 = new Timepoint(SPORADICNAME + "_T2",1);
			mergedSTP.addContingentTimepoint(seTimepoint2);
			TemporalDifference SEend2 = new TemporalDifference(seTimepoint2, zero, i+1);
			mergedSTP.addAdditionalConstraint(new DisjunctiveTemporalConstraint(SEend2));
			TemporalDifference SEstart2 = new TemporalDifference(zero, seTimepoint2, -i-1);
			mergedSTP.addAdditionalConstraint(new DisjunctiveTemporalConstraint(SEstart2));	 
            
			manySTPs.add(mergedSTP);
		}
		// Then restore these!
		SporadicEventET = initialSporadicEventET;
		SporadicEventLT = initialSporadicEventLT;
		System.out.println("The number of ISTPsMany is " + manySTPs.size());
		return manySTPs;
	}
	
	public ArrayList<SimpleDTP> populateISTPsRestrictSE(int n){
		//restricts the sporadic event to occurring during the first n activities
		ArrayList<SimpleDTP> stps = new ArrayList<SimpleDTP>();
		
		SimpleDTP base = stp.clone();
		base.updateInternalData();
		base.enumerateSolutions(0);
		base.simplifyMinNetIntervals();
		ArrayList<String> actOrder = getActivityOrdering();
//		System.out.println("Activity ordering is: " + actOrder);
		
		
		Timepoint s_s = new Timepoint(SPORADICNAME + "_S", 1);
		Timepoint s_e = new Timepoint(SPORADICNAME + "_E", 1);
		Timepoint zero = base.getTimepoint("zero");
		base.addContingentTimepoint(s_s);
		base.addContingentTimepoint(s_e);
		
		TemporalDifference tdmin = new TemporalDifference(s_s, s_e, -DUR);
		TemporalDifference tdmax = new TemporalDifference(s_e, s_s, DUR);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		
		TemporalDifference td1 = new TemporalDifference(s_e, zero, SporadicEventLT);
		TemporalDifference td2 = new TemporalDifference(zero, s_s, -SporadicEventET);
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
	
		dtcs.add(dtc1); dtcs.add(dtc2);
		
		base.addAdditionalConstraints(dtcs); 
		
		//now for each activity in the list, we add in component iSTPs

		
		for(int i = 0; i < n; i++){
			String actName = actOrder.get(i);
			//System.out.println("working with activity " + actName);
			SimpleDTP newstp = base.clone();
			
			int prec = precMap.get(new SimpleEntry<String, String>(actName, SPORADICNAME));
			
			//first component has the sporadic event glued to the front.
			if(i == 0){
				//before the first activity
				if(prec == 2){
					stps.addAll(getPrec2Comps("", actName, newstp));
				}else{
					newstp.addOrderingConstraint(SPORADICNAME, actName, 0, 0);
					newstp.updateInternalData();
					newstp.enumerateSolutions(0);
					newstp.simplifyMinNetIntervals();
					newstp.setIdleNodeTrue();
					//Generics.printDTP(newstp);
					stps.add(newstp);
				}
			}else{
				//if we've already completed an activity, SE needs to come after it
				// pretty sure this is adding the component that says "SE occurs between prev activity and current one"
				
				if(prec == 2){
					stps.addAll(getPrec2Comps(actOrder.get(i-1), actName, newstp));
				}
				
				newstp = base.clone();
				newstp.addOrderingConstraint(SPORADICNAME, actName, 0, 0);
				newstp.addOrderingConstraint(actOrder.get(i-1), SPORADICNAME, 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setIdleNodeTrue();
				//Generics.printDTP(newstp);
				stps.add(newstp);
			}
			
			//next we do what happens based on precedence and how SA interacts with activity. 
			
			
			//System.out.println("act is: " + actName + " and prec is: " + prec);
			if(prec == 1){
				//the sporadic activity takes precedence over the req act
				// SA interrupts then
				newstp = base.clone();
				newstp.updateDurationConstraintsValue(actName, DUR);
				Timepoint curr_act_end = newstp.getTimepoint(actName + "_E");
				TemporalDifference tdSync1 = new TemporalDifference(s_e, curr_act_end, 0);
				TemporalDifference tdSync2 = new TemporalDifference(curr_act_end, s_e, 0);
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync1));
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync2));
				
				//need to make sure that this activity is associated with this component
				newstp.setSEAct(actName);
				
				// SE still comes before the next activity
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				//Generics.printDTP(newstp);
				stps.add(newstp);
			}else if(prec == 0){
				//sporadic event follows (equal precedence

				newstp = base.clone();
				newstp.addOrderingConstraint(actName, SPORADICNAME, 0, 0);
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setSEAct(actName); //associate this activity with this component.
				//Generics.printDTP(newstp);
				stps.add(newstp);
				
			}else if (prec == 2){
				System.out.println("Precedence is 2");
				//so now we need to say that if the sporadic event occurs during C, sporadic activity follows
				newstp = base.clone();
				newstp.addOrderingConstraint(actName, SPORADICNAME, 0, 0);
				if(i != actOrder.size() - 1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(i+1), 0, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setSEAct(actName);
				//Generics.printDTP(newstp);
				stps.add(newstp);			
			}else{
				System.out.println("PRECEDENCE is not set properly!!");	
			}
		}
		
		
		
		System.out.println("length of stps: "+stps.size());
		return stps;

	}
	
	//A helper function to get the components for a prec 2 relation
	//actPrev is the activity coming before the time critical acticity, actC, which has prec 2 
	// with the sporadic activity. The base problem we get already has the sporadic event in it.
	public ArrayList<SimpleDTP> getPrec2Comps(String actPrev, String actC, SimpleDTP base){
		ArrayList<SimpleDTP> toReturn = new ArrayList<SimpleDTP>();
		SimpleDTP newstp = base.clone();
		
		int Cstart = base.getEarliestStartTime(base.timepoints.get(actC + "_S"));
		System.out.println("c start time is: " + Cstart);
		
		int prevEnd = base.getEarliestEndTime(base.timepoints.get(actPrev + "_E"));
		System.out.println("prev end time is: " + prevEnd);
		//first component where SE occurs before it would overlap with C 
		if(Cstart - DUR > prevEnd){
			if(actPrev.length() == 0){
				//then the first activity of the day is time critical. 
				//in the first component, we capture SE occurring before it would overlap with C. 
				newstp.addOrderingConstraint(SPORADICNAME, actC, DUR, Integer.MAX_VALUE);
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setIdleNodeTrue();
				Generics.printDTP(newstp);
				toReturn.add(newstp);
	
			}else{
				newstp = base.clone();
				//sporadic activity should always follow the previous activity for this "in between component"
				newstp.addOrderingConstraint(actPrev, SPORADICNAME, 0, Integer.MAX_VALUE);
				
				//this needs to be that the LET for the sporadic event is the crit start time - d_SA
				//newstp.addOrderingConstraint(SPORADICNAME, actC, DUR, Integer.MAX_VALUE);
				
				//actually, we just need the sporadic activity to end before C starts?
				newstp.addOrderingConstraint(SPORADICNAME, actC, 0, Integer.MAX_VALUE);
				
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				newstp.setIdleNodeTrue();
				Generics.printDTP(newstp);
				toReturn.add(newstp);
				
			}
		}
		newstp = base.clone();
		//now for the second component which is always the same. in which C extends the duration of the sporadic event.
		// we allow sporadic and actC to be concurrent with each other. no ordering constraint is added in this component 
		// but now, it's also the case that SA and C might not end at the same time, like in the other
		//   prec1 cases in which that's equivalent...
		newstp.addOrderingConstraint(actPrev, SPORADICNAME, 0, Integer.MAX_VALUE);
		int durC = newstp.getDuration(actC); //get duration of activity actC
		//need to add a constraint that makes sure the EST for the SA will overlap with C. 
		Timepoint se_s = base.getTimepoint(SPORADICNAME + "_S");
		//Timepoint se_e = base.getTimepoint(SPORADICNAME + "_E");
		Timepoint zero = base.getTimepoint("zero");
		int bound = Cstart - DUR;
		TemporalDifference est = new TemporalDifference(zero, se_s, -bound);
		TemporalDifference lst = new TemporalDifference(se_s, zero, Cstart);
		
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(est);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(lst);
		
		Collection<DisjunctiveTemporalConstraint> dtcs = new ArrayList<DisjunctiveTemporalConstraint>();
		dtcs.add(dtc1); dtcs.add(dtc2);
		newstp.addAdditionalConstraints(dtcs);
		newstp.updateInternalData();
		newstp.enumerateSolutions(0);
		newstp.simplifyMinNetIntervals();
		System.out.println("after updating EST and LST, before extending duration.");
		Generics.printDTP(newstp);
		newstp.updateDurationConstraintsValue(SPORADICNAME, durC);
		newstp.updateInternalData();
		newstp.enumerateSolutions(0);
		newstp.simplifyMinNetIntervals();
		
		newstp.setIdleNodeTrue();
		
		Generics.printDTP(newstp);
		toReturn.add(newstp);
		
		return toReturn;
	}
	
	//A function that can determine the activity ordering from the STP
	public ArrayList<String> getActivityOrdering(){
		ArrayList<String> order = new ArrayList<String>();
		Map<Integer, String> toSort = new TreeMap<Integer, String>();
		//ArrayList<SimpleEntry<Integer, String>> sorted = new ArrayList<SimpleEntry<Integer, String>>();
		int idx = 1;
		while(idx < stp.minimalNetwork.length){
			IntervalSet i = stp.minimalNetwork[0][idx];
			int lb = (int) -i.getLowerBound();
			String name = stp.localTimepoints.get(idx).getName();
			name = name.substring(0,name.length() - 2);
			toSort.put(lb,name);
			idx = idx+2;
		}
		Iterator<Integer> it = toSort.keySet().iterator();
		while(it.hasNext()){
			String tp = toSort.get(it.next());
			order.add(tp);
		}
		//System.out.println("The order of the activities is: " + order);
		return order;
	}
	
	//returns the root node of DC tree containing the valid STPs at each decision point
	public Node<ArrayList<SimpleDTP>> generateDCTree(){
		ArrayList<SimpleDTP> compSTPs = new ArrayList<SimpleDTP>();
		//clone the componentSTPs so we don't mess anything up here
		
		for(SimpleDTP stp : componentSTPs){
			compSTPs.add(stp.clone());
		}
		stp.setIdleNodeTrue();
		compSTPs.add(stp.clone()); //adding in the component where it doesn't happen.
		//the data value at the root node is always the full set of component STPs
		Node<ArrayList<SimpleDTP>> root = new Node<ArrayList<SimpleDTP>>(compSTPs);
		//here is where we want the componentSTPs to be sorted by the earliest start time
		// of the sporadic event
		//Actually, let's assume the method that generates the components puts them into
		//  the list in the order that we need
		Queue<SimpleDTP> cSTPs = new LinkedList<SimpleDTP>(compSTPs);
		Node<ArrayList<SimpleDTP>> currNode = root;
		
		
		while(cSTPs.peek() != null){
			SimpleDTP toadd = cSTPs.remove();
			
			ArrayList<SimpleDTP> data = new ArrayList<SimpleDTP>();
			data.add(toadd);
			currNode.setRightChild(new Node<ArrayList<SimpleDTP>>(data, currNode));
			//left node contains the rest of the queue
			currNode.setLeftChild(new Node<ArrayList<SimpleDTP>>(new ArrayList<SimpleDTP>(cSTPs), currNode));
			
			currNode = currNode.getLeftChild();
			if(cSTPs.size() == 1) break;
		}
		
		
		return root;
	}
	

	
	//takes the data from the root given in the problem and repopulates the tree underneath it with that info.
	public Node<ArrayList<SimpleDTP>> regenerateDCTree(ArrayList<SimpleDTP> newComps){
		ArrayList<SimpleDTP> compSTPs = newComps;
		
		Node<ArrayList<SimpleDTP>> root = new Node<ArrayList<SimpleDTP>>(compSTPs);
		Queue<SimpleDTP> cSTPs = new LinkedList<SimpleDTP>(compSTPs);
		Node<ArrayList<SimpleDTP>> currNode = root;
		
		
		while(cSTPs.peek() != null){
			SimpleDTP toadd = cSTPs.remove();
			
			ArrayList<SimpleDTP> data = new ArrayList<SimpleDTP>();
			data.add(toadd);
			currNode.setRightChild(new Node<ArrayList<SimpleDTP>>(data, currNode));
			//left node contains the rest of the queue
			currNode.setLeftChild(new Node<ArrayList<SimpleDTP>>(new ArrayList<SimpleDTP>(cSTPs), currNode));
			
			currNode = currNode.getLeftChild();
			if(cSTPs.size() == 1) break;
		}
		
		
		return root;
	}
	
	public boolean checkWeakControllability(){
		
		//want to iterate through components at the root of Dtree
		ArrayList<SimpleDTP> allComps = DTree.getNodeData();
		for(SimpleDTP comp : allComps){
			comp.updateInternalData();
			comp.enumerateSolutions(0);
			comp.simplifyMinNetIntervals();
			if(comp.getNumSolutions() == 0) return false;
			if(comp.hasZeroDurations()) return false;
			if(comp.getMinTime() == Integer.MAX_VALUE) return false;
		}
		
		return true;
	}
	
	

	public boolean checkDynamicControllability(){
		
		//need to do a forward and backward pass
		
		boolean forward = DCForwardPass();
		System.out.println("Forward Pass is " + forward);
		boolean backward = DCBackwardPass();	
		System.out.println("Backward Pass is " + backward);

		
		//sanity checks
		ArrayList<SimpleDTP> allComps = DTree.getNodeData();
		
		for(SimpleDTP comp : allComps){
			if(comp.getNumSolutions() == 0){
				System.out.println("COMPONENT HAS ZERO SOLUTIONS"); 
				return false;
			}
			if(comp.hasZeroDurations()){
				System.out.println("COMPONENT HAS ZERO DURATIONS");
				return false;
			}
			if(comp.getMinTime() == Integer.MAX_VALUE){
				System.out.println("COMPONENT HAS INFINITE MIN TIME");
				return false;
			}
		}
		
		
		if(forward && backward) DC = true;
		return forward && backward;
	}
	
	// we want to write a function that takes a non-DC DUSTP and iterates through 
	// things to get rid of problems. until it's solvable. 
	public boolean fixNonDCDUSTP(){
		ArrayList<SimpleDTP> comps = DTree.getNodeData();
		ArrayList<SimpleDTP> newComps = new ArrayList<SimpleDTP>();
		
		// now we want to iterate through and find the problematic ones. 
		// and we remove the sporadic event. which is equivalent to replacing it with the 
		// underlying SimpleDTP problem. 
		int se_times_len = se_times.size();
		//System.out.println("there are this many components in the root: " + comps.size());
		for(int i = 0; i < comps.size(); i++){
			SimpleDTP istp = comps.get(i);
			stp.updateInternalData();
			stp.enumerateSolutions(0);
			stp.simplifyMinNetIntervals();
			if(istp.getNumSolutions() == 0){
				System.out.println("non DC comp has zero solutions: " + i);
				
				if(se_times.contains(i)){
					System.out.println("we can't fix this component");
					return false;
				}
				
				se_times.add(i);
				newComps.add(i, stp.clone());
				continue;
			}
			if(istp.hasZeroDurations()){
				
				if(se_times.contains(i)){
					System.out.println("we can't fix this component");
					return false;
				}
				
				se_times.add(i);
				newComps.add(i,stp.clone());
				continue;
			}
			if(istp.getMinTime() == Integer.MAX_VALUE){
				
				if(se_times.contains(i)){
					System.out.println("we can't fix this component");
					return false;
				}
				
				se_times.add(i);
				newComps.add(i,stp.clone());
				
				
				continue;
			}
			newComps.add(i,istp); //nothing was wrong, so we want the original here
		}
		if(se_times.size() == se_times_len) return false;
		
		DTree = regenerateDCTree(newComps); //repopulate the tree with updated components.
		if(checkDynamicControllability()) {
			System.out.println("we made it DC again. se times is : " + se_times);
			
			return true;
		}
		else{
			
			if(iters > comps.size()) {
				System.out.println("returning false bc we're stuck in a loop");
				return false;
			}
			System.out.println("starting from the top. se times is: " + se_times);
			return false;
		}
		
	}
	
	private boolean DCForwardPass(){
		//helper function to compute the forward pass of our DC algorithm. 
		Node<ArrayList<SimpleDTP>> currNode = DTree;
		
		//keep track of depth. even + 0 are idle nodes, odd are activity nodes
		int curr_depth = 1;
		currNode = currNode.getLeftChild();
		while(currNode != null){
			/*
			Generics.printDTP(currNode.getNodeData().get(0));
			System.out.println("at depth "+ curr_depth);
			System.out.println(currNode.getNodeData().get(0).idleNode);
			*/
			if(currNode.getNodeData().get(0).idleNode){
				
				currNode = currNode.getLeftChild();
				curr_depth++; continue;
			}
			
			ArrayList<SimpleDTP> validComps = currNode.getNodeData();
			
			// for the name of the activity we want to tighten... it comes from the first component in the list, right?
			String act_name = currNode.getNodeData().get(0).se_act;
			
			if(act_name.length() == 0){
				System.out.println("act name isn't set right in the current component.");
			}
			
			//to get activity we just pull from the list of names and compare intervals.
	
			//now we need to get the EST for this activity. and add that constraint to each of the valid components 
			ArrayList<IntervalSet> iss = new ArrayList<IntervalSet>();
			for(SimpleDTP comp : validComps){
				iss.add(comp.getInterval("zero", act_name + "_S").inverse());
			
			}
			IntervalSet intersection = new IntervalSet();
			//compute intersection of the interval.
			if(iss.size() == 1){
				intersection = iss.get(0);
			}else{
				intersection = iss.get(0).intersection(iss.get(1));
			
				for(int i = 1; i < iss.size(); i++){
					intersection = intersection.intersection(iss.get(i));
				}
			}
			
			//return false if there is no intersection. Otherwise add a constraint to every component w the new EST 
		
			if(intersection.isNull()){
			
				return false;
			}
			//fix the start time of the activity to its earliest start time and continue down the tree. 
			// the lower bound of the intersect interval should be the EST for activity A in all comps.
			int lb = (int) intersection.getLowerBound();

			for(SimpleDTP comp : validComps){
				
				Timepoint act_start = comp.getTimepoint(act_name + "_S");
				Timepoint zero = comp.getTimepoint("zero");
				TemporalDifference td1 = new TemporalDifference(zero, act_start, -lb);
				DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
				comp.addAdditionalConstraint(dtc1);
				comp.updateInternalData();
				comp.enumerateSolutions(0);
				comp.simplifyMinNetIntervals();
				
			}
			//should we check here for problems w zero solutions? it would just be a sanity check. shouldn't
			 // actually happen since we picked an interval in the intersection. 
			currNode = currNode.getLeftChild();
			curr_depth++;
		}
		return true;
	}
	
	private boolean DCBackwardPass(){
		Node<ArrayList<SimpleDTP>> leftmostChild = DTree;
		int curr_depth = 0;
		while(leftmostChild.getLeftChild() != null){
			leftmostChild = leftmostChild.getLeftChild();
			curr_depth++;
		}
		ArrayList<String> act_order = getActivityOrdering();
		Collections.reverse(act_order);
		int act_ind = 0;
		
		Node<ArrayList<SimpleDTP>> currNode = leftmostChild;
		while(currNode != null){
			
			if(currNode.getNodeData().get(0).idleNode){
				currNode = currNode.getParent();
				curr_depth--; continue;
			}
			
			ArrayList<SimpleDTP> validComps = currNode.getNodeData();
			
			if(currNode.getParent() == null){ //we're at the top of the tree and don't want to dec act_ind anymore
				act_ind--; //node act_ind is ascending bc i reversed the act_order list 
			}
			if(act_ind >= act_order.size()){
				System.out.println("activity index "+act_ind+" compared to "+act_order.size()+" is too big!!");
				currNode = null;
				continue; // or break maybe? 
			}
			String act_name = currNode.getNodeData().get(0).se_act;
			ArrayList<IntervalSet> iss = new ArrayList<IntervalSet>();
			for(SimpleDTP comp : validComps){
				iss.add(comp.getInterval("zero", act_name + "_S").inverse());
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
			if(intersection.isNull()){
				return false;
			}
			
			//once we get the intersection we want to tighten the LETs.
			int ub = (int) intersection.getUpperBound();
			for(SimpleDTP comp : validComps){
				
				Timepoint act_st = comp.getTimepoint(act_name + "_S");
				Timepoint zero = comp.getTimepoint("zero");
				TemporalDifference td1 = new TemporalDifference(act_st, zero, ub);
				DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
				comp.addAdditionalConstraint(dtc1);
				comp.updateInternalData();
				comp.enumerateSolutions(0);
				comp.simplifyMinNetIntervals();
			}
			currNode = currNode.getParent();
			curr_depth--;
			act_ind++;
			
		}
		
		return true;
	}
	
	
	
	//returns intersected list nested in a list so that conversion to MA is easier
	public List<List<String>> getActivitiesFromSet(ArrayList<SimpleDTP> comps, ActivityFinder af, int time){
		List<List<String>> result = new LinkedList<>();
		List<List<String>> ret = new LinkedList<>();
		
//		System.out.println("Here are the component STPs from DUSTP getActivitiesFromSet");
//		for(SimpleDTP stp : comps){
//			Generics.printDTP(stp);		
//		}
		
		for(SimpleDTP stp : comps){
			List<String> acts = new ArrayList<String>();

			acts = stp.getActivities(af, time).get(0);
//			Generics.printDTP(stp);
//			if(acts.size() == 0) return null;
			if(acts.size() == 1 && acts.get(0).equals(SPORADICNAME)){
				//Generics.printDTP(stp);
//				System.out.println("Only activity in iSTP is sporadic.");
			} else {  //ED: Moved this into an else block, because intersection with only sporadic doesn't make sense to me (12-20-17)
//			System.out.println("Adding " + acts.size() + " activities " + acts);
			result.add(acts);
			}
		}
/*		for(List<String> act : result) {
			System.out.println(act);
		} */
		Set<String> first = new HashSet<String>(result.get(0));
		
		for(int i = 1; i < result.size(); i++){
//			first.retainAll(new HashSet<String>(result.get(i)));
			// ED: Not sure why intersection is sought rather than union?  Changed to union.
			first.addAll(new HashSet<String>(result.get(i)));
		}
		ret.add(new ArrayList<String>(first));
		
		return ret;
	}
	
	public DUSTP clone(){
		Iterator<SimpleDTP> it = componentSTPs.iterator();
		ArrayList<SimpleDTP> newComps = new ArrayList<SimpleDTP>();
		SimpleDTP newSTP = stp.clone();
		while(it.hasNext()){
			SimpleDTP curr = it.next();
			newComps.add(curr.clone());
		}
		HashMap<SimpleEntry<String, String>, Integer> newPrec = new HashMap<SimpleEntry<String, String>, Integer>();
		Iterator<SimpleEntry<String,String>> it2 = precMap.keySet().iterator();
		while(it2.hasNext()){
			SimpleEntry<String,String> names = it2.next();
			newPrec.put(names, precMap.get(names));
		}
		ArrayList<Integer> newTimes = new ArrayList<Integer>();
		newTimes.addAll(se_times);
		return new DUSTP(newSTP, newComps, this.precedence, newPrec, newTimes);
	}
	
	public void advanceDownTree(){
		if(currentNode.getLeftChild() != null) currentNode = currentNode.getLeftChild();
	}

	@Override
	public SimpleEntry<Integer, Integer> getMinSlackInterval(int scheduleTime) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void removeStickingPointsAndCheck(){
		//TODO: what is this supposed to do? 
		//we need to find the problematic component as we move down the DTree... 
		
	}
	
	public int getDTreeSize(){
		return DTree.getNodeData().size();
	}
	
	public ArrayList<SimpleDTP> getISTPs(){
		return DTree.getNodeData();
	}
	
	
	//input is the DUSTP with the many DTree set up. 
	public boolean compareAnswers(ManyDUSTP dMany){
		Node<ArrayList<SimpleDTP>> singleTree = DTree;
		Node<ArrayList<ArrayList<SimpleDTP>>> manyTree = dMany.DTreeMany;
		ArrayList<String> actOrder = getActivityOrdering();
		HashMap<String, IntervalSet> singleInts = new HashMap<String, IntervalSet>();
		HashMap<String, IntervalSet> manyInts = new HashMap<String, IntervalSet>();
		
		// advance down the DTree and pull the info we need. 
		while(singleTree != null){
			if(singleTree.getNodeData().get(0).idleNode){
				singleTree = singleTree.getLeftChild();
				continue;
			}
			
			String act_name = singleTree.getNodeData().get(0).se_act;
			IntervalSet av = singleTree.getNodeData().get(0).getInterval("zero", act_name + "_S").inverse();
			singleInts.put(act_name, av);
			singleTree = singleTree.getLeftChild();
		}
		
		while(manyTree != null){
			if(manyTree.getNodeData().get(0).get(0).idleNode){
				manyTree = manyTree.getLeftChild();
				continue;
			}
			
			String act_name = manyTree.getNodeData().get(0).get(0).se_act;
		}
		boolean ret = true;
		for(String act : actOrder){
			IntervalSet isS = singleInts.get(act);
			IntervalSet isM = manyInts.get(act);
			boolean same = isS.equals(isM);
			if(!same) ret = false;
		}
		
		return ret;
	}
	
}
