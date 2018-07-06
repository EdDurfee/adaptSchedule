/* 
 * Copyright 2010 by The Regents of the University of Michigan
 *    For questions or permissions contact durfee@umich.edu
 * 
 * Code developed by James Boerkoel and Ed Durfee
 */

package stp;

import java.util.*;

import dtp.DisjunctiveTemporalConstraint;

import util.XMLParser;

public class TemporalDifference implements Comparable<TemporalDifference>, java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4121385513300585740L;
	public Timepoint source;
	public Timepoint destination;
	public int bound;
	
	private Edge edge;

	private DisjunctiveTemporalConstraint constraint;

	public TemporalDifference(Timepoint source, Timepoint destination, int bound){
		this.source=source;
		this.destination=destination;
		this.bound=bound;
	}

	public static TemporalDifference parseTemporalDifference(String xmlString, ArrayList<Timepoint> timepoints){
		String sourceStr = XMLParser.getTrimmedByTag(xmlString, "source");
		String destStr = XMLParser.getTrimmedByTag(xmlString, "destination");
		String boundStr = XMLParser.getTrimmedByTag(xmlString, "bound");

		Timepoint source=null;
		Timepoint destination=null;
		for(int i=0; i<timepoints.size(); i++){
			if(timepoints.get(i).getName().equals(sourceStr)){
				source = timepoints.get(i);
			}
			if(timepoints.get(i).getName().equals(destStr)){
				destination= timepoints.get(i);
			}
		}
		int bound = Integer.parseInt(boundStr);
		return new TemporalDifference(source,destination,bound);
	}
	
	public TemporalDifference negate(){
		TemporalDifference newTD = new TemporalDifference(destination, source, bound);
		return newTD;
	}
	

	/**
	 * A TD subsumes another if it implies the other TD.
	 * 
	 * A Temporal Difference (A - B < 5) subsumes (A - B < 10).  
	 * 
	 * @param oterConstraint
	 * @return
	 */
	public boolean subsumes(TemporalDifference otherDifference){
		if(!this.source.equals(otherDifference.source) && !this.destination.equals(otherDifference.destination)){
			return false;
		}
		if(this.bound <= otherDifference.bound) return true;
		return false;
	}

	public void prune(){

//		edge.prune(this);
		
		constraint.prune(this);
		
	}
	
	
	public void subsume(){
//		status = Status.SUBSUMED;
		constraint.assign(this);
	}
	
	public Edge getEdge() {
		return edge;
	}

	public void setEdge(Edge edge) {
		assert(this.edge == null || this.edge.equals(edge));
		this.edge = edge;
	}
	
	public boolean isExternal(){
		return (source.getAgent()!=destination.getAgent());
	}
	
	
	public DisjunctiveTemporalConstraint getConstraint() {
		return constraint;
	}

	public void setConstraint(DisjunctiveTemporalConstraint constraint) {
		this.constraint = constraint;
	}

	@Override
	public String toString(){
		if(source == null) return "SRC IS NULL for tp, but dest is: " + destination.toString();
		if(destination == null) return"DEST IS NULL but src is: " + source.toString();
		return source.toString()+" - "+destination.toString()+" <= "+bound;
//		return source.getAbsIndex()+" - "+destination.getAbsIndex()+" < "+bound;
	}

	public String toSMTString(){
		StringBuffer td = new StringBuffer();
		if(bound>=0){
			td.append("( <= ( - ");
			td.append(source.getName());
			td.append(' ');
			td.append(destination.getName());
			td.append(" ) ");
			td.append(bound);
			td.append(" ) ");
		}
		else{
			td.append("( >= ( - ");
			td.append(destination.getName());
			td.append(' ');
			td.append(source.getName());
			td.append(" ) ");
			td.append((0-bound));
			td.append(" ) ");
		}
		return td.toString();
	}

	public String toXMLString(){
		StringBuffer tempDiff = new StringBuffer();
		tempDiff.append('\n');
		tempDiff.append("<temporal_difference>");
		tempDiff.append('\n');
		tempDiff.append(XMLParser.INDENTATION);
		tempDiff.append("<source>");
		tempDiff.append(source.getName());
		tempDiff.append("</source>");
		tempDiff.append('\n');
		tempDiff.append(XMLParser.INDENTATION);
		tempDiff.append("<destination>");
		tempDiff.append(destination.getName());
		tempDiff.append("</destination>");
		tempDiff.append('\n');
		tempDiff.append(XMLParser.INDENTATION);
		tempDiff.append("<bound>");
		tempDiff.append(bound);
		tempDiff.append("</bound>");
		tempDiff.append('\n');
		tempDiff.append("</temporal_difference>");
		return tempDiff.toString();
	}

	public String toXMLString(String indentation){
		StringBuffer tempDiff = new StringBuffer();
		tempDiff.append('\n');
		tempDiff.append(indentation);
		tempDiff.append("<temporal_difference>");
		tempDiff.append('\n');
		tempDiff.append(indentation);
		tempDiff.append(XMLParser.INDENTATION);
		tempDiff.append("<source>");
		tempDiff.append(source.getName());
		tempDiff.append("</source>");
		tempDiff.append('\n');
		tempDiff.append(indentation);
		tempDiff.append(XMLParser.INDENTATION);
		tempDiff.append("<destination>");
		tempDiff.append(destination.getName());
		tempDiff.append("</destination>");
		tempDiff.append('\n');
		tempDiff.append(indentation);
		tempDiff.append(XMLParser.INDENTATION);
		tempDiff.append("<bound>");
		tempDiff.append(bound);
		tempDiff.append("</bound>");
		tempDiff.append('\n');
		tempDiff.append(indentation);
		tempDiff.append("</temporal_difference>");
		return tempDiff.toString();	
	}

	@Override
	public boolean equals(Object o){
		if(!(o instanceof TemporalDifference)) return false;
		TemporalDifference td = (TemporalDifference) o;
		if(!td.source.equals(this.source) || !td.destination.equals(this.destination) || td.bound != this.bound) return false;
		return true;
	}
	
	@Override
	public int compareTo(TemporalDifference other) {
		if(this.source.getAbsIndex() != other.source.getAbsIndex()){
			return (this.source.getAbsIndex() - other.source.getAbsIndex());
		}
		return (this.destination.getAbsIndex() - other.destination.getAbsIndex());
	}
	
	
	public TemporalDifference clone() {
		if(this == null) return null;
		//System.out.println("Cloning a Temporal Difference");
		TemporalDifference newTD;
		newTD = new TemporalDifference(source.clone(), destination.clone(), bound);
		// RE ADD ME??? newTD.constraint = dtc;
		//newTD.edge = edge.clone();
		//newTD.constraint = (DisjunctiveTemporalConstraint) constraint.clone();
		//System.out.println("Finished a Temporal Difference");
		return newTD;
	}
	
	public void updateSource(Timepoint new_source){
		this.source = new_source;
	}
	public void updateDestination(Timepoint new_dest){
		this.destination = new_dest;
	}
}
