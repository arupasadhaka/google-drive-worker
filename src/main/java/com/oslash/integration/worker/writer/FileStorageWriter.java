package com.oslash.integration.worker.writer;

import com.oslash.integration.models.FileStorage;
import com.oslash.integration.service.FileStorageService;
import com.oslash.integration.worker.model.FileStorageInfo;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class FileStorageWriter implements ItemWriter<FileStorageInfo> {
    @Autowired
    FileStorageService fileStorageService;

    @Override
    public void write(List<? extends FileStorageInfo> list) throws Exception {
        for (FileStorageInfo input: list) {
            fileStorageService.upload(input);
            fileStorageService.save(input.getFile()).subscribe();
        }
    }
}
