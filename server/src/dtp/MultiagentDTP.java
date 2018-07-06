package dtp;

import interactionFramework.Generics;
import interactionFramework.MappedPack;
import interval.IntervalSet;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.AbstractMap.SimpleEntry;

import stp.Timepoint;

public class MultiagentDTP extends MultiDTP implements java.io.Serializable{
	/**
	 * Changes the “multi” pieces of the multiDTP such that the subDTPs in the problem correspond to the different agents 
	 * in the multiagent problem and the connecting DTP is the interagent DTP. 
	 */
	private static final long serialVersionUID = -5541267781573651346L;
	private int currentAgent = 0;
	
	public MultiagentDTP(Timepoint zero, DisjunctiveTemporalProblem decouplingDTP,	DisjunctiveTemporalProblem[] subDTPs, HashMap<String, Integer> timepoints,ArrayList<Timepoint> interfaceTimepoints) {
		super(zero, decouplingDTP, subDTPs, timepoints, interfaceTimepoints);
		subDTPLabel = "agent";
		connectingLabel = "interagentDTP";
	}
	
	@Override
	public void advanceToTime(int time, int deltaT, boolean pushSelection) {
		advanceToTime(time, true, deltaT, pushSelection);
	}	
	@Override
	public void advanceToTime(int time, boolean resolve, int deltaT, boolean pushSelection) {
//		System.out.println("Advancing system time for agent"+currentAgent+" by "+Generics.toTimeFormat(deltaT)+" to "+Generics.toTimeFormat(time));
		if(currentAgent == subDTPs.length){
			super.advanceToTime(time, resolve, deltaT, pushSelection);
		}
		else{
			selectionOrder.push(new SimpleEntry<Integer,Integer>(-1, deltaT));
			count++;
			advanceToTimeHelper(currentAgent, time, false, deltaT, pushSelection);
			cleanup(time,resolve,pushSelection);
		}
	}
	
	@Override
	public void executeAndAdvance(int timeS, String tpS, int timeE, String tpE, boolean resolve, int deltaT, boolean pushSelection){
		SimpleEntry<Integer,Integer> se = getIdxAndCheck(tpS, tpE); // returns a SE of the index of the subDTPs the timepoints belong to
		Integer idxS = se.getKey();
		Integer idxE = se.getValue();
		if(idxS != currentAgent && currentAgent != subDTPs.length){
			throw new IllegalArgumentException("Executing activity for the nonactive agent.");
		}
		selectionOrder.push(new SimpleEntry<Integer,Integer>(-1, deltaT));
		count++;
		subDTPs[idxS].executeAndAdvance(timeS, tpS, timeE, tpE, false, deltaT, false);
		//subDTPs[idxS].executeAndAdvance(timeS, tpS, timeE, tpE, false, deltaT, true);

		mustResolve[idxS] = true;
		cleanup(timeE, resolve, pushSelection);
	}
	
	@Override
	public int getMinTime(){
		if(currentAgent == subDTPs.length) return super.getMinTime();
		return subDTPs[currentAgent].getMinTime();
	}
	
	@Override
	public List<Integer> getMaxSlack(){
		//System.out.println("In MultiagentDTP getMaxslack");
		//System.out.println("Current Agent is " + currentAgent);
		int subdtpcount = 0;
		if(currentAgent == subDTPs.length){
			List<Integer> result = new ArrayList<Integer>(subDTPs.length);
			for(DisjunctiveTemporalProblem dtp: subDTPs){
				//System.out.println("SubDTP " + subdtpcount+ " has maxSlack " + dtp.getMaxSlack());
				result.add(dtp.getMaxSlack().get(0));
				subdtpcount++;
				
			}
			return result;
		}
		return subDTPs[currentAgent].getMaxSlack();
	}
	
	@Override
	public List<List<String>> getActivities(ActivityFinder af, int time){
		if(currentAgent == subDTPs.length){
			List<List<String>> result = new LinkedList<>();
			for(int i = 0 ; i < subDTPs.length; i++){
				result.add(subDTPs[i].getActivities(af, time).get(0));
			}
			return result;
		}
		return subDTPs[currentAgent].getActivities(af, time);
	}
	
	@Override
	public int getNumAgents(){
		return subDTPs.length;
	}
	
	@Override
	public int getCurrentAgent(){
		return currentAgent;
	}
	
	@Override
	public void setCurrentAgent(int agent){
		if(agent > subDTPs.length) throw new IllegalArgumentException("Max agent: "+subDTPs.length+"\treceived: "+agent);
		currentAgent = agent;
	}
	
