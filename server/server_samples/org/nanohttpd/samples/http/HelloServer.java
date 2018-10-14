package org.nanohttpd.samples.http;

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

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class HelloServer extends NanoHTTPD {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(HelloServer.class.getName());

    public static void main(String[] args) {
        ServerRunner.run(HelloServer.class);
    }

    public HelloServer() {
        super(8080);
    }

    
    /*
     * This is how the server reacts to a request. In order to implement custom servers, this method must be overridden
     * (non-Javadoc)
     * @see org.nanohttpd.protocols.http.NanoHTTPD#serve(org.nanohttpd.protocols.http.IHTTPSession)
     */
    @Override
    public Response serve(IHTTPSession session) {
        String msg = "";
        JSONObject json = new JSONObject();
        json.put("greeting", "Hello");
        json.put("who", "world");
        

        msg += json.toString();

        return Response.newFixedLengthResponse(msg);
    }
}
