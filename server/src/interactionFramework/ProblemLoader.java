package interactionFramework;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import stp.TemporalDifference;
import stp.Timepoint;
import util.XMLParser;
import dtp.*;

/**
 * 
 * This class provides methods for reading a problem in from XML files and creating a DTP
 *
 */
public class ProblemLoader {
	private DisjunctiveTemporalProblem[] subDTPs;
	private DisjunctiveTemporalProblem decouplingDTP;
	private ArrayList<Timepoint> decouplingTimepoints;
	private ArrayList<Timepoint>[] localTimepoints;
	private ArrayList<Timepoint>[] interfaceTimepoints;
	private HashMap<String,Timepoint> tpHash;
	private ArrayList<DisjunctiveTemporalConstraint> decouplingConstraints;
	private ArrayList<DisjunctiveTemporalConstraint>[] localConstraints;
	private HashMap<String, Integer> timepoints;
	private ArrayList<ArrayList<DisjunctiveTemporalConstraint>> exhaustiveConstraint;
	private static HashMap<String, Boolean> optionalActivityHash = new HashMap<>();
	protected HashMap<String, Vector<MappedPack> > decouplingInputPairs = new HashMap<>();
	protected HashMap<String, Vector<MappedPack> >[] localInputPairs;
	private static Timepoint zero;
	private static int id = 0;
	static{
		zero = new Timepoint("zero");
		zero.setReference(true);
	}
	
	public DisjunctiveTemporalProblem loadDTPFromFile(String file){
		try{
			id = 0;
			Scanner scan = new Scanner(new File(file));
			String xmlString = scan.useDelimiter("\\Z").next();
			scan.close();
			return loadDTP(xmlString);
		}
		catch(IOException e){
			System.err.println(e.getMessage()+"\n"+e.getStackTrace().toString());
			return null;
		}
	}
	
	public DisjunctiveTemporalProblem loadDTP(String xmlString){
		DisjunctiveTemporalProblem result;
		if(xmlString.startsWith("<multiagentDTP>")){
			System.out.println("Loading a multiagentDTP.");
			result = loadMultiagentDTP(XMLParser.getTrimmedByTag(xmlString, "multiagentDTP"));	
			xmlString = XMLParser.removeFirstTag(xmlString, "multiagentDTP");
		}
		else if(xmlString.startsWith("<multiDTP>")){
			System.out.println("Loading a multiDTP.");
			result = loadMultiDTP(XMLParser.getTrimmedByTag(xmlString, "multiDTP"));
			xmlString = XMLParser.removeFirstTag(xmlString, "multiDTP");
		}
		else if(xmlString.startsWith("<simpleDTP>")){
			System.out.println("Loading a simpleDTP.");
			result = loadSimpleDTP(XMLParser.getTrimmedByTag(xmlString, "simpleDTP"));
			xmlString = XMLParser.removeFirstTag(xmlString, "simpleDTP");			
		}
		else if(xmlString.startsWith("<exhaustiveDTP>")){
			System.out.println("Loading an exhaustiveDTP.");
			result = loadExhaustiveDTP(XMLParser.getTrimmedByTag(xmlString, "exhaustiveDTP"));
			xmlString = XMLParser.removeFirstTag(xmlString, "exhaustiveDTP");
		}
		else if(xmlString.startsWith("<DUTP>")){
			System.out.println("Loading an DUTP.");
			result = loadDUTP(XMLParser.getTrimmedByTag(xmlString, "DUTP"));
			xmlString = XMLParser.removeFirstTag(xmlString, "DUTP");
		}
		else{
			throw new java.lang.IllegalArgumentException("Unknown dtp type");
		}
		return result;
	}
	
	

	public void initialize(int numDTP){	
		subDTPs = new DisjunctiveTemporalProblem[numDTP];
		decouplingTimepoints = new ArrayList<Timepoint>();
		localTimepoints = new ArrayList[numDTP];
		interfaceTimepoints = new ArrayList[numDTP];
		for(int i = 0; i < numDTP; i++){
			localTimepoints[i] = new ArrayList<Timepoint>();
			interfaceTimepoints[i] = new ArrayList<Timepoint>();
		}
		tpHash = new HashMap<String,Timepoint>();
		decouplingConstraints = new ArrayList<DisjunctiveTemporalConstraint>();
		localConstraints = new ArrayList[numDTP];
		decouplingInputPairs = new HashMap<>();
		localInputPairs = new HashMap[numDTP];
		timepoints = new HashMap<String, Integer>();
		for(int i= 0; i < numDTP; i++){
			localConstraints[i] = new ArrayList<DisjunctiveTemporalConstraint>();
			localInputPairs[i] = new HashMap<String, Vector<MappedPack> >();
		} 
		decouplingTimepoints.add(zero);
		tpHash.put("zero",zero);
		timepoints.put("zero", -1);
		for(ArrayList<Timepoint> v : localTimepoints){
			v.add(zero);
		}
		for(ArrayList<Timepoint> v : interfaceTimepoints){
			v.add(zero);
		}
	}
	
