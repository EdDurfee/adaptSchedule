package stp;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import dtp.AgentDTP;



public class Node implements Comparable<Node>, java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 534951826625109255L;
	public int agentId;
	private AgentDTP agent;
	private int id;
	private int eliminationIndex=-1;
	private int stpIndex=-1;
	private boolean zero;
	
	private HashSet<Edge> neighbors;
	

	private static int idCounter=0;
	
	public Node(){
		this.id = idCounter++;
	}
	
	
	public Node(int id){
		this.id = id;
		idCounter = Math.max(id+1,idCounter);
	}	
	
	public void setAgent(AgentDTP agent){
		this.agent = agent;
		this.agentId = agent.getID();
	}
	
	public int getAgentID(){
		return this.agentId;
	}
	
	
	public String toString(){
			return Integer.toString(id);
		}
	
	@Override
	public int compareTo(Node otherNode) {
		// TODO Auto-generated method stub
		return this.id - otherNode.id;
	}
	
	public boolean equals(Node otherNode){
		return (this.compareTo(otherNode)==0);
	}
	
	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}
	

	public int getEliminationIndex() {
		return eliminationIndex;
	}


	public void setEliminationIndex(int eliminationIndex) {
		this.eliminationIndex = eliminationIndex;
	}


	public int getStpIndex() {
		return stpIndex;
	}


	public void setStpIndex(int stpIndex) {
		this.stpIndex = stpIndex;
	}
	
	public boolean isZero() {
		return zero;
	}


	public void setZero(boolean zero) {
		this.zero = zero;
	}
	
	public Set<Edge> getNeighbors(){
		return neighbors;
	}
	
	public void addNeighbor(Edge neighbor){
		neighbors.add(neighbor);
	}

	public Node clone() {
		if(this == null) return null;
		//System.out.println("Cloning a Node");
		Node newNode;
		HashSet<Edge> newNeighbors = new HashSet<Edge>();
		newNode = new Node(id);
		
		if(agent == null) newNode.agent = null;
		else newNode.setAgent((AgentDTP) agent.clone());
		
		if(neighbors == null) newNode.neighbors = null;
		else{
			for (Edge e : neighbors){
				newNeighbors.add((Edge) e.clone());
			}
			newNode.neighbors = newNeighbors;
		}
		newNode.eliminationIndex = eliminationIndex;
		newNode.stpIndex = stpIndex;
		newNode.zero = zero;
		//System.out.println("finished a Node");
		return  newNode;
	}
	
}
