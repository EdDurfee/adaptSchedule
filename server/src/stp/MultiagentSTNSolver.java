package stp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;

import dtp.*;

public class MultiagentSTNSolver{



	public STN temporalNetwork;

	boolean batchUpdates = true;
	boolean updateNow = true;


	public static int idCounter=0;



	private int numCompTriangles = 0;
	private int numAgents=0;
	private int numTimepointsPerAgent=0;
	int[] eliminationOrder;
	public int[] agentOfNode;
	int[][] agentEliminationOrder; 
	public int[] currentAgentComputation ;
	public long[] currentAgentComputationTime ;
	int[] latestAgentComputation;
	//	Vector<Edge>[] updatedEdges;

	int[] sharedEliminationOrder;
	int[] sharedEliminationOrderAgent;
	int numSharedTimepoints;

	Vector<Node>[] agentTimepoints;
	boolean[][] privateTP;
	SimpleDTP[] agentProblems;
	SimpleDTP centralizedDTP;
	boolean[] agentTriangulated;
	Vector<Node> nodes;
	HashSet<Edge> edges;
	HashSet<Edge> interAgentEdges;



	private int numMessageCycles = 0;
	private int numFillEdges = 0;
	private int nccc = 0;
	private int tcc= 0;
	private int totalMessages = 0;


	public int getTotalMessages() {
		return totalMessages;
	}


	Node zero;


	public static int HUNSBERGER = 0;
	public static int DECOUPLED = 1;
	public static int P3C = 2;
	int solveMethod;


	public MultiagentSTNSolver(SimpleDTP[] agentProblems,  SimpleDTP centralized,Timepoint zero, int solveMethod){
		super();

		numAgents = agentProblems.length;
		numTimepointsPerAgent = agentProblems[0].getNumLocalTimepoints()-1;
//		if(zero!=null) numTimepointsPerAgent--;

		this.agentProblems = agentProblems;
		this.centralizedDTP = centralized;

		//Convert TPs to graph nodes:

		this.nodes = new Vector<Node>();

		this.agentTimepoints = new Vector[numAgents];
		for(int i=0; i< agentProblems.length;i++) agentTimepoints[i] = new Vector<Node>();
		this.privateTP = new boolean[numAgents][numTimepointsPerAgent];
		for(Timepoint tp:centralizedDTP.getTimepoints()){

			Node tpNode = new Node(tp.getAbsIndex());
//			System.out.println("tp: "+tp);
			if(tp.equals(zero)){
				this.zero = tpNode;
				//Add to all agents?
				//				for(int i=0; i<numAgents; i++) agentTimepoints[i].add(tpNode);
			}
			else{
				int agentId = tp.getAgent();
				tpNode.setAgent((AgentDTP) agentProblems[agentId]);
				agentTimepoints[agentId].add(tpNode);
				if(agentProblems[agentId].isLocalTimepointPrivate(tp)){
					if(tp.getLocalIndex(agentId) >= privateTP[agentId].length)
					{
						System.out.println("about to be array out of bounds execptions...");
//						System.out.println("reported agent 0 num: "+agentProblems[0].numLocalTimepoints());
						System.out.println("reported num: "+numTimepointsPerAgent);
						System.out.println("curr Index : "+tp.getLocalIndex(agentId));
					}
					if(agentId >= privateTP.length){
						System.out.println("about to be AGENT array out of bounds execptions...");
					}
//					System.out.println("local index: "+tp.getLocalIndex(agentId)+" of agent: "+agentId);
					privateTP[agentId][tp.getLocalIndex(agentId)] = true;
				}	
			}
			nodes.add(tpNode);
		}





		this.agentOfNode = new int[numAgents*numTimepointsPerAgent+1];


		this.solveMethod = solveMethod;
		//		this.interAgentEdges = problem.interAgentEdges;


	
			temporalNetwork = new STN(nodes.size());
	

		//Now build edges!
		edges = new HashSet<Edge>();
		interAgentEdges = new HashSet<Edge>();
		for(DisjunctiveTemporalConstraint constraint : centralized.getTempConstraints()){
			if(!constraint.isDisjunctive()){  //If non disjunctive, incorporate the bound!
				temporalNetwork.tighten(constraint.get(0).source.getAbsIndex(), constraint.get(0).destination.getAbsIndex(), constraint.get(0).bound, false);
			}
			for(int i=0; i<constraint.size();i++){
				TemporalDifference td = constraint.get(i);
				Edge edge = temporalNetwork.addEdge(td.source.getAbsIndex(), td.destination.getAbsIndex());
				edge.setEnforceSingletonConsistency(true);
				edge.addTemporalDifferent(td);
				edges.add(edge);
				if(td.isExternal()) interAgentEdges.add(edge);
			}
		}
		makeSingletonConsistent();


		agentEliminationOrder = new int[agentTimepoints.length][agentTimepoints[0].size()];
		currentAgentComputation = new int[numAgents];
		currentAgentComputationTime = new long[numAgents];
		latestAgentComputation = new int[numAgents];
		agentTriangulated = new  boolean[numAgents];
		eliminationOrder = new int[numAgents*numTimepointsPerAgent+1];




		Collections.sort(this.nodes);
		for(int i=0; i<numAgents; i++){
			Collections.sort(agentTimepoints[i]);
		}
	}

