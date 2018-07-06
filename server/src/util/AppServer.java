package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

import interactionFramework.InteractionStageDrew;

/*
 * This class will set up a server at http://localhost:8080/ when run that asks for your name and returns a greeting
 */



import java.util.Map;
import java.util.logging.Logger;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.util.ServerRunner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
/**
 * Subclassing NanoHTTPD to make a custom HTTP server.
 */
public class AppServer extends NanoHTTPD { // implements Runnable
	
	boolean DEBUG = false;
	
	public String msg;
	public String cmdVal = "";
	public int test = 0;
	public InteractionStageDrew interactive;
	public Thread interactiveThread;
	public String clientID;

    public static void main(String[] args) {
    	// DREW: switched from using main in InteractionStageDrew to this main
    	// to avoid forced staticness of the ServerRUnner
    	// TODO: Will this change enable me to get rid of the saving to/from json files?
    	//interactive = new InteractionStageDrew();
		//interactiveThread = new Thread( interactive );
		//interactiveThread.start();
        ServerRunner.run(AppServer.class);
    }
    
    public void startup(String[] args) {
        ServerRunner.run(AppServer.class);
    }
    
    /*public void run(){ // This method is being commented out because if you switch back
     * 					//   to using it, there will be major logic changes needed through
     * 					//	 other parts of the code
     */
    	/*
    	 * Drew: This is a static method (even if not labeled so). It is only passing
    	 * the class structure up to the 'run' method, not the actual instance. So anything
    	 * in the response method is just be executed on some other instance created deeper
    	 
    	ServerRunner.run(AppServer.class);
    	
     }*/

    public AppServer() {
        super(8080);
    }
    
    public String getCmdVal() {
    	return cmdVal;
    }

    public String waitForValidCmd() {
    	try {
	    	while(cmdVal.length() == 0) {
				Thread.sleep(500);
			}
    	} catch (Exception e) {
    		System.out.println("wait error:");
        	e.printStackTrace();
    		
    	}
    	return cmdVal;
    }
    
