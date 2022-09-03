package com.github.dreamwill.fsclient;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

@Getter
@Builder
public class FileMetadata {
    /**
     * create time
     */
    private Optional<Instant> ctime;

    /**
     * modify time
     */
    private Optional<Instant> mtime;

    /**
     * file size
     */
    private Long size;

    /**
     * full file path
     */
    private String path;
}
