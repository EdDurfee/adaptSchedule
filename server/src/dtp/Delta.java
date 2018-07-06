package dtp;

import java.util.Comparator;

import interval.IntervalSet;

public class Delta implements Comparable<Delta> {


	// a delta is a pair of interval sets (the previous and then the current interval set) and a pair of timepoints between which we are examining the interval set
	// it also contains a relative difference (which is initialized to zero)
	// each delta is associated with a dtp 
	private DisjunctiveTemporalProblem currDTP;
	private DisjunctiveTemporalProblem prevDTP;
	private IntervalSet current;
	private IntervalSet previous;
	private String tp1;
	private String tp2;
	private double relativeDiff = 0;
	private double absoluteDiff = 0;;
	private int lengthDiff = 0;
	//flags depending on which delta type we're considering
	private int LEN = 0;
	private int ABS = 0;
	private int REL = 0;
	private int ALL = 0;
	
	public Delta(DisjunctiveTemporalProblem currdtp, DisjunctiveTemporalProblem prevdtp, String tp1, String tp2, String FLAG){
		this.currDTP = currdtp;
		this.prevDTP = prevdtp;
		this.tp1 = tp1;
		this.tp2 = tp2;
		this.current = currDTP.getInterval(tp1, tp2); // get interval should provide the properly formatted interval
		this.previous = prevDTP.getInterval(tp1, tp2);
		double currLB = current.getLowerBound();
		double currUB = current.getUpperBound();
		double prevLB = previous.getLowerBound();
		double prevUB = previous.getUpperBound();
		
		//relative change
		double prevDiff = Math.abs(prevUB - prevLB);
		double currDiff = Math.abs(currUB - currLB);
		this.relativeDiff = (prevDiff - currDiff) / prevDiff;
		
		//absolute change
		double diff = Math.abs((prevUB - prevLB) - (currUB - currLB));
		this.absoluteDiff = diff;
		
		//change is length of interval set
		int currLen = current.numIntervals();
		int prevLen = previous.numIntervals();
		this.lengthDiff = currLen - prevLen;
		
		if(FLAG == "LEN") this.LEN = 1;
		if(FLAG == "ABS") this.ABS = 1;
		if(FLAG == "REL") this.REL = 1;
		if(FLAG == "ALL") this.ALL = 1;			
	}
	 public double getAbsoluteDifference(){
		 return this.absoluteDiff;
	 }
	 public double getRelativeDifference() {
		 return this.relativeDiff;
	 }
	 public int getLengthDifference() {
		 return this.lengthDiff;
	 }
	 
	 public String toString(){
		 if(this.previous.getLowerBound() < 0) {
			 //System.out.println(this.tp1 + " is inverted.");
			 //System.out.println("prev: "+this.previous.toString() + " inverted: " + this.previous.inverse().toString());
			 previous = previous.inverse();
		 }
		 if(this.current.getLowerBound() < 0) current = current.inverse();
		 String str = "Delta between "+this.tp1+" and "+this.tp2+"\t"+ this.previous.toString()+" --> "+ this.current.toString();
		 //String debug = "\n prev lowerbound: "+ this.previous.getLowerBound() + "curr lowerbound " + this.current.getLowerBound();
		 return str;
	 }
	
	@Override
	// we want to sort in descending order (with the greatest element first)
	public int compareTo(Delta delt) {
		double compareVal = 0;
		double thisVal = 0;
		if(REL == 1) compareVal = delt.relativeDiff; thisVal = this.relativeDiff;
		if(ABS == 1) compareVal = delt.absoluteDiff; thisVal = this.absoluteDiff;
		
		if(thisVal > compareVal) return -1;
		if(compareVal > thisVal) return 1;
		else return 0;
	}
	
	public static Comparator<Delta> RelativeDifferenceComparator = new Comparator<Delta>() {
		public int compare(Delta d1, Delta d2) {
			Double diff1 = d1.getRelativeDifference();
			Double diff2 = d2.getRelativeDifference();
			
			return diff2.compareTo(diff1);
	}
	};
	public static Comparator<Delta> AbsoluteDifferenceComparator = new Comparator<Delta>() {
		public int compare(Delta d1, Delta d2){
			Double diff1 = d1.getAbsoluteDifference();
			Double diff2 = d2.getAbsoluteDifference();
			
			return diff2.compareTo(diff1);
		}
	};
}