	private void makeSingletonConsistent() {
		for(Edge edge: edges){
			edge.enforceSingletonConsistency();
		}
	}

	public boolean setup(){
			updatedEdges = new Vector[numAgents*numTimepointsPerAgent+1];
			for(int i=0; i<numAgents*numTimepointsPerAgent+1;i++) updatedEdges[i] = new Vector<Edge>();
			return icapsTriangulate(); 
	}

	

	public void teardown(){
			distP3C2();
	}



	



	private boolean decrementK(int aIndex, int[] agentK){
		agentK[aIndex]--;
		for(; agentK[aIndex] >= 0 && sharedEliminationOrderAgent[agentK[aIndex]]!=aIndex; agentK[aIndex]--);
		return (agentK[aIndex]<0);
	}

	private boolean decrementJ(int aIndex, int[] agentK, int[] agentJ){
		//FIND NEXT SHARED EDGE
		agentJ[aIndex]--;
		for(; (agentJ[aIndex] > agentK[aIndex]) && (!temporalNetwork.edgeNetwork[sharedEliminationOrder[agentK[aIndex]]][sharedEliminationOrder[agentJ[aIndex]]]); agentJ[aIndex]--);
		//NOW FIND NEXT EXTERNAL EDGE!
		//		for(; 	(agentJ[aIndex] <= agentK[aIndex]) && 
		//				(agentJ[aIndex] >= 0) && 
		//				(!temporalNetwork.edgeNetwork[sharedEliminationOrder[agentK[aIndex]]][sharedEliminationOrder[agentJ[aIndex]]]) &&
		//				(aIndex==sharedEliminationOrderAgent[agentJ[aIndex]]); agentJ[aIndex]--);
		//		return (agentJ[aIndex]<0);
		return agentJ[aIndex] <= agentK[aIndex];
	}


	private boolean incrementK(int aIndex, int[] agentK){
		agentK[aIndex]++;
		for(; agentK[aIndex] < numSharedTimepoints && sharedEliminationOrderAgent[agentK[aIndex]]!=aIndex; agentK[aIndex]++);
		return (agentK[aIndex]>=numSharedTimepoints);
	}

	private boolean incrementJ(int aIndex, int[] agentK, int[] agentJ){
		//FIND NEXT EXTERNAL EDGE
		agentJ[aIndex]++;
		for(; (agentJ[aIndex] < numSharedTimepoints) && 
				((!temporalNetwork.edgeNetwork[sharedEliminationOrder[agentK[aIndex]]][sharedEliminationOrder[agentJ[aIndex]]]) ||
						(aIndex==sharedEliminationOrderAgent[agentJ[aIndex]]))
						; agentJ[aIndex]++);
		//NOW FIND NEXT EXTERNAL EDGE!
		//		for(; 	(agentJ[aIndex] <= agentK[aIndex]) && 
		//				(agentJ[aIndex] >= 0) && 
		//				(!temporalNetwork.edgeNetwork[sharedEliminationOrder[agentK[aIndex]]][sharedEliminationOrder[agentJ[aIndex]]]) &&
		//				(aIndex==sharedEliminationOrderAgent[agentJ[aIndex]]); agentJ[aIndex]--);
		//		return (agentJ[aIndex]<0);
		return agentJ[aIndex] >= numSharedTimepoints;
	}


