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
	
	
	public static void main(String[] args){
		String xml = "<tag>key word</tag>";
		String tag = "tag";
		System.out.println(getTrimmedByTag(xml, tag));
		xml = "<tag>\n key word\t </tag>";
		tag = "tag";
		System.out.println(getTrimmedByTag(xml, tag));
	}
}
