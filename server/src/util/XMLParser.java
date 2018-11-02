package util;

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
	
	
	public static void main(String[] args){
		String xml = "<tag>key word</tag>";
		String tag = "tag";
		System.out.println(getTrimmedByTag(xml, tag));
		xml = "<tag>\n key word\t </tag>";
		tag = "tag";
		System.out.println(getTrimmedByTag(xml, tag));
	}
}
