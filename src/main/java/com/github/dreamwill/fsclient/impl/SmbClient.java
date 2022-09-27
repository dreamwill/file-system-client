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
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileBasicInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.utils.SmbFiles;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class SmbClient implements FileSystemClient {
    private SMBClient client;
    private Session session;
    private String host;
    private Integer port;
    private String username;
    private String password;

    public SmbClient(@NonNull String host, @NonNull Integer port, @NonNull String username, @NonNull String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public void connect() throws IOException {
        client = new SMBClient();
        Connection connection = client.connect(host, port);
        AuthenticationContext ac;
        if (StringUtils.isNotBlank(username)) {
            ac = new AuthenticationContext(username, password.toCharArray(), host);
        } else {
            ac = AuthenticationContext.anonymous();
        }
        session = connection.authenticate(ac);
    }

    @Override
    public boolean createFile(@NonNull String path, @NonNull InputStream in) throws IOException {
        if (fileExists(path)) {
            return false;
        }
        String dir = FilenameUtils.getFullPathNoEndSeparator(path);
        createDirs(dir);

        try (DiskShare diskShare = getDiskShare(path)) {
            String filePath = cutShareName(path, diskShare);
            Set<AccessMask> accessMask = new HashSet<>();
            accessMask.add(AccessMask.GENERIC_WRITE);
            Set<FileAttributes> attributes = new HashSet<>();
            attributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
            Set<SMB2CreateOptions> createOptions = new HashSet<>();
            createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);

            try (
                    com.hierynomus.smbj.share.File file = diskShare.openFile(
                            filePath,
                            accessMask,
                            attributes,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            createOptions
                    ); OutputStream os = file.getOutputStream()
            ) {
                IOUtils.copyLarge(in, os);
            }
            return true;
        }
    }

    @Override
    public boolean deleteFile(@NonNull String path) throws IOException {
        if (fileExists(path)) {
            try (DiskShare diskShare = getDiskShare(path)) {
                String filePath = cutShareName(path, diskShare);
                diskShare.rm(filePath);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean moveFile(@NonNull String from, @NonNull String to) throws IOException {
        if (!fileExists(from)) {
            log.error("Source file {} does not exist.", from);
            return false;
        }
        if (fileExists(to)) {
            log.info("Target file {} already exists. Prepare to delete it.", to);
            if (!deleteFile(to)) {
                return false;
            }
        } else {
            // make sure necessary dirs exist
            createDirs(FilenameUtils.getFullPathNoEndSeparator(to));
        }
        DiskShare diskShare = getDiskShare(from);
        String filePath = cutShareName(from, diskShare);
        try (
                com.hierynomus.smbj.share.File file = diskShare.openFile(
                        filePath,
                        EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        null
                )
        ) {
            String newName = cutShareName(to, getDiskShare(to)).replace("/", "\\");
            file.rename(newName, true);
        }
        return true;
    }

    @Override
    public boolean copyFile(@NonNull String from, @NonNull String to) throws IOException {
        if (!fileExists(from)) {
            log.error("Source file {} does not exist.", from);
            return false;
        }
        if (fileExists(to)) {
            log.info("Target file {} already exists. Prepare to delete it.", to);
            if (!deleteFile(to)) {
                return false;
            }
        } else {
            // make sure necessary dirs exist
            createDirs(FilenameUtils.getFullPathNoEndSeparator(to));
        }
        DiskShare sourceDiskShare = getDiskShare(from);
        DiskShare targetDiskShare = getDiskShare(to);
        try (
                com.hierynomus.smbj.share.File sourceFile = sourceDiskShare.openFile(
                        cutShareName(from, sourceDiskShare),
                        EnumSet.of(AccessMask.FILE_READ_DATA),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        null
                );
                com.hierynomus.smbj.share.File targetFile = targetDiskShare.openFile(
                        cutShareName(to, targetDiskShare),
                        EnumSet.of(AccessMask.FILE_WRITE_DATA),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OVERWRITE_IF,
                        EnumSet.of(SMB2CreateOptions.FILE_RANDOM_ACCESS)
                )
        ) {
            sourceFile.remoteCopyTo(targetFile);
            return true;
        } catch (Buffer.BufferException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream getInputStream(@NonNull String path) throws IOException {
        if (!fileExists(path)) {
            throw new IOException();
        }
        DiskShare diskShare = getDiskShare(path);
        String filePath = cutShareName(path, diskShare);
        com.hierynomus.smbj.share.File remoteFile = diskShare.openFile(
                filePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
        );
        return remoteFile.getInputStream();
    }

    @Override
    public FileMetadata getFileMetadata(@NonNull String path) throws IOException {
        if (!fileExists(path)) {
            return null;
        }
        DiskShare diskShare = getDiskShare(path);
        String filePath = cutShareName(path, diskShare);
        FileAllInformation fileAllInformation;
        try (
                com.hierynomus.smbj.share.File remoteFile = diskShare.openFile(
                        filePath,
                        EnumSet.of(AccessMask.GENERIC_READ),
                        null,
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        null
                )
        ) {
            fileAllInformation = remoteFile.getFileInformation();
        }
        Optional<Instant> mtime = Optional.of(fileAllInformation)
                                          .map(FileAllInformation::getBasicInformation)
                                          .map(FileBasicInformation::getLastWriteTime)
                                          .map(FileTime::toEpochMillis)
                                          .map(Instant::ofEpochMilli);
        Optional<Instant> ctime = Optional.of(fileAllInformation)
                                          .map(FileAllInformation::getBasicInformation)
                                          .map(FileBasicInformation::getCreationTime)
                                          .map(FileTime::toEpochMillis)
                                          .map(Instant::ofEpochMilli);
        return FileMetadata.builder()
                           .path(path)
                           .size(fileAllInformation.getStandardInformation().getEndOfFile())
                           .mtime(mtime)
                           .ctime(ctime)
                           .build();
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private DiskShare getDiskShare(String path) {
        String shareName = Arrays.stream(path.split("/"))
                                 .filter(StringUtils::isNotBlank)
                                 .findFirst()
                                 .orElseThrow(IllegalArgumentException::new);
        return (DiskShare) session.connectShare(shareName);
    }

    private void createDirs(String path) throws IOException {
        try (DiskShare diskShare = getDiskShare(path)) {
            new SmbFiles().mkdirs(diskShare, cutShareName(path, diskShare));
        }
    }

    private boolean fileExists(String path) throws IOException {
        try (DiskShare diskShare = getDiskShare(path)) {
            if (diskShare.folderExists(cutShareName(FilenameUtils.getFullPathNoEndSeparator(path), diskShare))) {
                String filePath = cutShareName(path, diskShare);
                return diskShare.fileExists(filePath);
            } else {
                return false;
            }
        }
    }

    private static String cutShareName(String path, DiskShare diskShare) {
        String shareName = diskShare.getSmbPath().getShareName();
        return path.substring(shareName.length() + 1);
    }
}
