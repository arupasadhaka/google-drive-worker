package com.oslash.integration.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
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

import java.util.List;


/**
 * The type File storage service.
 */
@Service
public class FileStorageService {
    /**
     * The Logger.
     */
    Logger logger = LoggerFactory.getLogger(getClass());

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
    public void upload(FileStorageInfo fileStorageInfo) {
        String userId = fileStorageInfo.getUserId();
        String fileName = fileStorageInfo.getFile().getFileName();
        AmazonS3 storageService = IntegrationResolver.resolveStorage();
        final String bucketName = (Constants.BUCKET_PREFIX + userId).replace("/", "");
        if (!storageService.doesBucketExist(bucketName)) {
            storageService.createBucket(bucketName);
            logger.info(String.format("created bucket with name %s for the user %s", bucketName, userId));
        }
        logger.info(String.format("saved file %s for user %s in bucket id %s", fileName, userId, bucketName));
        storageService.putObject(bucketName, fileName, fileStorageInfo.getFileStream(), new ObjectMetadata());
        // TODO get absolute URL from result with region
        fileStorageInfo.getFile().setSourceUrl(String.format("%s/%s", bucketName, fileName));
    }
}
