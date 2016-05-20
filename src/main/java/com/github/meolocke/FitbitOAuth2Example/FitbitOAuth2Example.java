package com.github.meolocke.FitbitOAuth2Example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.github.scribejava.apis.FitbitApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.apis.service.FitbitOAuth20ServiceImpl;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * This class is part of an example implementation of the Fitbit OAuth2
 * process for authorization and resource access.
 * 
 * It is an example of the Authorization Code Grant Flow outlined on Fitbit's API
 *     https://dev.fitbit.com/docs/oauth2/#authorization-code-grant-flow
 * 
 * It uses a simple servlet CallableAuthorizationCodeServlet to catch the 
 * redirected authorization code from Fitbit.  The servlet needs a port to
 * listen to so for this example we use 8080.
 * The redirect URI on the Fitbit app setup would be 
 *       Ex.   http://localhost:8080
 *       
 * It will first ask the user to login with Fitbit (through their website), 
 * then exchanges the authorization code for an access/refresh token pair,
 * then attempts a resource request.  It also shows the refresh token procedure
 *            (http response code 401 - see case statement below)
 * though the tokens will not have expired so quickly (they last about an hour).
 * 
 * @author BethLocke meolocke.github.com
 *
 */
public class FitbitOAuth2Example {

	private final static String CALL_BACK_URI="http://localhost:8080";
	private final static int CALL_BACK_PORT=8080;

