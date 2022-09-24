package com.oslash.integration.resolver;

import com.oslash.integration.service.UserService;
import com.oslash.integration.models.User;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.Person;
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
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

// implement interface and expose static methods
@Component
public class GoogleApiResolver {
    private final Logger logger = Logger.getLogger(GoogleApiResolver.class.getName());

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

    public final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static GoogleApiResolver INSTANCE;

    private GoogleAuthorizationCodeFlow authorizationCodeFlow;

    private final List<String> SCOPES = new ArrayList<String>();


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
            logger.log(SEVERE, String.format("Error connecting to google drive auth flow : reason %s", e.getMessage()), e);
            SpringApplication.exit(appContext, () -> 0);
        }
    }

    private PeopleService peopleService() throws IOException {
        Credential credential = apiResolver().authorizationCodeFlow().loadCredential("USER_IDENTIFIER_KEY");
        return new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(appName).build();
    }

    private void initializeAuthFlow() throws IOException {
        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(appSecretKey.getInputStream()));
        logger.info("connecting google drive auth flow");
        authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES).setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance()).build();
        logger.info("connected to google drive auth flow");
    }

    public Person saveUserDetails(GoogleTokenResponse response) throws Exception {
        Person person = this.peopleService().people().get("people/me")
            .setOauthToken(response.getAccessToken())
            // check and add additional fields
            .setPersonFields("names,emailAddresses")
            .execute();
        // persist refresh token with user details in mongo with encryption
        String refreshToken = response.getRefreshToken();
        String userId = person.getResourceName();
        String primaryEmail = person.getEmailAddresses().stream().filter(email -> email.getMetadata().getPrimary()).map(email -> email.getValue()).findFirst().orElseGet(() -> "");
        User user = new User.Builder().content(person).email(primaryEmail).refreshToken(refreshToken).id(userId).build();
        userService.save(user).block();
        this.authorizationCodeFlow().createAndStoreCredential(response, userId);
        return person;
    }


    public GoogleAuthorizationCodeFlow authorizationCodeFlow() {
        return authorizationCodeFlow;
    }

    public static GoogleApiResolver apiResolver() {
        assert INSTANCE != null : "driver not initialized";
        return INSTANCE;
    }

    public String callBackUrl() {
        return callBackUrl;
    }

    public String accessType() {
        return accessType;
    }
}
