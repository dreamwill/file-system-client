package com.github.dreamwill.fsclient;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FileMetadata {
    /**
     * create time
     */
    private Instant ctime;

    /**
     * modify time
     */
    private Instant mtime;

    /**
     * file size
     */
    private Long size;

    /**
     * full file path
     */
    private String path;
}
