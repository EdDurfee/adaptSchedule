/* 
 * Copyright 2010 by The Regents of the University of Michigan
 *    For questions or permissions contact durfee@umich.edu
 * 
 * Code developed by James Boerkoel and Ed Durfee
 */

package dtp;

import interactionFramework.Generics;
import interactionFramework.MappedPack;
import interval.Interval;
import interval.IntervalSet;
import stp.STN;
import stp.TemporalDifference;
import stp.Timepoint;
import util.HSPFileFormatException;

import java.io.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;



public abstract class SimpleDTP implements DisjunctiveTemporalProblem, java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -285030064346901600L;
	//Static fields
	public static int UNSOLVED = 0;
	public static int INFEASIBLE = -1;
	public static int SOLVED_STN = 1;
	public static int SOLVED_SCHEDULE = 2;
	public static int SOLVED_INFLUENCE = 3;
	public static int SOLVED_ALL = 4;
	public static int SOLVING_INFLUENCE = 5;
	public static int MAX_LET = 1440; // This is the latest of the latest end-times, marking the end of the scheduling period
	public int VALID = 1;
	public boolean idleNode = false;
	public String se_act = "";
	public static String z3ExecCommand = "C:\\Program Files (x86)\\Microsoft Research\\Z3-1.3.6\\bin\\z3.exe ";
//	public static String yicesExecCommand = "./yices-1.0.40/bin/yices";
//	public static String yicesExecCommand = "./yices-1.0.32/bin/yices.exe";
	public static String yicesExecCommand = "./yices-1.0.40_mac/bin/yices";
	
	//Data structure for recording solve statistics
	private int numConflicts = 0;
	private int numDecisions = 0;
	private int numRestarts = 0;
	private int numSolutions = 0;
	private long time = 0;
	private int callsToSolver = 0;

	//Local constraints holding details of the problem
	protected HashMap<String,Timepoint> timepoints;
	protected Timepoint zero;
	protected int id;
	protected int numTimepoints = 0;
	protected ArrayList<DisjunctiveTemporalConstraint> tempConstraints;
	protected ArrayList<DisjunctiveTemporalConstraint> incomingConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
	protected ArrayList<DisjunctiveTemporalConstraint> volitionalConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
	protected int status=UNSOLVED;
	protected STN temporalNetwork;
	protected Stack<DisjunctiveTemporalConstraint> additionalConstraints = new Stack<DisjunctiveTemporalConstraint>();
	protected HashMap<String, Vector<MappedPack> > inputPairs = new HashMap<>();
	
	//selectionCount is used to undo activity selections
	//entry.key gives the number of additionalConstraints to pop
	//entry.value gives the timepoints to remove from fixedTimepoints
	protected Stack<SimpleEntry<Integer,ArrayList<Timepoint>>> selectionCount = new Stack<SimpleEntry<Integer,ArrayList<Timepoint>>>();
	protected int count = 0;
	
	protected String supplementaryConstraints;
	protected boolean addSolutionsAsNoGoods=false;
	protected StringBuffer solutionNoGood;

	protected ArrayList<Timepoint> contingentTimepoints;
	protected ArrayList<Timepoint> interfaceTimepoints;
	protected ArrayList<Timepoint> localTimepoints;
	protected ArrayList<Timepoint> fixedTimepoints = new ArrayList<Timepoint>();

	protected double maxSlack = 0;
	protected double minSlack = 0;
	protected IntervalSet[][] minimalNetwork;
	public static boolean DEBUG=false;

	public HashMap<SimpleEntry<String,String>, Integer> precMap = new HashMap<SimpleEntry<String, String>, Integer>();
	
	
	public SimpleDTP(int id, ArrayList<Timepoint> timepoints, ArrayList<DisjunctiveTemporalConstraint> tempConstraints){
		constructorHelper(id, tempConstraints, timepoints, new ArrayList<Timepoint>(), new HashMap<String, Vector<MappedPack> >());
	}

	public SimpleDTP(int id, ArrayList<Timepoint> timepoints, ArrayList<DisjunctiveTemporalConstraint> tempConstraints, HashMap<SimpleEntry<String,String>, Integer> prec){
		constructorHelper(id, tempConstraints, timepoints, new ArrayList<Timepoint>(), new HashMap<String, Vector<MappedPack> >());
		this.precMap = prec;
	}
	
	public SimpleDTP(int id, ArrayList<DisjunctiveTemporalConstraint> tempConstraints, ArrayList<Timepoint> localTimepoints, ArrayList<Timepoint> interfaceTimepoints, HashMap<String, Vector<MappedPack>> localInputPairs){
		constructorHelper(id, tempConstraints, localTimepoints, interfaceTimepoints, localInputPairs);
	}
	
	public SimpleDTP(int id, ArrayList<DisjunctiveTemporalConstraint> tempConstraints, ArrayList<Timepoint> localTimepoints, ArrayList<Timepoint> interfaceTimepoints, HashMap<String, Vector<MappedPack>> localInputPairs, HashMap<SimpleEntry<String,String>, Integer> prec){
		constructorHelper(id, tempConstraints, localTimepoints, interfaceTimepoints, localInputPairs); 
		this.precMap = prec;
	}
	protected void constructorHelper(int id, ArrayList<DisjunctiveTemporalConstraint> tempConstraints, ArrayList<Timepoint> localTimepoints, ArrayList<Timepoint> interfaceTimepoints, HashMap<String, Vector<MappedPack>> localInputPairs){
		this.id = id;
		this.interfaceTimepoints = interfaceTimepoints;
		this.contingentTimepoints = new ArrayList<Timepoint>();
		this.localTimepoints = localTimepoints;
		this.tempConstraints = tempConstraints;
		this.timepoints = new HashMap<String, Timepoint>();
		this.inputPairs = localInputPairs;
		if(localTimepoints != null){
			for (Timepoint tp : localTimepoints) {
				this.timepoints.put(tp.getName(), tp);
				tp.setLocalIndex(id, numTimepoints++);
			}
		}
		this.zero = timepoints.get("zero");
		assert(zero != null);
		updateInternalData();			
	}
	
	public void updateInternalData(){
		minimalNetwork = new IntervalSet[localTimepoints.size()][localTimepoints.size()];
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				minimalNetwork[i][j] = new IntervalSet();
			}
		}	
		resetTemporalNetwork();
	}
	
	@Override
	public abstract void printTimepointIntervals(PrintStream out, int time);	
	@Override
	public abstract void printTimepointIntervals(PrintStream out);
	
	@Override
	public String toString(){
		String str = "local: "+this.localTimepoints.toString()+"\ninterface: "+this.interfaceTimepoints.toString()+"\nfixed: "+this.fixedTimepoints.toString();
		return str;
	}
	
	@Override
	public void printConstraints(PrintStream out){
		out.println("Temporal constraints ("+tempConstraints.size()+"): ");
		printConstraintsHelper(out,tempConstraints);
		out.println("Incoming constraints");
		printConstraintsHelper(out,incomingConstraints);
		out.println("Volitional constraints:");
		printConstraintsHelper(out,volitionalConstraints);
	}
	

	
	private void printConstraintsHelper(PrintStream out, ArrayList<DisjunctiveTemporalConstraint> v){
		int c = 0;
		String s = "";
		for(int i = 0; i < v.size(); i++){
			s += v.get(i).toString()+",\t";
			c++;
			if(c >= 3){
				out.println(s);
				s = "";
				c = 0;
			}
		}
		if(c != 0){
			out.println(s);
		}
	}
	
	public void printDTPstats(){
		System.out.println("DTP has "+this.timepoints.size()+" temporal variables.");
		System.out.println("DTP has "+this.tempConstraints.size()+" temporal constraints, and "+this.incomingConstraints.size()+" incoming constraints.");
	}
	
	@Override
	public void printSelectionStack(){
		//System.out.println("SimpleDTP");
		System.out.println(selectionCount.toString());
//		this.printTimepointIntervals(System.out);
	}

	@Override
	public IntervalSet getInterval(String x, String y){
		int idx1 = getIndex(timepoints.get(x));
		int idx2 = getIndex(timepoints.get(y));
		if(x.equals("zero")) idx1 = 0;
		if(y.equals("zero")) idx2 = 0;
		if(idx1 <= idx2) return minimalNetwork[idx1][idx2];
		else return minimalNetwork[idx2][idx1].inverse();
	}
	
	@Override
	public Timepoint getTimepoint(String str){
		return timepoints.get(str);
	}

	//NOTE: all of the selection stack stuff is not currently being used. Was mostly implemented by Jason, but never worked fully. 
	@Override
	public int popSelection(int time){
		return popSelection(time, true);
	}
	@Override
	public int popSelection(int time, boolean resolve) {
		SimpleEntry<Integer,ArrayList<Timepoint>> selection = selectionCount.pop();
		for(Timepoint tp : selection.getValue()){
			this.fixedTimepoints.remove(tp);
		}
		for(int i = 0; i < selection.getKey(); i++){
			this.additionalConstraints.pop();
		}
		softReset();
		if(resolve) enumerateSolutions(time);
		//TODO: fix how this class handles the selection stack so that it can return an appropriate value
		return -1;
	}
	
	@Override
	public void tightenTimepoint(int timeS, String timepointS, int timeE, String timepointE, int deltaTime, boolean pushSelection){
		tightenTimepoint(timeS,timepointS,timeE,timepointE,true, deltaTime, pushSelection);
	}
	@Override
	public void tightenTimepoint(int timeS, String timepointS, int timeE, String timepointE, boolean resolve, int deltaTime, boolean pushSelection){
		Timepoint tpS = timepoints.get(timepointS);
		this.addNewTemporalBound(zero, tpS, timeS);
		this.addNewTemporalBound(tpS, zero, -timeS);
		Timepoint tpE = timepoints.get(timepointE);
		this.addNewTemporalBound(zero, tpE, timeE);
		this.addNewTemporalBound(tpE, zero, -timeE);
		this.fixedTimepoints.add(tpS);
		this.fixedTimepoints.add(tpE);
		ArrayList<Timepoint> temp = fixNoIntervalTimepoints();
		temp.add(tpS);
		temp.add(tpE);
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(4,temp));
		softReset();
		if(resolve){
			enumerateSolutions(timeE);
		}
	}

	public void tightenTimepoint(int time, String timepoint){
		tightenTimepoint(time,timepoint,true);
	}
	public void tightenTimepoint(int time, String timepoint, boolean resolve){
		Timepoint tp = timepoints.get(timepoint);
		this.addNewTemporalBound(zero, tp, time);
		this.addNewTemporalBound(tp, zero, -time);
		this.fixedTimepoints.add(tp);
		ArrayList<Timepoint> temp = fixNoIntervalTimepoints();
		temp.add(tp);
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(2,temp));
		softReset();
		if(resolve){
			enumerateSolutions(time);
		}
	}
	
	/*
	 * Set system clock time and update all activity EST to be legal with new time
	 */
	@Override
	public void advanceToTime(int time, int deltaTime, boolean pushSelection){
		advanceToTime(time,true, deltaTime, pushSelection);
	}
	@Override
	public void advanceToTime(int time, boolean resolve, int deltaTime, boolean pushSelection){
		int count = 0;
		
		// check each timepoint. If the EST of that timepoint is < time, adjust the EST to be = time
		for(Timepoint tp: timepoints.values()){
			if(tp.name.equals("showerM_E")) {
				// do nothing but debug breakpoint
				System.out.println("showerM");
			}
			if(tp.name.equals("zero") || tp.isAssigned()) continue;
//			System.out.println(tp.toString()+" "+minimalNetwork[0][getIndex(tp)].getLowerBound());
			if(minimalNetwork[0][getIndex(tp)].getLowerBound() > time) continue;
//			System.out.println("Adding "+zero.toString()+" - "+tp.toString()+" <= "+time);
			
			// DREW: This line adds a new constraint forcing EST of act to be time later - I think? <- was in og code
			this.addNewTemporalBound(zero, tp, time);
			count++;
		}
		ArrayList<Timepoint> temp = fixNoIntervalTimepoints();
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(count,temp));
//		System.out.println("About to softReset");
		//FIXME: With softReset below, advancing to time went overboard; without it premature end...
		softReset();
		if(resolve){
//			System.out.println("About to enumerateSolutions for time "+time);
			enumerateSolutions(time);
		}
	}
	
	
	@Override
	public void advanceSubDTPToTime(int time, int deltaT, boolean pushSelection, int dtpNum) {
		advanceToTime(time, deltaT, pushSelection);
		return;
	}
	
	@Override
	public void executeAndAdvance(int timeS, String timepointS, int timeE, String timepointE, boolean resolve, int deltaT, boolean pushSelection){
		Timepoint tpS = timepoints.get(timepointS);
		this.addNewTemporalBound(zero, tpS, timeS);
		this.addNewTemporalBound(tpS, zero, -timeS);
		Timepoint tpE = timepoints.get(timepointE);
		this.addNewTemporalBound(zero, tpE, timeE);
		this.addNewTemporalBound(tpE, zero, -timeE);
		this.fixedTimepoints.add(tpS);
		this.fixedTimepoints.add(tpE);
//		System.out.println("Fixing timepoints " + timepointS + " and " + timepointE);

		int count = 0;
		for(Timepoint tp: timepoints.values()){
			
			if(tp.name.equals("zero") || tp.isAssigned() || tp.equals(tpS) || tp.equals(tpE)) continue;
			if(minimalNetwork[0][getIndex(tp)].getLowerBound() > timeE) continue;
			
			// DREW: Attempt to fix shower bug by forcefully setting its EST to end of tlast act
			if( tp.getTime() < timeE && !tp.equals(tpS) && ! tp.equals(tpE)) {//tp.name.equals("showerM_S")) {
				this.addNewTemporalBound(zero, tp, timeE); // set this timepoint time to be next free time
				//this.addNewTemporalBound(tp, zero, -timeE);
			}
			
			count++;
		}
		
//		System.out.println("MinTime here is " + getMinTime());
		
		ArrayList<Timepoint> temp = fixNoIntervalTimepoints();
		temp.add(tpS);
		temp.add(tpE);
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(4+count,temp));
//		System.out.println("MinTime before softReset is " + getMinTime());
		softReset();
