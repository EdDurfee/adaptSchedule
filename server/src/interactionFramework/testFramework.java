package interactionFramework;

import interval.Interval;
import interval.IntervalSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
//import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import java.util.Iterator;

import stp.TemporalDifference;
import stp.Timepoint;

import dtp.DUSTP;
import dtp.DUTP;
import dtp.DisjunctiveTemporalConstraint;
import dtp.DisjunctiveTemporalProblem;
import dtp.ExhaustiveDTP;
import dtp.SimpleDTP;

public class testFramework {
	private static boolean randomIdle = false; //use random values for the idle time the user takes when available
	private static boolean distributedIdle = true; // distribute the idle time across the scheduling period.
	private static boolean fullIdle = true; // use as much idle as available at each decision point.
	private static String folder = ""; //folder name in which to access saved problems
	//private static String PATH = "/home/lynnhg/dataFiles2/";
	private static String PATH = "/home/lynngarrett/research/dataFiles/stpfiles/"; //local path to the folder location
	private static boolean minIdle = false;
	private static int DUR = 20; //duration of the sporadic event 
	private static int FILL = 580; //amount of time activities should take up in the total length of the scheduling period
	private static int L = 600; //total length of the scheduling period.
	private static int A = 10; //NUMBER OF ACTIVITIES 
	private static int id = 0; //id number to use when saving and accessing randomly generated problems.
	private static int START = 0; //index at which id should start when saving or accessing problems
	
	private static int NumDisjuncts = 4;
	
	private static Integer[] small = new Integer[]{0,1,3,4};
	private static List<Integer> small_random = Arrays.asList(small);
	private static Integer[] med = new Integer[]{16, 10, 18, 14, 12, 17, 6, 19, 4, 1, 9, 13, 15, 11, 3, 7};
	//this tells whether or not a sporadic event happens in the problem with this index. should make one of length 50. 
	
	private static List<Integer> medium_random = Arrays.asList(med);
	private static Integer[] rands = new Integer[]{82,2,51,65,23,9,87,56,4,20,18,17,34,72,98,67,57,89,93,73,85,43,31,29,32,74,60,95,86,66,24,91,21,96,16,58,41,30,10,80,47,22,75,76,54,37,6,15,12,0,48,13,90,26,19,78,14,83,69,81,77,35,55,45,42,64,36,46,27,92,38,50,59,49,62,3,39,70,5,8};
	private static List<Integer> whichRandom = Arrays.asList(rands);
	private static Integer[] small_pl = new Integer[]{2, 1, 3, 5, 4};
	private static List<Integer> small_places = Arrays.asList(small_pl);
	private static Integer[] med_pl = new Integer[]{3, 2, 1, 4, 1, 3, 1, 4, 2, 2, 3, 5, 1, 2, 2, 5};
	private static List<Integer> medium_places = Arrays.asList(med_pl);
	private static Integer[] places = new Integer[]{2,1,2,2,2,2,3,2,4,1,4,3,3,1,5,3,5,4,1,5,3,1,4,2,2,3,1,2,1,1,1,3,3,3,2,1,1,4,5,1,3,3,3,2,2,3,4,1,2,5,3,3,4,5,2,4,2,4,4,4,2,4,1,3,5,1,1,1,1,5,1,4,3,4,2,3,5,2,3,5};
	private static List<Integer> whereToPlace = Arrays.asList(places);
	private static List<String> testOrder = null;
	
	private static Integer[] rands50 = new Integer[]{37, 44, 16, 34, 40, 1, 41, 33, 30, 4, 38, 22, 15, 47, 7, 25, 43, 11, 32, 39, 46
	                                                  , 36, 29, 24, 48, 9, 14, 6, 2, 5, 27, 18, 19, 31, 8, 35, 12, 20, 28, 21};
	private static List<Integer> fifty_sporadics = Arrays.asList(rands50); //placements of the sporadic event in a 60-min daye
	
	private static Integer[] randplaces50 = new Integer[]{124, 391, 180, 456, 248, 242, 330, 394, 523, 453, 522, 430, 392, 36, 206, 543, 
		570, 1, 321, 280, 571, 469, 37, 220, 282, 284, 156, 50, 559, 510, 414, 577, 344,
		 153, 232, 174, 368, 305, 25, 343, 326, 160, 96, 439, 355, 290, 261, 14, 57, 340};
	private static List<Integer> fifty_places = Arrays.asList(randplaces50); //placements of the sporadic event in a 600-min day
	
	private static Integer[] randplaces5060 = new Integer[]{27, 39, 33, 25, 27, 58, 35, 2, 8, 0, 56, 47, 35, 10, 3, 11, 40, 35, 11, 21, 48, 3, 52, 33, 19, 54, 44, 58, 
		2, 38, 18, 51, 31, 50, 33, 29, 57, 51, 40, 46, 39, 7, 33, 21, 4, 1, 48, 53, 55, 30};
	private static List<Integer> fifty_places_60 = Arrays.asList(randplaces5060);
	
	private static List<List<String>> orders = new ArrayList<List<String>>();
	static{
		String[] one = new String[]{"Ee", "Bb", "Ij", "Cc", "Dd", "Aa", "Jj", "Hh", "Ff", "Gg"};
		String[] two = new String[]{"Jj", "Dd", "Ij", "Bb", "Cc", "Gg", "Ee", "Aa", "Ff", "Hh"};
		String[] three = new String[]{"Hh", "Ee", "Gg", "Ff", "Jj", "Aa", "Bb", "Ij", "Cc", "Dd"};
		String[] four = new String[]{"Hh", "Ff", "Ij", "Cc", "Jj", "Gg", "Aa", "Bb", "Dd", "Ee"};
		String[] five = new String[]{"Dd", "Ij", "Hh", "Jj", "Gg", "Ff", "Cc", "Ee", "Bb", "Aa"};
		String[] six = new String[]{"Ij", "Hh", "Ee", "Dd", "Cc", "Ff", "Jj", "Gg", "Bb", "Aa"};
		String[] seven = new String[]{"Gg", "Cc", "Jj", "Dd", "Hh", "Ff", "Bb", "Ee", "Ij", "Aa"};
		String[] eight = new String[]{"Aa", "Cc", "Ij", "Ff", "Jj", "Dd", "Ee", "Gg", "Hh", "Bb"};
		String[] nine = new String[]{"Gg", "Cc", "Hh", "Ff", "Jj", "Dd", "Ij", "Ee", "Aa", "Bb"};
		String[] ten = new String[]{"Gg", "Ij", "Ee", "Jj", "Hh", "Aa", "Cc", "Bb", "Dd", "Ff"};
		orders.add(Arrays.asList(one));
		orders.add(Arrays.asList(two));
		orders.add(Arrays.asList(three));
		orders.add(Arrays.asList(four));
		orders.add(Arrays.asList(five));
		orders.add(Arrays.asList(six));
		orders.add(Arrays.asList(seven));
		orders.add(Arrays.asList(eight));
		orders.add(Arrays.asList(nine));
		orders.add(Arrays.asList(ten));
		List<String> letters = new ArrayList<String>(); letters.add("Aa"); letters.add("Bb"); letters.add("Cc"); letters.add("Dd"); letters.add("Ee"); letters.add("Ff");
		String[] oneprime = new String[]{"Bb", "Aa", "Dd", "Ee", "Ff", "Cc", "Gg", "Hh", "Ij", "Jj"};
		testOrder = Arrays.asList(oneprime);
	}
	
