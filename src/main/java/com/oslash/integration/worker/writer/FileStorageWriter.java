package com.oslash.integration.worker.writer;

import com.oslash.integration.service.FileStorageService;
import com.oslash.integration.worker.model.FileStorageInfo;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type File storage writer.
 */
@Component
public class FileStorageWriter implements ItemWriter<FileStorageInfo> {
    /**
     * The File storage service.
     */
    @Autowired
    FileStorageService fileStorageService;

    /**
     * Write.
     *
     * @param list the list
     * @throws Exception the exception
     */
    @Override
    public void write(List<? extends FileStorageInfo> list) throws Exception {
        for (FileStorageInfo input : list) {
            fileStorageService.upload(input);
            fileStorageService.save(input.getFile()).subscribe();
        }
    }
}
