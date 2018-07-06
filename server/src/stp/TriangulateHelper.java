package stp;

import java.util.ArrayList;

import dtp.AgentDTP;


public class TriangulateHelper {

	public static int  numNewFillEdges=0;
	

	public static int sharedTriangles = 0;
	public static int privateTriangles = 0;
	
	public static int numTriangles=0; //Do we need this?

	//	public static boolean[] isExternal;


	public static int[]/*ArrayList<Integer>*/ triangulate(boolean[][] edgeMatrix){
		if(edgeMatrix.length<=0) return new int[0];
		int[] eliminationOrder = new int[edgeMatrix.length];
		boolean[][] edgeNetwork = new boolean[edgeMatrix.length][edgeMatrix[0].length];
		for(int i=0; i<edgeMatrix.length; i++){
			for(int j=0;j<edgeMatrix[i].length;j++){
				edgeNetwork[i][j] = edgeMatrix[i][j]; //Make a DEEP copy!
			}
		}
		//		ArrayList<Integer> triangles = new ArrayList<Integer>();

		ArrayList<Integer> nodes = new ArrayList<Integer>(edgeNetwork.length);
		for(int i=0; i<edgeMatrix.length;i++) nodes.add(i);
		int counter=0;
		while(nodes.size()>0){
			int node = nextNode(nodes,edgeNetwork);
			eliminationOrder[counter++] = node;
			nodes.remove(nodes.indexOf(node));

			for(int i=0; i<edgeMatrix.length;i++){
				if(!edgeNetwork[node][i]) continue;
				edgeNetwork[node][i]=false;
				edgeNetwork[i][node]=false;

				for(int j=i+1; j< edgeNetwork.length;j++){
					if(!edgeNetwork[node][j]) continue;
					if(!edgeNetwork[i][j]) numNewFillEdges++;
					edgeNetwork[i][j] = true;
					edgeNetwork[j][i] = true;
					edgeMatrix[i][j] = true;
					edgeMatrix[j][i] = true;


//					if(NewEmpiricalResultsGenerator.version == NewEmpiricalResultsGenerator.CENTRALIZED){
//						if(STN.graphNodes[node].isExternalEdge()&&STN.graphNodes[i].isExternalEdge()&&STN.graphNodes[j].isExternalEdge()){
//							sharedTriangles++;							
//						}
//						else{
//							privateTriangles++;
//						}	
//					}



					//					ArrayList<Integer> triangle = new ArrayList<Integer>();
					//					triangle.add(i);
					//					triangle.add(j);
					//					triangle.add((-1 - Collections.binarySearch(triangle, node)),node);
					//					int val = 0;
					//					for(int t = 0; t<3; t++){
					//						val = val*edgeNetwork.length + triangle.get(t);
					//					}
					//					triangles.add(val);
				}
			}
		}
		//		return triangles;
		return eliminationOrder;
	}

	public static int[]/*ArrayList<Integer>*/ triangulate(boolean[][] edgeMatrix, ArrayList<Integer> nodes,AgentDTP agent){
		if(edgeMatrix.length<=0) return new int[0];
		int[] eliminationOrder = new int[edgeMatrix.length];
		int index = nodes.size();
		for(int i=0;i<eliminationOrder.length;i++){
			if(!nodes.contains(i)){
				eliminationOrder[index++] = i;
			}
		}


		boolean[][] edgeNetwork = new boolean[edgeMatrix.length][edgeMatrix[0].length];
		for(int i=0; i<edgeMatrix.length; i++){
			for(int j=0;j<edgeMatrix[i].length;j++){
				edgeNetwork[i][j] = edgeMatrix[i][j]; //Make a DEEP copy!
			}
		}
		//		ArrayList<Integer> triangles = new ArrayList<Integer>();

		int counter=0;
		while(nodes.size()>0){
			int node = nextNode(nodes,edgeNetwork);
			eliminationOrder[counter++] = node;
			nodes.remove(nodes.indexOf(node));

			for(int i=0; i<edgeMatrix.length;i++){
				if(!edgeNetwork[node][i]) continue;
				edgeNetwork[node][i]=false;
				edgeNetwork[i][node]=false;

				for(int j=i+1; j< edgeNetwork.length;j++){
					if(!edgeNetwork[node][j]) continue;
					if(!edgeNetwork[i][j]) numNewFillEdges++;
					edgeNetwork[i][j] = true;
					edgeNetwork[j][i] = true;
					edgeMatrix[i][j] = true;
					edgeMatrix[j][i] = true;
					numTriangles++;
				}
			}
		}
		//		return triangles;
		return eliminationOrder;
	}

	public static int[]/*ArrayList<Integer>*/ triangulateWithOrder(boolean[][] edgeMatrix){
		if(edgeMatrix.length<=0) return new int[0];
		int[] eliminationOrder = new int[edgeMatrix.length];


		boolean[][] edgeNetwork = new boolean[edgeMatrix.length][edgeMatrix[0].length];
		for(int i=0; i<edgeMatrix.length; i++){
			for(int j=0;j<edgeMatrix[i].length;j++){
				edgeNetwork[i][j] = edgeMatrix[i][j]; //Make a DEEP copy!
			}
		}
		//		ArrayList<Integer> triangles = new ArrayList<Integer>();

		int counter=0;
		for(int loop = 0; loop<eliminationOrder.length; loop++){
			int node = loop;
			eliminationOrder[counter++] = node;

			for(int i=0; i<edgeMatrix.length;i++){
				if(!edgeNetwork[node][i]) continue;
				edgeNetwork[node][i]=false;
				edgeNetwork[i][node]=false;

				for(int j=i+1; j< edgeNetwork.length;j++){
					if(!edgeNetwork[node][j]) continue;
					if(!edgeNetwork[i][j]) numNewFillEdges++;
					edgeNetwork[i][j] = true;
					edgeNetwork[j][i] = true;
					edgeMatrix[i][j] = true;
					edgeMatrix[j][i] = true;


				}
			}
		}
		//		return triangles;
		return eliminationOrder;
	}




	private static int nextNode(ArrayList<Integer> nodes, boolean edgeNetwork[][]){
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

	private static int numFillEdges(int index, boolean edgeNetwork[][]){
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
	
}
