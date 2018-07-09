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

import interactionFramework.InteractionStageGUI;
//import jdk.net.SocketFlow.Status;

import org.nanohttpd.protocols.http.response.Status;

/*
 * This class will set up a server at http://localhost:8080/ when run that asks for your name and returns a greeting
 */



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
public class AppServerGUI extends NanoHTTPD { // implements Runnable
	
	boolean DEBUG = false;
	
	public String msg;
	public String cmdVal = "";
	public int test = 0;
	public InteractionStageGUI interactive;
	public Thread interactiveThread;
	public String clientID;
	public String lastGetReplyInfoType = "";
	
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
//		interactiveThread = new Thread( interactive );
//		interactiveThread.start();
    }

    
    /*
     * This is how the server reacts to a request. In order to implement custom servers, this method must be overridden
     * (non-Javadoc)
     * @see org.nanohttpd.protocols.http.NanoHTTPD#serve(org.nanohttpd.protocols.http.IHTTPSession)
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
	    
        ArrayList debugArr = new ArrayList<String>();
        debugArr.add("uri: " + String.valueOf(session.getUri()));
        debugArr.add("method: " + String.valueOf(session.getMethod()));
        debugArr.add("headers: " + toString(session.getHeaders()));
        debugArr.add("params: " + toString(session.getParameters()));
        debugArr.add("decodedQueryParams: " + toString(decodedQueryParameters));
        

        switch(String.valueOf(session.getMethod())) {
        case "POST":
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
		        	if (DEBUG) System.out.println("command value = " + cmdVal);
		        	
		        	if (cmdVal.equals("startup")) {
		        		System.out.println("New server instanced.");
//		        		interactiveThread.stop();
//		        		interactiveThread.interrupt();
	        			interactive = new InteractionStageGUI(0);
	            		interactiveThread = new Thread( interactive );
	            		interactiveThread.start();
	            		connectionInitiated = true;
	            		clientID = null;
//	            		InteractiveServNum++;
		        	}else {
		        		interactive.fromClientQueue.put(JSONbody);
		        	}
	            }
        	} catch (Exception e){
				System.err.println("Error: "+e.toString());
				System.err.flush();
			}
        	break;
        case "GET": // if a GET request
        	if (connectionInitiated == false) break; // ignore gets until a confirmed connection with a valid client
        	/*
        	 * For now, we will use this as a type of ping / polling method
        	 * where the client sends a get every 0.2 seconds and the server
        	 * checks if there is any queued output and sends it back, if it exists
        	 */
        	if (clientID == null) { clientID = (String)session.getUri().substring(1); }
//        	if ( session.getUri().substring(1).equals(clientID) ) {
        	
        	
        	
        	if (lastGetReplyInfoType.equals("ganttImage")) {
        		try {
        			Response toReturn = Response.newChunkedResponse( Status.OK, "png", (InputStream) new FileInputStream("forClient_image.png"));
        			lastGetReplyInfoType = "";
        			File file = new File("forClient_image.png");
	                file.delete();
	                return toReturn;
	                
        		} catch (Exception e) {
        			System.err.println("Error: "+e.toString());
    				System.err.flush();
        		}
        		
        	} else if (interactive.toClientQueue.size() > 0) {
	        	JSONObject responseJSON = interactive.toClientQueue.poll();
	        	msg = responseJSON.toString();
	        	lastGetReplyInfoType = (String) responseJSON.get("infoType");
	        	return Response.newFixedLengthResponse(msg);
        	}
        	
		        
//        	}
        	
        } // end of switch
        
        // have to reply with something. So just reply with expect object with all default vals
        JSONObject outJSON = new JSONObject();
		outJSON.put("infoType", "");
		outJSON.put("startTime", "");
		outJSON.put("lastActivity", "");
		outJSON.put("nextActivities", new ArrayList<String>());
		outJSON.put("nextActsMinDur", new ArrayList<String>());
		outJSON.put("nextActsMaxDur", new ArrayList<String>());
		outJSON.put("remActivities", new ArrayList<String>());
		outJSON.put("remMinDurs", new ArrayList<String>());
		outJSON.put("remMaxDurs", new ArrayList<String>());
		outJSON.put("remMinStarts", new ArrayList<String>());
		outJSON.put("remMaxEnds", new ArrayList<String>());
		outJSON.put("debugInfo", new ArrayList<String>());
		
		msg = outJSON.toString();
        return Response.newFixedLengthResponse(msg); // if you have not returned before this line, the response does not require meaningful material
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
