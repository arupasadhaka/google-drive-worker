package com.oslash.integration.worker.writer;

import com.oslash.integration.service.FileMetaService;
import com.oslash.integration.models.FileMeta;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileMetaWriter implements ItemWriter<FileMeta> {

    @Autowired
    FileMetaService fileMetaService;

    @Override
    public void write(List<? extends FileMeta> items) {
        fileMetaService.save((List<FileMeta>) items).subscribe();
    }
}
