package com.mappy.fpm.batches.tomtom.download.json;

import com.google.inject.Inject;
import com.mappy.fpm.batches.tomtom.download.json.model.Contents.Content;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

@Slf4j
public class ArchiveDownloader {

    private final File outputFolder;
    private final HttpClient client;
    private final String token;

    @Inject
    public ArchiveDownloader(@Named("outputFolder") File outputFolder, HttpClient client, @Named("token") String token) {
        this.outputFolder = outputFolder;
        this.client = client;
        this.token = token;
    }

    public void download(Content content) {
        File downloaded = new File(outputFolder, content.getName());
        for (int i = 0; i < 3; i++) {
            try {
                log.info("Downloading {} to \"{}\"", content.getLocation(), downloaded.getAbsolutePath());

                HttpGet get = new HttpGet(content.getLocation());
                get.addHeader("Authorization", token);
                HttpResponse response = client.execute(get);
                try (InputStream archiveStream = response.getEntity().getContent()) {
                    copyInputStreamToFile(archiveStream, downloaded);
                }
                return;
            }
            catch (IOException ex) {
                log.error("Retrying.. ", ex);
            }
        }
        throw new RuntimeException("Too many retry");
    }
}