	public static void main( String[] args )
	{
		//**************  SETUP - READ IN CREDENTIALS **************
		
		/* You could access any number of ways - here they are read 
		 * from a file to avoid them being in the code for the example
		 */
		BufferedReader bufferedReader=null;

		String CLIENT_ID="";
		String API_SECRET="";	

		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = 
					new FileReader("./src/main/resources/private/ExampleCredentials.txt");		

			// Always wrap FileReader in BufferedReader.
			bufferedReader = new BufferedReader(fileReader);

			CLIENT_ID= bufferedReader.readLine();
			API_SECRET = bufferedReader.readLine();


			/* This code will read the current token information from a file
			 * if it had been saved there by a previous run of the program
			 * This is how you read it during the course, but this example
			 * will show you how to get one from a user login.  Also, feel free
			 * to change how you store the current tokens now (not in a file).
			 */
			//fileReader = new FileReader("./src/main/resources/private/ExampleTokens.txt");
			//bufferedReader = new BufferedReader(fileReader);

			//accessTokenItself = bufferedReader.readLine();
			//tokenType = bufferedReader.readLine();
			//refreshToken = bufferedReader.readLine();
			//expiresIn = Long.parseLong(bufferedReader.readLine());
			//rawResponse = bufferedReader.readLine();

		}
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file\n"+ex.getMessage()); 
			System.exit(1);
		}
		catch(IOException ex) {
			System.out.println("Error reading/write file\n"+ex.getMessage());   
			System.exit(1);
		}
		finally{
			try{
				if (bufferedReader!=null) bufferedReader.close();  
			}
			catch(Exception e){
				System.out.println("Error closing file\n"+e.getMessage());  
				System.exit(1);
			}
		}

		//************** MAIN LOGIC FOR SERVER INTERACTION **************

		/* A Future object is part of the concurrent package which helps us with the lock-step of 
		 * our two operations - running a servlet that will catch the response from Fitbit, and
		 * this program here that needs to get the code from that servlet.
		 */
		// Initialize empty Future object, this will eventually let us get the code from it
		Future<String> futureCode = null;
		// The executor is what will actually run the servlet task and handle threading for us
		ExecutorService executor = Executors.newFixedThreadPool(1);
		try{
			/* So we give control of that servlet task to the executor, and it gives us back an
			 * object that in the future we can "get()" a value from (Since the servlet implements the Callable interface)
			 */
			futureCode = executor.submit(new CallableAuthorizationCodeServlet(CALL_BACK_PORT));
		}
		catch (IOException e){
			System.out.println("Could not initialize Servlet");
			System.exit(1);
		}

		/*
		 * The client id here is your applications, not the user
		 * The scope can be defined to be whatever your application wants access to
		 *         See: https://dev.fitbit.com/docs/oauth2/#scope
		 * The read/write access has to be set in the application settings under Manage My Apps on dev.fitbit.com
		 */
		String scope = "activity%20heartrate%20nutrition";  
		String requestAddress = "https://www.fitbit.com/oauth2/authorize?response_type=code&"+""
				+ "client_id="+CLIENT_ID
				+ "&redirect_uri="+CALL_BACK_URI
				+ "&scope="+scope;

		String code="";
		try {
			/* This will open the browser so the user can log into Fitbit.  The redirect will
			 * come to the servlet which is listening to the port we have Fitbit redirecting 
			 * to on localhost
			 * NOTE:  If your browser has cookies/passwords enabled, you may not have to actually
			 *        log in each time, but I haven't found a way around the process from this side
			 * NOTE:  On some browsers you may have to enable/allow localhost to be accessed at all 
			 *        this would be done through the browsers settings (Google it).
			 */
			if(Desktop.isDesktopSupported())
			{
				Desktop.getDesktop().browse(new URI(requestAddress));
				
				/* This will stall execution until the code is received.
				 * Timeout parameters added so it does not wait forever.
				 */
				code = futureCode.get(20, TimeUnit.SECONDS);
			}
			else{
				System.out.println("No default browser could be started with Desktop.getDesktop().browse");
			}
		} catch (InterruptedException e) {
			System.out.println("Thread interrupted while waiting: " + e.getMessage()); 
		} catch (ExecutionException e) {
			System.out.println("Exception thrown while executing get" + e.getMessage());
		} catch (TimeoutException e) {
			System.out.println("Login operation timed out");
		} catch (IOException e) {
			System.out.println("Error while attempting to get access code: "+ e.getMessage());
		} catch (URISyntaxException e) {
			System.out.println("URI syntax likely invalid: " + e.getMessage());
		} 
		finally {
			// MUST close the executor running the servlet
			executor.shutdown();

			if( code==""){
				System.out.println("No code recieved");
				System.exit(1);
			}
		}

		System.out.println("Got the Request Verifier Code!");
		System.out.println("(if your curious it looks like this: " + code + " )");
		System.out.println();

		//************** Trade the Request Token and Verifier for the Access Token **************

		/* Setup the Fitbit Service - This is where you can trade the authorization code for
		 * an access token, and the access token allows you to do resource pulls
		 */
		FitbitOAuth20ServiceImpl service = (FitbitOAuth20ServiceImpl) new ServiceBuilder()
				.apiKey(CLIENT_ID)
				.apiSecret(API_SECRET)
				.callback("http://localhost:8080")
				.scope(scope)
				.grantType("authorization_code")
				.build(FitbitApi20.instance());

		System.out.println("Trading the Request Token for an Access Token...");

		//Setup placeholder token (required by library API, but not used by this service)
		final Token EMPTY_TOKEN = null;
		//Add the verifier, which in our case is the authorization code we got from the servlet
		Verifier verifier = new Verifier(code);

		//Make the actually request to the Fitbit service
		OAuth2AccessToken accessToken = (OAuth2AccessToken) service.getAccessToken(EMPTY_TOKEN, verifier);
		/* The access token contains everything you will need to authenticate your requests
		 * It can expire - at which point you will use the refresh token to refresh it (see below for example)
		 *       See: https://dev.fitbit.com/docs/oauth2/#refreshing-tokens
		 * I had provided these for you in the course as token files, but now you can get them on your own
		 */

		System.out.println("Got the Access Token!");
		System.out.println("(if your curious it looks like this: " + accessToken.getToken() + " )");
		System.out.println();

		//************** MAKE A RESOURCE REQUEST **************
		// Now let's go and ask for a protected resource!
		System.out.println("Now we're going to access a protected resource...");
		System.out.println();

		/*Example request
		 *     This is always the prefix (the dash is for the currently logged in user)
		 */
		String requestUrlPrefix = "https://api.fitbit.com/1/user/-/";

		 /* Or you can get the user's ID from the access token
		  * It is in the raw json response as "user_id"
		 */  
        JSONObject jsonObj;       
		try {
			jsonObj = new JSONObject(accessToken.getRawResponse());
			String userID = jsonObj.getString("user_id");
			System.out.println("User's ID is: "+userID);
		} catch (JSONException e1) {
			System.out.println("Could not print user ID - JSON issue with: " + accessToken.getRawResponse());
		}
		
		
		/* The URL from this point is how you ask for different information.
		 * Some examples:
		 *        activities/activityCalories/date/2016-01-08/1d.json
		 *        activities/heart/date/2016-01-07/1d/1min.json
		 *        foods/log/goal.json
		 *        foods/log/date/2016-02-26.json
		 *        foods/log/water/date/2016-02-26.json
		 *        foods/log/favorite.json
		 */
		String suffix = "activities.json";
		
		String requestUrl = requestUrlPrefix + suffix;
	
		// This actually generates an HTTP request from the URL it has a header, body ect.
		OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl, service);

		/* This adds the information required by Fitbit to add the authorization information to the HTTP request
		 * You must do this before the request will work
		 *          See: https://dev.fitbit.com/docs/oauth2/#making-requests
		 */
		service.signRequest(accessToken, request);
		/*You can add headers (for example for Locale and Language ie. Units) like this
		 *   (Ex. see https://dev.fitbit.com/docs/activity/)
		 */
		//    request.addHeader("Accept-Language", "en_GB");

		//  If you are curious
		System.out.println("The resource request looks like this:");
		System.out.println(request.toString());
		System.out.println(request.getHeaders());
		System.out.println(request.getBodyContents());

		//  This actually sends the request:
		Response response = request.send();

		/* The HTTP response from Fitbit will be in HTTP format, meaning that it has a numeric code indicating 
		 *     whether is was successful (200) or not (400's or 500's), each code has a different meaning
		 */
		System.out.println();
		System.out.println("HTTP response code: "+response.getCode());

		int statusCode = response.getCode();

		switch(statusCode){
		case 200:
			System.out.println("Success!");
			System.out.println("HTTP response body:\n"+response.getBody());
			
			/* Example of simple JSON parsing for lifetime steps
			 *    To ease the process of figuring out the format, use a JSON viewer like: http://jsonviewer.stack.hu/
			 *    on the various response bodies that you get.
			 */
			JSONObject activities;       
			try {
				activities = new JSONObject(response.getBody());
				Integer lifetimeSteps = activities.getJSONObject("lifetime").getJSONObject("total").getInt("steps");
				System.out.println("User's lifetime steps are: "+lifetimeSteps);
			} catch (JSONException e1) {
				System.out.println("Could not print steps - JSON issue" + e1.getMessage());
			}
			
			break;
		case 400:
			System.out.println("Bad Request - may have to talk to Beth");
			System.out.println("HTTP response body:\n"+response.getBody());
			break;
		case 401:
			System.out.println("Likely Expired Token");
			System.out.println("HTTP response body:\n"+response.getBody());  
			System.out.println("Try to refresh");

			/* This uses the refresh token to get a completely new accessToken object
			 *   See:  https://dev.fitbit.com/docs/oauth2/#refreshing-tokens			
			 * This accessToken is now the current one, and the old ones will not work 
			 *   again.  You should save the contents of accessToken.
			 */
			accessToken = service.refreshOAuth2AccessToken(accessToken);

			System.out.println("Try to again access the resource...");
			/*Now we can try to access the service again
			 * Make sure you create a new OAuthRequest object each time!
			 */
			request = new OAuthRequest(Verb.GET, requestUrl, service);
			service.signRequest(accessToken, request);
			response = request.send();

			// Hopefully got a response this time:
			System.out.println("HTTP response code: "+response.getCode());
			System.out.println("HTTP response body:\n"+response.getBody());	
			break;
		case 429:
			System.out.println("Rate limit exceeded");
			System.out.println("HTTP response body:\n"+response.getBody());
			break;
		default:
			System.out.println("HTTP response code: "+response.getCode());
			System.out.println("HTTP response body:\n"+response.getBody());	
		}

		//************** SHUTDOWN - SAVE CURRENT TOKEN TO A FILE**************
		/* You don't have to save it to a file, but as you well know, you do have
		 * to save it somehow.
		 */
		BufferedWriter bufferedWriter=null;
		try {
			FileWriter fileWriter;  
			fileWriter =
					new FileWriter("./src/main/resources/private/ExampleTokens.txt");
			bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(accessToken.getToken());
			bufferedWriter.newLine();
			bufferedWriter.write(accessToken.getTokenType());
			bufferedWriter.newLine();
			bufferedWriter.write(accessToken.getRefreshToken());
			bufferedWriter.newLine();
			bufferedWriter.write(accessToken.getExpiresIn().toString() );
			bufferedWriter.newLine();
			bufferedWriter.write(accessToken.getRawResponse());
			bufferedWriter.newLine();
			bufferedWriter.close();
		}
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file\n"+ex.getMessage());                
		}
		catch(IOException ex) {
			System.out.println("Error reading/write file\n"+ex.getMessage());                  
		}
		finally{
			try{
				if (bufferedReader!=null) bufferedReader.close();  
				if (bufferedWriter!=null) bufferedWriter.close();  
			}
			catch(Exception e){
				System.out.println("Error closing file\n"+e.getMessage());  

			}
		}
	}//end main
}//end class
