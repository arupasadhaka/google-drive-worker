package com.oslash.integration.service;

import com.oslash.integration.models.FileStorage;
import com.oslash.integration.repository.FileStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class FileStorageService {
    @Autowired
    FileStorageRepository fileStorageRepository;

    public Flux<FileStorage> save(List<FileStorage> fileStorage) {
        return fileStorageRepository.saveAll(fileStorage);
    }

}
