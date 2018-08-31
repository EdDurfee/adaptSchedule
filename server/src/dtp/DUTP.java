package dtp;

import interactionFramework.Generics;
import interactionFramework.MappedPack;
import interactionFramework.testFramework;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import dtp.DisjunctiveTemporalProblem.ActivityFinder;

import stp.Timepoint;
//import util.MapUtil;
import util.MultiNode;
import util.Node;
import util.Triple;
/**
 * A class for representing a more general Disjunctively-Uncertain Temporal Problem in which the underlying temporal 
 * problem is a DTP.  
 **/
public class DUTP implements DisjunctiveTemporalProblem {

	
	public static String SPORADICNAME = "sporadic";
	public static int DUR = 20; //duration of the sporadic event.
	public static  int SporadicIntervalLength = 600; //length of scheduling period for sporadic interval -- DEPRECATED, replaced by interval below
	public static  int SporadicEventET = 0;  //earliest time of sporadic event
	public static  int SporadicEventLT = 1440;  //latest time of sporadic event (NOTE: sporadic activity could happen later, depending on precendence)
	public ArrayList<DUSTP> componentDUSTPs = new ArrayList<DUSTP>();
	public ArrayList<DUSTP> allDUSTPs = new ArrayList<DUSTP>();
	public ArrayList<DUSTP> nonDCDUSTPs = new ArrayList<DUSTP>();
	public MultiNode<ArrayList<DUSTP>> dustpTree = null;
	public MultiNode<ArrayList<DUSTP>> currNode = null; // keeps track of, "globally" which point we're at in this tree
	private ArrayList<DisjunctiveTemporalProblem> componentSTPs = new ArrayList<DisjunctiveTemporalProblem>();
	public DisjunctiveTemporalProblem dtp = null;
	public HashMap<SimpleEntry<String,String>, Integer> precMap = new HashMap<SimpleEntry<String, String>, Integer>();

	
	public DUTP(DisjunctiveTemporalProblem dtproblem, boolean populate){
		dtp = dtproblem.clone();
		componentSTPs = dtp.getComponentSTPs().getValue();
		//System.out.println("dtp has this many component STPs: " + componentSTPs.size());
		if(populate) populateComponentDUSTPs(-1);

	}
	
	public DUTP(DisjunctiveTemporalProblem dtproblem, boolean populate, HashMap<SimpleEntry<String, String>, Integer> map){
		dtp = dtproblem.clone();
		componentSTPs = dtp.getComponentSTPs().getValue();
		precMap = map;
		if(populate) populateComponentDUSTPs(0);
		
	}
	public DUTP(DisjunctiveTemporalProblem dtproblem, ArrayList<DUSTP> comps){
		dtp = dtproblem;
		
		allDUSTPs = comps;
	}
	
	public DUTP(DisjunctiveTemporalProblem dtproblem, ArrayList<DUSTP> comps, HashMap<SimpleEntry<String,String>, Integer> map){
		dtp = dtproblem;
		
		allDUSTPs = comps;
		precMap = map;
	}
	
	public DUTP(DisjunctiveTemporalProblem dtproblem, boolean populate, int prec){
		dtp = dtproblem;
		componentSTPs = dtp.getComponentSTPs().getValue();
//		if(populate) populateComponentDUSTPs(prec);
		if(populate) populateComponentDUSTPsMany(prec);
	}
	
	public DUTP(DisjunctiveTemporalProblem dtproblem, boolean populate, int prec, HashMap<SimpleEntry<String,String>, Integer> map){
		dtp = dtproblem;
		componentSTPs = dtp.getComponentSTPs().getValue();
		precMap = map;
		if(populate) populateComponentDUSTPs(prec);
		
	}
	
	
	/**
	 * Generates the component DUSTPs one by one, checking for dynamic controllability. returns 
	 * @return true once one DC component DUSTP is found.
	 */
	public boolean generateComponentsFindDC(){

		for(DisjunctiveTemporalProblem stp : componentSTPs){
			DUSTP cand = new DUSTP((SimpleDTP) stp.clone());
			if(cand.checkWeakControllability()){
				if(cand.checkDynamicControllability()) {
					return true;
				
				}
			}
		} return false;
	}
	
	/**
	 * Generates the full set of component DUSTPs, dividing them into sets of DC and nonDC components.
	 * @param prec
	 */
	public void populateComponentDUSTPs(int prec){
		for(DisjunctiveTemporalProblem stp : componentSTPs){
			DUSTP cand = new DUSTP((SimpleDTP) stp, prec);
//			System.out.println("About to check DC.");
			if(cand.checkDynamicControllability()){
//				System.out.println("FOUND a DC component DU-STP");				
				componentDUSTPs.add(cand);
				allDUSTPs.add(cand);
			} else {
				System.out.println("FOUND a non-DC component DU-STP");
				allDUSTPs.add(cand);
				DUSTP cand2 = cand.clone();
				cand2.fixNonDCDUSTP();
				nonDCDUSTPs.add(cand2);

				
			}
		}
	}
	