	//Per Lynn's notes, ExhaustiveDTP is now defunct.  Earlier version of DUTP
	public DisjunctiveTemporalProblem loadExhaustiveDTP(String xmlString) {
		// TODO Add functionality to handle cases other than SimpleDTP
		HashSet<DisjunctiveTemporalProblem> dtps = new HashSet<DisjunctiveTemporalProblem>();
		SimpleDTP originalDTP = loadSimpleDTP(XMLParser.getTrimmedByTag(xmlString, "simpleDTP"));
		SimpleDTP augmentDTP = originalDTP.clone();
		xmlString = XMLParser.getTrimmedByTag(xmlString, "exhaustiveConstraint");
		// TODO: parse the above for components. then parse the components for constraints. 
		
		exhaustiveConstraint = new ArrayList<ArrayList<DisjunctiveTemporalConstraint>>();
		// first parse new activities that are part of this exhaustive constraint 
		while(XMLParser.containsTag(xmlString, "activity")){
			String activity = XMLParser.getTrimmedByTag(xmlString, "activity");
			String name = XMLParser.getTrimmedByTag(activity, "name");
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			augmentDTP.addContingentTimepoint(start);
			augmentDTP.addContingentTimepoint(end);
			//augmentDTP.updateInternalData();
			ArrayList<DisjunctiveTemporalConstraint> dtcs = new ArrayList<DisjunctiveTemporalConstraint>();
			dtcs.addAll(parseDuration(activity, start, end));
			dtcs.addAll(parseAvailability(activity, start, end));
			augmentDTP.addAdditionalConstraints(dtcs); //TODO: this might not properly capture these duration and availability constraints..
			xmlString = XMLParser.removeFirstTag(xmlString, "activity");

		}
		while(XMLParser.containsTag(xmlString, "component")){
			//parse the components. Each component becomes a list of constraints 
			DisjunctiveTemporalProblem newDTP = augmentDTP.clone();
			ArrayList<DisjunctiveTemporalConstraint> component = new ArrayList<DisjunctiveTemporalConstraint>();
			String componentString = XMLParser.getTrimmedByTag(xmlString, "component");
			while(XMLParser.containsTag(componentString, "constraint")){
				// ACTUALLY PARSE THE CONSTRAINTS 
				String constraint = XMLParser.getTrimmedByTag(componentString, "constraint");
				ArrayList<DisjunctiveTemporalConstraint> dtcs = parseConstraintAndAdd(constraint, newDTP); 
				// add parsed constraint(s) to the component list above. 
				if(dtcs.contains(null)) System.out.println("APPENDING A NULL DTC!");
				component.addAll(dtcs);
				componentString = XMLParser.removeFirstTag(componentString, "constraint");
			}
			exhaustiveConstraint.add(component);
			dtps.add(newDTP);
			component.clear();
			xmlString = XMLParser.removeFirstTag(xmlString, "component");
		}
		return new ExhaustiveDTP(originalDTP, exhaustiveConstraint, dtps);
	}
	
	public MultiagentDTP loadMultiagentDTP(String xmlString){
		int numAgent = Integer.parseInt(XMLParser.getTrimmedByTag(xmlString, "numAgent"));
		xmlString = XMLParser.removeFirstTag(xmlString, "numAgent");
		initialize(numAgent);
		
		//first load in the local DTPs
		for(int i = 0; i < numAgent; i++){
			subDTPs[i] = new ProblemLoader().loadDTP(xmlString);
			if(xmlString.startsWith("<multiagentDTP>")){
				xmlString = XMLParser.removeFirstTag(xmlString, "multiagentDTP");
			}
			else if(xmlString.startsWith("<multiDTP>")){
				xmlString = XMLParser.removeFirstTag(xmlString, "multiDTP");
			}
			else if(xmlString.startsWith("<simpleDTP>")){
				xmlString = XMLParser.removeFirstTag(xmlString, "simpleDTP");			
			}
			for(Timepoint tp : subDTPs[i].getTimepoints()){
				if(tp.equals(zero)) continue;
				timepoints.put(tp.name, i);
				tpHash.put(tp.name, tp);
			}
		}
		
		//now parse the interagent constraints
		while(XMLParser.containsTag(xmlString, "constraint")){
			String constraint = XMLParser.getTrimmedByTag(xmlString, "constraint");
			parseConstraint(constraint);
			xmlString = XMLParser.removeFirstTag(xmlString, "constraint");
		}
		//now cleanup the agent DTPs w.r.t. the interagent constraints
		for(int i = 0; i < subDTPs.length; i++){
			subDTPs[i].addInterfaceTimepoints(interfaceTimepoints[i]);
		}
		
		decouplingDTP = new AgentDTP(id++,decouplingConstraints,decouplingTimepoints,new ArrayList<Timepoint>(decouplingTimepoints),decouplingInputPairs);

		return new MultiagentDTP(zero, decouplingDTP, subDTPs, timepoints, new ArrayList<Timepoint>());
	}

