package com.oslash.integration.resolver;

import com.amazonaws.services.s3.AmazonS3;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.Person;
import com.oslash.integration.models.User;
import com.oslash.integration.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// implement interface and expose static methods
@Component
public class IntegrationResolver {
    private static IntegrationResolver INSTANCE;
    public final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private final Logger logger = LoggerFactory.getLogger(IntegrationResolver.class);
    private final List<String> SCOPES = new ArrayList<String>();

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFile;

    @Value("${google.app.access.type}")
    private String accessType;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${google.app.secret.key.path}")
    private Resource appSecretKey;
    @Value(("${google.oauth.callback.uri}"))
    private String callBackUrl;
    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private UserService userService;

    @Autowired
    private AmazonS3 amazonS3;

    private GoogleAuthorizationCodeFlow authorizationCodeFlow;

    public static IntegrationResolver integrationResolver() {
        assert INSTANCE != null : "driver not initialized";
        return INSTANCE;
    }

    public static Drive resolveGDrive(String userId) throws IOException {
        Credential cred = integrationResolver().authorizationCodeFlow().loadCredential(userId);
        return new Drive.Builder(integrationResolver().HTTP_TRANSPORT, integrationResolver().JSON_FACTORY, cred).setApplicationName("appName").build();
    }

    public static IntegrationResolver getInstance() {
        return INSTANCE;
    }

    public static AmazonS3 resolveStorage() {
        return getInstance().amazonS3;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        logger.info("initializing worker");
        init();
    }

    private void init() {
        try {
            logger.info("fetching secrets");
            // TODO: ADD only read permissions for drive and people
            SCOPES.addAll(DriveScopes.all());
            SCOPES.addAll(PeopleServiceScopes.all());
            initializeAuthFlow();
            INSTANCE = this;
        } catch (Exception e) {
            logger.error(String.format("Error connecting to google drive auth flow : reason %s", e.getMessage()), e);
            SpringApplication.exit(appContext, () -> 0);
        }
    }

    private PeopleService peopleService() throws IOException {
        Credential credential = integrationResolver().authorizationCodeFlow().loadCredential(null);
        return new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(appName).build();
    }

    private void initializeAuthFlow() throws IOException {
        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(appSecretKey.getInputStream()));
        logger.info("connecting google drive auth flow");
        authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES)
                // setting this to share credentials across manager and slave, need to replace this with custom cached DataStoreFactory
                .setDataStoreFactory(new FileDataStoreFactory(credentialsFile.getFile()))
                .build();
        logger.info("connected to google drive auth flow");
    }

    public User saveUserDetails(GoogleTokenResponse response, Person person) throws Exception {
        // persist refresh token with user details in mongo with encryption
        String refreshToken = response.getRefreshToken();
        String userId = person.getResourceName();
        String primaryEmail = person.getEmailAddresses().stream().filter(email -> email.getMetadata().getPrimary()).map(email -> email.getValue()).findFirst().orElseGet(() -> "");
        User user = new User.Builder().content(person).email(primaryEmail).refreshToken(refreshToken).id(userId).build();
        userService.save(user).block();
        this.authorizationCodeFlow().createAndStoreCredential(response, userId);
        return user;
    }

    public Person getPerson(GoogleTokenResponse response) throws IOException {
        Person person = this.peopleService().people().get("people/me")
                .setOauthToken(response.getAccessToken())
                // check and add additional fields
                .setPersonFields("names,emailAddresses")
                .execute();
        return person;
    }

    public GoogleAuthorizationCodeFlow authorizationCodeFlow() {
        return authorizationCodeFlow;
    }

    public String callBackUrl() {
        return callBackUrl;
    }

    public String accessType() {
        return accessType;
    }
}
