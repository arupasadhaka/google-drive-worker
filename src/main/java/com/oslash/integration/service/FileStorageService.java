package com.oslash.integration.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.models.FileStorage;
import com.oslash.integration.repository.FileStorageRepository;
import com.oslash.integration.resolver.IntegrationResolver;
import com.oslash.integration.utils.Constants;
import com.oslash.integration.worker.model.FileStorageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


/**
 * The type File storage service.
 */
@Service
public class FileStorageService {

    @Autowired
    AppConfiguration appConfiguration;

    /**
     * The Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    /**
     * The File storage repository.
     */
    @Autowired
    FileStorageRepository fileStorageRepository;

    /**
     * Save flux.
     *
     * @param fileStorage the file storage
     * @return the flux
     */
    public Flux<FileStorage> save(List<FileStorage> fileStorage) {
        return fileStorageRepository.saveAll(fileStorage);
    }

    /**
     * Save mono.
     *
     * @param fileStorage the file storage
     * @return the mono
     */
    public Mono<FileStorage> save(FileStorage fileStorage) {
        return fileStorageRepository.save(fileStorage);
    }

    /**
     * Upload.
     *
     * @param fileStorageInfo the file storage info
     */
    public void uploadFile(FileStorageInfo fileStorageInfo) {
        String userId = fileStorageInfo.getUserId();
        String fileName = fileStorageInfo.getFileStorage().getFileName();
        AmazonS3 storageService = IntegrationResolver.resolveStorage();
        final String bucketName = getBucketName(userId);
        if (!storageService.doesBucketExistV2(bucketName)) {
            storageService.createBucket(bucketName);
            logger.info(String.format("created bucket with name %s for the user %s", bucketName, userId));
        }
        logger.info(String.format("saved file %s for user %s in bucket id %s", fileName, userId, bucketName));
        ObjectMetadata objectMeta = new ObjectMetadata();
        if (fileStorageInfo.getFile().getSize() != null) {
            objectMeta.setContentLength(fileStorageInfo.getFile().getSize());
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, fileStorageInfo.getFileStream(), objectMeta);
        /**
         * need to add when size cannot be determined
         */
        if (objectMeta.getContentLength() == 0l) {
            putObjectRequest.getRequestClientOptions().setReadLimit(appConfiguration.getBufferSize());
        }
        storageService.putObject(putObjectRequest);
        // TODO get absolute URL from result with region
        fileStorageInfo.getFileStorage().setSourceUrl(String.format("%s/%s", bucketName, fileName));
    }

    /**
     * Gets bucket name.
     *
     * @param userId the user id
     * @return the bucket name
     */
    private static String getBucketName(String userId) {
        return (Constants.BUCKET_PREFIX + userId).replace("/", "");
    }

    /**
     * Delete file from storage.
     *
     * @param fileStorage the file storage
     */
    public void deleteFileFromStorage(FileStorage fileStorage) {
        String userId = fileStorage.getUserId();
        String fileName = fileStorage.getFileName();
        AmazonS3 storageService = IntegrationResolver.resolveStorage();
        final String bucketName = getBucketName(userId);
        if (!storageService.doesBucketExistV2(bucketName)) {
            logger.info(String.format("bucket and file not found with bucket name %s and file name %s", bucketName, fileName));
            return;
        }
        logger.info(String.format("saved file %s for user %s in bucket id %s", fileName, userId, bucketName));
        storageService.deleteObject(bucketName, fileName);
    }

    /**
     * Gets file storage by file id.
     *
     * @param resourceId the resource id
     * @return the file storage by file id
     */
    public Mono<FileStorage> getFileStorageByFileId(String resourceId) {
        return fileStorageRepository.findDistinctByFileId(resourceId);
    }

    /**
     * Gets file storage info.
     *
     * @param item the item
     * @return the file storage info
     * @throws IOException the io exception
     */
    public FileStorageInfo getFileStorageInfo(Map item) throws IOException {
        final FileStorage fileStorage = new FileStorage.Builder().file(item).build();
        logger.info("Processing file meta to download file " + fileStorage.getFileId());
        final Drive drive = IntegrationResolver.resolveGDrive(fileStorage.getUserId());
        File file = drive.files().get(fileStorage.getFileId()).execute();
        final InputStream fileStream = drive.files().get(file.getId()).executeMediaAsInputStream();
        return new FileStorageInfo.Builder().fileStream(fileStream).fileStorage(fileStorage).file(file).userId(fileStorage.getUserId()).build();
    }
}
