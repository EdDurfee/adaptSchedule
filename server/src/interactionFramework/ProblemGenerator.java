	package interactionFramework;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import stp.TemporalDifference;
import stp.Timepoint;
import dtp.AgentDTP;
import dtp.DisjunctiveTemporalConstraint;
import dtp.DisjunctiveTemporalProblem;
import dtp.SimpleDTP;

/**
 * A class for generating random instances of a DTP.
 * @author lynngarrett
 *
 */

public class ProblemGenerator {

	
	private final static int L = 600;
	private static int D_MIN = 15;
	private static int D_MAX = 100;
	private static int D_FILL = 580;
	private static int id = 0;
	private static final String names = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz";
	
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
	
	private static HashMap<SimpleEntry<String,String>, Integer> precOnes = new HashMap<SimpleEntry<String, String>, Integer>();
	static{
		precTest.put(new SimpleEntry<String, String>("Aa", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Bb", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Cc", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Dd", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Ee", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Ff", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Gg", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Hh", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Ii", "sporadic"), 1);
		precTest.put(new SimpleEntry<String, String>("Jj", "sporadic"), 1);		
	}
	
	private static ArrayList<String> nameList = new ArrayList<String>();
	
	
	
	static{
		for(String letter : names.split(" ")) {
			nameList.add(letter);
		}
	}
	