//		System.out.println("MinTime after softReset is " + getMinTime());
		cleanup(timeE, resolve, pushSelection);
//		System.out.println("MinTime after cleanup is " + getMinTime());
		// ED: added cleanup above, so commented out enumerateSolutions
//		if(resolve){
//			enumerateSolutions(timeE);
//		}
		
		// DREW ATTEMPT TO FIX SHOWER BUG
		//advanceToTime(timeE, deltaT, true);
	}

	
	/**
	 * Does some cleanup functionality after adding new constraints to the DTP.
	 * Options include: re-solving the DTP (and fixingZeroValueIntervals) and/or pushing the selectionStack
	 * @param time
	 * @param resolve
	 * @param pushSelection
	 */
	protected void cleanup(int time, boolean resolve, boolean pushSelection){
		softReset();
		while(resolve){
			enumerateSolutions(time);
			resolve = fixZeroValIntervals();
		}

	}
	

	@Override
	public void addAdditionalConstraint(String tpS, String tpE, IntervalSet dtc, int time, boolean resolve, boolean pushSelection) {
		Timepoint source = timepoints.get(tpS);
		Timepoint dest = timepoints.get(tpE);
		Collection<DisjunctiveTemporalConstraint> col = DisjunctiveTemporalConstraint.generateConstraint(source,dest,dtc);
		addAdditionalConstraints(col);
		cleanup(time, resolve, pushSelection);
	}
	
	/**
	 * Sweeps through the activities, and adds any activities that must have zero duration to fixedTimepoints
	 * @return The timepoints that are added to fixedTimepoints
	 */
	public ArrayList<Timepoint> fixNoIntervalTimepoints(){
		ArrayList<Timepoint> result = new ArrayList<Timepoint>();
		int idx = 1;
		while(idx < minimalNetwork.length){
			if(minimalNetwork[idx][idx+1].isZero() && !isFixed(idx)){
				fixedTimepoints.add(localTimepoints.get(idx));
				fixedTimepoints.add(localTimepoints.get(idx+1));
				result.add(localTimepoints.get(idx));
				result.add(localTimepoints.get(idx+1));

			}
			idx += 2;
		}
		return result;
	}
	
	@Override
	public boolean fixZeroValIntervals(){
		ArrayList<Timepoint> a = fixNoIntervalTimepoints();
		if(a.isEmpty()) return false;
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(0,a));
		return true;
	}
		
	/**
	 * Checks if the idx-th timepoint matches a selection criteria
	 * @param af Multiplexes between selection criteria
	 * @param idx The index of the timepoint to check
	 * @return true if the timepoint matches, false otherwise
	 */
	private boolean activityCheck(ActivityFinder af, int idx, int time){
		switch (af){
		case ALL:
			return !isFixed(idx);
		case VARDUR:
			return !isFixed(idx) && minimalNetwork[idx][idx+1].totalSize() != 1;
		case VARAVAIL:
			return !isFixed(idx) && (minimalNetwork[0][idx].totalSize() != 1 || minimalNetwork[0][idx+1].totalSize() != 1);
		case TIME:
			return !isFixed(idx) && minimalNetwork[0][idx].intersect(time);
		default:
			throw new java.lang.UnsupportedOperationException("ActivityFinder type \""+af.toString()+"\" is unsupported.");
		}
	}
	
	@Override
	public List<List<String>> getActivities(ActivityFinder af, int time){
		ArrayList<String> activities = new ArrayList<String>();
		int idx = 1;
//		System.out.println("MinimalNetwork length is " + minimalNetwork.length);
		while(idx < minimalNetwork.length){
			if(activityCheck(af,idx, time)){
//				System.out.println("Activity check succeeded for idx :" + idx);
				String s = localTimepoints.get(idx).getName();
				s = s.split("_")[0];
				activities.add(s);
			} else {
//				System.out.println("Activity check failed for idx :" + idx);
			}
			idx += 2;
		}
		List<List<String>> result = new LinkedList<>();
		result.add(activities);
		return result;
	}
	
	public List<String> getContingentActivities(int time){
		List<String> res = this.getActivities(ActivityFinder.TIME, time).get(0);
		List<String> ret = new ArrayList<String>();
		// we assume/know _S and _E timepoints are both in contingent, so we just check one
		for(String name : res){
			if(contingentTimepoints.contains(timepoints.get(name+"_S"))){
				ret.add(name);
			}
		}
		return ret;
	}
	
	@Override
	public List<Integer> getMaxSlack(){
		 List<Integer> result = new ArrayList<Integer>(1);
		 result.add((int) this.maxSlack);
		return result;
	}
	
	
	public List<Integer> getMinSlack(int scheduleTime){
		List<Integer> result = new ArrayList<Integer>(1);
		this.minSlack = computeMinSlack(scheduleTime);
		result.add((int) this.minSlack);
		return result;
	}
	
	@Override
	public void simplifyMinNetIntervals(){
		for(int j = 1; j < minimalNetwork[0].length; j++){
//			System.out.println("Simplifying MinNetInterval");
			minimalNetwork[0][j].simplify();
		}
	}
	
	@Override
	public int[] getMinTimeArray(){
		int[] result = new int[1];
		result[0] = getMinTime();
		return result;
	}
	
	@Override
	public int getMinTime(){
		int min = MAX_LET;
//		System.out.println("Min starts at: "+min);
		for(int j = 1; j < minimalNetwork[0].length; j++){
			if(-minimalNetwork[0][j].getUpperBound() < min && !isFixed(j)){
//				System.out.println("For j "+ j +" oldMin: "+Generics.toTimeFormat(min)+"\tnewMin: "+Generics.toTimeFormat((int) -minimalNetwork[0][j].getUpperBound()));
				
				min = (int) -minimalNetwork[0][j].getUpperBound();
			}
		}
//		System.out.println("getMinTime returning: " + min);
		return min;
	}
	
	private void updateConstraintSet(Collection<DisjunctiveTemporalConstraint> v){
		Iterator<DisjunctiveTemporalConstraint> it = v.iterator();
		while(it.hasNext()){
			DisjunctiveTemporalConstraint constraint = it.next();
			TemporalDifference difference = null;
			if(constraint.size()==1){
				difference = constraint.get(0);
			}
			else if(constraint.isAssigned()){
				difference = constraint.getAssigned();
			}
			if(difference!=null){
				//System.out.println("tightening "+difference.toString());
				temporalNetwork.tighten(getIndex(difference.source),getIndex(difference.destination),difference.bound,true);
			}
		}			
	}
	
	public int getIndex(Timepoint tp) {
		return tp.getLocalIndex(id);
	}
	
	public int getID(){
		return id;
	}
	
	private void updateTemporalNetwork(){
		//System.out.println("Updating TN tempconstraints. Length is " +tempConstraints.size());
		updateConstraintSet(tempConstraints);
		//System.out.println("Updating incoming constraints. Length is "+incomingConstraints.size());
		updateConstraintSet(incomingConstraints);
		//System.out.println("Updating additionalConstraints. Length is "+additionalConstraints.size());
		updateConstraintSet(additionalConstraints);
		//System.out.println("Updating volitionalConstraints. Length is "+volitionalConstraints.size());
		updateConstraintSet(volitionalConstraints);
	}
	
	/**
	 * WARNING: be careful calling this function because it throws away the subsumed constraints with no way to get them back 
	 */
	//TODO: extend this function to keep track of the constraints it subsumes, as well as code to add them back in if constraints are rolled back
	public void removeSubsumedVariables(){		
		STN tempNetwork = new STN(timepoints.size());

		for(int i=0; i<tempConstraints.size();i++){
			DisjunctiveTemporalConstraint constraint = tempConstraints.get(i);
			if(constraint.size()==1){
				TemporalDifference difference = constraint.get(0);
				tempNetwork.tighten(getIndex(difference.source),getIndex(difference.destination),difference.bound,true);
			}
		}
		for(int i=0; i<incomingConstraints.size();i++){
			DisjunctiveTemporalConstraint constraint = incomingConstraints.get(i);
			if(constraint.size()==1){
				TemporalDifference difference = constraint.get(0);
				tempNetwork.tighten(getIndex(difference.source),getIndex(difference.destination),difference.bound,true);
			}
		}

		Iterator<DisjunctiveTemporalConstraint> dtcIter = tempConstraints.iterator();
		while(dtcIter.hasNext()){
			DisjunctiveTemporalConstraint constraint = dtcIter.next();
			if(constraint.size()>1){  
				for(int i=0; i<constraint.size(); i++){
					TemporalDifference difference = constraint.get(i);
					if(tempNetwork.getBound(getIndex(difference.source), getIndex(difference.destination)) <= difference.bound){
						dtcIter.remove();
//												System.out.println("\nRemoving 1 Temporal Constraint");
						break;
					}
				}
			}
		}
		dtcIter = incomingConstraints.iterator();
		while(dtcIter.hasNext()){
			DisjunctiveTemporalConstraint constraint = dtcIter.next();
			if(constraint.size()>1){  
				for(int i=0; i<constraint.size(); i++){
					TemporalDifference difference = constraint.get(i);
					if (tempNetwork.getBound(getIndex(difference.source), getIndex(difference.destination)) <= difference.bound){
						dtcIter.remove();
//												System.out.println("\nRemoving 1 Temporal Constraint");
						break;
					}
				}
			}
		}
	}

	public void removeExternalConstraints(){		
		Iterator<DisjunctiveTemporalConstraint> dtcIter = tempConstraints.iterator();
		while(dtcIter.hasNext()){
			DisjunctiveTemporalConstraint constraint = dtcIter.next();
			if(constraint.size()>1){
				for(int i=0; i<constraint.size(); i++){
					TemporalDifference difference = constraint.get(i);
					if(!localTimepoints.contains(difference.source)) {
//						System.out.println("\n\nRemoving: "+constraint.toString());
						dtcIter.remove();
						break;
					}
					if(!localTimepoints.contains(difference.destination)) {
//						System.out.println("\n\nRemoving: "+constraint.toString());
						dtcIter.remove();
						break;
					}
				}
			}
		}
	}


	public void unassign(){
		Iterator<Timepoint> tpIter = timepoints.values().iterator();
		while(tpIter.hasNext()){
			tpIter.next().unassign();
		}
		Iterator<DisjunctiveTemporalConstraint> dtcIter = tempConstraints.iterator();
		while(dtcIter.hasNext()){
			dtcIter.next().unassign();
		}
		dtcIter = incomingConstraints.iterator();
		while(dtcIter.hasNext()){
			dtcIter.next().unassign();
		}
		dtcIter = volitionalConstraints.iterator();
		while(dtcIter.hasNext()){
			dtcIter.next().unassign();
		}
	}


	public void resetTemporalNetwork(){
		//System.out.println("In resetTemporalNetwork. size of timepoints is "+timepoints.size());
		this.temporalNetwork = new STN(localTimepoints.size());
		updateTemporalNetwork();
	}

	/**
	 * Resets the entire dtp back to its initial state
	 */
	public void reset(){
		softReset();
		additionalConstraints.clear();
		fixedTimepoints.clear();
		clearIncomingConstraints();
	}
	
	@Override
	public void softReset(){
		//System.out.println("Performing a soft reset");
		status = UNSOLVED;
		unassign();
		clearNumSolutions();
		resetTemporalNetwork();
		resetNumConflicts();
		resetNumDecisions();
		resetNumRestarts();
		resetTime();
		this.maxSlack = 0;
		this.minSlack = 0;
		resetMinNetwork();		
	}
	
	public void resetMinNetwork(){
		//System.out.println("Resetting/clearing minimal network");
		for(int i = 0; i < minimalNetwork.length; i++){
			for(int j = i+1; j < minimalNetwork[i].length; j++){
				minimalNetwork[i][j].reset();
			}
		}
	}
	
	@Override
	public double getRigidity(){
		double total=0;
		double counter=0;
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				total+=Math.pow(1.0/(1+minimalNetwork[i][j].totalSize()), 2);
				counter++;
			}
		}
		return Math.sqrt(total/counter);
	}
	
	@Override
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals(){
		double total=0;
		double counter=0;
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				total+=Math.pow(1.0/(1+minimalNetwork[i][j].totalSize()), 2);
				counter++;
			}
		}
		
		ArrayList<SimpleEntry<Double,Double>> r = new ArrayList<SimpleEntry<Double,Double>> ();
		r.add(new SimpleEntry<Double, Double>(total,counter));
		return r;
	}

	@Override
	public long getTotalFlexibility(){
		long total=0;
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				total+=minimalNetwork[i][j].totalSize();
			}
		}
		return total;
	}
	
	protected boolean isFixed(int idx){
		for(Timepoint tp : this.fixedTimepoints){
			if(getIndex(tp) == idx) return true;
		}
		return false;
	}
	 
	private void updateMinNetwork(int scheduleTime){
		// the max of these mins should give us dtp.maxSlack and taking the min of these mins should give us dtp.minSlack
		ArrayList<Double> allSlacks = new ArrayList<Double>();
		//stupid error checking thing to make sure that we're using a NEGATIVE schedule time
		if(scheduleTime > 0) scheduleTime = -1*scheduleTime;
		double minSlack = Double.POSITIVE_INFINITY;
		double earliestStart = Double.NEGATIVE_INFINITY;  //holds the earliest time that any timepoint could start
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				int upperBound = (int)temporalNetwork.getBound(getIndex(localTimepoints.get(i)), getIndex(localTimepoints.get(j)));
				int lowerBound = (int)(0-temporalNetwork.getBound(getIndex(localTimepoints.get(j)), getIndex(localTimepoints.get(i))));
				//if(i == 0 & j == 3) System.out.println("Upper is "+ upperBound +" and lower is "+lowerBound);
				if(lowerBound > upperBound){
					int temp = upperBound;
					upperBound = lowerBound;
					lowerBound = temp;
				}
				Interval interval = new Interval(lowerBound, upperBound);
				//if i = 0, the upperbound of the interval is the latest time that the activity can start. 
				// so I think actually for min slack, we want this to be interval.getLowerBound, right? 
				if(interval.getUpperBound() > earliestStart && !isFixed(j) && i == 0) earliestStart = interval.getUpperBound();
				if(i == 0 && !isFixed(j) && interval.getLowerBound() <= scheduleTime && earliestStart == interval.getUpperBound()){
					//System.out.println(interval);
					if(-interval.getUpperBound()+scheduleTime < minSlack){
						//System.out.println("i: " + i + " J: " + j);
						//System.out.println("timepoint: " + localTimepoints.get(j));
						//System.out.println("locminSlack: "+minSlack+"\toldLocdMin: "+(-interval.getLowerBound()+scheduleTime));
						minSlack = -interval.getLowerBound()+scheduleTime;
						//allSlacks.add(minSlack);
						//System.out.println("timepoint: "+localTimepoints.get(j).toString()+" -- "+interval.inverse().toString());
					}
				}
				minimalNetwork[i][j].add(interval);
			}
		}
		if(minSlack > this.maxSlack){
//			System.out.println("maxSlack: "+minSlack+"\toldMax: "+maxSlack);
			this.maxSlack = minSlack;
		}
		//System.out.println(allSlacks);
		//System.out.println("this.maxSlack: "+ this.maxSlack + " allSlacks.max: "+Collections.max(allSlacks) + "allSlacks.min: " +Collections.min(allSlacks));
		this.minSlack = minSlack;
		//System.out.println("maxSlack: " + this.maxSlack + " minSlack: "+ this.minSlack);
