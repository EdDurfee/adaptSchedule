package util;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import interactionFramework.InteractionStageGUI;

import org.nanohttpd.protocols.http.response.Status;

import java.util.Map;
import java.util.logging.Logger;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.util.ServerRunner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;



/**
 * Subclassing NanoHTTPD to make a custom HTTP server.
 */

/*
 * This class will set up a server at http://localhost:8080/ when run that asks for your name and returns a greeting
 */
public class AppServerGUI extends NanoHTTPD { // implements Runnable
	
	boolean DEBUG = false;
	
	public String msg;
	public String cmdVal = "";
	public String agentNum = "";
	public int test = 0;
	public InteractionStageGUI interactive = new InteractionStageGUI();
	public Thread interactiveThread;
	public String clientID_agent0;
	public String clientID_agent1;
	public String lastGetReplyInfoType = "";
	public String lastGetReplyClientID = "";
	
	public boolean connectionInitiated = false;

    public static void main(String[] args) {
    	// DREW: switched from using main in InteractionStageGUI to this main
    	// to avoid forced staticness of the ServerRUnner
    	// TODO: Will this change enable me to get rid of the saving to/from json files?
        ServerRunner.run(AppServerGUI.class);
    }
    
    public AppServerGUI() {
        super(8080);
//		interactive = new InteractionStageGUI(0);
		interactiveThread = new Thread( interactive );
		interactiveThread.start();
    }

    
    /*
     * This is how the server reacts to a request from a client.
     */
    @Override
    public Response serve(IHTTPSession session) {
    	
    	Map<String, List<String>> decodedQueryParameters = decodeParameters(session.getQueryParameterString());
    	if (DEBUG) {
	        System.out.println("uri: " + String.valueOf(session.getUri()));
	        System.out.println("method: " + String.valueOf(session.getMethod()));
	        System.out.println("headers: " + toString(session.getHeaders()));
	        System.out.println("params: " + toString(session.getParameters()));
	        System.out.println("decodedQueryParams: " + toString(decodedQueryParameters));
    	}
	    
    	// react depending on which type of request this is
        switch(String.valueOf(session.getMethod())) {
        case "POST": // if a POST request
        	try {
	        	session.getInputStream();
	            Map<String, String> files = new HashMap<String, String>();
	            session.parseBody(files); // internally process the post body
	            String postBody = session.getQueryParameterString(); // get the POST body
	            if (DEBUG) System.out.print("body: ");
	            if (DEBUG) System.out.println(postBody);
	            JSONParser Jparser = new JSONParser();
	            JSONObject JSONbody;
	            if (postBody == null) {
	            	System.out.println("body of message is empty. Proceeding.");
	            	break;
	            } else {
	            	JSONbody = (JSONObject) Jparser.parse( postBody );
	            	cmdVal = (String) JSONbody.get("infoType");
	            	agentNum = (String) JSONbody.get("agentNum");
		        	if (DEBUG) System.out.println("command value = " + cmdVal);
		        	
		        	if (cmdVal.equals("startup")) {
	            		connectionInitiated = true;
	                	if (clientID_agent0 == null && agentNum.equals("0")) { clientID_agent0 = (String)session.getUri().substring(1); }
	                	if (clientID_agent1 == null && agentNum.equals("1")) { clientID_agent1 = (String)session.getUri().substring(1); }
		        	}
		        	interactive.fromClientQueue.put(JSONbody);
	            }
        	} catch (Exception e){
				System.err.println("Error: "+e.toString());
				System.err.flush();
			}
        	break;
        
        case "GET": // if a GET request
        	if (connectionInitiated == false) break; // ignore gets until a confirmed connection with a valid client
        	
        	String getClientID = session.getUri().substring(1);
        	
        	// check which agent this get is from and get the JSON to process accordingly
        	JSONObject responseJSON;
        	if (getClientID.equals(clientID_agent0) && interactive.toClientAgent0Queue.size() > 0) {
        		responseJSON = interactive.toClientAgent0Queue.poll();
        	} else if (getClientID.equals(clientID_agent1) && interactive.toClientAgent1Queue.size() > 0) {
        		responseJSON = interactive.toClientAgent1Queue.poll();
        	} else {
        		break; // break from switch if there are no messages for any clients
        	}
	        	
        	msg = responseJSON.toString();
        	return Response.newFixedLengthResponse(msg);
	        
        	
        } // end of switch
        
        // if a reply has not been sent yet, no meaninful information needs to be sent
        // but have to reply with correct format, so reply with all default vals
        JSONObject outJSON = new JSONObject();
		outJSON.put("infoType",        "");
		outJSON.put("startTime",       "");
		outJSON.put("lastActivity",    "");
		outJSON.put("nextActivities",  new ArrayList<String>());
		outJSON.put("nextActsMinDur",  new ArrayList<String>());
		outJSON.put("nextActsMaxDur",  new ArrayList<String>());
		outJSON.put("remActivities",   new ArrayList<String>());
		outJSON.put("remMinDurs",      new ArrayList<String>());
		outJSON.put("remMaxDurs",      new ArrayList<String>());
		outJSON.put("remMinStarts",    new ArrayList<String>());
		outJSON.put("remMaxEnds",      new ArrayList<String>());
		outJSON.put("activityDetails", new JSONObject());
		outJSON.put("debugInfo",       new ArrayList<String>());
		
		msg = outJSON.toString();
        return Response.newFixedLengthResponse(msg);
    }
    
    
    
    
    private String toString(Map<String, ? extends Object> map) {
        if (map.size() == 0) {
            return "";
        }
        return unsortedList(map);
    }
    
    private String unsortedList(Map<String, ? extends Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
            listItem(sb, entry);
        }
        sb.append("</ul>");
        return sb.toString();
    }
    
    private void listItem(StringBuilder sb, Map.Entry<String, ? extends Object> entry) {
        sb.append("<li><code><b>").append(entry.getKey()).append("</b> = ").append(entry.getValue()).append("</code></li>");
    }
    
    
    
}
