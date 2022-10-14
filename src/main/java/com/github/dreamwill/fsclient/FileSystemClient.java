/*
 * Copyright 2022 许王伟
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * @param source source file path
     * @param target target file path
     * @return <code>true</code> if move successfully; <code>false</code> if target file exists
     * @throws IOException If an I/O error occurred
     */
    boolean moveFile(@NonNull String source, @NonNull String target) throws IOException;

    /**
     * Copy the file.
     *
     * @param source source file path
     * @param target target file path
     * @return <code>true</code> if copy successfully; <code>false</code> if target file exists
     * @throws IOException If an I/O error occurred
     */
    boolean copyFile(@NonNull String source, @NonNull String target) throws IOException;

    /**
     * Get an InputStream of the file denoted by this path.
     * <p>
     * Note: The input stream should be closed after use.
     *
     * @param path full file path
     * @return an input stream, or null if the file is not present
     * @throws IOException If an I/O error occurred
     */
    InputStream getInputStream(@NonNull String path) throws IOException;

    /**
     * Get metadata of the file denoted by this path.
     *
     * @param path full file path
     * @return metadata, or null if the file is not present
     * @throws IOException If an I/O error occurred
     */
    FileMetadata getFileMetadata(@NonNull String path) throws IOException;
}