	@Override
	public int getAgent(String tpS){
		Integer idx = timepoints.get(tpS);
		if(idx == null) return -1;
		else return idx;
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, int threshold){
		int currAgent = this.getCurrentAgent();
		
		DisjunctiveTemporalProblem currSubDTP = this.subDTPs[currAgent];
		DisjunctiveTemporalProblem prevSubDTP = ((MultiagentDTP) prevDTP).subDTPs[currAgent];
		
		return currSubDTP.getDeltas(prevSubDTP, threshold); 
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, DeltaFinder df){
		int currAgent = this.getCurrentAgent();
		
		DisjunctiveTemporalProblem currSubDTP = this.subDTPs[currAgent];
		DisjunctiveTemporalProblem prevSubDTP = ((MultiagentDTP) prevDTP).subDTPs[currAgent];
		
		return currSubDTP.getDeltas(prevSubDTP, df); 
	}
	
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, double relThreshold){
		int currAgent = this.getCurrentAgent();
		DisjunctiveTemporalProblem currSubDTP = this.subDTPs[currAgent];
		DisjunctiveTemporalProblem prevSubDTP = ((MultiagentDTP) prevDTP).subDTPs[currAgent];
		
		return currSubDTP.getDeltas(prevSubDTP, relThreshold);
	}
	
	@Override
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP){
		int currAgent = this.getCurrentAgent();
		
		DisjunctiveTemporalProblem currSubDTP = this.subDTPs[currAgent];
		DisjunctiveTemporalProblem prevSubDTP = ((MultiagentDTP) prevDTP).subDTPs[currAgent];
		
		return currSubDTP.getDeltas(prevSubDTP); 
	}
	
	public ArrayList<Delta> rankDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim){
		int currAgent = this.getCurrentAgent();
		
		DisjunctiveTemporalProblem currSubDTP = this.subDTPs[currAgent];
		DisjunctiveTemporalProblem prevSubDTP = ((MultiagentDTP) prevDTP).subDTPs[currAgent];
		
		return currSubDTP.rankDeltas(prevSubDTP, rankLim);
	}
	
	public ArrayList<Delta> rankRelativeDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim){
		int currAgent = this.getCurrentAgent();
		
		DisjunctiveTemporalProblem currSubDTP = this.subDTPs[currAgent];
		DisjunctiveTemporalProblem prevSubDTP = ((MultiagentDTP) prevDTP).subDTPs[currAgent];
		
		return currSubDTP.rankRelativeDeltas(prevSubDTP, rankLim);
	}
	            
	public MultiagentDTP clone() {
		MultiagentDTP newDTP;
		
		DisjunctiveTemporalProblem newcDTP = connectingDTP.clone();
		DisjunctiveTemporalProblem[] newSubDTPs = new DisjunctiveTemporalProblem[subDTPs.length];
		for(int i=0; i < subDTPs.length; i++){
			newSubDTPs[i] = subDTPs[i].clone();
		}

		ArrayList<Timepoint> newInterfaceTimepoints = new ArrayList<Timepoint>();
		for(Timepoint tp : interfaceTimepoints) newInterfaceTimepoints.add(tp.clone());
		newDTP = new MultiagentDTP(zero, newcDTP, newSubDTPs, timepoints, newInterfaceTimepoints);

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
		
		
		newDTP.count = count;
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
		
		ArrayList<DisjunctiveTemporalConstraint> newIncoming = new ArrayList<DisjunctiveTemporalConstraint>();
		for( DisjunctiveTemporalConstraint dtc : incomingConstraints) newIncoming.add(dtc.clone(hashedTimepoints));
		newDTP.incomingConstraints = newIncoming;
		newDTP.currentSubDTPSumCons = csdsc;
		newDTP.currentConnectingSumCons = ccsc;
		newDTP.mustResolve = mustResolve.clone();
		newDTP.reEmbed = reEmbed;
		
		newDTP.currentAgent = 0;
		newDTP.subDTPLabel = "agent";
		newDTP.connectingLabel = "interagentDTP";
		return newDTP;
	}
	
//	public MultiagentDTP clone() {
//		if(this == null) return null;
//		//System.out.println("Cloning a Multiagent DTP");
//		MultiagentDTP newDTP;
//		DisjunctiveTemporalProblem newcDTP = connectingDTP.clone();
//		DisjunctiveTemporalProblem[] newSubDTPs = new DisjunctiveTemporalProblem[subDTPs.length];
//		for(int i=0; i < subDTPs.length; i++){
//			newSubDTPs[i] = subDTPs[i].clone();
//		}
//		ArrayList<Timepoint> newAllTimepoints = new ArrayList<Timepoint>(allTimepoints);
//		ArrayList<Timepoint> newInterfaceTimepoints = new ArrayList<Timepoint>(interfaceTimepoints);
//		
//		newDTP = new MultiagentDTP(zero, newcDTP, newSubDTPs, (HashMap<String, Integer>) timepoints.clone(), newInterfaceTimepoints);
//		newDTP.allTimepoints = newAllTimepoints;
//		newDTP.count = count;
//		//System.out.println("Finished a Multiagent DTP");
//		return newDTP;
//	}
	
}