	public int getNonConcurrentConstraintChecks(){
		return nccc;
	}

	public int getMessageCycles(){
		return numMessageCycles;
	}

	public int getNumFillEdges(){
		return numFillEdges;
	}

	public int getTotalConstraintChecks(){
		return tcc;
	}

	public int getNumTriangles(){
		return numCompTriangles;
	}

	public double getExtectedAgentContraintChecks(){
		double ecc = 0.0;
		for(int i=0; i<latestAgentComputation.length; i++){
			ecc+= (((double)latestAgentComputation[i])/((double)latestAgentComputation.length));
		}
		return ecc;
	}


	public boolean icapsTriangulate(){
		for(int i=0;i<numAgents;i++){
//			System.out.println(i+" - agentTPS: "+agentTimepoints[i]);
			for(int j=0;j<numTimepointsPerAgent;j++){
				agentTimepoints[i].get(j).setEliminationIndex(-1);
			}
		}

		boolean[][] edgeNetwork = new boolean[temporalNetwork.edgeNetwork.length][temporalNetwork.edgeNetwork[0].length];
		for(int i=0; i<edgeNetwork.length; i++){
			for(int j=0;j<edgeNetwork[i].length;j++){
				//				edgeNetwork[i][j] = temporalNetwork.edgeNetwork[i][j]; //Make a DEEP copy!
				if(temporalNetwork.temporalNetwork[i][j]!=null){
//					edgeNetwork[i][j] = temporalNetwork.temporalNetwork[i][j].exists();
					edgeNetwork[i][j] = temporalNetwork.edgeNetwork[i][j];
				}
				assert(edgeNetwork[i][j] == temporalNetwork.hasEdge(i, j));
			}
		}


		ArrayList<Integer>[] privateNodes = new ArrayList[numAgents];
		ArrayList<Integer>[] sharedNodes = new ArrayList[numAgents];

		for(int i=0; i<numAgents; i++){
			privateNodes[i] = new ArrayList<Integer>();
			sharedNodes[i] = new ArrayList<Integer>();
			for(int j=0; j<numTimepointsPerAgent;j++){
				if(privateTP[i][j]){
					privateNodes[i].add(agentTimepoints[i].get(j).getId());
				}
				else{
					sharedNodes[i].add(agentTimepoints[i].get(j).getId());
					numSharedTimepoints++;
				}
				agentOfNode[agentTimepoints[i].get(j).getId()]=i;
			}
		}

		int eliminationIndex = 0;

		int[] agentEliminationOrderIndex = new int[numAgents];
		sharedEliminationOrder = new int[numSharedTimepoints];
		sharedEliminationOrderAgent = new int[numSharedTimepoints];
		int sharedEliminationIndex = 0;

		int minComp = Integer.MAX_VALUE;
		do {
			//Keep choosing min computation node, select next, eliminate
			int agent = -1;
			minComp = Integer.MAX_VALUE;
			for(int i=0; i<numAgents;i++){
				if(!agentTriangulated[i]){
					int newComp = currentAgentComputation[i]+compOfNextNode(privateNodes[i],sharedNodes[i],edgeNetwork);
					if (newComp <minComp){
						minComp = newComp;
						agent = i;
					}
				}
			}
			if(minComp<Integer.MAX_VALUE){
				int node = -1;
				long beforeTimeInNanos = System.nanoTime();
				if(privateNodes[agent].size()>0){
					node = nextNode(privateNodes[agent],edgeNetwork);
					privateNodes[agent].remove(privateNodes[agent].indexOf(node)); 
				}
				else{
					node = nextNode(sharedNodes[agent],edgeNetwork);
					sharedNodes[agent].remove(sharedNodes[agent].indexOf(node));
					sharedEliminationOrderAgent[sharedEliminationIndex] = agent;
					//					System.out.println("eliminated: "+node+" at time :"+ sharedEliminationIndex);
					sharedEliminationOrder[sharedEliminationIndex++] = node;
				}
				
				agentEliminationOrder[agent][agentEliminationOrderIndex[agent]++] = eliminationIndex;
				eliminationOrder[eliminationIndex++] = node;
				
				int numTriangles = marryParents(node,edgeNetwork);
				long afterTimeInNanos = System.nanoTime();
				if(numTriangles<0){
					currentAgentComputation[agent] -= numTriangles;
					return false;
				}
				currentAgentComputation[agent] += numTriangles;
				currentAgentComputationTime[agent] += (afterTimeInNanos-beforeTimeInNanos);
				tcc+=numTriangles;
				if(privateNodes[agent].size()+sharedNodes[agent].size() <=0){
					agentTriangulated[agent] = true;
				}
			}


		}while(minComp<Integer.MAX_VALUE);
		eliminationOrder[eliminationIndex++] = zero.getId();
		temporalNetwork.eliminationOrder = eliminationOrder;
		return true;
	}

