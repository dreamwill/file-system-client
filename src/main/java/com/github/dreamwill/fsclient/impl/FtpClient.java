package com.github.dreamwill.fsclient.impl;

import com.github.dreamwill.fsclient.FileMetadata;
import com.github.dreamwill.fsclient.FileSystemClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RFC 959 defines the File Transfer Protocol (FTP), and it is the only INTERNET STANDARD about FTP.
 * RFC 2228, RFC 2640, RFC 2773, RFC 3659, RFC 5797, RFC 7151 are extensions to FTP, and they are PROPOSED STANDARD except RFC 2773 (EXPERIMENTAL).
 * So some FTP servers do not support UTF-8 (proposed in RFC 2640) and others features.
 */
@Slf4j
public class FtpClient implements FileSystemClient {
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("(/[^/\\f\\n\\r\\t\\v\\\\]+)+");
    private FTPClient client;
    private String host;
    private Integer port;
    private String username;
    private String password;

    public FtpClient(@NonNull String host, @NonNull Integer port, @NonNull String username, @NonNull String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        client = new FTPClient();
    }

    @Override
    public void connect() throws IOException {
        // Detect whether the FTP server supports UTF8 or not.
        client.setAutodetectUTF8(true);

        client.connect(host, port);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            log.error("Fail to connect to the FTP server, FTP server ip: {}, FTP server port: {}.", host, port);
            throw new IOException();
        }
        log.info("Successfully connect to the FTP server, FTP server ip: {}, FTP server port: {}.", host, port);

        log.debug("Client will use {} charset to communicate with server.", client.getControlEncoding());

        if (!client.login(username, password)) {
            log.error("Fail to login to the FTP server.");
            throw new IOException();
        }
        log.info("Successfully login to the FTP server.");

        if (!client.setFileType(FTPClient.BINARY_FILE_TYPE)) {
            log.error("Fail to set file type to binary.");
            throw new IOException();
        }
        client.enterLocalPassiveMode();
    }

    @Override
    public boolean createFile(@NonNull String path, @NonNull InputStream in) throws IOException {
        validateFilePath(path);
        String dir = FilenameUtils.getFullPath(path);
        createDirs(dir);
        return client.storeFile(path, in);
    }

    @Override
    public boolean deleteFile(@NonNull String path) throws IOException {
        validateFilePath(path);
        if (fileExists(path)) {
            return client.deleteFile(path);
        } else {
            return false;
        }
    }

    @Override
    public boolean moveFile(@NonNull String from, @NonNull String to) throws IOException {
        validateFilePath(from);
        validateFilePath(to);
        if (!fileExists(from)) {
            log.error("Source file {} does not exist.", from);
            return false;
        }
        if (fileExists(to)) {
            log.info("Target file {} already exists. Prepare to delete it.", to);
            if (!deleteFile(to)) {
                return false;
            }
        }
        return client.rename(from, to);
    }

    @Override
    public boolean copyFile(@NonNull String from, @NonNull String to) throws IOException {
        validateFilePath(from);
        validateFilePath(to);
        if (!fileExists(from)) {
            log.error("Source file {} does not exist.", from);
            return false;
        }
        if (fileExists(to)) {
            log.info("Target file {} already exists. Prepare to delete it.", to);
            if (!deleteFile(to)) {
                return false;
            }
        }
        File tempFile = new File(FileUtils.getTempDirectory(), FilenameUtils.getName(from));
        FileUtils.copyInputStreamToFile(getInputStream(from), tempFile);
        try (InputStream in = new FileInputStream(tempFile)) {
            if (!createFile(to, in)) {
                return false;
            }
        }
        Files.delete(tempFile.toPath());
        return true;
    }

    @Override
    public InputStream getInputStream(@NonNull String path) throws IOException {
        if (!fileExists(path)) {
            throw new IOException();
        }
        return client.retrieveFileStream(path);
    }

    @Override
    public FileMetadata getFileMetadata(@NonNull String path) throws IOException {
        validateFilePath(path);
        String dir = FilenameUtils.getFullPath(path);
        if (!client.changeWorkingDirectory(dir)) {
            return null;
        }
        String name = FilenameUtils.getName(path);
        FTPFile[] ftpFiles = client.listFiles();
        FTPFile ftpFile = Arrays.stream(ftpFiles)
                .filter(Objects::nonNull)
                .filter(FTPFile::isFile)
                .filter(file -> file.getName().equals(name))
                .findAny()
                .orElse(null);
        if (ftpFile == null) {
            return null;
        }
        Optional<Instant> mtime = Optional.of(ftpFile)
                .map(FTPFile::getTimestamp)
                .map(Calendar::getTimeInMillis)
                .map(Instant::ofEpochMilli);
        FileMetadata fileMetadata = FileMetadata.builder()
                .path(path)
                .size(ftpFile.getSize())
                .mtime(mtime)
                .ctime(mtime)
                .build();
        return fileMetadata;
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            try {
                client.disconnect();
            } catch (IOException e) {
                // ignore
            } finally {
                client = null;
            }
        }
    }

    private void reconnectIfRequired() throws IOException {
        String pwd = null;
        try {
            pwd = client.printWorkingDirectory();
        } catch (IOException e) {
            // ignore
        }
        if (pwd == null) {
            connect();
        }
    }

    private boolean fileExists(String path) throws IOException {
        validateFilePath(path);
        String dir = FilenameUtils.getFullPath(path);
        if (client.changeWorkingDirectory(dir)) {
            String name = FilenameUtils.getName(path);
            FTPFile[] ftpFiles = client.listFiles();
            return Arrays.stream(ftpFiles)
                    .filter(Objects::nonNull)
                    .filter(FTPFile::isFile)
                    .anyMatch(file -> file.getName().equals(name));
        }
        return false;
    }

    public boolean dirExists(String path) throws IOException {
        return client.changeWorkingDirectory(path);
    }

    private boolean createDirs(String path) throws IOException {
        if (dirExists(path)) {
            return true;
        } else {
            String parent = Paths.get(path).getParent().toString();
            createDirs(parent);
            return client.makeDirectory(path);
        }
    }

    private void validateFilePath(String path) {
        if (isIllegalFilePath(path)) {
            log.warn("Illegal file path: {}.", path);
        }
    }

    private boolean isIllegalFilePath(String path) {
        Matcher m = FILE_PATH_PATTERN.matcher(path);
        return !m.matches();
    }
}
