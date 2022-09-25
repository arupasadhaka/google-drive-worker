package com.oslash.integration.manager.steps.reader;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.oslash.integration.models.FileMeta;
import com.oslash.integration.utils.Constants;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.oslash.integration.resolver.GoogleApiResolver.apiResolver;

public class FileMetaReader implements ItemReader, StepExecutionListener {

    Logger logger = LoggerFactory.getLogger(getClass());

    private String userId;
    private Drive drive;
    private String nextPageToken;

    private boolean reachedEnd = false;

    private List<File> files = new ArrayList();

    public FileMetaReader(String userId) {
        this.userId = userId;
        init(userId);
    }

    //    @SneakyThrows
    public void init(String userId) {
        final Credential credential;
        try {
            credential = apiResolver().authorizationCodeFlow().loadCredential(userId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.drive = new Drive.Builder(apiResolver().HTTP_TRANSPORT, apiResolver().JSON_FACTORY, credential).setApplicationName("appName").build();
        nextPageToken = null;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        init(userId);
    }

    public ExitStatus afterStep(StepExecution stepExecution) {
        return reachedEnd ? ExitStatus.COMPLETED : ExitStatus.FAILED;
    }

    public void readFileList() {
        FileList result = null;
        try {
            result = drive.files()
                    .list()
                    .setPageSize(10)
                    .setPageToken(nextPageToken)
                    .setFields("files(id,name,thumbnailLink,mimeType),nextPageToken")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (Strings.isEmpty(result.getNextPageToken())) {
            reachedEnd = true;
        } else {
            nextPageToken = result.getNextPageToken();
        }
        this.files = result.getFiles();
    }

    public Serializable checkpointInfo() {
        HashMap<String, String> checkpointInfo = new HashMap<>();
        checkpointInfo.put(Constants.USER_ID, userId);
        checkpointInfo.put(Constants.NEXT_PAGE_TOKEN, nextPageToken);
        return checkpointInfo;
    }

    //    @SneakyThrows
    @Override
    public FileMeta read() {
        if (!reachedEnd && files.isEmpty()) {
            readFileList();
            logger.info(String.format("check point : %s", checkpointInfo()));
        }
        if (files.isEmpty()) {
            return null;
        }
        File file = files.remove(0);
        // .content(file).mimeType(file.getMimeType()).content(file).userId(userId)
        return new FileMeta.Builder().userId(userId).id(file.getId()).build();
    }
}