//		for(int i = 0; i < minimalNetwork.length; i++){
//			System.out.println(Arrays.deepToString(minimalNetwork[i]));
//		}
	//System.out.println("updateMinNetwork complete. Value of one of the intervals is "+minimalNetwork[0][3]+"\n");
	
	}
	
	public int computeMinSlack(int scheduleTime){
		if(scheduleTime > 0) scheduleTime = -1*scheduleTime;
		int minSlack = Integer.MAX_VALUE;
		double earliestStart = Double.NEGATIVE_INFINITY; 
		double minTime = getMinTime();
		ArrayList<Integer> mins = new ArrayList<Integer>();
		/*
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				//we want to iterate through the intervals in each interval set
				Iterator<Interval> itis = minimalNetwork[i][j].iterator();
				while (itis.hasNext()){
					Interval interval = itis.next();
				
					//if i = 0, the upperbound of the interval is the latest time that the activity can start. 
					if(interval.getLowerBound() > earliestStart && !isFixed(j) && i == 0) earliestStart = interval.getUpperBound();
					// I think that we can take out the later parts of this because minNetwork has already been 
					// 		updated and should only contain the intervals we want it to. 
					if(i == 0 && !isFixed(j) && interval.intersects(scheduleTime)){ // i think we only want to compute this for the start times. 
						//System.out.println(interval.getLowerBound());
						//System.out.println(interval);
						if(-interval.getUpperBound()+scheduleTime < minSlack){
							//System.out.println("i: " + i + " J: " + j);
							//System.out.println("old min slack: " + minSlack+"  new min slack: " + -interval.getLowerBound()+scheduleTime);
							//System.out.println("locminSlack: "+minSlack+"\toldLocdMin: "+(-interval.getLowerBound()+scheduleTime));
							mins.add((int) -interval.getLowerBound()+scheduleTime);
							//allSlacks.add(minSlack);
							//System.out.println("timepoint: "+localTimepoints.get(j).toString()+" -- "+interval.inverse().toString());
						}
					}
				}
			}	
		}
		if(mins.size() == 0) return 0;
		minSlack = Collections.min(mins);
		if(minSlack > maxSlack) minSlack = (int) maxSlack; //System.out.println("replacing min w max slack");
		//int oldIntervals = countIntervals(scheduleTime);
		List<String> oldActs = this.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, scheduleTime).get(0);
		//System.out.println("minslack starts as: " + minSlack + " and maxSlack starts as: " + maxSlack);
		//int newIntervals = ((SimpleDTP) temp).countIntervals(scheduleTime-minSlack);
		
//		while(!listContains(newActs, oldActs) && minSlack > 0){
//			//System.out.println("different activites are being offered! old: " + oldActs + " new: " + newActs);
//			minSlack -= 1;
//			newActs = getActivitesAfterIdle(scheduleTime, minSlack);
//			
//		}
		*/
		//DisjunctiveTemporalProblem temp = this.clone();
		List<String> newActs = getActivitiesAfterIdle(scheduleTime, minSlack);
		int maxNumActs = getActivitiesAfterIdle(scheduleTime, (int) maxSlack).size();
		int idleToReturn = (int) maxSlack; 
		int count = (int) maxSlack;
		//System.out.println("max slack: " + maxSlack);
		if(maxSlack == Double.POSITIVE_INFINITY){
			//Generics.printDTP(this);
			System.out.println("max value was infinity. returning zero.");
			minSlack = 0;
			return minSlack;
		}
		
		while(count >= 0){
			//System.out.println(count);
			newActs = getActivitiesAfterIdle(scheduleTime, count);
			if(maxNumActs < newActs.size()){
				System.out.println("max num acts: " + maxNumActs + " new num acts: " + newActs.size());
				maxNumActs = newActs.size();
				idleToReturn = count;
			}
			count -= 1;
		}
		
		minSlack = idleToReturn;
		
		System.gc();
		return minSlack;
	}
	
	public SimpleEntry<Integer, Integer> getMinSlackInterval(int scheduleTime){
		int lb = -1;
		int ub = Integer.MAX_VALUE;
		if(scheduleTime > 0) scheduleTime = -1*scheduleTime;
		int minSlack = Integer.MAX_VALUE;
		//double earliestStart = Double.NEGATIVE_INFINITY; 
		//double minTime = getMinTime();
		//ArrayList<Integer> mins = new ArrayList<Integer>();
		/*
		for(int i=0; i<minimalNetwork.length; i++){
			for(int j=i+1; j<minimalNetwork.length; j++){
				//we want to iterate through the intervals in each interval set
				Iterator<Interval> itis = minimalNetwork[i][j].iterator();
				while (itis.hasNext()){
					Interval interval = itis.next();
				
					//if i = 0, the upperbound of the interval is the latest time that the activity can start. 
					if(interval.getLowerBound() > earliestStart && !isFixed(j) && i == 0) earliestStart = interval.getUpperBound();
					// I think that we can take out the later parts of this because minNetwork has already been 
					// 		updated and should only contain the intervals we want it to. 
					if(i == 0 && !isFixed(j) && interval.intersects(scheduleTime)){ // i think we only want to compute this for the start times. 
						//System.out.println(interval.getLowerBound());
						//System.out.println(interval);
						if(-interval.getUpperBound()+scheduleTime < minSlack){
							//System.out.println("i: " + i + " J: " + j);
							//System.out.println("old min slack: " + minSlack+"  new min slack: " + -interval.getLowerBound()+scheduleTime);
							//System.out.println("locminSlack: "+minSlack+"\toldLocdMin: "+(-interval.getLowerBound()+scheduleTime));
							mins.add((int) -interval.getLowerBound()+scheduleTime);
							//allSlacks.add(minSlack);
							//System.out.println("timepoint: "+localTimepoints.get(j).toString()+" -- "+interval.inverse().toString());
						}
					}
				}
			}	
		}
		if(mins.size() == 0) return 0;
		minSlack = Collections.min(mins);
		if(minSlack > maxSlack) minSlack = (int) maxSlack; //System.out.println("replacing min w max slack");
		//int oldIntervals = countIntervals(scheduleTime);
		List<String> oldActs = this.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, scheduleTime).get(0);
		//System.out.println("minslack starts as: " + minSlack + " and maxSlack starts as: " + maxSlack);
		//int newIntervals = ((SimpleDTP) temp).countIntervals(scheduleTime-minSlack);
		
//		while(!listContains(newActs, oldActs) && minSlack > 0){
//			//System.out.println("different activites are being offered! old: " + oldActs + " new: " + newActs);
//			minSlack -= 1;
//			newActs = getActivitesAfterIdle(scheduleTime, minSlack);
//			
//		}
		*/
		//DisjunctiveTemporalProblem temp = this.clone();
		List<String> newActs = getActivitiesAfterIdle(scheduleTime, minSlack);
		int maxNumActs = getActivitiesAfterIdle(scheduleTime, (int) maxSlack).size();
		int idleToReturn = (int) maxSlack; 
		int count = (int) maxSlack;
		//System.out.println("max slack: " + maxSlack);
		if(maxSlack == Double.POSITIVE_INFINITY){
			//Generics.printDTP(this);
			System.out.println("max value was infinity. returning zero.");
			minSlack = 0;
			return new SimpleEntry<Integer, Integer>(0,0);
		}
		
		while(count >= 0){
			//System.out.println(count);
			newActs = getActivitiesAfterIdle(scheduleTime, count);
			if(maxNumActs < newActs.size()){
				System.out.println("max num acts: " + maxNumActs + " new num acts: " + newActs.size());
				maxNumActs = newActs.size();
				idleToReturn = count;
			}
			count -= 1;
		}
		
		minSlack = idleToReturn;
		
		System.gc();
		return new SimpleEntry<Integer, Integer>(lb,ub);
	}
	
	private boolean listContains(List<String> bigList, List<String> subList){
		for(String elm : subList){
			if(!bigList.contains(elm)) return false;
		}
		return true;
	}
	
	
	//helper function for calculating min slack
	private List<String> getActivitiesAfterIdle(int scheduleTime, int idle){
		
		DisjunctiveTemporalProblem temp = this.clone();
		
		temp.advanceToTime(-(-scheduleTime+idle), idle, true);
		temp.simplifyMinNetIntervals();
		System.gc();
		return temp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -(-scheduleTime+idle)).get(0);
	}

	private int countIntervals(int scheduleTime){
		int numIntervals = 0;
		for(int i = 0; i < minimalNetwork.length; i++){
			for( int j = i + 1; j < minimalNetwork.length; j++){
				if(i==0 && !isFixed(j)){
					numIntervals += minimalNetwork[i][j].numIntervals();
				}
			}
		}
		//System.out.println("Num intervals: " + numIntervals + "scheduleTime: " + scheduleTime);
		return numIntervals;
	}
	
	@Override
	public void enumerateSolutions(int scheduleTime){
		establishMinimality(new ArrayList<Timepoint>(timepoints.values()), scheduleTime);
	}

	@Override
	public void enumerateInfluences(int scheduleTime){
		establishMinimality(interfaceTimepoints, scheduleTime);
	}
	
	@Override
	public void establishMinimality(List<Timepoint> timepointsToConsider, int scheduleTime){
//		System.out.println("solving for scheduleTime: "+scheduleTime);
		addSolutionsAsNoGoods = true;
		solutionNoGood = new StringBuffer();
		maxSlack = 0;
		//System.out.println("About to enter while loop");
		while(solveSTN()){
			//System.out.println("In while(solveSTN()");
			String result = stpToSMT(temporalNetwork,timepointsToConsider);
			numSolutions++;
			if(result==null) break;
			//temporalNetwork.printSchedule();
			solutionNoGood.append("( not ");
			solutionNoGood.append(result);
			solutionNoGood.append(" ) \n");	
			//System.out.println("In establishMinimality, calling updateMinNetwork");
			updateMinNetwork(scheduleTime);
			unassign();
			resetTemporalNetwork();
			if(numSolutions%100==0){
				System.out.println("NUM SOLUTIONS: "+getNumSolutions()+" time: "+(time/1000000000)+" seconds");
				//return;
			}
		}

		addSolutionsAsNoGoods = false;
		status = SOLVED_ALL;
		unassign();
		resetTemporalNetwork();
	}
	
	public boolean checkValidity(){
		if(solveSTN()) return true;
		else return false;
	}

	@Override
	public boolean nextSolution(int scheduleTime){
		return solveNext(new ArrayList<Timepoint>(timepoints.values()), scheduleTime);
	}
	
	@Override
	public boolean nextInfluence(int scheduleTime){
		return solveNext(interfaceTimepoints, scheduleTime);
	}
	
	@Override
	public boolean solveNext(List<Timepoint> timepointsToConsider, int scheduleTime){
		addSolutionsAsNoGoods = true;
		solutionNoGood = new StringBuffer();
		boolean hasNextSolution = solveSTN();
		String result = stpToSMT(temporalNetwork,timepointsToConsider);
		numSolutions++;
		if(result==null){
			hasNextSolution=false;
		}
		if(hasNextSolution){
			//System.out.println("In solveNext, calling updateMinNetwork");
			updateMinNetwork(scheduleTime);
			solutionNoGood.append("( not ");
			solutionNoGood.append(result);
			solutionNoGood.append(" ) \n");	
		}

		unassign();
		resetTemporalNetwork();
		if(numSolutions%100==0) {
			System.out.println("NUM SOLUTIONS: "+getNumSolutions()+" time: "+(time/1000000000)+" seconds");
			//return false;
		}

		addSolutionsAsNoGoods = false;
		if(hasNextSolution) {
			status = SOLVING_INFLUENCE;
		}
		else{
			status = SOLVED_INFLUENCE;
		}
		return hasNextSolution;
	}
	
	@Override
	public ArrayList<DisjunctiveTemporalConstraint> computeSummarizingConstraints(ArrayList<Timepoint> timepointsToDecouple){
//		Generics.print2log("Summarizing: "+timepointsToDecouple.toString());
		ArrayList<DisjunctiveTemporalConstraint> result = new ArrayList<DisjunctiveTemporalConstraint>();
		for(int i = 0; i < timepointsToDecouple.size(); i++){ 
			Timepoint t = timepointsToDecouple.get(i);
			if(t.name.equals("zero")) continue;
			result.addAll(getInterval("zero", t.name).generateConstraints(t,zero));  //add availability constraints
			
			for(int j = i+1; j < timepointsToDecouple.size(); j++){
				Timepoint t2 = timepointsToDecouple.get(j);
				result.addAll(getInterval(t.name, t2.name).generateConstraints(t2,t));  //add interval constraints				
			}
		}
//		Generics.print2log("Summarizing results: "+result.toString());
		return result;		
	}
	
	public ArrayList<DisjunctiveTemporalConstraint> computeDecouplingConstraints(ArrayList<Timepoint> timepointsToDecouple){
		ArrayList<DisjunctiveTemporalConstraint> newConstraints= new ArrayList<DisjunctiveTemporalConstraint>();
		int zeroTime = zero.getTime();
//		Generics.print2log(timepointsToDecouple.toString());
		for (DisjunctiveTemporalConstraint constraint : tempConstraints) {
//			Generics.print2log("constraint: "+constraint.toString());
			TemporalDifference diff = constraint.getAssigned();
			if(timepointsToDecouple.contains(diff.source)){
				if(timepointsToDecouple.contains(diff.destination)){ 
//					Generics.print2log("true, true "+diff.toString());
					newConstraints.add(new DisjunctiveTemporalConstraint(diff));
				}
				else if(!diff.source.equals(zero)){
					int sourceTime = diff.source.getTime()-zeroTime;
					int destTime = diff.destination.getTime()-zeroTime;
					int newBound = sourceTime + (diff.bound-(sourceTime-destTime))/2;
					newConstraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(diff.source, zero, newBound)));
//					Generics.print2log("true, false "+diff.toString()+"\tst: "+sourceTime+"\tdt: "+destTime);
				}
			}
			else if(timepointsToDecouple.contains(diff.destination) && !diff.destination.equals(zero)){
				int sourceTime = diff.source.getTime()-zeroTime;
				int destTime = diff.destination.getTime()-zeroTime;
				int newBound =  (diff.bound-(sourceTime-destTime))/2 - destTime;
				newConstraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(zero,diff.destination, newBound)));
