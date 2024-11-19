/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.doxia;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.doxia.DefaultConverter.MacroFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultConverterTest {

    @Test
    void testExecuteCommand() throws IOException, InterruptedException {
        DefaultConverter.executeCommand("git", "version");
    }

    @Test
    void testExecuteInvalidCommand() {
        assertThrows(IOException.class, () -> {
            DefaultConverter.executeCommand("invalid command");
        });
    }

    @Test
    void testMacroFormatter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", "value2");
        assertEquals("%{toc|param1=value1|param2=value2}", MacroFormatter.APT.format("toc", parameters));
        assertEquals("%{toc}", MacroFormatter.APT.format("toc", Collections.emptyMap()));
        assertEquals(
                "<!-- MACRO{toc|param1=value1|param2=value2} -->", MacroFormatter.MARKDOWN.format("toc", parameters));
        assertEquals("<!-- MACRO{toc} -->", MacroFormatter.MARKDOWN.format("toc", Collections.emptyMap()));
    }
}
