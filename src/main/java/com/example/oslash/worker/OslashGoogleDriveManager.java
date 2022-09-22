package com.example.oslash.worker;


import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

// implement interface and expose static methods
@Component
public class OslashGoogleDriveManager {
    private static final Logger LOG = Logger.getLogger(OslashGoogleDriveManager.class.getName());

    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE, "https://www.googleapis.com/auth/drive.install");

    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private GoogleAuthorizationCodeFlow gAuthFlow;

    @Autowired
    private ApplicationContext appContext;

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFolder;

    @Autowired
    private Environment environment;

    @Value("${google.app.secret.key.path}")
    private Resource appSecretKey;

    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        LOG.info("initializing worker");
        init();
    }

    public void init() {
        try {
            LOG.info("fetching secrets");
            GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(appSecretKey.getInputStream()));
            LOG.info("connecting google drive auth flow");
            gAuthFlow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile())).build();
            LOG.info("connected to google drive auth flow");
        } catch (Exception e) {
            LOG.log(SEVERE, String.format("Error connecting to google drive auth flow : reason %s", e.getMessage()), e);
            SpringApplication.exit(appContext, () -> 1);
        }
    }
}