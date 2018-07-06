package interactionFramework;

import interval.Interval;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.AbstractMap.SimpleEntry;

import dtp.DisjunctiveTemporalProblem;

public class XMLMaker {

	static int tabDepth;  //number of tabs to print before printing a line.
	static int dtpIdx = 1; //keeps track of which agent we are working on
	static Scanner input = new Scanner(System.in); 
	static PrintWriter output; //writes to the XML file
	static String activity;
	static Collection<String> all_acts = new ArrayList<String>(); // the list of activities already in the dtp.
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("What would you like the filename of this XML file to be?");
		String filename = input.nextLine();
		if(!filename.endsWith(".xml"))
		{
			filename += ".xml";
		}
		//prepare to write to an XML file
		output = new PrintWriter(filename);
		tabDepth = 0;
		makeDTPType();
		output.close(); //flushes output's stream before shutting down
	}

	/* automates the production of tabDepth tabs before printing a line of XML to the file*/
	static void tab()
	{
		for(int i = 0; i < tabDepth; i++)
		{
			output.print("\t");
		}
	}
	
	static String tabString(){
		String out = "";
		for(int i = 0; i < tabDepth; i++)
		{
			out = out + "\t";
		}
		return out;
	}

	/*allows text-based menu selection between the choices contained in options. This function is not case sensitive.*/
	static String getNextUserInput(String[] options, int numOptions)
	{
		boolean ansFound = false;
		String answer;
		do
		{
			answer = input.nextLine();
			for(int i = 0; i < numOptions; i++)
			{
				if(answer.equalsIgnoreCase(options[i]))
					ansFound = true;
			}
			if(!ansFound)
				System.out.println("Please use the options listed above to answer this prompt.");
		} while(!ansFound);
		return answer;
	}

	/* allows the user to select whether the problem requires multiple agents */
	static void makeDTPType()
	{
		System.out.println("Is this problem a multiagentDTP?");
		System.out.println("[Y]es or [N]o");
		String[] options = {"Y","N"};
		String answer = getNextUserInput(options, 2);
		if(answer.equals("Y"))
		{
			output.println("<multiagentDTP>");
			tabDepth++;

			System.out.println("How many agents?");
			int numAgents = input.nextInt();
			@SuppressWarnings("unused")
			String junkinput = input.nextLine();
			tab();
			output.println("<numAgent> " + numAgents + " </numAgent>");

			makeDTP();

			output.println("</multiagentDTP>");
		}
		else
		{
			makeDTP();
		}
	}

	/* creates simpleDTPs and multiDTPs */
	static void makeDTP()
	{
		String answer;
		do{
			System.out.println("Do you need a [S]impleDTP, a [M]ultiDTP, or are you [F]inished making DTPs?");
			String[] options = {"S","M","F"};
			answer = getNextUserInput(options, 3);
			if(answer.equalsIgnoreCase("s"))
			{
				dtpIdx++;
				tab();
				output.println("<simpleDTP>");
				tabDepth++;
				activityOrConstraint();
				tabDepth--;
				tab();
				output.println("</simpleDTP>");
			}
			else if(answer.equalsIgnoreCase("M"))
			{
				dtpIdx++;
				tab();
				output.println("<multiDTP>");
				tabDepth++;
				activityOrConstraint();
				tabDepth--;
				tab();
				output.println("</multiDTP>");
			}  
		} while(!answer.equalsIgnoreCase("F"));
	}

	static void activityOrConstraint()
	{
		String answer;
		do
		{
			System.out.println("Would you like to add an [A]ctivity, a [C]onstraint, or [F]inish the current DTP?");
			String[] options = {"A","C","F"};
			answer = getNextUserInput(options, 3);
			if(answer.equalsIgnoreCase("A"))
				makeActivity();
			else if(answer.equalsIgnoreCase("C"))
				makeConstraint();
		} while(!answer.equalsIgnoreCase("F"));

	}

	static void makeActivity()
	{
		System.out.println("What is the name of this activity?");
		String name = input.nextLine();
		tab();
		all_acts.add(name); // add the new activity to the list of activities. 
		output.println("<activity>");
		tabDepth++;
		tab();
		output.println("<name> " + name + " </name>");
		tab();
		output.println("<dtpIdx> " + dtpIdx + " </dtpIdx>");

		String userInput;
		do
		{
			makeDuration();
			System.out.println("Is there another disjunctive possibility for the duration of this activity? (Y/N)");
			String[] options = {"Y","N"};
			userInput = getNextUserInput(options, 2);
		} while(userInput.equalsIgnoreCase("Y"));
		System.out.println("Would you like to set availability for " + name + "? (Y/N)");
		String[] options = {"Y","N"};
		userInput = getNextUserInput(options, 2);
		if(userInput.equalsIgnoreCase("Y"))
			makeAvailability();
		tabDepth--;
		tab();
		output.println("</activity>");

	}
	
	//The input to this function is the boundaries of the DTP/MultiDTP that we're adding an activity to. 
	static SimpleEntry<String, Integer> makeActivityString(ArrayList<Interval> seps)	{
		String out = "";
		String dur_string = "";
		int idx = 0;
		
		System.out.println("What is the name of this activity?");
		String name = input.nextLine();
		activity = name; // save the name of the activity we're currently creating a thing for. 
		all_acts.add(name); // add the new activity to the list of activities. 
		

		String userInput;
		do
		{
			dur_string = dur_string + makeDurationString();
			System.out.println("Is there another disjunctive possibility for the duration of this activity? (Y/N)");
			String[] options = {"Y","N"};
			userInput = getNextUserInput(options, 2);
		} while(userInput.equalsIgnoreCase("Y"));
		//System.out.println("Would you like to set availability for " + name + "? (Y/N)");
		//String[] options = {"Y","N"};
		//userInput = getNextUserInput(options, 2);
		//if(userInput.equalsIgnoreCase("Y")){
		System.out.println("Please specify availability for activity: " + name);
			SimpleEntry<Interval, String> avail = makeAvailabilityString();
			//out = out + avail.getValue();
		//}
		idx = computeDTPIdx(avail.getKey(), seps);
		out = out + tabString();
		out = out + "<activity>" + "\n";
		tabDepth++;
		out = out + tabString();
		out = out + "<name> " + name + " </name>" + "\n";
		out = out + tabString();
		out = out + "<dtpIdx> " + idx + " </dtpIdx>" + "\n";	
		tabDepth--;
		out = out + dur_string;
		out = out + avail.getValue();
		out = out + tabString();
		out = out + "</activity>" + "\n";
		return new SimpleEntry<String, Integer>(out, idx);

	}
	

	static void makeDuration()
	{
		System.out.println("What is the minimum duration of this activity in minutes?");
		int minDur = input.nextInt();
		@SuppressWarnings("unused")
		String junk = input.nextLine(); //eats input of \n
		System.out.println("What is the maximum duration of this activity in minutes?");
		int maxDur = input.nextInt();
		junk = input.nextLine(); //eats input of \n
		tab();
		output.println("<duration>");
		tabDepth++;
		tab();
		output.println("<min> " + minDur + " </min>");
		tab();
		output.println("<max> " + maxDur + " </max>");
		tabDepth--;
		tab();
		output.println("</duration>");
	}

	static String makeDurationString()
	{
		String out = "";
		System.out.println("What is the minimum duration of this activity in minutes?");
		int minDur = input.nextInt();
		@SuppressWarnings("unused")
		String junk = input.nextLine(); //eats input of \n
		System.out.println("What is the maximum duration of this activity in minutes?");
		int maxDur = input.nextInt();
		junk = input.nextLine(); //eats input of \n
		out = out + tabString();
		out = out+"<duration>"+"\n";
		tabDepth++;
		out = out + tabString();
		out = out + "<min> " + minDur + " </min>"+"\n";
		out = out + tabString();
		out = out + "<max> " + maxDur + " </max>"+"\n";
		tabDepth--;
		out = out + tabString();
		out = out + "</duration>"+ "\n";
		return out;
	}
	
	static void makeAvailability()
	{
		String[] usersNames = {"EARLIEST START TIME", "LATEST START TIME", "EARLIEST END TIME", "LATEST END TIME"};
		String[] xmlNames = {"est","lst","eet","let"};
		tab();
		output.println("<availability>");
		tabDepth++;
		for(int j = 0; j < 4; j++)
		{
			System.out.println("What is the " + usersNames[j] + " for this activity (measured in minutes past midnight?)");
			System.out.println("If you do not wish to enter a value for " + usersNames[j] + ", simply press enter.");
			String userInput = input.nextLine();
			
			boolean isDigits = true;
			for(int i = 0; i < userInput.length(); i++)
			{
				Character cand = userInput.charAt(i);
				if(!Character.isDigit(cand))
				{
					isDigits = false;
				}				
			}
			if(userInput.length() == 0)
			{
				isDigits = false;
			}
			if(isDigits)
			{
				tab();
				output.println("<" + xmlNames[j] + "> " + userInput + " </" + xmlNames[j] + ">");
			}
		}
		tabDepth--;
		tab();
		output.println("</availability>");

	}
	
	static SimpleEntry<Interval,String> makeAvailabilityString()
	{
		String out = "";
		Interval key = null;
		int lb = 0;
		int ub = 0;
		String[] usersNames = {"EARLIEST START TIME", "LATEST START TIME", "EARLIEST END TIME", "LATEST END TIME"};
		String[] xmlNames = {"est","lst","eet","let"};
		out = out + tabString();
		out = out + "<availability>"+"\n";
		tabDepth++;
		for(int j = 0; j < 4; j++)
		{
			System.out.println("What is the " + usersNames[j] + " for this activity in the form hh:mm");
			System.out.println("If you do not wish to enter a value for " + usersNames[j] + ", simply press enter.");
			String userInput = input.nextLine();
			if(userInput.length() > 0){
				
				int userInputVal = Generics.fromTimeFormat(userInput);
				
				if(j == 0){
					lb = userInputVal;
				}
				else if(j == 3){
					ub = userInputVal;
				}
				
				out = out + tabString();
				out = out + "<" + xmlNames[j] + "> " + userInputVal + " </" + xmlNames[j] + ">"+"\n";
			}
		
		}
		
		if(lb == 0) lb = Integer.MIN_VALUE;
		if(ub == 0) ub = Integer.MAX_VALUE;
		
		key = new Interval((double) lb, (double) ub);
		
		tabDepth--;
		out = out + tabString();
		out = out + "</availability>"+"\n";
		return new SimpleEntry<Interval, String>(key,out);
	}

	static void makeConstraint()
	{
		tab();
		output.println("<constraint>");
		tabDepth++;

		//do stuff
		System.out.println("What type of constraint is it?");
		System.out.println("[O]rdering, [N]onconcurrent, [E]xclusive, or [S]ynchronized"); 	
		String[] options = {"O","N","E","S"}; //TODO
		String response = getNextUserInput(options, 4); //TODO
		tab();
		if(response.equalsIgnoreCase("O"))
			output.println("<type> ordering </type>");
		else if(response.equalsIgnoreCase("N"))
			output.println("<type> nonconcurrent </type>");
		else if(response.equalsIgnoreCase("E"))
			output.println("<type> exclusive </type>");
		else if(response.equalsIgnoreCase("S"))
			output.println("<type> synchronized </type>");

		System.out.println("What activity is the source of this constraint?");
		String source = input.nextLine();
		tab();
		output.println("<source> " + source + " </source>");

		System.out.println("What activity is the destination of this constraint?");
		String dest = input.nextLine();
		tab();
		output.println("<destination> " + dest + " </destination>");

		if(response.equalsIgnoreCase("O") || response.equalsIgnoreCase("N"))
		{
			tab();
			output.println("<min_duration> 0 </min_duration>");
		}
		else if(response.equalsIgnoreCase("S"))
		{
			System.out.println("What is the start value for this Synchronized constraint?");
			int start = input.nextInt();
			@SuppressWarnings("unused")
			String junk = input.nextLine(); //eat input of \n
			tab();
			output.println("<start> " + start + " </start>");

			System.out.println("What is the end value for this Synchronized constraint?");
			int end = input.nextInt();
			junk = input.nextLine(); //eat input of \n
			tab();
			output.println("<end> " + end + " </end>");
		}

		tabDepth--;
		tab();
		output.println("</constraint>");

	}

	static String makeConstraintString(Collection<String> activities)
	{
		String out = "";
		out = out + tabString();
		out = out + "<constraint>";
		tabDepth++;
		// get a list of activities for the user to choose from but actually this comes from interaction stage
		//Collection<String> activities = Generics.concat(dtp.getActivities(DisjunctiveTemporalProblem.ActivityFinder.ALL, -getSystemTime()));
		// WE NEED TO ADD THE NEW ACTIVITY
		all_acts.addAll(activities);
		
		System.out.println("What type of constraint is it?");
		System.out.println("[O]rdering, [N]onconcurrent"); 	
		String[] options = {"O","N"}; //TODO
		String response = getNextUserInput(options, 2); //TODO
		out = out + tabString();
		if(response.equalsIgnoreCase("O"))
			out = out + "<type> ordering </type>";
		else if(response.equalsIgnoreCase("N"))
			out = out + "<type> nonconcurrent </type>";


		System.out.println("What activity is the source of this constraint?");
		System.out.println("Activities are: "+ all_acts.toString());
		String source = input.nextLine();
		
		while(!all_acts.contains(source)){
			System.out.println("Unexpected response \""+source+"\"");
			source = input.nextLine();
		}
		
		out = out + tabString();
		out = out + "<source> " + source + " </source>";

		System.out.println("What activity is the destination of this constraint?");
		System.out.println("Activities are: "+ all_acts.toString());
		String dest = input.nextLine();
		while(!all_acts.contains(dest)){
			System.out.println("Unexpected response \""+dest+"\"");
			dest = input.nextLine();
		}
		out = out + tabString();
		out = out + "<destination> " + dest + " </destination>";

		if(response.equalsIgnoreCase("O") || response.equalsIgnoreCase("N"))
		{
			out = out + tabString();
			out = out + "<min_duration> 0 </min_duration>";
		}
		else if(response.equalsIgnoreCase("S"))
		{
			System.out.println("What is the start value for this Synchronized constraint?");
			int start = input.nextInt();
			@SuppressWarnings("unused")
			String junk = input.nextLine(); //eat input of \n
			tab();
			output.println("<start> " + start + " </start>");

			System.out.println("What is the end value for this Synchronized constraint?");
			int end = input.nextInt();
			junk = input.nextLine(); //eat input of \n
			tab();
			output.println("<end> " + end + " </end>");
		}

		tabDepth--;
		out = out + tabString();
		out = out + "</constraint>";
		System.out.println(out);
		return out;

	}
	static SimpleEntry<Integer,String> makeNewActivityAndConstraintString(Collection<String> activities, ArrayList<Interval> seps){
		String out = "";
		int dtp = -1;
		SimpleEntry<String, Integer> output = makeActivityString(seps);
		out = out + output.getKey();
		dtp = output.getValue();
		String userInput;
		System.out.println("Would you like to add any ORDERING constraints? (Y/N)");
		String[] options = {"Y", "N"};
		userInput = getNextUserInput(options, 2);
		if(userInput.equalsIgnoreCase("N")) return new SimpleEntry<Integer, String>(dtp,out);
		do
		{
			out = out + makeConstraintString(activities);
			System.out.println("Would you like to add another constraint to the problem? (Y/N)");
			userInput = getNextUserInput(options, 2);
		} while(userInput.equalsIgnoreCase("Y"));
		return new SimpleEntry<Integer, String>(dtp, out);		
	}

	static int computeDTPIdx(Interval new_act, ArrayList<Interval> seps){
		// new_act is the interval of the activity we just specified. We also need to have the intervals 
		// of each multiDTP in the DTP we're adding. 
		if(seps.size() <= 1) return 0;
		
		for(int i = 0; i < seps.size(); i++){
			if(new_act.intersects(seps.get(i))) {
				System.out.println("activity interval " + new_act.toString() + " intersects with " + seps.get(i).toString());
				return i;
			}
			
		}
		System.out.println("We didn't find an intersection...");
		return 0;
		
	}

}
