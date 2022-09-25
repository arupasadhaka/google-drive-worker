package com.oslash.integration.worker.writer;

import com.oslash.integration.models.FileStorage;
import com.oslash.integration.service.FileStorageService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileStorageWriter implements ItemWriter<FileStorage> {

    @Autowired
    FileStorageService fileStorageService;

    @Override
    public void write(List<? extends FileStorage> items) {
        fileStorageService.save((List<FileStorage>) items).subscribe();
    }
}
