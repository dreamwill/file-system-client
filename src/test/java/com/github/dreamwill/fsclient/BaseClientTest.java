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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseClientTest {
    public static final String never_exist_dir = "/dreamwill/never_exist/never_exist.doc";
    public static final String never_exist = "/dreamwill/never_exist.doc";
    public static final String already_exist = "/dreamwill/already_exist.txt";
    public static final String to_be_creating = "/dreamwill/to_be_creating.xlsx";
    public static final String to_be_delete = "/dreamwill/to_be_delete.txt";
    public static final String copy_source = "/dreamwill/copy_from/source.txt";
    public static final String copy_target = "/dreamwill/copy_to/target.txt";
    public static final String move_source = "/dreamwill/move_from/source.txt";
    public static final String move_target = "/dreamwill/move_to/target.txt";
    protected FileSystemClient client;

    @Test
    void should_create_a_file() throws IOException {
        try (InputStream in = new ByteArrayInputStream("abcdef 1234567890".getBytes(StandardCharsets.US_ASCII))) {
            boolean result = client.createFile(to_be_creating, in);
            Assertions.assertThat(result).isTrue();
        }
    }

    @Test
    void should_throw_null_pointer_exception_while_create_file_with_any_null_parameter() {
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.createFile(null, null));

        ByteArrayInputStream in = new ByteArrayInputStream("abcdef 1234567890".getBytes(StandardCharsets.US_ASCII));
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.createFile(null, in));

        Assertions.assertThatNullPointerException().isThrownBy(() -> client.createFile(to_be_creating, null));
    }

    @Test
    void should_return_false_while_file_already_exists() throws IOException {
        try (InputStream in = new ByteArrayInputStream("abcdef 1234567890".getBytes(StandardCharsets.US_ASCII))) {
            boolean result = client.createFile(already_exist, in);
            Assertions.assertThat(result).isFalse();
        }
    }

    @Test
    void should_delete_successfully() throws IOException {
        boolean result = client.deleteFile(to_be_delete);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void should_throw_null_pointer_exception_while_delete_file_with_null_path() {
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.deleteFile(null));
    }

    @Test
    void should_return_false_while_file_does_not_exist() throws IOException {
        boolean result = client.deleteFile(never_exist);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    void should_move_file_successfully() throws IOException {
        boolean result = client.moveFile(move_source, move_target);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void should_return_false_while_move_file_and_source_file_does_not_exist() throws IOException {
        boolean result = client.moveFile(never_exist, move_target);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    void should_throw_null_pointer_exception_while_move_file_with_any_null_parameter() {
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.moveFile(null, null));
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.moveFile(move_source, null));
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.moveFile(null, move_target));
    }

    @Test
    void should_copy_file_successfully() throws IOException {
        boolean result = client.copyFile(copy_source, copy_target);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void should_return_false_while_copy_file_and_source_file_does_not_exist() throws IOException {
        boolean result = client.copyFile(never_exist, copy_target);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    void should_throw_null_pointer_exception_while_copy_file_with_any_null_parameter() {
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.copyFile(null, null));
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.copyFile(copy_source, null));
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.copyFile(null, copy_target));
    }

    @Test
    void should_get_input_stream() throws IOException {
        try (InputStream in = client.getInputStream(already_exist)) {
            Assertions.assertThat(in).isNotNull();
        }
    }

    @Test
    void should_throw_null_pointer_exception_while_get_input_stream_with_null_path() {
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.getInputStream(null));
    }

    @Test
    void should_throw_io_exception_while_get_input_stream_with_non_existent_file() {
        Assertions.assertThatIOException().isThrownBy(() -> client.getInputStream(never_exist));
    }

    @Test
    void should_get_file_metadata() throws IOException {
        FileMetadata fileMetadata = client.getFileMetadata(already_exist);
        Assertions.assertThat(fileMetadata).isNotNull();
    }

    @Test
    void should_get_file_metadata_unsuccessfully_while_directory_does_not_exist() throws IOException {
        FileMetadata fileMetadata = client.getFileMetadata(never_exist_dir);
        Assertions.assertThat(fileMetadata).isNull();
    }

    @Test
    void should_get_file_metadata_unsuccessfully_while_file_does_not_exist() throws IOException {
        FileMetadata fileMetadata = client.getFileMetadata(never_exist);
        Assertions.assertThat(fileMetadata).isNull();
    }

    @Test
    void should_throw_null_pointer_exception_while_null_path() throws IOException {
        Assertions.assertThatNullPointerException().isThrownBy(() -> client.getFileMetadata(null));
    }
}
