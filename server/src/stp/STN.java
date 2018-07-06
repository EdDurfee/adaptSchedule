/* 
 * Copyright 2010 by The Regents of the University of Michigan
 *    For questions or permissions contact durfee@umich.edu
 * 
 * Code developed by James Boerkoel and Ed Durfee
 */

package stp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class STN implements java.io.Serializable{
		/**
	 * 
	 */
	private static final long serialVersionUID = -4121964380244147433L;
		public Edge[][] temporalNetwork;
		public boolean[][] edgeNetwork;
		public int[] eliminationOrder;

		public Node[] graphNodes;

		private int solveMethod=0;

		public static final int FW=0;
		public static final int P3C=1;
		public static final int DPC=2;


		public static int constraintChecks=0;



		public STN(int size){
			this(size,0);
		}

		public STN(int size, int solveMethod){
			this.solveMethod=solveMethod;
			temporalNetwork = new Edge[size][size];
			edgeNetwork = new boolean[size][size];

			graphNodes=new Node[size];
			for(int i=0; i<size; i++){
				graphNodes[i] = new Node(i);
				graphNodes[i].setStpIndex(i);
			}

			for(int i=0; i<size; i++){
				for(int j=i+1; j<size; j++){
					temporalNetwork[i][j] = new Edge(graphNodes[i],graphNodes[j]);
					temporalNetwork[j][i] = new Edge(graphNodes[j],graphNodes[i]); //Creates the opposite edge
					temporalNetwork[i][j].setOpposite(temporalNetwork[j][i]);
				}
			}

		}

		public STN(List<Node> nodes, List<Edge> edges, int solveMethod){
			this(nodes,edges,solveMethod,false);
		}
		
		
		public STN(List<Node> nodes, int solveMethod){
			this(nodes,new Vector<Edge>(),solveMethod,false);
		}

		public STN(List<Node> nodes, List<Edge> edges, int solveMethod, boolean duplicateEdges){
			this.solveMethod = solveMethod;
			temporalNetwork = new Edge[nodes.size()][nodes.size()];
			edgeNetwork = new boolean[nodes.size()][nodes.size()];

			graphNodes=new Node[nodes.size()];
			Collections.sort(nodes);
			for(int i=0; i<nodes.size(); i++){
				graphNodes[i] = nodes.get(i);
				graphNodes[i].setStpIndex(i);
			}

			for(Edge edge: edges){
				if(!duplicateEdges){
					temporalNetwork[edge.getSourcePoint().getStpIndex()][edge.getDestinationPoint().getStpIndex()] = edge;
					if(edge.getOpposite()!=null){
						temporalNetwork[edge.getDestinationPoint().getStpIndex()][edge.getSourcePoint().getStpIndex()] = edge.getOpposite();	
					}
				}
				else {
					temporalNetwork[edge.getSourcePoint().getStpIndex()][edge.getDestinationPoint().getStpIndex()] = new Edge(edge.getSourcePoint(),edge.getDestinationPoint(),edge.getBound());
					if(edge.getOpposite()!=null){
						temporalNetwork[edge.getDestinationPoint().getStpIndex()][edge.getSourcePoint().getStpIndex()] = new Edge(edge.getDestinationPoint(),edge.getSourcePoint(),edge.getOpposite().getBound());	
					}
				}


				edgeNetwork[edge.getSourcePoint().getStpIndex()][edge.getDestinationPoint().getStpIndex()] = true;
				edgeNetwork[edge.getDestinationPoint().getStpIndex()][edge.getSourcePoint().getStpIndex()] = true;
			}

			for(int i=0; i<nodes.size(); i++){
				for(int j=i+1; j<nodes.size(); j++){
					if(edgeNetwork[i][j] || solveMethod==FW){
						if(temporalNetwork[i][j]==null) temporalNetwork[i][j] = new Edge(graphNodes[i],graphNodes[j]);
						if(temporalNetwork[j][i]==null) temporalNetwork[j][i] = new Edge(graphNodes[j],graphNodes[i]);
						temporalNetwork[i][j].setOpposite(temporalNetwork[j][i]);
					}
				}
			}
			if(solveMethod==P3C){
				triangulate();
			}

		}
		

		public void triangulate(){
			eliminationOrder = TriangulateHelper.triangulate(edgeNetwork);

			for(int i=0; i<graphNodes.length; i++){
				for(int j=i+1; j<graphNodes.length; j++){
					if(edgeNetwork[i][j] || solveMethod==FW){
						if(temporalNetwork[i][j]==null) temporalNetwork[i][j] = new Edge(graphNodes[i],graphNodes[j]);
						if(temporalNetwork[j][i]==null) temporalNetwork[j][i] = new Edge(graphNodes[j],graphNodes[i]);
						temporalNetwork[i][j].setOpposite(temporalNetwork[j][i]);
					}

				}
			}
		}

		public void triangulateWithOrder(){
			eliminationOrder = TriangulateHelper.triangulateWithOrder(edgeNetwork);

			for(int i=0; i<graphNodes.length; i++){
				for(int j=i+1; j<graphNodes.length; j++){
					if(edgeNetwork[i][j] || solveMethod==FW){
						if(temporalNetwork[i][j]==null) temporalNetwork[i][j] = new Edge(graphNodes[i],graphNodes[j]);
						if(temporalNetwork[j][i]==null) temporalNetwork[j][i] = new Edge(graphNodes[j],graphNodes[i]);
						temporalNetwork[i][j].setOpposite(temporalNetwork[j][i]);
					}

				}
			}
		}



		//	
		private STN(STN toCopy){
			temporalNetwork = new Edge[toCopy.temporalNetwork.length][toCopy.temporalNetwork.length];
			for(int i=0; i<toCopy.temporalNetwork.length; i++){
				System.arraycopy(toCopy.temporalNetwork[i],0,temporalNetwork[i],0,temporalNetwork.length);
			}
			solveMethod = toCopy.solveMethod;
		}

		public STN copy(){ //make a copy of this efficiently...
			return new STN(this);
		}
		
		public STN copy(boolean duplicate, int solveMethod){
			if(duplicate==false){
				return new STN(this);
			}
			Vector<Edge> allEdges = new Vector<Edge>();
			for(Edge[] edgeArray: temporalNetwork){
				allEdges.addAll(Arrays.asList(edgeArray));
			}
			return new STN(new Vector<Node>(Arrays.asList(graphNodes)),new Vector<Edge>(),solveMethod,duplicate);
		}

		private boolean updated=false;


		public double getBound(int fromIndex, int toIndex){
			if(fromIndex == -1 || toIndex == -1){
				return 0;
			}
			if(fromIndex==toIndex) return 0;
			//System.out.println(temporalNetwork[fromIndex][toIndex].toString());
			if(temporalNetwork[fromIndex][toIndex]==null) {
				System.out.println("getBound is returning positive infinity because TN[from][to] is undefined");
				System.out.println("from: " + fromIndex + " to: " + toIndex);
				return Double.POSITIVE_INFINITY;
			}
			return temporalNetwork[fromIndex][toIndex].getBound();
		}
		
		public boolean isInfinite(int fromIndex, int toIndex){
			if(fromIndex==toIndex) return false;
			if(temporalNetwork[fromIndex][toIndex]==null || temporalNetwork[fromIndex][toIndex].getBound()==Double.POSITIVE_INFINITY) return true;
			return false;
		}

		public Edge getEdge(int fromIndex, int toIndex){
			return temporalNetwork[fromIndex][toIndex];
		}

		public boolean hasEdge(int fromIndex, int toIndex){
			return edgeNetwork[fromIndex][toIndex];
		}

		public boolean tighten(int fromIndex, int toIndex, double bound, boolean update){
			return tighten(fromIndex, toIndex, bound, update,false);
		}

		public Edge addEdge(int fromIndex, int toIndex){
			edgeNetwork[fromIndex][toIndex] = true;
			edgeNetwork[toIndex][fromIndex] = true;
			if(temporalNetwork[fromIndex][toIndex]==null) {
				temporalNetwork[fromIndex][toIndex] = new Edge(graphNodes[fromIndex],graphNodes[toIndex]);
			}
			if(temporalNetwork[toIndex][fromIndex]==null) {
				temporalNetwork[toIndex][fromIndex] = new Edge(graphNodes[toIndex],graphNodes[fromIndex]);
			}
			temporalNetwork[toIndex][fromIndex].setOpposite(temporalNetwork[fromIndex][toIndex]);
			return temporalNetwork[fromIndex][toIndex];
		}
		
		
		public boolean tighten(int fromIndex, int toIndex, double bound, boolean update, boolean addEdge){
	//		System.out.println("fromIndex is " + fromIndex +" and to Index is "+toIndex);
			if(addEdge){
				addEdge(fromIndex,toIndex);
			}
			constraintChecks++;
			if(getBound(toIndex, fromIndex)+bound < 0){  //Quick consistency check
				return false;
			}
			else if(getBound(fromIndex, toIndex) > bound){//If any change is made at all
				//System.out.println("from ind: "+fromIndex +" and to index "+ toIndex);
				temporalNetwork[fromIndex][toIndex].tightenBound(bound);
				//Try new update procedure... any cheaper?
				if(solveMethod==FW && update){
					ArrayList<Integer> setI = new ArrayList<Integer>();
					ArrayList<Integer> setJ = new ArrayList<Integer>();
					for(int i=0; i<temporalNetwork.length;i++){
						if(i==toIndex || i==fromIndex) continue;
						constraintChecks++;
						if(temporalNetwork[i][toIndex].tightenBound(getBound(i,fromIndex)+bound)){
							setI.add(i);
						}
						constraintChecks++;
						if(temporalNetwork[fromIndex][i].tightenBound(getBound(toIndex,i)+bound)){
							setJ.add(i);
						}
					}
					for(int i=0; i<setI.size(); i++){
						for(int j=0; j<setJ.size(); j++){
							if(setI.get(i).intValue()==setJ.get(j).intValue()) continue;
							constraintChecks++;
							temporalNetwork[setI.get(i)][setJ.get(j)].tightenBound(getBound(setI.get(i),fromIndex)+getBound(fromIndex, setJ.get(j)));
						}
					}
					return true;
				}
				else if(update){
					updated=true;
					return update();
				}
			}
			return true;
		}
		
		public boolean loosen(int fromIndex, int toIndex, double bound, boolean update, boolean addEdge){
			if(addEdge){
				addEdge(fromIndex, toIndex);
			}
			constraintChecks++;
			
			return true;
		}


		public boolean isUpdated(){
			return updated;
		}

		public void clearUpdated(){
			updated=false;
		}

		public boolean subsumes(int fromIndex, int toIndex, int bound){
			if(getBound(fromIndex,toIndex)< bound){  //temporal network already implies a tighter bound
				return true;
			}
			return false;
		}
		
		
		public boolean floydWarshall(){
			for( int k=0; k<temporalNetwork.length;k++){
				for(int i=0; i<temporalNetwork.length; i++){
					if(i==k) continue;
					if(getEdge(i,k).getBound()==Double.POSITIVE_INFINITY){
						constraintChecks++;
						continue;
					}
					for(int j=0; j<temporalNetwork.length;j++){
						if(i==j || k==j) continue;
						if(getEdge(k,j).getBound()==Double.POSITIVE_INFINITY){
							constraintChecks++;
							continue;
						}
						double sum = temporalNetwork[i][k].getBound()+temporalNetwork[k][j].getBound();
						constraintChecks++;
						if(temporalNetwork[i][j].tightenBound(sum)){
//							if(!temporalNetwork[i][j].isConsistent()){
								return false;
//							}
						}
					}
				}
			}
			return true;
		}



		public boolean DPC(){
			for(int kIndex=0; kIndex<eliminationOrder.length; kIndex++){
				int k = eliminationOrder[kIndex];
				for(int iIndex = kIndex+1; iIndex < eliminationOrder.length; iIndex++){
					int i = eliminationOrder[iIndex];
					if(!edgeNetwork[i][k] || temporalNetwork[i][k].getBound()>=Double.POSITIVE_INFINITY) continue;
					for(int jIndex=kIndex+1; jIndex < eliminationOrder.length; jIndex++){
						int j = eliminationOrder[jIndex];
						if(!edgeNetwork[i][j]||!edgeNetwork[k][j] || iIndex==jIndex || temporalNetwork[k][j].getBound()>=Double.POSITIVE_INFINITY) continue;
						double sum = temporalNetwork[i][k].getBound()+temporalNetwork[k][j].getBound();
						constraintChecks++;
						if(temporalNetwork[i][j].tightenBound(sum)){
//							if(!temporalNetwork[i][j].isConsistent()){
								return false;
//							}
						}
					}
				}
			}
			return true;
		}

		public boolean P3CPhase2() {
			for(int kIndex=eliminationOrder.length-1; kIndex>=0; kIndex--){
				int k = eliminationOrder[kIndex];
				for(int iIndex = kIndex+1; iIndex < eliminationOrder.length; iIndex++){
					int i = eliminationOrder[iIndex];
					if(!edgeNetwork[i][k] /*|| temporalNetwork[i][k].getBound()>=Double.POSITIVE_INFINITY*/) continue;
					for(int jIndex=kIndex+1; jIndex < eliminationOrder.length; jIndex++){
						int j = eliminationOrder[jIndex];
						if(!edgeNetwork[i][j]||!edgeNetwork[k][j] || iIndex==jIndex /*|| temporalNetwork[i][k].getBound()>=Double.POSITIVE_INFINITY/*temporalNetwork[i][j].getLastUpdate()<choicePoint*/) continue;
						constraintChecks++;
						double sum = temporalNetwork[i][j].getBound()+temporalNetwork[j][k].getBound();
						if(temporalNetwork[i][k].tightenBound(sum)){
							return false;
						}
						constraintChecks++;
						sum = temporalNetwork[k][i].getBound()+temporalNetwork[i][j].getBound();
						if(temporalNetwork[k][j].tightenBound(sum)){
							return false;
						}
					}
				}
			}
			return true;
		}

		public boolean P3C() {
			if(!DPC()) return false;
			return P3CPhase2();
		}

		public int size(){
			return temporalNetwork.length;
		}



		public boolean update(){//Updates the STN

			if(solveMethod==FW){
				return floydWarshall();
			}
			else if(solveMethod==P3C){
				return P3C();
			}
			else if(solveMethod==DPC){
				return DPC();
			}
			else {
				throw new RuntimeException("Not implemented");
			}
		}


		public String toString(){
			String currentString = "";
			for(int i=0; i< temporalNetwork.length;i++){
				for(int j=0; j<temporalNetwork[i].length; j++){
					currentString += getBound(i,j)+",";
				}
				currentString+="\n";
			}
			currentString+="\n";
			return currentString;
		}

	public void printSchedule(){
		for(int i=0; i<temporalNetwork.length;i++){
			System.out.println(i+": "+(0-getBound(0,i))+" - "+getBound(i,0));
		}
	}
	
	public STN clone() {
		if(this == null) return null;
		STN newSTN;
		int newsize = this.temporalNetwork.length;
		newSTN = new STN(newsize, solveMethod);
		Edge[][] newTN = new Edge[newsize][newsize];
		for(int i=0; i < newsize; i++){
			for(int j=i+1; j < newsize; j++){
				newTN[i][j] = temporalNetwork[i][j].clone();
			}
		}
		boolean[][] newEN = new boolean[newsize][newsize];
		for(int i=0; i < newsize; i++){
			for(int j=0; j < newsize; j++){
				newEN[i][j] = edgeNetwork[i][j];
			}
		}
		if(eliminationOrder == null) newSTN.eliminationOrder = null;
		else{
			int[] newEO = new int[eliminationOrder.length];
			for(int k = 0; k < eliminationOrder.length; k++){
				newEO[k] = eliminationOrder[k];
			}
			newSTN.eliminationOrder = newEO;
		}
		Node[] newGN = new Node[newsize];
		for(int i = 0; i < newsize; i++){
			newGN[i] = graphNodes[i].clone();
		}
		
		newSTN.temporalNetwork = newTN;
		newSTN.edgeNetwork = newEN;
		newSTN.graphNodes = newGN;
		
		return newSTN;
	}
}