	/**
	 * Generates the full set of component DUSTPs, based on considering every specific sporadic event occurrance time, 
	 * dividing them into sets of DC and nonDC components.
	 * @param prec
	 */
	public void populateComponentDUSTPsMany(int prec){
		for(DisjunctiveTemporalProblem stp : componentSTPs){					
				DUSTP cand = new DUSTP((SimpleDTP) stp, prec, true);
//				System.out.println("About to check DC.");
				if(cand.checkDynamicControllability()){
//					System.out.println("FOUND a DC component DU-STP");				
					componentDUSTPs.add(cand);
					allDUSTPs.add(cand);
				} else {
					System.out.println("FOUND a non-DC component DU-STP");
					allDUSTPs.add(cand);
					DUSTP cand2 = cand.clone();
					cand2.fixNonDCDUSTP();
					nonDCDUSTPs.add(cand2);
				}
			
		}
	}
	

	public void skipOverSporadicNotDone() {
		for(DUSTP dustp : componentDUSTPs) {
			dustp.skipOverSporadicNotDone();
		}
	}

	

	private void populatePrecedenceMap(int prec){
		if(!(prec < 0)){
			//then we populate with all values of this integer. 
			ArrayList<String> acts = this.getActivityNames(0);
			ArrayList<String> sps = new ArrayList<String>();
			sps.add(SPORADICNAME);
			//iterate over both of these lists computing all combinations (not permutations)
			for(int i = 0; i < acts.size(); i++){
				for(int j = 0; j < sps.size(); j++){
					SimpleEntry<String, String> pair = new SimpleEntry<String,String>(acts.get(i), sps.get(j));
					this.precMap.put(pair, prec);
				}
			}
		}
	}
	
	/**
	 * Populates the allDUSTPs structure without checking for controllability
	 * @param prec
	 */
	public void populateAllComponentDUSTPs(int prec){
		for(DisjunctiveTemporalProblem stp : componentSTPs){
			DUSTP cand = new DUSTP((SimpleDTP) stp, prec);
			allDUSTPs.add(cand);
		}
	}
	
	//checks DC for all component DUSTPs. Makes the non DC ones invalid.
	// this function does iterate through the full set before returning true or false.
	public boolean checkDynamicControllability(){
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		for(DUSTP comp : allDUSTPs){
			boolean dc = comp.checkDynamicControllability();
			if(!dc) {
				comp.updateValidity(0);
				nonDCDUSTPs.add(comp);
			}else{ //problem is DC, we want to add it to this set.
				componentDUSTPs.add(comp);
			}
			res.add(dc);
			
		}
		for(boolean elm : res){
			if(elm) return true;
		}
		return false;
	}
	
	public boolean existHybridInNonDC(){
		for(DisjunctiveTemporalProblem stp : componentSTPs){
			DUSTP cand = new DUSTP((SimpleDTP) stp.clone(), 0, precMap);
			if(cand.checkWeakControllability()){
				if(cand.checkDynamicControllability()){
					componentDUSTPs.add(cand);
				}else{
					nonDCDUSTPs.add(cand);
				}
			}else{
				nonDCDUSTPs.add(cand);
			}
		}
		
		if(nonDCDUSTPs.size() == 0 || nonDCDUSTPs.size() == 1) {
			return false;
		}
		
		if(nonDCDUSTPs.size() > 0){
 			System.out.println("more than 0 non DC DUSTPs to consider.");
		}
		
		//iterate through the nonDC components looking for hybrids. 
		ArrayList<ArrayList<Integer>> stickingPoints = new ArrayList<ArrayList<Integer>>();
		for(DUSTP d : nonDCDUSTPs){
			if(!d.fixNonDCDUSTP()){
				nonDCDUSTPs.remove(d);
			}
		}
		for(DUSTP d1 : nonDCDUSTPs){
			stickingPoints.add(d1.se_times);
		}
		System.out.println(stickingPoints);
		boolean val = existHybridAnswers(stickingPoints);
		
		if(!val) return false;
		if(nonDCDUSTPs.size() < 2) return false;
		System.out.println("we do have non DC DUSTPs with possible hybrid answers, continuing on.");
		//testFramework.writeToFile(this.dtp);
		System.out.println(stickingPoints);
		//buildDUSTPTree(); //populate the DUSTP tree that we need.
		// so this is where we fix them and make them DC. 
		
		return false;
	}
	