	public static void updateFillAmount(int amt){
		D_FILL = amt;
	}
	
	
	/**
	 * Helper function that contains a boolean value for whether or not randomly generated constraints 
	 * should be paired.
	 * @param n number of timepoints in the problem
	 * @param m the number of additional random constraints added to the problem
	 * @param numDisjuncts the number of pairs of (subsequent) activities whose ordering constraints are relaxed to nonconcurrency
	 * @param paired boolean value determining whether or not randomly generated constraints are paired or unpaired.
	 * @return
	 */
	public static DisjunctiveTemporalProblem generateStructuredDTP(int n, int m, int numDisjuncts, boolean paired){
		if(paired) return generateStructuredDTPNEW(n,m,numDisjuncts);
		else return generateStructuredDTP(n,m,numDisjuncts);	
	}
	
	
	/**
	 * This function builds a DTP that has n timepoints (n/2 activities), m additional randomly generated constraints, and then relaxes 
	 * numDisjuncts pairs of orderings of activities. This eliminates the nonconcurrency constraints between all activities that leads 
	 * to an explosion in the number of disjuncts to be considered when generating component STPs. 
	 * @param n number of timepoints in the number problem (+ the zero timepoint, not included in the count)
	 * @param m the number of additional random constraints to be added to the generated DTP
	 * @param numDisjuncts the number of pairs of (subsequent) activities whose ordering constraints will be relaxed to non-concurrency constraints.
	 * @return the randomly generated DTP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateStructuredDTP(int n, int m, int numDisjuncts){
		Random rn = new Random();
		Timepoint zero = new Timepoint("zero",1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
		

		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		//ArrayList<SimpleEntry<Integer,Integer>> bounds2 = new ArrayList<SimpleEntry<Integer, Integer>>();
		//for(int i = 0; i < 10; i++) bounds2.add(new SimpleEntry<Integer,Integer>(58,58));
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
		
		//random disjunctive constraints
		for(int i = 0; i < m ; i++){
			//System.out.println("Adding random constraints");
			//first we pick two timepoint variables from the sets of timepoints 
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			//System.out.println("x " + pair.getKey()+" Y " + pair.getValue());
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			int bound = getRandomBound(rn);
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, bound);
			//System.out.println("TD1 " + td1.toString());
			// now we formulate the second part of the disjunction
			pair = getRandomPair(rn, len);
			tp1 = localTimepoints.get(pair.getKey());
			tp2 = localTimepoints.get(pair.getValue());
			bound = getRandomBound(rn);
			//TODO: Make sure these two bounds are not the same if the two pairs of timepoints are the same
			TemporalDifference td2 = new TemporalDifference(tp1, tp2, bound);
			//System.out.println("TD2 " + td2.toString());
			ArrayList<TemporalDifference> tds = new ArrayList<TemporalDifference>();
			tds.add(td1); tds.add(td2);
			localConstraints.add(new DisjunctiveTemporalConstraint(tds));
			
		}
		HashMap<SimpleEntry<String,String>,Integer> randPrec = getRandomPrecMap(n/2);
		DisjunctiveTemporalProblem dtp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack>>(), randPrec);
		//finally we need to add in our ordering constraints for a fixed order. 
		ArrayList<String> tpNames = ((SimpleDTP) dtp).getActivityNames();
		Collections.shuffle(tpNames);
		//System.out.println("Activity ordering is: " + tpNames);
		for(int i = 0; i < tpNames.size(); i++){
			for(int j = i+1; j < tpNames.size(); j++){
				String tpOne = tpNames.get(i);
				String tpTwo = tpNames.get(j);
				((SimpleDTP)dtp).addOriginalOrderingConstraint(tpOne,tpTwo,0,Integer.MAX_VALUE);
				//System.out.println("adding constraint that " +tpOne+" comes before " + tpTwo);
			}
		}
	
		//and finally, we relax a set number of these ordering constraints. 
		ArrayList<SimpleEntry<Integer,Integer>> pairs = getUniquePairedValues(rn, tpNames.size()-1,numDisjuncts);
		ArrayList<DisjunctiveTemporalConstraint> ocs = findOrderingConstraints(dtp);
		
		for(SimpleEntry<Integer,Integer> pair : pairs){
			//get the constraint between these two activities from the activity ordering
			//System.out.println("pair is: " + pair);
			String act1 = tpNames.get(pair.getKey());
			String act2 = tpNames.get(pair.getValue());
			//String act3 = tpNames.get(pair.getValue()+1);
			DisjunctiveTemporalConstraint toModify1 = null;
			//DisjunctiveTemporalConstraint toModify2 = null;
			//DisjunctiveTemporalConstraint toModify3 = null;
			for(DisjunctiveTemporalConstraint dtc : ocs){
				// want to find the one that has act1E - act2S <=0
				TemporalDifference temp = dtc.getTemporalDifferences().get(0);
				String source = temp.source.getName();
				String dest = temp.destination.getName();
				
				
				if(source.equals(act1+"_E") && dest.equals(act2+"_S")){
					toModify1 = dtc;
					continue;
				}

				
			}
			
			Timepoint new_source = dtp.getTimepoint(act2+"_E");
			Timepoint new_dest = dtp.getTimepoint(act1+"_S");
			TemporalDifference td_new = new TemporalDifference(new_source, new_dest, 0);
			toModify1.add(td_new);

		}
		return dtp;
	}
	
	/**
	 * This function is similar to the above function except instead of adding in random disjunctive constraints where each component is of the form "t1 - t2 <= b", this
	 * function adds random disjunctive constraints where each component is of the form "t1 - t2 \in [a,b]."  
	 * @param n number of timepoints in the number problem (+ the zero timepoint, not included in the count)
	 * @param m the number of additional random constraints to be added to the generated DTP
	 * @param numDisjuncts the number of pairs of (subsequent) activities whose ordering constraints will be relaxed to non-concurrency constraints.
	 * @return the randomly generated DTP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateStructuredDTPNEW(int n, int m, int numDisjuncts){
		Random rn = new Random();
		Timepoint zero = new Timepoint("zero",1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
		

		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		//ArrayList<SimpleEntry<Integer,Integer>> bounds2 = new ArrayList<SimpleEntry<Integer, Integer>>();
		//for(int i = 0; i < 10; i++) bounds2.add(new SimpleEntry<Integer,Integer>(58,58));
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
		
		//random disjunctive constraints
		for(int i = 0; i < m ; i++){
			//System.out.println("Adding random constraints");
			//first we pick two timepoint variables from the sets of timepoints 
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			//System.out.println("x " + pair.getKey()+" Y " + pair.getValue());
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			int l = 0;
			int u = 0;
			int bound = getRandomBound(rn);
			int bound2 = getRandomBound(rn);
			
			if(bound < bound2){
				l = bound; u = bound2;
			}
			if(bound2 < bound){
				l = bound2; u = bound;
			}
			
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, u);
			TemporalDifference td2 = new TemporalDifference(tp2, tp1, -l);
			//System.out.println("TD1 " + td1.toString());
			// now we formulate the second part of the disjunction
			pair = getRandomPair(rn, len);
			tp1 = localTimepoints.get(pair.getKey());
			tp2 = localTimepoints.get(pair.getValue());
			 bound = getRandomBound(rn);
			 bound2 = getRandomBound(rn);
			
			if(bound < bound2){
				l = bound; u = bound2;
			}
			if(bound2 < bound){
				l = bound2; u = bound;
			}
			
			//TODO: Make sure these two bounds are not the same if the two pairs of timepoints are the same
			TemporalDifference td3 = new TemporalDifference(tp1, tp2, u);
			TemporalDifference td4 = new TemporalDifference(tp2, tp1, -l);
			//System.out.println("TD2 " + td2.toString());
			ArrayList<TemporalDifference> tds1 = new ArrayList<TemporalDifference>();
			ArrayList<TemporalDifference> tds2 = new ArrayList<TemporalDifference>();
			ArrayList<ArrayList<TemporalDifference>> cross = new ArrayList<ArrayList<TemporalDifference>>();
			tds1.add(td1); tds1.add(td3); tds2.add(td2); tds2.add(td4);
			cross.add(tds1); cross.add(tds2);
			Collection<DisjunctiveTemporalConstraint> crossConstraints = DisjunctiveTemporalConstraint.crossProduct(cross);
			System.out.println(crossConstraints);
			localConstraints.addAll(crossConstraints);
			
		}
		HashMap<SimpleEntry<String,String>,Integer> randPrec = getRandomPrecMap(n/2);
		DisjunctiveTemporalProblem dtp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack>>(), randPrec);
		//finally we need to add in our ordering constraints for a fixed order. 
		ArrayList<String> tpNames = ((SimpleDTP) dtp).getActivityNames();
		Collections.shuffle(tpNames);
		//System.out.println("Activity ordering is: " + tpNames);
		for(int i = 0; i < tpNames.size(); i++){
			for(int j = i+1; j < tpNames.size(); j++){
				String tpOne = tpNames.get(i);
				String tpTwo = tpNames.get(j);
				((SimpleDTP)dtp).addOriginalOrderingConstraint(tpOne,tpTwo,0,Integer.MAX_VALUE);
				//System.out.println("adding constraint that " +tpOne+" comes before " + tpTwo);
			}
		}
	
		//and finally, we relax a set number of these ordering constraints. 
		ArrayList<SimpleEntry<Integer,Integer>> pairs = getUniquePairedValues(rn, tpNames.size()-1,numDisjuncts);
		ArrayList<DisjunctiveTemporalConstraint> ocs = findOrderingConstraints(dtp);
		
		for(SimpleEntry<Integer,Integer> pair : pairs){
			//get the constraint between these two activities from the activity ordering
			//System.out.println("pair is: " + pair);
			String act1 = tpNames.get(pair.getKey());
			String act2 = tpNames.get(pair.getValue());
			//String act3 = tpNames.get(pair.getValue()+1);
			DisjunctiveTemporalConstraint toModify1 = null;
			//DisjunctiveTemporalConstraint toModify2 = null;
			//DisjunctiveTemporalConstraint toModify3 = null;
			for(DisjunctiveTemporalConstraint dtc : ocs){
				// want to find the one that has act1E - act2S <=0
				TemporalDifference temp = dtc.getTemporalDifferences().get(0);
				String source = temp.source.getName();
				String dest = temp.destination.getName();
				
				
				if(source.equals(act1+"_E") && dest.equals(act2+"_S")){
					//System.out.println("Foudn constraint: " + dtc);
					toModify1 = dtc;
					continue;
				}

			}
			
			Timepoint new_source = dtp.getTimepoint(act2+"_E");
			Timepoint new_dest = dtp.getTimepoint(act1+"_S");
			TemporalDifference td_new = new TemporalDifference(new_source, new_dest, 0);
			toModify1.add(td_new);
			
		}
		return dtp;
	}
	
	/**
	 * This function generates DTPs whose only disjunctive constraints are non-concurrency constraints. 
	 * @param n number of timepoints in the number problem (+ the zero timepoint, not included in the count)
	 * @param m the number of additional random constraints to be added to the generated DTP
	 * @param numDisjuncts the number of pairs of (subsequent) activities whose ordering constraints will be relaxed to non-concurrency constraints.
	 * @return the randomly generated DTP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateStructuredDTPSTPConstraints(int n, int m, int numDisjuncts){
		Random rn = new Random();
		Timepoint zero = new Timepoint("zero",1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
		

		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		//ArrayList<SimpleEntry<Integer,Integer>> bounds2 = new ArrayList<SimpleEntry<Integer, Integer>>();
		//for(int i = 0; i < 10; i++) bounds2.add(new SimpleEntry<Integer,Integer>(58,58));
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
		
		//random constraints
		for(int i = 0; i < m ; i++){
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			int l = 0;
			int u = 0;
			int bound = getRandomPositiveBound(rn);
			int bound2 = getRandomPositiveBound(rn);
			if(bound < bound2){
				l = bound; u = bound2;
			}
			if(bound2 < bound){
				l = bound2; u = bound;
			}
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, u);
			TemporalDifference td2 = new TemporalDifference(tp2, tp1, -l);
			System.out.println(td1); System.out.println(td2);
			ArrayList<TemporalDifference> tds = new ArrayList<TemporalDifference>();
			ArrayList<TemporalDifference> tds2 = new ArrayList<TemporalDifference>();
			tds.add(td1);
			tds2.add(td2);
			localConstraints.add(new DisjunctiveTemporalConstraint(tds));
			localConstraints.add(new DisjunctiveTemporalConstraint(tds2));
			
		}
		HashMap<SimpleEntry<String,String>,Integer> randPrec = getRandomPrecMap(n/2);
		DisjunctiveTemporalProblem dtp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack>>(), randPrec);
		//finally we need to add in our ordering constraints for a fixed order. 
		ArrayList<String> tpNames = ((SimpleDTP) dtp).getActivityNames();
		Collections.shuffle(tpNames);
		//System.out.println("Activity ordering is: " + tpNames);
		for(int i = 0; i < tpNames.size(); i++){
			for(int j = i+1; j < tpNames.size(); j++){
				String tpOne = tpNames.get(i);
				String tpTwo = tpNames.get(j);
				((SimpleDTP)dtp).addOriginalOrderingConstraint(tpOne,tpTwo,0,Integer.MAX_VALUE);
				//System.out.println("adding constraint that " +tpOne+" comes before " + tpTwo);
			}
		}
	
		//and finally, we relax a set number of these ordering constraints. 
		ArrayList<SimpleEntry<Integer,Integer>> pairs = getUniquePairedValues(rn, tpNames.size()-1,numDisjuncts);
		ArrayList<DisjunctiveTemporalConstraint> ocs = findOrderingConstraints(dtp);
		
		for(SimpleEntry<Integer,Integer> pair : pairs){
			//get the constraint between these two activities from the activity ordering
			//System.out.println("pair is: " + pair);
			String act1 = tpNames.get(pair.getKey());
			String act2 = tpNames.get(pair.getValue());
			DisjunctiveTemporalConstraint toModify1 = null;

			for(DisjunctiveTemporalConstraint dtc : ocs){
				// want to find the one that has act1E - act2S <=0
				TemporalDifference temp = dtc.getTemporalDifferences().get(0);
				String source = temp.source.getName();
				String dest = temp.destination.getName();
				
				
				if(source.equals(act1+"_E") && dest.equals(act2+"_S")){
					//System.out.println("Foudn constraint: " + dtc);
					toModify1 = dtc;
					continue;
				}
				
			}
			
			Timepoint new_source = dtp.getTimepoint(act2+"_E");
			Timepoint new_dest = dtp.getTimepoint(act1+"_S");
			TemporalDifference td_new = new TemporalDifference(new_source, new_dest, 0);
			toModify1.add(td_new);
			

		}
		return dtp;
	}
	
	/**
	 * This function is the most general generation method for random DTPs. It generates a problem with randomly generated disjunctive constraints where each 
	 * component is of the form "t1 - t2 <= b"
	 * @param n the number of timepoints in the randomly generated problem (not including the zero timepoint)
	 * @param m the number of randomly generated constraints to add to the problem
	 * @return the randomly generated DTP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateDTP(int n, int m){
		Random rn = new Random();
		System.out.println("Fill amount is: " + D_FILL);
		Timepoint zero = new Timepoint("zero",1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
		

		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		//ArrayList<SimpleEntry<Integer,Integer>> bounds2 = new ArrayList<SimpleEntry<Integer, Integer>>();
		//for(int i = 0; i < 10; i++) bounds2.add(new SimpleEntry<Integer,Integer>(58,58));
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
		
		
		// now we generate constraints.
		for(int i = 0; i < m ; i++){
			//System.out.println("Adding random constraints");
			//first we pick two timepoint variables from the sets of timepoints 
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			//System.out.println("x " + pair.getKey()+" Y " + pair.getValue());
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			int bound = getRandomBound(rn);
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, bound);
			//System.out.println("TD1 " + td1.toString());
			// now we formulate the second part of the disjunction
			pair = getRandomPair(rn, len);
			tp1 = localTimepoints.get(pair.getKey());
			tp2 = localTimepoints.get(pair.getValue());
			bound = getRandomBound(rn);
			//TODO: Make sure these two bounds are not the same if the two pairs of timepoints are the same
			TemporalDifference td2 = new TemporalDifference(tp1, tp2, bound);
			//System.out.println("TD2 " + td2.toString());
			ArrayList<TemporalDifference> tds = new ArrayList<TemporalDifference>();
			tds.add(td1); tds.add(td2);
			localConstraints.add(new DisjunctiveTemporalConstraint(tds));
			
		}
		
		//TODO: I have changed the data structure and possibly I have to fix this constructor. -Chi
		DisjunctiveTemporalProblem dtp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack> >());
		
		// add non-concurrency constraints
		
		ArrayList<String> tpNames = ((SimpleDTP) dtp).getActivityNames();
		for(int w = 0; w < tpNames.size(); w++){
			for(int v = w+1; v < tpNames.size(); v++){
				String tpOne = tpNames.get(w);
				String tpTwo = tpNames.get(v);
				((SimpleDTP) dtp).addOriginalNonconcurrentConstraint(tpOne, tpTwo, -1);
				//System.out.println("adding nonconcurrency constraint between "+tpOne+" "+tpTwo);
			}
		}
		//add in ordering constraints. 
		/**
		for(int f = 1; f < len - 1; f+=2){
			Timepoint tpS = localTimepoints.get(f);
			Timepoint tpE = localTimepoints.get(f+1);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(new TemporalDifference(tpS, tpE, 0));
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(new TemporalDifference(tpE, tpS, Integer.MAX_VALUE));
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
		**/
		return dtp;
		
	}
	
	/**
	 * Helper function for generating STPs with or without paired randomly generated constraints
	 * @param n the number of timepoints
	 * @param m the number of constraints
	 * @param paired whether or not randomly generated constraints should be paired
	 * @return the randomly generated STP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateSTP(int n, int m, boolean paired){
		if(paired) return generateSTP(n,m);
		else return generateSTPOld(n,m);
	}
	
	/**
	 * Generates a random STP problem with n timepoints and m additional temporal difference constraints. Randomly generated constraints
	 * are of the form "t1 - t2 \in [a,b]" where both bounds are randomly generated
	 * @param n the number of timepoints in the new STP problem (Excluding the zero timepoint)
	 * @param m the number of random constraints added to the new STP problem
	 * @return randomly generated STP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateSTP(int n, int m){
		System.out.println("Fill amount is: " + D_FILL);
		Random rn = new Random();
		Timepoint zero = new Timepoint("zero", 1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
				
		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
			
		//now we generate constraints, but unlike in the DTP case, these only have one disjunct (so not disjunctive)
		for(int i = 0; i < m; i++){
			//get the pair of timepoints this constraint will be between
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			int l = 0;
			int u = 0;
			int bound = getRandomPositiveBound(rn);
			int bound2 = getRandomPositiveBound(rn);
			if(bound < bound2) l = bound; u = bound2;
			if(bound2 < bound) l = bound2; u = bound;
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, u);
			TemporalDifference td2 = new TemporalDifference(tp2, tp1, -l);
			ArrayList<TemporalDifference> tds = new ArrayList<TemporalDifference>();
			ArrayList<TemporalDifference> tds2 = new ArrayList<TemporalDifference>();
			tds.add(td1);
			tds2.add(td2);
			localConstraints.add(new DisjunctiveTemporalConstraint(tds));
			localConstraints.add(new DisjunctiveTemporalConstraint(tds2));
		}
		
		
		DisjunctiveTemporalProblem stp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack>>());
		//finally we need to add in our ordering constraints for a fixed order. 
		ArrayList<String> tpNames = ((SimpleDTP) stp).getActivityNames();
		Collections.shuffle(tpNames);
		System.out.println("Activity ordering is: " + tpNames);
		for(int i = 0; i < tpNames.size(); i++){
			for(int j = i+1; j < tpNames.size(); j++){
				String tpOne = tpNames.get(i);
				String tpTwo = tpNames.get(j);
				((SimpleDTP)stp).addOriginalOrderingConstraint(tpOne,tpTwo,0,Integer.MAX_VALUE);
				//System.out.println("adding constraint that " +tpOne+" comes before " + tpTwo);
			}
		}
		
		return stp;
	}
	
	/**
	 * This version for generating STPs introduces random constraints of the for "t1 - t2 <= b"
	 * @param n the number of timepoints in the newly generated STP
	 * @param m number of randomly generated constraints to be added to the newly generated STP
	 * @return the randomly generated STP with the above properties.
	 */
	public static DisjunctiveTemporalProblem generateSTPOld(int n, int m){
		System.out.println("Fill amount is: " + D_FILL);
		Random rn = new Random();
		Timepoint zero = new Timepoint("zero", 1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
				
		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
			
		//now we generate constraints, but unlike in the DTP case, these only have one disjunct (so not disjunctive)
		for(int i = 0; i < m; i++){
			//get the pair of timepoints this constraint will be between
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			//int l = 0;
			//int u = 0;
			int bound = getRandomBound(rn);
			//int bound2 = getRandomPositiveBound(rn);
			//if(bound < bound2) l = bound; u = bound2;
			//if(bound2 < bound) l = bound2; u = bound;
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, bound);
			//TemporalDifference td2 = new TemporalDifference(tp2, tp1, -l);
			ArrayList<TemporalDifference> tds = new ArrayList<TemporalDifference>();
			//ArrayList<TemporalDifference> tds2 = new ArrayList<TemporalDifference>();
			tds.add(td1);
			//tds2.add(td2);
			localConstraints.add(new DisjunctiveTemporalConstraint(tds));
			//localConstraints.add(new DisjunctiveTemporalConstraint(tds2));
		}
		
		
		DisjunctiveTemporalProblem stp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack>>());
		//finally we need to add in our ordering constraints for a fixed order. 
		ArrayList<String> tpNames = ((SimpleDTP) stp).getActivityNames();
		Collections.shuffle(tpNames);
		System.out.println("Activity ordering is: " + tpNames);
		for(int i = 0; i < tpNames.size(); i++){
			for(int j = i+1; j < tpNames.size(); j++){
				String tpOne = tpNames.get(i);
				String tpTwo = tpNames.get(j);
				((SimpleDTP)stp).addOriginalOrderingConstraint(tpOne,tpTwo,0,Integer.MAX_VALUE);
				//System.out.println("adding constraint that " +tpOne+" comes before " + tpTwo);
			}
		}
		
		return stp;
	}
	
	/**
	 * helper function that returns a random pair of indices in a list of length "len"
	 * @param rn random number structure to use for generating the pair
	 * @param len the length of the list that the random paired indices must be valid for
	 * @return a random pair of integers that are valid indices for a list of length len. 
	 */
	private static SimpleEntry<Integer, Integer> getRandomPair(Random rn, int len){
		int x = 0;
		int y = 0;
		
		while (x == y || (x%2 == 1 && y == x+1) || (y%2 == 1 && x == y + 1)){
			x = rn.nextInt(len-1) + 1;
			y = rn.nextInt(len-1) + 1;
		}
		return new SimpleEntry<Integer,Integer>(x,y);
	}
	
	/**
	 * Helper function that returns a random bound <= L. This bound is negative with 1/2 probability.
	 * @param rn the random number structure to use for generating the random bound
	 * @return a random, possibly negative, bound in the interval [-L, L]
	 */
	private static int getRandomBound(Random rn){
		int dur = rn.nextInt(L+1);
		double half = rn.nextDouble();
		if (half <= 0.5) dur = dur * -1;
		return dur;
	}
	
	/**
	 * Generate a random precedence map based on the number of activities in the problem. 
	 * @param numActs the number of activities (NOT timepoints) in the problem.
	 * @return A hash map representing the precedence relation between each activity and the sporadic event.
	 */
	// n is the number of activities, so here n would be 10.
	private static HashMap<SimpleEntry<String,String>,Integer> getRandomPrecMap(int numActs){
		Random rn = new Random();
		HashMap<SimpleEntry<String,String>, Integer> prec = new HashMap<SimpleEntry<String,String>,Integer>();
		for(int i = 0; i < numActs; i++){
			String act = nameList.get(i);
			boolean ranprec = rn.nextBoolean();
			if(ranprec) prec.put(new SimpleEntry<String,String>(act, "sporadic"), 1);
			else prec.put(new SimpleEntry<String,String>(act, "sporadic"), 0);
			
		}
		return prec;
		
		
	}
	/**
	 * Gets a random bound and ensures that it is positive.
	 * @param rn the random structure to use for generating the random bound.
	 * @return a random bound in [0, L]
	 */
	private static int getRandomPositiveBound(Random rn){
		int dur = rn.nextInt(L+1);
		return dur;
	}
	
	/**
	 * Helper function that returns a simple entry containing two sequential numbers that are randomly selected
	 * @param rn the random structure to use to generated the random value
	 * @param M the upper bound of the range to select the values from
	 * @return a pair of consecutive (directly sequential) values in the interval [0,M).
	 */
	private static SimpleEntry<Integer, Integer> getRandomPairedValues(Random rn, int M){
		//returns a pair of consecutive values in the range [0,M)
		
		int first = 0; int second = 0;
		int num = rn.nextInt(M);
		if(num == 0){
			first = 0;
			second = 1;
		}else{
			first = num-1;
			second = num;
		}
		return new SimpleEntry<Integer,Integer>(first,second);
	}
	
	/**
	 * Helper function that returns a list of paired consecutive values in a specific range.
	 * @param rn the random structure to use to generate random values
	 * @param M the upper bound of the range from which to select the values
	 * @param len the number of unique pairs to return
	 * @return a list of length "len" that contains unique pairs of consecutive random numbers. 
	 */
	public static ArrayList<SimpleEntry<Integer,Integer>> getUniquePairedValues(Random rn, int M, int len){
		// returns len pairs of consecutive ints in the range [0,M)
		ArrayList<SimpleEntry<Integer, Integer>> pairs = new ArrayList<SimpleEntry<Integer,Integer>>();
		boolean contains = false;
		while(pairs.size() < len){
			SimpleEntry<Integer,Integer> cand = getRandomPairedValues(rn, M);
			int val = cand.getKey();
			contains = false;
			for(SimpleEntry<Integer, Integer> elm : pairs){
				if(elm.getKey() == val) contains = true;
			}
			if(!contains) pairs.add(cand);
		}
		//System.out.println("Returning pairs: " + pairs);
		return pairs;
	}
	
	
	/**
	 * Takes a randomly generated stp problem and transforms it into a DTP problem by relaxing numDisjuncts ordering constraints
	 * @param stp the randomly generated stp problem to be transformed into a DTP problem
	 * @param numDisjuncts the number of ordering constraints in the stp to relax to nonconcurrency constraints
	 * @return a DTP based on the input stp that has numDisjuncts ordering constraints relaxed. 
	 */
	// takes in the problem to turn into a DTP and the number of ordering constraints that should be made disjunctive.
	public static DisjunctiveTemporalProblem translateSTPtoDTP(DisjunctiveTemporalProblem stp, int numDisjuncts){
		DisjunctiveTemporalProblem dtp = stp.clone();
		ArrayList<DisjunctiveTemporalConstraint> ocs = findOrderingConstraints(dtp);
		ArrayList<Integer> randInds = getUniqueRandomNumbersInRange(ocs.size(), numDisjuncts);
		for(int i : randInds){
			// modify the ith constraint in ocs
			DisjunctiveTemporalConstraint toUpdate = ocs.get(i);
			//System.out.println("Constraint to updatE: " + toUpdate);
			TemporalDifference td = toUpdate.getTemporalDifferences().get(0);
			// we want to flip this constraint to capture the other ordering 
			String source = td.source.getName();
			String source_name = source.substring(0,source.length() - 2);
			String dest = td.destination.getName();
			String dest_name = dest.substring(0, dest.length() -2);
			Timepoint new_source = dtp.getTimepoint(dest_name + "_E");
			Timepoint new_dest = dtp.getTimepoint(source_name + "_S");
			
			TemporalDifference td_new = new TemporalDifference(new_source, new_dest, 0);
			toUpdate.add(td_new);
			//System.out.println("Updated constraint: "+toUpdate);
		}
		
		return dtp;
	}
	
	/**
	 * This function takes in a randomly generated STP and returns a DTP in which numDisjuncts ordered triples of activities have 
	 * been relaxed to be nonconcurrency constraints. For example, if the first three activities in the STP are A, B, C in that order, the new DTP 
	 * will say that A, B, and C can come in any order. 
	 * @param stp the input randomly generated STP problem
	 * @param numDisjuncts the number of triples of activities for which to relax the ordering constraints. 
	 * @return a new DTP with the above properties.
	 */
	public static DisjunctiveTemporalProblem translateSTPtoStructuredDTP(DisjunctiveTemporalProblem stp, int numDisjuncts){
		DisjunctiveTemporalProblem dtp = stp.clone();
		Random rn = new Random();
		ArrayList<String> actOrder = ((SimpleDTP) stp).getActivityOrdering();
		System.out.println("act order is : " + actOrder);
		ArrayList<SimpleEntry<Integer,Integer>> pairs = getUniquePairedValues(rn, actOrder.size()-1,numDisjuncts);
		ArrayList<DisjunctiveTemporalConstraint> ocs = findOrderingConstraints(dtp);
		
		for(SimpleEntry<Integer,Integer> pair : pairs){
			//get the constraint between these two activities from the activity ordering
			System.out.println("pair is: " + pair);
			String act1 = actOrder.get(pair.getKey());
			String act2 = actOrder.get(pair.getValue());
			String act3 = actOrder.get(pair.getValue()+1);
			DisjunctiveTemporalConstraint toModify1 = null;
			DisjunctiveTemporalConstraint toModify2 = null;
			DisjunctiveTemporalConstraint toModify3 = null;
			for(DisjunctiveTemporalConstraint dtc : ocs){
				// want to find the one that has act1E - act2S <=0
				TemporalDifference temp = dtc.getTemporalDifferences().get(0);
				String source = temp.source.getName();
				String dest = temp.destination.getName();
				
				
				if(source.equals(act1+"_E") && dest.equals(act2+"_S")){
					//System.out.println("Foudn constraint: " + dtc);
					toModify1 = dtc;
					continue;
				}
				if(source.equals(act1+"_E") && dest.equals(act3+"_S")){
					toModify2 = dtc;
					continue;
				}
				if(source.equals(act2+"_E") && dest.equals(act3+"_S")){
					toModify3 = dtc;
				}
				
			}
			
			Timepoint new_source = dtp.getTimepoint(act2+"_E");
			Timepoint new_dest = dtp.getTimepoint(act1+"_S");
			TemporalDifference td_new = new TemporalDifference(new_source, new_dest, 0);
			toModify1.add(td_new);
			
			Timepoint new_source2 = dtp.getTimepoint(act3+"_E");
			Timepoint new_dest2 = dtp.getTimepoint(act1+"_S");
			TemporalDifference td_new2 = new TemporalDifference(new_source2, new_dest2, 0);
			toModify2.add(td_new2);
			
			Timepoint new_source3 = dtp.getTimepoint(act3+"_E");
			Timepoint new_dest3 = dtp.getTimepoint(act2+"_S");
			TemporalDifference td_new3 = new TemporalDifference(new_source3, new_dest3, 0);
			toModify3                                                                     .add(td_new3);
			
			//TESTING
			//get the earliest start time of the first activity
			Timepoint tp = dtp.getTimepoint(act1+"_S");
			int est = ((SimpleDTP)dtp).getEarliestStartTime(tp);
			System.out.println("earliest start is: " + est);
			//tighten the constraint by 5 minutes
			TemporalDifference tight = new TemporalDifference(dtp.getTimepoint("zero"), tp, -est - 2);
			System.out.println("Adding td: " + tight);
			dtp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tight));
		}
		 
		return dtp;
	}
	
	/**
	 * This function allows the addition of more randomly-generated disjunctive constraints to an existing dtp problem.
	 * This function adds random disjunctive constraints whose components are of the form "t1-t2 <= b"
	 * @param dtp the existing dtp problem (may itself be randomly generated)
	 * @param m the number of additional randomly-generated disjunctibe constraints to add. 
	 * @return the modified DTP 
	 */
	public static DisjunctiveTemporalProblem addRandomDisjunctiveConstraints(DisjunctiveTemporalProblem dtp, int m){
		Random rn = new Random();
		
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>(dtp.getTimepoints());
		int len = localTimepoints.size();
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = dtp.getTempConstraints();
		for(int i = 0; i < m ; i++){
			//System.out.println("Adding random constraints");
			//first we pick two timepoint variables from the sets of timepoints 
			SimpleEntry<Integer,Integer> pair = getRandomPair(rn,len);
			//System.out.println("x " + pair.getKey()+" Y " + pair.getValue());
			Timepoint tp1 = localTimepoints.get(pair.getKey());
			Timepoint tp2 = localTimepoints.get(pair.getValue());
			int bound = getRandomBound(rn);
			TemporalDifference td1 = new TemporalDifference(tp1, tp2, bound);
			//System.out.println("TD1 " + td1.toString());
			// now we formulate the second part of the disjunction
			pair = getRandomPair(rn, len);
			tp1 = localTimepoints.get(pair.getKey());
			tp2 = localTimepoints.get(pair.getValue());
			bound = getRandomBound(rn);
			//TODO: Make sure these two bounds are not the same if the two pairs of timepoints are the same
			TemporalDifference td2 = new TemporalDifference(tp1, tp2, bound);
			//System.out.println("TD2 " + td2.toString());
			ArrayList<TemporalDifference> tds = new ArrayList<TemporalDifference>();
			tds.add(td1); tds.add(td2);
			DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(tds);
			System.out.println("Adding constraint: " + dtc);
			localConstraints.add(dtc);
			
		}
		return dtp;
	}
	
	/**
	 * This helper function finds and returns the list of constraints in the given problem that are ordering constraints, i.e. 
	 * if the first timepoint in the constraint is an end timepoint and the second is a start timepoint of a different activity.
	 * @param stp the problem from which to get the ordering constraints
	 * @return a list of disjunctive temporal constraints containing only the ordering constraints from the stp 
	 */
	private static ArrayList<DisjunctiveTemporalConstraint> findOrderingConstraints(DisjunctiveTemporalProblem stp){
		ArrayList<DisjunctiveTemporalConstraint> ordering = new ArrayList<DisjunctiveTemporalConstraint>();
		
		for(DisjunctiveTemporalConstraint cons : stp.getTempConstraints()){
			//check if a constraint is an ordering constraint by checking if first tp is end tp and secondi s start of a different act
			for( TemporalDifference td: cons.getTemporalDifferences()){
				//there should just be one temporalDifference here.
				String source_name = td.source.getName();
				String dest_name = td.destination.getName();
				int bound = td.bound;
				if(source_name.substring(source_name.length() - 2).equals("_E")
						&& dest_name.substring(dest_name.length()-2).equals("_S")
						&& bound == 0){
					ordering.add(cons);
					continue;
				}
			}
		}
		//System.out.println(ordering);
		return ordering;
		
	}
	
	//len describes the range to select numbers from [0,len)
	// n is the number of unique random numbers we need
	/**
	 * Helper function to get a series of unique random number in a specific range.
	 * @param len the random from which to select numbers, [0, len)
	 * @param n the number of unique random numbers needed
	 * @return a list of integers of length "n" tht contains n unique random numbers from the range [0,len)
	 */
	public static ArrayList<Integer> getUniqueRandomNumbersInRange(int len, int n){
		ArrayList<Integer> output = new ArrayList<Integer>();
		Random rn = new Random();
		while(output.size() < n){
			int cand = rn.nextInt(len-1);
			if(output.contains(cand)) continue;
			output.add(cand);
		}
		
		//System.out.println("Asked for " + n +" random numbers in range 0 to " + len);
		//System.out.println(output);
		return output;
		
	}
	
	private static ArrayList<SimpleEntry<Integer,Integer>> getAllDurationsOld(Random rn, int numDurs){
		int lastval = 0;
		int val;
		ArrayList<SimpleEntry<Integer, Integer>> vals = new ArrayList<SimpleEntry<Integer, Integer>>();
		//int total_dur = (int) (D_TOTAL * D_RATIO);
		int total_dur = D_FILL;
		//System.out.println("TOTAL DURATION: "+total_dur);
		// right now we won't worry about making sure values are within our range of activity durations
		ArrayList<Integer> ubs = new ArrayList<Integer>();
		for(int i = 0; i < numDurs-1; i++){
			do {
				val = rn.nextInt(total_dur);
				//val = 5*Math.round(val/5);
			
			}while((val - lastval < D_MIN) || (val - lastval > D_MAX));
			ubs.add(val);
			lastval = val;
		}
		Collections.sort(ubs);
		System.out.println("BOUNDS: " + ubs);
		for(int j = 0; j < numDurs-1; j++){
			int dur = 0;
			//if(j == 0) dur = ubs.get(j);
			//else if(j == numDurs - 2) dur = total_dur - ubs.get(j);
			if(j == 0) continue;
			dur = ubs.get(j) - ubs.get(j-1);
			// assume fixed durations. 
			vals.add(new SimpleEntry<Integer, Integer>(dur,dur)); 
			
		}
		vals.add(new SimpleEntry<Integer,Integer>(ubs.get(0), ubs.get(0))); //first interval
		int lastDur = D_FILL - ubs.get(numDurs - 2);
		vals.add(new SimpleEntry<Integer, Integer>(lastDur,lastDur));
		System.out.println(vals);
		return vals;
		
	}
	
	/**
	 * Helper function to generate a set of randomly generated durations for all activities in a problem.
	 * @param rn the random structure to use to generate the random values
	 * @param numDurs the number of activities for which durations are needed
	 * @return a list of pairs of integers capturing the duration of an activity. This function is written so that variable durations could be returned (i.e. se.key() is the lb duration
	 * and se.value() is the ub duration), but we return fixed durations to ensure that the sum of the durations is a fixed number. 
	 */
	public static ArrayList<SimpleEntry<Integer,Integer>> getAllDurations(Random rn, int numDurs){
		int total_dur = D_FILL;
		int val;
		ArrayList<SimpleEntry<Integer, Integer>> vals = new ArrayList<SimpleEntry<Integer, Integer>>();
		boolean found_valid = false;
		while(!found_valid){
			ArrayList<Integer> bounds = new ArrayList<Integer>();
			for(int i = 0; i < numDurs - 1; i++){
				val = rn.nextInt(total_dur);
				bounds.add(val);
			}
			Collections.sort(bounds);
			//System.out.println("BOUNDS ARE: " + bounds);
			for(int j = 1; j < numDurs-1; j++){
				int dur = 0;
				dur = bounds.get(j) - bounds.get(j-1);
				vals.add(new SimpleEntry<Integer, Integer> (dur,dur));
			}
			//add in the first duration
			vals.add(new SimpleEntry<Integer, Integer>( bounds.get(0), bounds.get(0)));
			int lastDur = D_FILL - bounds.get(numDurs - 2);
			vals.add(new SimpleEntry<Integer, Integer>(lastDur,lastDur));
			//now check that they're all within D_MIN and D_MAX, and if not, then continue
			ArrayList<Boolean> res = new ArrayList<Boolean>();
			//System.out.println("DURS are " + vals);
			for(SimpleEntry<Integer,Integer> dur : vals){
				if(dur.getValue() < D_MIN || dur.getValue() > D_MAX) res.add(false);
				else res.add(true);
			}
			boolean output = true;
			for(boolean elm : res){
				if(elm == false) output = false;
			}
			if(!output) {
				vals.clear();
				continue;
			}
			//System.out.println("FOUND valid durs: " + vals);
			found_valid = true;
		}
		return vals;
	}

	/**
	 * This helper function introduces time critical functions into the problem (i.e. gives them fixed start times)
	 * @param rn the random structure to use for random index generation
	 * @param dtp the problem to modify
	 * @param i the number of randomly selected activities to make time critical
	 * @param n the number of activities in the dtp
	 * @return the updated dtp problem. 
	 */
	//selects i random activities and fixes their start times. also changes the prec map to be all ones, other than this value?
	// n is the number of activities
	public static DisjunctiveTemporalProblem makeTimeCritical(Random rn, DisjunctiveTemporalProblem dtp, int i, int n){
		HashMap<SimpleEntry<String,String>, Integer> prec = precOnes; //all 1s
		ArrayList<Integer> randIdxs = getUniqueRandomNumbersInRange(n,i);
		for(int k = 0; k < i; k++){
			
			String actName = nameList.get(randIdxs.get(k));//gets a random activity name and proceeds from there 
			System.out.println(actName);
			SimpleEntry<String,String> ent = new SimpleEntry<String,String>(actName, "sporadic");
			prec.replace(ent, 2);
			Timepoint z = dtp.getTimepoint("zero");
			Timepoint act_s = dtp.getTimepoint(actName + "_S");
			int lst = ((SimpleDTP) dtp).getLatestStartTime(act_s);
			// do we want to fix the earliest or latest start time? does it matter? 
			TemporalDifference new_est = new TemporalDifference(z, act_s, -lst);
			DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new_est);
			dtp.addAdditionalConstraint(dtc);
			dtp.updateInternalData();
			dtp.enumerateSolutions(0);
			dtp.simplifyMinNetIntervals();
		}
		return dtp;
	}
	
	//this doesn't actually work yet. 
	public static DisjunctiveTemporalProblem induceHybridSolutions(int n, int k){
		//k is the number of kinds of these constraints we're going to introduce.
		// n is the number of activities. 		
		Random rn = new Random();
		Timepoint zero = new Timepoint("zero",1);
		zero.setReference(true);
		ArrayList<Timepoint> localTimepoints = new ArrayList<Timepoint>();
		localTimepoints.add(zero);
		ArrayList<Timepoint> interfaceTimepoints = new ArrayList<Timepoint>(); // will not be modified.
		ArrayList<DisjunctiveTemporalConstraint> localConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		// first we need to load in the timepoints
		for(int i = 0; i < n/2; i++){
			String name = nameList.get(i);
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			localTimepoints.add(start);
			localTimepoints.add(end);
		}
		int len = localTimepoints.size(); // should be n+1! (with the zero timepoint) 
		//add in meaningful constraints with the zero tp
		for(int g = 1; g < len; g++){
			Timepoint dest = localTimepoints.get(g);
			TemporalDifference td1 = new TemporalDifference(dest, zero, L);
			TemporalDifference td2 = new TemporalDifference(zero, dest, 0);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
		}
		

		//randomly assign durations that sum to d. 
		ArrayList<SimpleEntry<Integer,Integer>> bounds = getAllDurations(rn, n/2);
		ArrayList<SimpleEntry<Integer,Integer>> bounds2 = new ArrayList<SimpleEntry<Integer, Integer>>();
		for(int i = 0; i < 10; i++) bounds2.add(new SimpleEntry<Integer,Integer>(58,58));
		int c = 0;
		for(int h = 1; h < len - 1; h+=2){
			Timepoint start = localTimepoints.get(h);
			Timepoint end = localTimepoints.get(h+1);
			SimpleEntry<Integer,Integer> vals = bounds2.get(c);
			int ub = vals.getValue();
			int lb = vals.getKey();
			//System.out.println("duration of activity "+ h +" is between " + lb + " and "+ub);
			TemporalDifference tdlb = new TemporalDifference(start, end, -1*lb);
			TemporalDifference tdub = new TemporalDifference(end,start,ub);
			DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(tdlb);
			DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(tdub);
			localConstraints.add(dtc1); localConstraints.add(dtc2);
			c++;
		}
		
		HashMap<SimpleEntry<String,String>,Integer> randPrec = getRandomPrecMap(n/2);
		DisjunctiveTemporalProblem dtp = new AgentDTP(id++, localConstraints, localTimepoints, interfaceTimepoints, new HashMap<String, Vector<MappedPack>>(), precTest);
		//finally we need to add in our ordering constraints for a fixed order. 
		ArrayList<String> tpNames = ((SimpleDTP) dtp).getActivityNames();
		//Collections.shuffle(tpNames);
		System.out.println("Activity ordering is: " + tpNames);
		for(int i = 0; i < tpNames.size(); i++){
			for(int j = i+1; j < tpNames.size(); j++){
				String tpOne = tpNames.get(i);
				String tpTwo = tpNames.get(j);
				((SimpleDTP)dtp).addOriginalOrderingConstraint(tpOne,tpTwo,0,Integer.MAX_VALUE);
				//System.out.println("adding constraint that " +tpOne+" comes before " + tpTwo);
			}
		}
		
		//now we want to leave the strict ordering between the first and second activity. 
		// but relax the ordering between the third and fourth. 
		SimpleEntry<Integer,Integer> pair = new SimpleEntry<Integer, Integer>(2,3);
		ArrayList<DisjunctiveTemporalConstraint> ocs = findOrderingConstraints(dtp);
		String act0 = tpNames.get(0);
		String act1 = tpNames.get(1);
		String act2 = tpNames.get(pair.getKey());
		String act3 = tpNames.get(pair.getValue());
		DisjunctiveTemporalConstraint toModify1 = null;

		for(DisjunctiveTemporalConstraint dtc : ocs){
			// want to find the one that has act1E - act2S <=0
			TemporalDifference temp = dtc.getTemporalDifferences().get(0);
			String source = temp.source.getName();
			String dest = temp.destination.getName();
			
			
			if(source.equals(act2+"_E") && dest.equals(act3+"_S")){
				//System.out.println("Foudn constraint: " + dtc);
				toModify1 = dtc;
				continue;
			}

			
		}
		
		Timepoint new_source = dtp.getTimepoint(act3+"_E");
		Timepoint new_dest = dtp.getTimepoint(act2+"_S");
		TemporalDifference td_new = new TemporalDifference(new_source, new_dest, 0);
		toModify1.add(td_new);
		
		dtp.updateInternalData();
		dtp.enumerateSolutions(0);
		dtp.simplifyMinNetIntervals();
		
		//now add the constraints that hopefully give us what we want. 
		Timepoint act1s = dtp.getTimepoint(act1 + "_S");
		Timepoint act2e = dtp.getTimepoint(act2 + "_E");
		Timepoint z = dtp.getTimepoint("zero");
		Timepoint act3s = dtp.getTimepoint(act3 + "_S");
		Timepoint act3e = dtp.getTimepoint(act3 + "_E");
		Timepoint act2s = dtp.getTimepoint(act2 + "_S");
		
		//duration of first activity
		int dA = ((SimpleDTP) dtp).getDuration(act0);
		
		TemporalDifference tdA = new TemporalDifference(act1s, z, 58);
		TemporalDifference tdB = new TemporalDifference(act2e, act3s, 0);
		TemporalDifference tdC = new TemporalDifference(z,act1s,-59);
		TemporalDifference tdD = new TemporalDifference(act3e, act2s, 0);
		ArrayList<ArrayList<TemporalDifference>> diffs = new ArrayList<ArrayList<TemporalDifference>>();
		ArrayList<TemporalDifference> tds1 = new ArrayList<TemporalDifference>();
		tds1.add(tdA); tds1.add(tdC);
		ArrayList<TemporalDifference> tds2 = new ArrayList<TemporalDifference>();
		tds2.add(tdB); tds2.add(tdD);
		diffs.add(tds1); diffs.add(tds2);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(diffs);
		//dtp.addAdditionalConstraints(dtcs);
		System.out.println(dtcs);
		return dtp;
	}

}
