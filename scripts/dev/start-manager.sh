
export JAVA_HOME=`/usr/libexec/java_home -v 17.0.4`
mvn compile;
mvn spring-boot:run -Dspring.profiles.active=manager;