	public boolean checkDCFindFirst(){
		for(DisjunctiveTemporalProblem stp : componentSTPs){
			DUSTP cand = new DUSTP((SimpleDTP) stp.clone(), 0, precMap);
			if(cand.checkWeakControllability()){
				if(cand.checkDynamicControllability()){
					//Generics.printDTP(cand.stp);
					//System.out.println(cand.stp.getTempConstraints());
					return true;
				}else{
					nonDCDUSTPs.add(cand);
				}
			}else{
				nonDCDUSTPs.add(cand);
			}
		}
		System.out.println("no component STPs were DC DUSTPs. checking for hybrid solutions.");
		System.out.println("number of non-DC components: " + nonDCDUSTPs.size());
		
		if(nonDCDUSTPs.size() == 0 || nonDCDUSTPs.size() == 1) {
			return false;
		}
		
		if(nonDCDUSTPs.size() > 0){
 			System.out.println("more than 0 non DC DUSTPs to consider.");
		}
		
		//iterate through the nonDC components looking for hybrids. 
		ArrayList<ArrayList<Integer>> stickingPoints = new ArrayList<ArrayList<Integer>>();
		for(DUSTP d : nonDCDUSTPs){
			if(!d.fixNonDCDUSTP()){
				nonDCDUSTPs.remove(d);
			}
		}
		for(DUSTP d1 : nonDCDUSTPs){
			stickingPoints.add(d1.se_times);
		}
		System.out.println(stickingPoints);
		boolean val = existHybridAnswers(stickingPoints);
		
		if(!val) return false;
		if(nonDCDUSTPs.size() < 2) return false;
		System.out.println("we do have non DC DUSTPs with possible hybrid answers, continuing on.");
		//testFramework.writeToFile(this.dtp);
		System.out.println(stickingPoints);
		//buildDUSTPTree(); //populate the DUSTP tree that we need.
		// so this is where we fix them and make them DC. 
		
		
		return false;
	}
	
	public HashMap<ArrayList<Integer>, Node<ArrayList<SimpleDTP>>> getHybridMap(){
		HashMap<ArrayList<Integer>, Node<ArrayList<SimpleDTP>>> map = new HashMap<ArrayList<Integer>, Node<ArrayList<SimpleDTP>>>();
		ArrayList<ArrayList<Integer>> stickingPoints = new ArrayList<ArrayList<Integer>>();

		for(DUSTP d1 : nonDCDUSTPs){
			stickingPoints.add(d1.se_times);
		}
		
		for(int i = 0; i < stickingPoints.size(); i++){
			map.put(stickingPoints.get(i), nonDCDUSTPs.get(i).DTree); //might not be the right value?!
		}
		
		return map;
	}
	
	public boolean checkDynamicControllabilityNoAnswers(){
		if(componentDUSTPs.size() > 0){
			return true;
		}else if(nonDCDUSTPs.size() > 0){
			boolean bool = false;
			//use function to check for hybrid answers
			return bool;
		}else{
			return false;
		
		}
	}
	
	 public boolean checkWeakControllability(){
		 return componentDUSTPs.size() > 0;
	 }
	
	//return true as soon as you find one.
	public boolean findOneDC(){
		for(DUSTP comp : componentDUSTPs){
			if(comp.checkDynamicControllability()) return true;
		}
		return false;
	}
	