//				Generics.print2log("false "+diff.toString()+"\tst: "+sourceTime+"\tdt: "+destTime);
			}
		}
//		Generics.print2log(newConstraints.toString());
		return newConstraints;
	}
	
	/**
	 * Writes problem to file.  Format is dependent on extension (.xml or .smt).
	 * @param filename - name of file to write problem to
	 * @param append - append this to existing file, or overwrite
	 */
	public void writeToFile(String filename, boolean append){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			if(filename.contains(".smt")){
				//				out.write(toSMTString());
				writeSMTOut(out);
			}
			else {
				out.close();
				throw new HSPFileFormatException("writeToFile: Unsupported file format error");
			}
			out.close();
		} catch (IOException e) {
		}
	}

	private boolean solveYices(){
		callsToSolver++;
		writeToFile("temp.smt", false);

		try {
			if(DEBUG) System.out.println(yicesExecCommand+" -smt temp.smt -st -e ");
			long current = System.nanoTime();
			Process p = Runtime.getRuntime().exec(yicesExecCommand+" -smt temp.smt -st -e ");// /rs:"+seed);
			p.waitFor();
			time += (System.nanoTime() - current);
			InputStream inputstream = p.getInputStream();
			InputStream errorStream = p.getErrorStream();
			InputStreamReader errorstreamreader = new InputStreamReader(errorStream);
			BufferedReader errorbufferedreader = new BufferedReader(errorstreamreader);
			String errorLine;
			boolean errorsExist = false;
			while (errorbufferedreader.ready() && (errorLine = errorbufferedreader.readLine())!=null) {
				if (DEBUG) System.out.println(errorLine);
				errorsExist = true;
			}
			if(errorsExist) return false;

			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			String line;

			while ((line = bufferedreader.readLine())!=null) {
				if (DEBUG) System.out.println(line);
				if(line.contains("unsat")){
					status = INFEASIBLE; 
				}
				else if(line.contains("(=")){
					int startIndex = line.lastIndexOf(' ')+1;
					int endIndex = line.indexOf(")");
					//String line_num = line.substring(startIndex,endIndex).trim();
					
					//long long_time = Long.valueOf(line_num);
					//int time = (int) long_time;
					
					int time = Integer.valueOf(line.substring(startIndex,endIndex).trim());

					startIndex = line.indexOf(' ')+1;
					endIndex = line.lastIndexOf(' ');
					line = line.substring(startIndex, endIndex);
					//System.out.println("getting tp: "+ line);
					timepoints.get(line).assign(time);
				}
				else if(line.contains("num. conflicts:")){
					numConflicts+=Integer.parseInt(line.substring(line.indexOf(':')+1).trim());
				}
				else if(line.contains("num. decisions:")){
					numDecisions+=Integer.parseInt(line.substring(line.indexOf(':')+1).trim());
				}
			}

		}
		catch(Exception e ){
			System.out.println("catching exception in YICES");
			e.printStackTrace();
		}

		if(status!=INFEASIBLE){
			//Attempt to assign all temporal constraints
			//System.out.println("	Checking tempConstraints");
			checkConstraintAssignment(tempConstraints.iterator());
			//System.out.println("Checking incoming Constraints");
			checkConstraintAssignment(incomingConstraints.iterator());
			//System.out.println("Checking additionalconstraints");
			checkConstraintAssignment(additionalConstraints.iterator());
			
			checkConstraintAssignment(volitionalConstraints.iterator());
		}

		//Tighten the temporal network
		updateTemporalNetwork();
		//System.out.println("TN AFTER UPDATE");
		//temporalNetwork.printSchedule();
		if(status==INFEASIBLE){
			return false;
		}
		else{
			return true;
		}
	}

	private void checkConstraintAssignment(Iterator<DisjunctiveTemporalConstraint> dtcIter){
		while(dtcIter.hasNext()){
			DisjunctiveTemporalConstraint constraint = dtcIter.next();
			boolean result = constraint.assign();

			assert(result);
		}

	}
	
	//not used anymore. we only use Yices, but this is another SMT solver
	private boolean solveZ3(){
		writeToFile("temp.smt", false);
		boolean conflict = false;
		
		//This code solves using Z3, and writes output to console.
		try {
			if(DEBUG) System.out.println(z3ExecCommand+" temp.smt /m /st");
			Process p = Runtime.getRuntime().exec(z3ExecCommand+" temp.smt /m /st ");// /rs:"+seed);
			InputStream inputstream = p.getInputStream();
			InputStream errorStream = p.getErrorStream();
			InputStreamReader errorstreamreader = new InputStreamReader(errorStream);
			BufferedReader errorbufferedreader = new BufferedReader(errorstreamreader);
			String errorLine;
			boolean errorsExist = false;
			while (errorbufferedreader.ready() && (errorLine = errorbufferedreader.readLine())!=null) {
				if (DEBUG) System.out.println(errorLine);
				errorsExist = true;
			}
			if(errorsExist) return false;
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			String line;
			while ((line = bufferedreader.readLine())!=null) {
				System.out.println(line);
				if (DEBUG) System.out.println(line);
				if(line.contains("unsat")){
					status = INFEASIBLE; 
					System.out.println("FOUND UNSAT!");
				}
				else if(line.contains(":int")){
					int startIndex = line.indexOf("->")+2;
					int endIndex = line.indexOf(":int");
					int time = Integer.valueOf(line.substring(startIndex,endIndex).trim());

					startIndex = line.indexOf('{');
					endIndex = line.indexOf('}');
					line = line.substring(startIndex, endIndex);
					StringTokenizer lineTokenizer = new StringTokenizer(line,"*{}_ ");
					while(lineTokenizer.hasMoreTokens()){
						String token = lineTokenizer.nextToken();
						//						System.out.println("Token is: "+token+" Time is: "+time);
						timepoints.get(token).assign(time);
					}
				}
				else if(line.contains("num. conflicts:")){
					numConflicts+=Integer.parseInt(line.substring(line.indexOf(':')+1).trim());
				}
				else if(line.contains("num. decisions:")){
					numDecisions+=Integer.parseInt(line.substring(line.indexOf(':')+1).trim());
				}
				else if(line.contains("num. restarts:")){
					numRestarts+=Integer.parseInt(line.substring(line.indexOf(':')+1).trim());
				}
				else if(line.contains("time:")){
					time+=Double.parseDouble(line.substring(line.indexOf(':')+1,line.indexOf("secs")).trim());
				}
			}
		}
		catch(Exception e ){
			e.printStackTrace();
		}

		if(status!=INFEASIBLE){
			//Attempt to assign all temporal constraints
			checkConstraintAssignment(tempConstraints.iterator());
			checkConstraintAssignment(incomingConstraints.iterator());
			checkConstraintAssignment(additionalConstraints.iterator());
			checkConstraintAssignment(volitionalConstraints.iterator());
		}


		//Tighten the temporal network
		updateTemporalNetwork();
		//		Delete temporary file
		try {
			File tempFile = new File("temp.smt");
			tempFile.delete();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		if(status==INFEASIBLE){
			return false;
		}
		else{
			return true;
		}
	}

	/**
	 * Solves this HSP to a component STN.  Returns false if infeasible.
	 * @return
	 */
	public boolean solveSTN(){
		boolean solved = solveYices();
		if(solved) {
			status = SOLVED_STN;
		}
		return solved;
	}

	/**
	 * Creates a string of an SMT representation of this problem.
	 * @return
	 */
	public String toSMTString(){
		StringBuffer hsp = new StringBuffer();
		hsp.append("( benchmark ");
		hsp.append('\n');
		hsp.append(":source { unknown } ");
		hsp.append('\n');
		hsp.append(":status unknown ");
		hsp.append('\n');
		hsp.append(":category { unknown } ");
		hsp.append('\n');

		hsp.append(":difficulty { unknown } ");
		hsp.append('\n');
		hsp.append(":logic QF_IDL ");
		hsp.append('\n');
		Iterator<Timepoint> tpIter = timepoints.values().iterator();
		while(tpIter.hasNext()){
			hsp.append(tpIter.next().toSMTString());
		}
		hsp.append(":formula ");
		hsp.append('\n');
		hsp.append("( and ");
		hsp.append('\n');
		//		if(reference!=null){
		//			hsp.append("(= ");
		//			hsp.append(reference.getName());
		//			hsp.append(" 0 ) \n");
		//		}

		Collections.sort(tempConstraints);
		for(int i=0; i<tempConstraints.size(); i++){
			hsp.append(tempConstraints.get(i).toSMTString());
			hsp.append(tempConstraints.get(i).toAssignedSMTString());
		}
		
		Collections.sort(incomingConstraints);
		for(int i=0; i<incomingConstraints.size(); i++){
			hsp.append(incomingConstraints.get(i).toSMTString());
			hsp.append(incomingConstraints.get(i).toAssignedSMTString());
		}

		//		Collections.sort(additionalTemporalDifferences);
		for(int i=0; i<additionalConstraints.size();i++){
			hsp.append(additionalConstraints.get(i).toSMTString());
			hsp.append('\n');
		}
		
		Collections.sort(volitionalConstraints);
		for(int i=1; i < volitionalConstraints.size(); i++){
			hsp.append(volitionalConstraints.get(i).toSMTString());
			hsp.append(volitionalConstraints.get(i).toAssignedSMTString());
		}

		hsp.append("))");
		hsp.append('\n');	
		return hsp.toString();
	}

	public String stpToSMT(STN stp, List<Timepoint> timepoints){
		StringBuffer out = new StringBuffer();
		out.append("( and ");
		boolean anyEdges = false;
		for(int i=0;i < timepoints.size(); i++){
			Timepoint source = timepoints.get(i);
			for(int j=0; j<timepoints.size(); j++){
				if(i==j) continue;
				Timepoint destination = timepoints.get(j);
				if(stp.isInfinite(getIndex(source), getIndex(destination))) continue;
				anyEdges = true;
				int bound = (int)stp.getBound(getIndex(source), getIndex(destination));
				if(bound>=0){
					out.append("( <= ( - ");
					out.append(source.getName());
					out.append(' ');
					out.append(destination.getName());
					out.append(" ) ");
					out.append(Integer.toString(bound));
					out.append(" ) ");
				}
				else{
					out.append("( >= ( - ");
					out.append(destination.getName());
					out.append(' ');
					out.append(source.getName());
					out.append(" ) ");
					out.append(Integer.toString((0-bound)));
					out.append(" ) ");
				}
			}
		}
		if(!anyEdges) return null;
		out.append(" ) ");
		return out.toString();

	}

	public void writeSMTOut(BufferedWriter hsp){
		try{
			hsp.append("( benchmark temp.smt");
			hsp.append('\n');
			hsp.append(":source { unknown } ");
			hsp.append('\n');
			hsp.append(":status unknown ");
			hsp.append('\n');
			hsp.append(":category { unknown } ");
			hsp.append('\n');

			hsp.append(":difficulty { unknown } ");
			hsp.append('\n');
			hsp.append(":logic QF_IDL ");
			hsp.append('\n');
			//Print fd related predicates
			Iterator<Timepoint> tpIter = timepoints.values().iterator();
			while(tpIter.hasNext()){
				hsp.append(tpIter.next().toSMTString());
			}
			hsp.append(":formula ");
			hsp.append('\n');
			hsp.append("( and ");
			hsp.append('\n');
			//			if(reference!=null){
			//				hsp.append("(= ");
			//				hsp.append(reference.getName());
			//				hsp.append(" 0 ) \n");
			//			}
			Collections.sort(tempConstraints);
			for(int i=0; i<tempConstraints.size(); i++){
				hsp.append(tempConstraints.get(i).toSMTString());
				hsp.append(tempConstraints.get(i).toAssignedSMTString());
			}
			
			Collections.sort(incomingConstraints);
			for(int i=0; i<incomingConstraints.size(); i++){
				hsp.append(incomingConstraints.get(i).toSMTString());
				hsp.append(incomingConstraints.get(i).toAssignedSMTString());
			}
			
			Collections.sort(volitionalConstraints);
			for(int i=0; i < volitionalConstraints.size(); i++){
				hsp.append(volitionalConstraints.get(i).toSMTString());
				hsp.append(volitionalConstraints.get(i).toAssignedSMTString());
			}
			
			//This guarantees that we find a unique solution STP that has not been encountered before.
			if(addSolutionsAsNoGoods){
				hsp.append(solutionNoGood);
			}

			if(hasSupplementaryConstraints()){
				hsp.append(supplementaryConstraints());
			}

			for(int i=0; i<additionalConstraints.size();i++){
				hsp.append(additionalConstraints.get(i).toSMTString());
				hsp.append(additionalConstraints.get(i).toAssignedSMTString());
			}

			hsp.append("))");
			hsp.append('\n');
		}
		catch (IOException e) {
		}
	}

	@Override
	public int getNumSolutions(){
		return numSolutions;
	}
	
	public int getNumLocalTimepoints(){
		return localTimepoints.size();
	}
	
	public boolean contains(String str){
		return timepoints.containsKey(str);
	}
	
	public boolean isLocalTimepointPrivate(Timepoint tp){
		if(localTimepoints.contains(tp) && !interfaceTimepoints.contains(tp)) return true;
		return false;
	}
	
	public boolean hasZeroDurations(){
		for(int i = 1; i < timepoints.size() - 1; i+=2){
			int dur = (int) minimalNetwork[i][i+1].getLowerBound();
			if(dur == 0) return true;
		}
		return false;
	}
	
	@Override
	public void addInterfaceTimepoint(Timepoint tp){
		if(timepoints.get(tp.name) == null){ //this is a brand new timepoint
			timepoints.put(tp.name, tp);
			this.localTimepoints.add(tp);
			this.interfaceTimepoints.add(tp);
			updateInternalData();
		}
		else if(!this.interfaceTimepoints.contains(tp)){ //this timepoint already existed but wasn't interface before
			this.interfaceTimepoints.add(tp);
		}
	}

	@Override
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
		for(Timepoint tp: timepoints){
			addInterfaceTimepoint(tp);
		}
	}
	
	@Override
	public ArrayList<Timepoint> getInterfaceTimepoints(){
		return this.interfaceTimepoints;
	}

	public void clearNumSolutions(){
		numSolutions=0;
	}

	public void resetStatus(){
		status = SimpleDTP.UNSOLVED;
	}
	
	/**
	 * Add new temporal difference constraint (source - dest <= bound)
	 * @param sourceTimepoint
	 * @param destTimepoint
	 * @param bound
	 */
	public void addNewTemporalBound(String sourceTimepoint, String destTimepoint, int bound){
		additionalConstraints.push(new DisjunctiveTemporalConstraint(new TemporalDifference(timepoints.get(sourceTimepoint),timepoints.get(destTimepoint),bound)));
	}
	
	/**
	 * Add new temporal difference constraint (source - dest <= bound)
	 * @param sourceTimepoint
	 * @param destTimepoint
	 * @param bound
	 */
	public void addNewTemporalBound(Timepoint sourceTimepoint, Timepoint destTimepoint, int bound){
		additionalConstraints.push(new DisjunctiveTemporalConstraint(new TemporalDifference(sourceTimepoint,destTimepoint,bound)));
	}
	
	public void addVolitionalConstraints(Collection<DisjunctiveTemporalConstraint> col){
		volitionalConstraints.addAll(col);
	}
	
	public void addVolitionalConstraint(DisjunctiveTemporalConstraint dtc){
		volitionalConstraints.add(dtc);
	}

	@Override
	public void addAdditionalConstraints(Collection<DisjunctiveTemporalConstraint> collection){
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(collection.size(),new ArrayList<Timepoint>()));
		additionalConstraints.addAll(collection);
		
		//System.out.println("Adding new constraints");
		//System.out.println(collection);
	}
	
	@Override
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint newConstraint){
		selectionCount.push(new SimpleEntry<Integer,ArrayList<Timepoint>>(1,new ArrayList<Timepoint>()));
		additionalConstraints.add(newConstraint);
		//System.out.println("Adding new constraint");
	}

	public ArrayList<Timepoint> getLocalTimepoints(){
		return localTimepoints;
	}

	@Override
	public Set<Timepoint> getTimepoints(){
		return new TreeSet<Timepoint>(timepoints.values());
	}
	
	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getTempConstraints(){
		return tempConstraints;
	}
	
	public Stack<DisjunctiveTemporalConstraint> getAdditionalConstraints(){
		return additionalConstraints;
	}
	
	public ArrayList<DisjunctiveTemporalConstraint> getVolitionalConstraints() {
		return volitionalConstraints;
	}

	public void addTempConstraints(Collection<DisjunctiveTemporalConstraint> newConstraints){
		tempConstraints.addAll(newConstraints);
	}
	
	public ArrayList<DisjunctiveTemporalConstraint> getIncomingConstraints(){
		return incomingConstraints;
	}

	@Override
	public void addIncomingConstraint(DisjunctiveTemporalConstraint newConstraint){
		incomingConstraints.add(newConstraint);
	}

	@Override
	public void addIncomingConstraints(Collection<DisjunctiveTemporalConstraint> newConstraints){
		incomingConstraints.addAll(newConstraints);
	}

	@Override
	public void setIncomingConstraints(ArrayList<DisjunctiveTemporalConstraint> newConstraints){
		incomingConstraints = newConstraints;
	}

	@Override
	public void clearIncomingConstraints(){
		incomingConstraints.clear();
	}
	
	/**
	 * Returns number of conflicts (backtracks) encountered during search process
	 * @return
	 */
	public int getNumConflicts() {
		return numConflicts;
	}

	/**
	 * Resets the number of conflicts to 0
	 */
	public void resetNumConflicts() {
		this.numConflicts = 0;
	}

	/**
	 * Returns the number of decisions made during the search process
	 * @return
	 */
	public int getNumDecisions() {
		return numDecisions;
	}

	/**
	 * Resets the number of decisions to 0
	 */
	public void resetNumDecisions() {
		this.numDecisions = 0;
	}

	/**
	 * Gets the minimal network
	 */
	public IntervalSet[][] getMinimalNetwork(){
		return this.minimalNetwork;
	}
	
	/**
	 * Returns the number of restarts during the search process (determined heuristically)
	 * @return
	 */
	public int getNumRestarts() {
		return numRestarts;
	}

	/**
	 * Resets the number of restarts to 0
	 */
	public void resetNumRestarts() {
		this.numRestarts = 0;
	}

	/**
	 * Returns the amount of time (in seconds) spent solving the problem 
	 * @return
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Resets the time to 0
	 */
	public void resetTime() {
		this.time = 0;
	}

	public boolean feasible(){
		if (status != INFEASIBLE){
			return true;}
		else {return false;}
	}

	public boolean scheduled(){
		if (status == SOLVED_SCHEDULE){
			return true;}
		else {return false;}
	}

	public void setSupplementaryConstraints(String supplementaryConstraints) {
		this.supplementaryConstraints = supplementaryConstraints;
	}

	public String supplementaryConstraints() {
		return supplementaryConstraints;
	}

	public boolean hasSupplementaryConstraints() {
		return (supplementaryConstraints!=null);
	}
	
	@Override
	public int getNumAgents(){
		return 1;
	}
	
	@Override
	public int getCurrentAgent(){
		return 0;
	}
	
	@Override
	public void setCurrentAgent(int agent){}
	
	@Override
	public int getAgent(String tpS){
		return 0;
	}
	
	public void printFixedIntervals(PrintStream out, int time){
		int idx = 1;
		while(idx < minimalNetwork.length){
			if(time == Integer.MIN_VALUE || minimalNetwork[0][idx].intersect((int) time)){
				String s1 = isFixed(idx) ? "*" : "";
				String s2 = isFixed(idx+1) ? "*" : "";
				if(isFixed(idx) || isFixed(idx+1)){
					out.println(localTimepoints.get(idx).getName()+s1+": "+minimalNetwork[0][idx].inverse().toString()+"\t"+localTimepoints.get(idx+1).getName()+s2+": "+minimalNetwork[0][idx+1].inverse().toString()
							+"\tinterval: "+minimalNetwork[idx][idx+1].inverse().toString());				
				}
			}
		}
	}

	public boolean existsFixedTimepoint(){
		return fixedTimepoints.size() > 0;
	}
	
	public boolean timepointExists(String tpname) {
		for (Timepoint tp : timepoints.values()) {
			if(tp.getName()==tpname) {
				return true;
			}
		}
		return false;
	}
	
	public int getLatestStartTime(Timepoint tp){
		int ind = tp.getLocalIndex(id);
		IntervalSet is = minimalNetwork[0][ind];
		//System.out.println(is.toString());
		double ret = is.inverse().getUpperBound();
		//System.out.println("Latest start time is " + ret);
		return (int) (1 * ret);
	}
	
	public int getLatestEndTime(Timepoint tp){
		int ind = tp.getLocalIndex(id);
		IntervalSet is = minimalNetwork[0][ind];
		double ret = is.inverse().getUpperBound();
		return (int) ret;
	}
	
	public int getEarliestEndTime(Timepoint tp){
		int ind = tp.getLocalIndex(id);
		IntervalSet is = minimalNetwork[0][ind];
		double ret = is.inverse().getLowerBound();
		return (int) ret;
	}
	
	public int getEarliestStartTime(Timepoint tp) {
		int ind = tp.getLocalIndex(id);
		IntervalSet is = minimalNetwork[0][ind];
		//System.out.println(is.toString());
		double ret = is.inverse().getLowerBound();
		//System.out.println("Earliest start time is "+ ret);
		return (int) (1 * ret);
	}
	
	//returns the start timepoint of the last activity of the day. 
	public Timepoint getLastActivity(){
		Timepoint max = zero; 
		double max_time = -1;
		double temp;
		for(int i = 1; i < minimalNetwork.length; i+=2){
			temp = -minimalNetwork[0][i].getUpperBound();
			if(temp > max_time) max_time = temp; max = localTimepoints.get(i);
		}
		//System.out.println("Last activity of the day is: "+ max.getName());
		return max;
	}
	
	
	
	// forces source_E to come before dest_S by [min_duration, max_duration]
	public void addOrderingConstraint(String source, String dest, int min_duration, int max_duration){
		//check to see if either is the zero timepoint and adjust accordingly 
		Timepoint source_E = null;
		Timepoint dest_S= null;
		if(source.equals("zero")) source_E = timepoints.get("zero");
		else source_E = timepoints.get(source +"_E");
		if(dest.equals("zero")) dest_S = timepoints.get("zero");
		else dest_S = timepoints.get(dest +"_S");
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(new TemporalDifference(source_E, dest_S, -min_duration));
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(new TemporalDifference(dest_S, source_E, max_duration));
		additionalConstraints.add(dtc1);
		additionalConstraints.add(dtc2);
	}
	
	public void addOriginalOrderingConstraint(String source, String dest, int min_duration, int max_duration){
		//check to see if either is the zero timepoint and adjust accordingly 
		Timepoint source_E = null;
		Timepoint dest_S= null;
		if(source.equals("zero")) source_E = timepoints.get("zero");
		else source_E = timepoints.get(source +"_E");
		if(dest.equals("zero")) dest_S = timepoints.get("zero");
		else dest_S = timepoints.get(dest +"_S");
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(new TemporalDifference(source_E, dest_S, -min_duration));
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(new TemporalDifference(dest_S, source_E, max_duration));
		tempConstraints.add(dtc1);
		tempConstraints.add(dtc2);
	}
	
	public ArrayList<DisjunctiveTemporalConstraint> getOrderingConstraint(String source, String dest, int min_duration, int max_duration){
		Timepoint source_E = timepoints.get(source +"_E");
		Timepoint dest_S = timepoints.get(dest +"_S");
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(new TemporalDifference(source_E, dest_S, -min_duration));
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(new TemporalDifference(dest_S, source_E, max_duration));
		ArrayList<DisjunctiveTemporalConstraint> res = new ArrayList<DisjunctiveTemporalConstraint>();
		res.add(dtc1); res.add(dtc2);
		return res;
	}
	
	public ArrayList<DisjunctiveTemporalConstraint> getNonconcurrentConstraint(String source, String dest){
		Timepoint source_S = timepoints.get(source+"_S");
		Timepoint source_E = timepoints.get(source+"_E");
		Timepoint dest_S = timepoints.get(dest+"_S");
		Timepoint dest_E = timepoints.get(dest+"_E");
		
		DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(dest_E, source_S, 0));
		dtc.add(new TemporalDifference(source_E, dest_S, 0));
		ArrayList<DisjunctiveTemporalConstraint> res = new ArrayList<DisjunctiveTemporalConstraint>();
		res.add(dtc);
		return res;
	}
	
	public void addNonconcurrentConstraint(String source, String dest, int agent){
		Timepoint source_S = timepoints.get(source+"_S");
		Timepoint source_E = timepoints.get(source+"_E");
		Timepoint dest_S = timepoints.get(dest+"_S");
		Timepoint dest_E = timepoints.get(dest+"_E");
		
		DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(dest_E, source_S, 0));
		dtc.add(new TemporalDifference(source_E, dest_S, 0));
		addAdditionalConstraint(dtc);
	}
	
	public void addOriginalNonconcurrentConstraint(String source, String dest, int agent){
		Timepoint source_S = timepoints.get(source+"_S");
		Timepoint source_E = timepoints.get(source+"_E");
		Timepoint dest_S = timepoints.get(dest+"_S");
		Timepoint dest_E = timepoints.get(dest+"_E");
		
		DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(dest_E, source_S, 0));
		dtc.add(new TemporalDifference(source_E, dest_S, 0));
		tempConstraints.add(dtc);
	}
	
	public void addDurationConstraint(Timepoint start, Timepoint end, int duration){
		TemporalDifference tdmin = new TemporalDifference(start, end, -duration);
		TemporalDifference tdmax = new TemporalDifference(end,start, duration);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		System.out.println(tdVec);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		additionalConstraints.addAll(dtcs); 
	}
	
	public ArrayList<String> getActivityNames() {
		ArrayList<String> strs = new ArrayList<String>();
		for(int i = 1; i < numTimepoints; i += 2){
			Timepoint tp = localTimepoints.get(i);
			String s = tp.getName();
			String substr = s.substring(0,s.length() - 2);
			strs.add(substr);
		}
		return strs;
	}
	
	public ArrayList<String> getActivityNames(int agent){
		return getActivityNames();
	}
	

	public int checkBookends(int duration){
		int num_conflicts = 0;
		double val_lb = 0;
		double val_ub = 0;
		IntervalSet is;
		for(int i = 2; i < minimalNetwork.length; i+=2){
			for(int j = minimalNetwork.length - 2; j > 0; j-=2){
				if(checkPairedTimepoints(i,j)) continue;
				if(isFixed(i) || isFixed(j)) continue;
				//System.out.println(i + " " + j);
				if(i < j) is = minimalNetwork[i][j];
				else is = minimalNetwork[j][i];
				
				val_lb = is.getLowerBound();
				val_ub = is.getUpperBound();
				String tp1 = localTimepoints.get(i).getName();
				String tp2 = localTimepoints.get(j).getName();
				
				if ((int) (val_ub - val_lb) < duration){
					System.out.println("BOOKEND between "+ tp1 +" " + tp2);
					//System.out.println(is.toString());
					num_conflicts++;
				}
			}
		}
		System.out.println("NUM CONFLICTS = " + num_conflicts);
		return num_conflicts;
	}
	
	
	
	
	
	public int resolveConflicts(int duration, int sep){
		int dur = duration;
		while(checkBookends(dur) > 0){
			dur = dur - sep;
		}
		return dur;
		
	}
	
	public boolean checkPairedTimepoints(int id1, int id2){
		Timepoint tp1 = localTimepoints.get(id1);
		Timepoint tp2 = localTimepoints.get(id2);
		String name1 = tp1.getName().substring(0,tp1.getName().length()-2);
		String name2 = tp2.getName().substring(0,tp2.getName().length() - 2);
		//System.out.println(name1 + " " + name2);
		if(name1.equals(name2)){
			return true;
		}

		return false;
	}
	
	// a version of getDeltas that takes in an argument that specifies the minimum threshold of delta you want to show.
	// note getDeltas(prevDTP, 0) is the same as the below function
	//TODO: combine this and the function below??
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, int threshold) {
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		ArrayList<Delta> allDeltas = getDeltas(prevDTP);
		
		for(Delta delt : allDeltas){
			if(delt.getAbsoluteDifference() > threshold){
				deltas.add(delt);
			}
		}
		return deltas;
	}
	
	// a version of getDeltas that takes in a relative amount by which the interval has changed and returns deltas that fit this criteria
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, double relThreshold){
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		ArrayList<Delta> allDeltas = getDeltas(prevDTP);	
		for(Delta delt : allDeltas){
			
			// we want to add this to the return set if the ratio of prevDiff / currDiff is >= relative threshold.
			if (delt.getRelativeDifference() >= relThreshold) deltas.add(delt); System.out.println("relative diff: " + delt.getRelativeDifference());
		}
		return deltas;
	}
	
	// get all deltas between previous DTP and current dtp (this)

	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP) {
		int i;
		int j;
		int len = this.getNumLocalTimepoints();
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		for(i=1; i < len; i++){
			//TODO: Add code that makes sure we aren't looking at any timepoints that have already been fixed! 
			String tp1 = this.localTimepoints.get(i).getName();
			String tp2 = this.localTimepoints.get(0).getName();
			Delta delta = new Delta(this, prevDTP, tp1, tp2, "ALL");
			
			if (delta.getAbsoluteDifference() > 0 || delta.getLengthDifference() != 0){
				deltas.add(delta);
			}
		} for(j=1; j < len-1; j=j+2){
			String tpS = this.localTimepoints.get(j).getName();
			String tpE = this.localTimepoints.get(j+1).getName();
			Delta delta = new Delta(this, prevDTP, tpS, tpE, "ALL");

			if (delta.getAbsoluteDifference() > 0 || delta.getLengthDifference() != 0){
				deltas.add(delta);
			}			
		}
		return deltas;

	}
	
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, DeltaFinder df) {
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		ArrayList<Delta> allDeltas = getDeltas(prevDTP);
		
		for(Delta delt : allDeltas){
			switch(df){
			case DC:
				if(delt.getLengthDifference() != 0) {
					deltas.add(delt);
				}
			
			case LEN:
				if(delt.getAbsoluteDifference() > 0) deltas.add(delt);
			case ALL:
				return allDeltas;
			}
			
		}
		return deltas;
		
	}

	// returns a list of the top rankLim deltas to be passed to printDeltas
	public ArrayList<Delta> rankDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim){
		ArrayList<Delta> allDeltas = getDeltas(prevDTP);
		Collections.sort(allDeltas, Delta.AbsoluteDifferenceComparator);
		ArrayList<Delta> returnDeltas = new ArrayList<Delta>();
		//System.out.println("In rank deltas");
		for(Delta elm: allDeltas) System.out.println("abs diff: " + elm.getAbsoluteDifference());
		for(int i=0; i < rankLim; i++){
			
			if(i < allDeltas.size()) returnDeltas.add(allDeltas.get(i));
		}
		return returnDeltas;
	}
	
	public ArrayList<Delta> rankRelativeDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim){
		ArrayList<Delta> allDeltas = getDeltas(prevDTP);
		Collections.sort(allDeltas, Delta.RelativeDifferenceComparator);
		ArrayList<Delta> returnDeltas = new ArrayList<Delta>();
		//System.out.println("In rank deltas");
		//for(Delta elm: allDeltas) System.out.println(elm.getRelativeDifference());
		for(int i=0; i < rankLim; i++){
			
			if(i < allDeltas.size()) returnDeltas.add(allDeltas.get(i));
		}
		return returnDeltas;
	}
	
	public void printDeltas(ArrayList<Delta> deltas){
		int len = deltas.size();
		int i;
		if(len == 0) System.out.println("No deltas found.");
		for(i = 0; i < len; i++){
			String out_str = deltas.get(i).toString();
			System.out.println(out_str);
		
		}
	}
	public SimpleDTP clone() {		
		if(this == null) return null;
		SimpleDTP newDTP;
		
		//we want to use these cloned timepoints in all the other structures below. 
		HashMap<String, Timepoint> newTimepoints = new HashMap<String, Timepoint>();
		for(String key : timepoints.keySet()) newTimepoints.put(key, timepoints.get(key).clone());	
		ArrayList<DisjunctiveTemporalConstraint> newTempConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		for (DisjunctiveTemporalConstraint dtc : tempConstraints) {
			DisjunctiveTemporalConstraint newDTC = dtc.clone(newTimepoints);
			newTempConstraints.add(newDTC);
		}
		
		ArrayList<Timepoint> newLocalTimepoints = new ArrayList<Timepoint>();
		for(Timepoint tp: localTimepoints) newLocalTimepoints.add(newTimepoints.get(tp.getName()));
		
		ArrayList<Timepoint> newInterfaceTimepoints = new ArrayList<Timepoint>();
		for(Timepoint tp: interfaceTimepoints) newInterfaceTimepoints.add(newTimepoints.get(tp.getName()));
		
		
		
		int len = additionalConstraints.size();
		Stack<DisjunctiveTemporalConstraint> newAdditionalConstraints = new Stack<DisjunctiveTemporalConstraint>();
		Stack<DisjunctiveTemporalConstraint> tempStack = new Stack<DisjunctiveTemporalConstraint>();
		ArrayList<DisjunctiveTemporalConstraint> tempList = new ArrayList<DisjunctiveTemporalConstraint>();
		for(int i = 0; i < len; i++) tempList.add(additionalConstraints.pop());
		for(int j = 0; j < len; j++) tempStack.push(tempList.get(j).clone(newTimepoints));
		for(int k = 0; k < len; k++) {
			newAdditionalConstraints.push(tempStack.pop());
			additionalConstraints.push(tempList.get(k));
		}
		
	
		ArrayList<DisjunctiveTemporalConstraint> newIncomingConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		for(DisjunctiveTemporalConstraint dtc : incomingConstraints) newIncomingConstraints.add(dtc.clone(newTimepoints));
		
		ArrayList<DisjunctiveTemporalConstraint> newVolitionalConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		for(DisjunctiveTemporalConstraint dtc : volitionalConstraints) newVolitionalConstraints.add(dtc.clone(newTimepoints));
		
		ArrayList<Timepoint> newFixedTimepoints = new ArrayList<Timepoint>();
		// I think here we can just pull these from the hashmap of timepoints we've cloned. 
		// I think those that have been fixed will already be fixed in that big list. 
		for(Timepoint tp : fixedTimepoints) newFixedTimepoints.add(newTimepoints.get(tp.getName()));
		
		ArrayList<Timepoint> newContingentTimepoints = new ArrayList<Timepoint>();
		for(Timepoint tp : contingentTimepoints) newContingentTimepoints.add(newTimepoints.get(tp.getName()));
		
		if(this instanceof AgentDTP){	
			newDTP = new AgentDTP(id, newTempConstraints, newLocalTimepoints, newInterfaceTimepoints, inputPairs);
		}
		else if(this instanceof SharedDTP) {		
			newDTP = new SharedDTP(id, newTempConstraints, newLocalTimepoints, newInterfaceTimepoints, inputPairs);
		}
		else throw new IllegalArgumentException("Unrecognized SimpleDTP type");
		newDTP.numTimepoints = numTimepoints;
		newDTP.timepoints = newTimepoints;
		for(int i = 0; i < minimalNetwork.length; i++) {
			for (int j = 0; j < minimalNetwork[i].length; j++){
				if(minimalNetwork[i][j] != null){
					//System.out.println("Cloning interval between "+i+" and "+j);
					//FIXME: Note, this won't make an exact copy.
					//If there is overlap between intervals in this intervalset then copy collapses them
					//That is a reason just cloning a dtp can yield a different flexibility measure
					newDTP.minimalNetwork[i][j] = minimalNetwork[i][j].clone();
					//System.out.println("Old: "+minimalNetwork[i][j]+" New: "+newDTP.minimalNetwork[i][j]);
				}
			}
		}
		//newDTP.timepoints = newTimepoints;
		newDTP.maxSlack = maxSlack;
		newDTP.minSlack = minSlack;
		newDTP.additionalConstraints = newAdditionalConstraints;
		newDTP.incomingConstraints = newIncomingConstraints;
		newDTP.volitionalConstraints = newVolitionalConstraints;
		newDTP.supplementaryConstraints = supplementaryConstraints; //String
		newDTP.fixedTimepoints = newFixedTimepoints;
		newDTP.contingentTimepoints = newContingentTimepoints;
		newDTP.addSolutionsAsNoGoods = addSolutionsAsNoGoods;
		newDTP.solutionNoGood = solutionNoGood;
		
		newDTP.numConflicts = numConflicts;
		newDTP.numDecisions = numDecisions;
		newDTP.numRestarts = numRestarts;
		newDTP.numSolutions = numSolutions;
		newDTP.time = time;
		newDTP.status = status;
		newDTP.id = id;
		newDTP.idleNode = idleNode;
		newDTP.temporalNetwork = temporalNetwork.clone();
		newDTP.se_act = se_act;
		newDTP.precMap = precMap; //TODO: DOES THE PREC MAP ACTUALLY NEED TO BE CLONED?????
		//newDTP.callsToSolver = callsToSolver;
		//System.out.println("Finished a SimpleDTP");
		return newDTP;
	}
	
	public void setSEAct(String act){
		this.se_act = act;
	}
	
	public void addTimepoint(Timepoint tp){
		tp.setLocalIndex(id, numTimepoints++);
		localTimepoints.add(tp);
		timepoints.put(tp.getName(), tp);
		updateInternalData();
	}
	
	public void addContingentTimepoint(Timepoint tp){
		int newInd = numTimepoints++;
		//System.out.println("adding timepoint with index " + newInd);
		tp.setLocalIndex(id, newInd);
		contingentTimepoints.add(tp);
		localTimepoints.add(tp); // adding this so getActivites works. might not make sense to keep it this way tho
		timepoints.put(tp.getName(), tp);
		//updateInternalData(); // do I need this call her ein this function?
	}
	
	public int getValidity(){
		return this.VALID;
	}
	
	public void updateValidity(int val){
		if(val != 0 && val != 1){
			System.out.println("ERROR: Incorrect value for VALID.");
		} else {
			this.VALID = val;
		}
	}
	
	public ArrayList<Timepoint> getFixedTimepoints(){
		return this.fixedTimepoints;
	}
	
	public void addFixedTimepoint(Timepoint tp){
		this.fixedTimepoints.add(tp);
	}
	
	public void addFixedTimepoints(Collection<Timepoint> tps){
		for(Timepoint t : tps){
			addFixedTimepoint(t);
		}
	}
	
	public int getCallsToSolver(){
		return this.callsToSolver;
	}
	
	public List<String> getUnFixedTimepoints(){
		List<String> unfixed = new ArrayList<String>();
		List<Timepoint> fixed = getFixedTimepoints();
		for(String name : timepoints.keySet()){
			if(name.equals("zero")) continue;
			if(!unfixed.contains(name) && !fixed.contains(timepoints.get(name))) unfixed.add(name);
		}
		
		return unfixed;
	}
	
	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getSpecificTempConstraints(Timepoint tp){
		if(this.contains(tp.name))
			return this.tempConstraints;
		else
			return null;
	}
	
	
	@Override
	public double getOriginalUpperBound(Timepoint tp){
		double ub = 0;
		String name = tp.name;
		for (DisjunctiveTemporalConstraint i : this.tempConstraints){
			if(i.getTemporalDifferences().size() >= 1){
				TemporalDifference temp = i.getTemporalDifferences().get(0);
				if(temp.destination.name == name){
					if(temp.source.name == "zero"){
						if(ub >= temp.bound){
							ub = temp.bound;
						}
					}
				}	
			}

		}
		return ub;
	}
	
	@Override
	public double getOriginalLowerBound(Timepoint tp){
		double ub = 0;
		String name = tp.name;
		for (DisjunctiveTemporalConstraint i : this.tempConstraints){
			if(i.getTemporalDifferences().size() >= 1){
				TemporalDifference temp = i.getTemporalDifferences().get(0);
				if(temp.source.name == name){
					if(temp.destination.name == "zero"){
						if(ub <= temp.bound){
							ub = temp.bound;
						}
					}
				}
			}

		}
		return ub;
	}
	
	@Override
	public Vector<MappedPack> getRelatedInputPairs(String crucial) {
		if(inputPairs.containsKey(crucial))
			return inputPairs.get(crucial);
		else
			return new Vector<MappedPack>();
	}
	
	public void addNewTimepoint(Timepoint tp){
		String tp_name = tp.getName();
		tp.setLocalIndex(id, numTimepoints++);
		localTimepoints.add(tp);
		timepoints.put(tp_name, tp);
		updateInternalData();
	}
	
	public void addNewTimepoint(String name){
		Timepoint tp = new Timepoint(name,1);
		addNewTimepoint(tp);
	}
	
	public ArrayList<Interval> getDTPBoundaries(){
		// we want the earliest EST and the latest LET. 
		double max_time = Double.MIN_VALUE;
		double min_time = Double.MAX_VALUE;
		double temp; 
		int min_ind = -1;
		int max_ind = -1;
		ArrayList<Interval> ret = new ArrayList<Interval>();
		for(int i = 1; i < minimalNetwork.length; i+=2){
			// we look at the start times of activities to get the minimum value. 
			temp = -minimalNetwork[0][i].getUpperBound();
			//System.out.println("temp is: " + temp +" min_time is: " + min_time);
			//System.out.println("min_ind is: " + min_ind);
			if(temp < min_time) {
				min_time = temp; 
				min_ind = i;
			}
		}
		for(int j=2; j < minimalNetwork.length; j+=2){
			// we look at the end times of activities to get the maximum value. 
			temp = -minimalNetwork[0][j].getLowerBound();
			if(temp > max_time) max_time = temp; max_ind = j;
		}
		
		//convert min and max time to Time format. 
		String formatted_min = Generics.toTimeFormat((int) min_time);
		String formatted_max = Generics.toTimeFormat((int) max_time);
		//System.out.println("min_ind: " + min_ind + " max_ind: " + max_ind);
		//System.out.println(minimalNetwork[0][min_ind]);
		//System.out.println(minimalNetwork[0][max_ind]);
		//System.out.println("The boundaries are [" + formatted_min + ", " + formatted_max + "]");
		Interval inter = new Interval(min_time, max_time);
		ret.add(inter);
		return ret;
	}
	
	public void updateTimepointName(Timepoint tp, String new_name){
		String curr_name = tp.getName();
		tp.changeName(new_name);
		// update the entry in the timepoints hashtable
		timepoints.remove(curr_name);
		timepoints.put(new_name, tp);
		updateInternalData();
	}
	
	public void removeDurationConstraint(String tpName){
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		
		for(int i = 0; i < tempConstraints.size(); i++){
			boolean remove = false;
			DisjunctiveTemporalConstraint dtc = tempConstraints.get(i);
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains(tpName) && td.destination.getName().contains(tpName)){
					//System.out.println("Removing constraint: " + dtc.toString() + " from temp constraints");
					remove = true;
				}
			}
			if(remove) toRemove.add(i);
		}
		
		//remove durational constraints
		for(int ind : toRemove) tempConstraints.remove(ind);
		toRemove.clear();
		
		/**
		for(DisjunctiveTemporalConstraint dtc : tempConstraints){
			boolean remove = false;
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains(tpName) && td.destination.getName().contains(tpName)){
					System.out.println("Removing constraint: " + dtc.toString());
					remove = true;
				}
			}
			if(remove) tempConstraints.remove(dtc);
		} **/
		for(int i = 0; i < additionalConstraints.size(); i++){
			boolean remove = false;
			DisjunctiveTemporalConstraint dtc = additionalConstraints.get(i);
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains(tpName) && td.destination.getName().contains(tpName)){
				//	System.out.println("Removing constraint: " + dtc.toString() + " from addl constraints");
					remove = true;
				}
			}
			if(remove) toRemove.add(i);
		}
		for(int ind : toRemove) additionalConstraints.remove(ind);
	}
	
	
	public ArrayList<DisjunctiveTemporalConstraint> getAllDurationConstraints(String tpName){
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		ArrayList<DisjunctiveTemporalConstraint> durCons = new ArrayList<DisjunctiveTemporalConstraint>();
		for(int i = 0; i < tempConstraints.size(); i++){
			boolean remove = false;
			DisjunctiveTemporalConstraint dtc = tempConstraints.get(i);
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains(tpName) && td.destination.getName().contains(tpName)){
					//System.out.println("Removing constraint: " + dtc.toString() + " from temp constraints");
					remove = true;
				}
			}
			if(remove) toRemove.add(i);
		}
		
		//remove durational constraints
		for(int ind : toRemove) durCons.add(tempConstraints.get(ind));
		toRemove.clear();
		
		/**
		for(DisjunctiveTemporalConstraint dtc : tempConstraints){
			boolean remove = false;
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains(tpName) && td.destination.getName().contains(tpName)){
					System.out.println("Removing constraint: " + dtc.toString());
					remove = true;
				}
			}
			if(remove) tempConstraints.remove(dtc);
		} **/
		for(int i = 0; i < additionalConstraints.size(); i++){
			boolean remove = false;
			DisjunctiveTemporalConstraint dtc = additionalConstraints.get(i);
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains(tpName) && td.destination.getName().contains(tpName)){
					//System.out.println("Removing constraint: " + dtc.toString() + " from addl constraints");
					remove = true;
				}
			}
			if(remove) toRemove.add(i);
		}
		for(int ind : toRemove) durCons.add(additionalConstraints.get(ind));
		return durCons;
	}	
	
	public void updateDurationConstraints(String actName, Timepoint newEnd){
		//update the end timepoint of durational constraints for the given actName
		ArrayList<DisjunctiveTemporalConstraint> durCons = getAllDurationConstraints(actName);
		System.out.println("To start durCons is: " + durCons);
		for(DisjunctiveTemporalConstraint dtc : durCons){
			for(TemporalDifference td : dtc){
				if(td.source.getName().contains("_E")){
					//update the source timepoint
					td.source = newEnd;
				}else if(td.destination.getName().contains("_E")){
					td.destination = newEnd;
				}
			}
		}
	System.out.println("To end, durCons is: " + durCons);
	}
	
	//add toAdd amount of time onto the existing duration of actName
	public void updateDurationConstraintsValue(String actName, int toAdd){
		ArrayList<DisjunctiveTemporalConstraint> durCons = getAllDurationConstraints(actName);
		//System.out.println("To start durCons is: " + durCons);

		for(DisjunctiveTemporalConstraint dtc : durCons){
			for(TemporalDifference td : dtc){
				if(td.bound >= 0) td.bound = td.bound + toAdd;
				else td.bound = td.bound - toAdd;
			}
		}
		//System.out.println("To end, durCons is: " + durCons);

	}
	
	private void addNewTemporalConstraints(ArrayList<DisjunctiveTemporalConstraint> dtcs){
		this.tempConstraints.addAll(dtcs);
	}
	
	public SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> getComponentSTPs(){
		SimpleDTP base = (SimpleDTP) this.clone();
		base.tempConstraints.clear();
		
		ArrayList<DisjunctiveTemporalProblem> cSTPs = new ArrayList<DisjunctiveTemporalProblem>();
		//SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> output = new SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>>(0, cSTPs);
		ArrayList<DisjunctiveTemporalConstraint> nonDisjunctiveConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		ArrayList<DisjunctiveTemporalConstraint> disjunctiveConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		
		// iterate through the temporal constraints in the problem and pull out the disjunctive and non-disjunctive constraints
		for(DisjunctiveTemporalConstraint dtc : tempConstraints){
			if(dtc.isDisjunctive()){
				disjunctiveConstraints.add(dtc);
			}else{
				nonDisjunctiveConstraints.add(dtc);
			}
		}
		for(DisjunctiveTemporalConstraint dtc : additionalConstraints){
			if(dtc.isDisjunctive()){
				disjunctiveConstraints.add(dtc);
			}else{
				nonDisjunctiveConstraints.add(dtc);
			}
		}
		//return here if there are no disjunctive constraints?
		if(disjunctiveConstraints.size() == 0){
			//there are no disjunctive constraints, and we can return the problem as is. 
			System.out.println("No disjunctive constraints here, returning original problem");
			cSTPs.add(this.clone());
			return new SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>>(0, cSTPs);
		}
		//System.out.println(disjunctiveConstraints);
		base.addNewTemporalConstraints(nonDisjunctiveConstraints);
		//System.out.println("Base DTP:");
		//Generics.printDTP(base);
		//System.out.println("Num disjunctive constraints: " + disjunctiveConstraints.size());
		//Total number of constraints: 18
		//System.out.println(disjunctiveConstraints);
		//System.out.println("Num non-disjunctive constraints: " + nonDisjunctiveConstraints.size());
		//System.out.println("Total number of constraints: " + (tempConstraints.size() + additionalConstraints.size()));
		
		List<String> result = new ArrayList<String>();
		List<ArrayList<Character>> constraintLengths = new ArrayList<ArrayList<Character>>();
		for(int i = 0; i < disjunctiveConstraints.size(); i++){
			DisjunctiveTemporalConstraint dtc = disjunctiveConstraints.get(i);
			ArrayList<Character> inds = new ArrayList<Character>();
			int length = dtc.size();
			for(int j = 0; j < length; j++){
				inds.add((char)(j + '0'));
			}
			constraintLengths.add(inds);
		}
		//generate list of lengths
		generatePermutations(constraintLengths,result, 0, "");
		
		//go through and build actual lists of selections from the disjuncts
		//need to create the list of disjunctive temporal constraints here
		ArrayList<ArrayList<DisjunctiveTemporalConstraint>> constraintsToAdd = new ArrayList<ArrayList<DisjunctiveTemporalConstraint>>();
		
		for(int k = 0; k < result.size(); k++){
			ArrayList<DisjunctiveTemporalConstraint> combination = new ArrayList<DisjunctiveTemporalConstraint>();
			//each one of these represents a specific combination of disjuncts 
			String combinationStr = result.get(k);
			//System.out.println(combinationStr);
			for(int m = 0; m < combinationStr.length(); m++){
				int ind = (int) combinationStr.charAt(m) - 48; //subtract to get back to the proper integer value
				TemporalDifference comp = disjunctiveConstraints.get(m).get(ind);
				combination.add(new DisjunctiveTemporalConstraint(comp));
			}
			constraintsToAdd.add(combination);
			
		}
		//System.out.println("CONSTRAINTS TO ADD");
		//System.out.println(constraintsToAdd);
		//now we need to iterate through the constraint to Add and add each set to a version of the problem. 
		
		for(ArrayList<DisjunctiveTemporalConstraint> dtcs : constraintsToAdd){
			SimpleDTP stp = base.clone();
			stp.addNewTemporalConstraints(dtcs);
			stp.updateInternalData();
			stp.enumerateSolutions(0);
			stp.simplifyMinNetIntervals();
			//check if the STP is valid before we add it to the set. 
			if(stp.getNumSolutions() == 0) continue;
			if(stp.hasZeroDurations()) continue;
			if(stp.getMinTime() == Integer.MAX_VALUE) continue;
			
			cSTPs.add(stp);
			
		}
		//System.out.println("size of original cstp list is: " + constraintsToAdd.size());
		//System.out.println("Size of final componentSTP list is: "+cSTPs.size());
		SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> output = new SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>>(0, cSTPs);
		return output;
	}
	
	public void generatePermutations(List<ArrayList<Character>> Lists, List<String> result, int depth, String current){
		if(depth == Lists.size()){
			result.add(current);
			return;
		}
		for(int i = 0; i < Lists.get(depth).size(); ++i){
			generatePermutations(Lists, result, depth+1, current+Lists.get(depth).get(i));
		}
	}

	public ArrayList<String> getActivityOrdering() {
		this.updateInternalData();
		this.enumerateSolutions(0);
		this.simplifyMinNetIntervals();
		
		ArrayList<String> order = new ArrayList<String>();
		Map<Integer, String> toSort = new TreeMap<Integer, String>();
		//ArrayList<SimpleEntry<Integer, String>> sorted = new ArrayList<SimpleEntry<Integer, String>>();
		int idx = 1;
		while(idx < minimalNetwork.length){
			IntervalSet i = minimalNetwork[0][idx];
			int lb = (int) -i.getLowerBound();
			String name = localTimepoints.get(idx).getName();
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
	
	
	public SimpleDTP testDurationUpdate(String actName){
		String SPORADICNAME = "sporadic";
		int DUR = 10;
		SimpleDTP base = this;
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
		
		TemporalDifference td1 = new TemporalDifference(s_e, zero, 1000);
		TemporalDifference td2 = new TemporalDifference(zero, s_s, 0);
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
	
		dtcs.add(dtc1); dtcs.add(dtc2);
		
		base.addAdditionalConstraints(dtcs); 
		
		SimpleDTP newstp = base.clone();
		newstp.updateDurationConstraintsValue(actName, DUR);
		//synchronize sporadic event end time with end time of actName
		Timepoint curr_act_end = newstp.getTimepoint(actName + "_E");
		TemporalDifference tdSync1 = new TemporalDifference(s_e, curr_act_end, 0);
		TemporalDifference tdSync2 = new TemporalDifference(curr_act_end, s_e, 0);
		newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync1));
		newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdSync2));
		newstp.updateInternalData();
		newstp.enumerateSolutions(0);
		newstp.simplifyMinNetIntervals();
		Generics.printDTP(newstp);
		return newstp;
	}
	
	
	public void displayDTP(){
		//prints out the regular DTP encoding along with the interesting constraints
		// interesting constriants mean non-ordering, non-zero or infinity constraints.
		
		//will display if any disjunct of a dtc is non-zero or non-infinite
		ArrayList<DisjunctiveTemporalConstraint> toprint = new ArrayList<DisjunctiveTemporalConstraint>();
		boolean toAdd = true;
		Generics.printDTP(this);
		for(DisjunctiveTemporalConstraint dtc : tempConstraints){
			toAdd = true;
			for(TemporalDifference td : dtc){
				String name1 = td.source.getName();
				String name2 = td.destination.getName();
				if(td.bound == 0 || td.bound == Integer.MAX_VALUE || td.bound == Integer.MIN_VALUE || td.bound == 600){
					toAdd = false;
				}
				//System.out.println(name1.substring(0,name1.length()));
				if(name1.substring(0,name1.length()-1).equals(name2.substring(0,name2.length()-1))){
					toAdd = false;
				}
				
				
			}
			if(toAdd) toprint.add(dtc);
		}
		
		for(DisjunctiveTemporalConstraint dtc : toprint){
			System.out.println(dtc);
		}
	}
	
	
	public int getDuration(String tpName){
		// currently assumes that this duration is not variable. Returns worst case duration if it is variable
		Timepoint tp = timepoints.get(tpName + "_S");
		int idx = getIndex(tp);
		double duration = minimalNetwork[idx][idx+1].getUpperBound();
		int dur = (int) duration;
		if(dur < 0) return -1 * dur;
		else return dur;
	}
	
	public void setIdleNodeTrue(){
		idleNode = true;
	}
	

	public SimpleDTP mergeSTPs(ArrayList<SimpleDTP> inputSTPs) {
		// signal an error if initial list is empty?
		SimpleDTP mergedSTP = inputSTPs.get(0).clone();
//		System.out.println("Created baseline mergeSTP");
		if (inputSTPs.size() > 1) {
			for(Timepoint tp : mergedSTP.timepoints.values()) {
				if(tp.getName() == "zero") continue;
//				System.out.println("Merging timepoint "+tp.getName());
				IntervalSet tpIS = mergedSTP.getInterval("zero",tp.getName());
//				System.out.println("Initial intervalSet is "+tpIS);
			// for each timepoint in the merged STP
				for(int i=1; i< inputSTPs.size(); i++) {
				// find the corresponding timepoint in each of the other inputSTPs
					Timepoint othertp = inputSTPs.get(i).timepoints.get(tp.getName());
					// then merge together their time interval sets
					IntervalSet othertpIS = inputSTPs.get(i).getInterval("zero",tp.getName());
					for(Interval othertpI : othertpIS) {
						tpIS.add(othertpI);
						}
					}
			}
		}
		return mergedSTP;
	}
	
	
}
