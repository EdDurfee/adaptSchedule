package util;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

public class XMLParser {
	
	public static String INDENTATION = "   ";
	
	
	/**
	 * Returns the first instance
	 * @param xmlString
	 * @param tag
	 * @return
	 */
	public static String getTrimmedByTag(String xmlString, String tag){
		int subStringStart = xmlString.indexOf("<"+tag+">")+tag.length()+2;
		int subStringEnd = xmlString.indexOf("</"+tag+">");
		if(subStringEnd<=subStringStart){
			throw new HSPFileFormatException("Invalid tag: <"+tag+">\n in: "+xmlString);
		}
		return xmlString.substring(subStringStart, subStringEnd).trim();
	}
	
	/**
	 * Removes the first instance of the tag and returns rest of xml string
	 */
	public static String removeFirstTag(String xmlString, String tag){
		int subStringStart = xmlString.indexOf("<"+tag+">");
		int subStringEnd = xmlString.indexOf("</"+tag+">")+tag.length()+3;
		if(subStringEnd<=subStringStart){
			throw new HSPFileFormatException("Invalid tag: <"+tag+">\n in: "+xmlString);
		}
		return xmlString.replaceFirst(xmlString.substring(subStringStart, subStringEnd), "").trim();	
	}
	
	public static String removeAllTags(String xmlString, String tag){
		String result = xmlString;
		while(containsTag(result,tag)){
			result = removeFirstTag(result,tag);
		}
		return result;
	}
	
