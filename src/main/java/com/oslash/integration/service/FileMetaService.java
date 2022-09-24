package com.oslash.integration.service;


import com.oslash.integration.models.FileMeta;
import com.oslash.integration.repository.FileMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class FileMetaService {
    @Autowired
    FileMetaRepository fileMetaRepository;

    public Mono<FileMeta> save(FileMeta fileMeta) {
        return fileMetaRepository.save(fileMeta);
    }

    public Flux<FileMeta> save(List<FileMeta> fileMetas) {
        return fileMetaRepository.saveAll(fileMetas);
    }
}