	//random idle values selected uniformly at random from the interval [0, 1.0]. the last value is always 1.0 so whatever
	// remaining idle time is taken at the end of the scheduling period.
	private static ArrayList<List<Double>> uniform_idles = new ArrayList<List<Double>>();
	static{
		Double[] one = new Double[]{0.24, 0.27, 0.59, 0.77, 0.43, 0.83, 0.56, 0.81, 0.36, 0.98, 1.0};
		Double[] two = new Double[]{0.42, 0.58, 0.29, 0.82, 0.42, 0.72, 0.92, 0.63, 0.03, 0.78, 1.0};
		Double[] three = new Double[]{0.77, 0.18, 0.93, 0.02, 0.73, 0.42, 0.26, 0.52, 0.07, 0.94, 1.0};
		Double[] four = new Double[]{0.72, 0.94, 0.82, 0.95, 0.31, 0.44, 0.26, 0.65, 0.76, 0.45, 1.0};
		Double[] five = new Double[]{0.98, 0.51, 0.1, 0.99, 0.86, 0.43, 0.46, 0.45, 0.62, 0.88, 1.0};
		Double[] six = new Double[]{0.04, 0.94, 0.01, 0.88, 0.6, 0.79, 0.58, 0.59, 0.87, 0.29, 1.0};
		Double[] seven = new Double[]{0.73, 0.82, 0.12, 0.09, 0.45, 0.84, 0.4, 0.89, 0.13, 0.28, 1.0};
		Double[] eight = new Double[]{0.8, 0.89, 0.5, 0.36, 0.69, 0.36, 0.31, 0.87, 0.61, 0.44, 1.0};
		Double[] nine = new Double[]{0.28, 0.73, 0.45, 0.78, 0.91, 0.28, 0.61, 0.94, 0.61, 0.81, 1.0};
		Double[] ten = new Double[]{0.93, 0.5, 0.75, 0.34, 0.08, 0.61, 0.96, 0.06, 0.64, 0.65, 1.0};
		uniform_idles.add(Arrays.asList(one));
		uniform_idles.add(Arrays.asList(two));
		uniform_idles.add(Arrays.asList(three));
		uniform_idles.add(Arrays.asList(four));
		uniform_idles.add(Arrays.asList(five));
		uniform_idles.add(Arrays.asList(six));
		uniform_idles.add(Arrays.asList(seven));
		uniform_idles.add(Arrays.asList(eight));
		uniform_idles.add(Arrays.asList(nine));
		uniform_idles.add(Arrays.asList(ten));
		
	}
	private static Double[] o = new Double[]{1.0,1.0,1.0,1.0,1.0,1.0,1.0};
	private static List<Double> ones = Arrays.asList(o);
	private static Double[] z = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0};
	private static List<Double> zeros = Arrays.asList(z);
	
	private static HashMap<SimpleEntry<String,String>, Integer> precTest = new HashMap<SimpleEntry<String, String>, Integer>();
	static{
		precTest.put(new SimpleEntry<String, String>("Aa", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Bb", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Cc", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Dd", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Ee", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Ff", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Gg", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Hh", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Ii", "sporadic"), 0);
		precTest.put(new SimpleEntry<String, String>("Jj", "sporadic"), 0);		
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args){
	START = 0;	
	int SHIFT = 0; // used to shift the random placements of sporadic event when running additional test iterations
				   //over the same set of problems.
	
	ArrayList<Integer> ns = new ArrayList<Integer>(); ns.add(20);
	ArrayList<Double> rs = new ArrayList<Double>(); 
	//rs.add(0.0); 
	//rs.add(0.1); rs.add(0.15); rs.add(0.2); rs.add(0.25); rs.add(0.5);
	
	
	//rs.add(0.05); 
	rs.add(0.3); 
	//rs.add(0.15); 
	//rs.add(0.2); 
	//rs.add(0.25);
	//rs.add(0.3); 
	//rs.add(0.35); 
	//rs.add(0.4); rs.add(0.5); rs.add(1.0);
	
	id = 1;
	ProblemGenerator.updateFillAmount(560);
	
	folder = "";
	
	//generateNewPhaseDTPs(1000,ns,rs);

	
	}
	
	
	
	/**
	 *A function used to test the validity of the DUSTPMany class in which equivalence classes are not used
	 * Compares the answers using the same underlying STP problems.
	 * @param num the number of problems to compare
	 * @param fill the fill level to use for the STP problems (used when opening the files from a folder)
	 */
	public static void testEquivalenceClasses(int num, int fill){
		try{
			FileWriter f_out = new FileWriter(PATH + folder + "/eqclass" + fill+".data");
			
			for(int i = 5; i < num; i++){
				//do the DC checking and calculate the equivalence class sizes and calls to solver
				//   necessary to do the checking. 
				System.out.println(i);
				DisjunctiveTemporalProblem stp = readFromFile(PATH+folder+"/test_stp_"+fill+"/dtp" +i+".data"); //UPDATE
				
				stp.updateInternalData();
				stp.enumerateSolutions(0);
				stp.simplifyMinNetIntervals();
				Generics.printDTP(stp);
				
				DUSTP d = new DUSTP(((SimpleDTP)stp.clone()),0,precTest);
				//DUSTP dmany = new DUSTP(((SimpleDTP)stp.clone()),precTest,true);
				// so we have two DUTPs to work with here. 
				// honestly. i think we're gonna want to try these with DUSTPs first. 
				// can use the set of STPs already generated. 
				d.checkDynamicControllability();
				int few = d.getDTreeSize();
				//dmany.checkDCMany();
				//SimpleEntry<Integer,Integer> many = dmany.getDTreeManySize();
				
				// and get callstoSolver
				int few_calls = d.getCallsToSolver();
				//SimpleEntry<Integer, Integer> many_calls = dmany.getCallsToSolverMany();
				
				//String out_str = i +"," + few + "," + few_calls + "," + many + "," +many_calls+"\n";
				//f_out.write(out_str);
			}
		f_out.close();
		
		}catch(IOException i){
			i.printStackTrace();
			
		}
		
	}
	
	/**
	 * Function used to generate a set of PhaseDTPs, varying the values for r. Does NOT save any 
	 * problems during generation
	 * @param total the total number of problems to generate (including invalid ones)
	 * @param n_vals the values of n to consider (number of timepoints)
	 * @param r_vals the values of r to consider (proportion of constraints added to timepoints)
	 * @param paired a boolean value for whether or not randomly generated constraints should be paired.
	 */
	public static void generateNewPhaseDTPs(int total, ArrayList<Integer> n_vals, ArrayList<Double> r_vals, boolean paired){
		int count = 0;
		int num_valid = 0; //valid as a dtp
		int num_wc = 0; //weakly controllable
		int num_dc = 0; //dynamically controllable
		
		try{
			FileWriter f_out = new FileWriter(PATH + "validityDTP113017.data", true);
		for(int n: n_vals){
			System.out.println("n: "+ n);
			for(double r : r_vals){
				int m = (int) ((int) n*r);
				count = 0;
				num_valid = 0;
				num_wc = 0;
				num_dc = 0; //reset all values for next r,n pairing
				
				while(count < total){
					System.out.println("COUNT: " + count);
					DisjunctiveTemporalProblem dtp = new ProblemGenerator().generateStructuredDTP(n,m,NumDisjuncts, paired);
					//check if valid as a DTP
					DisjunctiveTemporalProblem clone = dtp.clone();
					clone.updateInternalData();
					clone.enumerateSolutions(0);
					clone.simplifyMinNetIntervals();
					if(clone.getNumSolutions() == 0 || ((SimpleDTP) clone).hasZeroDurations()) {
						count++;
						continue;
					}//else it is valid and we move on
					System.out.println("FOUND a valid dtp");
					Generics.printDTP(clone);

					((SimpleDTP) clone).removeSubsumedVariables();
					DUTP dutp = new DUTP(clone, false, 0, ((SimpleDTP) clone).precMap);
					//check if WC
					//if(!dutp.checkWeakControllability()){
					//	count++;
					//	num_valid++;
					//	continue;
					//}
					//System.out.println("FOUND a WC dtp");
					//finally if it is valid and WC we check DC
					//boolean dc = dutp.generateComponentsFindDC();
					boolean dc = dutp.checkDCFindFirst();
					if(!dc){
						System.out.println("NOT DC");
						count++;
						num_valid++;
						//num_wc++;
						continue;
					}
					if(dc){
						System.out.println("FOUND a DC dtp");
						count++;
						num_valid++;
					//num_wc++;
						num_dc++;
					}
				}
				System.out.println("n: "+ n +" r: "+r+ " num valid: "+num_valid + " num_wc: " + num_wc + " num_dc: " + num_dc);
				String out_str = ""+n+","+r+","+num_valid+", " + num_wc+", "+num_dc+"\n";
				f_out.write(out_str);
			}
		}
		System.out.println("File written.");
		f_out.close();
		}catch(IOException i){
			i.printStackTrace();
		}
	}
		
	/**
	 * Similar to above, but specifically generates STP problems. 
	 * @param total the total number of problems to generate (including invalid ones)
	 * @param n_vals the values of n to consider (number of timepoints)
	 * @param r_vals the values of r to consider (proportion of constraints added to timepoints)
	 */
	private static void generateNewPhaseSTPs(int total, ArrayList<Integer> n_vals, ArrayList<Double> r_vals){
		int count = 0;
		int num_valid = 0;
		int num_dc = 0;
		
		try{
			FileWriter f_out = new FileWriter(PATH + "validitySTPsmall.data", true);
		for(int n: n_vals){
			System.out.println("n: "+ n);
			for(double r : r_vals){
				int m = (int) ((int) n*r);
				count = 0;
				num_valid = 0;
				num_dc = 0; //reset all values for next r,n pairing
				
						
				while(count < total){
					System.out.println("COUNT: " + count);
					DisjunctiveTemporalProblem stp = new ProblemGenerator().generateSTP(n,m);
					//check if valid as a DTP
					DisjunctiveTemporalProblem clone = stp.clone();
					clone.updateInternalData();
					clone.enumerateSolutions(0);
					clone.simplifyMinNetIntervals();
					if(clone.getNumSolutions() == 0 || ((SimpleDTP) clone).hasZeroDurations()) {
						count++;
						continue;
					}//else it is valid and we move on
					System.out.println("FOUND a valid stp");
					Generics.printDTP(clone);

					((SimpleDTP) clone).removeSubsumedVariables();
					DUSTP dustp = new DUSTP(((SimpleDTP)clone));
					//check if WC
					//if(!dutp.checkWeakControllability()){
					//	count++;
					//	num_valid++;
					//	continue;
					//}
					//System.out.println("FOUND a WC dtp");
					//finally if it is valid and WC we check DC
					boolean dc = dustp.checkDynamicControllability();
					//boolean dc = false;
					if(!dc){
						System.out.println("NOT DC");
						count++;
						num_valid++;
						continue;
					}
					if(dc){
					System.out.println("FOUND a DC dtp");
					count++;
					num_valid++;
					num_dc++;
					}
				}
				System.out.println("n: "+ n +" r: "+r+ " num valid: "+num_valid + " num_wc: " + " num_dc: " + num_dc);
				String out_str = ""+n+","+r+","+num_valid+", " + ", "+num_dc+"\n";
				f_out.write(out_str);
			}
		}
		System.out.println("File written.");
		f_out.close();
		}catch(IOException i){
			i.printStackTrace();
		}
	
	}
	
	/**
	 * This function looks at the impact of precedence level ont he dynamic controllability of problems.
	 * It randomly generates STPs and then sets the precedence relations to be either all zeros or all ones
	 * and records the number of problems in which this changes the dynamic controllability
	 * @param total the total number of problems to try
	 * @param n_vals 
	 * @param n_vals the values of n to consider (number of timepoints)
	 * @param r_vals the values of r to consider (proportion of constraints added to timepoints)
	 */
	private static void comparePrecedence(int total, ArrayList<Integer> n_vals, ArrayList<Double> r_vals, int fill, boolean paired){
		int count = 0;
		int num_valid = 0;
		int num_dc0 = 0;
		int num_dc1 = 0;
		int num_different = 0;
		try{
			FileWriter f_out = new FileWriter("/home/lynngarrett/research/dataFiles/comparePrec81617OLD.data", true);
			f_out.write("FILL: " +fill + "\n");
			for(int n: n_vals){
			System.out.println("n: "+ n);
			for(double r : r_vals){
				int m = (int) ((int) n*r);
				count = 0;
				num_valid = 0;
				num_dc0 = 0; //reset all values for next r,n pairing
				num_dc1 = 0;
				num_different = 0;
						
				while(count < total){
					System.out.println("COUNT: " + count);
					DisjunctiveTemporalProblem stp = new ProblemGenerator().generateSTP(n,m,paired);
					//check if valid as a DTP
					DisjunctiveTemporalProblem clone = stp.clone();
					clone.updateInternalData();
					clone.enumerateSolutions(0);
					clone.simplifyMinNetIntervals();
					if(clone.getNumSolutions() == 0 || ((SimpleDTP) clone).hasZeroDurations()) {
						count++;
						continue;
					}//else it is valid and we move on
					System.out.println("FOUND a valid stp");
					//num_valid++;
					Generics.printDTP(clone);

					((SimpleDTP) clone).removeSubsumedVariables();
					DUSTP dustp0 = new DUSTP(((SimpleDTP)clone),0);
					DUSTP dustp1 = new DUSTP(((SimpleDTP)clone.clone()),1);
					
					boolean dc0 = dustp0.checkDynamicControllability();
					boolean dc1 = dustp1.checkDynamicControllability();
					//boolean dc = false;
					if(!dc0){
						System.out.println("NOT DC0");
					}
					if(!dc1){
						System.out.println("NOT DC1");
					}
					
					if(dc0 && !dc1) {
						num_different++;
						writeToFile(stp);
					}
					if(dc1 && !dc0) {
						num_different++;
						writeToFile(stp);
					}
					
					if(dc0) {
						System.out.println("FOUND a DC 0 dtp"); 
						num_dc0++;
					}
					if(dc1) {
						System.out.println("FOUND a DC 1 dtp"); 
						num_dc1++;
					}

					
					count++;
					num_valid++;
				}
				String out_str = "n: " + n + " r: " + r+  " num dc0: " + num_dc0 + " num dc1: " + num_dc1 + " num valid : " + num_valid + " num_diff: " + num_different + "\n";
				System.out.println("num dc0: " + num_dc0 + " num dc1: " + num_dc1 + " num valid : " + num_valid);
				f_out.write(out_str);
			}
		}
		f_out.close();
		}catch(IOException i){
			i.printStackTrace();
		}
	
	}	
	
	/**
	 * This function generates num new DTPs and writees them to a file in the folder specified in
	 * the global variables above. 
	 * @param num the number of valid DTPs to generate (valid and DC)
	 * @param n the number of timepoints to use 
	 * @param r the proportion of random constraints to introduce
	 * @param numDisjuncts the number of ordering constraints to relax to nonconcurrency constraints
	 * @param paired whether or not the randomly generated constraints should be paired.
	 */
	private static void generateNewDTPs(int num, int n, double r, int numDisjuncts, boolean paired){
		int count = 0;
		int total_tried = 0;
		DisjunctiveTemporalProblem dtp = null;
		while(count < num){
			System.out.println("COUNT: "+count);
			int m = (int) ((int) n*r);
			//new ProblemGenerator();
			dtp = new ProblemGenerator().generateStructuredDTP(n,m, numDisjuncts, paired);
			//check if valid as a DTP
			DisjunctiveTemporalProblem clone = dtp.clone();
			clone.updateInternalData();
			clone.enumerateSolutions(0);
			clone.simplifyMinNetIntervals();
			if(clone.getNumSolutions() == 0 || ((SimpleDTP) clone).hasZeroDurations()) {
				
				total_tried++;
				continue;
			}//else it is valid and we move on
			System.out.println("FOUND a valid dtp");
			Generics.printDTP(clone);

			((SimpleDTP) clone).removeSubsumedVariables();
			DUTP dutp = new DUTP(clone, false, 0, ((SimpleDTP) clone).precMap);
			
			boolean dc = dutp.checkDCFindFirst();
			if(!dc){
				System.out.println("NOT DC");
				total_tried++;

				continue;
			}
			if(dc){
			writeToFile(dtp);
			total_tried++;
			count++;

			}
			
		}
		System.out.println("total tried: " + total_tried);
	}
	
	/**
	 * Takes a set of stps and translates them into dtps. Reads the stps from files snd writes dtps to files
	 * @param num_stps the number of stps to read in from their files
	 * @param disjunctsToAdd the number of ordering constraints to relax
	 */
	private static void generateDTPsFromSTPs(int num_stps, int disjunctsToAdd){
		id = 0;
		for(int i = 0; i < num_stps; i++){
			String folder_str = "test_stp_" + FILL;
			DisjunctiveTemporalProblem stp = readFromFile(PATH+folder_str+"/dtp"+i+".data");
			folder_str = "test_dtp" + "_" + FILL;
			stp.updateInternalData();
			stp.enumerateSolutions(0);
			stp.simplifyMinNetIntervals();
			DisjunctiveTemporalProblem dtp = ProblemGenerator.translateSTPtoDTP(stp, disjunctsToAdd);
			folder = folder_str;
			writeToFile(dtp);
		}
	}
	
	/**
	 * Takes a set of files representing 60 minute problems and multiplies all constraint bounds
	 * by 10 in order to create problems with 600 minutes. Writes these problems to new files
	 * @param num_probs the number of stp files to open and translate to 600 min problems
	 * @param fill the fill we're working with, used to open the proper files 
	 */
	private static void generate600from60(int num_probs, int fill){
		id = 0;
		for(int i = 0; i < num_probs; i++){
			String folder_str = "test_stp_"+fill;
			int new_fill = fill * 10;
			DisjunctiveTemporalProblem dtp = readFromFile(PATH+folder_str+"/dtp"+i+".data");
			folder_str = "test_stp_" + new_fill;
			dtp.updateInternalData();
			dtp.enumerateSolutions(0);
			dtp.simplifyMinNetIntervals();
			//update the dtp
			DisjunctiveTemporalProblem new_dtp = dtp.clone();
			ArrayList<DisjunctiveTemporalConstraint> constraints = new_dtp.getTempConstraints();
			for(DisjunctiveTemporalConstraint c : constraints){
				for(TemporalDifference td : c){
					// multiply every bound by 10
					System.out.println(td);
					if(td.bound == Integer.MAX_VALUE || td.bound == Integer.MIN_VALUE) continue;
					td.bound = td.bound * 10;
					System.out.println(td);
				}
			}
			folder = folder_str;
			writeToFile(new_dtp);
		}
	}
	
	/**
	 * Takes in 60 minute problems and multiples all constraint bounds by multiply
	 * @param num_probs the number of problem files to read in and convery
	 * @param fill the fill level of said problem files
	 * @param multiply the factor by which to multiply all constraint bounds
	 */
	private static void generatefrom60(int num_probs, int fill,int multiply){
		id = 0;
		for(int i = 0; i < num_probs; i++){
			String folder_str = "test_stp_"+fill;
			int new_fill = fill * multiply;
			DisjunctiveTemporalProblem dtp = readFromFile(PATH+folder_str+"/dtp"+i+".data");
			folder_str = "test_stp_" + new_fill;
			dtp.updateInternalData();
			dtp.enumerateSolutions(0);
			dtp.simplifyMinNetIntervals();
			//update the dtp
			DisjunctiveTemporalProblem new_dtp = dtp.clone();
			ArrayList<DisjunctiveTemporalConstraint> constraints = new_dtp.getTempConstraints();
			for(DisjunctiveTemporalConstraint c : constraints){
				for(TemporalDifference td : c){
					// multiply every bound by multiply
					System.out.println(td);
					if(td.bound == Integer.MAX_VALUE || td.bound == Integer.MIN_VALUE) continue;
					td.bound = td.bound * multiply;
					System.out.println(td);
				}
			}
			folder = folder_str;
			writeToFile(new_dtp);
		}
	}
	
	/**
	 * Generates new STP problems that are DC and writes them to files. Fill level must be set before calling this function
	 * @param num the number of problems to generate 
	 * @param n the number of timepoints 
	 * @param r the ratio of constraints to timepoints
	 * All problems are written to files based on the "folder" and PATH global variables
	 */
	private static void generateNewSTPs(int num, int n, double r, boolean paired){
		id = 0;
		int count = 0;
		int num_tried = 0;
		int m = (int) ((int) n*r);
		
		while(count < num){
			System.out.println("COUNT: " + count);
			DisjunctiveTemporalProblem stp = new ProblemGenerator().generateSTP(n,m,paired);
			DisjunctiveTemporalProblem clone = stp.clone();
			clone.updateInternalData();
			clone.enumerateSolutions(0);
			clone.simplifyMinNetIntervals();
			if(clone.getNumSolutions() == 0 || ((SimpleDTP) clone).hasZeroDurations()) {
				num_tried++;
				continue;
			}//else it is valid and we move on
			System.out.println("FOUND a valid stp");
			Generics.printDTP(clone);

			((SimpleDTP) clone).removeSubsumedVariables();
			DUSTP dustp = new DUSTP(((SimpleDTP)clone));
			
			boolean dc = dustp.checkDynamicControllability();
			if(!dc){
				System.out.println("NOT DC");
				num_tried++;
				continue;
			}
			if(dc){
				System.out.println("FOUND a DC dtp");
				writeToFile(stp);
				count++;
				num_tried++;
			}
		}
		System.out.println("Number tried: " + num_tried);
	}
	/**
	 * Writes the input dtp to a file based on the PATH and folder global variables.
	 * @param dtp the disjunctive temporal problem to be saved to a file.
	 */
	public static void writeToFile(DisjunctiveTemporalProblem dtp){
		File f = new File(PATH+folder+"/dtp"+id+".data");
		id++;
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try{
			FileOutputStream f_out = new FileOutputStream(f);
			ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
			obj_out.writeObject(dtp);
			obj_out.close();
			f_out.close();
			System.out.println("file written");
		}catch(IOException i){
			i.printStackTrace();
		}
	}
	/**
	 * Function for reading a DTP from file
	 * @param filename the path from which to read in the DTP 
	 * @return the DTP represented by the file at filename.
	 */
	public static DisjunctiveTemporalProblem readFromFile(String filename){
		DisjunctiveTemporalProblem d = null;
		try{
			FileInputStream f_in = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(f_in);
			d = (DisjunctiveTemporalProblem) in.readObject();
			in.close();
			f_in.close();
			//System.out.println("Recovered dtp from test.data");
		}catch(ClassNotFoundException c){
			c.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return d;
	}


	/*
	 * The runExperiments functions follow, and they all follow the format of the first version presented 
	 * in terms of arguments required and functionality. 
	 */
	
	/**
	 * Function to run Naive experiments using problems stored in files.
	 * @param num the number of problems to open and run through the simulator
	 * @param shift the shift used to determine the occurrence of the sporadic event and order in which activities are performed
	 * @param type either "MAX", "MIN", or "HALF", or "PACKING" depending on the strategy for the user idling. MAX refers to the mDTF method, 
	 * 		MIN to the mACF method, and HALF is a variant in which the user is always offered halfway between MAX and MIN.
	 * 		In the PACKING strategy, the user is only offered idle time if it is necessary
	 * @param out the output file to write the simulation results to. 
	 * @param act_filename an output file that contains a separate set of simulation results necessary for ACF calculation
	 */
	private static void runExperiments(int num, int shift, String type, String out, String act_filename){
		DisjunctiveTemporalProblem dtp = null;
		
		FileWriter f = null;
		boolean contingent = false;
		int num_contingent = 0;
		try {
			f = new FileWriter(PATH+folder+"/"+out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		long start = 0;
		long end = 0;
		for(int i = START; i < 50; i++){
			int where = -1;
			System.out.println(i);
			if(fifty_sporadics.contains((i+shift) % 50)){
				System.out.println("inserting contingent!");
				contingent = true;
				int ind = fifty_sporadics.indexOf((i+shift) % 50);
				where = fifty_places_60.get(ind);
				num_contingent++;
			}
			List<Double> idleAmt = uniform_idles.get((i+shift) % 10);
			//List<Double> idleAmt = ones;
			List<String> actOrder = orders.get((i+shift) % 10);
			//System.out.println("DTP "+ i);
			dtp = readFromFile(PATH+folder+"/dtp"+i+".data");
			//System.out.println(((SimpleDTP) dtp).hasZeroDurations());
			start = System.nanoTime();
			stepThroughWithOutput(dtp, i, f, contingent, where, actOrder, idleAmt, type, act_filename);
			end = System.nanoTime();
			System.gc();
			long total = (end - start) / 1000000;
			System.out.println(total);
			String out_str = "TIME: " + total+ "\n";
			try{
				f.write(out_str);
			}catch(IOException e){
				e.printStackTrace();
			}

			contingent = false;
		}
		try {
			f.write("Number of contingent events: "+num_contingent);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static void runExperimentsPreemptive(int shift, String type, String out, String act_filename){
	
		DisjunctiveTemporalProblem dtp = null;
		
		FileWriter f = null;
		boolean contingent = false;
		int num_contingent = 0;
		try {
			f = new FileWriter(PATH+folder+"/"+out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		long start = 0;
		long end = 0;
		for(int i = START; i < 50; i++){
			int where = -1;
			System.out.println(i);
			if(fifty_sporadics.contains((i+shift) % 50)){
				System.out.println("inserting contingent!");
				contingent = true;
				int ind = fifty_sporadics.indexOf((i+shift) % 50);
				where = fifty_places_60.get(ind);
				num_contingent++;
			}
			List<Double> idleAmt = uniform_idles.get((i+shift) % 10);
			//List<Double> idleAmt = ones;
			List<String> actOrder = orders.get((i+shift) % 10);
			//System.out.println("DTP "+ i);
			dtp = readFromFile(PATH+folder+"/dtp"+i+".data");
			//System.out.println(((SimpleDTP) dtp).hasZeroDurations());
			start = System.nanoTime();
			stepThroughPreemptive(dtp, i, f, contingent, where, actOrder, idleAmt, type, act_filename);
			end = System.nanoTime();
			System.gc();
			long total = (end - start) / 1000000;
			System.out.println(total);
			String out_str = "TIME: " + total+ "\n";
			try{
				f.write(out_str);
			}catch(IOException e){
				e.printStackTrace();
			}

			contingent = false;
		}
		try {
			f.write("Number of contingent events: "+num_contingent);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void runExperimentsDCA(int shift, String out, String act_filename){
		
		DisjunctiveTemporalProblem dtp = null;
		
		FileWriter f = null;
		boolean contingent = false;
		int num_contingent = 0;
		try {
			f = new FileWriter(PATH+folder+"/"+out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		long start = 0;
		long end = 0;
		for(int i = START; i < 50; i++){
			int where = -1;
			System.out.println(i);
			if(fifty_sporadics.contains((i+shift) % 50)){
				System.out.println("inserting contingent!");
				contingent = true;
				int ind = fifty_sporadics.indexOf((i+shift) % 50);
				where = fifty_places_60.get(ind);
				num_contingent++;
			}
			List<Double> idleAmt = uniform_idles.get((i+shift) % 10);
			//List<Double> idleAmt = ones;
			List<String> actOrder = orders.get((i+shift) % 10);
			//System.out.println("DTP "+ i);
			dtp = readFromFile(PATH+folder+"/dtp"+i+".data");
			//System.out.println(((SimpleDTP) dtp).hasZeroDurations());
			start = System.nanoTime();
			stepThroughPutAtEnd(dtp, i, f, contingent, where, actOrder, idleAmt, act_filename);
			end = System.nanoTime();
			long total = (end - start) / 1000000;
			String out_str = "TIME: " + total+ "\n";
			try{
				f.write(out_str);
			}catch(IOException e){
				e.printStackTrace();
			}

			contingent = false;
		}
		try {
			f.write("Number of contingent events: "+num_contingent);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static void runExperimentsDUTP(int shift, String out, String act_filename){
	
	DisjunctiveTemporalProblem dtp = null;
	
	FileWriter f = null;
	boolean contingent = false;
	int num_contingent = 0;
	try {
		f = new FileWriter(PATH+folder+"/"+out);
	} catch (IOException e) {
		e.printStackTrace();
	}
	long start = 0;
	long end = 0;
	for(int i = START; i < 50; i++){
		int where = -1;
		System.out.println(i);
		if(fifty_sporadics.contains((i+shift) % 50)){
			System.out.println("inserting contingent!");
			contingent = true;
			int ind = fifty_sporadics.indexOf((i+shift) % 50);
			where = fifty_places_60.get(ind);
			num_contingent++;
		}
		List<Double> idleAmt = uniform_idles.get((i+shift) % 10);
		//List<Double> idleAmt = ones;
		List<String> actOrder = orders.get((i+shift) % 10);
		//System.out.println("DTP "+ i);
		dtp = readFromFile(PATH+folder+"/dtp"+i+".data");
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		//System.out.println(((SimpleDTP) dtp).hasZeroDurations());
		start = System.nanoTime();
		DUTP dutp = new DUTP(((SimpleDTP) dtp), true);
		//DUSTP dustp = new DUSTP(((SimpleDTP) dtp));
		stepThroughDUTPWithOutput(dutp, i, f, contingent, where, actOrder, idleAmt, act_filename);
		end = System.nanoTime();
		long total = (end - start) / 1000000;
		String out_str = "TIME: " + total+ "\n";
		try{
			f.write(out_str);
		}catch(IOException e){
			e.printStackTrace();
		}

		contingent = false;
	}
	try {
		f.write("Number of contingent events: "+num_contingent);
		f.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	
}


	/**
	 * Translates a dtp into a problem to be used in the DC-A method. This problem has the sporadic event glued to the end of the 
	 * scheduling period.
	 * @param dtp the dtp to convert
	 * @return a dtp with the sporadic event inserted. 
	 */
	public static DisjunctiveTemporalProblem createPutAtEnd(DisjunctiveTemporalProblem dtp){
		DisjunctiveTemporalProblem newDTP = dtp.clone();
		Timepoint h_s = new Timepoint("sporadic_S",1);
		Timepoint h_e = new Timepoint("sporadic_E", 1);
		Timepoint zero = newDTP.getTimepoint("zero");
		newDTP.addTimepoint(h_s);
		newDTP.addTimepoint(h_e);
		//add meaningful constraints w zero tp
		TemporalDifference td1 = new TemporalDifference(h_s, zero, L);
		TemporalDifference td2 = new TemporalDifference(zero, h_s, 0);
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
		newDTP.addAdditionalConstraint(dtc1);
		newDTP.addAdditionalConstraint(dtc2);
		td1 = new TemporalDifference(h_e, zero, L);
		td2 = new TemporalDifference(zero, h_e, -L);
		dtc1 = new DisjunctiveTemporalConstraint(td1);
		dtc2 = new DisjunctiveTemporalConstraint(td2);
		newDTP.addAdditionalConstraint(dtc1);
		newDTP.addAdditionalConstraint(dtc2);
		//add constraints that say duration is DUR. 
		TemporalDifference tdmin = new TemporalDifference(h_s, h_e, -DUR);
		TemporalDifference tdmax = new TemporalDifference(h_e, h_s, DUR);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		newDTP.addAdditionalConstraints(dtcs); 
		Timepoint last = ((SimpleDTP) dtp).getLastActivity();
		//System.out.println("Putting health before "+ last);
		for(String name: ((SimpleDTP) dtp).getActivityNames()){
				((SimpleDTP) newDTP).addOrderingConstraint(name, "sporadic", 0, Integer.MAX_VALUE);
		}
		return newDTP;
	}
	
	/*
	 * Updated Step Through function. Accepts Type values of MAX, MIN, HALF, and PACKING
	 * dtp: problem we're working with
	 * id: id number of the problem we're working with
	 * f: output file 
	 * contingent: true if sporadic event occurs, false otherwise
	 * int contingentOccurs: minute in the day that the sporadic event occurs
	 * actOrder: order in which activities will be performed in the case of multiple activity choices at one decision point.
	 * idleAmt: amount of idle we want to take each time it is offered. 
	 * String type: MAX, MIN, HALF, PACKING
	 * String act_out: file name to write activity data output to 
	 */
	public static boolean stepThroughWithOutput(DisjunctiveTemporalProblem dtp, int id, FileWriter f, boolean contingent, int contingentOccurs, List<String> actOrder, List<Double> idleAmt, String type, String act_out){
		boolean sporadic_inserted = false;
		//System.out.println("DTP id: "+ id);
		int systemTime = 0;
		int time;
		int total_acts = 0;
		
		int actsMin = 0;
		int idleMin = 0;
		List<List<String>> minActs = null;
		DisjunctiveTemporalProblem minDTP = readFromFile(PATH+folder+"/dtp"+id+".data");
		
		
		FileWriter a = null;
		try {
			a = new FileWriter(PATH+folder+"/"+act_out, true);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		int numActsPerformed = 0;
		Random rn = new Random();
		Interval zeroInterval = new Interval(0,0);

		boolean runningFlag = true;
		boolean justIdled = false;
		
		//DisjunctiveTemporalProblem minDTP = dtp.clone();
		dtp.updateInternalData(); // i don't think it hurts to throw this in here jic
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		
		Generics.printDTP(dtp);
		
		minDTP.updateInternalData();
		minDTP.enumerateSolutions(0);
		//System.out.println("enumerated solutions");
		minDTP.simplifyMinNetIntervals();
		//System.out.println("simplified min net intervals");
		
		while(runningFlag){
			//Generics.printDTP(dtp);
			int minTime = dtp.getMinTime();
			if(minTime > 24*60) { 
				if(dtp.getFixedTimepoints().size() < (dtp.getTimepoints().size() - 1)){
					//if we haven't fixed all the timepoints, not all activities have been performed. 
					String str = "" + id +" FAIL " + contingent + "\n";
					try {
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed +"\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}else{
					int diff = L - systemTime;
					//System.out.println("Inserting " + diff + " idle that was unused at end of day.");
					String str2 = "" + id + ","+systemTime+","+diff+","+diff+","+diff + "," + diff+"\n";
					String str = "" + id + " PASS " + contingent + "\n";
					try {
						f.write(str2);
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed + "\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
			else if(minTime < systemTime){

				String str = "" + id +" FAIL " + contingent + "\n";
				try {
					f.write(str);
					f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
					f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
					a.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			else if(minTime > systemTime){
				//System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
				//This is inserting a forced idle. 
				int temp = minTime - systemTime;
				
				String out_str = id+","+systemTime+",0,0,0," + temp + "\n";
				try {
					f.write(out_str);
				} catch (IOException e) {
					e.printStackTrace();
				}
				systemTime = minTime;
				
				dtp.advanceToTime(-systemTime, temp, false);
				dtp.simplifyMinNetIntervals();
				
				minDTP.advanceToTime(-systemTime, temp, false);
				minDTP.simplifyMinNetIntervals();
				
				continue;
			}
			//Prompt user for their activity selection	
			
			List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -systemTime);
			if(activities.get(0).size() == 0) runningFlag = false;
			List<Integer> slack = new ArrayList<Integer>();
			
			//the slack varies based on which type we have 
			if(type.equals("MIN")) slack = dtp.getMinSlack(-systemTime);
			else if (type.equals("MAX")) slack = dtp.getMaxSlack();
			else if (type.equals("HALF")) slack = getHalfSlacks(dtp.getMaxSlack(), dtp.getMinSlack(-systemTime));
			else if (type.equals("PACKING")) slack.add(0);
			//else System.out.println("ERROR: UNKNOWN PROBLEM TYPE");

			if(!justIdled) {
				
				DisjunctiveTemporalProblem tempDTP = minDTP.clone();
				System.out.println("getting min slack");
				int minSlack = tempDTP.getMinSlack(-systemTime).get(0);
				System.out.println("finished getting min slack");
				//System.out.println("System time: " + systemTime + " minSlack: " + minSlack + " originalSlack: " + slack.get(0));
				idleMin = minSlack;
				int tempTime = systemTime + minSlack;
				tempDTP.advanceToTime(-tempTime, minSlack, true);
				tempDTP.simplifyMinNetIntervals();
				minActs = tempDTP.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -tempTime);
				actsMin = minActs.get(0).size();
				List<Integer> DTPminSlack = minDTP.getMinSlack(-systemTime);

				if(slack.get(0) == 0){
					String str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+"0,0";
					try {
						f.write(str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					justIdled = true; 
					continue;
				}
				else{
					//if idle is available, the user always idles first. 
					
					if(randomIdle) time = (int) (slack.get(0) * idleAmt.get(numActsPerformed));
					else if(distributedIdle){
						time =  (int) Math.ceil((slack.get(0) * (1.0 /(A - numActsPerformed))));
					}
					else time = slack.get(0);
					//if(slack.get(0) > 0) time = 1;
					String out_str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+ slack.get(0)+","+time;
					try {
						f.write(out_str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					//System.out.println("idling for "+ time);
					systemTime+= time;
					dtp.advanceToTime(-systemTime, time, true);
					dtp.simplifyMinNetIntervals();
					
					minDTP.advanceToTime(-systemTime, time, true);
					minDTP.simplifyMinNetIntervals();
					
					justIdled = true;
					continue;
				}
			} else { //we did just idle. 
				// need to check for the occurrence of the sporadic event before continuing
				
				if(systemTime > contingentOccurs && !sporadic_inserted && contingent){ //then the sporadic event happened when we were performing our last activity 
					sporadic_inserted = true;
					int slackCon = dtp.getMaxSlack().get(0);
					if(slackCon >= DUR && slackCon < Integer.MAX_VALUE) {
						
						systemTime = systemTime + DUR;
						dtp.advanceToTime(-systemTime, DUR, false);
						dtp.simplifyMinNetIntervals();
						
						minDTP.advanceToTime(-systemTime, DUR, false);
						minDTP.simplifyMinNetIntervals();
						
						continue;
					}
					else{
						// we've failed.
				
						String str = "" + id +" FAIL " + contingent + "\n";
						try {
							f.write(str);
							f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
							f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
							a.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						return false;
					}
				}
				
				
				List<String> currActs = activities.get(0);
	
				String a_str = id + ","+systemTime+", offered: " + currActs.size()+", min offered: " + actsMin + "\n";
				total_acts += activities.get(0).size();
				try {
					a.write(a_str);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				String choice = getUserChoice(currActs, actOrder);
	
				IntervalSet interval = dtp.getInterval(choice+"_S", choice+"_E").inverse().subtract(zeroInterval);
				if(interval.totalSize() != 1){ 
					time = getRandomTimeInInterval(interval, rn);
					System.out.println("Performing "+choice+" for "+interval.toString());

				}else{
					time = (int) interval.getLowerBound();
					System.out.println("Performing "+choice+" for "+interval.toString());
				}
				
				dtp.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				minDTP.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				numActsPerformed++;
				systemTime+=time;
				dtp.simplifyMinNetIntervals();
				minDTP.simplifyMinNetIntervals();
				justIdled = false;
				
				if(systemTime > contingentOccurs && !sporadic_inserted &&contingent){ //then the sporadic event happened when we were performing our last activity 
					sporadic_inserted = true;
					int slackCon = dtp.getMaxSlack().get(0);					
					
					if(slackCon >= DUR && slackCon < Integer.MAX_VALUE) {
						//System.out.println("slack here is: "+ slackCon);
						//System.out.println("INSERTING THE CONTINGENT ACTIVITY");
						systemTime = systemTime + DUR;
						dtp.advanceToTime(-systemTime, DUR, false);
						dtp.simplifyMinNetIntervals();
						
						minDTP.advanceToTime(-systemTime, DUR, false);
						minDTP.simplifyMinNetIntervals();
						
						continue;
					}
					else{
						// we've failed.
						
						String str = "" + id +" FAIL " + contingent + "\n";
						try {
							f.write(str);
							f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
							f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
							a.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						return false;
					}
				}
			}
			
		}
		//System.out.println("Returning FALSE in the outer layer. MAY NOT BE CORRECT!");
		return false;
	}
	
	public static boolean stepThroughMIN(DisjunctiveTemporalProblem dtp, int id, FileWriter f, boolean contingent, int contingentOccurs, List<String> actOrder, List<Double> idleAmt, String act_out){
		boolean sporadic_inserted = false;
		//System.out.println("DTP id: "+ id);
		int systemTime = 0;
		int time;
		int total_acts = 0;
		
		int actsMin = 0;
		int idleMin = 0;
		List<List<String>> minActs = null;
		DisjunctiveTemporalProblem minDTP = readFromFile(PATH+folder+"/dtp"+id+".data");
		
		
		FileWriter a = null;
		try {
			a = new FileWriter(PATH+folder+"/"+act_out, true);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		int numActsPerformed = 0;
		Random rn = new Random();
		Interval zeroInterval = new Interval(0,0);

		boolean runningFlag = true;
		boolean justIdled = false;
		
		//DisjunctiveTemporalProblem minDTP = dtp.clone();
		dtp.updateInternalData(); // i don't think it hurts to throw this in here jic
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		
		Generics.printDTP(dtp);
		
		minDTP.updateInternalData();
		minDTP.enumerateSolutions(0);
		//System.out.println("enumerated solutions");
		minDTP.simplifyMinNetIntervals();
		//System.out.println("simplified min net intervals");
		
		while(runningFlag){
			//Generics.printDTP(dtp);
			int minTime = dtp.getMinTime();
			if(minTime > 24*60) { 
				if(dtp.getFixedTimepoints().size() < (dtp.getTimepoints().size() - 1)){
					//if we haven't fixed all the timepoints, not all activities have been performed. 
					String str = "" + id +" FAIL " + contingent + "\n";
					try {
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed +"\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}else{
					int diff = L - systemTime;
					//System.out.println("Inserting " + diff + " idle that was unused at end of day.");
					String str2 = "" + id + ","+systemTime+","+diff+","+diff+","+diff + "," + diff+"\n";
					String str = "" + id + " PASS " + contingent + "\n";
					try {
						f.write(str2);
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed + "\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
			else if(minTime < systemTime){

				String str = "" + id +" FAIL " + contingent + "\n";
				try {
					f.write(str);
					f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
					f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
					a.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			else if(minTime > systemTime){
				//System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
				//This is inserting a forced idle. 
				int temp = minTime - systemTime;
				
				String out_str = id+","+systemTime+",0,0,0," + temp + "\n";
				try {
					f.write(out_str);
				} catch (IOException e) {
					e.printStackTrace();
				}
				systemTime = minTime;
				
				dtp.advanceToTime(-systemTime, temp, false);
				dtp.simplifyMinNetIntervals();
				
				minDTP.advanceToTime(-systemTime, temp, false);
				minDTP.simplifyMinNetIntervals();
				
				continue;
			}
			//Prompt user for their activity selection	
			
			List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -systemTime);
			if(activities.get(0).size() == 0) runningFlag = false;
			List<Integer> slack = new ArrayList<Integer>();
			
			//here we want to get the slack Interval with an upper and lower bound 
			
			slack = dtp.getMinSlack(-systemTime);
			SimpleEntry<Integer, Integer> slack_interval = dtp.getMinSlackInterval(-systemTime);
			int slack_lb = slack_interval.getKey();
			int slack_ub = slack_interval.getValue();
			if(!justIdled) {
				
				DisjunctiveTemporalProblem tempDTP = minDTP.clone();
				System.out.println("getting min slack");
				int minSlack = tempDTP.getMinSlack(-systemTime).get(0);
				System.out.println("finished getting min slack");
				//System.out.println("System time: " + systemTime + " minSlack: " + minSlack + " originalSlack: " + slack.get(0));
				idleMin = minSlack;
				int tempTime = systemTime + minSlack;
				tempDTP.advanceToTime(-tempTime, minSlack, true);
				tempDTP.simplifyMinNetIntervals();
				minActs = tempDTP.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -tempTime);
				actsMin = minActs.get(0).size();
				List<Integer> DTPminSlack = minDTP.getMinSlack(-systemTime);

				if(slack.get(0) == 0){
					String str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+"0,0";
					try {
						f.write(str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					justIdled = true; 
					continue;
				}
				else{
					//if idle is available, the user always idles first. 
					//System.out.println("Max slack: "+ slack.get(0));
					//System.out.println("Available idle: " + slack.get(0));
					
					//for random idle, we now want to take a random amount between the two values. 
					if(randomIdle) time = slack_lb + (int) ((slack_ub - slack_lb) * idleAmt.get(numActsPerformed));
					else if(distributedIdle){
						//time = (int) (slack.get(0) * (1.0 /(A - numActsPerformed)));
						int dist =  (int) Math.ceil((slack.get(0) * (1.0 /(A - numActsPerformed))));
						//System.out.println("Distributed idle amount: " + time + " original slack: " + slack.get(0));
						if(dist < slack_lb) time = slack_lb;
						else if(dist < slack_ub) time = dist;
						else time = slack_ub; //dist is greater than slack_ub
						
					}
					else time = slack_lb;
					String out_str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+ slack.get(0)+","+time;
					try {
						f.write(out_str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					//System.out.println("idling for "+ time);
					systemTime+= time;
					dtp.advanceToTime(-systemTime, time, true);
					dtp.simplifyMinNetIntervals();
					
					minDTP.advanceToTime(-systemTime, time, true);
					minDTP.simplifyMinNetIntervals();
					
					justIdled = true;
					continue;
				}
			} else { //we did just idle. 
				// need to check for the occurrence of the sporadic event before continuing
				
				if(systemTime > contingentOccurs && !sporadic_inserted && contingent){ //then the sporadic event happened when we were performing our last activity 
					sporadic_inserted = true;
					int slackCon = dtp.getMaxSlack().get(0);
					if(slackCon >= DUR && slackCon < Integer.MAX_VALUE) {
						//System.out.println("WE JUST IDLED. SE HAPPENED THEN");
						//System.out.println("slack here is: "+ slackCon);
						//System.out.println("INSERTING THE SPORADIC EVENT");
						systemTime = systemTime + DUR;
						dtp.advanceToTime(-systemTime, DUR, false);
						dtp.simplifyMinNetIntervals();
						
						minDTP.advanceToTime(-systemTime, DUR, false);
						minDTP.simplifyMinNetIntervals();
						
						continue;
					}
					else{
						// we've failed.
						//System.out.println("tried to insert contingent and failed at time: " + systemTime);
						//System.out.println("sporadic event was triggered at time: " + contingentOccurs);
						String str = "" + id +" FAIL " + contingent + "\n";
						try {
							f.write(str);
							f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
							f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
							a.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						return false;
					}
				}
				
				
				List<String> currActs = activities.get(0);
	
				String a_str = id + ","+systemTime+", offered: " + currActs.size()+", min offered: " + actsMin + "\n";
				total_acts += activities.get(0).size();
				try {
					a.write(a_str);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String choice = getUserChoice(currActs, actOrder);
	
				IntervalSet interval = dtp.getInterval(choice+"_S", choice+"_E").inverse().subtract(zeroInterval);
				if(interval.totalSize() != 1){ 
					time = getRandomTimeInInterval(interval, rn);
					System.out.println("Performing "+choice+" for "+interval.toString());

				}else{
					time = (int) interval.getLowerBound();
					System.out.println("Performing "+choice+" for "+interval.toString());
				}
				
				dtp.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				minDTP.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				numActsPerformed++;
				systemTime+=time;
				dtp.simplifyMinNetIntervals();
				minDTP.simplifyMinNetIntervals();
				justIdled = false;
				
				if(systemTime > contingentOccurs && !sporadic_inserted &&contingent){ //then the sporadic event happened when we were performing our last activity 
					sporadic_inserted = true;
					int slackCon = dtp.getMaxSlack().get(0);
					
					//add in a case here for when the problem type we're working with is a DUTP? how do we check/know this? 
					
					
					if(slackCon >= DUR && slackCon < Integer.MAX_VALUE) {
						//System.out.println("slack here is: "+ slackCon);
						//System.out.println("INSERTING THE CONTINGENT ACTIVITY");
						systemTime = systemTime + DUR;
						dtp.advanceToTime(-systemTime, DUR, false);
						dtp.simplifyMinNetIntervals();
						
						minDTP.advanceToTime(-systemTime, DUR, false);
						minDTP.simplifyMinNetIntervals();
						
						continue;
					}
					else{
						// we've failed.
						//System.out.println("tried to insert contingent and failed at time: " + systemTime);
						//System.out.println("sporadic event was triggered at time: " + contingentOccurs);
						String str = "" + id +" FAIL " + contingent + "\n";
						try {
							f.write(str);
							f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
							f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
							a.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						return false;
					}
				}
			}
			
		}
		//System.out.println("Returning FALSE in the outer layer. MAY NOT BE CORRECT!");
		
		
		
		return false;
	}
	
	/**
	 * Returns a set of half slacks, those that fall halfway between min and max slacks
	 * @param maxSlack the list of maxSlacks
	 * @param minSlack list of minSlacks
	 * @return list containing slack values in between min and max slack lists above.
	 */
	public static List<Integer> getHalfSlacks(List<Integer> maxSlack, List<Integer> minSlack){
		List<Integer> out = new ArrayList<Integer>();
		for(int i = 0; i < maxSlack.size(); i++){
			out.add((int) ((maxSlack.get(i) - minSlack.get(i)) / 2.0) + minSlack.get(i));
		}
		return out;
	}
	
	public static boolean stepThroughPutAtEnd(DisjunctiveTemporalProblem dtp, int id, FileWriter f, boolean contingent, int contingentOccurs,  List<String> actOrder, List<Double> idleAmt, String act_out){
		int systemTime = 0;
		int time = 0;;
		int numActsPerformed = 0;
		int total_acts = 0;
		
		boolean sporadic_inserted = false;
		
		Random rn = new Random();
		Interval zeroInterval = new Interval(0,0);
		boolean runningFlag = true;
		boolean justIdled = false;
		int actsMin = 0;
		List<List<String>> minActs = null;
		int idleMax = 0;
		
		DisjunctiveTemporalProblem minDTP = readFromFile(PATH+folder+"/dtp"+id+".data");
		
		FileWriter a = null;
		try {
			a = new FileWriter(PATH+folder+"/" + act_out, true);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		DisjunctiveTemporalProblem originalDTP = dtp;
		
		DisjunctiveTemporalProblem currdtp = createPutAtEnd(dtp);
		
		
		originalDTP.updateInternalData();
		originalDTP.enumerateSolutions(0);
		originalDTP.simplifyMinNetIntervals();
		
		currdtp.updateInternalData();
		currdtp.enumerateSolutions(0);
		currdtp.simplifyMinNetIntervals();
		//Generics.printDTP(currdtp);
		minDTP.updateInternalData();
		minDTP.enumerateSolutions(0);
		minDTP.simplifyMinNetIntervals();
		
		while(runningFlag){
	
			//System.out.println("\nCurrent Time: "+Generics.toTimeFormat(systemTime));
			int minTime = currdtp.getMinTime();
			if(minTime > 24*60 || (minTime == 58 && !sporadic_inserted)) {
				
				
				if(!sporadic_inserted && minTime == 58){
					currdtp = originalDTP;
				}

				if(currdtp.getFixedTimepoints().size() < (currdtp.getTimepoints().size() - 1)){
					//if we haven't fixed all the timepoints, not all activities have been performed. 
					String str = "" + id +" FAIL " + contingent + "\n";
					System.out.println("Failed because not all tps were fixed");
					Generics.printDTP(currdtp);
					System.out.println("original DTP:");
					Generics.printDTP(originalDTP);
					try {
						f.write(str);
						f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
						f.write("Calls to Solver: "+(currdtp.getCallsToSolver()+originalDTP.getCallsToSolver())+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}else{
					int diff = L - systemTime;
					//System.out.println("Inserting " + diff + " idle that was unused at end of day.");
					String str2 = "" + id + ","+systemTime+","+diff+","+diff+","+diff + "," + diff+"\n";
					String str = "" + id + " PASS " + contingent + "\n";
					try {
						f.write(str2);
						f.write(str);
						f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
						f.write("Calls to Solver: "+(currdtp.getCallsToSolver()+originalDTP.getCallsToSolver())+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
			else if(minTime < systemTime){
				//throw new Error("minTime("+minTime+") < systemTime("+systemTime+")");
				
				String str = "" + id +" FAIL " + contingent + "\n";
				System.out.println("failed bc minTime < systemTime");
				System.out.println("Error: minTime("+minTime+") < systemTime("+systemTime+")");
				Generics.printDTP(currdtp);
				try {
					f.write(str);
					f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
					f.write("Calls to Solver: "+(currdtp.getCallsToSolver()+originalDTP.getCallsToSolver())+"\n");
					a.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			else if(minTime > systemTime){
				//System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
				//This is inserting a forced idle. 
				
				int temp = minTime - systemTime;
				
				String out_str = id+","+systemTime+",0,0,0,"+temp+"\n";
				try {
					f.write(out_str);
				} catch (IOException e) {
					e.printStackTrace();
				}
				systemTime = minTime;
				currdtp.advanceToTime(-systemTime, temp, false);
				if(!currdtp.equals(originalDTP)){
					//System.out.println("currdtp is different from orig");
					originalDTP.advanceToTime(-systemTime, temp, false);
					originalDTP.simplifyMinNetIntervals();
				}
				currdtp.simplifyMinNetIntervals();
				minDTP.advanceToTime(-systemTime, temp, false);
				minDTP.simplifyMinNetIntervals();
				
				continue;
			}
			
			//Prompt user for their activity selection				
			List<List<String>> activities = currdtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -systemTime);
			if(activities.get(0).size() == 0) runningFlag = false;
			List<Integer> slack = currdtp.getMaxSlack();
			//List<Integer> slack = getHalfSlacks(currdtp.getMaxSlack(), currdtp.getMinSlack(-systemTime));
			//List<Integer> slack = currdtp.getMinSlack(-systemTime);
			//List<Integer> slack = new ArrayList<Integer>(); //PACKING
			//slack.add(0);
			if(!justIdled){
				//System.out.println("we didn't just idle");
				//System.out.println(slack);
				//System.out.println("systemTime before min calc is : " + systemTime);
				DisjunctiveTemporalProblem tempDTP = minDTP.clone();
				//System.out.println("getting minslack");
				int minSlack = tempDTP.getMinSlack(-systemTime).get(0);
				//System.out.println("finished minslack");
				//System.out.println("System time: " + systemTime + " minSlack: " + minSlack + " originalSlack: " + slack.get(0));
				int idleMin = minSlack;
				int tempTime = systemTime + minSlack;
				tempDTP.advanceToTime(-tempTime, minSlack, true);
				tempDTP.simplifyMinNetIntervals();
				minActs = tempDTP.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -tempTime);
				actsMin = minActs.get(0).size();
				//System.out.println("minACts is offering: " + actsMin);
				//Generics.printDTP(tempDTP);
				List<Integer> minDTPSlack = minDTP.getMinSlack(-systemTime);
				
				if(slack.get(0) == 0){
					//System.out.println("no idle available");
					//we still want to output if there's no idle available i guess? 
					String out_str = id+","+systemTime+ "," + minDTPSlack.get(0) + "," + currdtp.getMaxSlack().get(0)+",0,0";
					try {
						f.write(out_str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					justIdled = true;
					continue;
				}
				else{

					if(randomIdle) time = (int) (slack.get(0)*idleAmt.get(numActsPerformed));
					else if(distributedIdle){
						time =  (int) Math.ceil((slack.get(0) * (1.0 /(A - numActsPerformed))));
						//System.out.println("distributed idle amt: " + time + " original slack: "+ slack.get(0));
					}
					else time = slack.get(0);
					//if(slack.get(0) > 0) time = 1;
					//time = 0;
					String out_str = id + "," +systemTime+ ","+ minDTPSlack.get(0)+","+minDTP.getMaxSlack().get(0)+","+ slack.get(0)+","+time;
					try {
						f.write(out_str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					//System.out.println("idling for "+ time);
					systemTime+= time;
					currdtp.advanceToTime(-systemTime, time, true);
					currdtp.simplifyMinNetIntervals();

					minDTP.advanceToTime(-systemTime, time, true);
					minDTP.simplifyMinNetIntervals();

					if(!currdtp.equals(originalDTP)){
						//System.out.println("curr dtp and originaldtp are not the same.");
						originalDTP.advanceToTime(-systemTime, time, true);
						originalDTP.simplifyMinNetIntervals();
					}
					
					justIdled = true;
					continue;
				}
			}else{ // we did just idle so now we perform an activity 
				
				//first we check if SE occurred during idle
				if(systemTime > contingentOccurs && !sporadic_inserted && contingent){
					//System.out.println("INSERTING CONTINGENT ACT after idle");
					sporadic_inserted = true;
					//Generics.printDTP(currdtp);
					int slackCon = originalDTP.getMaxSlack().get(0);
					//Generics.printDTP(originalDTP);
					//System.out.println("MAX slack: "+slackCon);
					if(slackCon >=DUR) {
						systemTime = systemTime + DUR;
						currdtp = originalDTP;
						currdtp.advanceToTime(-systemTime, DUR, false);
						currdtp.simplifyMinNetIntervals();
						
						minDTP.advanceToTime(-systemTime, DUR, false);
						minDTP.simplifyMinNetIntervals();
						justIdled = false;
						//Generics.printDTP(currdtp);
						continue;
					}
					else{
						// we've failed.
						System.out.println("Failed inserting SE after idle");
						Generics.printDTP(currdtp);
						String str = "" + id +" FAIL "+ contingent + "\n";
						try {
							f.write(str);
							f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
							f.write("Calls to Solver: "+(currdtp.getCallsToSolver()+originalDTP.getCallsToSolver())+"\n");
							a.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return false;
					}
				}

				
				List<String> currActs = activities.get(0);
				
					
				if(currActs.size() == 1 && currActs.get(0).equals("sporadic")){
					//we don't want to perform the sporadic activity.
					//TODO: should this switch back to originalDTP or force to idle the 20 min? 
					// answer: it needs to force 20 min idle in originalDTP.
					systemTime = systemTime + DUR;
					int temp = systemTime - DUR;
					//we aren't considering this forced anymore. 
					String out_str = id+","+temp+","+DUR+","+DUR+"," + DUR + "," + DUR+"\n";
					try {
						f.write(out_str);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					currdtp = originalDTP.clone();
					currdtp.advanceToTime(-systemTime, DUR, false);
					currdtp.simplifyMinNetIntervals();
					
					//now update the min DTP

					minDTP.advanceToTime(-systemTime, DUR, true);
					minDTP.simplifyMinNetIntervals();
					justIdled = false;
					continue;
					
				}
				
				String a_str = id + ","+systemTime+", offered: " + currActs.size()+", min offered: " + actsMin + "\n";
				//System.out.println("writing activity information before trying");
				try {
					//System.out.println("writing activity information");
					a.write(a_str);
				} catch (IOException e1) {
					//System.out.println("writing activity information in catch block");
					e1.printStackTrace();
				}
				
				String choice = getUserChoice(currActs, actOrder);
				//System.out.println("USER CHOICE: " + choice);
				total_acts += currActs.size();
				IntervalSet interval = currdtp.getInterval(choice+"_S", choice+"_E").inverse().subtract(zeroInterval);
				if(interval.totalSize() != 1){ 
					time = getRandomTimeInInterval(interval, rn);
					//forced idle is possible
					
				
					//System.out.println("Performing "+choice+" for "+ Generics.toTimeFormat(time));
				}else{
					time = (int) interval.getLowerBound();
					//System.out.println("Performing "+choice+" for "+interval.toString());
				}
				currdtp.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				
				currdtp.simplifyMinNetIntervals();
				
				minDTP.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				minDTP.simplifyMinNetIntervals();
				
				if(!currdtp.equals(originalDTP)){
					//System.out.println("currdtp and original not the same");
					originalDTP.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
					originalDTP.simplifyMinNetIntervals();
				}
				numActsPerformed++;
				systemTime+=time;
				justIdled = false;
				
				if(systemTime > contingentOccurs && !sporadic_inserted && contingent){
					//System.out.println("INSERTING CONTINGENT ACT");
					sporadic_inserted = true;
					int slackCon = originalDTP.getMaxSlack().get(0);
					//Generics.printDTP(originalDTP);
					//System.out.println("MAX slack: "+slackCon);
					if(slackCon >= DUR) {
						systemTime = systemTime + DUR;
						currdtp = originalDTP;
						currdtp.advanceToTime(-systemTime, DUR, false);
						currdtp.simplifyMinNetIntervals();
						
						minDTP.advanceToTime(-systemTime, DUR, false);
						minDTP.simplifyMinNetIntervals();
						//justIdled = false;
						continue;
					}
					else{
						// we've failed.
						System.out.println("Failed inserting SE after a req activity");
						Generics.printDTP(currdtp);
						String str = "" + id +" FAIL "+ contingent + "\n";
						try {
							f.write(str);
							f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
							f.write("Calls to Solver: "+(currdtp.getCallsToSolver()+originalDTP.getCallsToSolver())+"\n");
							a.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return false;
					}
				}
				continue;
				
			}
		}
		//System.out.println("Returning FALSE in the outer layer. MAY NOT BE CORRECT!");
		return false;
		
	}
	
	
	public static boolean stepThroughPreemptive(DisjunctiveTemporalProblem dtp, int id, FileWriter f, boolean contingent, int contingentOccurs, 
			List<String> actOrder, List<Double> idleAmt, String type, String act_out){
		boolean sporadic_inserted = false;
		//System.out.println("DTP id: "+ id);
		int systemTime = 0;
		int time;
		int total_acts = 0;
		boolean inserted = false;
		int actsMin = 0;
		int idleMin = 0;
		List<List<String>> minActs = null;
		DisjunctiveTemporalProblem minDTP = readFromFile(PATH+folder+"/dtp"+id+".data");
		
		FileWriter a = null;
		try {
			a = new FileWriter(PATH+folder+"/"+act_out, true);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		int numActsPerformed = 0;
		Random rn = new Random();
		Interval zeroInterval = new Interval(0,0);

		boolean runningFlag = true;
		boolean justIdled = false;
		
		dtp.updateInternalData(); // i don't think it hurts to throw this in here jic
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		
		Generics.printDTP(dtp);
		
		minDTP.updateInternalData();
		minDTP.enumerateSolutions(0);
		//System.out.println("enumerated solutions");
		minDTP.simplifyMinNetIntervals();
		//System.out.println("simplified min net intervals");
		
		while(runningFlag){
			//Generics.printDTP(dtp);
			int minTime = dtp.getMinTime();
			if(minTime > 24*60) { 
				if(dtp.getFixedTimepoints().size() < (dtp.getTimepoints().size() - 1)){
					//if we haven't fixed all the timepoints, not all activities have been performed. 
					String str = "" + id +" FAIL " + contingent + "\n";
					try {
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed +"\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}else{
					int diff = L - systemTime;
					//System.out.println("Inserting " + diff + " idle that was unused at end of day.");
					String str2 = "" + id + ","+systemTime+","+diff+","+diff+","+diff + "," + diff+"\n";
					String str = "" + id + " PASS " + contingent + "\n";
					try {
						f.write(str2);
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed + "\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
			else if(minTime < systemTime){

				String str = "" + id +" FAIL " + contingent + "\n";
				try {
					f.write(str);
					f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
					f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
					a.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			else if(minTime > systemTime){
				//System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
				//This is inserting a forced idle. 
				int temp = minTime - systemTime;
				
				String out_str = id+","+systemTime+",0,0,0," + temp + "\n";
				try {
					f.write(out_str);
				} catch (IOException e) {
					e.printStackTrace();
				}
				systemTime = minTime;
				
				dtp.advanceToTime(-systemTime, temp, false);
				dtp.simplifyMinNetIntervals();
				
				minDTP.advanceToTime(-systemTime, temp, false);
				minDTP.simplifyMinNetIntervals();
				
				continue;
			}
			//Prompt user for their activity selection	
			
			List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -systemTime);
			if(activities.get(0).size() == 0) runningFlag = false;
			List<Integer> slack = new ArrayList<Integer>();
			
			//the slack varies based on which type we have 
			if(type.equals("MIN")) slack = dtp.getMinSlack(-systemTime);
			else if (type.equals("MAX")) slack = dtp.getMaxSlack();
			else if (type.equals("HALF")) slack = getHalfSlacks(dtp.getMaxSlack(), dtp.getMinSlack(-systemTime));
			else if (type.equals("PACKING")) slack.add(0);
			//else System.out.println("ERROR: UNKNOWN PROBLEM TYPE");

			if(!justIdled) {
				
				DisjunctiveTemporalProblem tempDTP = minDTP.clone();
				System.out.println("getting min slack");
				int minSlack = tempDTP.getMinSlack(-systemTime).get(0);
				System.out.println("finished getting min slack");
				//System.out.println("System time: " + systemTime + " minSlack: " + minSlack + " originalSlack: " + slack.get(0));
				idleMin = minSlack;
				int tempTime = systemTime + minSlack;
				tempDTP.advanceToTime(-tempTime, minSlack, true);
				tempDTP.simplifyMinNetIntervals();
				minActs = tempDTP.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -tempTime);
				actsMin = minActs.get(0).size();
				List<Integer> DTPminSlack = minDTP.getMinSlack(-systemTime);

				//at the first idle timepoint we should insert the preemptive activity
				if(!inserted){
					if(slack.get(0) < DUR) System.out.println("SOMETHING WRONG. don't have enough slack in preemptive: " + slack.get(0));
					//insert preemptive activity as a forced idle 
					String str = id + "," + systemTime + "," + DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+"0," + DUR;
					try {
						f.write(str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					justIdled = true; 
					inserted = true;
					sporadic_inserted = true;
					System.out.println("inserted sporadic event!!");
					time = DUR;
					systemTime+= time;
					dtp.advanceToTime(-systemTime, time, true);
					dtp.simplifyMinNetIntervals();
					
					minDTP.advanceToTime(-systemTime, time, true);
					minDTP.simplifyMinNetIntervals();
					Generics.printDTP(dtp);
					continue;
				}
				
				if(slack.get(0) == 0){
					String str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+"0,0";
					try {
						f.write(str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					justIdled = true; 
					continue;
				}
				else{
					//if idle is available, the user always idles first. 
					//System.out.println("Max slack: "+ slack.get(0));
					//System.out.println("Available idle: " + slack.get(0));
					if(randomIdle) time = (int) (slack.get(0) * idleAmt.get(numActsPerformed));
					else if(distributedIdle){
						//time = (int) (slack.get(0) * (1.0 /(A - numActsPerformed)));
						time =  (int) Math.ceil((slack.get(0) * (1.0 /(A - numActsPerformed))));
						//System.out.println("Distributed idle amount: " + time + " original slack: " + slack.get(0));
					}
					else time = slack.get(0);
					time = 0;
					String out_str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+ slack.get(0)+","+time;
					try {
						f.write(out_str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					//System.out.println("idling for "+ time);
					systemTime+= time;
					dtp.advanceToTime(-systemTime, time, true);
					dtp.simplifyMinNetIntervals();
					
					minDTP.advanceToTime(-systemTime, time, true);
					minDTP.simplifyMinNetIntervals();
					
					justIdled = true;
					continue;
				}
			} else { //we did just idle. 
				// need to check for the occurrence of the sporadic event before continuing
					List<String> currActs = activities.get(0);
	
				String a_str = id + ","+systemTime+", offered: " + currActs.size()+", min offered: " + actsMin + "\n";
				total_acts += activities.get(0).size();
				try {
					a.write(a_str);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String choice = getUserChoice(currActs, actOrder);
	
				IntervalSet interval = dtp.getInterval(choice+"_S", choice+"_E").inverse().subtract(zeroInterval);
				if(interval.totalSize() != 1){ 
					time = getRandomTimeInInterval(interval, rn);
					System.out.println("Performing "+choice+" for "+interval.toString());

				}else{
					time = (int) interval.getLowerBound();
					System.out.println("Performing "+choice+" for "+interval.toString());
				}
				
				dtp.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				minDTP.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				numActsPerformed++;
				systemTime+=time;
				dtp.simplifyMinNetIntervals();
				minDTP.simplifyMinNetIntervals();
				justIdled = false;
				
			}
			
		}
		//System.out.println("Returning FALSE in the outer layer. MAY NOT BE CORRECT!");

		return false;
	}
	
	/**
	 * Helper function to get a user's choice given a priority ordering over activities. The activity in the available activities 
	 * that occurs earlier in the list is returned
	 * @param activities list of available activities to consider
	 * @param order priority ordering over all activities in the scheduling problem.
	 * @return the activity the user selects based on order.
	 */
	private static String getUserChoice(List<String> activities, List<String> order){
		int len = activities.size();
		String min_str = "";
		int min_ind = 10;
		for(String act : activities){
			int ind = order.indexOf(act);
			if(ind < min_ind){
				min_ind = ind;
				min_str = act;
			}
		}
		
		return min_str;
	}
	
	/**
	 * Helper function that returns a random time within an interval. Used when variable durations are available.
	 * @param interval, the IntervalSet to get a random time from within
	 * @param rn, the random generator to use
	 * @return a randomly selected integer time that falls within interval
	 */
	//does this handle disjunct intervals well? 
	private static int getRandomTimeInInterval(IntervalSet interval, Random rn){
		int lb = (int) interval.getLowerBound();
		int ub = (int) interval.getUpperBound();
		
		int val = -1;
		while(!interval.intersect(val)){
			val = rn.nextInt(ub-lb) + lb;
		}
		
		return val;
	}
	
	/**
	 * Helper function, not currently used
	 * @param original list of Strings
	 * @return permutations of items in the original list. 
	 */
	public static List<List<String>> generatePerm(List<String> original) {
	     if (original.size() == 0) { 
	       List<List<String>> result = new ArrayList<List<String>>();
	       result.add(new ArrayList<String>());
	       return result;
	     }
	     String firstElement = original.remove(0);
	     List<List<String>> returnValue = new ArrayList<List<String>>();
	     List<List<String>> permutations = generatePerm(original);
	     for (List<String> smallerPermutated : permutations) {
	       for (int index=0; index <= smallerPermutated.size(); index++) {
	         List<String> temp = new ArrayList<String>(smallerPermutated);
	         temp.add(index, firstElement);
	         returnValue.add(temp);
	       }
	     }
	     return returnValue;
	   }
	
	/*
	 * Updated Step Through function. Accepts Type values of MAX, MIN, HALF, and PACKING
	 * dtp: problem we're working with
	 * id: id of the problem we're working with
	 * f: output fil 
	 * contingent: true if contingent event occurs, false otherwise
	 * int contingentOccurs: minute in the day that the sporadic event occurs
	 * actOrder: order in which activities will be performed in the case of multiple offerings
	 * idleAmt: amount of idle we want to take each time it is offered. 
	 * String act_out: file name to write activity data output to 
	 */
	public static boolean stepThroughDUTPWithOutput(DisjunctiveTemporalProblem dtp, int id, FileWriter f, boolean contingent, int contingentOccurs, List<String> actOrder, List<Double> idleAmt, String act_out){
		boolean sporadic_inserted = false;
		//System.out.println("DTP id: "+ id);
		int systemTime = 0;
		int time;
		int total_acts = 0;
		boolean first_decision_pt = false;
		int actsMin = 0;
		int idleMin = 0;
		List<List<String>> minActs = null;
		DisjunctiveTemporalProblem minDTP = readFromFile(PATH+folder+"/dtp"+id+".data");
		//DisjunctiveTemporalProblem minDTP = new ProblemLoader().loadDTPFromFile("/home/lynngarrett/research/maDTPRepo/MaDTP/branches/Lynn/trunk/toyexampleDTP.xml");
		
		FileWriter a = null;
		try {
			a = new FileWriter(PATH+folder+"/"+act_out, true);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		int numActsPerformed = 0;
		Random rn = new Random();
		Interval zeroInterval = new Interval(0,0);

		boolean runningFlag = true;
		boolean justIdled = false;
		
		//DisjunctiveTemporalProblem minDTP = dtp.clone();
		dtp.updateInternalData(); // i don't think it hurts to throw this in here jic
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		
		//Generics.printDTP(dtp);
		
		minDTP.updateInternalData();
		minDTP.enumerateSolutions(0);
		//System.out.println("enumerated solutions");
		minDTP.simplifyMinNetIntervals();
		//System.out.println("simplified min net intervals");
		
		while(runningFlag){
			//Generics.printDTP(dtp);
			int minTime = dtp.getMinTime();
			if(minTime > 24*60) { 
				//System.out.println("mintime> 24*60 ");
				if(dtp.getFixedTimepoints().size() < (dtp.getTimepoints().size() - 1)){
					//if we haven't fixed all the timepoints, not all activities have been performed. 
					
					//System.out.println("Didn't fix all timepoints");
					String str = "" + id +" FAIL " + contingent + "\n";
					try {
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed +"\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}else{
					int diff = L - systemTime;
					//System.out.println("Inserting " + diff + " idle that was unused at end of day.");
					String str2 = "" + id + ","+systemTime+","+diff+","+diff+","+diff + "," + diff+"\n";
					String str = "" + id + " PASS " + contingent + "\n";
					try {
						f.write(str2);
						f.write(str);
						f.write("Total activities offered: "+ total_acts + " performed: "+ numActsPerformed + "\n");
						f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
						a.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
			else if(minTime < systemTime){
				//System.out.println("MINTIME<SYSTEMTIME");
				//System.out.println(minTime);
				String str = "" + id +" FAIL " + contingent + "\n";
				try {
					f.write(str);
					f.write("Total activities offered: "+ total_acts +" performed: "+ numActsPerformed +"\n");
					f.write("Calls to Solver: "+dtp.getCallsToSolver()+"\n");
					a.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			else if(minTime > systemTime){
				//System.out.println("\nNo activities available until "+Generics.toTimeFormat(minTime)+". Idling until then.");
				//This is inserting a forced idle. 
				String out_str = "";
				int temp = minTime - systemTime;
				if(contingent && sporadic_inserted && first_decision_pt){
					first_decision_pt = false;
					out_str = id+","+systemTime+",-1,-1,-1," + temp + "\n";
				}else out_str = id+","+systemTime+",0,0,0," + temp + "\n";
				try {
					f.write(out_str);
				} catch (IOException e) {
					e.printStackTrace();
				}
				systemTime = minTime;
				
				dtp.advanceToTime(-systemTime, temp, false);
				dtp.simplifyMinNetIntervals();
				
				minDTP.advanceToTime(-systemTime, temp, false);
				minDTP.simplifyMinNetIntervals();
				
				continue;
			}
			//Prompt user for their activity selection	
			
			//List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -systemTime);
			//if(activities.get(0).size() == 0) runningFlag = false;
			List<Integer> slack = new ArrayList<Integer>();
			
			//the slack varies based on which type we have 
			slack = dtp.getMaxSlack();
			//minslack
			//slack = dtp.getMinSlack(-systemTime);
			//slack.add(0); //PACKING
			first_decision_pt = false;
			if(!justIdled) {
				
				DisjunctiveTemporalProblem tempDTP = minDTP.clone();
				System.out.println("getting min slack");
				int minSlack = tempDTP.getMinSlack(-systemTime).get(0);
				System.out.println("System time: " + systemTime + " minSlack: " + minSlack + " originalSlack: " + slack.get(0));
				idleMin = minSlack;
				int tempTime = systemTime + minSlack;
				tempDTP.advanceToTime(-tempTime, minSlack, true);
				tempDTP.simplifyMinNetIntervals();
				minActs = tempDTP.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -tempTime);
				actsMin = minActs.get(0).size();
				List<Integer> DTPminSlack = minDTP.getMinSlack(-systemTime);

				if(slack.get(0) == 0){
					String str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+dtp.getMaxSlack().get(0)+","+"0,0";
					try {
						f.write(str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					justIdled = true; 
					continue;
				}
				else{
					//if idle is available, the user always idles first. 
					//System.out.println("Max slack: "+ slack.get(0));
					//System.out.println("Available idle: " + slack.get(0));
					if(randomIdle) time = (int) (slack.get(0) * idleAmt.get(numActsPerformed));
					else if(distributedIdle){
						//time = (int) (slack.get(0) * (1.0 /(A - numActsPerformed)));
						time =  (int) Math.ceil((slack.get(0) * (1.0 /(A - numActsPerformed))));
						
						//System.out.println("Distributed idle amount: " + time + " original slack: " + slack.get(0));
					}
					else time = slack.get(0);
					time = 0;
					//if(slack.get(0) > 0) time = 1;
					System.out.println("Slack available is: " + slack.get(0)+ " idling for: "+ time);
					String out_str = id + "," +systemTime+ ","+ DTPminSlack.get(0)+","+minDTP.getMaxSlack().get(0)+","+ slack.get(0)+","+time;
					try {
						f.write(out_str +"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("idling for "+ time);
					systemTime+= time;
					dtp.advanceToTime(-systemTime, time, true);
					dtp.simplifyMinNetIntervals();
					
					minDTP.advanceToTime(-systemTime, time, true);
					minDTP.simplifyMinNetIntervals();
					
					justIdled = true;
					continue;
				}
			} else { //we did just idle. 
				// need to check for the occurrence of the sporadic event before continuing				
				
				if(systemTime > contingentOccurs && !sporadic_inserted && contingent){ //then the sporadic event happened when we were performing our last activity 
					sporadic_inserted = true;
					// the underlying functions in the DUTP/DUSTP that was passed as input should suffice here. 
					dtp.executeAndAdvance(-systemTime, "sporadic_S", -(systemTime+DUR), "sporadic_E", true, DUR, true);
					dtp.simplifyMinNetIntervals();
					//we insert an idle into the minDTP	
					minDTP.advanceToTime(-systemTime, DUR, false);
					minDTP.simplifyMinNetIntervals();
					systemTime= systemTime + DUR;
					first_decision_pt = true;
					continue;
					
				}
				//if SE didn't happen after idling, we need to advance down the tree past the idling node.
				dtp.advanceDownTree();
				System.out.println("getting activities");
				List<List<String>> activities = dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.TIME, -systemTime);
				if(activities.get(0).size() == 0) runningFlag = false;
				
				List<String> currActs = activities.get(0);
	
				String a_str = id + ","+systemTime+", offered: " + currActs.size()+", min offered: " + actsMin + "\n";
				total_acts += activities.get(0).size();
				try {
					a.write(a_str);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String choice = getUserChoice(currActs, actOrder);
	
				IntervalSet interval = dtp.getInterval(choice+"_S", choice+"_E").inverse().subtract(zeroInterval);
				if(interval.totalSize() != 1){ 
					time = getRandomTimeInInterval(interval, rn);
					
				}else{
					time = (int) interval.getLowerBound();
					
				}
				System.out.println("Performing "+choice+" for "+interval.toString());
				dtp.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				minDTP.executeAndAdvance(-systemTime, choice+"_S",-(systemTime+time),choice+"_E",true, time, true);
				numActsPerformed++;
				systemTime+=time;
				dtp.simplifyMinNetIntervals();
				minDTP.simplifyMinNetIntervals();
				justIdled = false;
				
				if(systemTime > contingentOccurs && !sporadic_inserted &&contingent){ //then the sporadic event happened when we were performing our last activity 
					sporadic_inserted = true;
					// the underlying functions in the DUTP/DUSTP that was passed as input should suffice here. 
					dtp.executeAndAdvance(-systemTime, "sporadic_S", -(systemTime+DUR), "sporadic_E", true, DUR, true);
					dtp.simplifyMinNetIntervals();
					systemTime+=DUR;
					//we insert an idle into the minDTP	
					minDTP.advanceToTime(-systemTime, DUR, false);
					minDTP.simplifyMinNetIntervals();
					first_decision_pt = true;
						continue;
					
				}
				
			}
			
		}
		//System.out.println("Returning FALSE in the outer layer. MAY NOT BE CORRECT!");
		return false;
	}
	
	
	
}