	@Override
	public void enumerateSolutions(int scheduleTime) {
		dtp.enumerateSolutions(scheduleTime);
//		System.out.println("Underlying DTP has this many solutions: " + dtp.getNumSolutions());
//		System.out.println("Number of componentDUSTPs is " + componentDUSTPs.size());
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) {
//				System.out.println("Enumerating solutions for DUSTP");
				curr.enumerateSolutions(scheduleTime);
			} else {
//				System.out.println("DUSTP is invalid.");
			}
		}
		
	}

	@Override
	public boolean nextSolution(int scheduleTime) {
		boolean orig = dtp.nextSolution(scheduleTime);
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		res.add(orig);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
				r = curr.nextSolution(scheduleTime);
				res.add(r);
			}
		}
		
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void enumerateInfluences(int scheduleTime) {
		dtp.enumerateInfluences(scheduleTime);
		
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.enumerateInfluences(scheduleTime);
		}
		
	}

	@Override
	public boolean nextInfluence(int scheduleTime) {
		boolean orig = dtp.nextInfluence(scheduleTime);
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		res.add(orig);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
				r = curr.nextInfluence(scheduleTime);
				res.add(r);
			}
		}
		
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void establishMinimality(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		dtp.establishMinimality(timepointsToConsider, scheduleTime);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.establishMinimality(timepointsToConsider, scheduleTime);
		}

	}

	@Override
	public boolean solveNext(List<Timepoint> timepointsToConsider,
			int scheduleTime) {
		boolean orig = dtp.solveNext(timepointsToConsider, scheduleTime);
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		res.add(orig);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
				r = curr.solveNext(timepointsToConsider, scheduleTime);
				res.add(r);
			}
		}
		
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void advanceToTime(int time, int deltaTime, boolean pushSelection) {
		dtp.advanceToTime(time, deltaTime, pushSelection);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.advanceToTime(time, deltaTime, pushSelection);
		}

	}

	@Override
	public void advanceToTime(int time, boolean resolve, int deltaTime,
			boolean pushSelection) {
		dtp.advanceToTime(time, resolve, deltaTime, pushSelection);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.advanceToTime(time, resolve, deltaTime, pushSelection);
		}
		
	}
	
	@Override
	public void advanceSubDTPToTime(int time, int deltaT, boolean pushSelection, int dtpNum) {
		System.out.println("Error: advanceSubDTPToTime() not implemented for " + this.getClass().getSimpleName());
	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, int deltaTime, boolean pushSelection) {
		dtp.tightenTimepoint(timeStart, tp1, timeEnd, tp2, deltaTime, pushSelection);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.tightenTimepoint(timeStart, tp1, timeEnd, tp2, deltaTime, pushSelection);
		}
		
	}

	@Override
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		dtp.tightenTimepoint(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.tightenTimepoint(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		}
		
	}

	@Override
	public void executeAndAdvance(int timeStart, String tp1, int timeEnd,
			String tp2, boolean resolve, int deltaTime, boolean pushSelection) {
		if(!tp1.equals(SPORADICNAME+"_S")){
			dtp.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
		
		
		
			Iterator<DUSTP> it = componentDUSTPs.iterator();
			while(it.hasNext()){
				DUSTP curr = it.next();
				
				if(curr.getValidity() == 1){
					if(curr.getActivities(ActivityFinder.TIME, timeStart).get(0).contains(tp1.substring(0,tp1.length()-2))){
						curr.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
					}else{
						curr.updateValidity(0);
					}
				}
			}
		}else{
			// we are performing the sporadic activity, and we want to let the DUSTP do all the stuff
			System.out.println("Processing the sporadic activity");
			dtp.advanceToTime(timeEnd,DUR,false);
			dtp.simplifyMinNetIntervals();
			Iterator<DUSTP> it = componentDUSTPs.iterator();
			while(it.hasNext()){
				DUSTP curr = it.next();
				if(curr.getValidity() == 1) {
//					System.out.println("Advancing a component DUSTP");
					curr.executeAndAdvance(timeStart, tp1, timeEnd, tp2, resolve, deltaTime, pushSelection);
				}
			}
		}
		
	}

	@Override
	public void addAdditionalConstraint(String tpS, String tpE,
			IntervalSet dtc, int time, boolean resolve, boolean pushSelection) {
		dtp.addAdditionalConstraint(tpS, tpE, dtc, time, resolve, pushSelection);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addAdditionalConstraint(tpS, tpE, dtc, time, resolve, pushSelection);
		}
		
	}

	@Override
	public void addAdditionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		dtp.addAdditionalConstraints(col);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addAdditionalConstraints(col);
		}
		
	}

	@Override
	public void addVolitionalConstraints(
			Collection<DisjunctiveTemporalConstraint> col) {
		dtp.addVolitionalConstraints(col);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addVolitionalConstraints(col);
		}
		
	}

	@Override
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons) {
		dtp.addAdditionalConstraint(cons);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addAdditionalConstraint(cons);
		}
		
	}

	@Override
	public void addVolitionalConstraint(DisjunctiveTemporalConstraint cons) {
		dtp.addVolitionalConstraint(cons);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addVolitionalConstraint(cons);
		}
		
	}

	@Override
	public boolean fixZeroValIntervals() {
		boolean orig = dtp.fixZeroValIntervals();
		boolean r = false;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		res.add(orig);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) {
				r = curr.fixZeroValIntervals();
				res.add(r);
			}
		}
		for(boolean elm : res){
			if(elm == false) return false;
		}
		return true;
	}

	@Override
	public void simplifyMinNetIntervals() {
		dtp.simplifyMinNetIntervals();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.simplifyMinNetIntervals();
		}

		
	}

	@Override
	public int getMinTime() {
//		System.out.println("Getting minTime for DUTP.");
		ArrayList<Integer> minTimes = new ArrayList<Integer>();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
//				System.out.println("Computing minTime for valid component DUSTP.");
				minTimes.add(curr.getMinTime());
//				System.out.println("Computed minTime for valid component DUSTP.");
			}
		}
//		System.out.println("The number of mintimes is: " + minTimes.size());
		if(dtp.getValidity() == 1) minTimes.add(dtp.getMinTime());
