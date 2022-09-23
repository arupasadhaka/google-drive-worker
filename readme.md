### Setup 

- Create a Project in Google could and include Google Drive API
- Add credential with Oauth with callback and redirect URLs
- Permissions have to be mapped only for read files

- Whitelist the local URI and add the Oauth callback to get the user credentials
  `google.oauth.callback.uri=http://localhost/oauth`

- Download the client secret and store in the path specified 
  `google.app.secret.key.path=classpath:keys/client_secret.json`

- Add test users initially before publishing the project

# links
- https://medium.com/javarevisited/oauth-2-0-with-google-client-libraries-java-sdk-e5439accdf7a
- https://developers.google.com/api-client-library/java/google-api-java-client/oauth2
- https://developers.google.com/identity/sign-in/android/backend-auth
- https://github.com/googleworkspace/java-samples