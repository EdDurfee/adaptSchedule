/* 
 * Copyright 2010 by The Regents of the University of Michigan
 *    For questions or permissions contact durfee@umich.edu
 * 
 * Code developed by James Boerkoel and Ed Durfee
 */

package stp;

import java.util.ArrayList;
import java.util.Vector;

import util.XMLParser;

public class Timepoint implements Comparable<Timepoint>, java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8967065106734808938L;
	public String name;
	private int time =Integer.MIN_VALUE;
	private boolean scheduled =false;
	private ArrayList<Integer> localIndices = new ArrayList<Integer>();
	private int absoluteIndex;
	public static int absIndexCounter=0;
	private int agent;
	
	private boolean referencePoint = false;
	
	
	public Timepoint(String name){
//		System.out.println("Making timepoint for "+name);
		this.name = name;
		this.absoluteIndex = absIndexCounter++;
		//System.out.println("Making timepoint with absolute index "+ absoluteIndex);
	}
	// constructor used only for cloning. Creates a timepoint with a given absolute index. 
	public Timepoint(String name, int absoluteIndex, int agent, ArrayList<Integer> localIndices){
		this.name = name;
		this.absoluteIndex = absoluteIndex;
		this.localIndices = localIndices;
		this.agent = agent;
		
	}
	
	public int getAgent(){
		return agent;
	}
	
	public void setAgent(int agent){
		this.agent = agent;
	}
	
	public Timepoint(String name, int numAgents){
//		System.out.println("Making timepoint for "+name);
		this(name);
		this.localIndices.ensureCapacity(numAgents);
		for(int i = 0; i < numAgents; i++){
			this.localIndices.add(-1);
		}
	}
	
	public Timepoint(String name,int numAgents, boolean reference){
		this(name, numAgents);
		this.referencePoint = reference;
	}
	
	public boolean isReference(){
		return referencePoint;
	}
		
	public void setReference(boolean val){
		this.referencePoint = true;
	}
		
	public String getName(){
		return name;
	}
	
	public int getLocalIndex(int agentId){
		if(agentId > localIndices.size()){
			System.out.println("AgentID is: " + agentId + " but len localIndices is: " + localIndices + " when timepoint is " + this.name);
		}
		return localIndices.get(agentId);
		
	}
	
	public int getAbsIndex(){
		return absoluteIndex;
	}
		
	public static Vector<Timepoint> parseTimepoints(String xmlString){
		Vector<Timepoint> vars = new Vector<Timepoint>();
		int count = 0;
		while(XMLParser.containsTag(xmlString, "timepoint")){
			String trimmedString = XMLParser.getTrimmedByTag(xmlString, "timepoint");
			vars.add(new Timepoint(trimmedString));
			count++;
			xmlString=XMLParser.removeFirstTag(xmlString,"timepoint");
		}
		//Check for a reference timepoint
//		if(XMLParser.containsTag(xmlString, "reference_point")){
//			String trimmedString = XMLParser.getTrimmedByTag(xmlString, "reference_point");
//			vars.add(new Timepoint(trimmedString,true));
//			count++;
//			xmlString=XMLParser.removeFirstTag(xmlString,"reference_point");
//		}
		System.out.println("HSP parsed "+count+" temporal variables.");
		return vars;
	}
	
	public void assign(int time){
		this.time = time;
		this.scheduled = true;
	}
	
	public int getTime(){
		return time;
	}
	
	public void unassign(){
		this.time = Integer.MIN_VALUE;
		this.scheduled = false;
	}
	
	public boolean isAssigned(){
		return scheduled;
	}
	
	public int getAssignedTime(){
		return time;
	}
	
	@Override 
	public boolean equals(Object o){
		if(!(o instanceof Timepoint)) return false;
		Timepoint t = (Timepoint) o;
		if(!t.name.equals(this.name) || t.scheduled != this.scheduled || t.absoluteIndex != this.absoluteIndex || t.agent != this.agent
				|| t.time != this.time || t.localIndices.size() != this.localIndices.size()) return false;
		for(int i = 0; i < t.localIndices.size(); i++){
			if(t.localIndices.get(i) != this.localIndices.get(i)) return false;
		}
		return true;
	}
	
	@Override
	public int hashCode(){
		int hash = this.name.hashCode();
		hash = ((hash << 5)+hash) + this.absoluteIndex;
		return hash;
	}

	@Override
	public int compareTo(Timepoint t) {
		return this.name.compareTo(t.name);
	}
	
	@Override
	public String toString(){
		return name;
	}
	
	public String toSMTString(){
		return ":extrafuns (("+name+" Int )) \n";
	}
	
	public String toXMLString(){
		StringBuffer timepoint = new StringBuffer();
		timepoint.append('\n');
		if(referencePoint){
			timepoint.append("<reference_point>");	
		}
		else{
			timepoint.append("<timepoint>");
		}
		timepoint.append(name);
		if(referencePoint){
			timepoint.append("</reference_point>");	
		}
		else{
			timepoint.append("</timepoint>");
		}
		return timepoint.toString();
	}
	public String toXMLString(String indentation){
		StringBuffer timepoint = new StringBuffer();
		timepoint.append('\n');
		timepoint.append(indentation);
		if(referencePoint){
			timepoint.append("<reference_point>");	
		}
		else{
			timepoint.append("<timepoint>");
		}
		timepoint.append(name);
		if(referencePoint){
			timepoint.append("</reference_point>");	
		}
		else{
			timepoint.append("</timepoint>");
		}
		return timepoint.toString();
	}

	public void setLocalIndex(int agentID, int localIndex) {
		int count = agentID-localIndices.size()+1;
		for(int i = 0; i <= count; i++){
			localIndices.add(-1);
		}
		this.localIndices.set(agentID, localIndex);
	}
	
	public Timepoint clone(){
		if(this == null) {
			System.out.println("RETURNING NULL when cloning a timepoint");
			return null;
		}
		//System.out.println("Cloning a Timepoint");
		Timepoint newTP;
		newTP = new Timepoint(name, absoluteIndex, agent, localIndices);
		newTP.scheduled = scheduled;
		newTP.referencePoint = referencePoint;
		newTP.time = time;
		//System.out.println("Finished a Timepoint");
		//System.out.println("Original Timepoint assigned is "+this.isAssigned());
		//System.out.println("Cloned Timepoints assigned is "+newTP.isAssigned());
		
		return newTP;
	}
	
	public void changeName(String new_name){
		this.name = new_name;
	}
	
}