//		System.out.println("The number of mintimes is: " + minTimes.size());
		return Collections.min(minTimes);
	}

	@Override
	public int[] getMinTimeArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getMaxSlack() {
		ArrayList<Integer> maxSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
				int slack = curr.getMaxSlack().get(0);
				maxSlacks.add(slack);
			}
		}
		maxSlacks.add(dtp.getMaxSlack().get(0));
		ret.add(Collections.min(maxSlacks));               //ED: this used to compute min instead of max.  Not sure why.
		return ret;
	}

	@Override
	public IntervalSet getInterval(String tp1, String tp2) {
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		IntervalSet outis = new IntervalSet();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
				IntervalSet is = curr.getInterval(tp1, tp2);
				for(Interval i : is) outis.add(i);
			}
		}
		outis.simplify();
		return outis;
	}
	
	public int getNumDUSTPs(){
		return componentDUSTPs.size();
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Timepoint> getTimepoints() {
		
		return dtp.getTimepoints();
	}

	@Override
	public ArrayList<Timepoint> getInterfaceTimepoints() {
		return dtp.getInterfaceTimepoints();
	}

	@Override
	public Timepoint getTimepoint(String tpS) {
		return dtp.getTimepoint(tpS);
	}

	@Override
	public boolean contains(String tp1) {
		return dtp.contains(tp1);
	}

	@Override
	public int getNumSolutions() {
		ArrayList<Integer> solns = new ArrayList<Integer>();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			solns.add(it.next().getNumSolutions());
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
		return dtp.getNumAgents();
	}

	@Override
	public int getCurrentAgent() {
		return dtp.getCurrentAgent();
	}

	@Override
	public void setCurrentAgent(int agent) {
		dtp.setCurrentAgent(agent);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.setCurrentAgent(agent);
		}

		
	}

	@Override
	public int getAgent(String tpS) {
		return dtp.getAgent(tpS);
	}

	@Override
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void softReset() {
		dtp.softReset();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.softReset();
		}

		
	}

	@Override
	public void clearIncomingConstraints() {
		dtp.clearIncomingConstraints();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.clearIncomingConstraints();
		}

		
	}

	@Override
	public void addIncomingConstraint(DisjunctiveTemporalConstraint cons) {
		dtp.addIncomingConstraint(cons);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addIncomingConstraint(cons);
		}

		
	}

	@Override
	public void addIncomingConstraints(
			Collection<DisjunctiveTemporalConstraint> constraints) {
		dtp.addIncomingConstraints(constraints);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addIncomingConstraints(constraints);
		}

		
	}

	@Override
	public void setIncomingConstraints(
			ArrayList<DisjunctiveTemporalConstraint> constraints) {
		dtp.setIncomingConstraints(constraints);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
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
		dtp.addInterfaceTimepoint(tp);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addInterfaceTimepoint(tp);
		}

		
	}

	@Override
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
		dtp.addInterfaceTimepoints(timepoints);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) dtp.addInterfaceTimepoints(timepoints);
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
	
	public int getNumValidCompDUSTPs(){
		int count = 0;
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			if(it.next().getValidity() == 1) count++;
		}
		return count;
	}

	@Override
	public List<List<String>> getActivities(ActivityFinder af, int time) {
//		System.out.println("Getting activities for " + componentDUSTPs.size() + " component DUSTPs at time " +time);
		List<List<String>> ret = new LinkedList<>();
		Set<String> acts_out = new HashSet<String>();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		int countDUSTPs = 0;
		if(!(it.hasNext())) {System.out.println("No Component DUSTPs!");}
		while(it.hasNext()){
			DUSTP curr = it.next();
			countDUSTPs = countDUSTPs + 1;
//			System.out.println("DUSTP count is " + countDUSTPs);
			if(curr.getValidity() == 1){
				ArrayList<String> acts = new ArrayList<String>();
//				System.out.println("Getting activities for DUSTP " + countDUSTPs);
				List<String> curr_acts = curr.getActivities(af, time).get(0);
//				System.out.println("Got activities for DUSTP" + countDUSTPs + curr_acts);
				if(curr_acts.size() == 0){
					//this is bad bc we aren't offering any activities 
					System.out.println("One of the components doesn't have any valid activities");
					Generics.printDTP(curr);
					curr.updateValidity(0);
				}
				if(curr_acts.size() == 1 && curr_acts.get(0).contains(SPORADICNAME)){		
					//ED: Second test used to be the one below, but seems to just redundantly do work of finding current activities
					// curr.getActivities(af,time).get(0).contains(SPORADICNAME)){
					//if it's only offering sporadic, it should be made invalid
					curr.updateValidity(0);
					continue;
				}
				acts.addAll(curr_acts);
				acts_out.addAll(acts);
				//result.add(acts);
			} else {
//				System.out.println("Invalid DUSTP is :");
//				Generics.printDTP(curr);
			}
		}
		ret.add(new ArrayList<String>(acts_out));
		return ret;
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
		return dtp.getAdditionalConstraints();
	}


	@Override
	public void addTimepoint(Timepoint tp) {
		dtp.addTimepoint(tp);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addTimepoint(tp);
		}

		
	}

	@Override
	public void addContingentTimepoint(Timepoint source) {
		dtp.addContingentTimepoint(source);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addContingentTimepoint(source);
		}

		
	}

	@Override
	public int getValidity() {
		// TODO Auto-generated method stub
		return -1;
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
		dtp.updateInternalData();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.updateInternalData();
		}
	}

	@Override
	public ArrayList<Timepoint> getFixedTimepoints() {
		return dtp.getFixedTimepoints();
	}

	@Override
	public List<Integer> getMinSlack(int scheduleTime) {

		ArrayList<Integer> minSlacks = new ArrayList<Integer>();
		List<Integer> ret = new ArrayList<Integer>();
		
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1){
				int slack = curr.getMinSlack(scheduleTime).get(0);
				minSlacks.add(slack);
			}
		}
		minSlacks.add(dtp.getMinSlack(scheduleTime).get(0));
		ret.add(Collections.min(minSlacks));
		return ret;
	}

	@Override
	public int getCallsToSolver() {
		int total = 0;
		total += dtp.getCallsToSolver();
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			total += it.next().getCallsToSolver();
		}
		return total;

	}

	@Override
	public List<String> getUnFixedTimepoints() {
		return dtp.getUnFixedTimepoints();
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
		return dtp.getDTPBoundaries();
	}

	@Override
	public void addNonconcurrentConstraint(String source, String dest, int agent) {
		dtp.addNonconcurrentConstraint(source, dest, agent);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addNonconcurrentConstraint(source, dest, agent);
			
		}
	}

	@Override
	public ArrayList<String> getActivityNames(int agent) {
		return dtp.getActivityNames(agent);
	}

	@Override
	public void addFixedTimepoints(Collection<Timepoint> tps) {
		dtp.addFixedTimepoints(tps);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addFixedTimepoints(tps);
		}

		
	}

	@Override

	public void updateTimepointName(Timepoint tp, String new_name) {
		dtp.updateTimepointName(tp, new_name);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.updateTimepointName(tp, new_name);
		}

		
	}

	@Override
	public void addDurationConstraint(Timepoint start, Timepoint end,
			int duration) {
		String tp_start = start.getName();
		String tp_end = end.getName();
		dtp.addDurationConstraint(start, end, duration);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			Timepoint tps = curr.getTimepoint(tp_start);
			Timepoint tpe = curr.getTimepoint(tp_end);
			if(curr.getValidity() == 1) curr.addDurationConstraint(tps, tpe, duration);
		}

		
	}

	@Override
	public void addOrderingConstraint(String source, String dest,
			int min_duration, int max_duration) {
		dtp.addOrderingConstraint(source, dest, min_duration, max_duration);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.addOrderingConstraint(source, dest, min_duration, max_duration);
		}

		
	}

	@Override
	public void removeDurationConstraint(String tpName) {
		dtp.removeDurationConstraint(tpName);
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.removeDurationConstraint(tpName);
		}

		
	}
	
	public void pruneComponentDUSTPs(String act_name, int time){
		//assumes all components have been updated. 
		//prunes components that don't offer the activity the user selected as the next option. 
		
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 0) continue;
			String act_now = curr.getActivities(ActivityFinder.TIME, time).get(0).get(0);
			//need to see if the validity needs to be changed to zero. 
			if(!act_now.equals(act_name)) curr.updateValidity(0);
		}
	}

	@Override
	//this function isn't applicable for this class. refers to compSTPs of a DTP.
	public SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> getComponentSTPs() {
		return null;
	}
	
	public DUTP clone(){
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		DisjunctiveTemporalProblem newDTP = dtp.clone();
		ArrayList<DUSTP> newComps = new ArrayList<DUSTP>();
		while(it.hasNext()){
			DUSTP curr = it.next();
			newComps.add(curr.clone());
		}
		HashMap<SimpleEntry<String, String>, Integer> newPrec = new HashMap<SimpleEntry<String, String>, Integer>();
		Iterator<SimpleEntry<String,String>> it2 = precMap.keySet().iterator();
		while(it2.hasNext()){
			SimpleEntry<String,String> names = it2.next();
			newPrec.put(names, precMap.get(names));
		}
		return new DUTP(newDTP, newComps, newPrec);
	}
	@Override
	public void advanceDownTree() {
		Iterator<DUSTP> it = componentDUSTPs.iterator();
		while(it.hasNext()){
			DUSTP curr = it.next();
			if(curr.getValidity() == 1) curr.advanceDownTree();
		}
	}
	@Override
	public SimpleEntry<Integer, Integer> getMinSlackInterval(int scheduleTime) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void buildDUSTPTree() {
		//does the initial population of a DUSTP tree for this DUTP to set up for the 
		// full complete algorithm
		ArrayList<DUSTP> compDUSTPs = new ArrayList<DUSTP>();
		HashMap<DUSTP, ArrayList<String>> orderings = new HashMap<DUSTP, ArrayList<String>>();
		//first make clones of all of the component DU-STPs to place into the tree
		// and accumulate orderings of activities in various DUSTPs
		
		for(DUSTP dustp : nonDCDUSTPs){
			DUSTP newcomp = dustp.clone();
			compDUSTPs.add(newcomp);
			orderings.put(newcomp, newcomp.getActivityOrdering());
		
		}
		//all components are in the root before we make any decisions
		MultiNode<ArrayList<DUSTP>> root = new MultiNode<ArrayList<DUSTP>>(compDUSTPs);
		MultiNode<ArrayList<DUSTP>> curr = root;
		int numActs = orderings.get(compDUSTPs.get(0)).size();
//		System.out.println("There are this many activities: " + numActs);
		String currAct = "";
		//top level: iterate over component DUSTPs
		for(DUSTP comp : root.getNodeData()){
			//second level iterate through activity ordering
			ArrayList<String> order = comp.getActivityOrdering();
			curr = root;
			for(int i = 0; i < numActs; i++){
				currAct = order.get(i);
				ArrayList<MultiNode<ArrayList<DUSTP>>> children = curr.getChildren();
				ArrayList<String> labels = curr.getChildrenLabels();
//				System.out.println("labels are : " + labels);
				if(labels.contains(currAct)){
					//then the child we need already exists and we can iterate through and find it
//					System.out.println("child with this label exists already:" + currAct);
					for(MultiNode<ArrayList<DUSTP>> child : children){
						if(child.getLabel().equals(currAct)){
							// we want to add the new DUSTP to this node. and then continue
							child.data.add(comp);
							curr = child;
						}
					}
				}
				
				else{
//					System.out.println("this node has no valid children yet or child doesn't exist.");
					//need to make a new child node corresponding to the current activity \
					ArrayList<DUSTP> toAdd = new ArrayList<DUSTP>();
					toAdd.add(comp);
					MultiNode<ArrayList<DUSTP>> newChild = new MultiNode<ArrayList<DUSTP>>(toAdd, curr);
					newChild.setLabel(currAct);
					curr.addChild(newChild);
					curr = newChild;
				}
			}
		}
		dustpTree = root;
		currNode = root;
		
	}	
	
		//at each step we need to look at what the "nth" activity availabile in each DUSTP is. 
		// then we order them that way. First coming up with a list of possible first activities 
		
		
		//it's possible that, during execution, other constraints differences (apart from activity ordering)
		// present in these component problems actually rule them out of particular paths... 
		// ARE you accounting for this properly?? 
		
		
	
	
	//note: this is unfinished 
	//TODO: finish this... or delete it.
	public boolean checkDUSTPTree(){
		// check that there is some branch that handles the sporadic event at each level of the tree. 
		// can do this by checking is SPORADICNAME is a member of the list of activities in the problem. 
		
		MultiNode<ArrayList<DUSTP>> curr = dustpTree;
		ArrayList<MultiNode<ArrayList<DUSTP>>> childList = new ArrayList<MultiNode<ArrayList<DUSTP>>>();
		
		childList = curr.getChildren();

			//THIS CHECK is probably not what you're looking for, actually
		while(curr.getChildren().size() > 0){
			//check the curr node
			ArrayList<ArrayList<String>> actLists = new ArrayList<ArrayList<String>>();
			for(MultiNode<ArrayList<DUSTP>> child : childList){
				ArrayList<DUSTP> comps = child.getNodeData();
				ArrayList<String> tempacts = new ArrayList<String>();
				for(DUSTP d : comps){
					tempacts.addAll(d.getActivityOrdering());
				}
				actLists.add(tempacts);
				tempacts.clear();
			}
			
			
		}
		
		return true;
	}
	// not finished, might be abandoned.
	public ArrayList<DUSTP> exploreDUSTPTree(){
		ArrayList<DUSTP> hybrids = new ArrayList<DUSTP>();
		
		MultiNode<ArrayList<DUSTP>> curr = dustpTree;
		
		while(curr.getChildren() != null || curr.getChildren().size() > 0){
			//do something with current node. 
			
		}
		
		return hybrids;
	}
	
	//tested and works. 
	public boolean existHybridAnswers(Collection<ArrayList<Integer>> stickingPoints){
		//want to check to make sure that these lists don't have any numbers in common. 
		int n = nonDCDUSTPs.get(0).getDTreeSize();
		//boolean ret = true;
		ArrayList<Boolean> res = new ArrayList<Boolean>();
		for(int i = 0; i < n; i++){
			for(ArrayList<Integer> comp : stickingPoints){
				
				if(!comp.contains(i)){
					res.add(true);
					break;
				}
				
			}
			
		//System.out.println(res);	
		}
		if(res.size() == n) return true;
		return false;
	}
	
	public ArrayList<DUSTP> iterativeExplore(){
		
		ArrayList<DUSTP> hybrids = new ArrayList<DUSTP>();
		ArrayList<ArrayList<SimpleDTP>> DTreesToBuild = new ArrayList<ArrayList<SimpleDTP>>();
		HashMap<DUSTP, ArrayList<SimpleDTP>> allPossibleISTPs= new HashMap<DUSTP, ArrayList<SimpleDTP>>();
		//ArrayList<SimpleEntry<DUSTP, ArrayList<SimpleDTP>>> allPossibleComps = new ArrayList<SimpleEntry<DUSTPArrayList<SimpleDTP>>(); //to keep track of lists of valid components from which we want to build DTrees
		//ArrayList<SimpleEntry<DUSTP, ArrayList<Integer>>> stickingPoints = new ArrayList<SimpleEntry<DUSTP, ArrayList<Integer>>>();
		HashMap<DUSTP, ArrayList<Integer>> stickingPoints = new HashMap<DUSTP, ArrayList<Integer>>();
		for(DUSTP d : dustpTree.getNodeData()){
			allPossibleISTPs.put(d, d.getISTPs());
			stickingPoints.put(d,d.se_times);
		}
		System.out.println(stickingPoints.values());
		
		//moved this up to the checkDC function 
		//if(!existHybridAnswers(stickingPoints.values())) return null;
		
		// we want a stack of triples that keep track of info we want to jump back to. 
		// Left: MultiNode that we're jumping to
		// Middle: ArrayList<SimpleDTP> holds the iSTPs that we've accumulated up to, but not including the multinode
		// Right: next DUSTP we want to consider in NodeData. thus we will add one of these for each additional
		//       possible DUSTP in the node data at the MultiNode level.
		
		Stack<Triple<MultiNode<ArrayList<DUSTP>>, ArrayList<SimpleDTP>, DUSTP>> jumpBack   = new Stack<Triple<MultiNode<ArrayList<DUSTP>>, ArrayList<SimpleDTP>, DUSTP>>();
		
		//we'll want to add all items from the root node to this stack initially. probably. only ones that handle the SE in the first place?
		
		MultiNode<ArrayList<DUSTP>> curr = dustpTree; // start with the root.
		for(int i = 0; i < curr.getNodeData().size(); i++){
			//now we want to figure out which of these can handle the first sporadic event. if it can't, continue
			//remember. se_times tells which pieces the problem CANT handle.
			DUSTP currdustp = curr.getNodeData().get(i);
			if(currdustp.se_times.contains(0)){
				continue;
			}
			// if it can handle the first time. then we want to add that component to a list.
			ArrayList<SimpleDTP> iSTPs = allPossibleISTPs.get(currdustp);
			ArrayList<SimpleDTP> compsToAdd = new ArrayList<SimpleDTP>();
			compsToAdd.add(iSTPs.get(0));
		}
		
		
		
		Stack<MultiNode<ArrayList<DUSTP>>> stack = new Stack<MultiNode<ArrayList<DUSTP>>>();
		//MultiNode<ArrayList<DUSTP>> curr = dustpTree;
		stack.push(curr);
		ArrayList<SimpleDTP> compsToAdd = new ArrayList<SimpleDTP>();
		int last_added = 0;
		while(!stack.empty()){
			curr = stack.pop();
			int i = stickingPoints.get(curr).get(0); //the first point at which curr breaks and needs to switch to a different problem. 
			if(i < last_added) //we need to move down the tree or something?
			//how do we get the relevant DUSTP problem from the current node??
				//should the zero here be last_added??
			for(int j = 0; j < i; j++){
				
				//compsToAdd.add(j, allPossibleISTPs.get());
			}
				
				
			if(!curr.isExplored()){
				//do something with curr in this part??
				//i think we need to do something here to check if it's a leaf node and break. or something
				// or check that it's a leaf node and then decide that we're adding the hybrid thing we've developed to the list 
				
				//mark it as explored
				curr.markExplored();
				//push all of curr's children onto the stack
				for(MultiNode<ArrayList<DUSTP>> child : curr.getChildren()){
					stack.push(child);
				}
			}
		}
		
		return hybrids;
	}
	
	
	//change the return here to whatever you end up needing!!
	public ArrayList<DUSTP> checkHybridSolutions(ArrayList<ArrayList<SimpleDTP>> hybrids){
		ArrayList<DUSTP> hybridOutput = new ArrayList<DUSTP>();
		
		//can first test that each iSTP but the last one has the sporadic event in it?? 
		
		boolean valid = true;
		for(ArrayList<SimpleDTP> hybrid : hybrids){
			for(int i = 0; i < hybrid.size() - 1; i++){
				SimpleDTP d = hybrid.get(i);
				ArrayList<String> acts = d.getActivityNames();
				Generics.printDTP(d);
				if(!acts.contains("sporadic")){
					valid = false;
				}
			} 
			if(!valid) continue;
			
			DUSTP du = new DUSTP(hybrid); //TODO: you've put the underlying problem in as null.. maybe not what you want.
			if(du.checkDynamicControllability()){
				hybridOutput.add(du);
			}
		}
		return hybridOutput;

	}

}