	public DUTP loadDUTP(String xmlString){
		if(!(xmlString.startsWith("<simpleDTP>"))) {
			throw new java.lang.IllegalArgumentException("Unsupported dtp type for DUDTP specification");
		}
		DisjunctiveTemporalProblem dtp = new ProblemLoader().loadDTP(xmlString);
		xmlString = XMLParser.removeFirstTag(xmlString, "simpleDTP");
			
		//now parse the sporadic events
		//TODO: The code assumes there is exactly 1 sporadic event
		//TODO: It also assumes the static variables for the DUTP and DUSTP classes
//		while(XMLParser.containsTag(xmlString, "sporadicEvent")){
			String SE = XMLParser.getTrimmedByTag(xmlString, "sporadicEvent");
			String name = XMLParser.getTrimmedByTag(SE, "name");
			DUTP.SPORADICNAME = name;
			DUSTP.SPORADICNAME = name;
			int duration = Integer.parseInt(XMLParser.getTrimmedByTag(SE, "duration"));
			DUTP.DUR = duration;
			DUSTP.DUR = duration;
			int interval = Integer.parseInt(XMLParser.getTrimmedByTag(SE, "activeInterval"));
			DUTP.SporadicIntervalLength = interval;
			DUSTP.SporadicIntervalLength = interval;
			int seEST = Integer.parseInt(XMLParser.getTrimmedByTag(SE, "est"));
			DUTP.SporadicEventET = seEST;
			DUSTP.SporadicEventET = seEST;
			int seLET = Integer.parseInt(XMLParser.getTrimmedByTag(SE, "let"));
			DUTP.SporadicEventLT = seLET;
			DUSTP.SporadicEventLT = seLET;
			int precedence = Integer.parseInt(XMLParser.getTrimmedByTag(SE, "precedence"));			
			xmlString = XMLParser.removeFirstTag(xmlString, "sporadicEvent");
//	}

		return new DUTP(dtp, true, precedence);
	}
	
	public SimpleDTP loadSimpleDTP(String xmlString){
		initialize(1);
		//now parse and create all the timepoints.  have to do this before parsing the constraints because constraints could be specified in any order
		String xmlStringCopy = new String(xmlString);
		while(XMLParser.containsTag(xmlStringCopy, "activity")){
			String activity = XMLParser.getTrimmedByTag(xmlStringCopy, "activity");
			String name = XMLParser.getTrimmedByTag(activity,"name");
			ArrayList<DisjunctiveTemporalConstraint> dtc = localConstraints[0];
			Timepoint start = new Timepoint(name+"_S",1);
			Timepoint end = new Timepoint(name+"_E",1);
			tpHash.put(start.getName(),start);
			tpHash.put(end.getName(),end);
			localTimepoints[0].add(start); // Drew: zero timepoint is already added in the initialization
			localTimepoints[0].add(end);
			timepoints.put(name+"_S", 0);
			timepoints.put(name+"_E", 0);
			xmlStringCopy = XMLParser.removeFirstTag(xmlStringCopy, "activity");
		}
		
		//now parse and add the constraints associated with an activity
		while(XMLParser.containsTag(xmlString, "activity")){
			String activity = XMLParser.getTrimmedByTag(xmlString, "activity");
			//first get activity name, and create associated timepoints
			SimpleEntry<Timepoint, Timepoint> tpSE = parseTimepoint(activity,"name");
			Timepoint start = tpSE.getKey();
			Timepoint end = tpSE.getValue();
			ArrayList<DisjunctiveTemporalConstraint> dtc = localConstraints[0];
			
			//get duration bounds
			dtc.addAll(parseDuration(activity,start,end));
			
			//check and add constraints for earliest-/latest- start-/end- times
			dtc.addAll(parseAvailability(activity,start,end));
					
			xmlString = XMLParser.removeFirstTag(xmlString, "activity");
		}
				
		//now parse and add any remaining constraints
		while(XMLParser.containsTag(xmlString, "constraint")){
			String constraint = XMLParser.getTrimmedByTag(xmlString, "constraint");
			parseConstraint(constraint);
			xmlString = XMLParser.removeFirstTag(xmlString, "constraint");
		}

		return new AgentDTP(id++,localConstraints[0],localTimepoints[0],interfaceTimepoints[0],localInputPairs[0]);
	}
	
