package dtp;

import interactionFramework.MappedPack;
import interval.Interval;
import interval.IntervalSet;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Vector;

import stp.TemporalDifference;
import stp.Timepoint;
import util.Node;

public class ManyDUSTP implements DisjunctiveTemporalProblem {

	
	public static  String SPORADICNAME = "Sporadic";
	public static  int DUR = 20; //duration of the sporadic event
	public static  int SporadicIntervalLength = 600; //length of scheduling period for sporadic interval -- DEPRECATED, replaced by interval below
	public static  int SporadicEventET = 0;  //earliest time of sporadic event
	public static  int SporadicEventLT = 1440;  //latest time of sporadic event (NOTE: sporadic activity could happen later, depending on precendence)
	public static  int MaxSporadicActivityEndTimeAfterSporadicEvent = 1440;  //Even though the SE can't happen after the SporadiceEventLT, the activity to address it can, depending on precedence
	public static  int L = 600; //length of scheduling period
	public SimpleDTP stp = null; // underlying stp problem
	
	public Node<ArrayList<ArrayList<SimpleDTP>>> DTreeMany = null;
	public Node<ArrayList<ArrayList<SimpleDTP>>> currNodeMany = null;
	public ArrayList<ArrayList<SimpleDTP>> componentSTPsMany = new ArrayList<ArrayList<SimpleDTP>>();
	public ArrayList<SimpleDTP> eliminatedManyComps = new ArrayList<SimpleDTP>();
	
	public int VALIDITY = 1;
	private boolean DC = false;
	
	public ManyDUSTP(SimpleDTP underlying) {
		stp = underlying;
		componentSTPsMany = populateISTPs(1);
		DTreeMany = generateDTreeMany();
		currNodeMany = DTreeMany;
	}
	
	public ManyDUSTP(SimpleDTP underlying, ArrayList<ArrayList<SimpleDTP>> compsMany){
		stp = underlying;
		componentSTPsMany = compsMany;
		DTreeMany = generateDTreeMany();
		currNodeMany = DTreeMany;
	}
	
