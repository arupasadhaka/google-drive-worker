### Gooogle Drive <-> S3 Integration Worker 
Driver plugin and text parser to download files and upload them to s3 for processing. 

### Design
Application sequence diagram

```
[user]            [manager]            [g-auth]   [notification]     [people]        [gdrive]    [que]             [worker's]               [s3]           [mongo]          [mysql]        [timeline]
 |                    |                   |           |                  |              |          |                   |                     |               |                |  
 |--(signup)--------> |                   |           |                  |              |          |                   |                     |               |                |
 |                    |------------------>|           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |<---(credential)---|           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |-----(persist user details)---------------------------------------------------------------------------------------------------------->|                |          [user saved]
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |--(subscribe-to-file-changes)->|                  |              |          |                   |                     |               |                |          [registered a channel and webhook]
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |        (triggers-download-meta-and-files-job)-----(persist job parameters for user)--------------------------------------------------------------------------------------->|          [job triggered]
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |            (fetches file meta)---------|-------------------------------------------->|          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |          (partition into chunks)       |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |---------------(send partition meta to que)-------------------------------->|                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |----(chunk-1)----->|                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |<---(fetch from drive)---(process)                  |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |----(upload to s3)-->|               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |-----(save file meta)--------------->|                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |<--(when needed)-->|           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |<------------------------------(when needed)------------------------------->|                     |               |                |
 |                    |                   |           |                  |              |          |                   |-----(persist job step completion for chunk 1)------->|         [step completed]
 |                    |                   |           |                  |              |          |----(chunk-2)----->|                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |----(chunk-3)----->|                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |----(chunk-4)----->|                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |----------------------------------------------------->|        [all steps completed]
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                [~loop]-----(check for job completion by polling)---------------------------------------------------------------------------------------------------------->|        [job completed]
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |------------(user updates a file in drive)------------------------------------------->|          |                   |                     |               |                |        [user makes changes in drive]
 |                    |                   |           |                  |              |          |                   |                     |               |                |  
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |<-------(calls webhook to listen change)-------------------------|          |                   |                     |               |                |        [notification triggers webhook]
 |                    |                   |           |                  |              |          |                   |                     |               |                |      
 |                    |                   |           |                  |              |          |                   |                     |               |                |      
 |                    |-------(update or remove file from s3)------------------------------------------------------------------------------->|               |                |      
 |                    |                   |           |                  |              |          |                   |                     |               |                |      
 |                    |-------(update meta and storage details)-------------------------------------------------------------------------------------------------------------->|        [notification updated]
 |                    |                   |           |                  |              |          |                   |                     |               |                |
 |                    |                   |           |                  |              |          |                   |                     |               |                |      
 ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```

### Spring batch integration 
![workflow](docs/job-repository-polling.png)

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

- start ngrok
```shell
ngrok http 80 
```

- update end point url in [src/main/resources/application.yml](src/main/resources/application.yml)
```shell
app.host.url
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

### que delete
```shell
- aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url=http://localhost:4566/000000000000/file-meta-simple-request-que
- aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url=http://localhost:4566/000000000000/file-meta-simple-reply-que 
```

## s3 
```shell
delete bucket
- aws s3 mb s3://my_bucket --endpoint-url http://localhost:4566
download all files for a use in local to verify
  - `awslocal s3 sync s3://user-people111647754396159229803 ~/mukundhan/s3`
check bucket size 
  - `awslocal s3 ls --summarize --human-readable --recursive s3://user-people111647754396159229803`
```

## dev tools
```shell
pip install awscli-local
```
- ngrok https://ngrok.com/download

## purge 

```shell
mongo database --eval 'db.users.drop()';
mongo database --eval 'db.file_meta.drop()';
mongo database --eval 'db.file_storage.drop()';
awslocal s3 rb s3://user-people111647754396159229803 --force;
```

## metric (includes fetching file from drive, saving meta, uploading to s3)
| Type   | Record Count | Time taken                   | Size per record ) | Workers |
|--------|--------------|------------------------------|-------------------|---------|
| Sync   | 50           | 38s                          | < 50kb            | 1       |
| Async  | 100          | 32s                          | 100kb             | 1       |
| Async  | 100          | 42s                          | 500kb             | 1       |
| Async  | 100          | 33s (increased buffer limit) | 1000kb            | 1       |



## links

### Google api
- https://medium.com/javarevisited/oauth-2-0-with-google-client-libraries-java-sdk-e5439accdf7a
- https://developers.google.com/api-client-library/java/google-api-java-client/oauth2
- https://developers.google.com/identity/sign-in/android/backend-auth
- https://github.com/googleworkspace/java-samples
- https://github.com/localstack/localstack

### reactive
- https://www.baeldung.com/spring-data-mongodb-reactive
- https://docs.spring.io/spring-integration/docs/current/reference/html/reactive-streams.html#reactive-streams


### spring-batch
- https://docs.spring.io/spring-batch/docs/current/reference/html/spring-batch-integration.html#remote-chunking
- https://github.com/spring-projects/spring-batch/issues/1488
- https://stackoverflow.com/questions/30786382/spring-batch-difference-between-multithreading-vs-partitioning
- https://frandorado.github.io/spring/2019/10/11/spring-batch-aws-series-partitioning.html
- https://github.com/frandorado/spring-projects/tree/master/spring-batch-aws-integration

### remote-chunking
- https://frandorado.github.io/spring/2019/09/19/spring-batch-aws-series-chunking.html#:~:text=With%20Remote%20Chunking%20the%20data,be%20returned%20to%20the%20master.&text=Slave%20doesn't%20need%20database,This%20arrives%20through%20SQS%20messages.


### file generator
- https://onlinefiletools.com/generate-random-text-file

- https://pinetools.com/random-file-generator