	public MultiDTP loadMultiDTP(String xmlString){			
		//first do some data structure setup and initialization
		int numDTP = Integer.parseInt(XMLParser.getTrimmedByTag(xmlString, "numDTP"));
		initialize(numDTP);
		
		//now parse and create all the timepoints.  have to do this before parsing the constraints because constraints could be specified in any order
		String xmlStringCopy = new String(xmlString);
		while(XMLParser.containsTag(xmlStringCopy, "activity")){
			String activity = XMLParser.getTrimmedByTag(xmlStringCopy, "activity");
			String name = XMLParser.getTrimmedByTag(activity,"name");
			int dtpIdx = Integer.parseInt(XMLParser.getTrimmedByTag(xmlStringCopy, "dtpIdx"));
			ArrayList<DisjunctiveTemporalConstraint> dtc = localConstraints[dtpIdx];
			Timepoint start = new Timepoint(name+"_S",numDTP+1);
			Timepoint end = new Timepoint(name+"_E",numDTP+1);
			tpHash.put(start.getName(),start);
			tpHash.put(end.getName(),end);
			localTimepoints[dtpIdx].add(start);
			localTimepoints[dtpIdx].add(end);
			timepoints.put(name+"_S", dtpIdx);
			timepoints.put(name+"_E", dtpIdx);
			xmlStringCopy = XMLParser.removeFirstTag(xmlStringCopy, "activity");
		}
		
		//now parse and add the constraints associated with an activity
		while(XMLParser.containsTag(xmlString, "activity")){
			String activity = XMLParser.getTrimmedByTag(xmlString, "activity");
			//first get activity name, and create associated timepoints
			SimpleEntry<Timepoint, Timepoint> tpSE = parseTimepoint(activity,"name");
			Timepoint start = tpSE.getKey();
			Timepoint end = tpSE.getValue();
			int dtpIdx = Integer.parseInt(XMLParser.getTrimmedByTag(xmlString, "dtpIdx"));
			ArrayList<DisjunctiveTemporalConstraint> dtc = localConstraints[dtpIdx];
			activity = XMLParser.removeFirstTag(activity, "dtpIdx");
			
			//get duration bounds
			dtc.addAll(parseDuration(activity,start,end));
			//check and add constraints for earliest-/latest- start-/end- times
			dtc.addAll(parseAvailability(activity,start,end));
					
			xmlString = XMLParser.removeFirstTag(xmlString, "activity");
		}
				
		//now parse and add any remaining constraints
		while(XMLParser.containsTag(xmlString, "constraint")){
			String constraint = XMLParser.getTrimmedByTag(xmlString, "constraint");
			parseConstraint(constraint);
			xmlString = XMLParser.removeFirstTag(xmlString, "constraint");
		}

		decouplingDTP = new AgentDTP(id++,decouplingConstraints,decouplingTimepoints,new ArrayList<Timepoint>(decouplingTimepoints), decouplingInputPairs);
		for(int i = 0; i < numDTP; i++){
			subDTPs[i] = new AgentDTP(id++,localConstraints[i],localTimepoints[i],interfaceTimepoints[i], localInputPairs[i]);
		}
		return new MultiDTP(zero, decouplingDTP, subDTPs, timepoints, new ArrayList<Timepoint>());
	}
	
	public static int parseVal(String xmlString, String tag, int defaultVal){
		int r = defaultVal;
		if(XMLParser.containsTag(xmlString, tag)){
			r = Integer.parseInt(XMLParser.getTrimmedByTag(xmlString, tag));
			XMLParser.removeFirstTag(xmlString, tag);
		}
		return r;
	}
	
	public SimpleEntry<Timepoint, Timepoint> parseTimepoint(String xmlString, String tag){
		String name = XMLParser.getTrimmedByTag(xmlString, tag);
		xmlString = XMLParser.removeFirstTag(xmlString, tag);
		Timepoint tS = tpHash.get(name+"_S");
		Timepoint tE = tpHash.get(name+"_E");
		return new SimpleEntry<Timepoint,Timepoint>(tS,tE);
	}
	
	public SimpleEntry<Timepoint, Timepoint> parseTimepoint(DisjunctiveTemporalProblem dtp, String xmlString, String tag){
		String name = XMLParser.getTrimmedByTag(xmlString, tag);
		xmlString = XMLParser.removeFirstTag(xmlString, tag);
		Timepoint tS = dtp.getTimepoint(name+"_S");
		Timepoint tE = dtp.getTimepoint(name+"_E");
		return new SimpleEntry<Timepoint,Timepoint>(tS,tE);
	}
	
	public Collection<DisjunctiveTemporalConstraint> parseAvailability(String xmlString, Timepoint start, Timepoint end){
		if(!XMLParser.containsTag(xmlString, "availability")){
			return new LinkedList<DisjunctiveTemporalConstraint>();
		}
		//default values of 0 for earliest start-/end- times, infinite for latest start-/end-times
		ArrayList<TemporalDifference> eStart = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> lStart = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> eEnd = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> lEnd = new ArrayList<TemporalDifference>();
		while(XMLParser.containsTag(xmlString, "availability")){
			String availability = XMLParser.getTrimmedByTag(xmlString, "availability");
			int est = parseVal(availability,"est",0);
			int lst = parseVal(availability,"lst",Integer.MAX_VALUE);
			int eet = parseVal(availability,"eet",0);
			int let = parseVal(availability,"let",Integer.MAX_VALUE);
			eStart.add(new TemporalDifference(zero,start,-est));
			lStart.add(new TemporalDifference(start,zero,lst));
			eEnd.add(new TemporalDifference(zero,end,-eet));
			lEnd.add(new TemporalDifference(end,zero,let));

			xmlString = XMLParser.removeFirstTag(xmlString, "availability");
		}
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(eStart);
		tdVec.add(lStart);
		tdVec.add(eEnd);
		tdVec.add(lEnd);
		return DisjunctiveTemporalConstraint.crossProduct(tdVec);
	}
	
