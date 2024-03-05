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
package org.apache.maven.doxia.wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.impl.SinkWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preserves all Velocity (VTL) statements, which are
 * <ul>
 *  <li>comments</li>
 *  <li>references and</li>
 *  <li>directives</li>
 * </ul>
 * by emitting them in dedicated {@link Sink#rawText(String)} statements.
 * @see <a href="https://velocity.apache.org/engine/devel/user-guide.html">Velocity - User Guide</a>
 */
public class PreserveVelocityStatements extends SinkWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreserveVelocityStatements.class);

    /**
     * Pattern for VTL references
     * @see <a href="https://velocity.apache.org/engine/devel/user-guide.html#references">Reference</a>
     */
    static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$([a-zA-Z](\\w)*(\\.\\w*)*)|(\\{[^\\}]*\\})");

    // this approach does not work due to macros and directives not always at the beginning of a line
    static final Set<String> VALID_DIRECTIVES = new HashSet<>(Arrays.asList(
            "#set",
            "#if",
            "#else",
            "#end",
            "#foreach",
            "#include",
            "#parse",
            "#break",
            "#stop",
            "#evaluate",
            "#define",
            "#macro"));

    public PreserveVelocityStatements(Sink delegate) {
        super(delegate);
    }

    @Override
    public void text(String text, SinkEventAttributes attributes) {
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (!isFirstLine) {
                    super.text(EOL);
                } else {
                    isFirstLine = false;
                }
                processLine(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void processLine(String line) {
        String trimmedText = line.trim();
        if (trimmedText.startsWith("##")) {
            LOGGER.debug("Found line {} being a VTL comment, emitting as raw text", line);
            rawText(line);
        } else if (trimmedText.startsWith("#") && VALID_DIRECTIVES.stream().anyMatch(trimmedText::startsWith)) {
            LOGGER.debug("Found line {} being a VTL directive, emitting as raw text", line);
            rawText(line);
        } else {
            Matcher matcher = REFERENCE_PATTERN.matcher(line);
            int start = 0;
            while (matcher.find()) {
                if (start < matcher.start()) {
                    super.text(line.substring(start, matcher.start()));
                }
                String reference = line.substring(matcher.start(), matcher.end());
                LOGGER.debug("Found VTL reference {} in line {}, emitting as raw text", reference, line);
                rawText(reference);
                start = matcher.end();
            }
            if (start == 0) {
                super.text(line);
            } else {
                if (start < line.length() - 1) {
                    super.text(line.substring(start));
                }
            }
        }
    }
}