	//populate component STP function used when not using equivalence classes to represent the occurrences
	//  of the sporadic activity in the problem.
	public ArrayList<ArrayList<SimpleDTP>> populateISTPs(int increment){
		ArrayList<ArrayList<SimpleDTP>> stps = new ArrayList<ArrayList<SimpleDTP>>();
		SimpleDTP newstp;
		SimpleDTP base = stp.clone();
		base.updateInternalData();
		base.enumerateSolutions(0);
		base.simplifyMinNetIntervals();
		ArrayList<String> actOrder = getActivityOrdering();
		System.out.println("Activity ordering is: " + actOrder);
		
		
		Timepoint s_s = new Timepoint(SPORADICNAME + "_S", 1);
		Timepoint s_e = new Timepoint(SPORADICNAME + "_E", 1);
		Timepoint zero = base.getTimepoint("zero");
		base.addContingentTimepoint(s_s);
		base.addContingentTimepoint(s_e);
		
		TemporalDifference tdmin = new TemporalDifference(s_s, s_e, -DUR);
		TemporalDifference tdmax = new TemporalDifference(s_e, s_s, DUR);
		ArrayList<TemporalDifference> min = new ArrayList<TemporalDifference>(); min.add(tdmin);
		ArrayList<TemporalDifference> max = new ArrayList<TemporalDifference>(); max.add(tdmax);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(min);
		tdVec.add(max);
		Collection<DisjunctiveTemporalConstraint> dtcs = DisjunctiveTemporalConstraint.crossProduct(tdVec);
		
		TemporalDifference td1 = new TemporalDifference(s_e, zero, L);
		TemporalDifference td2 = new TemporalDifference(zero, s_s, 0);
		DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
		DisjunctiveTemporalConstraint dtc2 = new DisjunctiveTemporalConstraint(td2);
	
		dtcs.add(dtc1); dtcs.add(dtc2);
		
		base.addAdditionalConstraints(dtcs); 
		base.updateInternalData();
		base.enumerateSolutions(0);
		base.simplifyMinNetIntervals();
		// ^^ these three lines might not be necessary?
		// get the LST of the first activity. This - 1 is the latest time the SE can start 
		//    before any of the activities start 
		int LST = base.getLatestStartTime(base.getTimepoint(actOrder.get(0)+"_S"));
		
		// info we need about each activity: EST, LET, and duration.
		ArrayList<Integer> earliestStarts = new ArrayList<Integer>();
		ArrayList<Integer> latestStarts = new ArrayList<Integer>();
		ArrayList<Integer> earliestEnds = new ArrayList<Integer>();
		ArrayList<Integer> latestEnds = new ArrayList<Integer>();
		ArrayList<Integer> durations = new ArrayList<Integer>();
		for(int i = 0; i < actOrder.size(); i++){
			int e = base.getEarliestStartTime(base.getTimepoint(actOrder.get(i)+"_S"));
			earliestStarts.add(e);
			int l = base.getLatestEndTime(base.getTimepoint(actOrder.get(i)+"_E"));
			latestEnds.add(l);
			int d = base.getDuration(actOrder.get(i));
			durations.add(d);
			int ls = base.getLatestStartTime(base.getTimepoint(actOrder.get(i)+"_S"));
			latestStarts.add(ls);
			int ee = base.getEarliestEndTime(base.getTimepoint(actOrder.get(i)+"_E"));
			earliestEnds.add(ee);
			
		}
		int currInd = 0;
		//String currAct = actOrder.get(currInd);
		for(int t = 0; t <= (L - DUR); t=t+increment){
			//System.out.println("T: "+t);
			if(t > latestEnds.get(currInd)){
				currInd = currInd+1;
				//System.out.println("incrementing currInd");
			}
			
			ArrayList<SimpleDTP> comps = new ArrayList<SimpleDTP>();
			if(t < LST){
				//System.out.println("t comes before first activity, t: "+t);
				newstp = base.clone();
				// then the sporadic event can be at its fixed time T and come before the first activity
				Timepoint sp_s = newstp.getTimepoint(SPORADICNAME + "_S");
				Timepoint z = newstp.getTimepoint("zero");
				TemporalDifference tdt = new TemporalDifference(z, sp_s, -t);
				TemporalDifference tdt2 = new TemporalDifference(sp_s, z, t);
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt));
				newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt2));
				newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(0), 0, Integer.MAX_VALUE);
				
				newstp.updateInternalData();
				newstp.enumerateSolutions(0);
				newstp.simplifyMinNetIntervals();
				//Generics.printDTP(newstp);
				newstp.setIdleNodeTrue();
				comps.add(newstp);
			}
			
			//next we need to figure out what the sporadic event can intersect with if it occurs at time T. 
			// so if T falls in the availability for A, we need to compute t - Da to see the times A could have started
			// and intersected with the sporadic event. 
			for(int j = currInd; j < earliestStarts.size(); j++){
				if(t >= earliestStarts.get(j)){
				//	System.out.println("T comes after the earliest start of: " + actOrder.get(currInd));
					//System.out.println("iterating through for i starting at: "+Math.max(earliestStarts.get(currInd), t - durations.get(currInd)));
					// for when the sporadic event intersects current activity starting at specific times. 
					for(int i = Math.max(earliestStarts.get(j), t - durations.get(j)); i <= t; i++){
						//set currInd act to start at i. note if A starts at time t and SE starts at time t, A 
						//   is started and immediately interrupted (aka se still follows). 
						
						//set currInd act to start at i.
						newstp = base.clone();
						Timepoint act_s = newstp.getTimepoint(actOrder.get(j) + "_S");
						Timepoint z = newstp.getTimepoint("zero");
						TemporalDifference tdt = new TemporalDifference(z, act_s, -i);
						TemporalDifference tdt2 = new TemporalDifference(act_s, z, i);
						newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt));
						newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt2));
						//the sporadic activity immediately follows A 
						newstp.addOrderingConstraint(actOrder.get(j), SPORADICNAME, 0, 0);
						//also sporadic event needs to come before the next activity
						if(j != actOrder.size()-1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(j+1), 0, Integer.MAX_VALUE);
						newstp.updateInternalData();
						newstp.enumerateSolutions(0);
						newstp.simplifyMinNetIntervals();
						//Generics.printDTP(newstp);
						newstp.setSEAct(actOrder.get(j));
						comps.add(newstp);
					}
				// now we check to see if it can occur in between the current activity and the next
				
				}
				//put this one last so we can continue if currInd+1 is oob?
				if(j+1 < actOrder.size()){
					if(t > earliestEnds.get(j) && t < latestStarts.get(j+1)){
						//System.out.println("t can come in between: "+ actOrder.get(currInd) + actOrder.get(currInd+1));
						// then t can come in between A and B. 
						newstp = base.clone();
						Timepoint se_s = newstp.getTimepoint(SPORADICNAME + "_S");
						Timepoint z = newstp.getTimepoint("zero");
						
						TemporalDifference tdt = new TemporalDifference(z, se_s, -t);
						TemporalDifference tdt2 = new TemporalDifference(se_s, z, t);
						newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt));
						newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt2));
						newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(j+1), 0, Integer.MAX_VALUE);
						newstp.addOrderingConstraint(actOrder.get(j), SPORADICNAME, 0, Integer.MAX_VALUE);
						
						newstp.updateInternalData();
						newstp.enumerateSolutions(0);
						newstp.simplifyMinNetIntervals();
						//Generics.printDTP(newstp);
						newstp.setIdleNodeTrue();
						comps.add(newstp);
					}
				}
				// then check to see if it comes early enough to come after EST of next 
				/* I think this loop below is covered in the new one you've written
				if(currInd+1 < actOrder.size()){
					if(t >= earliestStarts.get(currInd+1)){
						//System.out.println("T can interrupt the next activity: "+actOrder.get(currInd+1));
						//System.out.println("i starts at: "+Math.max(earliestStarts.get(currInd+1), t - durations.get(currInd+1)));
						//add the intersection possibilities here like above. 
						for(int i = Math.max(earliestStarts.get(currInd+1), t - durations.get(currInd+1)); i <= t; i++){
							//set currInd act to start at i. note if A starts at time t and SE starts at time t, A 
							//   is started and immediately interrupted (aka se still follows). 
							
							//set currInd act to start at i.
							newstp = base.clone();
							Timepoint act_s = newstp.getTimepoint(actOrder.get(currInd+1) + "_S");
							Timepoint z = newstp.getTimepoint("zero");
							TemporalDifference tdt = new TemporalDifference(z, act_s, -i);
							TemporalDifference tdt2 = new TemporalDifference(act_s, z, i);
							newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt));
							newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt2));
							//the sporadic activity immediately follows A 
							newstp.addOrderingConstraint(actOrder.get(currInd+1), SPORADICNAME, 0, 0);
							//also sporadic event needs to come before the next activity
							if(currInd+1 != actOrder.size()-1) newstp.addOrderingConstraint(SPORADICNAME, actOrder.get(currInd+2), 0, Integer.MAX_VALUE);
							newstp.updateInternalData();
							newstp.enumerateSolutions(0);
							newstp.simplifyMinNetIntervals();
							//Generics.printDTP(newstp);
							newstp.setSEAct(actOrder.get(currInd+1));
							comps.add(newstp);
						}
					// n
					}
				}*/
				
				
			}
			
			if(currInd == actOrder.size() - 1){
				// need to check if T can come after the last activity. 
				if(t >= earliestEnds.get(currInd)){
					//then set t to occur at this time and the previous activity to come before it.
					newstp = base.clone();
					Timepoint z = newstp.getTimepoint("zero");
					Timepoint se_s = newstp.getTimepoint(SPORADICNAME + "_S");
					
					TemporalDifference tdt = new TemporalDifference(z, se_s, -t);
					TemporalDifference tdt2 = new TemporalDifference(se_s, z, t);
					newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt));
					newstp.addAdditionalConstraint(new DisjunctiveTemporalConstraint(tdt2));
					newstp.addOrderingConstraint(actOrder.get(currInd), SPORADICNAME, 0, Integer.MAX_VALUE);
					
					newstp.updateInternalData();
					newstp.enumerateSolutions(0);
					newstp.simplifyMinNetIntervals();
					//Generics.printDTP(newstp);
					newstp.setIdleNodeTrue();
					comps.add(newstp);
					
				}
			}
			
			stps.add(comps);
		}
		
		//System.out.println(stps);
		return stps;
		
	}

	// populates the DTree structure in the case in which equivalence classes are not used.
	// returns the root node of the DTree containing valid nodes at each time.
	public Node<ArrayList<ArrayList<SimpleDTP>>> generateDTreeMany(){
		ArrayList<ArrayList<SimpleDTP>> compSTPs = new ArrayList<ArrayList<SimpleDTP>>();
		for(ArrayList<SimpleDTP> comps : componentSTPsMany){
			ArrayList<SimpleDTP> copy = new ArrayList<SimpleDTP>();
			for(SimpleDTP comp : comps){
				copy.add(comp.clone());
			}
			compSTPs.add(copy);
		}
		
		stp.setIdleNodeTrue();
		ArrayList<SimpleDTP> stpComp = new ArrayList<SimpleDTP>();
		stpComp.add(stp.clone());
		compSTPs.add(stpComp);
		
		Node<ArrayList<ArrayList<SimpleDTP>>> root = new Node<ArrayList<ArrayList<SimpleDTP>>>(compSTPs);
		
		Queue<ArrayList<SimpleDTP>> cSTPs = new LinkedList<ArrayList<SimpleDTP>>(compSTPs);
		Node<ArrayList<ArrayList<SimpleDTP>>> currNode = root;
		
		while(cSTPs.peek() != null){
			ArrayList<SimpleDTP> toAdd = cSTPs.remove();
			ArrayList<ArrayList<SimpleDTP>> data = new ArrayList<ArrayList<SimpleDTP>>();
			data.add(toAdd);
			
			currNode.setRightChild(new Node<ArrayList<ArrayList<SimpleDTP>>>(data, currNode));
			currNode.setLeftChild(new Node<ArrayList<ArrayList<SimpleDTP>>>(new ArrayList<ArrayList<SimpleDTP>>(cSTPs), currNode));
			currNode = currNode.getLeftChild();
			if(cSTPs.size() == 1) break;
		}
		
		return root;
	}
	
	//checks weak controllability in the case in which we aren't using equivalence classes.
	public boolean checkWCMany(){
		ArrayList<ArrayList<SimpleDTP>> allComps = DTreeMany.getNodeData();
		for(ArrayList<SimpleDTP> comps : allComps){
			for(int i = 0; i < comps.size(); i++){
				SimpleDTP comp = comps.get(i);
				comp.updateInternalData();
				comp.enumerateSolutions(0);
				comp.simplifyMinNetIntervals();
				boolean remove = false;
				if(comp.getNumSolutions() == 0) remove = true;
				if(comp.hasZeroDurations()) remove = true;
				if(comp.getMinTime() == Integer.MAX_VALUE) remove = true;
				if(remove) comps.remove(i);
			}
			if(comps.size() == 0){
				return false;
			}
		}
		return true;
	}
	
	//checking dynamic controllability for DUSTPs in which the components of the DUTC aren't compressed into
	// equivalence classes 
	public boolean DCForwardPassMany(){
		Node<ArrayList<ArrayList<SimpleDTP>>> currNode = DTreeMany;
		
		ArrayList<String> act_order = getActivityOrdering();
		//are we still using the depth for anything? 
		int curr_depth = 1;
		
		currNode = currNode.getLeftChild();
		while(currNode != null){
			
		if(currNode.getNodeData().get(0).get(0).idleNode){
			//advance down the tree
			currNode = currNode.getLeftChild();
			curr_depth++;
			continue;
		}
		
		ArrayList<ArrayList<SimpleDTP>> validComps = currNode.getNodeData();
		
		String act_name = currNode.getNodeData().get(0).get(0).se_act;
		
		if(act_name.length() == 0){
			System.out.println("act name isn't set right in the current component.");
		}
		
		//ArrayList<ArrayList<IntervalSet>> intervals = new ArrayList<ArrayList<IntervalSet>>();
		/**
		 * I think this may need to be something like an ArrayList<SimpleEntry<String, ArrayList<IntervalSet>> so that we can 
		 * capture the different intervals for the different possible activities at this level
		 */
		ArrayList<IntervalSet> iss = new ArrayList<IntervalSet>();
		for(ArrayList<SimpleDTP> istps : validComps){
			//get the intervals for the EST of this activity and add it to the list.
			IntervalSet toAdd = new IntervalSet();

			for(SimpleDTP comp : istps){
				IntervalSet is = comp.getInterval("zero", act_name + "_S").inverse();
				for(Interval in : is){
					toAdd.add(in);
				}
			}
			iss.add(toAdd);
		}
			//theoretically the above has resulted in a list of IntervalSets that we can now take the intersection
			//  over and get what we need.
		IntervalSet intersection = new IntervalSet();
		//compute intersection of the interval.
		
		if(iss.size() == 1){
			intersection = iss.get(0);
		}else{
			intersection = iss.get(0).intersection(iss.get(1));
		
			for(int i = 1; i < iss.size(); i++){
				intersection = intersection.intersection(iss.get(i));
			}
		}
	
		if(intersection.isNull()){
			return false;
		}
		int lb = (int) intersection.getLowerBound();
		// now we iterate through the different lists, updating the lowerbound in all of the components and resimplifying.
		//    if we find that we've emptied out a list, then there's a problem.
		
		for(ArrayList<SimpleDTP> comps : validComps){
			// here we might have different activities that we're adjusting in the different components. because some of them 
			// could have a lot of slack in which later activities could indeed be happening earlier... 
			//   do we need to iterate through the problems in the set at a higher level in the function?
			for(int i = 0; i < comps.size(); i++){
				SimpleDTP comp = comps.get(i);
				act_name = comp.se_act; // holds the activity that the SE interacts with. 
				Timepoint act_start = comp.getTimepoint(act_name + "_S");
				Timepoint zero = comp.getTimepoint("zero");
				TemporalDifference td1 = new TemporalDifference(zero, act_start, -lb);
				DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
				comp.addAdditionalConstraint(dtc1);
				comp.updateInternalData();
				comp.enumerateSolutions(0);
				comp.simplifyMinNetIntervals();
				
				//however here we might have eliminated some components from possibilities. One must remain
				//  at least based on the logic used.
				boolean remove = false;
				if(comp.hasZeroDurations()){
					remove = true;
				}
				if(comp.getMinTime() == Integer.MAX_VALUE){
					remove=true;
				}
				if(comp.getNumSolutions() == 0){
					remove=true;
				}
				if(remove) {
					eliminatedManyComps.add(comp);
					comps.remove(i);
				}
			}
		}
		
		for(ArrayList<SimpleDTP> comps : validComps){
			if(comps.size() == 0){
				System.out.println("no more valid comps at this point: " + curr_depth);
				return false;
			}
		}
		
		currNode = currNode.getLeftChild();
		curr_depth++;
		
		}
		
		return true;
	}
	
	public boolean DCBackwardPassMany(){
		Node<ArrayList<ArrayList<SimpleDTP>>> leftmostChild = DTreeMany;
		int curr_depth = 0;
		while(leftmostChild.getLeftChild() != null){
			leftmostChild = leftmostChild.getLeftChild();
			curr_depth++;
		}
		
		ArrayList<String> act_order = getActivityOrdering();
		Collections.reverse(act_order);
		
		Node<ArrayList<ArrayList<SimpleDTP>>> currNode = leftmostChild;
		while(currNode != null){
			
			if(currNode.getNodeData().get(0).get(0).idleNode){
				currNode = currNode.getParent();
				curr_depth--;
				continue;
			}
			
			ArrayList<ArrayList<SimpleDTP>> validComps = currNode.getNodeData();
			
			String act_name = currNode.getNodeData().get(0).get(0).se_act;
			
			ArrayList<IntervalSet> iss = new ArrayList<IntervalSet>();
			for(ArrayList<SimpleDTP> istps : validComps){
				//get the intervals for the EST of this activity and add it to the list.
				IntervalSet toAdd = new IntervalSet();

				for(SimpleDTP comp : istps){
					IntervalSet is = comp.getInterval("zero", act_name + "_S").inverse();
					for(Interval in : is){
						toAdd.add(in);
					}
				}
				iss.add(toAdd);
			}
				//theoretically the above has resulted in a list of IntervalSets that we can now take the intersection
				//  over and get what we need.
			IntervalSet intersection = new IntervalSet();
			//compute intersection of the interval.
			if(iss.size() == 1){
				intersection = iss.get(0);
			}else{
				intersection = iss.get(0).intersection(iss.get(1));
			
				for(int i = 1; i < iss.size(); i++){
					intersection = intersection.intersection(iss.get(i));
				}
			}			
			if(intersection.isNull()){
				return false;
			}
			int ub = (int) intersection.getUpperBound();
			// now we iterate through the different lists, updating the lowerbound in al of the components and resimplifying.
			//    if we find that we've emptied out a list, then there's a problem.
			
			for(ArrayList<SimpleDTP> comps : validComps){
				for(int i = 0; i < comps.size(); i++){
					SimpleDTP comp = comps.get(i);
					act_name = comp.se_act; //this needs to differ among components! 
					Timepoint act_start = comp.getTimepoint(act_name + "_S");
					Timepoint zero = comp.getTimepoint("zero");
					TemporalDifference td1 = new TemporalDifference(act_start, zero, ub);
					DisjunctiveTemporalConstraint dtc1 = new DisjunctiveTemporalConstraint(td1);
					comp.addAdditionalConstraint(dtc1);
					comp.updateInternalData();
					comp.enumerateSolutions(0);
					comp.simplifyMinNetIntervals();
					
					//however here we might have eliminated some components from possibilities. One must remain
					//  at least based on the logic used.
					boolean remove = false;
					if(comp.hasZeroDurations()){
						remove = true;
					}
					if(comp.getMinTime() == Integer.MAX_VALUE){
						remove = true;
					}
					if(comp.getNumSolutions() == 0){
						remove = true;
					}
					if(remove){
						eliminatedManyComps.add(comp);
						comps.remove(i);
					}
				}
			}
			
			for(ArrayList<SimpleDTP> comps : validComps){
				if(comps.size() == 0){
					System.out.println("no more valid comps at this point: " + curr_depth);
					return false;
				}
			}
			
			currNode = currNode.getParent();
			curr_depth--;
		}
		
		return true;
	}
	
	public boolean checkDCMany(){
		boolean fwd = DCForwardPassMany();
		if(!fwd) return false;
		boolean bkwd = DCBackwardPassMany();
		
		//sanity checks for things? do you include?
		
		if(fwd && bkwd) DC = true;
		return fwd && bkwd;
	}
	public SimpleEntry<Integer,Integer> getDTreeManySize(){
		int total = 0;
		for(int i = 0; i < DTreeMany.getNodeData().size(); i++){
			total = total + DTreeMany.getNodeData().get(i).size();
		}
		int elmin = eliminatedManyComps.size();
		return new SimpleEntry<Integer, Integer>(total,elmin);
	}
	
	//A function that can determine the activity ordering from the STP
		public ArrayList<String> getActivityOrdering(){
			ArrayList<String> order = new ArrayList<String>();
			Map<Integer, String> toSort = new TreeMap<Integer, String>();
			//ArrayList<SimpleEntry<Integer, String>> sorted = new ArrayList<SimpleEntry<Integer, String>>();
			int idx = 1;
			while(idx < stp.minimalNetwork.length){
				IntervalSet i = stp.minimalNetwork[0][idx];
				int lb = (int) -i.getLowerBound();
				String name = stp.localTimepoints.get(idx).getName();
				name = name.substring(0,name.length() - 2);
				toSort.put(lb,name);
				idx = idx+2;
			}
			Iterator<Integer> it = toSort.keySet().iterator();
			while(it.hasNext()){
				String tp = toSort.get(it.next());
				order.add(tp);
			}
			//System.out.println("The order of the activities is: " + order);
			return order;
		}

		@Override
		public void enumerateSolutions(int scheduleTime) {
			ArrayList<ArrayList<SimpleDTP>> validComps = currNodeMany.getNodeData();
			for(ArrayList<SimpleDTP> comps : validComps){
				for(SimpleDTP component : comps){
					component.enumerateSolutions(scheduleTime);
				}
			}
			
		}

		@Override
		public boolean nextSolution(int scheduleTime) {
			boolean r = false;
			ArrayList<ArrayList<Boolean>> res = new ArrayList<ArrayList<Boolean>>();
			ArrayList<ArrayList<SimpleDTP>> validComps = currNodeMany.getNodeData();
			for(ArrayList<SimpleDTP> comps : validComps){
				ArrayList<Boolean> temp = new ArrayList<Boolean>();
				for(SimpleDTP component : comps){
					r = component.nextSolution(scheduleTime);
					temp.add(r);
				}
				res.add(temp);
			}
			//each lst needs to have at least one true.
			return false;
		}

		@Override
		public void enumerateInfluences(int scheduleTime) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean nextInfluence(int scheduleTime) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void establishMinimality(List<Timepoint> timepointsToConsider,
				int scheduleTime) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean solveNext(List<Timepoint> timepointsToConsider,
				int scheduleTime) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void advanceToTime(int time, int deltaTime, boolean pushSelection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void advanceToTime(int time, boolean resolve, int deltaTime,
				boolean pushSelection) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void advanceSubDTPToTime(int time, int deltaT, boolean pushSelection, int dtpNum) {
			System.out.println("Error: advanceSubDTPToTime() not implemented for " + this.getClass().getSimpleName());
		}

		@Override
		public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
				String tp2, int deltaTime, boolean pushSelection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void tightenTimepoint(int timeStart, String tp1, int timeEnd,
				String tp2, boolean resolve, int deltaTime,
				boolean pushSelection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void executeAndAdvance(int timeStart, String tp1, int timeEnd,
				String tp2, boolean resolve, int deltaTime,
				boolean pushSelection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addAdditionalConstraint(String tpS, String tpE,
				IntervalSet dtc, int time, boolean resolve,
				boolean pushSelection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addAdditionalConstraints(
				Collection<DisjunctiveTemporalConstraint> col) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addVolitionalConstraints(
				Collection<DisjunctiveTemporalConstraint> col) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addVolitionalConstraint(DisjunctiveTemporalConstraint cons) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean fixZeroValIntervals() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void simplifyMinNetIntervals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getMinTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int[] getMinTimeArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<Integer> getMaxSlack() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IntervalSet getInterval(String tp1, String tp2) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IntervalSet getIntervalGlobal(String tp1, String tp2) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<Timepoint> getTimepoints() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<Timepoint> getInterfaceTimepoints() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Timepoint getTimepoint(String tpS) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean contains(String tp1) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getNumSolutions() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getTotalFlexibility() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getRigidity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getNumAgents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getCurrentAgent() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setCurrentAgent(int agent) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getAgent(String tpS) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ArrayList<SimpleEntry<Double, Double>> getRigidityVals() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void softReset() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void clearIncomingConstraints() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addIncomingConstraint(DisjunctiveTemporalConstraint cons) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addIncomingConstraints(
				Collection<DisjunctiveTemporalConstraint> constraints) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setIncomingConstraints(
				ArrayList<DisjunctiveTemporalConstraint> constraints) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ArrayList<DisjunctiveTemporalConstraint> computeSummarizingConstraints(
				ArrayList<Timepoint> timepointsToDecouple) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<DisjunctiveTemporalConstraint> computeDecouplingConstraints(
				ArrayList<Timepoint> timepointsToDecouple) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addInterfaceTimepoint(Timepoint tp) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addInterfaceTimepoints(Collection<Timepoint> timepoints) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void printTimepointIntervals(PrintStream out) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void printTimepointIntervals(PrintStream out, int time) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void printConstraints(PrintStream out) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void printSelectionStack() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int popSelection(int time) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int popSelection(int time, boolean resolve) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public List<List<String>> getActivities(ActivityFinder af, int time) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void printFixedIntervals(PrintStream out, int time) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean existsFixedTimepoint() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP,
				int threshold) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP,
				DeltaFinder df) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP,
				double relThreshold) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<Delta> rankDeltas(DisjunctiveTemporalProblem prevDTP,
				int rankLim) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<Delta> rankRelativeDeltas(
				DisjunctiveTemporalProblem prevDTP, int rankLim) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void printDeltas(ArrayList<Delta> deltas) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getLatestEndTime(Timepoint tp) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getEarliestEndTime(Timepoint tp) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Stack<DisjunctiveTemporalConstraint> getAdditionalConstraints() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addTimepoint(Timepoint tp) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addContingentTimepoint(Timepoint source) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getValidity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void updateValidity(int val) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<String> getContingentActivities(int time) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void updateValidity(DisjunctiveTemporalProblem currDTP, int i) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateInternalData() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ArrayList<Timepoint> getFixedTimepoints() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<Integer> getMinSlack(int scheduleTime) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getCallsToSolver() {
			// TODO Auto-generated method stub
			return 0;
		}

		/**
		 * Calculates calls to solver when we do not use equivalence classes. 
		 * @return Returns a tuple containing
		 * the total number of calls to solver followed by the number of calls to solver from eliminated problems.
		 */
		public SimpleEntry<Integer,Integer> getCallsToSolverMany() {
			int total = 0;
			int elm_total = 0;
			
			ArrayList<ArrayList<SimpleDTP>> allComps = DTreeMany.getNodeData();
			for(ArrayList<SimpleDTP> comps : allComps){
				for(SimpleDTP comp : comps){
					total += comp.getCallsToSolver();
				}
			}
			
			for(SimpleDTP d : eliminatedManyComps){
				elm_total += d.getCallsToSolver();
			}
			return new SimpleEntry<Integer, Integer>(total, elm_total);
		}
		
		@Override
		public List<String> getUnFixedTimepoints() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int checkBookends(int duration) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getOriginalUpperBound(Timepoint tp) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getOriginalLowerBound(Timepoint tp) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ArrayList<DisjunctiveTemporalConstraint> getSpecificTempConstraints(
				Timepoint tp) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<DisjunctiveTemporalConstraint> getTempConstraints() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Vector<MappedPack> getRelatedInputPairs(String crucial) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addNewTimepoint(Timepoint tp) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ArrayList<Interval> getDTPBoundaries() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addNonconcurrentConstraint(String source, String dest,
				int agent) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ArrayList<String> getActivityNames(int agent) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addFixedTimepoints(Collection<Timepoint> tps) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateTimepointName(Timepoint tp, String new_name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addDurationConstraint(Timepoint start, Timepoint end,
				int duration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addOrderingConstraint(String source, String dest,
				int min_duration, int max_duration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeDurationConstraint(String tpName) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> getComponentSTPs() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void advanceDownTree() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public SimpleEntry<Integer, Integer> getMinSlackInterval(
				int scheduleTime) {
			// TODO Auto-generated method stub
			return null;
		}
		
		// function used to remove invalid component STPs from the Many structure (no equivalence classes)
		public boolean cleanupCompSTPs(){
			int total = 0;
			int after = 0;
			for(ArrayList<SimpleDTP> stps : componentSTPsMany){
				total = total + stps.size();
				for(int i = 0; i < stps.size(); i++){
					SimpleDTP d = stps.get(i);
					boolean remove = false;
					if(d.getNumSolutions() == 0){
						remove = true;
					}
					if(d.hasZeroDurations()){
						remove = true;
						}
					if(d.getMinTime() == Integer.MAX_VALUE){
						remove = true;
					}
					if(remove) stps.remove(i);
				}
				
				after = after + stps.size();
				
			}
			for(ArrayList<SimpleDTP> istps : componentSTPsMany){
				if(istps.size() == 0) {
					return false;
				}
			}
			return true;
		}
		
		public ManyDUSTP clone(){
			return new ManyDUSTP(this.stp);
		}
}
