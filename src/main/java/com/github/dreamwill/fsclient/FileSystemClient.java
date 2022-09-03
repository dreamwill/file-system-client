package com.github.dreamwill.fsclient;

import lombok.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface FileSystemClient extends Closeable {
    /**
     * Connect to the remote file system. This method must be called at first.
     *
     * @throws IOException If the remote file system could not be connected.
     */
    void connect() throws IOException;

    /**
     * Create a new file and copy bytes from the input stream to it.
     *
     * @param path full file path of the new file
     * @param in   the input stream to copy from
     * @return <code>true</code> if create and copy successfully; <code>false</code> if this file already exists
     * @throws IOException If an I/O error occurred
     */
    boolean createFile(@NonNull String path, @NonNull InputStream in) throws IOException;

    /**
     * Delete the file denoted by this path.
     *
     * @param path full file path
     * @return <code>true</code> if delete successfully; <code>false</code> if the file does not exist
     * @throws IOException If an I/O error occurred
     */
    boolean deleteFile(@NonNull String path) throws IOException;

    /**
     * Move the file.
     *
     * @param from source file path
     * @param to   target file path
     * @return <code>true</code> if move successfully; <code>false</code> otherwise
     * @throws IOException If an I/O error occurred
     */
    boolean moveFile(@NonNull String from, @NonNull String to) throws IOException;

    /**
     * Copy the file.
     *
     * @param from source file path
     * @param to   target file path
     * @return <code>true</code> if copy successfully; <code>false</code> otherwise
     * @throws IOException If an I/O error occurred
     */
    boolean copyFile(@NonNull String from, @NonNull String to) throws IOException;

    /**
     * Get an InputStream of the file denoted by this path.
     * <p>
     * Note: The input stream should be closed after use.
     *
     * @param path full file path
     * @return an input stream
     * @throws IOException If an I/O error occurred
     */
    InputStream getInputStream(@NonNull String path) throws IOException;

    /**
     * Get metadata of the file denoted by this path.
     *
     * @param path full file path
     * @return metadata
     * @throws IOException If an I/O error occurred
     */
    FileMetadata getFileMetadata(@NonNull String path) throws IOException;
}
