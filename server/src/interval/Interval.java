package interval;

import interactionFramework.Generics;

public class Interval implements Comparable<Interval>, java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3455662633275210807L;
	private double lowerBound;
	private double upperBound;
	
	
	public Interval(double lowerBound, double upperBound){
		assert(lowerBound <= upperBound);
		if(lowerBound > upperBound){
			throw new RuntimeException("Invalid interval: ["+Generics.toTimeFormat((int) lowerBound)+", "+Generics.toTimeFormat((int) upperBound)+"]");
		}
		
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Interval(Interval i) {
		this.lowerBound = i.lowerBound;
		this.upperBound = i.upperBound;
	}
	
	public boolean intersects(Interval other){
		return ((lowerBound <= other.upperBound) && (upperBound >= other.lowerBound));
	}
	
	public boolean intersect(Interval other){
		lowerBound = Math.max(lowerBound, other.lowerBound);
		upperBound = Math.min(upperBound, other.upperBound);
		return lowerBound<=upperBound;
	}
	
	public boolean adjacent(Interval other) {
		return ((upperBound+1 == other.lowerBound) || (other.upperBound+1 == lowerBound));
	}
	
	public boolean intersects(int val) {
		if(lowerBound <= val && upperBound >= val) return true;
		return false;
	}
	
	public void merge(Interval other){
		lowerBound = Math.min(lowerBound, other.lowerBound);
		upperBound = Math.max(upperBound, other.upperBound);
	}
	
	public void compose(Interval other){
		lowerBound = lowerBound + other.lowerBound;
		upperBound = upperBound + other.upperBound;
	}
	
	public void invert(){
		double oldUpperbound = upperBound;
		upperBound = 0-lowerBound;
		lowerBound = 0-oldUpperbound;
	}
	
	public Interval intersection(Interval other){
		return new Interval(Math.max(lowerBound,other.lowerBound), Math.min(upperBound, other.upperBound));
	}
	
	public Interval union(Interval other){
		return new Interval(Math.min(lowerBound,other.lowerBound), Math.max(upperBound, other.upperBound));
	}
	public Interval composition(Interval other){
		return new Interval(lowerBound+other.lowerBound, upperBound+other.upperBound);
	}
	
	public Interval increment(int t){
		lowerBound-=t;
		upperBound-=t;
		return this;
	}
	
	public Interval decrement(int t){
		lowerBound+=t;
		upperBound+=t;
		return this;
	}
	
	public Interval inverse(){
		return new Interval(0-upperBound, 0-lowerBound);
	}
	
	public Interval positive(){
		double newupper = 0;
		double newlower = 0;
		if (this.upperBound < 0) newupper = 0 - upperBound;
		else newupper = upperBound;
		if(this.lowerBound < 0) newlower = 0 - lowerBound;
		else newlower = lowerBound;
		return new Interval(newupper, newlower);
	}
	
	public static Interval intersection(Interval a, Interval b){
		return new Interval(Math.max(a.lowerBound,b.lowerBound), Math.min(a.upperBound, b.upperBound));
	}
	
	public static Interval union(Interval a, Interval b){
		return new Interval(Math.min(a.lowerBound,b.lowerBound), Math.max(a.upperBound, b.upperBound));
	}
	public static Interval composition(Interval a, Interval b){
		return new Interval(a.lowerBound+b.lowerBound, a.upperBound+b.upperBound);
	}
	
	public static Interval inverse(Interval a){
		return new Interval(0-a.upperBound, 0-a.lowerBound);
	}

	@Override
	public int compareTo(Interval other) {
		if(lowerBound > other.upperBound+1) return 1;
		else if (upperBound < other.lowerBound+1) return -1;
		//ED: Edited these to support adjacency, not just strict overlap
		return 0;
	}

//	public int ubCompareTo(Interval other) {
//		return (int)Math.ceil(upperBound - other.upperBound);
//	}
//
//	
//	public int lbCompareTo(Interval other) {
//		return (int)Math.ceil(lowerBound - other.lowerBound);
//	}
	
	public double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double v){
		this.lowerBound = v;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double v){
		this.upperBound = v;
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		if(lowerBound == upperBound){
			return "["+Generics.toTimeFormat((int)lowerBound)+"]";
		}
		buffer.append('[');
		buffer.append(Generics.toTimeFormat((int) lowerBound));
		buffer.append(", ");
		buffer.append(Generics.toTimeFormat((int) upperBound));
		buffer.append(']');
		return buffer.toString();
	}
	
	public Interval clone() {
		if(this == null) return null;
		return new Interval(lowerBound, upperBound);
	}
}
