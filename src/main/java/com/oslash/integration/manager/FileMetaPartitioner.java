package com.oslash.integration.manager;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.oslash.integration.models.User;
import com.oslash.integration.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oslash.integration.resolver.GoogleApiResolver.apiResolver;

public class FileMetaPartitioner implements Partitioner {
    Logger logger = LoggerFactory.getLogger(FileMetaPartitioner.class);
    public static final String PARTITION_PREFIX = "partition";
    private final User user;

    public FileMetaPartitioner(User user) {
        this.user = user;
    }

    // todo: implement file meta processing for an user
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        try {
            Credential cred = apiResolver().authorizationCodeFlow().loadCredential(user.getId());
            Drive drive = new Drive.Builder(apiResolver().HTTP_TRANSPORT, apiResolver().JSON_FACTORY, cred).setApplicationName("appName").build();
            FileList fileList = drive.files().list().setFields("files(id,name,thumbnailLink,mimeType)").execute();
            for (int i = 0; i < fileList.getFiles().size(); i++) {
                File file = fileList.getFiles().get(i);
                Map item = new HashMap();
                item.put(Constants.FILE_ID, file.getId());
                item.put(Constants.USER_ID, user.getId());
                item.put(Constants.MIME_TYPE, file.getMimeType());
                item.put(Constants.THUMBNAIL_LINK, file.getThumbnailLink());
                ExecutionContext executionContext = new ExecutionContext();
                executionContext.put("data", item);
                partitions.put(PARTITION_PREFIX + i, executionContext);
            }
        } catch (IOException e) {
            logger.error(String.format("error while partitioning files for user %s", user.getId()), e);
            throw new RuntimeException(e);
        }
        return partitions;
    }
}