	public static Collection<DisjunctiveTemporalConstraint> parseDuration(String xmlString, Timepoint start, Timepoint end){
		if(!XMLParser.containsTag(xmlString, "duration")){
			return new LinkedList<DisjunctiveTemporalConstraint>();
		}
		ArrayList<TemporalDifference> minDurs = new ArrayList<TemporalDifference>();
		ArrayList<TemporalDifference> maxDurs = new ArrayList<TemporalDifference>();
		boolean optionalActivity = false;
		while(XMLParser.containsTag(xmlString,"duration")){
			String duration = XMLParser.getTrimmedByTag(xmlString,"duration");
			int minDuration = parseVal(duration,"min",0);
			int maxDuration = parseVal(duration,"max",Integer.MAX_VALUE);
			minDurs.add(new TemporalDifference(start,end,-minDuration));
			maxDurs.add(new TemporalDifference(end,start,maxDuration));
			if(minDuration == 0 && maxDuration == 0) optionalActivity = true;
			xmlString = XMLParser.removeFirstTag(xmlString, "duration");
		}
		optionalActivityHash.put(start.name,optionalActivity);
		ArrayList<ArrayList<TemporalDifference>> tdVec = new ArrayList<ArrayList<TemporalDifference>>();
		tdVec.add(minDurs);
		tdVec.add(maxDurs);
		return DisjunctiveTemporalConstraint.crossProduct(tdVec);
	}
	
					
//TODO: extend code to work with the optionalActivity information for other constraint types besides ordering
	public void parseConstraint(String constraint){
		String type = XMLParser.getTrimmedByTag(constraint, "type");
		constraint = XMLParser.removeFirstTag(constraint, "type");
		//System.out.println(type);
		switch(type){
		
		//forces source_E to come before destination_S by [min_duration, max_duration], default min= 0, max = MAX_VALUE
		case "ordering":
			SimpleEntry<Timepoint, Timepoint> tpSource = parseTimepoint(constraint,"source");
			Timepoint sourceT = tpSource.getValue();
			SimpleEntry<Timepoint, Timepoint> tpDest = parseTimepoint(constraint,"destination");
			Timepoint destT = tpDest.getKey();
			int sourceDTPi = timepoints.get(sourceT.name);
			int destDTPi = timepoints.get(destT.name);
			Boolean containsMin = false;
			Boolean containsMax = false;
			if(sourceDTPi != destDTPi){
				decouplingTimepoints.add(sourceT);
				decouplingTimepoints.add(destT);
				interfaceTimepoints[sourceDTPi].add(sourceT);
				interfaceTimepoints[destDTPi].add(destT);
			}
			
			if(XMLParser.containsTag(constraint, "min_duration")){
				int x = parseVal(constraint,"min_duration",0);
				DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(sourceT,destT,-x));
				//must only satisfy ordering constraint if the optionalActivity is selected
				if(optionalActivityHash.get(tpSource.getKey().name)) dtc.add(new TemporalDifference(sourceT,tpSource.getKey(),0));
				if(sourceDTPi != destDTPi){
					decouplingConstraints.add(dtc);
				}	
				else{
					localConstraints[sourceDTPi].add(dtc);				
				}
				containsMin = true;
			}
			if(XMLParser.containsTag(constraint, "max_duration")){
				int x = parseVal(constraint,"max_duration",Integer.MAX_VALUE);					
				DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(destT,sourceT,x));
				//must only satisfy ordering constraint if the optionalActivity is selected
				if(optionalActivityHash.get(tpSource.getKey().name)) dtc.add(new TemporalDifference(sourceT,tpSource.getKey(), 0));
				if(sourceDTPi != destDTPi){
					decouplingConstraints.add(dtc);
				}	
				else{
					localConstraints[sourceDTPi].add(dtc);				
				}
				containsMax = true;
			}
			
			String sourceName = sourceT.getName().substring(0, sourceT.getName().lastIndexOf('_'));
			String destName = destT.getName().substring(0, destT.getName().lastIndexOf('_'));
			
			/* Always true in this case.
			 * In other cases, true if
			 * 1) source comes before dest and the constraint is between s_E and d_S, or
			 * 2) source comes after dest and the constraint is between s_S and d_E.
			 * False otherwise.
			 */
			Boolean highPriOrder = true;
			
			/* True if
			 * 1) neither min nor max is specified, or
			 * 2) min is specified.
			 * False only if
			 * 1) min is not specified while max is specified.
			 */
			Boolean gt = true;
			if(!containsMin && containsMax)
				gt = false;

			/* True if this pair of activity is already stored in loaderInputPairs 
			 */			
			Boolean contains = false;
			
