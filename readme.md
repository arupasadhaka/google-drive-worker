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

### spring-batch
- https://docs.spring.io/spring-batch/docs/current/reference/html/spring-batch-integration.html#remote-chunking
- https://github.com/spring-projects/spring-batch/issues/1488
- https://arnoldgalovics.com/spring-batch-remote-partitioning-aws-sqs
- https://frandorado.github.io/spring/2019/07/29/spring-batch-aws-series-introduction.html
- https://frandorado.github.io/spring/2019/09/19/spring-batch-aws-series-chunking.html#:~:text=With%20Remote%20Chunking%20the%20data,be%20returned%20to%20the%20master.&text=Slave%20doesn't%20need%20database,This%20arrives%20through%20SQS%20messages.