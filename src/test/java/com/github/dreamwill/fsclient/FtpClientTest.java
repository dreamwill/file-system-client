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

import com.github.dreamwill.fsclient.impl.FtpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.IOException;

class FtpClientTest extends BaseClientTest {
    static Integer port;

    @BeforeAll
    @DisplayName("Build a virtual FTP server.")
    public static void prepareEnv() {
        FakeFtpServer fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("dreamwill", "123456", "/dreamwill"));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/dreamwill"));
        fileSystem.add(new FileEntry(already_exist, "abcdef 1234567890"));
        fileSystem.add(new FileEntry(to_be_delete, "abcdef 1234567890"));
        fileSystem.add(new FileEntry(copy_source, "abcdef 1234567890"));
        fileSystem.add(new FileEntry(move_source, "abcdef 1234567890"));
        fakeFtpServer.setFileSystem(fileSystem);

        // choose random port
        fakeFtpServer.setServerControlPort(0);

        fakeFtpServer.start();

        port = fakeFtpServer.getServerControlPort();
    }

    @BeforeEach
    public void setUp() throws IOException {
        client = new FtpClient("127.0.0.1", port, "dreamwill", "123456");
        client.connect();
    }

    @AfterEach
    public void tearDown() throws IOException {
        client.close();
    }
}
