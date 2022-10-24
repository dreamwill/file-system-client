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

package com.github.dreamwill.fsclient.impl;

import com.github.dreamwill.fsclient.FileMetadata;
import com.github.dreamwill.fsclient.FileSystemClient;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SftpClient implements FileSystemClient {
    private JSch jsch;
    private Session session;
    private ChannelSftp client;
    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    public SftpClient(@NonNull String host, @NonNull Integer port, @NonNull String username, @NonNull String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public void connect() throws IOException {
        jsch = new JSch();
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            client = (ChannelSftp) session.openChannel("sftp");
            client.connect();
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean createFile(@NonNull String path, @NonNull InputStream in) throws IOException {
        if (fileExists(path)) {
            return false;
        }
        String dirPath = FilenameUtils.getFullPathNoEndSeparator(path);
        try {
            createDirs(dirPath);
            client.put(in, path);
            return true;
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean deleteFile(@NonNull String path) throws IOException {
        try {
            if (fileExists(path)) {
                client.rm(path);
                return true;
            } else {
                return false;
            }
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean moveFile(@NonNull String source, @NonNull String target) throws IOException {
        if (validateBeforeMove(source, target)) {
            return false;
        }
        try {
            client.rename(source, target);
            return true;
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean copyFile(@NonNull String source, @NonNull String target) throws IOException {
        if (validateBeforeMove(source, target)) {
            return false;
        }
        File tempFile = new File(FileUtils.getTempDirectory(), FilenameUtils.getName(source));
        FileUtils.copyInputStreamToFile(getInputStream(source), tempFile);
        try (InputStream in = new FileInputStream(tempFile)) {
            if (!createFile(target, in)) {
                return false;
            }
        }
        Files.delete(tempFile.toPath());
        return true;
    }

    @Override
    public InputStream getInputStream(@NonNull String path) throws IOException {
        if (!fileExists(path)) {
            return null;
        }
        try {
            return client.get(path);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileMetadata getFileMetadata(@NonNull String path) throws IOException {
        if (!fileExists(path)) {
            return null;
        }
        SftpATTRS attrs;
        try {
            attrs = client.stat(path);
        } catch (SftpException e) {
            throw new IOException(e);
        }
        Optional<Instant> mtime = Optional.of(attrs)
                                          .map(SftpATTRS::getMTime)
                                          .map(Instant::ofEpochSecond);
        return FileMetadata.builder()
                           .path(path)
                           .size(attrs.getSize())
                           .ctime(Optional.empty())
                           .mtime(mtime)
                           .build();
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
        jsch = null;
        session = null;
        client = null;
    }

    private boolean fileExists(String path) throws IOException {
        try {
            client.stat(path);
            return true;
        } catch (SftpException e) {
            if (Objects.equals(e.getMessage(), "No such file")) {
                return false;
            }
            throw new IOException(e);
        }
    }

    private boolean dirExists(String path) throws IOException {
        try {
            client.cd(path);
            return true;
        } catch (SftpException e) {
            if (Objects.equals(e.getMessage(), "No such file")) {
                return false;
            } else {
                throw new IOException(e);
            }
        }
    }

    private void createDirs(String path) throws IOException {
        if (!dirExists(path)) {
            String parent = Paths.get(path).getParent().toString();
            createDirs(parent);
            try {
                client.mkdir(path);
            } catch (SftpException e) {
                throw new IOException(e);
            }
        }
    }

    private boolean validateBeforeMove(final String source, final String target) throws IOException {
        if (!fileExists(source)) {
            log.error("Source file {} does not exist.", source);
            return true;
        }
        if (fileExists(target)) {
            log.error("Target file {} already exists.", target);
            return true;
        }
        // make sure necessary dirs exist
        createDirs(FilenameUtils.getFullPath(target));
        return false;
    }
}
