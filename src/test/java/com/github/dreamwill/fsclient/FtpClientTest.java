package com.github.dreamwill.fsclient;

import com.github.dreamwill.fsclient.impl.FtpClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class FtpClientTest {
    FileSystemClient client;

    @BeforeAll
    @DisplayName("Build a virtual FTP server.")
    public static void prepareEnv() {
        FakeFtpServer fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("tom", "123456", "/home/tom"));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/home/tom"));
        fileSystem.add(new FileEntry("/home/tom/file1.txt", "abcdef 1234567890"));
        fileSystem.add(new FileEntry("/home/tom/file2.txt", "abcdef 1234567890"));
        fileSystem.add(new FileEntry("/home/tom/file3.txt", "abcdef 1234567890"));
        fileSystem.add(new FileEntry("/home/tom/file4.txt", "abcdef 1234567890"));
        fakeFtpServer.setFileSystem(fileSystem);

        fakeFtpServer.start();
    }

    @BeforeEach
    public void setUp() throws IOException {
        client = new FtpClient("127.0.0.1", 21, "tom", "123456");
        client.connect();
    }

    @AfterEach
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    void should_create_a_file() throws IOException {
        try (InputStream in = new ByteArrayInputStream("abcdef 1234567890".getBytes(StandardCharsets.ISO_8859_1))) {
            boolean result = client.createFile("/home/tom/3.xlsx", in);
            Assertions.assertThat(result).isTrue();
        }
    }

    @Test
    void should_return_false_while_file_already_exists() throws IOException {
        try (InputStream in = new ByteArrayInputStream("abcdef 1234567890".getBytes(StandardCharsets.ISO_8859_1))) {
            boolean result = client.createFile("/home/tom/file1.txt", in);
            Assertions.assertThat(result).isFalse();
        }
    }

    @Test
    void should_delete_successfully() throws IOException {
        boolean result = client.deleteFile("/home/tom/file2.txt");
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void should_return_false_while_file_does_not_exist() throws IOException {
        boolean result = client.deleteFile("/home/tom/air.txt");
        Assertions.assertThat(result).isFalse();
    }

    @Test
    void should_move_file_successfully() throws IOException {
        boolean result = client.moveFile("/home/tom/file3.txt", "/home/tom/b/1.txt");
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void should_copy_file_successfully() throws IOException {
        boolean result = client.copyFile("/home/tom/file4.txt", "/home/tom/z/abc.txt");
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void should_get_input_stream() throws IOException {
        try (InputStream in = client.getInputStream("/home/tom/file1.txt")) {
            Assertions.assertThat(in).isNotNull();
        }
    }

    @Test
    void should_get_file_metadata() throws IOException {
        FileMetadata fileMetadata = client.getFileMetadata("/home/tom/file1.txt");
        Assertions.assertThat(fileMetadata).isNotNull();
    }
}
