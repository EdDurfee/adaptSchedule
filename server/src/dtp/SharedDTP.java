package dtp;


import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import interactionFramework.MappedPack;
import interval.IntervalSet;
import stp.Timepoint;


public class SharedDTP extends SimpleDTP {

	private int id;
	private int tpCount;
	
	public SharedDTP(int id, ArrayList<DisjunctiveTemporalConstraint> tempConstraints, ArrayList<Timepoint> localTimepoints,ArrayList<Timepoint> interfaceTimepoints, HashMap<String, Vector<MappedPack> > inputPairs) {
		super(id, tempConstraints, localTimepoints,interfaceTimepoints, inputPairs);
		this.id=id;		
		tpCount = 0;
		if(localTimepoints != null){
			for(Timepoint tp : localTimepoints){
				tp.setLocalIndex(id, tpCount++);
			}
		}
	}
	
	@Override
	public void addInterfaceTimepoint(Timepoint tp){
		if(timepoints.get(tp.name) == null){
			tp.setLocalIndex(id, tpCount++);
		}
		super.addInterfaceTimepoint(tp);
	}

	@Override
	public int getIndex(Timepoint tp) {
		return tp.getLocalIndex(id);
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public void printTimepointIntervals(PrintStream out, int time){
		int idx = 1;
		while(idx < minimalNetwork.length){
			if(time == Integer.MIN_VALUE || minimalNetwork[0][idx].intersect((int) time)){
				String s1 = isFixed(idx) ? "*" : "";
				String s2 = isFixed(idx+1) ? "*" : "";
				out.println(localTimepoints.get(idx).getName()+s1+": "+minimalNetwork[0][idx].inverse().toString()+"\t"+localTimepoints.get(idx+1).getName()+s2+": "+minimalNetwork[0][idx+1].inverse().toString()
						+"\tinterval: "+minimalNetwork[idx][idx+1].inverse().toString());
			}	
			idx += 2;
		}
	}


	@Override
	public void printTimepointIntervals(PrintStream out) {
		printTimepointIntervals(out,Integer.MIN_VALUE);
	}

	@Override
	public void addContingentTimepoint(Timepoint source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateValidity(DisjunctiveTemporalProblem currDTP, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IntervalSet getIntervalGlobal(String tp1, String tp2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<MappedPack> getRelatedInputPairs(String crucial) {
		// TODO Auto-generated method stub
		System.out.println("in getRelatedInputPairs in SharedDTP.java...shouldn't be here");

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