			if(sourceDTPi != destDTPi){
				if(!decouplingInputPairs.containsKey(sourceName))
					decouplingInputPairs.put(sourceName, new Vector<MappedPack>());
				if(!decouplingInputPairs.containsKey(destName))
					decouplingInputPairs.put(destName, new Vector<MappedPack>());
				for(MappedPack it : decouplingInputPairs.get(sourceName)){
					if(it.activity.equals(destName)){
						contains = true;
						if(highPriOrder){
							//update source's vec
							it.gt = gt;
							it.keyS = false;
							it.valueS = true;
							
							//update dest's vec
							for(MappedPack itOpposite : decouplingInputPairs.get(destName)){
								if(itOpposite.activity.equals(sourceName)){
									itOpposite.gt = gt;
									itOpposite.keyS = true;
									itOpposite.valueS = false;
									break;
								}
							}
						}
						break;
					}
				}
				if(!contains){
					decouplingInputPairs.get(sourceName).add(new MappedPack(destName, gt, false, true, false));
					decouplingInputPairs.get(destName).add(new MappedPack(sourceName, gt, true, false, false));
				}				
			}
			else{
				//same sub dtp
				if(!localInputPairs[sourceDTPi].containsKey(sourceName))
					localInputPairs[sourceDTPi].put(sourceName, new Vector<MappedPack>());
				if(!localInputPairs[sourceDTPi].containsKey(destName))
					localInputPairs[sourceDTPi].put(destName, new Vector<MappedPack>());
				for(MappedPack it : localInputPairs[sourceDTPi].get(sourceName)){
					if(it.activity.equals(destName)){
						contains = true;
						if(highPriOrder){
							//update source's vec
							it.gt = gt;
							it.keyS = false;
							it.valueS = true;
							
							//update dest's vec
							for(MappedPack itOpposite : localInputPairs[sourceDTPi].get(destName)){
								if(itOpposite.activity.equals(sourceName)){
									itOpposite.gt = gt;
									itOpposite.keyS = true;
									itOpposite.valueS = false;
									break;
								}
							}
						}
						break;
					}
				}
				if(!contains){
					localInputPairs[sourceDTPi].get(sourceName).add(new MappedPack(destName, gt, false, true, false));
					localInputPairs[sourceDTPi].get(destName).add(new MappedPack(sourceName, gt, true, false, false));
				}				
			}


			break;
			
		//forces source and destination activities to not be overlapping
		case "nonconcurrent":
			tpSource = parseTimepoint(constraint,"source");
			Timepoint sourceS = tpSource.getKey();
			Timepoint sourceE = tpSource.getValue();			
			tpDest = parseTimepoint(constraint,"destination");
			Timepoint destS = tpDest.getKey();
			Timepoint destE = tpDest.getValue();
			DisjunctiveTemporalConstraint cons = new DisjunctiveTemporalConstraint(new TemporalDifference(destE,sourceS,0));
			cons.add(new TemporalDifference(sourceE,destS,0));
			
			sourceDTPi = timepoints.get(sourceS.name);
			//System.out.println(constraint);
			destDTPi = timepoints.get(destS.name);
			if(sourceDTPi != destDTPi){
				decouplingTimepoints.add(sourceS);
				decouplingTimepoints.add(destS);
				decouplingTimepoints.add(sourceE);
				decouplingTimepoints.add(destE);
				interfaceTimepoints[sourceDTPi].add(sourceS);
				interfaceTimepoints[sourceDTPi].add(sourceE);
				interfaceTimepoints[destDTPi].add(destS);
				interfaceTimepoints[destDTPi].add(destE);
				decouplingConstraints.add(cons);
			}
			else{
				localConstraints[sourceDTPi].add(cons);
			}
			
			sourceName = sourceS.getName().substring(0, sourceS.getName().lastIndexOf('_'));
			destName = destS.getName().substring(0, destS.getName().lastIndexOf('_'));

			break;
			
