package dtp;

import interactionFramework.Generics;
import interactionFramework.MappedPack;
import interval.Interval;
import interval.IntervalSet;
import stp.TemporalDifference;
import stp.Timepoint;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

import dtp.DisjunctiveTemporalProblem.DeltaFinder;


/**
 * Provides methods for specifying and solving a structured disjunctive temporal problem
 * Multiple subDTPs are connected via a connectingDTP
 * Utilizes Jim Boerkoel's MaDTP solution approach (i.e., subDTPs are analogous to an agent's local DTP, and a connectingDTP is analogous to a shared DTP)
 * However, this code is intended to reside within a single agent, and thus assumes that the subDTPs do not need to be fully decoupled
 * Rather, maximal flexibility, disjunctive constraints are maintained across the subDTPs
 */
public class MultiDTP implements DisjunctiveTemporalProblem, java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -874367134213693865L;
	private static final int DEBUG = 1;
	public int VALID = 1;
	protected String subDTPLabel = "subDTPs";
	protected String connectingLabel = "connectingDTP";
	protected DisjunctiveTemporalProblem connectingDTP;
	protected DisjunctiveTemporalProblem[] subDTPs;
	protected Timepoint zero;
	protected ArrayList<Timepoint> interfaceTimepoints;
	protected ArrayList<Timepoint> allTimepoints;
	protected HashMap<String, Integer> timepoints;  //tells you which subDTP a timepoint belongs to, -1 means its the zero timepoint
	protected ArrayList<DisjunctiveTemporalConstraint> incomingConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
	
	//currentSubDTPSumCons, currentConnectingSumCons, and mustResolve are used to optimize which DTPs need to be solved
	//optimizations are:
	//-only recompute the connectingDTP if one of the influences from a subDTP has changed
	//-only recompute the influences from subDTP[i] if a new constraint has been added to subDTP[i] since the last time its influences were computed
	//-only establish minimality on subDTP[i] if either: a) we just recomputed the influences for subDTP[i] or b) an influence from the connectingDTP to subDTP[i] has changed 
	protected ArrayList<DisjunctiveTemporalConstraint>[] currentSubDTPSumCons;  //the subDTPs' summarizing constraints 
	protected ArrayList<DisjunctiveTemporalConstraint>[] currentConnectingSumCons; //the connectingDTP's summarizing constraints 
	protected boolean[] mustResolve; //mustResolve[i] means that subDTPs[i] should be resolved
	protected boolean reEmbed = false;
	
	//selectionOrder, selectionCount, and count are used to undo activity selections
	//selectionOrder provides which DTP to pop from and a time increment associated with the selection
	//selectionCount provides the number of selectionOrder entries to pop (since an activity could correspond to multiple pops, e.g., due to having to insert an idle)
	//count keeps track of the number of selectionOrder.push() that have been done since the last selectionCount.push()
	protected Stack<SimpleEntry<Integer,Integer>> selectionOrder = new Stack<SimpleEntry<Integer,Integer>>();  //-1 corresponds to a advanceToTime, -2 corresponds to connectingDTP, i corresponds to localDTP[i]
	protected Stack<Integer> selectionCount = new Stack<Integer>();
	protected int count = 0;
	
	
	public MultiDTP(Timepoint zero, DisjunctiveTemporalProblem decouplingDTP, DisjunctiveTemporalProblem[] subDTPs, HashMap<String, Integer> timepoints, ArrayList<Timepoint> interfaceTimepoints){
		this.zero = zero;
		this.connectingDTP = decouplingDTP;
		this.subDTPs = subDTPs;
		this.mustResolve = new boolean[subDTPs.length];
		this.currentSubDTPSumCons = new ArrayList[subDTPs.length];
		this.currentConnectingSumCons = new ArrayList[subDTPs.length];
		for(int i = 0; i < this.currentSubDTPSumCons.length; i++){
			this.mustResolve[i] = true;
			this.currentSubDTPSumCons[i] = new ArrayList<>();
			this.currentConnectingSumCons[i] = new ArrayList<>();
		}
		this.timepoints = timepoints;
		this.interfaceTimepoints = interfaceTimepoints;
		this.allTimepoints = new ArrayList<Timepoint>();
		Set<Timepoint> tpSet = new TreeSet<Timepoint>();  //do things through this set so there won't be duplicates
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			tpSet.addAll(dtp.getTimepoints());
		}
		this.allTimepoints.addAll(tpSet);
	}
	
	@Override
	public void enumerateSolutions(int time) {
		establishMinimality(allTimepoints, time);
	}
	
	@Override
	public boolean nextSolution(int time){
		return solveNext(allTimepoints,time);
	}
	
	@Override
	public void enumerateInfluences(int scheduleTime) {
		establishMinimality(interfaceTimepoints, scheduleTime);
	}

	@Override
	public boolean nextInfluence(int scheduleTime) {
		return solveNext(interfaceTimepoints,scheduleTime);
	}
	
	@Override
	public void establishMinimality(List<Timepoint> timepointsToConsider, int scheduleTime) {
		if(connectingDTP.getTimepoints().size() > 1){ //always has the zero timepoint, if there are others, need to propagate constraints
			//first enumerate the outgoing influences for each subDTP, and add summarizing constraints to the connectingDTP
			for(int i = 0; i < subDTPs.length; i++){
				if(!mustResolve[i]) continue;
				subDTPs[i].softReset();
//				subDTPs[i].clearIncomingConstraints();
			}
			ArrayList<DisjunctiveTemporalConstraint> connectingConstraints;
			if(reEmbed){
				connectingConstraints = embedConstraints(incomingConstraints);  //have to remember to add the incoming influences in before solving the subDTPs
				reEmbed = false;
			}
			else connectingConstraints = new ArrayList<>();
			
			boolean resolveConnectingFlag = false;
			for(int i = 0; i < subDTPs.length; i++){	
				if(!mustResolve[i]) continue;
//				System.out.println("resolving "+subDTPLabel+"["+i+"] for influences");
				subDTPs[i].enumerateInfluences(scheduleTime);
				ArrayList<DisjunctiveTemporalConstraint> newSummarizingConstraints = subDTPs[i].computeSummarizingConstraints(subDTPs[i].getInterfaceTimepoints());
				//must use a temp variable below or the compiler will optimize away calling testAndSetSummarizingConstraints if resolveConnectingFlag is already true
				boolean temp = testAndSetSummarizingConstraints(newSummarizingConstraints,i, currentSubDTPSumCons);  
				resolveConnectingFlag = resolveConnectingFlag || temp;
			}
			

			//then solve the connectingDTP
			if(resolveConnectingFlag){
//				System.out.println("resolving "+subDTPLabel+" connecting");
				connectingDTP.softReset();
//				connectingDTP.clearIncomingConstraints();
				embedConstraints(connectingConstraints);
				for(int i = 0; i < subDTPs.length; i++){
					connectingDTP.addIncomingConstraints(currentSubDTPSumCons[i]);
				}
				
				connectingDTP.enumerateInfluences(scheduleTime);
				
				//then add the summarizing constraints to the subDTPs
				for(int i = 0; i < subDTPs.length; i++){	
					ArrayList<DisjunctiveTemporalConstraint> newSummarizingConstraints = connectingDTP.computeSummarizingConstraints(subDTPs[i].getInterfaceTimepoints());
					boolean temp = testAndSetSummarizingConstraints(newSummarizingConstraints,i, currentConnectingSumCons);
	//				Generics.print2log("Summarizing: "+dtp.getInterfaceTimepoints().toString()+"\nConstraints: "+temp.toString());
					if(temp){
						subDTPs[i].addIncomingConstraints(newSummarizingConstraints);
						mustResolve[i] = true;
					}
				}
			}
			else{
//				System.out.println("not resolving "+subDTPLabel+" connecting");
				for(int i = 0; i < subDTPs.length; i++){
					if(mustResolve[i]) subDTPs[i].addIncomingConstraints(currentConnectingSumCons[i]);
				}
			}
		}	
		
		//finally, establish minimality on the timepointsToConsider
		ArrayList<ArrayList<Timepoint>> tpPartition = partitionTimepoints(timepointsToConsider);
		for(int i = 0; i < subDTPs.length; i++){
			if(tpPartition.get(i).isEmpty() || !mustResolve[i]) continue;
//			System.out.println("resolving "+subDTPLabel+"["+i+"] for minimality");
//			Generics.print2log("\nestablishMinimality in subDTP for:"+ tpPartition.get(i));
			subDTPs[i].softReset();
//			System.out.println("calling establish minimality on SubDTP " + i + "with tps: "+tpPartition.get(i));
			subDTPs[i].establishMinimality(tpPartition.get(i), -scheduleTime);
			mustResolve[i] = false;
//			subDTPs[i].printTimepointIntervals(Generics.getLogStream());
		}
	}

	@Override
	public boolean solveNext(List<Timepoint> timepointsToConsider,	int scheduleTime) {
		// TODO Implement
		throw new java.lang.UnsupportedOperationException();
	}

	/**
	 * Compares newSummarizingConstraints[i] to currentSummarizingConstraint[i] and returns true if they are not the same
	 * Also sets currentSummarizingConstraints[i] to equal newSummarizingConstraints[i]
	 * @param newSummarizingConstraints
	 * @param i indices which constraints to test against
	 * @return newSummarizingConstraint == currentSummarizingConstraint[i]
	 */
	private boolean testAndSetSummarizingConstraints(ArrayList<DisjunctiveTemporalConstraint> newSummarizingConstraint, int i, ArrayList<DisjunctiveTemporalConstraint>[] currentSummarizingConstraint){
		if(currentSummarizingConstraint[i].size() != newSummarizingConstraint.size()){
			currentSummarizingConstraint[i] = newSummarizingConstraint;
			return true;
		}
		if(!newSummarizingConstraint.containsAll(currentSummarizingConstraint[i])){
			currentSummarizingConstraint[i] = newSummarizingConstraint;
			return true;				
		}
		return false;
	}
	
	/**
	 * Goes through the DTP's incoming decoupling constraints and embeds them in the appropriate subDTPs and/or connectingDTP
	 */
	private ArrayList<DisjunctiveTemporalConstraint> embedConstraints(Collection<DisjunctiveTemporalConstraint> col){
//		Generics.print2log("embedding: "+incomingConstraints.toString());
		ArrayList<DisjunctiveTemporalConstraint> result = new ArrayList<>();
		for(DisjunctiveTemporalConstraint cons : col){
			if(embedIncomingConstraint(cons)) result.add(cons);
		}
		return result;
	}
	
	/**
	 * @param cons
	 * @return true iff cons crosses subDTPs
	 */
	private boolean embedIncomingConstraint(DisjunctiveTemporalConstraint cons){
		Set<Integer> indices = getConstraintIndices(cons);
		if(indices.size() == 1){ //adding a constraint to a single subDTP
			int idx = indices.iterator().next();
//			Generics.print2log("Adding to "+idx+" "+cons.toString());
			subDTPs[idx].addIncomingConstraint(cons);
			return false;
		}
		else{  //adding a constraint that crosses subDTPs, so it has to go in the connectingDTP
//			Generics.print2log("Adding to connecting "+cons.toString());
			for(TemporalDifference td : cons){
				Integer idxS = timepoints.get(td.source.name);
				Integer idxD = timepoints.get(td.destination.name);
				subDTPs[idxS].addInterfaceTimepoint(td.source);
				subDTPs[idxD].addInterfaceTimepoint(td.destination);
				connectingDTP.addInterfaceTimepoint(td.source);
				connectingDTP.addInterfaceTimepoint(td.destination);
			}
			connectingDTP.addIncomingConstraint(cons);
			return true;
		}
	}
	
	@Override
	public ArrayList<DisjunctiveTemporalConstraint> computeSummarizingConstraints(ArrayList<Timepoint> timepointsToDecouple) {
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

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> computeDecouplingConstraints(ArrayList<Timepoint> timepointsToDecouple) {
		// TODO Implement
		throw new java.lang.UnsupportedOperationException("");
	}

	public void simplifyMinNetIntervals() {
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			dtp.simplifyMinNetIntervals();
		}		
	}
	
	/**
	 * Partitions the timepoints into their respective subDTPs, and adds the zero timepoint to any non-empty partitions
	 * @param col
	 * @return
	 */
	private ArrayList<ArrayList<Timepoint>> partitionTimepoints(List<Timepoint> col){
		ArrayList<ArrayList<Timepoint>> result = new ArrayList<ArrayList<Timepoint>>(subDTPs.length);
		for(int i = 0; i < subDTPs.length; i++) result.add(new ArrayList<Timepoint>());
		for(Timepoint tp : col){
			Integer idx = timepoints.get(tp.name);
			if(idx == null) throw new java.lang.IllegalArgumentException("Timepoint: "+tp.toString()+" does not exist.");
			if(idx == -1) continue;
			result.get(idx).add(tp);
		}
		//need to add the zero timepoint to any non empty partitions so that establishMinimality calls will give correct results.
		for(ArrayList<Timepoint> ar: result){
			if(!ar.isEmpty()) ar.add(zero);
		}
		return result;
	}

	public int getMinTime() {
		int min = Integer.MAX_VALUE;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			int temp = dtp.getMinTime();
//			System.out.println("minTime could be: "+Generics.toTimeFormat(temp));
			if(temp < min) min = temp;
		}
		return min;
	}

	@Override
	public List<List<String>> getActivities(ActivityFinder af, int time){
		ArrayList<String> activities = new ArrayList<String>();
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			activities.addAll(dtp.getActivities(af, time).get(0));
		}
		List<List<String>> result = new LinkedList<>();
		result.add(activities);
		return result;
	}

	@Override
	public List<Integer> getMaxSlack() {
//		System.out.println("In MultiDTP getmaxSlack");
		int i = 0;
		int min = Integer.MAX_VALUE;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			int temp = dtp.getMaxSlack().get(0);
//			System.out.println("min is "+ min + " subDTP "+i+" has slack " + temp);
//			System.out.println("slack: "+temp+" oldMin: "+min);
			if(temp < min) min = temp;
			i++;
		}
		List<Integer> result = new ArrayList<Integer>(1);
		result.add(min);
		return result;
	}

	@Override
	public IntervalSet getInterval(String tp1, String tp2) {
		Integer idx1 = timepoints.get(tp1);
		Integer idx2 = timepoints.get(tp2);
		if(idx1 == null) throw new java.lang.IllegalArgumentException("Timepoint: "+tp1+" does not exist.");
		if(idx2 == null) throw new java.lang.IllegalArgumentException("Timepoint: "+tp2+" does not exist.");
		if(idx1 == -1 && idx2 == -1) return IntervalSet.getEmptyInterval();  //interval from zero to zero
		if(idx1 == -1) return subDTPs[idx2].getInterval("zero", tp2);           //interval from zero to tp2
		if(idx2 == -1) return subDTPs[idx1].getInterval(tp1, "zero");           //interval from tp1 to zero
		if(connectingDTP.contains(tp1) && connectingDTP.contains(tp2)) return connectingDTP.getInterval(tp1, tp2);  //both timepoints are in the connectingDTP
		if(idx1 == idx2) return subDTPs[idx1].getInterval(tp1, tp2);         //interval between timepoints in the same subDTP
		
		//else timepoints don't have constraints between them, so answer is just the difference of their individual intervalSets
		IntervalSet is1 = subDTPs[idx1].getInterval("zero", tp1);
		IntervalSet is2 = subDTPs[idx2].getInterval("zero", tp2);
		return IntervalSet.difference(is2, is1);
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
		Integer idx1 = timepoints.get(tp1);
		Integer idx2 = timepoints.get(tp2);
		if(idx1 == null || idx2 == null){
			return connectingDTP.getInterval(tp1, tp2);
		}
		if(idx1 == -1 && idx2 == -1) return IntervalSet.getEmptyInterval();  //interval from zero to zero
		if(idx1 == -1) return subDTPs[idx2].getInterval("zero", tp2);           //interval from zero to tp2
		if(idx2 == -1) return subDTPs[idx1].getInterval(tp1, "zero");           //interval from tp1 to zero
		if(connectingDTP.contains(tp1) && connectingDTP.contains(tp2)) return connectingDTP.getInterval(tp1, tp2);  //both timepoints are in the connectingDTP
		if(idx1 == idx2) return subDTPs[idx1].getInterval(tp1, tp2);         //interval between timepoints in the same subDTP
		
		//else timepoints don't have constraints between them, so answer is just the difference of their individual intervalSets
		IntervalSet is1 = subDTPs[idx1].getInterval("zero", tp1);
		IntervalSet is2 = subDTPs[idx2].getInterval("zero", tp2);
		return IntervalSet.difference(is2, is1);
	}
	
	public int[] getMinTimeArray(){
		int[] minTime = new int[subDTPs.length];
		for(int i = 0; i < minTime.length; i++){
			minTime[i] = subDTPs[i].getMinTime();
		}
		return minTime;
	}
	
	@Override
	public void advanceToTime(int time, int deltaT, boolean pushSelection) {
		advanceToTime(time, true, deltaT, pushSelection);
	}	
	@Override
	public void advanceToTime(int time, boolean resolve, int deltaT, boolean pushSelection) {
		selectionOrder.push(new SimpleEntry<Integer,Integer>(-1, deltaT));
		count++;
		for(int i = 0; i < subDTPs.length; i++){
			advanceToTimeHelper(i, time, false, deltaT, pushSelection);
		}
		cleanup(time, resolve, pushSelection);
		return;
	}
	
	protected boolean advanceToTimeHelper(int idx, int time, boolean resolve, int deltaT, boolean pushSelection){
		int minTime = subDTPs[idx].getMinTime();
//		System.out.println("In advancetoTimeHelper, updating subDTP " + idx +" pushSelection is " + pushSelection);
		if(-time >= minTime || minTime == Integer.MAX_VALUE){
			subDTPs[idx].advanceToTime(time, resolve, deltaT, pushSelection);
			mustResolve[idx] = true;
			return true;
		}
		return false;
	}
	
	@Override
	//TODO: fix code to add time-incrementing constraints for all subDTPs, not just the one being tightened
	//TODO: fix code so that you can tighten timepoints across subDTPs.  Does this mean you have to add new interface timepoints? think so.
	public void tightenTimepoint(int timeS, String tpS, int timeE, String tpE, int deltaT, boolean pushSelection){
		tightenTimepoint(timeS,tpS,timeE,tpE,true,deltaT,pushSelection);
	}	
	@Override
	public void tightenTimepoint(int timeS, String tpS, int timeE, String tpE, boolean resolve, int deltaT, boolean pushSelection) {
		SimpleEntry<Integer,Integer> se = getIdxAndCheck(tpS, tpE);
		Integer idxS = se.getKey();
		Integer idxE = se.getValue();
		selectionOrder.push(new SimpleEntry<Integer,Integer>(idxS,deltaT));
		count++;
		subDTPs[idxS].tightenTimepoint(timeS, tpS, timeE, tpE, false, deltaT, false);
		mustResolve[idxS] = true;
		cleanup(timeE,resolve,pushSelection);
	}
	
	@Override
	public void executeAndAdvance(int timeS, String tpS, int timeE, String tpE, boolean resolve, int deltaT, boolean pushSelection){
		SimpleEntry<Integer,Integer> se = getIdxAndCheck(tpS, tpE); //returns indices of the subdtps that these timepoints belong to
		Integer idxS = se.getKey();
		Integer idxE = se.getValue();
		selectionOrder.push(new SimpleEntry<Integer,Integer>(-1, deltaT));
		count++;
		subDTPs[idxS].executeAndAdvance(timeS, tpS, timeE, tpE, false, deltaT, false); // only do this for the subDTP that the timepoint belongs to? changed to all subdtps 
		
		mustResolve[idxS] = true;
		for(int i = 0; i < subDTPs.length; i++){
			if(i != idxS){
				advanceToTimeHelper(i, timeE, false, deltaT, pushSelection);
			}
		}
		cleanup(timeE, resolve, pushSelection);
	}
	
	public void addNewConstraint(String source, String dest, ArrayList<IntervalSet> v, int time, boolean resolve, boolean pushSelection){
		for(IntervalSet dtc : v){
			addAdditionalConstraint(source, dest, dtc, time, false, false);
		}
		cleanup(time,resolve,pushSelection);
	}
	
	@Override
	public void addAdditionalConstraint(String tpS, String tpE, IntervalSet dtc, int time, boolean resolve, boolean pushSelection){
		SimpleEntry<Integer,Integer> se = getIdxAndCheck(tpS,tpE);
//		System.out.println("In addAdditionalConstraint subDTPs the tp belongs to is "+ se);
		Timepoint source, dest;
		if(se.getKey() != -1) source = subDTPs[se.getKey()].getTimepoint(tpS);
		else source = zero;
		if(se.getValue() != -1) dest = subDTPs[se.getValue()].getTimepoint(tpE);
		else dest = zero;
		Collection<DisjunctiveTemporalConstraint> col = DisjunctiveTemporalConstraint.generateConstraint(source,dest,dtc);
		addConstraints(col, time, resolve, pushSelection);
	}

	@Override
	public void addAdditionalConstraints(Collection<DisjunctiveTemporalConstraint> col) {
		addConstraints(col,-1,false,true);
	}
	protected void addConstraints(Collection<DisjunctiveTemporalConstraint> col, int time, boolean resolve, boolean pushSelection){
		for(DisjunctiveTemporalConstraint cons: col){
			addConstraint(cons,time,false,false);
		}
		cleanup(time,resolve,pushSelection);
	}

	@Override
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons) {
		addConstraint(cons,-1,false,true);
	}
	
	
	protected void addConstraint(DisjunctiveTemporalConstraint cons, int time, boolean resolve, boolean pushSelection){
		Set<Integer> indices = getConstraintIndices(cons);
		if(DEBUG >= 1) System.out.println("Adding new constraints: "+cons.toString()+" to subDTP "+ indices);
		if(indices.size() == 1){ //adding a constraint to a single subDTP
			int idx = indices.iterator().next();
			//DisjunctiveTemporalProblem copy = subDTPs[idx].clone();
			// we need to check to see if the constraint we're adding is actually valid. 
			
			
			
			subDTPs[idx].addAdditionalConstraint(cons);
			mustResolve[idx] = true;
			selectionOrder.push(new SimpleEntry<Integer,Integer>(idx,0));
			count++;
		}
		else{  //adding a constraint that crosses subDTPs, so it has to go in the connectingDTP
			for(TemporalDifference td : cons){
				//TODO: make it so that popSelection removes the new interface timepoints if appropriate
				Integer idxS = timepoints.get(td.source.name);
				Integer idxD = timepoints.get(td.destination.name);
				//TODO: mustResolve for connectingDTP constraints
				mustResolve[idxS] = true;
				mustResolve[idxD] = true;
				subDTPs[idxS].addInterfaceTimepoint(td.source);
				subDTPs[idxD].addInterfaceTimepoint(td.destination);
				connectingDTP.addInterfaceTimepoint(td.source);
				connectingDTP.addInterfaceTimepoint(td.destination);
			}
			
			connectingDTP.addAdditionalConstraint(cons);
			selectionOrder.push(new SimpleEntry<Integer,Integer>(-2,0));
			count++;
		}
		
		cleanup(time,resolve,pushSelection);
	}
	
	/**
	 * Looks at a constraint and determines which subDTPs it interacts with.
	 * Ignores the zero timepoint.
	 * @param cons is the constraint to check
	 * @return the set of subDTP indices that cons interacts with
	 */
	protected Set<Integer> getConstraintIndices(DisjunctiveTemporalConstraint cons){
		Set<Integer> result = new TreeSet<Integer>();
		for(TemporalDifference td : cons){
			Integer idxS = timepoints.get(td.source.name);
			Integer idxD = timepoints.get(td.destination.name);
			if(idxS == null || idxD == null) throw new java.lang.IllegalArgumentException("Constraint "+cons.toString()+" has bad timepoint indices");
			//-1 idx corresponds to the zero timepoint, which is unimportant since that timepoint is in every subDTP
			if(idxS != -1) result.add(idxS);
			if(idxD != -1) result.add(idxD);
		}
		if(result.size() == 0) throw new java.lang.IllegalArgumentException("Constraint "+cons.toString()+" has bad timepoint indices");
		return result;
	}
	
	/**
	 * Does some cleanup functionality after adding new constraints to the DTP.
	 * Options include: re-solving the DTP (and fixingZeroValuIntervals) and/or pushing the selectionStack
	 * @param time
	 * @param resolve
	 * @param pushSelection
	 */
	protected void cleanup(int time, boolean resolve, boolean pushSelection){
		while(resolve){
			enumerateSolutions(time);
			resolve = fixZeroValIntervals();
		}
		if(pushSelection){
			//System.out.println("Pushed count! It was : " + count);
			selectionCount.push(count);
			
			count = 0;
		}
	}
	
	public boolean fixZeroValIntervals(){
		boolean result = false;
		for(int i = 0; i < subDTPs.length; i++){
			DisjunctiveTemporalProblem dtp = subDTPs[i];
			boolean flag = dtp.fixZeroValIntervals();
			if(flag){
				//if(DEBUG >= 1) System.out.println("\nfixed zeroValInterval for dtp["+i+"]\n");
				selectionOrder.push(new SimpleEntry<Integer,Integer>(i,0));
				count++;
			}
			result = result || flag;
		}
		return result;
	}
	
	/**
	 * Looks up the indices for two timepoints and does some error checking to make sure things are well formed
	 * @param tp1
	 * @param tp2
	 * @return indices for tp1 and tp2
	 */
	protected SimpleEntry<Integer,Integer> getIdxAndCheck(String tp1, String tp2){
		Integer idx1 = timepoints.get(tp1);
		if(idx1 == null && tp1 != null){
			throw new java.lang.IllegalArgumentException ("Timepoint "+tp1+" is not part of a DTP.");
		}
		Integer idx2 = timepoints.get(tp2);
		if(idx2 == null && tp2 != null){
			throw new java.lang.IllegalArgumentException ("Timepoint "+tp2+" is not part of a DTP.");
		}
		if(tp2 == null && tp1 == null) return new SimpleEntry<Integer,Integer>(idx1,idx2);
		else if(tp2 == null && idx1 == -1) throw new java.lang.UnsupportedOperationException("Timepoint "+tp1+" belongs to multiple DTPs.");
		else if(tp1 == null && idx2 == -1) throw new java.lang.UnsupportedOperationException("Timepoint "+tp2+" belongs to multiple DTPs.");
		
		//additional error checks if there are two timepoints
		if(idx2 == -1 && idx1 == -1){
			throw new java.lang.UnsupportedOperationException("Timepoints "+tp1+" and "+tp2+" each belong to multiple DTPs.");				
		}
		return new SimpleEntry<Integer,Integer>(idx1,idx2);
	}
	
	@Override
	public int popSelection(int time) {
		return popSelection(time,true);
	}
	
	@Override
	public int popSelection(int time, boolean resolve) {
//		System.out.println("running pop selection");
		resetMustResolve();
//		System.out.println("In MultiDTP pop selection, selectionCount is " + selectionCount.toString());
		int sC = selectionCount.pop();
		int result = 0;
		for(int i =0; i < sC+count; i++){
			SimpleEntry<Integer,Integer> selection = selectionOrder.pop();
			if(selection.getKey() == -1){
				for(DisjunctiveTemporalProblem dtp : subDTPs){
					dtp.popSelection(time + selection.getValue(), false);
				}
			}
			else if(selection.getKey() == -2){
				connectingDTP.popSelection(time+selection.getValue(), false);
			}
			else{
				subDTPs[selection.getKey()].popSelection(time + selection.getValue(), false);
			}		
			result += selection.getValue();
		}
		cleanup(time+result,resolve,false);
		count = 0;
//		printTimepointIntervals(System.out);
		return result;
	}
	
	@Override
	public void printSelectionStack(){
		System.out.println("Selection stack information\ncount: "+count+"\tstack: "+selectionCount.toString());
		System.out.println("selections: "+selectionOrder.toString());
		int i = 0;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			System.out.println("SubDTP " + i);
			dtp.printSelectionStack();
			i++;
		}
	}

	@Override
	public void printTimepointIntervals(PrintStream out) {
		out.println(connectingLabel+": ");
		this.connectingDTP.printTimepointIntervals(out);
		for(int i = 0; i < subDTPs.length; i++){
			out.println(subDTPLabel+"["+i+"]: ");
			subDTPs[i].printTimepointIntervals(out);
//			dtp.printConstraints(out);
		}
	}

	@Override
	public void printTimepointIntervals(PrintStream out, int time) {
		out.println(connectingLabel+": ");
		this.connectingDTP.printTimepointIntervals(out,time);
		for(int i = 0; i < subDTPs.length; i++){
			out.println(subDTPLabel+"["+i+"]: ");
			subDTPs[i].printTimepointIntervals(out, time);
//			dtp.printConstraints(out);
		}
	}
	
	@Override
	public void printConstraints(PrintStream out){
		connectingDTP.printConstraints(out);
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			dtp.printConstraints(out);
		}		
	}

	private void resetMustResolve(){
		for(int i = 0; i < mustResolve.length; i++){
			mustResolve[i] = true;
		}
	}
	
	@Override
	public int getNumSolutions() {
		int r = 0;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			r += dtp.getNumSolutions();
		}
		return r;
	}

	@Override
	public long getTotalFlexibility() {
		long r = 0;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			r += dtp.getTotalFlexibility();
		}
		return r;
	}

	@Override
	public double getRigidity() {
		double total = 0;
		double count = 0;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			for(SimpleEntry<Double,Double> temp : dtp.getRigidityVals()){
				total += temp.getKey();
				count += temp.getValue();
			}
		}
		return Math.sqrt(total/count);
	}

	@Override
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals() {
		ArrayList<SimpleEntry<Double, Double>> result = new ArrayList<SimpleEntry<Double, Double>>();
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			result.addAll(dtp.getRigidityVals());
		}
		return result;
	}

	@Override
	public void softReset() {
//		connectingDTP.softReset();
//		for(DisjunctiveTemporalProblem dtp : subDTPs){
//			dtp.softReset();
//		}
	}

	@Override
	public void clearIncomingConstraints() {
		resetMustResolve();
		incomingConstraints.clear();
	}

	@Override
	public void addIncomingConstraint(DisjunctiveTemporalConstraint constraint) {
		if(!incomingConstraints.contains(constraint)){
			incomingConstraints.add(constraint);
			reEmbed = true;
			resetMustResolve();
		}
	}

	@Override
	public void addIncomingConstraints(Collection<DisjunctiveTemporalConstraint> constraints) {
		for(DisjunctiveTemporalConstraint cons : constraints){
			if(!incomingConstraints.contains(cons)){
				incomingConstraints.add(cons);
				reEmbed = true;
				resetMustResolve();
			}			
		}
	}

	@Override
	public void setIncomingConstraints(ArrayList<DisjunctiveTemporalConstraint> constraints) {
		if(!incomingConstraints.containsAll(constraints)){
			incomingConstraints = constraints;
			reEmbed = true;
			resetMustResolve();
		}
	}

	@Override
	public void addInterfaceTimepoint(Timepoint tp) {
		if(interfaceTimepoints.contains(tp)) return;
		Integer idx = timepoints.get(tp.name);
		//TODO: fix so that it will add a new timepoint to the MultiDTP (which subDTP to add into though?)
		if(idx == null) throw new java.lang.UnsupportedOperationException("Cannot add non-existent timepoint "+tp.toString()+" for MultiDTPs.");
		else if(idx == -1) interfaceTimepoints.add(zero);
		else interfaceTimepoints.add(tp);
	}

	@Override
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
		for(Timepoint tp: timepoints){
			addInterfaceTimepoint(tp);
		}
	}

	@Override
	public ArrayList<Timepoint> getInterfaceTimepoints(){
		return interfaceTimepoints;
	}
	
	@Override
	public Timepoint getTimepoint(String tpS) {
		Integer idx = timepoints.get(tpS);
		if(idx == null) return null;
		else if(idx == -1) return zero;
		else return subDTPs[idx].getTimepoint(tpS);
	}

	@Override
	public Set<Timepoint> getTimepoints() {
		Set<Timepoint> result = new TreeSet<Timepoint>();
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			result.addAll(dtp.getTimepoints());
		}
		return result;
	}

	@Override
	public boolean contains(String tp1) {
		return timepoints.get(tp1) != null;
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
	

	@Override
	public void printFixedIntervals(PrintStream out, int time){
		int i = 0;
		out.println(subDTPs.length);
		while(i < subDTPs.length) {
			out.println("subDTP "+i+":");
			subDTPs[i].printFixedIntervals(out, time);
			i = i + 1;
		}
		out.println("Connecting DTP:");
		connectingDTP.printFixedIntervals(out, time);
	}
	
	
	public void addNewTimepoint(Timepoint tp){
		// add timepoint to relevant subDTP. 
		// assuming now that the relevant subDTP is the first one.... 
		subDTPs[0].addNewTimepoint(tp);
	}
	
	@Override
	public boolean existsFixedTimepoint(){
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			if(dtp.existsFixedTimepoint()) return true;
		}
		return false;
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, int threshold) {
		int currAgent = this.getCurrentAgent();
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		int i;
		for(i = 0; i< this.subDTPs.length; i++){
			DisjunctiveTemporalProblem currSubDTP = this.subDTPs[i];
			deltas.addAll(currSubDTP.getDeltas(((MultiDTP)prevDTP).subDTPs[i], threshold));

		}
		
		return deltas;
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, double relThreshold){
		int currAgent = this.getCurrentAgent();
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		int i;
		for(i = 0; i< this.subDTPs.length; i++){
			DisjunctiveTemporalProblem currSubDTP = this.subDTPs[i];
			deltas.addAll(currSubDTP.getDeltas(((MultiDTP)prevDTP).subDTPs[i], relThreshold));

		}
		
		return deltas;
		
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP){
		int currAgent = this.getCurrentAgent();
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		int i;
		for(i = 0; i< this.subDTPs.length; i++){
			DisjunctiveTemporalProblem currSubDTP = this.subDTPs[i];
			deltas.addAll(currSubDTP.getDeltas(((MultiDTP)prevDTP).subDTPs[i]));
		}
		
		return deltas; 
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, DeltaFinder df) {
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		int i;
		for(i = 0; i< this.subDTPs.length; i++){
			DisjunctiveTemporalProblem currSubDTP = this.subDTPs[i];
			deltas.addAll(currSubDTP.getDeltas(((MultiDTP)prevDTP).subDTPs[i], df));
		}
		
		return deltas; 
	}
	
	public ArrayList<Delta> rankDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim){
		ArrayList<Delta> allDeltas = getDeltas(prevDTP);
		Collections.sort(allDeltas, Delta.AbsoluteDifferenceComparator);
		ArrayList<Delta> returnDeltas = new ArrayList<Delta>();
		System.out.println("In rank deltas");
		for(Delta elm: allDeltas) System.out.println(elm.getAbsoluteDifference());
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
	
	@Override
	public void printDeltas(ArrayList<Delta> deltas) {
		int len = deltas.size();
		int i;
		if(len == 0) System.out.println("No deltas found.");
		for(i = 0; i < len; i++){
			String out_str = deltas.get(i).toString();
			System.out.println(out_str);
		
		}
	}
	//	public MultiDTP(Timepoint zero, DisjunctiveTemporalProblem decouplingDTP, DisjunctiveTemporalProblem[] subDTPs, HashMap<String, Integer> timepoints, ArrayList<Timepoint> interfaceTimepoints){

	public MultiDTP clone(){
		if(this == null) return null;
		//System.out.println("Cloning a MultiDTP");
		
		//first we clone the subDTPS that make up a MultiDTP
		MultiDTP newDTP;
		DisjunctiveTemporalProblem newcDTP = connectingDTP.clone();
		DisjunctiveTemporalProblem[] newSubDTPs = new DisjunctiveTemporalProblem[subDTPs.length];
		for(int i=0; i < subDTPs.length; i++){
			newSubDTPs[i] = subDTPs[i].clone();
		}
		
		// Instead of doing the below, we need to get the list of all of the timepoints from the
		// subDTPs and then union that with the cloned interface timepoints below. 
		//ArrayList<Timepoint> newAllTimepoints = new ArrayList<Timepoint>();
		//for(Timepoint tp : allTimepoints) newAllTimepoints.add(tp.clone());
		ArrayList<Timepoint> newInterfaceTimepoints = new ArrayList<Timepoint>();
		for(Timepoint tp : interfaceTimepoints) newInterfaceTimepoints.add(tp.clone());
		newDTP = new MultiDTP(zero, newcDTP, newSubDTPs, timepoints, newInterfaceTimepoints);
		//newDTP.allTimepoints = newAllTimepoints; <-- this should be set appropriately by cloning 
			// the subDTPS first and then passing in cloned interfaceTimepoints. 
		newDTP.count = count;
		
		//make a hashMap<String, Timepoint> like in Simple DTP to pass to the DTC clone function
		HashMap<String, Timepoint> hashedTimepoints = new HashMap<String, Timepoint>();
		for(Timepoint tp : newDTP.allTimepoints){
			String name = tp.getName();
			hashedTimepoints.put(name, tp);
		}
		for(Timepoint tp : newDTP.interfaceTimepoints){
			String name = tp.getName();
			hashedTimepoints.put(name, tp);
		}

		ArrayList<DisjunctiveTemporalConstraint> newIncoming = new ArrayList<DisjunctiveTemporalConstraint>();
		for( DisjunctiveTemporalConstraint dtc : incomingConstraints) newIncoming.add(dtc.clone(hashedTimepoints));
		newDTP.incomingConstraints = newIncoming;
		ArrayList<DisjunctiveTemporalConstraint>[] csdsc = new ArrayList[subDTPs.length];
		for(int i = 0; i < csdsc.length; i++) csdsc[i] = new ArrayList<>();
		for(int i = 0; i < csdsc.length; i++){
			ArrayList<DisjunctiveTemporalConstraint> subList = currentSubDTPSumCons[i];
			for(int j = 0; j < subList.size(); j++){
				DisjunctiveTemporalConstraint elm = subList.get(j);
				csdsc[i].add(elm.clone(hashedTimepoints));
			}
		}
		ArrayList<DisjunctiveTemporalConstraint>[] ccsc = new ArrayList[subDTPs.length];
		for(int i = 0; i < ccsc.length; i++) ccsc[i] = new ArrayList<>();
		for(int i = 0; i < ccsc.length; i++){
			ArrayList<DisjunctiveTemporalConstraint> subList = currentConnectingSumCons[i];
			for(int j = 0; j < subList.size(); j++){
				DisjunctiveTemporalConstraint elm = subList.get(j);
				ccsc[i].add(elm.clone(hashedTimepoints));
			}
		}
		newDTP.currentSubDTPSumCons = csdsc;
		newDTP.currentConnectingSumCons = ccsc;
		newDTP.mustResolve = mustResolve.clone();
		newDTP.reEmbed = reEmbed;
		
		
		//System.out.println("Finished a MultiDTP");
		return newDTP;
	}


	@Override
	public Stack<DisjunctiveTemporalConstraint> getAdditionalConstraints() {
		// TODO Auto-generated method stub
		Stack<DisjunctiveTemporalConstraint> output = new Stack<DisjunctiveTemporalConstraint>();
		for(DisjunctiveTemporalProblem subDTP : subDTPs){
			output.addAll(subDTP.getAdditionalConstraints());
		}
		
		return output;
	}



	@Override
	public void addTimepoint(Timepoint tp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addContingentTimepoint(Timepoint source) {
		// TODO Auto-generated method stub
		
	}
	
	public int getValidity(){
		return this.VALID;
	}
	
	public void updateValidity(int val){
		if(val != 0 || val != 1){
			System.out.println("ERROR: Incorrect value for VALID.");
		} else {
			this.VALID = val;
		}
	}

	public List<String> getContingentActivities(int time) {
		ArrayList<String> act = new ArrayList<String>();
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			act.addAll(dtp.getContingentActivities(time));
		}
		return act;
	}



	@Override
	public void updateValidity(DisjunctiveTemporalProblem currDTP, int i) {
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
	public void updateInternalData() {
		// TODO Auto-generated method stub
		
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
		ArrayList<Timepoint> all_fixed = new ArrayList<Timepoint>();
		
		for(DisjunctiveTemporalProblem dtp: subDTPs){
			all_fixed.addAll(dtp.getFixedTimepoints());
		}
		
		return all_fixed;
	}

	@Override
	public List<Integer> getMinSlack(int scheduleTime) {
		int i = 0;
		int min = Integer.MAX_VALUE;
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			int temp = dtp.getMinSlack(scheduleTime).get(0);
//			System.out.println("min is "+ min + " subDTP "+i+" has slack " + temp);
//			System.out.println("slack: "+temp+" oldMin: "+min);
			if(temp < min) min = temp;
			i++;
		}
		List<Integer> result = new ArrayList<Integer>(1);
		result.add(min);
		return result;
	}

	@Override
	public int getCallsToSolver() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<String> getUnFixedTimepoints() {
		List<String> unfixed = new ArrayList<String>();
		for(DisjunctiveTemporalProblem dtp : subDTPs){
			List<String> temp = new ArrayList<String>();
			temp = dtp.getUnFixedTimepoints();
			for(String n : temp){
				if(!unfixed.contains(n)) unfixed.add(n);
			}
		}
		return unfixed;
	}

	@Override
	public int checkBookends(int duration) {
		int total = 0;
		for (DisjunctiveTemporalProblem dtp : subDTPs){
			total += dtp.checkBookends(duration);
		}
		total += connectingDTP.checkBookends(duration);
		return total;
	}

	@Override
	public double getOriginalUpperBound(Timepoint tp) {
		MultiDTP initialDTP = this.clone();
		double ub = 0;
		String name = tp.name;
		for (int idx = 0; idx < initialDTP.subDTPs.length; idx++){
			for(DisjunctiveTemporalProblem child : initialDTP.subDTPs){
				ArrayList<DisjunctiveTemporalConstraint> cons = child.getSpecificTempConstraints(tp);
				if(cons != null){
					for (DisjunctiveTemporalConstraint i : cons){
						if(i.getTemporalDifferences().size() >= 1){
							//the 0 below is the agent index
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
					break;//assume this tp is in only one dtp
				}
			}
		}
		return ub;
	}

	@Override
	public double getOriginalLowerBound(Timepoint tp) {
		double ub = 0;
		String name = tp.name;
		for (int idx = 0; idx < this.subDTPs.length; idx++){
			for(DisjunctiveTemporalProblem child : this.subDTPs){
				ArrayList<DisjunctiveTemporalConstraint> cons = child.getSpecificTempConstraints(tp);
				if(cons != null){
					for (DisjunctiveTemporalConstraint i : cons){
						if(i.getTemporalDifferences().size() >= 1){
							//the 0 below is the agent index
							TemporalDifference temp = i.getTemporalDifferences().get(0);
							if(temp.destination.name == "zero"){
								if(temp.source.name == name){
									if(ub <= temp.bound){
										ub = temp.bound;
									}
								}
							}	
						}
					}
					break;//assume this tp is in only one dtp
				}
			}
		}
		return ub;
	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getSpecificTempConstraints(Timepoint tp) {
		String name = tp.name;
		for(int childSimple = 0; childSimple < this.subDTPs.length; childSimple++){
			if(this.subDTPs[childSimple].contains(name)){
				return this.subDTPs[childSimple].getTempConstraints();
			}
		}
		return null;
	}

	@Override
	public ArrayList<DisjunctiveTemporalConstraint> getTempConstraints() {
		// TODO Auto-generated method stub
		// maybe never use it
		return null;
	}

	@Override
	public Vector<MappedPack> getRelatedInputPairs(String crucial) {
		Vector<MappedPack> returnVector = new Vector<MappedPack>();
		String crucialTp = crucial + "_S";
		if(connectingDTP.contains(crucialTp)){
			returnVector = connectingDTP.getRelatedInputPairs(crucial);
		}
		for(int i = 0; i < subDTPs.length; i++){
			if(subDTPs[i].contains(crucialTp)){
				Vector<MappedPack> tempVector = new Vector<MappedPack>();
				tempVector = subDTPs[i].getRelatedInputPairs(crucial);
				for(MappedPack u : tempVector){
					returnVector.add(u);
				}
			}
		}
		return returnVector;
	}

	public ArrayList<Interval> getDTPBoundaries(){
		ArrayList<Interval> ret = new ArrayList<Interval>();

		for(int i=0; i < subDTPs.length; i++){
			// each of these is a Simple DTP or AgentDTP. 
			DisjunctiveTemporalProblem currDTP = subDTPs[i];
			//System.out.println("Boundaries for "+ subDTPLabel + " "+ i + ":");
			ArrayList<Interval> currInt = currDTP.getDTPBoundaries();
			ret.add(i,currInt.get(0));
		}
		return ret;
	}

	@Override
	public void addNonconcurrentConstraint(String source, String dest, int agent) {
		subDTPs[agent].addNonconcurrentConstraint(source, dest, -1);
		
	}

	@Override
	public ArrayList<String> getActivityNames(int agent) {
		// TODO Auto-generated method stub
		return subDTPs[agent].getActivityNames(agent);
	}
	
	public void addFixedTimepoints(Collection<Timepoint> tps){
		for(Timepoint t: tps){
			String name = t.getName();
			Timepoint curr_tp = null;
			for(Timepoint tp: allTimepoints){
				if(tp.getName().equals(name)) curr_tp = tp;
			}
			Integer idx = timepoints.get(curr_tp.getName());
			ArrayList<Timepoint> new_tps = new ArrayList<Timepoint>();
			new_tps.add(curr_tp);
			System.out.println("Re-adding fixed tp: " + curr_tp.getName() + " to subDTP: " + idx);
			subDTPs[idx].addFixedTimepoints(new_tps);
		}
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
