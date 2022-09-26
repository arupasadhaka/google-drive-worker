package com.oslash.integration.service;


import com.oslash.integration.models.FileMeta;
import com.oslash.integration.repository.FileMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * The type File meta service.
 */
@Service
public class FileMetaService {
    /**
     * The File meta repository.
     */
    @Autowired
    FileMetaRepository fileMetaRepository;

    /**
     * Save mono.
     *
     * @param fileMeta the file meta
     * @return the mono
     */
    public Mono<FileMeta> save(FileMeta fileMeta) {
        return fileMetaRepository.save(fileMeta);
    }

    /**
     * Save flux.
     *
     * @param fileMetas the file metas
     * @return the flux
     */
    public Flux<FileMeta> save(List<FileMeta> fileMetas) {
        return fileMetaRepository.saveAll(fileMetas);
    }

    public Mono<FileMeta> getFileById(String resourceId) {
        return fileMetaRepository.findById(resourceId);
    }
}
