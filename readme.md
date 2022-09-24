### Google Cloud Setup 

- Create a Project in Google could and include Google Drive API
- Add credential with Oauth with callback and redirect URLs
- Permissions have to be mapped only for read files

- Whitelist the local URI and add the Oauth callback to get the user credentials
  `google.oauth.callback.uri=http://localhost/oauth`

- Download the client secret and store in the path specified 
  `google.app.secret.key.path=classpath:keys/client_secret.json`

- Add test users initially before publishing the project

### Local Setup

- install dependencies
```shell
mvn dependency:resolve;
```

- setup infra
```
docker-compose up -d;
```

- start manager
```shell
sh scripts/dev/start-manager.sh;
```

- start worker
```shell
sh scripts/dev/start-worker.sh;
```

- signup using g-auth and provide permissions
```shell
http://localhost/signup 
```
- file meta and user details will be persisted in mongo db

## links
### Google api
- https://medium.com/javarevisited/oauth-2-0-with-google-client-libraries-java-sdk-e5439accdf7a
- https://developers.google.com/api-client-library/java/google-api-java-client/oauth2
- https://developers.google.com/identity/sign-in/android/backend-auth
- https://github.com/googleworkspace/java-samples
- https://github.com/localstack/localstack
### mongo reactive
- https://www.baeldung.com/spring-data-mongodb-reactive