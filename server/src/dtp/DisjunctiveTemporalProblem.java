package dtp;

import interval.Interval;
import interval.IntervalSet;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;

import interactionFramework.MappedPack;

import java.util.*;

import stp.Timepoint;

public interface DisjunctiveTemporalProblem {
		
	
	/**
	 * Solve the DTP for all possible solutions by establishing minimality on the entire network for all component STNs.
	 * Equivalent to establishMinimality(allTimepoints, scheduleTime).
	 * @param scheduleTime
	 */
	public void enumerateSolutions(int scheduleTime);
	
	/**
	 * Anytime version of enumerateSolutions.  Solves for the next solution on the entire network of timepoints.
	 * Equivalent to solveNext(allTimepoints, scheduleTime).
	 * @param scheduleTime
	 */
	public boolean nextSolution(int scheduleTime);
	
	/**
	 * Solve the DTP for all possible outgoing influences by establishing minimality on the interface timepoints for all component STNs.
	 * Other timepoints in the DTP will contain only values that permit a satisfying schedule, but may be only a subset of such values.
	 * Equivalent to establishMinimality(iterfaceTimepoints, scheduleTime).
	 * @param scheduleTime
	 */
	public void enumerateInfluences(int scheduleTime);
	
	/**
	 * Anytime version of enumerateInfluences.  Solves for the next outgoing influence on the interface timepoints.
	 * Equivalent to solveNext(iterfaceTimepoints, scheduleTime).
	 * @param scheduleTime
	 * @return true if there was another influence, false if no more influences could be found
	 */
	public boolean nextInfluence(int scheduleTime);
	
	/**
	 * Solve the DTP for all possible values of the timepoints by establishing minimality on them.
	 * Other timepoints in the DTP will contain only values that permit a satisfying schedule, but may be only a subset of such values.
	 * @param scheduleTime
	 * @param timepointsToConsider
	 */
	public void establishMinimality(List<Timepoint> timepointsToConsider, int scheduleTime);
	
	/**
	 * Anytime version of establishMinimaility.  Solves for the next solution on the set of timepoints
	 * @param scheduleTime
	 * @param timepointsToConsider
	 * @return true if there was another solution, false if no more solutions could be found.
	 */
	public boolean solveNext(List<Timepoint> timepointsToConsider, int scheduleTime);
	
	/**
	 * Advance the DTP to time.  Adds new constraints to the DTP to force available schedule choices to be no earlier than time.
	 * Equivalent to advanceToTime(time,true,deltaTime,pushSelection,getMinTimeArray())
	 * @param time to advance the DTP to
	 * @param deltaTime the increment that time is advancing
	 * @param pushSelection whether to push onto the selection stack
	 */
	public void advanceToTime(int time, int deltaTime, boolean pushSelection);
	
	/**
	 * Equivalent to advanceToTime(time,resolve,deltaTime,pushSelection,getMinTimeArray())
	 * Advance the DTP to time.  Adds new constraints to the DTP to force available schedule choices to be no earlier than time.
	 * @param time to advance the DTP to
	 * @param resolve whether to resolve the DTP or not
	 * @param deltaTime the increment that time is advancing
	 * @param pushSelection whether to push onto the selection stack
	 */
	public void advanceToTime(int time, boolean resolve, int deltaTime, boolean pushSelection);

