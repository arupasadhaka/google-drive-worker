
export JAVA_HOME=`/usr/libexec/java_home -v 17.0.4`
mvn compile;
mvn spring-boot:run -Dspring-boot.run.profiles=worker -Dcom.amazonaws.sdk.s3.defaultStreamBufferSize=100000