	public int getMostConstrainedNodeIndex(){
		if(eliminationOrder[eliminationOrder.length-1]==zero.getId()){
			return eliminationOrder[eliminationOrder.length-1];
		}
		return eliminationOrder[eliminationOrder.length-2];
	}

	public int[] getEliminationOrder(){
		return eliminationOrder;
	}
	

	public int marryParents(int node, boolean[][] edgeNetwork){
		int numTriangles=0; 
		boolean consistent = true;

		for(int i=0; i<temporalNetwork.edgeNetwork.length;i++){
			if(!edgeNetwork[node][i]) continue;
			edgeNetwork[node][i]=false;
			edgeNetwork[i][node]=false;

			if(agentOfNode[node]!=agentOfNode[i]  && i!=zero.getId()) totalMessages++;

			for(int j=i+1; j< edgeNetwork.length;j++){
				if(!edgeNetwork[node][j]) continue;
				numTriangles+=2;
				numCompTriangles++;
				if(!edgeNetwork[i][j]) numFillEdges++;
				edgeNetwork[i][j] = true;
				edgeNetwork[j][i] = true;
				double newBound = temporalNetwork.getBound(i, node)+temporalNetwork.getBound(node, j);
				temporalNetwork.tighten(i, j, newBound, false, true);
				if(updatedEdges!=null) updatedEdges[node].add(temporalNetwork.getEdge(i, j));
				double newOppBound = temporalNetwork.getBound(j, node)+temporalNetwork.getBound(node, i);
				temporalNetwork.tighten(j, i, newOppBound, false, true);
				if(newBound+newOppBound < 0) consistent = false;
				if(updatedEdges!=null)				updatedEdges[node].add(temporalNetwork.getEdge(j,i));
				temporalNetwork.getEdge(i,j).addAdjacentTriangleIndices(node);
				temporalNetwork.getEdge(j,i).addAdjacentTriangleIndices(node);
			}
		}
		if(!consistent) return (0-numTriangles);
		return numTriangles;
	}