	public static boolean containsTag(String xmlString, String tag){
		int subStringStart = xmlString.indexOf("<"+tag+">");
		int subStringEnd = xmlString.indexOf("</"+tag+">");
		if(subStringStart>=0 && subStringEnd>subStringStart){
			return true;
		}
		return false;
	}
	
	
	/**
	 * DREW
	 * Removes the first instance of the tag and returns rest of xml string
	 * Returns NULL if error in removing
	 */
	public static String removeSpecificActivity(String xmlString, String name){
		String originalXMLString = xmlString;
		int subStringStart;
		int subStringEnd;
		
		
		String tag = "activity";
		while (true) {
			subStringStart = xmlString.indexOf("<"+tag+">");
			subStringEnd   = xmlString.indexOf("</"+tag+">")+tag.length()+3;
			
			// if no more of this tag in the file, return NULL
			if (subStringStart == -1) {
				return null;
			}
			
			if(subStringEnd<=subStringStart) {
				throw new HSPFileFormatException("Invalid tag: <"+tag+">\n in: "+xmlString);
			}
			
			String activityString = xmlString.substring(subStringStart, subStringEnd);
			int nameIdx = activityString.indexOf("<name> "+name);

			// if this is not the matching activity, search through the rest of the xml string
			if (nameIdx == -1) {
				xmlString = xmlString.substring(subStringEnd);
				continue;
			} // else, reset xmlString and remove the substring from the original string
			xmlString = originalXMLString.replaceFirst(xmlString.substring(subStringStart, subStringEnd), "").trim();
			break;
		}
		
		// need to remove all constraints from the xml string that involve the activity
		tag = "constraint";
//		int lastCheckedPoint = 0;
		String finalXMLString = xmlString;
		while (true) {
			subStringStart = xmlString.indexOf("<"+tag+">");// + lastCheckedPoint;
			subStringEnd   = xmlString.indexOf("</"+tag+">")+tag.length()+3;// + lastCheckedPoint+1;
			
			// if no more constraints in the file, exit loop
			if (subStringStart == -1){// + lastCheckedPoint) {
				break;
			}
			
			String constraintString = xmlString.substring(subStringStart, subStringEnd);
//			BUG IS HERE BECAUE THIS ISNT THE RIGHT FORMAT IT ISNT <NAME> INSTEAD <SOURCE>
			int sourceIdx = constraintString.indexOf("<source> "+name);
			int destIdx = constraintString.indexOf("<destination> "+name);
			
			// if the name is not in this constraint, ignore it and search through the rest of the xml string
			if (sourceIdx == -1 && destIdx == -1) {
				; // do nothing
//				lastCheckedPoint = subStringEnd-1;
			} else { // else, remove the substring from the original string
				finalXMLString = finalXMLString.replaceFirst(xmlString.substring(subStringStart, subStringEnd), "").trim();
//				lastCheckedPoint = subStringStart-1;
			}
			xmlString = xmlString.substring(subStringEnd);
		}
		
		return finalXMLString;
	}
	
	
	/**
	 * DREW
	 * Adds an activity and returns new xml string
	 * Returns NULL if error in adding
	 */
	public static String addActivity(String xmlString, int agentNum, String dtpIdx, String name, String est,
									 String lst, String eet, String let, String minDur, String maxDur,
									 ArrayList<String> precConstraints, ArrayList<String> succConstraints){
		
		
		// Holds pairs of (DTPname, startIdx)
		ArrayList<SimpleEntry<String,Integer>> agentStarts = new ArrayList<SimpleEntry<String,Integer>>();
		
		int searchIdx = 1;
		int dtpCount = 0;
		while (true) {
			// starting from last found DTP start, search for next dtp
			searchIdx = xmlString.indexOf("DTP>",searchIdx+1);
			if (searchIdx == -1) {break;}
			
			int dtpTypeStart = xmlString.lastIndexOf("<", searchIdx); // beginning of this DTP < >
			String dtpTypeString = xmlString.substring(dtpTypeStart+1, searchIdx+3);
			
			// if this is the line denoting the end of the dtp, ignore it
			if (dtpTypeString.indexOf("/") != -1) {continue;}
			
			// if this is the line declaring this is a multiagentDTP, ignore it
			if (dtpTypeString.equals("multiagentDTP")) {continue;}
			
			// if this is a numDTP line of a multidtp, ignore it
			if (dtpTypeString.equals("numDTP")) {continue;}
			
			// else add it to the array of DTP starts
			agentStarts.add(new SimpleEntry<String,Integer>(dtpTypeString,searchIdx+4));
			dtpCount++;
		}
		
		int subStringStart;
		int subStringEnd;
		
		
		
		// not including the first line, each instance of "<...DTP>" indicates a new agent (and corresponding DTP)
		// so idx of simpleDTPstarts corresponds to agent number
		int agentStringStart = agentStarts.get(agentNum).getValue();
		int agentStringEnd   = xmlString.indexOf("</"+agentStarts.get(agentNum).getKey());
		
		// this is the point where new details will be inserted before
		int prependPoint = xmlString.indexOf("<activity>", agentStringStart); //substring(agentStringStart,agentStringEnd).
		
		// create full string that will be inserted
		
		// create <activity> part of insertion
		String insertStr = "";
		insertStr += "\t\t<activity>\n";
		insertStr += "\t\t\t<name> " +name+ " </name>\n";
		if (agentStarts.get(agentNum).getKey().equals("multiDTP")) {insertStr += "\t\t\t<dtpIdx> "+ dtpIdx +" </dtpIdx>\n";}
		insertStr += "\t\t\t<duration>\n";
		insertStr += "\t\t\t\t<min> "+ minDur +" </min>\n";
		insertStr += "\t\t\t\t<max> "+ maxDur +" </max>\n";
		insertStr += "\t\t\t</duration>\n";
		insertStr += "\t\t\t<availability>\n";
		if (!est.equals("")) {insertStr += "\t\t\t\t<est> "+ est +" </est>\n";}
		if (!lst.equals("")) {insertStr += "\t\t\t\t<lst> "+ lst +" </lst>\n";}
		if (!eet.equals("")) {insertStr += "\t\t\t\t<eet> "+ eet +" </eet>\n";}
		if (!let.equals("")) {insertStr += "\t\t\t\t<let> "+ let +" </let>\n";}
		insertStr += "\t\t\t</availability>\n";
		insertStr += "\t\t</activity>\n";
		
		// create <constraint> parts of insertion
		for (int i = 0; i < precConstraints.size(); i++) {
			insertStr += "\t\t<constraint>\n"; // <constraint>
			insertStr += "\t\t\t<type> ordering </type>\n"; // <type> ordering </type>
			insertStr += "\t\t\t<source> " +precConstraints.get(i)+ " </source>\n"; // <source> wakeup </source>
			insertStr += "\t\t\t<destination> " +name+ " </destination>\n"; // <destination> showerM </destination>
			insertStr += "\t\t\t<min_duration> 0 </min_duration>\n"; // <min_duration> 0 </min_duration>
			insertStr += "\t\t</constraint>\n"; // </constraint>
		}
		for (int i = 0; i < succConstraints.size(); i++) {
			insertStr += "\t\t<constraint>\n"; // <constraint>
			insertStr += "\t\t\t<type> ordering </type>\n"; // <type> ordering </type>
			insertStr += "\t\t\t<source> " +name+ " </source>\n"; // <source> wakeup </source>
			insertStr += "\t\t\t<destination> " +succConstraints.get(i)+ " </destination>\n"; // <destination> showerM </destination>
			insertStr += "\t\t\t<min_duration> 0 </min_duration>\n"; // <min_duration> 0 </min_duration>
			insertStr += "\t\t</constraint>\n"; // </constraint>
		}
		
		
		// because we are assuming all activities to be noncopncurrent, we need to add specific non-concurrency constraints for every act
		// if this isnt a multidtp problem, the dtp is same as agent str
		String dtpStr = "";
		dtpStr = xmlString.substring(agentStringStart,agentStringEnd);
		
		int anActNameStart, anActNameEnd = -1;
		int anActStart = dtpStr.indexOf("<activity>");
		int anActEnd = dtpStr.indexOf("</activity>");
		String anActStr = "";
		while (anActStart != -1) {
			anActStr = dtpStr.substring(anActStart,anActEnd);
			
			// if this agent is a multiDTP and not matching dtpIdx, do not add noncurrent constraint
			if (agentStarts.get(agentNum).getKey().equals("multiDTP")
				&& anActStr.indexOf("<dtpIdx> " +dtpIdx+ " </dtpIdx>") == -1) {
					; // do nothing
			} else {
			
				insertStr += "\t\t<constraint>\n";
				insertStr += "\t\t\t<type> nonconcurrent </type>\n";
				insertStr += "\t\t\t<source> " +name+ " </source>\n";
				anActNameStart = anActStr.indexOf("<name> ")+7;
				anActNameEnd   = anActStr.indexOf(" </name>");
				insertStr += "\t\t\t<destination> " + anActStr.substring(anActNameStart,anActNameEnd) + " </destination>\n";
				insertStr += "\t\t\t<min_duration> 0 </min_duration>\n";
				insertStr += "\t\t</constraint>\n";
			}
			
			// shift forward by cutting out this activity definition
			dtpStr = dtpStr.substring(anActEnd+11);
			anActStart = dtpStr.indexOf("<activity>");
			anActEnd = dtpStr.indexOf("</activity>");
			
		}
		
		
		// TODO add custom constraints to insertString
		
		// insert insertStr into the xml string and return it
		return new StringBuffer(xmlString).insert(prependPoint, insertStr).toString();
		
	}
		
		
	
	public static void main(String[] args){
		String xml = "<tag>key word</tag>";
		String tag = "tag";
		System.out.println(getTrimmedByTag(xml, tag));
		xml = "<tag>\n key word\t </tag>";
		tag = "tag";
		System.out.println(getTrimmedByTag(xml, tag));
	}
}
