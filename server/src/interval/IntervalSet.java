package interval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import stp.TemporalDifference;
import stp.Timepoint;
import dtp.DisjunctiveTemporalConstraint;

public class IntervalSet implements Iterable<Interval>, java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3989377566153552905L;
	ArrayList<Interval> intervals = new ArrayList<Interval>();

	public IntervalSet(IntervalSet is){
		intervals = new ArrayList<Interval>();
		for(Interval i : is.intervals){
			intervals.add(new Interval(i));
		}
	}
	
	public IntervalSet(){
		intervals = new ArrayList<Interval>();
	}
	
	public boolean isNull(){
		if(intervals.size() == 0) return true;
		else return false;
	}

	public IntervalSet(Interval[] intervals){
		this();
		for (Interval interval : intervals) {
			this.intervals.add(interval);
		}
	}
	
	public static IntervalSet getEmptyInterval(){
		IntervalSet result = new IntervalSet();
		result.add(new Interval(0,0));
		return result;
	}
	
	public double getSlack(int time){
		double slack = Double.POSITIVE_INFINITY;
		for(Interval i : intervals){
			if(i.getLowerBound() <= time && i.getUpperBound() >= time){
				double temp = -i.getLowerBound() + time;
//				System.out.println("time: "+time+"\tlb: "+i.getLowerBound()+"\tub: "+i.getUpperBound()+"\tslack: "+temp);
				if(temp <= slack) slack = temp;
			}
		}
		return slack;
	}
	
	public double getUpperBound(){
		double ub = Double.NEGATIVE_INFINITY;
		for(Interval i : intervals){
			if(i.getUpperBound() > ub){
				ub = i.getUpperBound();
			}
		}
		return ub;
//		return intervals.get(intervals.size()-1).getUpperBound();
	}
	
	public double getLowerBound(){
		double lb = Double.POSITIVE_INFINITY;
		for(Interval i : intervals){
			if(i.getLowerBound() < lb) lb = i.getLowerBound();
		}
		return lb;
//		return intervals.get(0).getUpperBound();
	}
	
	/**
	 * reduces the interval set so that there are no intersecting intervals
	 */
	public void simplify(){
//		System.out.print("Simplifying: "+intervals.toString());
		for(int i = 0; i < intervals.size(); i++){
			for(int j = i+1; j < intervals.size(); j++){
				if(intervals.get(i).intersects(intervals.get(j)) || intervals.get(i).adjacent(intervals.get(j))){
					// System.out.println(intervals.get(i).inverse().toString()+" "+i+" intersects "+j+" "+intervals.get(j).inverse().toString());
					intervals.set(i, intervals.get(i).union(intervals.get(j)));
					intervals.remove(j);
					j--;
				}
			}
		}
//		System.out.println(" to "+intervals.toString());
	}
	
	public Collection<DisjunctiveTemporalConstraint> generateConstraints(Timepoint source, Timepoint dest){
		this.simplify(); 
		
		ArrayList<ArrayList<TemporalDifference>> diffs = new ArrayList<ArrayList<TemporalDifference>>();
		diffs.add(new ArrayList<TemporalDifference>());  //diffs.get(0) is lower bounds
		diffs.add(new ArrayList<TemporalDifference>());  //diffs.get(1) is upper bounds
		for(Interval i : intervals){
			diffs.get(0).add(new TemporalDifference(source, dest, (int) -i.getLowerBound()));
			diffs.get(1).add(new TemporalDifference(dest, source, (int) i.getUpperBound()));
		}
		
		return DisjunctiveTemporalConstraint.crossProduct(diffs);
	}
	
	public IntervalSet add(Interval interval){
		//Perform binary search to find add site, merge any intervals that intersect along the way
		int minIdx = 0;
		int maxIdx = intervals.size();
		while (minIdx < maxIdx){
			int midIdx = (minIdx+maxIdx)/2;
			int comparison = intervals.get(midIdx).compareTo(interval);
			if(comparison <0){
				maxIdx = midIdx;
			}
			else if(comparison >0){
				minIdx = midIdx;
				if(minIdx==midIdx) minIdx++;
			}
			else{
				maxIdx--;
				Interval ref = intervals.remove(midIdx);
				if(interval.getUpperBound() <= ref.getUpperBound()) maxIdx = midIdx; 
				if(interval.getLowerBound() >= ref.getLowerBound()) minIdx = midIdx;
				interval.merge(ref);
			}
		}
		intervals.add(minIdx, interval);
		return this;
	}
	
	public IntervalSet subtract(IntervalSet interval){
		simplify();
		interval.simplify();
		Iterator<Interval> it = intervals.iterator();
		while(it.hasNext()){
			Interval i = it.next();
			if(interval.intersect(i)){
				if(interval.getLowerBound() <= i.getLowerBound() && interval.getUpperBound() >= i.getUpperBound()){  //interval is completely erased
					it.remove();
				}
				else if(interval.getUpperBound() >= i.getUpperBound()){  //the lower part of the interval survives
					i.setUpperBound(interval.getLowerBound());
				}
				else if(interval.getLowerBound() <= i.getLowerBound()){  //the upper part of the interval survives
					i.setLowerBound(interval.getUpperBound());
					
				}
				else{ //the interval is split into two
					Interval temp = new Interval(i);
					i.setUpperBound(interval.getLowerBound());
					temp.setLowerBound(interval.getUpperBound());
					intervals.add(temp);
				}
			}
		}
		return this;
	}
	
	public IntervalSet subtract(Interval interval){
		simplify();
		Iterator<Interval> it = intervals.iterator();
		while(it.hasNext()){
			Interval i = it.next();
			if(i.intersects(interval)){
				if(interval.getLowerBound() <= i.getLowerBound() && interval.getUpperBound() >= i.getUpperBound()){  //interval is completely erased
					it.remove();
				}
				else if(interval.getUpperBound() >= i.getUpperBound()){  //the lower part of the interval survives
					i.setUpperBound(interval.getLowerBound());
				}
				else if(interval.getLowerBound() <= i.getLowerBound()){  //the upper part of the interval survives
					i.setLowerBound(interval.getUpperBound());
					
				}
				else{ //the interval is split into two
					Interval temp = new Interval(i);
					i.setUpperBound(interval.getLowerBound());
					temp.setLowerBound(interval.getUpperBound());
					intervals.add(temp);
					return this;  //interval can't intersect any other intervals in the simplified intervalSet
				}
			}
		}
		return this;
	}
	
	public void invert(){
		for (Interval interval : intervals) {
			interval.invert();
		}
		Collections.sort(intervals);
	}
	
	public static IntervalSet inverse(IntervalSet iIntervals){
		IntervalSet newIntervals = new IntervalSet();
		for (Interval interval : iIntervals.intervals) {
			newIntervals.add(interval.inverse());
		}
		newIntervals.sort();
		return newIntervals;
	}
	
	public IntervalSet inverse(){
		IntervalSet newIntervals = new IntervalSet();
		for (Interval interval : intervals) {
			newIntervals.add(interval.inverse());
		}
		newIntervals.sort();
		return newIntervals;
	}
	
	public IntervalSet positive() {
		IntervalSet newIntervals = new IntervalSet();
		for (Interval interval : intervals) {
			newIntervals.add(interval.inverse());
		}
		newIntervals.sort();
		return newIntervals; 
	}
	
	public void reset(){
		intervals.clear();
	}

	private void sort(){
		Collections.sort(intervals);
	}
	
	public IntervalSet increment(int t){
		IntervalSet result = new IntervalSet(this);
		for(Interval i : result.intervals){
			i.increment(t);
		}
//		System.out.println("Incrementing "+this.toString()+" by "+t+" yields "+result.toString());
		return result;
	}
	
	public IntervalSet decrement(int t){
		IntervalSet result = new IntervalSet(this);
		for(Interval i : result.intervals){
			i.decrement(t);
		}
//		System.out.println("Decrementing "+this.toString()+" by "+t+" yields "+result.toString());
		return result;
	}
	
	/**
	 * Computes is2 - is1
	 * E.g., {[20:00, 25:00]v[30:00]} - {[0:00, 5:00]v[10:00, 12:00]} = {[15:00, 25:00]v[8:00, 15:00]v[25:00, 30:00]v[18:00, 20:00]} = {[8:00, 30:00]}
	 * @param is1
	 * @param is2
	 * @return
	 */
	public static IntervalSet difference(IntervalSet is1, IntervalSet is2){
		IntervalSet result = new IntervalSet();
		for(Interval i1 : is1){
			for(Interval i2: is2){
				result.add(new Interval(i2.getLowerBound() - i1.getUpperBound(), i2.getUpperBound() - i1.getLowerBound()));
			}
		}
			
		result.simplify();
		return result;
	}
	
	public IntervalSet composition(IntervalSet other){
		IntervalSet newIntervals = new IntervalSet();
		for(int i=0; i< intervals.size(); i++){
			for(int j=0; j< other.intervals.size(); j++){
				newIntervals.add(Interval.composition(intervals.get(i), other.intervals.get(j)));
			}
		}
		//TODO: Traverse in a smarter way to possibly avoid duplication of effort
		return newIntervals;
	}
	
	public static IntervalSet composition(IntervalSet a, IntervalSet b){
		IntervalSet newIntervals = new IntervalSet();
		for(int i=0; i< a.intervals.size(); i++){
			for(int j=0; j< b.intervals.size(); j++){
				newIntervals.add(Interval.composition(a.intervals.get(i), b.intervals.get(j)));
			}
		}
		//TODO: Traverse in a smarter way to possibly avoid duplication of effort
		return newIntervals;
	}
	

	public boolean intersect(int val){
		for(Interval i : intervals){
			if(i.getLowerBound() <= val && i.getUpperBound() >= val) return true;
//			System.out.println("val: "+val+"\tlb: "+i.getLowerBound()+"\tub: "+i.getUpperBound());
		}
		return false;
	}
	
	public boolean intersect(Interval other){
		for(Interval i : intervals){
			if(i.intersects(other)) return true;
		}
		return false;
	}
	
	public boolean intersect(IntervalSet other){
		int thisIndex=0;
		int otherIndex=0;
		//TODO: Traverse in a smarter way to possibly avoid duplication of effort

		ArrayList<Interval> newIntervals = new ArrayList<Interval>();
		while(thisIndex<intervals.size() && otherIndex<other.intervals.size()){
			int comparison = intervals.get(thisIndex).compareTo(other.intervals.get(otherIndex));
			if (comparison<0) otherIndex++;
			else if (comparison>0) thisIndex++;
			else{
				newIntervals.add(Interval.intersection(intervals.get(thisIndex), other.intervals.get(otherIndex)));
				double aUB = intervals.get(thisIndex).getUpperBound();
				double bUB = other.intervals.get(otherIndex).getUpperBound();
				if(aUB <= bUB) thisIndex++; 
				if(bUB <= aUB) otherIndex++;
			}
		}
		//TODO: TEST!
		intervals = newIntervals;
		return intervals.size()>0;
	}
	
	public int numIntervals(){
		return intervals.size();
	}
	
	public boolean isZero(){
		for(Interval interval : intervals){
			if(interval.getLowerBound() != interval.getUpperBound() || interval.getLowerBound() != 0){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @return The total number of possible values contained in the IntervalSet, min is 1
	 */
	public int totalSize(){
		int totalSize = 0;
//		System.out.println("Number of intervals is "+intervals.size());
		for (Interval interval : intervals) {
			totalSize += Math.max(1, interval.getUpperBound() - interval.getLowerBound());
//			System.out.println("TotalSize is "+totalSize);
		}
		return totalSize;
	}
	
	public IntervalSet intersection(IntervalSet other){
		int thisIndex=0;
		int otherIndex=0;
		//TODO: Traverse in a smarter way to possibly avoid duplication of effort

		IntervalSet newIntervals = new IntervalSet();
		while(thisIndex<intervals.size() && otherIndex<other.intervals.size()){
			int comparison = intervals.get(thisIndex).compareTo(other.intervals.get(otherIndex));
			if (comparison<0) otherIndex++;
			else if (comparison>0) thisIndex++;
			else{
				newIntervals.add(Interval.intersection(intervals.get(thisIndex), other.intervals.get(otherIndex)));
				double aUB = intervals.get(thisIndex).getUpperBound();
				double bUB = other.intervals.get(otherIndex).getUpperBound();
				if(aUB <= bUB) thisIndex++; 
				if(bUB <= aUB) otherIndex++;
			}
		}
		//TODO: TEST!
		return newIntervals;
	}

	public static IntervalSet intersection(IntervalSet a, IntervalSet b){
		int aIndex=0;
		int bIndex=0;
		//TODO: Traverse in a smarter way to possibly avoid duplication of effort

		IntervalSet newIntervals = new IntervalSet();
		while(aIndex<a.intervals.size() && bIndex<b.intervals.size()){
			System.out.println("new: "+newIntervals);
			System.out.println("curr A: "+a.intervals.get(aIndex));
			System.out.println("curr B: "+b.intervals.get(bIndex));
			
			
			int comparison = a.intervals.get(aIndex).compareTo(b.intervals.get(bIndex));
			if (comparison<0) bIndex++;
			else if (comparison>0) aIndex++;
			else{
				newIntervals.add(Interval.intersection(a.intervals.get(aIndex), b.intervals.get(bIndex)));
				double aUB = a.intervals.get(aIndex).getUpperBound();
				double bUB = b.intervals.get(bIndex).getUpperBound();
				if(aUB <= bUB) aIndex++; 
				if(bUB <= aUB) bIndex++;
			}
		}
		//TODO: TEST!
		return newIntervals;
	}

	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append('{');
		for (int i=0; i<intervals.size(); i++) {
			buffer.append(intervals.get(i));
			if(i<(intervals.size()-1)){
				buffer.append('v');
			}

		}
		buffer.append('}');
		return buffer.toString();
	}

	@Override
	public Iterator<Interval> iterator() {
		return this.intervals.iterator();
	}
	
	public IntervalSet clone() {
		if(this == null) return null;
		IntervalSet newIS = new IntervalSet();
		for(Interval i : intervals) newIS.add(i.clone());
		return newIS;
	}
}
