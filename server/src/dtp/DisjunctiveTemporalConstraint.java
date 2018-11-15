/* 
 * Copyright 2010 by The Regents of the University of Michigan
 *    For questions or permissions contact durfee@umich.edu
 * 
 * Code developed by James Boerkoel and Ed Durfee
 */

package dtp;

import interval.*;

import java.util.*;

import stp.TemporalDifference;
import stp.Timepoint;
import util.XMLParser;

public class DisjunctiveTemporalConstraint implements Comparable<DisjunctiveTemporalConstraint>, Iterable<TemporalDifference>, java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -608793911082778996L;
	private ArrayList<TemporalDifference> temporalDifferences;
	private int assignment=-1;
	
	public DisjunctiveTemporalConstraint(DisjunctiveTemporalConstraint dtc){
		this.assignment = dtc.assignment;
		this.temporalDifferences = new ArrayList<TemporalDifference>(dtc.temporalDifferences);
	}
	
	public DisjunctiveTemporalConstraint(ArrayList<TemporalDifference> tempDiffs){
		this.temporalDifferences = tempDiffs;
		Collections.sort(this.temporalDifferences);
		for(TemporalDifference td: temporalDifferences){
			td.setConstraint(this);//Set up pointer to this constraint
		}
	}
	
	public DisjunctiveTemporalConstraint(TemporalDifference tempDiff){
		this.temporalDifferences = new ArrayList<TemporalDifference>(1);
		this.temporalDifferences.add(tempDiff);
		for(TemporalDifference td: temporalDifferences){
			td.setConstraint(this);//Set up pointer to this constraint
		}
	}
	
	public void add(TemporalDifference td){
		this.temporalDifferences.add(td);
	}

	@Override
	public Iterator<TemporalDifference> iterator() {
		return this.temporalDifferences.iterator();
	}

	//intervalset = [0,10]v[30,100] goes to (d-s<=10 and s-d<=0) or (d-s<=100 and s-d <=30) 
	public static Collection<DisjunctiveTemporalConstraint> generateConstraint(Timepoint source, Timepoint dest, IntervalSet is){
		ArrayList<ArrayList<TemporalDifference>> diffs = new ArrayList<ArrayList<TemporalDifference>>();
		diffs.add(new ArrayList<TemporalDifference>()); //diffs.get(0) is lower bounds
		diffs.add(new ArrayList<TemporalDifference>()); //diffs.get(1) is upper bounds
		for(Interval i : is){
			diffs.get(0).add(new TemporalDifference(source,dest,(int)-i.getLowerBound()));
			diffs.get(1).add(new TemporalDifference(dest,source,(int)i.getUpperBound()));
		}
		
		return crossProduct(diffs);
	}
	
	/**
	 * @param diffs - vector of temporal differences to take the cross product of.  Assumes diffs[i].size() == diffs[j].size() for all i,j
	 */
	public static Collection<DisjunctiveTemporalConstraint> crossProduct(ArrayList<ArrayList<TemporalDifference>> diffs){	
		List<DisjunctiveTemporalConstraint> constraints = new LinkedList<DisjunctiveTemporalConstraint>();
		//System.out.println(diffs);
		
		// Error catching for when you've added a constraint that isn't actually valid.

		
		for(ArrayList<TemporalDifference> v : diffs){
			if(v.size() == 0){
				System.out.println(v);
				System.out.println(diffs);
				System.out.println("This is not a valid constraint to be added");
				throw new IllegalArgumentException("This is not a valid constraint to be added");
//				return constraints;
			}
			constraints.add(new DisjunctiveTemporalConstraint(v.get(0)));
		}
		for(int i = 1; i < diffs.get(0).size(); i++){
			int num = constraints.size();
			for(int j = 0; j < num; j++){
				for(int k = 0; k < diffs.size(); k++){
					DisjunctiveTemporalConstraint cons = new DisjunctiveTemporalConstraint(constraints.get(j));	
					if(diffs.get(k).get(i) != null){
						cons.add(diffs.get(k).get(i));
						constraints.add(cons);
					}
				}
			}
			constraints = constraints.subList(num, constraints.size());
		}
		return constraints;
	}

	public ArrayList<TemporalDifference> getTemporalDifferences() {
		return this.temporalDifferences;
	}
	
	public static DisjunctiveTemporalConstraint parseDisjunctiveTemporalConstraint(String xmlString,ArrayList<Timepoint> timepoints){
		ArrayList<TemporalDifference> diffs = new ArrayList<TemporalDifference>();		
		while(xmlString.length()>0){
			String trimmedString = XMLParser.getTrimmedByTag(xmlString, "temporal_difference");
			diffs.add(TemporalDifference.parseTemporalDifference(trimmedString, timepoints));
			xmlString=XMLParser.removeFirstTag(xmlString,"temporal_difference");
		}
		return new DisjunctiveTemporalConstraint(diffs);
	}

	public static ArrayList<DisjunctiveTemporalConstraint> parseDisjunctiveTemporalConstraints(String xmlString,ArrayList<Timepoint> timepoints){
		ArrayList<DisjunctiveTemporalConstraint> tempCons = new ArrayList<DisjunctiveTemporalConstraint>();
		int count = 0;
		while(xmlString.length()>0){
			String trimmedString = XMLParser.getTrimmedByTag(xmlString, "temporal_constraint");
			tempCons.add(parseDisjunctiveTemporalConstraint(trimmedString, timepoints));
			count++;
			xmlString=XMLParser.removeFirstTag(xmlString,"temporal_constraint");
		}
		System.out.println("HSP parsed "+count+" temporal constraints.");
		return tempCons;
	}
	
	public boolean assign(){
		for(int i=0; i<temporalDifferences.size(); i++){
			TemporalDifference tempDiff = temporalDifferences.get(i);
			if(tempDiff.source.isAssigned() //If source is assigned 
					&& tempDiff.destination.isAssigned() //and destination timepoint is assigned
					&& (tempDiff.source.getAssignedTime() - tempDiff.destination.getAssignedTime() <= tempDiff.bound)){ //And this assignment is consistent with the current temporal different
				assignment = i;
				return true;
			}
		}
		return false;
	}
	
	public void assign(TemporalDifference td){
//		System.out.println("assigning td: "+td+" of constraint: "+temporalDifferences);
		if(!temporalDifferences.contains(td)) return;  //a different td was already subsumed!
		for(int i=0; i<temporalDifferences.size(); i++){
			TemporalDifference tempDiff = temporalDifferences.get(i);
			if(!tempDiff.equals(td)){
				prune(tempDiff);
			}
		}
	}
	
	public static DisjunctiveTemporalConstraint generalize(DisjunctiveTemporalConstraint dtc1, DisjunctiveTemporalConstraint dtc2){
		ArrayList<TemporalDifference> diffs = new ArrayList<TemporalDifference>();
		for(int i=0; i<dtc1.temporalDifferences.size(); i++){
			diffs.add(new TemporalDifference(dtc1.get(i).source,dtc1.get(i).destination,Math.max(dtc1.get(i).bound,dtc2.get(i).bound)));
		}
		return new DisjunctiveTemporalConstraint(diffs);
	}
	
	/**
	 * Returns true if this subsumes other constraint.  It only subsumes otherConstraint if every TemporalDifference is subsumed.
	 * 
	 * A Temporal Difference (A - B < 5) subsumes (A - B < 10).  
	 * 
	 * @param oterConstraint
	 * @return
	 */
	public boolean subsumes(DisjunctiveTemporalConstraint otherConstraint){
		for(int i=0; i<this.temporalDifferences.size(); i++){
			if(!this.temporalDifferences.get(i).subsumes(otherConstraint.get(i))){
				return false;
			}
		}
		return true;
	}
	
	public void prune(TemporalDifference td){
//		System.out.println("pruning: "+td+" from constraint "+temporalDifferences);
		assert(temporalDifferences.contains(td));
		temporalDifferences.remove(td);
		
	}
	
	public void unassign(){
		assignment = -1;
	}

	public boolean isAssigned(){
		return assignment>=0;
	}
	
	public boolean isDisjunctive(){
		return temporalDifferences.size()>1;
	}
	
	public TemporalDifference get(int index){
		return temporalDifferences.get(index);
	}
	
	public TemporalDifference getAssigned(){
		if(!isAssigned()) return null;
		return temporalDifferences.get(assignment);
	}
	
	public int size(){
		return temporalDifferences.size();
	}

	public String toString() {
		return temporalDifferences.toString();
	}

	public String toSMTString(){
		StringBuffer dtc = new StringBuffer();
		dtc.append("(or ");
		for(int i=0; i<temporalDifferences.size(); i++){
			dtc.append(temporalDifferences.get(i).toSMTString());
		}
		dtc.append(") \n");	
		return dtc.toString();
	}

	public String toXMLString(){
		StringBuffer dtc = new StringBuffer();
		dtc.append('\n');
		dtc.append("<temporal_constraint>");
		for(int i=0;i<temporalDifferences.size(); i++){
			dtc.append(temporalDifferences.get(i).toXMLString(XMLParser.INDENTATION));
		}	
		dtc.append('\n');
		dtc.append("</temporal_constraint>");
		return dtc.toString();
	}

	public String toXMLString(String indentation){
		StringBuffer dtc = new StringBuffer();
		dtc.append('\n');
		dtc.append(indentation);
		dtc.append("<temporal_constraint>");
		for(int i=0;i<temporalDifferences.size(); i++){
			dtc.append(temporalDifferences.get(i).toXMLString(XMLParser.INDENTATION+indentation));
		}	
		dtc.append('\n');
		dtc.append(indentation);
		dtc.append("</temporal_constraint>");
		return dtc.toString();
	}

	public String toAssignedSMTString(){
		if(!isAssigned()) return "";
		StringBuffer dtc = new StringBuffer();
		dtc.append("(or ");
		dtc.append(temporalDifferences.get(assignment).toSMTString());
		dtc.append(") \n");	
		return dtc.toString();
	}

	public String toAssignedXMLString(){
		if(isAssigned())return "";
		StringBuffer dtc = new StringBuffer();
		dtc.append('\n');
		dtc.append("<temporal_constraint>");
		dtc.append(temporalDifferences.get(assignment).toXMLString(XMLParser.INDENTATION));
		dtc.append('\n');
		dtc.append("</temporal_constraint>");
		return dtc.toString();
	}

	public String toAssignedXMLString(String indentation){
		if(!isAssigned())return "";
		StringBuffer dtc = new StringBuffer();
		dtc.append('\n');
		dtc.append(indentation);
		dtc.append("<temporal_constraint>");
		dtc.append(temporalDifferences.get(assignment).toXMLString(XMLParser.INDENTATION+indentation));
		dtc.append('\n');
		dtc.append(indentation);
		dtc.append("</temporal_constraint>");
		return dtc.toString();
	}

	@Override
	public boolean equals(Object o){
		if(!(o instanceof DisjunctiveTemporalConstraint)) return false;
		DisjunctiveTemporalConstraint dtc = (DisjunctiveTemporalConstraint) o;
		if(dtc.temporalDifferences.size() != this.temporalDifferences.size() || dtc.assignment != this.assignment) return false;
		if(!this.temporalDifferences.containsAll(dtc.temporalDifferences)) return false;
		return true;
	}	

	@Override
	public int compareTo(DisjunctiveTemporalConstraint other) {
		if(this.temporalDifferences.size() != other.temporalDifferences.size()){
			return this.temporalDifferences.size() - other.temporalDifferences.size();
		}
		for(int i=0; i<this.temporalDifferences.size(); i++){
			int comparedVal = this.temporalDifferences.get(i).compareTo(other.temporalDifferences.get(i));
			if(comparedVal!=0) return comparedVal;
		}
		return 0;
	}
	
	public static ArrayList<DisjunctiveTemporalConstraint> equateTimepoints(Timepoint tp1, Timepoint tp2) {
		ArrayList<DisjunctiveTemporalConstraint> constraints = new ArrayList<DisjunctiveTemporalConstraint>();
		constraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(tp1, tp2, 0)));
		constraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(tp2, tp1, 0)));
		
		return constraints;
	}

	public static ArrayList<DisjunctiveTemporalConstraint> offsetTimepoints(Timepoint tp1, Timepoint tp2, int bound) {
		ArrayList<DisjunctiveTemporalConstraint> constraints = new ArrayList<DisjunctiveTemporalConstraint>();
		constraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(tp1, tp2, (0-bound))));
		constraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(tp2, tp1, bound)));
		
		return constraints;
	}

	public static ArrayList<DisjunctiveTemporalConstraint> boundTimepoints(Timepoint earlier, Timepoint later, int mingap, int maxgap) {
		ArrayList<DisjunctiveTemporalConstraint> constraints = new ArrayList<DisjunctiveTemporalConstraint>();
		constraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(earlier, later, (0-mingap))));
		constraints.add(new DisjunctiveTemporalConstraint(new TemporalDifference(later, earlier, maxgap)));
		
		return constraints;
	}
	
	public DisjunctiveTemporalConstraint clone(){
		DisjunctiveTemporalConstraint dtc;
		ArrayList<TemporalDifference> newTDs = new ArrayList<TemporalDifference>();
		
		for(TemporalDifference td : temporalDifferences){
			newTDs.add(td.clone());
		}
		dtc = new DisjunctiveTemporalConstraint(newTDs);
		return dtc;
	}
	
	public DisjunctiveTemporalConstraint clone(HashMap<String, Timepoint> newTimepoints) {
		if(this == null) {
			System.out.println("RETURNING NULL when cloning a dtc");
			return null;
		}
		//System.out.println("Cloning a DisjunctiveTemporalConstraint");
		DisjunctiveTemporalConstraint newDTC;
		ArrayList<TemporalDifference> newTDs = new ArrayList<TemporalDifference>();
		//System.out.println("In clone, Dtc: "+ this.toString());
		for (TemporalDifference td: temporalDifferences){
			if(td.source == null || td.destination == null){
				System.out.println("either source or dest is null. not cloning!");
				continue;
			}
			//System.out.println("cloning td: " + td.toString());
			TemporalDifference newTD = new TemporalDifference(newTimepoints.get(td.source.getName()), newTimepoints.get(td.destination.getName()), td.bound);
			newTDs.add(newTD);
		}
		newDTC = new DisjunctiveTemporalConstraint(newTDs);
		newDTC.assignment = assignment;
		//for(TemporalDifference td : temporalDifferences) td.setConstraint(newDTC);
		//for (TemporalDifference td : newDTC.temporalDifferences) td.setConstraint(newDTC);
		//System.out.println("Finished a DisjunctiveTemporalConstraint");
		return newDTC;
	}
}
