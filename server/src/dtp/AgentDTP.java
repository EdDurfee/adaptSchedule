package dtp;


import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import interactionFramework.MappedPack;
import interval.IntervalSet;
import stp.Timepoint;

public class AgentDTP extends SimpleDTP implements java.io.Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6264211912243791467L;
	private int tpCount;
	
	public AgentDTP(int id, ArrayList<DisjunctiveTemporalConstraint> tempConstraints, ArrayList<Timepoint> localTimepoints,ArrayList<Timepoint> interfaceTimepoints, HashMap<String, Vector<MappedPack>> localInputPairs){
		super(id, tempConstraints, localTimepoints, interfaceTimepoints, localInputPairs);
	}
	
	public AgentDTP(int i,
			ArrayList<DisjunctiveTemporalConstraint> localConstraints,
			ArrayList<Timepoint> localTimepoints,
			ArrayList<Timepoint> interfaceTimepoints,
			HashMap<String, Vector<MappedPack>> hashMap,
			HashMap<SimpleEntry<String, String>, Integer> precTest) {
		// TODO Auto-generated constructor stub
		super(i, localConstraints, localTimepoints, interfaceTimepoints, hashMap, precTest);
	}

	@Override
	public void addInterfaceTimepoint(Timepoint tp){
		if(timepoints.get(tp.name) == null){
			tp.setLocalIndex(id, tpCount++);
		}
		super.addInterfaceTimepoint(tp);
	}
	
	@Override
	public void printTimepointIntervals(PrintStream out){
		printTimepointIntervals(out,Integer.MIN_VALUE);
	}
	@Override
	public void printTimepointIntervals(PrintStream out, int time){
		int idx = 1;            // ED: Assumes the zero reference point is index 0 I believe
		while(idx < minimalNetwork.length){
			if(time == Integer.MIN_VALUE || minimalNetwork[0][idx].intersect((int) time)){
//				System.out.println("The first of the pair is "+localTimepoints.get(idx).getName());
//				System.out.println("The second of the pair is "+localTimepoints.get(idx+1).getName());
				String s1 = isFixed(idx) ? "*" : "";
				String s2 = isFixed(idx+1) ? "*" : "";
				out.println(localTimepoints.get(idx).getName()+s1+": "+minimalNetwork[0][idx].inverse().toString()+"\t"+localTimepoints.get(idx+1).getName()+s2+": "+minimalNetwork[0][idx+1].inverse().toString()
						+"\tinterval: "+minimalNetwork[idx][idx+1].inverse().toString());
			}	
			idx += 2;
		}
	}

	@Override
	public void addContingentTimepoint(Timepoint source) {
		super.addContingentTimepoint(source);
		
	}

	@Override
	public void updateValidity(DisjunctiveTemporalProblem currDTP, int i) {
		currDTP.updateValidity(i);
		
	}

	@Override
	public void updateInternalData() {
		super.updateInternalData();
		
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
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
	

