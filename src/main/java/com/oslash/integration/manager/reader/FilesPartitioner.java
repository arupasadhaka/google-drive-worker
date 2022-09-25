package com.oslash.integration.manager.reader;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.models.User;
import com.oslash.integration.utils.Constants;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.item.ExecutionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oslash.integration.resolver.GoogleApiResolver.apiResolver;

public class FilesPartitioner extends MultiResourcePartitioner {
    public static final String PARTITION_PREFIX = "partition";
    private final User user;
    private final AppConfiguration appConfiguration;
    Logger logger = LoggerFactory.getLogger(getClass());
    private String nextPageToken;
    private Drive drive;
    private boolean reachedEnd;

    public FilesPartitioner(User user, AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
        this.user = user;
        init(user);
    }

    @SneakyThrows
    private void init(User user) {
        Credential cred = apiResolver().authorizationCodeFlow().loadCredential(user.getId());
        this.drive = new Drive.Builder(apiResolver().HTTP_TRANSPORT, apiResolver().JSON_FACTORY, cred).setApplicationName("appName").build();
    }


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int partitionCount = 0;
        Map<String, ExecutionContext> partitions = new HashMap<>();
        while (!reachedEnd) {
            try {
                FileList fileList = drive.files()
                        .list()
                        // selecting only documents
                        .setQ(String.format("%s='%s'", Constants.MIME_TYPE, appConfiguration.getMimeType()))
                        .setSpaces(Constants.DRIVE_SPACE)
                        .setPageSize(gridSize)
                        .setPageToken(nextPageToken)
                        .setFields("files(id,name,thumbnailLink,mimeType),nextPageToken")
                        .execute();
                if (fileList.size() > 0) {
                    ExecutionContext executionContext = new ExecutionContext();
                    executionContext.put("data", getPartitionMetaData(fileList));
                    partitions.put(PARTITION_PREFIX + partitionCount, executionContext);
                    partitionCount++;
                    nextPageToken = fileList.getNextPageToken();
                    if (Strings.isEmpty(fileList.getNextPageToken())) {
                        reachedEnd = true;
                        logger.info(String.format("fetched and add files for the user %s", user.getId()));
                    }
                    logger.info(String.format("fetched %s files for the user %s, next page token:%s batch-size:%s", fileList.getFiles().size(), user.getId(), nextPageToken, gridSize));
                } else {
                    reachedEnd = true;
                    logger.info(String.format("fetched %s files for the user %s", partitions.keySet().size(), user.getId()));
                }
            } catch (IOException e) {
                logger.error(String.format("error while partitioning files for user %s", user.getId()), e);
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    private List<Map> getPartitionMetaData(FileList fileList) {
        List<Map> filesMeta = new ArrayList();
        int size = fileList.size();
        for (int i = size; i < fileList.getFiles().size(); i++) {
            File file = fileList.getFiles().get(i);
            Map item = new HashMap();
            item.put(Constants.FILE_ID, file.getId());
            item.put(Constants.USER_ID, user.getId());
            item.put(Constants.MIME_TYPE, file.getMimeType());
            filesMeta.add(item);
        }
        return filesMeta;
    }
}
