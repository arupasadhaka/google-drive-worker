package com.oslash.integration.controller;

import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.manager.config.ManagerConfiguration;
import com.oslash.integration.models.FileMeta;
import com.oslash.integration.models.FileStorage;
import com.oslash.integration.service.FileMetaService;
import com.oslash.integration.service.FileStorageService;
import com.oslash.integration.utils.Constants;
import com.oslash.integration.utils.ResourceState;
import com.oslash.integration.worker.model.FileStorageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.oslash.integration.utils.ResourceState.*;
import static java.util.Objects.nonNull;


/**
 * The type Driver controller.
 */
@Profile("manager")
@RestController
public class FilesController {

    private final Logger logger = LoggerFactory.getLogger(FilesController.class);

    /**
     * The App configuration.
     */
    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    FileMetaService fileMetaService;

    @Autowired
    FileStorageService fileStorageService;

    /**
     * The Manager.
     */
    @Autowired
    ManagerConfiguration manager;

    /**
     * File changes.
     *
     * @param request the request
     * @return the object
     * @throws IOException the io exception
     */
    @PostMapping(value = {"/changes"})
    public void fileChanges(HttpServletRequest request) throws IOException {
        /**
         * === MimeHeaders ===
         * host = 8d0a-27-116-40-142.in.ngrok.io
         * user-agent = APIs-Google; (+https://developers.google.com/webmasters/APIs-Google.html)
         * content-length = 0
         * accept =
         * accept-encoding = gzip, deflate, br
         * x-forwarded-for = 66.102.8.195
         * x-forwarded-proto = https
         * x-goog-channel-expiration = Mon, 26 Sep 2022 12:24:41 GMT
         * x-goog-channel-id = user-channel-people/111647754396159229803
         * x-goog-channel-token = people/111647754396159229803-changes
         * x-goog-message-number = 1594405
         * x-goog-resource-id = EfMXrulmYZsODN_m0554_Rew9R4
         * x-goog-resource-state = change
         * x-goog-resource-uri = https://www.googleapis.com/drive/v3/changes?fields=files(id,name,thumbnailLink,mimeType),nextPageToken&includeCorpusRemovals=false&includeItemsFromAllDrives=false&includeRemoved=true&includeTeamDriveItems=false&oauth_token=my_token&pageSize=100&pageToken=25230&restrictToMyDrive=false&spaces=drive&supportsAllDrives=false&supportsTeamDrives=false&alt=json
         */
        String resourceState = request.getHeader(Constants.GOOGLE_RESOURCE_STATE_HEADER);
        String resourceId = request.getHeader(Constants.GOOGLE_RESOURCE_ID_HEADER);
        ResourceState fileState = ResourceState.valueOf(resourceState);
        boolean isDeleted = fileState.in(remove, trash);
        FileMeta fileMeta = fileMetaService.getFileById(resourceId).block();
        FileStorage fileStorage = fileStorageService.getFileStorageByFileId(resourceId).block();
        handleFileMeta(isDeleted, fileMeta);
        handleFileStorage(resourceState, fileState, isDeleted, fileMeta, fileStorage);
        logger.info(String.format("received message for file changes for user"));
    }

    /**
     * Handle file meta.
     *
     * @param isDeleted the is deleted
     * @param fileMeta  the file meta
     */
    private void handleFileMeta(boolean isDeleted, FileMeta fileMeta) {
        if (nonNull(fileMeta) && isDeleted) {
            fileMeta.setDeleted(true);
            fileMetaService.save(fileMeta);
        }
    }

    /**
     * Handle file storage.
     *
     * @param resourceState the resource state
     * @param fileState     the file state
     * @param isDeleted     the is deleted
     * @param fileMeta      the file meta
     * @param fileStorage   the file storage
     * @throws IOException the io exception
     */
    private void handleFileStorage(String resourceState, ResourceState fileState, boolean isDeleted, FileMeta fileMeta, FileStorage fileStorage) throws IOException {
        if (nonNull(fileStorage)) {
            fileStorage.setResourceState(resourceState);
            if (fileState.in(add)) {
                FileStorageInfo fileStorageInfo = fileStorageService.getFileStorageInfo(fileMeta.asMap());
                fileStorageService.uploadFile(fileStorageInfo);
                fileStorage = fileStorageInfo.getFile();
            }
            fileStorageService.save(fileStorage);
            if (isDeleted) {
                fileStorageService.deleteFileFromStorage(fileStorage);
            }
        }
    }
}