	Vector<Edge>[] updatedEdges;
	public void distP3C2(){
		boolean complete = false;
		for(;!complete;nccc++){
			complete = true;
			int messages = 0;
			for(int aIndex = 0; aIndex<numAgents; aIndex++){
				if(currentAgentComputation[aIndex]<=nccc){ // Only update an agent that is 
					//Find node to update
					int agentIndexToUpdate = numTimepointsPerAgent-1;
					for(; agentIndexToUpdate>=0 && updatedEdges[eliminationOrder[agentEliminationOrder[aIndex][agentIndexToUpdate]]].size()<=0; agentIndexToUpdate--);
					if(agentIndexToUpdate>=0){//We found node to update
						latestAgentComputation[aIndex]= nccc;
						complete=false;
						int totalIndexToUpdate = agentEliminationOrder[aIndex][agentIndexToUpdate];
						int nodeToUpdate = eliminationOrder[totalIndexToUpdate];
						Edge edgeToUpdate = updatedEdges[nodeToUpdate].remove(0);
						long beforeTimeInNanos = System.nanoTime();
						messages = Math.max(messages,processUpdate(edgeToUpdate, nodeToUpdate, aIndex, totalIndexToUpdate));
						long afterTimeInNanos = System.nanoTime();
						currentAgentComputationTime[aIndex] += (afterTimeInNanos-beforeTimeInNanos);
						
					}
				}
				else if(complete==true){
					//Discover if any other updates still exist
					for(int i=numTimepointsPerAgent-1; complete && i>=0; i--){
						if(updatedEdges[agentEliminationOrder[aIndex][i]].size()>0) complete=false;
					}
				}
			}
			if(messages >2){
				System.out.println("check into this dude...");
			}
			numMessageCycles+=messages;
		}
		nccc--;//We will have overcounted by 1
	}

	public long getRuntimeInNanos(){
		long maxTime = currentAgentComputationTime[0];
		for(int i=1; i<numAgents; i++){
			if(currentAgentComputationTime[i]>maxTime){
				maxTime = currentAgentComputationTime[i];
			}
		}
		return maxTime;
	}
	
	public int processUpdate(Edge sourceEdge, int node, int agent, int totalIndex){
		int messages = 0;



		int fromIndex= sourceEdge.getSourcePoint().getId();
		int toIndex= sourceEdge.getDestinationPoint().getId();
		double bound = temporalNetwork.getBound(fromIndex, toIndex);


		//check to see if this edge results in any tightening of future edges, 
		//if so send message if needed, and add to updated edges
		double newBound = bound + temporalNetwork.getBound(toIndex, node);
		double oldBound = temporalNetwork.getBound(fromIndex, node);

		boolean messageSent = false;
		tcc++;
		if(newBound < oldBound){
			temporalNetwork.tighten(fromIndex, node, newBound, false, false);
			Edge tightenedEdge = temporalNetwork.getEdge(fromIndex, node);
			//			for(int elimIndex = totalIndex-1; elimIndex>=0; elimIndex--){
			//				int nodeToConsider = eliminationOrder[elimIndex];
			for (Integer nodeToConsider: tightenedEdge.getAdjacentTriangleIndices()) {
				//Does this node correspond to a triangle involving updated edge?
				//				if(temporalNetwork.hasEdge(nodeToConsider, node)&&temporalNetwork.hasEdge(nodeToConsider, fromIndex)){
				//Add edge if it isnt already there!
				if(!updatedEdges[nodeToConsider].contains(tightenedEdge)) updatedEdges[nodeToConsider].add(tightenedEdge);
				//If this node belongs to a diff agent, we need to send a message!
				if(agent!=agentOfNode[nodeToConsider]){
					messageSent = true;
					totalMessages++;
				}
				//				}
			}
		}
		if(messageSent) messages++;
		messageSent=false;
		newBound = temporalNetwork.getBound(node, fromIndex)+bound;
		oldBound = temporalNetwork.getBound(node, toIndex);
		tcc++;
		if(newBound<oldBound){
			temporalNetwork.tighten(node, toIndex, newBound, false, false);
			Edge tightenedEdge = temporalNetwork.getEdge(node, toIndex);
			//			for(int elimIndex = totalIndex-1; elimIndex>=0; elimIndex--){
			//				int nodeToConsider = eliminationOrder[elimIndex];
			for (Integer nodeToConsider: tightenedEdge.getAdjacentTriangleIndices()) {
				//Does this node correspond to a triangle involving updated edge?
				//				if(temporalNetwork.hasEdge(nodeToConsider, node)&&temporalNetwork.hasEdge(nodeToConsider, toIndex)){
				//Add edge if it isnt already there!
				if(!updatedEdges[nodeToConsider].contains(tightenedEdge)) updatedEdges[nodeToConsider].add(tightenedEdge);
				//If this node belongs to a diff agent, we need to send a message!
				if(agent!=agentOfNode[nodeToConsider]){
					messageSent = true;
					totalMessages++;
				}
				//				}
			}
		}
		currentAgentComputation[agent]+=2;
		if(messageSent) messages++;
		return messages;
	}

