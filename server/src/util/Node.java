package util;

public class Node<T> {
//Implementation of a binary tree structure, essentially 
		private T data;
		private Node<T> parent;
		private Node<T> leftChild;
		private Node<T> rightChild;
		
		public Node(T nodeData, Node<T> parentNode){
			data = nodeData;
			parent = parentNode;
			leftChild = null;
			rightChild = null;
			
		}
		public Node(T nodeData, Node<T> parentNode, Node<T> left, Node<T> right){
			data = nodeData;
			parent = parentNode;
			leftChild = left;
			rightChild = right;
		}
		
		public Node(T nodeData){
			data = nodeData;
			parent = null;
			leftChild = null;
			rightChild = null;
		}
		
		public void setLeftChild(Node<T> childNode){
			leftChild = childNode;
		}
	
		public void setRightChild(Node<T> childNode){
			rightChild = childNode;
		}
		
		public void setChildren(Node<T> left, Node<T> right){
			leftChild = left;
			rightChild = right;
		}
		
		//returns true if the node has no parent. 
		public boolean isRoot(){
			return parent == null;
		}
		
		public Node<T> getLeftChild(){
			return leftChild;
		}
		public Node<T> getRightChild() {
			return rightChild;
		}
		public Node<T> getParent() {
			return parent;
		}
		
		public T getNodeData(){
			return data;
		}
}
