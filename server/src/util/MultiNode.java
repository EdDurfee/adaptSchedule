package util;

import java.util.ArrayList;

public class MultiNode<T> {
	
			public T data;
			private MultiNode<T> parent;
			private ArrayList<MultiNode<T>> children;
			private String label;
			private boolean explored = false;
			
			public MultiNode(T nodeData, MultiNode<T> parentNode){
				data = nodeData;
				parent = parentNode;
				children = null;
				label = "";
				
			}
			public MultiNode(T nodeData, MultiNode<T> parentNode, ArrayList<MultiNode<T>> childrenNode){
				data = nodeData;
				parent = parentNode;
				children = childrenNode;
				label = "";
			}
			
			public MultiNode(T nodeData, MultiNode<T> parentNode, ArrayList<MultiNode<T>> childrenNode, String lab){
				data = nodeData;
				parent = parentNode;
				children = childrenNode;
				label = lab;
			}
			public MultiNode(T nodeData){
				data = nodeData;
				parent = null;
				children = null;
				label = "";
			}
				
			public void setChildren(ArrayList<MultiNode<T>> childlist){
				children = childlist;
			}
			
			public void setLabel(String lab){
				label = lab;
			}
			
			public void addChild(MultiNode<T> newchild){
				if(children == null){
					children = new ArrayList<MultiNode<T>>();
				}
				children.add(newchild);
			}
			
			//returns true if the node has no parent. 
			public boolean isRoot(){
				return parent == null;
			}
			
			public ArrayList<MultiNode<T>> getChildren(){
				return children;
			}
		
			public MultiNode<T> getParent() {
				return parent;
			}
			
			public T getNodeData(){
				return data;
			}
			
			public ArrayList<String> getChildrenLabels(){
				ArrayList<String> ret = new ArrayList<String>();
				if(children == null || children.size() == 0){
					return ret;
				}else{
					for(MultiNode<T> child : children){
						ret.add(child.label);
					}
					return ret;
				}
				
			}
			
			public String getLabel(){
				return this.label;
			}
			
			public boolean isExplored(){
				return explored;
			
			}
			
			public void markExplored(){
				explored = true;
			}
	
}

