package util;

import java.util.Collections;
import java.util.LinkedList;

import stp.TemporalDifference;

public class TDSortedList extends LinkedList<TemporalDifference> {
	
	public boolean add(TemporalDifference td){
		return insert(td);
	}
	
	public boolean insert(TemporalDifference td){
		
		int index = Collections.binarySearch(this, td);
		if(index<0){
			this.add((-index-1), td);
			return true;
		}
		return false;
	}
	
	public int indexOf(TemporalDifference td){
		return Collections.binarySearch(this, td);
	}
	
	
	public boolean remove(TemporalDifference td){
		int index = Collections.binarySearch(this, td);
		if(index>=0){
			this.remove(index);
			return true;
		}
		return false;
	}
	
	public TDSortedList clone() {
		if(this == null) return null;
		//System.out.println("Cloning a TDSortedList");
		TDSortedList newList = new TDSortedList();
		for (TemporalDifference td : this){
			TemporalDifference newTd = td.clone();
			newList.insert(newTd);
		}
		//System.out.println("Finished a TDSortedList");
		return newList;
	}
	
	
	
}
