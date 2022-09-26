package com.oslash.integration.manager.reader;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.models.User;
import com.oslash.integration.resolver.IntegrationResolver;
import com.oslash.integration.utils.Constants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Files partitioner.
 */
public class FilesPartitioner extends MultiResourcePartitioner {
    /**
     * The constant PARTITION_PREFIX.
     */
    public static final String PARTITION_PREFIX = "partition";
    private final User user;
    private final AppConfiguration appConfiguration;
    /**
     * The Logger.
     */
    Logger logger = LoggerFactory.getLogger(getClass());
    private String nextPageToken;
    private Drive drive;
    private boolean reachedEnd;

    /**
     * Instantiates a new Files partitioner.
     *
     * @param user             the user
     * @param appConfiguration the app configuration
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2"})
    public FilesPartitioner(User user, AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
        this.user = user;
        init(user);
    }

    /**
     * Init.
     *
     * @param user the user
     */
    @SneakyThrows
    private void init(User user) {
        this.drive = IntegrationResolver.resolveGDrive(user.getId());
    }


    /**
     * Partition map.
     *
     * @param gridSize the grid size
     * @return the map
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int partitionCount = 0;
        Map<String, ExecutionContext> partitions = new HashMap<>();
        while (!reachedEnd) {
            try {
                String filesQuery = String.format("%s='%s'", Constants.MIME_TYPE, appConfiguration.getMimeType());
                filesQuery += Constants.AND + getFolderQuery();
                logger.debug(String.format("partition query %s", filesQuery));
                FileList fileList = drive.files().list()
                        // selecting only documents
                        .setQ(filesQuery).setSpaces(Constants.DRIVE_SPACE).setPageSize(gridSize).setPageToken(nextPageToken).setFields("files(id,name,thumbnailLink,mimeType),nextPageToken").execute();
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
                String message = String.format("error while partitioning files for user %s", user.getId());
                throw new RuntimeException(message, e);
            }
        }
        return partitions;
    }

    /**
     * Gets folder query.
     *
     * @return the folder query
     * @throws IOException the io exception
     */
    private String getFolderQuery() throws IOException {
        String result = "";
        if (Strings.isNotBlank(appConfiguration.getFolderName())) {
            String folderQuery = String.format("%s='%s'", Constants.MIME_TYPE, Constants.GOOGLE_DRIVE_FOLDER_MIME_TYPE);
            folderQuery += Constants.AND + String.format("%s='%s'", Constants.NAME, appConfiguration.getFolderName());
            FileList folders = drive.files().list().setQ(folderQuery).setSpaces(Constants.DRIVE_SPACE).setFields("files(id,name),nextPageToken").execute();
            if (folders != null && !CollectionUtils.isEmpty(folders.getFiles())) {
                logger.info(String.format("found folder %s for the user %s", appConfiguration.getFolderName(), user.getId()));
                // eg: https://www.googleapis.com/drive/v3/files?q="folderId"+in+parents&fields=files(md5Checksum,+originalFilename)
                result += String.format("'%s' %s %s", folders.getFiles().get(0).getId(), Constants.IN, Constants.FOLDERS);
            } else {
                logger.info(String.format("folder %s not found for the user %s", appConfiguration.getFolderName(), user.getId()));
            }
        }
        return result;
    }

    /**
     * Gets partition meta data.
     *
     * @param fileList the file list
     * @return the partition meta data
     */
    private List<Map> getPartitionMetaData(FileList fileList) {
        List<Map> filesMeta = new ArrayList();
        int size = fileList.size();
        for (int i = size; i < fileList.getFiles().size(); i++) {
            File file = fileList.getFiles().get(i);
            Map item = new HashMap();
            item.put(Constants.FILE_ID, file.getId());
            item.put(Constants.FILE_NAME, file.getName());
            item.put(Constants.USER_ID, user.getId());
            item.put(Constants.MIME_TYPE, file.getMimeType());
            filesMeta.add(item);
        }
        return filesMeta;
    }
}
