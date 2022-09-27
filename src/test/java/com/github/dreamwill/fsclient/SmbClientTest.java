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

import com.github.dreamwill.fsclient.impl.SmbClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

public class SmbClientTest extends BaseClientTest {
    @BeforeEach
    public void setUp() throws IOException {
        client = new SmbClient("127.0.0.1", 44500, "dreamwill", "123456");
        client.connect();
    }

    @AfterEach
    public void tearDown() throws IOException {
        client.close();
    }
}