	/**
	 * Tightens two timepoints relative to the zero timepoint
	 * Equivalent to tightenTimepoint(timeStart, tp1, timeEnd, tp2, true, deltaTime, pushSelection)
	 * @param timeStart Value to fix tp1 to
	 * @param tp1 First timepoint
	 * @param timeEnd Value to fix tp2 to
	 * @param tp2 Second timepoint
	 * @param deltaTime the increment that time is advancing
	 * @param pushSelection whether to push onto the selection stack
	 */
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd, String tp2, int deltaTime, boolean pushSelection);
	
	/**
	 * Tightens two timepoints relative to the zero timepoint
	 * @param timeStart Value to fix tp1 to
	 * @param tp1 First timepoint
	 * @param timeEnd Value to fix tp2 to
	 * @param tp2 Second timepoint
	 * @param resolve whether to resolve the DTP
	 * @param deltaTime the increment that time is advancing
	 * @param pushSelection whether to push onto the selection stack
	 */
	public void tightenTimepoint(int timeStart, String tp1, int timeEnd, String tp2, boolean resolve, int deltaTime, boolean pushSelection);

	/**
	 * Performs tightenTimepoint and advanceToTime as an atomic action
	 * @param timeStart Value to fix tp1 to
	 * @param tp1 First timepoint
	 * @param timeEnd Value to fix tp2 to
	 * @param tp2 Second timepoint
	 * @param resolve whether to resolve the DTP
	 * @param deltaTime the increment that time is advancing
	 * @param pushSelection whether to push onto the selection stack
	 */
	public void executeAndAdvance(int timeStart, String tp1, int timeEnd, String tp2, boolean resolve, int deltaTime, boolean pushSelection);
	
	/**
	 * Creates and adds a set of DisjunctiveTemporalConstraints into the DTP based on an IntervalSet
	 * @param tpS Source timepoint for the new constraint
	 * @param tpE Destination timepoint for the new constraint
	 * @param dtc Specifies the (disjunctive) temporal bounds that the constraint should enforce
	 * @param time System time
	 * @param resolve whether to resolve the DTP
	 * @param pushSelection whether to push onto the selection stack
	 */
	public void addAdditionalConstraint(String tpS, String tpE, IntervalSet dtc, int time, boolean resolve, boolean pushSelection);

	/**
	 * Adds a collection of new constraints into the additionalConstraints variables and pushes the selectionStack.
	 * Does not re-solve the DTP.
	 * These constraints will persist over softResets()
	 */
	public void addAdditionalConstraints(Collection<DisjunctiveTemporalConstraint> col);
	
	/**
	 * Adds a collection of new constraints into the volitionalConstraints variables and pushes the selectionStack.
	 * Does not re-solve the DTP.
	 * These constraints will persist over softResets()
	 */
	public void addVolitionalConstraints(Collection<DisjunctiveTemporalConstraint> col);
	
	/**
	 * Adds a new constraint into the additionalConstraints variables and pushes the selectionStack.
	 * Does not re-solve the DTP.
	 * This constraint will persist over softResets()
	 */
	public void addAdditionalConstraint(DisjunctiveTemporalConstraint cons);
	
	/**
	 * Adds a new constraint into the volitionalConstraints variables and pushes the selectionStack.
	 * Does not re-solve the DTP.
	 * This constraint will persist over softResets()
	 */
	public void addVolitionalConstraint(DisjunctiveTemporalConstraint cons);
	
	/**
	 * Search through the timepoints and see if any IntervalSets must be 0 duration.
	 * Those timepoints are then fixed to be 0 duration
	 * @return true if an interval was fixed
	 */
	public boolean fixZeroValIntervals();
	
	/**
	 * Simplifies the minimalNetwork IntervalSets to not contain overlapping intervals
	 */
	public void simplifyMinNetIntervals();

	/**
	 * Gets the minimal time that any timepoint in the DTP could contain
	 */
	public int getMinTime();
	
	/**
	 * Returns an array of minimal times for each subcomponent of the DTP
	 */
	public int[] getMinTimeArray();

	/**
	 * Gets the maximum amount of time that each agent can wait before starting an activities and still have at least one satisfiable schedule
	 */
	public List<Integer> getMaxSlack();
	
	/**
	 * Gets the IntervalSet from tp1 to tp2
	 * @param tp1
	 * @param tp2
	 */
	public IntervalSet getInterval(String tp1, String tp2);

	public IntervalSet getIntervalGlobal(String tp1, String tp2);
	
	/**
	 * Get all timepoints in the DTP
	 */
	public Set<Timepoint> getTimepoints();

	/**
	 * Get all interface timepoints in the DTP
	 */
	public ArrayList<Timepoint> getInterfaceTimepoints();
	
	/**
	 * Get a timepoint from a string
	 */
	public Timepoint getTimepoint(String tpS);
	
	/**
	 * Returns true if the DTP contains the timepoint
	 */
	public boolean contains(String tp1);
	
	/**
	 * Get the number of solutions
	 */
	public int getNumSolutions();

	/**
	 * Get the flexibility of the DTP
	 */
	public long getTotalFlexibility();

	/**
	 * Get the rigidity of the DTP
	 */
	public double getRigidity();
	
	/**
	 * Get the number of agents in the DTP
	 */
	public int getNumAgents();

	/**
	 * Gets the current agent of the DTP
	 * @return
	 */
	public int getCurrentAgent();
	
	/**
	 * Sets the current agent of the DTP
	 * @param agent
	 */
	public void setCurrentAgent(int agent);
	
	/**
	 * Get the agent that the timepoint belongs to
	 * @param tpS
	 * @return
	 */
	public int getAgent(String tpS);
	
	/**
	 * @return An arrayList of rigidity values.  Key is sum^1_n sqrt(1/interval_n.size()), value is n
	 */
	public ArrayList<SimpleEntry<Double, Double>> getRigidityVals();

	/**
	 * Resets all solve-time variables, but doesn't undo/alter/clear any of the constraints
	 */
	public void softReset();

	/**
	 * Remove any incoming constraints in the DTP
	 */
	public void clearIncomingConstraints();

	/**
	 * Adds a incoming constraint into the DTP
	 * @param cons
	 */
	public void addIncomingConstraint(DisjunctiveTemporalConstraint cons);
	
	/**
	 * Adds a set of incoming constraints into the DTP.
	 */
	public void addIncomingConstraints(Collection<DisjunctiveTemporalConstraint> constraints);
	
	/**
	 * Overwrites the incoming constraints
	 */
	public void setIncomingConstraints(ArrayList<DisjunctiveTemporalConstraint> constraints);
	
	/**
     * Returns an ArrayList of constraints that summarize the outgoing influences of the DTP to the timepointsToDecouple.
     * Assumes the DTP contains all of the timepointsToDecouple
     * Does not eliminate any satisfying solutions; however, DTPs "decoupled" with these constraints may still have interdependencies
	 */
	public ArrayList<DisjunctiveTemporalConstraint> computeSummarizingConstraints(ArrayList<Timepoint> timepointsToDecouple);
	
	/**
     * Returns an ArrayList of constraints that fully decouple the outgoing influences of the DTP to the timepointsToDecouple.
     * Assumes the DTP contains all of the timepointsToDecouple
     * May eliminate satisfying solution; however, DTPs can be fully decoupled with these constraints
	 */
	public ArrayList<DisjunctiveTemporalConstraint> computeDecouplingConstraints(ArrayList<Timepoint> timepointsToDecouple);
	
	/**
	 * Adds a new timepoint to the DTPs interface timepoints.
	 * If this timepoint was not even a part of the DTP, it will be added to the local timepoints first.
	 */
	public void addInterfaceTimepoint(Timepoint tp);
	
	/**
	 * Adds new timepoints to the DTPs interface timepoints.
	 * If a timepoint was not even a part of the DTP, it will be added to the local timepoints first.
	 */
	public void addInterfaceTimepoints(Collection<Timepoint> timepoints);
	
	/**
	 * Prints the interval that each timepoint may have
	 * @param out
	 * @param time
	 */
	public void printTimepointIntervals(PrintStream out);
	
	/**
	 * Prints the interval that each timepoint may have if that timepoint intersects time.
	 * calling with time==-1 is equivalent to calling printTimepointIntervals(out)
	 * @param out
	 * @param time
	 */
	public void printTimepointIntervals(PrintStream out, int time);
	
	/**
	 * Print the DTP's constraints
	 * @param out
	 */
	public void printConstraints(PrintStream out);
	
	/**
	 * Prints the selection stack to System.out
	 */
	public void printSelectionStack();

	/**
	 * Pop a selection from the selection stack.
	 * Equivalent to popSelection(time,true)
	 * @param time is the current system time
	 * @return The increment to system time as a result of the popSelection
	 */
	public int popSelection(int time);

	/**
	 * Pop a selection from the selection stack and optionally re-solve the DTP
	 * @param time is the current system time
	 * @param resolve is whether to re-solve the DTP or not
	 * @return The increment to system time as a result of the popSelection
	 */
	public int popSelection(int time, boolean resolve);
	
	/**
	 * Get the activities contained in the DTP based on some selection criteria
	 * @param af Multiplexes between selection criteria
	 * @param time System time
	 * @return A list of the activities that meet the selection criteria
	 */
	public List<List<String>> getActivities(ActivityFinder af, int time);
	
	/**
	 * Provides signals for deciding which set of activities to return in getActivities
	 */
	public static enum ActivityFinder implements java.io.Serializable{
		/**
		 * Signals to get all activities
		 */
		ALL, 
		/**
		 * Signals to get all activities with variable duration
		 */
		VARDUR, 
		/**
		 * Signals to get all activities with variable availability
		 */
		VARAVAIL,
		/**
		 * Signals to get all activities that intersect a time
		 */
		TIME;
	}
	
	/**
	 * Provides signals for deciding which type of deltas to return in getDeltas
	 */
	public static enum DeltaFinder implements java.io.Serializable{
		/**
		 * Signals to get all deltas
		 */
		ALL, 
		/**
		 * Signals to get all deltas that involve a change in the number of disjunts
		 */
		DC, 
		/**
		 * Signals to get all deltas that involve a change in the length of the interval
		 */
		LEN
	}
	
	/**
	 * Prints the timepoint intervals in which either the start or end time has been fixed due to user selection of disjuncts in earlier iterations. 
	 * @param out where to print to
	 * @param time system time. 
	 */
	public void printFixedIntervals(PrintStream out, int time);
	
	/** 
	 * @return true if there are timepoints whose values have been fixed in the problem.
	 */
	public boolean existsFixedTimepoint();
	
	/**
	 * Gets all of the deltas between the current DTP and the previous DTP. 
	 * @param prevDTP the DTP before the current action was executed
	 * @return Returns an ArrayList of these Deltas between prevDTP and the current problem
	 */
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP);
	
	/**
	 * Gets all of the deltas between the current DTP and the previous DTP whose absolute change is greater than the threshold 
	 * @param prevDTP: the DTP before the current action was executed
	 * @param threshold: an integer value specifying the threshold number of minutes 
	 * @return an ArrayList of Deltas that are greater in magnitude than the specified threshold.
	 */
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, int threshold);
	
	/**
	 * Gets all of the deltas between the current DTP and the previous DTP according to the class specified by DeltaFinder
	 * @param prevDTP the DTP before the current action was executed
	 * @param df DeltaFinder flag to indicate the class of deltas we want to return 
	 * @return an ArrayList of Deltas between the prevDTP and current problem according to the DeltaFinder specified.
	 */
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, DeltaFinder df);

	/**
	 * Gets all of the deltas between the current DTP and the previous DTP whose relative change is greater than the specified relative threshold 
	 * @param prevDTP the DTP before the current action was executed
	 * @param relThreshold the value above which the relative change in interval length must be 
	 * @return ArrayList of Deltas whose relative change between prevDTP and DTP is greater than the threshold
	 */
	public ArrayList<Delta> getDeltas(DisjunctiveTemporalProblem prevDTP, double relThreshold);
	
	/**
	 * Gets all of the deltas between the current DTP and the previous DTP and returns the top rankLim ranked by absolute change
	 * @param prevDTP the DTP before the current action was executed
	 * @param rankLim the number of elements to pull off the front of the ranked list
	 * @return an ArrayList of Deltas containing the top rankLim elements when sorted in terms of absolute change.
	 */
	public ArrayList<Delta> rankDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim);
	
	/**
	 * Gets all of the deltas between the current DTP and the previous DTP and returns the top rankLim ranked by relative change
	 * @param prevDTP the DTP before the current action was executed
	 * @param rankLim the number of elements to pull off the front of the ranked list
	 * @return an ArrayList of Deltas containing the top rankLim elements when sorted in terms of relative change
	 */
	public ArrayList<Delta> rankRelativeDeltas(DisjunctiveTemporalProblem prevDTP, int rankLim);

	/**
	 * Prints the deltas specified in the argument.
	 * @param deltas the list of deltas that should be printed.
	 */
	public void printDeltas(ArrayList<Delta> deltas);
	
	/**
	 * Gets the latest end time of a specified timepoint.
	 * @param tp the end timepoint of the activity for which the latest end time is desired.
	 * @return the latest end time for specified timepoint in minutes from midnight.
	 */
	public int getLatestEndTime(Timepoint tp);
	
	/**
	 * Gets earliest end time of a specified timepoint.
	 * @param tp the end timepoint of the activity for which the earliest end time is desired.
	 * @return the earliest end time forr the specified timepoint in minutes from midnight.
	 */
	public int getEarliestEndTime(Timepoint tp);

	/**
	 * Makes a cloned copy of a DTP structure
	 * @return a fully cloned (independent) copy of the problem. 
	 */
	public DisjunctiveTemporalProblem clone();
	
	/**
	 * 
	 * @return a stack containing the collection of DTCs that have been added to the problem since execution began. 
	 * In other words, the set of constraints added after the initial minimization and solving of the problem.
	 */
	public Stack<DisjunctiveTemporalConstraint> getAdditionalConstraints();

	/**
	 * Adds a given timepoint to the problem. 
	 * @param tp A timepoint to add. 
	 * NOTE: Does not do any error checking to see that the timepoint does not already exist in the problem!
	 */
	public void addTimepoint(Timepoint tp);
	/**
	 * Adds a new contingent timepoint to the problem (not controllable by the user)
	 * @param source Timepoint to add
	 * Note: as above, this does not do any error checking. Also, this is not currently in use, but left because it is functional.
	 */
	public void addContingentTimepoint(Timepoint source);
	
	/**
	 * Returns the validity of the given problem. This is used by the DUSTP/DUTP classes to determine whether or not component 
	 * problems are still valid (aka solvable) at the current time during execution.
	 * @return 0 or 1 where 1 means valid and 0 means invalid. 
	 */
	public int getValidity();
	
	/*** 
	 * changes the value of the VALID flag to val. val must be 0 or 1. 
	 * @param val must be zero or one.
	 */
	public void updateValidity(int val);

	/***
	 * returns a list of contingent activities (From contingent timepoints) at the given time
	 * @param time
	 * @return
	 */
	public List<String> getContingentActivities(int time);
	
	/***
	 * takes in the one DTP not to change (in the exhaustiveDTP case, and the value i to which we 
	 * change the rest of the validity flags of the other component DTPs
	 * @param currDTP the one remaining valid component
	 * @param i, the value to change the rest of the validity flags to (0 in almost every case).
	 */
	public void updateValidity(DisjunctiveTemporalProblem currDTP, int i);

	/**
	 * Reloads the minimal network of the DTP to include any new timepoints and resets the temporal network to reminimize
	 */
	public void updateInternalData();
	
	/**
	 * @return a list of timepoints whose values have been fixed during execution. 
	 **/
	public ArrayList<Timepoint> getFixedTimepoints();
	
	/**
	 * returns the maximum amount of time the user could idle and still end up with the maximum number of activities available to 
	 * perform at the current decision point, scheduleTime.
	 * @param scheduleTime Time at which to compute the min slack
	 * @return a list of min slacks (for the different MultiDTP components if needed, if for a simpleDTP, then the length of the list is 1).
	 */
	public List<Integer> getMinSlack(int scheduleTime);
	
	/**
	 * 
	 * @return the number of times Yices is called in the solving of a DTP. 
	 */
	public int getCallsToSolver();
	
	/**
	 * 
	 * @return the list of timepoint names in the problem whose values have not been fixed and remain intervals.
	 */
	public List<String> getUnFixedTimepoints();
	
	/**
	 * 
	 * @param duration
	 * @return
	 */
	public int checkBookends(int duration);

	double getOriginalUpperBound(Timepoint tp);

	double getOriginalLowerBound(Timepoint tp);

	ArrayList<DisjunctiveTemporalConstraint> getSpecificTempConstraints(Timepoint tp);

	ArrayList<DisjunctiveTemporalConstraint> getTempConstraints();

	public Vector<MappedPack> getRelatedInputPairs(String crucial);

	public void addNewTimepoint(Timepoint tp);
	
	// Returns the EST an LET of each subDTP in a MultiDTP (or just of the simpleDTP for the other types of DTPs)
	public ArrayList<Interval> getDTPBoundaries(); 
	
	/**
	 * Adds a constraint to the problem that says that the two activities are nonconcurrent. In other words,
	 * either source ends before dest starts or dest ends before source starts. 
	 * @param source Activity name of one activity
	 * @param dest Activity name of other activity
	 * @param agent Agent for which this constraint needs to be added
	 */
	public void addNonconcurrentConstraint(String source, String dest, int agent);
	
	/**
	 * 
	 * @param agent the agent for which the list of activity names is desired.
	 * @return a list of activity names for the given agent.
	 */
	public ArrayList<String> getActivityNames(int agent);
	
	/**
	 * Adds a set of fixed timepoints to the problem. This is used when updating a problem mid execution, like in the insertion 
	 * of the sporadic activity that is possible in the InteractionStage framework.
	 * @param tps the collection of timepoints that should be considered fixed.
	 */
	public void addFixedTimepoints(Collection<Timepoint> tps);
	/**
	 * Changes the name of the given timepoint to new_name. Used in the breaking apart of activies for insertion of 
	 * sporadic activity in InteractionStage framework.
	 * @param tp the timepoint to be renamed.
	 * @param new_name new name for the timepoint.
	 */
	public void updateTimepointName(Timepoint tp, String new_name);
	
	/**
	 * Adds a new duration constraint between the start and end timepoints below with the given duration.
	 * @param start start timepoint for the duration constraint to update.
	 * @param end end timepoint for the duration constraint to update.
	 * @param duration integer value for the new duration constraint.
	 */
	public void addDurationConstraint(Timepoint start, Timepoint end, int duration);
	
	/**
	 * Adds a new ordering constraint that says that source activity comes before dest activity by [min_duration, max_duration] minutes
	 * @param source the name of the activity that comes first
	 * @param dest the name of the activity that comes second
	 * @param min_duration minimum duration by which dest must follow source
	 * @param max_duration maximum duration by which dest can follow source. 
	 * If source must just come before dest without any specific timing constraints, then 
	 * min_duration should be set to 0, and 
	 * max_duration should be set to Integer.MAX_VALUE.
	 */
	public void addOrderingConstraint(String source, String dest, int min_duration, int max_duration);
	
	/**
	 * Removes the duration constraint associated with the named activity. 
	 * @param tpName the name of the activity for which to remove the existing duration constraint.
	 * A call to this method is followed by a call to the addDurationConstraint method above. 
	 * Used in the insertion of sporadic activity into existing activity in InteractionStage framework.
	 */
	public void removeDurationConstraint(String tpName);
	
	/**
	 * Gets the component STPs of the given DTP. The integer in the simple entry will reference the agent id if we're 
	 * working with a multiagent problem or the component if we're working with a multiDTP. 
	 * @return a simple entry that contains the lists of component STPs for each part of the given problem
	 */
	public SimpleEntry<Integer, ArrayList<DisjunctiveTemporalProblem>> getComponentSTPs();

	// in component DTrees in the case of the DUTP. 
	public void advanceDownTree();
	
	
	public SimpleEntry<Integer, Integer> getMinSlackInterval(int scheduleTime);		
	
	/**
	 * Returns true if the temporal problem has a timepoint with the name in the string argument
	 */
//	public boolean timepointExists(String tpName);

	
}
