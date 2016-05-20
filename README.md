# FitbitOAuth2Example
This example was developed for students as part of a class I taught CS2212 at the University of Western Ontario. It uses a version of the scribejava package which includes the addition of a fitbit implementation. It demonstrates the use of a localhost servlet for authentication, as well as a resource pull example.

![FitbitOAuth2Example Code Sequence Diagram](http://i.imgur.com/PoxWDha.png)

# Getting started
1. Create a developer account with the Fitbit dev site http://dev.fitbit.com
  * Make sure to read the terms and conditions: https://dev.fitbit.com/terms
1. Register an App
  * OAuth 2.0 Application Type should be:

      personal --if you will only login with your own Fitbit account 
   
      client --if you want anyone to be able to log in
  * Callback URL for this example is:
  
      http://localhost:8080

# Save App credentials in src/main/resources/private/ExampleCredentials.txt

Note: The src/main/resources/private directory is ignored by git using the .gitignore file
Students: There is no longer an API key provided by Fitbit, so take it out of the credentials file.

1. Under "Manage My Applications" you can click on the name of the App to bring up the credentials page.
1. You will need to use the Client ID and Client Secret for this example
  * However - don't release them onto Github in your source code or with your App as a text file or readable in any way.  They are private to you as a developer.
1. If, for example, your client ID was 555GGG, and your secret was 85h6ca8e9009f87ee67882e18a029bc8, create a text file with the following format (two lines, no indent):
```
555GGG
85h6ca8e9009f87ee67882e18a029bc8
```
1. This example code will read the credentials from this file, and save the access/refresh token pair recieved as ExampleTokens.txt in the same directory

# Package and Run
```
mvn package
java -jar target/FitbitOAuth2Example-1.0-jar-with-dependencies.jar
```
You would need a Fitbit account or willing user to log in.