    /*
     * This is how the server reacts to a request. In order to implement custom servers, this method must be overridden
     * (non-Javadoc)
     * @see org.nanohttpd.protocols.http.NanoHTTPD#serve(org.nanohttpd.protocols.http.IHTTPSession)
     */
    @Override
    public Response serve(IHTTPSession session) {
    	
//    	class fromServer { // this class should match the fromServer struct in the client
//    		
//    		String toPrint;
//    		
//    		public fromServer() {
//    			String toPrint = "";
//    		}
//    		
//    	}
    	
    	Map<String,String> sendToClient = new HashMap<>();
    	sendToClient.put("toPrint","");
    	
    	if (DEBUG) System.out.println("----------------------- Server Output Begin -----------------------");
        
        //json.put("greeting", "Response");
        //json.put("who", "complete");

    	Map<String, List<String>> decodedQueryParameters = decodeParameters(session.getQueryParameterString());
        
    	if (DEBUG) {
	        System.out.println("uri: " + String.valueOf(session.getUri()));
	        System.out.println("method: " + String.valueOf(session.getMethod()));
	        System.out.println("headers: " + toString(session.getHeaders()));
	        System.out.println("params: " + toString(session.getParameters()));
	        System.out.println("decodedQueryParams: " + toString(decodedQueryParameters));
    	}
	    
        JSONArray debugArray = new JSONArray();
        JSONObject tempObj = new JSONObject();
        tempObj.put("infoType", "uri"); tempObj.put("value", String.valueOf(session.getUri()));
        debugArray.add(tempObj); tempObj = new JSONObject();
        tempObj.put("infoType", "method"); tempObj.put("value", String.valueOf(session.getMethod()));
        debugArray.add(tempObj); tempObj = new JSONObject();
        tempObj.put("infoType", "headers"); tempObj.put("value", toString(session.getHeaders()));
        debugArray.add(tempObj); tempObj = new JSONObject();
        tempObj.put("infoType", "params"); tempObj.put("value", toString(session.getParameters()));
        debugArray.add(tempObj); tempObj = new JSONObject();
        tempObj.put("infoType", "decodedQueryParams"); tempObj.put("value", toString(decodedQueryParameters));
        debugArray.add(tempObj); tempObj = new JSONObject();
        //json.put("debugInfo", debugArray);
        

        if ( String.valueOf(session.getMethod()).equals("POST") ) { // if this is a POST request
	        try {
	        	session.getInputStream();
	            Map<String, String> files = new HashMap<String, String>();
	            session.parseBody(files); // internally process the opost body
	            String postBody = session.getQueryParameterString(); // get the POST body
	            if (DEBUG) System.out.print("body: ");
	            if (DEBUG) System.out.println(postBody);
	            JSONParser parser = new JSONParser();
	            JSONObject JSONbody;
	            if (postBody == null) {
	            	System.out.println("body of message is empty. Proceeding.");
	            	sendToClient.replace("toPrint",""); // nothing to print
	            } else {
	            	JSONbody = (JSONObject) parser.parse( postBody );
	            	cmdVal = (String) JSONbody.get("value");
		        	if (DEBUG) System.out.println("command value = " + cmdVal);
		        	
		        	// Here we will parse the command received from the client and react accordingly
		            switch( cmdVal ) {
		            	case "RESTART":
		            		//json.put("toDisplay", )
		            		if (interactiveThread != null) {// && interactiveThread.isAlive()) {
		            			interactiveThread.stop();
		            		}
		            		
		            		// clear old communication files
		            		File tmpDir = new File("JSON_to_client.json");
							if ( tmpDir.exists() ) tmpDir.delete();
							tmpDir = new File("JSON_from_client.json");
							if ( tmpDir.exists() ) tmpDir.delete();
							
//							FileWriter resFile = new FileWriter("JSON_from_client.json");
//							resFile.write(JSONbody.toJSONString());
//							resFile.flush();
//							resFile.close();
							
							clientID = String.valueOf(session.getUri()).substring(1);
							
							interactive = null;
							interactiveThread = null;
							System.gc();
							
	            			interactive = new InteractionStageDrew();
		            		interactiveThread = new Thread( interactive );
		            		interactiveThread.start();
		            		// DREW: TODO : 2 new threads are being created each time RESTART is called
		            		//sendToClient.replace("toPrint","New session started.");
		            		
		            		if (DEBUG) System.out.println("cmdVal set");
		            		break;
		            	case "HELP":
		            		String helpStr = "List of client control commands:\n"
		            					    +"RESTART - to create a new session and start from the beginning of a day\n"
		            				        +"HELP - display the list of client commands\n"
		            						+"KILL - kill the server";
							sendToClient.replace("toPrint", helpStr);
		            		break;
		            	case "KILL":
		            		System.exit(0);
		            		break;
		            	default:
		            		// if it is not from the current client, ignore
		            		if ( !((String)JSONbody.get("clientID")).equals(clientID) ) {break;}
		            		FileWriter file = new FileWriter("JSON_from_client.json");
			                if (DEBUG) System.out.println(JSONbody);
			                if (DEBUG) System.out.println("JSON body saved to JSON_from_client.json");
			                file.write(JSONbody.toJSONString());
			                file.flush();
			            	file.close();
			            	break;
		            }
		        	
	            	
	            	
		            // trigger main server code to resume after the message from the client is parsed
		        	
		            /*synchronized(cmdVal) {
		            	cmdVal.notify();
		            }*/
		            
		            
		            
		        
		            if (DEBUG) System.out.println("----------------------- Server Output End -----------------------");
	            }
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
        } // if not POST method
        
        else if (String.valueOf(session.getMethod()) == "GET" ) { // if a GET request
        	/*
        	 * For now, we will use this as a type of ping / polling method
        	 * where the client sends a get every 0.2 seconds and the server
        	 * checks if there is any queued output and sends it back, if it exists
        	 */
        	if (clientID == null) { clientID = (String)session.getUri().substring(1); }
        	//System.out.println("Session with client " +clientID+ " making calls");
        	if ( session.getUri().substring(1).equals(clientID) ) {
        		//System.out.println("Session with client " +clientID+ " avlive");
        		
	        	try {
		        	File tmpDir = new File("JSON_to_client.json");
					if ( tmpDir.exists() ) {
						JSONParser parser = new JSONParser();
						Object fileIN = parser.parse(new FileReader("JSON_to_client.json"));
						JSONObject jsonIN = (JSONObject) fileIN;
						if (!tmpDir.delete()) { // delete the JSON file after receiving it
							System.out.println("Error: Failed to delete to_client file while reading");
							// if fail to delete, do not print it out yet
							sendToClient.replace("toPrint", "");
						} else {
							sendToClient.replace("toPrint", (String)jsonIN.get("toPrint"));
						}
						
						
					}
	
					
	        	} catch (Exception e) {
		        	e.printStackTrace();
		        }
        	} else { // if not valid client
        		sendToClient.replace("toPrint", "");
        	}
        }
        
        JSONObject responeJSON = new JSONObject(sendToClient);
        msg = responeJSON.toString();
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
