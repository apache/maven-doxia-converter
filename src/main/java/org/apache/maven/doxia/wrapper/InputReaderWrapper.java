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

import java.io.Reader;

import org.apache.maven.doxia.DefaultConverter;

/**
 * Wrapper for an input reader.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class InputReaderWrapper {
    /** serialVersionUID */
    static final long serialVersionUID = 3260213754615748766L;

    private final Reader reader;

    private final DefaultConverter.DoxiaFormat format;

    /**
     * Private constructor.
     *
     * @param format not null
     * @param supportedFormat not null
     * @throws IllegalArgumentException if the format equals AUTO_FORMAT.
     */
    private InputReaderWrapper(Reader reader, DefaultConverter.DoxiaFormat format) {
        this.format = format;

        if (reader == null) {
            throw new IllegalArgumentException("input reader is required");
        }
        this.reader = reader;
    }

    /**
     * @return the reader
     */
    public Reader getReader() {
        return this.reader;
    }

    public DefaultConverter.DoxiaFormat getFormat() {
        return format;
    }
    /**
     * @param reader not null
     * @param format not null
     * @return a type safe input reader
     */
    public static InputReaderWrapper valueOf(Reader reader, DefaultConverter.DoxiaFormat format) {
        return new InputReaderWrapper(reader, format);
    }
}
