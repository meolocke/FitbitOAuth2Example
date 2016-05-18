package com.github.meolocke.FitbitOAuth2Example;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
/**
 * This class is part of an example implementation of the Fitbit OAuth2
 * process for authorization and resource access.
 * 
 * It is a small and extremely basic servlet that accepts redirects from
 * the Fitbit authorization process by listening to a specific port (Ex. 8080).
 * The redirect URI on the Fitbit app setup would be 
 *       Ex.   http://localhost:8080
 *       
 * It will return the verification code from the call function, and is meant
 * to be used with ExecutorService and Future objects.
 * 
 * You can also get the verification code using the simple getters if you
 * keep a reference to the servlet.
 * 
 * @author BethLocke meolocke.github.com
 *
 */
public class CallableAuthorizationCodeServlet implements Callable<String>{

	//The standard beginning of a simple HTTP response where the request succeeds
	private final String STATUS_200 = "HTTP/1.1 200 OK";
	
	//Particular to Fitbit API - may change in future
	private final String EXPECTED_RESPONSE_PREFIX = "GET /?code=";
	private final String EXPECTED_CODE_SUFFIX = " HTTP/1.1";
	
	private ServerSocket serverSocket;
	private Socket socket;
	private BufferedReader in;
	
	private StringBuilder httpRequest;
	private String code="";
	
	//This might throw an exception if you cannot listen on that port
	public CallableAuthorizationCodeServlet(int portNumber) throws IOException{
		this.serverSocket = new ServerSocket(portNumber);	
	}
	
	public String getFullCallBack(){
		if (socket==null){
			return "";
		}
		return httpRequest.toString();
	}
	public boolean codeReceived(){
		return code!="";
	}
	public String getCode(){
		return code;
	}

	public String call() throws IOException {
		try{
			//Try to connect
			this.socket = serverSocket.accept();
			//Now connected
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			httpRequest = new StringBuilder();

			// VERY BASIC http interaction dependent on exact syntax
			// You could replace this with more robust javax HttpServlet for example
			// This worked for the course and did not require more libraries
			if(in.ready()){
				//until we get the HTTP request as expected
				//   this request actually contains the code we want 
				while(httpRequest.indexOf(EXPECTED_CODE_SUFFIX) == -1)		
					httpRequest.append(in.readLine());
			}
			this.code = httpRequest.substring(httpRequest.indexOf(EXPECTED_RESPONSE_PREFIX)+EXPECTED_RESPONSE_PREFIX.length(), httpRequest.indexOf(EXPECTED_CODE_SUFFIX));
			//System.out.println(httpRequest);
			//Quick and dirty HTTP response posted to the browser
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println(STATUS_200 + "\n\nThank you. You can now return to the desktop application.");
		}
		catch (IOException e) {
			throw new IOException("Exception caught when trying to listenning for connection", e);		
		}
		catch(StringIndexOutOfBoundsException e){
			throw new IOException("Response did not include expected prefix", e);
		}
		finally{
			try{
				socket.close();
			} catch (IOException e) {
				throw new IOException("Error closing socket", e);
			}
		}
		return this.code;
	}

}
