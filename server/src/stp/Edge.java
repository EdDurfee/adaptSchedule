package stp;

import java.util.Vector;

import dtp.AgentDTP;

import util.TDSortedList;


public class Edge implements Comparable<Edge>, java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8361000962980839008L;
	private Node sourcePoint;
	private Node destinationPoint;
	private TDSortedList diffs;
	boolean enforceSingletonConsistency = false;
	
	/**
	 * This remains simply for backwards compability of previous algorithms
	 */
	
	private AgentDTP agent;  //Every edge is inherently "owned" by an agent 
	
	double bound;
	
	private Vector<Integer> adjacentTriangleIndices; // I believe these are redudant
	


	
	private Edge opposite;

	public Edge(Node source, Node destination, double bound){
		if(source.equals(destination)) throw new InvalidEdgeException("Edge must be between unique endpoints");
		this.sourcePoint = source;
		this.destinationPoint = destination;
		this.adjacentTriangleIndices = new Vector<Integer>();
		this.bound = bound;
		this.diffs = new TDSortedList();
	}
	
	public Edge(Node source, Node destination){
		this(source, destination,Double.POSITIVE_INFINITY);
	}

	public void setEnforceSingletonConsistency(boolean enforce){
		this.enforceSingletonConsistency=enforce;
	}
	
	public void enforceSingletonConsistency(){
		subsumeCheck();
		forwardCheck();
	}
	
	public void addAdjacentTriangleIndices(int triIndex){
		if(!adjacentTriangleIndices.contains(triIndex)){
			adjacentTriangleIndices.add(triIndex);
		}
	}

	public Vector<Integer> getAdjacentTriangleIndices(){
		return adjacentTriangleIndices;
	}
	
	public void addTemporalDifferent(TemporalDifference td){
		assert(sourcePoint.getId() == td.source.getAbsIndex());
		assert(destinationPoint.getId() == td.destination.getAbsIndex());
		
		td.setEdge(this);
//		td.setStatus(Status.ACTIVE);  Instead choose active as default edge status
		diffs.insert(td);
	}
	

	public void subsumeCheck(){
		while(diffs.size() >0 && bound <= diffs.getLast().bound){
			TemporalDifference td = diffs.removeLast();
//			System.out.println(this+" subsumes "+td);
			td.subsume(); //Subsume if it is not already!
		}
	}

	public boolean prune(TemporalDifference td){
		assert(sourcePoint.getId() == td.source.getAbsIndex());
		assert(destinationPoint.getId() == td.destination.getAbsIndex());
		return diffs.remove(td);
	}

	public void forwardCheck(){
		while(diffs.size() >0 && diffs.getFirst().bound + opposite.bound <0){
			TemporalDifference td = diffs.removeFirst();
//			System.out.println(opposite+" prunes "+td);
				td.prune();

		}
	}
	
	public Node getSourcePoint(){
		return sourcePoint;
	}
	
	public Node getDestinationPoint(){
		return destinationPoint;
	}
	
	
	/**
	 * Tightens the edge that leaves from sourceEndPoint, returns true if value is tighter than previous
	 * @param newBound
	 * @return
	 */
	public boolean tightenBound(double newBound){
		if(newBound < bound){
			bound = newBound;
			if(enforceSingletonConsistency){
				enforceSingletonConsistency();
			}
			return true;
		}
		return false;
	}
	
	/***
	 * Loosens the edge that leaves from sourceEndPoint, returns true is new value is looser than previous
	 * @param newBound
	 * @return boolean
	 */
	public boolean loosenBound(double newBound){
		if (newBound > bound){
			bound = newBound;
			if(enforceSingletonConsistency){
				enforceSingletonConsistency();
			}
			return true;
		}
		return false;
	}
	
	public void setAgent(AgentDTP agent){
		this.agent = agent;
		if(opposite!=null){
			opposite.agent = agent;
		}
	}
	
	public AgentDTP getAgent(){
		return this.agent;
	}

	/**
	 * Returns true if edge is self consistent (no negative cycle)
	 * @return
	 */
	public boolean isConsistent(){
		if(this.opposite == null){
			System.out.println("We want to avoid this!");
		}
		return (this.bound + this.opposite.bound>=0);
	}

	public double getBound(){
		return bound;
	}

	public Edge getOpposite(){
		return opposite;
	}
	
	public void setOpposite(Edge oppEdge){
		this.opposite = oppEdge;
		oppEdge.opposite = this;
		//If there is joint info, we can "share" copies of data structures, must make consistent!
		
	}
	
	public boolean contains(Node node){
		return (sourcePoint.equals(node) || destinationPoint.equals(node));
	}
	
	public String toString(){
		return "["+sourcePoint+" - "+destinationPoint+" < "+bound+"]";
	}
	
	public int compareTo(Edge otherEdge){
		if(sourcePoint.compareTo(otherEdge.sourcePoint)==0){
			return destinationPoint.compareTo(otherEdge.destinationPoint);
		}
		return sourcePoint.compareTo(otherEdge.sourcePoint);
	}
	
	public boolean equals(Edge otherEdge){
		return (this.compareTo(otherEdge)==0);
	}


	public class InvalidEdgeException extends RuntimeException {
		public InvalidEdgeException(String justification){
			super(justification);
		}
	}

	public Edge clone() {
		if(this == null) return null;
		//System.out.println("Cloning an Edge");
		Edge newEdge;
		double newBound = bound;
		newEdge = new Edge(sourcePoint.clone(), destinationPoint.clone(), newBound);
		newEdge.enforceSingletonConsistency = enforceSingletonConsistency;
		if(agent == null) newEdge.agent = null;
		else newEdge.agent = (AgentDTP) agent.clone();
		newEdge.adjacentTriangleIndices = (Vector<Integer>) adjacentTriangleIndices.clone();
		newEdge.diffs = (TDSortedList) diffs.clone();
		//System.out.println("Finished an Edge");
		return newEdge;
	}

}