		//forces source and destination activities to be synchronized so that difference between start timepoints <= start, and difference between end timepoints <= end
		//default start and end = MAX_VALUE
		case "synchronized":
			tpSource = parseTimepoint(constraint,"source");
			sourceS = tpSource.getKey();
			sourceE = tpSource.getValue();			
			tpDest = parseTimepoint(constraint,"destination");
			destS = tpDest.getKey();
			destE = tpDest.getValue();
			int start = parseVal(constraint, "start", Integer.MAX_VALUE);	
			int end = parseVal(constraint, "end", Integer.MAX_VALUE);
			sourceDTPi = timepoints.get(sourceS.name);
			destDTPi = timepoints.get(destS.name);
			Collection<DisjunctiveTemporalConstraint> consCol = new LinkedList<DisjunctiveTemporalConstraint>(); 
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(sourceS, destS, start)));
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(sourceE, destE, end)));
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(destE, sourceE, end)));
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(destS, sourceS, start)));
			if(sourceDTPi != destDTPi){
				decouplingTimepoints.add(sourceS);
				decouplingTimepoints.add(destS);
				decouplingTimepoints.add(sourceE);
				decouplingTimepoints.add(destE);
				interfaceTimepoints[sourceDTPi].add(sourceS);
				interfaceTimepoints[sourceDTPi].add(sourceE);
				interfaceTimepoints[destDTPi].add(destS);
				interfaceTimepoints[destDTPi].add(destE);
				decouplingConstraints.addAll(consCol);				
			}
			else{
				localConstraints[sourceDTPi].addAll(consCol);
			}
			//System.out.println("breaking in synchronized");
			sourceName = sourceS.getName().substring(0, sourceS.getName().lastIndexOf('_'));
			destName = destS.getName().substring(0, destS.getName().lastIndexOf('_'));
			contains = false;
			highPriOrder = true;
			gt = true;
			
			if(sourceDTPi != destDTPi){
				if(!decouplingInputPairs.containsKey(sourceName))
					decouplingInputPairs.put(sourceName, new Vector<MappedPack>());
				if(!decouplingInputPairs.containsKey(destName))
					decouplingInputPairs.put(destName, new Vector<MappedPack>());
				for(MappedPack it : decouplingInputPairs.get(sourceName)){
					if(it.activity.equals(destName)){
						contains = true;
						if(highPriOrder){
							//update source's vec
							it.gt = gt;
							it.keyS = false;
							it.valueS = true;
							
							//update dest's vec
							for(MappedPack itOpposite : decouplingInputPairs.get(destName)){
								if(itOpposite.activity.equals(sourceName)){
									itOpposite.gt = gt;
									itOpposite.keyS = true;
									itOpposite.valueS = false;
									break;
								}
							}
						}
						break;
					}
				}
				if(!contains){
					decouplingInputPairs.get(sourceName).add(new MappedPack(destName, gt, false, true, true));
					decouplingInputPairs.get(destName).add(new MappedPack(sourceName, gt, true, false, true));
				}				
			}
			else{
				//same sub dtp
				if(!localInputPairs[sourceDTPi].containsKey(sourceName))
					localInputPairs[sourceDTPi].put(sourceName, new Vector<MappedPack>());
				if(!localInputPairs[sourceDTPi].containsKey(destName))
					localInputPairs[sourceDTPi].put(destName, new Vector<MappedPack>());
				for(MappedPack it : localInputPairs[sourceDTPi].get(sourceName)){
					if(it.activity.equals(destName)){
						contains = true;
						if(highPriOrder){
							//update source's vec
							it.gt = gt;
							it.keyS = false;
							it.valueS = true;
							
							//update dest's vec
							for(MappedPack itOpposite : localInputPairs[sourceDTPi].get(destName)){
								if(itOpposite.activity.equals(sourceName)){
									itOpposite.gt = gt;
									itOpposite.keyS = true;
									itOpposite.valueS = false;
									break;
								}
							}
						}
						break;
					}
				}
				if(!contains){
					localInputPairs[sourceDTPi].get(sourceName).add(new MappedPack(destName, gt, false, true, true));
					localInputPairs[sourceDTPi].get(destName).add(new MappedPack(sourceName, gt, true, false, true));
				}				
			}

			break;
			
		//forces source and destination activities to be mutually exclusive
		case "exclusive":
			tpSource = parseTimepoint(constraint,"source");
			sourceS = tpSource.getKey();
			sourceE = tpSource.getValue();			
			tpDest = parseTimepoint(constraint,"destination");
			destS = tpDest.getKey();
			destE = tpDest.getValue();
			ArrayList<ArrayList<TemporalDifference>> diffs = new ArrayList<ArrayList<TemporalDifference>>();
			ArrayList<TemporalDifference> vec1 = new ArrayList<TemporalDifference>();
			ArrayList<TemporalDifference> vec2 = new ArrayList<TemporalDifference>();
			vec1.add(new TemporalDifference(sourceS, sourceE, 0));
			vec2.add(new TemporalDifference(sourceE, sourceS, 0));
			vec1.add(new TemporalDifference(destS, destE, 0));
			vec2.add(new TemporalDifference(destE, destS, 0));
			diffs.add(vec1);
			diffs.add(vec2);
			Collection<DisjunctiveTemporalConstraint> crossConstraints = DisjunctiveTemporalConstraint.crossProduct(diffs);
			DisjunctiveTemporalConstraint ensureOneNonZero = new DisjunctiveTemporalConstraint(new TemporalDifference(sourceS,sourceE,-1));
			ensureOneNonZero.add(new TemporalDifference(destS,destE,-1));
			crossConstraints.add(ensureOneNonZero);
			
			sourceDTPi = timepoints.get(sourceS.name);
			destDTPi = timepoints.get(destS.name);
			if(sourceDTPi != destDTPi){
				decouplingTimepoints.add(sourceS);
				decouplingTimepoints.add(destS);
				decouplingTimepoints.add(sourceE);
				decouplingTimepoints.add(destE);
				interfaceTimepoints[sourceDTPi].add(sourceS);
				interfaceTimepoints[sourceDTPi].add(sourceE);
				interfaceTimepoints[destDTPi].add(destS);
				interfaceTimepoints[destDTPi].add(destE);
				decouplingConstraints.addAll(crossConstraints);				
			}
			else{
				localConstraints[sourceDTPi].addAll(crossConstraints);
			}
			break;
		default:
			throw new java.lang.UnsupportedOperationException("Unknown constraint type \""+type+"\"");
		}
	}
	
	public ArrayList<DisjunctiveTemporalConstraint> parseConstraintAndAdd(String constraint, DisjunctiveTemporalProblem dtp){
		String type = XMLParser.getTrimmedByTag(constraint, "type");
		constraint = XMLParser.removeFirstTag(constraint, "type");
		ArrayList<DisjunctiveTemporalConstraint> ret = new ArrayList<DisjunctiveTemporalConstraint>();
		//System.out.println(type);
		switch(type){
		
		//forces source_E to come before destination_S by [min_duration, max_duration], default min= 0, max = MAX_VALUE
		case "ordering":
			SimpleEntry<Timepoint, Timepoint> tpSource = parseTimepoint(dtp, constraint,"source");
			Timepoint sourceT = tpSource.getValue();
			if(sourceT == null) System.out.println("SOURCE IS NULL");
			SimpleEntry<Timepoint, Timepoint> tpDest = parseTimepoint(dtp, constraint,"destination");
			Timepoint destT = tpDest.getKey();
			if(destT == null) System.out.println("DEST IS NULL");
			
			if(XMLParser.containsTag(constraint, "min_duration")){
				int x = parseVal(constraint,"min_duration",0);
				DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(sourceT,destT,-x));
				//must only satisfy ordering constraint if the optionalActivity is selected
				if(optionalActivityHash.get(tpSource.getKey().name)) dtc.add(new TemporalDifference(sourceT,tpSource.getKey(),0));

				dtp.addVolitionalConstraint(dtc);
				ret.add(dtc);
				

			}
			if(XMLParser.containsTag(constraint, "max_duration")){
				int x = parseVal(constraint,"max_duration",Integer.MAX_VALUE);					
				DisjunctiveTemporalConstraint dtc = new DisjunctiveTemporalConstraint(new TemporalDifference(destT,sourceT,x));
				//must only satisfy ordering constraint if the optionalActivity is selected
				
				
				dtp.addVolitionalConstraint(dtc);
				ret.add(dtc);
							
			}
			return ret;
			
		//forces source and destination activities to not be overlapping
		case "nonconcurrent":
			tpSource = parseTimepoint(dtp, constraint,"source");
			Timepoint sourceS = tpSource.getKey();
			Timepoint sourceE = tpSource.getValue();			
			tpDest = parseTimepoint(dtp, constraint,"destination");
			Timepoint destS = tpDest.getKey();
			Timepoint destE = tpDest.getValue();
			DisjunctiveTemporalConstraint cons = new DisjunctiveTemporalConstraint(new TemporalDifference(destE,sourceS,0));
			cons.add(new TemporalDifference(sourceE,destS,0));
			
			dtp.addVolitionalConstraint(cons);
			ret.add(cons);
			return ret;
			
		//forces source and destination activities to be synchronized so that difference between start timepoints <= start, and difference between end timepoints <= end
		//default start and end = MAX_VALUE
		case "synchronized":
			tpSource = parseTimepoint(dtp, constraint,"source");
			sourceS = tpSource.getKey();
			sourceE = tpSource.getValue();			
			tpDest = parseTimepoint(dtp, constraint,"destination");
			destS = tpDest.getKey();
			destE = tpDest.getValue();
			int start = parseVal(constraint, "start", Integer.MAX_VALUE);	
			int end = parseVal(constraint, "end", Integer.MAX_VALUE);
	
			Collection<DisjunctiveTemporalConstraint> consCol = new LinkedList<DisjunctiveTemporalConstraint>(); 
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(sourceS, destS, start)));
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(sourceE, destE, end)));
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(destE, sourceE, end)));
			consCol.add(new DisjunctiveTemporalConstraint(new TemporalDifference(destS, sourceS, start)));
			
			dtp.addVolitionalConstraints(consCol);
			ret.addAll(consCol);
			System.out.println("breaking in synchronized");
			return ret;
			
		//forces source and destination activities to be mutually exclusive
		case "exclusive":
			tpSource = parseTimepoint(dtp, constraint,"source");
			sourceS = tpSource.getKey();
			sourceE = tpSource.getValue();			
			tpDest = parseTimepoint(dtp, constraint,"destination");
			destS = tpDest.getKey();
			destE = tpDest.getValue();
			ArrayList<ArrayList<TemporalDifference>> diffs = new ArrayList<ArrayList<TemporalDifference>>();
			ArrayList<TemporalDifference> vec1 = new ArrayList<TemporalDifference>();
			ArrayList<TemporalDifference> vec2 = new ArrayList<TemporalDifference>();
			vec1.add(new TemporalDifference(sourceS, sourceE, 0));
			vec2.add(new TemporalDifference(sourceE, sourceS, 0));
			vec1.add(new TemporalDifference(destS, destE, 0));
			vec2.add(new TemporalDifference(destE, destS, 0));
			diffs.add(vec1);
			diffs.add(vec2);
			Collection<DisjunctiveTemporalConstraint> crossConstraints = DisjunctiveTemporalConstraint.crossProduct(diffs);
			DisjunctiveTemporalConstraint ensureOneNonZero = new DisjunctiveTemporalConstraint(new TemporalDifference(sourceS,sourceE,-1));
			ensureOneNonZero.add(new TemporalDifference(destS,destE,-1));
			crossConstraints.add(ensureOneNonZero);
			
			
			dtp.addVolitionalConstraints(crossConstraints);
			ret.addAll(crossConstraints);
			return ret;
			
		default:
			throw new java.lang.UnsupportedOperationException("Unknown constraint type \""+type+"\"");
		}
	}
}