	private int compOfNextNode(ArrayList<Integer> privateNodes, ArrayList<Integer> sharedNodes, boolean edgeNetwork[][]){
		ArrayList<Integer> nodes;
		if(privateNodes.size()>0){
			nodes = privateNodes;
		}
		else if(sharedNodes.size()>0){
			nodes = sharedNodes;
		}
		else{
			return 0;
		}
		int minNode = nodes.get(0);
		int minFill = numFillEdges(minNode,edgeNetwork);
		if(minFill==0) return numTriangles(minNode, edgeNetwork);
		for(int i=1; i<nodes.size(); i++){
			int otherFill = numFillEdges(nodes.get(i),edgeNetwork);
			if(minFill> otherFill){
				minNode = nodes.get(i);
				minFill = otherFill;
				if(minFill==0) return numTriangles(minNode, edgeNetwork);
			}
		}
		return numTriangles(minNode, edgeNetwork);
	}

	private int numTriangles(int index, boolean edgeNetwork[][]){
		int numTriangles= 0;
		for(int i=0; i<edgeNetwork.length;i++){
			if(!edgeNetwork[index][i]) continue;//No i->node edge , continue
			for(int j=i+1; j<edgeNetwork.length; j++){
				if(!edgeNetwork[index][j]) continue;//No j->node edge, continue
				numTriangles+=2;
			}
		}
		return numTriangles;
	}

	private int nextNode(ArrayList<Integer> nodes, boolean edgeNetwork[][]){
		int minNode = nodes.get(0);
		int minFill = numFillEdges(minNode,edgeNetwork);
		if(minFill==0) return minNode;
		for(int i=1; i<nodes.size(); i++){
			int otherFill = numFillEdges(nodes.get(i),edgeNetwork);
			if(minFill> otherFill){
				minNode = nodes.get(i);
				minFill = otherFill;
				if(minFill==0) return minNode;
			}
		}
		return minNode;
	}

	private int numFillEdges(int index, boolean edgeNetwork[][]){
		int numFillEdges = 0;
		for(int i=0; i<edgeNetwork.length;i++){
			if(!edgeNetwork[index][i]) continue;//No i->node edge , continue
			for(int j=i+1; j<edgeNetwork.length; j++){
				if(!edgeNetwork[index][j]) continue;//No j->node edge, continue
				if(!edgeNetwork[i][j]){//No edge exists, so count it!
					numFillEdges++;
				}
			}
		}
		return numFillEdges;
	}


	public double getWeight(Timepoint source, Timepoint dest){
		return temporalNetwork.getBound(source.getAbsIndex(), dest.getAbsIndex());
	}

	public Edge getEdge(Timepoint source, Timepoint dest){
		return temporalNetwork.getEdge(source.getAbsIndex(),dest.getAbsIndex());
	}

	public boolean hasEdge(Timepoint source, Timepoint dest){
		return  temporalNetwork.hasEdge(source.getAbsIndex(),dest.getAbsIndex()) && temporalNetwork.hasEdge(dest.getAbsIndex(),source.getAbsIndex()) ;
	}

	public boolean update(){
		return temporalNetwork.update();
	}

	public void destroy(){
		for(int i=0; i<temporalNetwork.temporalNetwork.length; i++){
			for(int j=0; j<temporalNetwork.temporalNetwork.length; j++){
				temporalNetwork.temporalNetwork[i][j]=null;
			}
		}
		nodes = null;
		System.gc();
	}

	public void printTemporalNetwork(){
		System.out.println(temporalNetwork);
	}




}
