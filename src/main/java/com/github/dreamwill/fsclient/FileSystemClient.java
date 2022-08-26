package com.github.dreamwill.fsclient;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface FileSystemClient extends Closeable {
    void connect() throws IOException;

    boolean createFile(String path, InputStream in) throws IOException;

    boolean deleteFile(String path) throws IOException;

    boolean moveFile(String from, String to) throws IOException;

    boolean copyFile(String from, String to) throws IOException;

    InputStream getInputStream(String path) throws IOException;

    FileMetadata getFileMetadata(String path) throws IOException;
}
