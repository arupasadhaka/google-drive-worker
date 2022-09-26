package com.oslash.integration.worker.writer;

import com.oslash.integration.models.FileMeta;
import com.oslash.integration.service.FileMetaService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type File meta writer.
 */
@Component
public class FileMetaWriter implements ItemWriter<FileMeta> {

    /**
     * The File meta service.
     */
    @Autowired
    FileMetaService fileMetaService;

    /**
     * Write.
     *
     * @param items the items
     */
    @Override
    public void write(List<? extends FileMeta> items) {
        fileMetaService.save((List<FileMeta>) items).subscribe();
    }
}
