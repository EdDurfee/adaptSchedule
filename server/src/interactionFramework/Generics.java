package interactionFramework;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import interval.Interval;
import interval.IntervalSet;
import dtp.DisjunctiveTemporalProblem;

public class Generics {
	private static PrintStream output;
	static{
		try{
			output = new PrintStream(new FileOutputStream("./log.txt"));
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static <T> Collection<T> concat(Collection<List<T>> a){
		Collection<T> result = new ArrayList<T>();
		for(Collection<T> c : a) result.addAll(c);
		return result;
	}
	
	public static PrintStream getLogStream(){
		return output;
	}
	
	public static void print2log(String str){
		output.println(str);
	}
	
	public static String toTimeFormat(int min){
		String negative = min < 0 ? "-" : "";
		min = Math.abs(min);
		int hour = min / 60;
		min = min % 60;
		return negative+hour+":"+String.format("%02d",min);
	}
	
	public static int fromTimeFormat(String str){
		String[] values = str.split(":");
		return Integer.parseInt(values[0])*60 + Integer.parseInt(values[1]);
	}
	
	public static IntervalSet stringToInterval(String str){
		IntervalSet result = new IntervalSet();
		String[] arr = str.split("[\\[\\]\\{\\}]");
		for(String s : arr){
			if(!s.contains(":")){
				continue;
			}
			String[] vals = s.split("\\,");
			if(vals.length == 1){
				int v = Generics.fromTimeFormat(vals[0]);
				result.add(new Interval(v,v));
			}
			else if(vals.length == 2){
				int v1 = Generics.fromTimeFormat(vals[0]);
				int v2 = Generics.fromTimeFormat(vals[1]);
				result.add(new Interval(v1,v2));
			}
			else{
				System.out.println("Unexpected response \""+str+"\". Check format.");
				return null;
			}
		}
		result.simplify();
		return result;
	}
	
	public static void printDTP2log(DisjunctiveTemporalProblem dtp){
		output.println("numSolutions: "+dtp.getNumSolutions());
		output.println("flexibility: "+dtp.getTotalFlexibility());
		output.println("ridgidity: "+dtp.getRigidity());		
		dtp.printTimepointIntervals(output);
	}
	
	public static void printDTP(DisjunctiveTemporalProblem dtp){
		System.out.println("numSolutions: "+dtp.getNumSolutions());
		System.out.println("flexibility: "+dtp.getTotalFlexibility());
		System.out.println("ridgidity: "+dtp.getRigidity());		
		dtp.printTimepointIntervals(System.out);
	}
	
	public static void printDTP(DisjunctiveTemporalProblem dtp, int time){
		System.out.println("numSolutions: "+dtp.getNumSolutions());
		System.out.println("flexibility: "+dtp.getTotalFlexibility());
		System.out.println("ridgidity: "+dtp.getRigidity());		
		dtp.printTimepointIntervals(System.out, time);
	}
	
	public static List<String> intersect(List<String> l1, List<String> l2){
		List<String> result = new ArrayList<String>();
		for(String o : l1){
			if(l2.contains(o)) result.add(o);
		}
		return result;
	}
	
	public static List<String> intersect(List<List<String>> els){
		List<String> firstelm = els.get(0);
		els.remove(firstelm);
		List<String> result = new ArrayList<String>(); result.addAll(firstelm);
		for(List<String> l : els){
			result = intersect(result, l);
		}
		return result;
	}
}
