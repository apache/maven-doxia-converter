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

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.apache.maven.doxia.DefaultConverter;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Wrapper for an input file.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class InputFileWrapper extends AbstractFileWrapper {
    /** serialVersionUID */
    static final long serialVersionUID = 6510443036267371188L;

    private final DefaultConverter.DoxiaFormat format;

    /**
     * Private constructor.
     *
     * @param absolutePath not null
     * @param format not null
     * @param charsetName could be null
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     * @throws FileNotFoundException if the file for absolutePath is not found.
     */
    private InputFileWrapper(String absolutePath, DefaultConverter.DoxiaFormat format, String charsetName)
            throws UnsupportedEncodingException, FileNotFoundException {
        super(absolutePath, charsetName);

        this.format = format;
        if (!getFile().exists()) {
            throw new FileNotFoundException("The file '" + getFile().getAbsolutePath() + "' doesn't exist.");
        }
    }

    /**
     * @param absolutePath for a file or a directory not null.
     * @param format not null
     * @return a type safe input reader
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     * @throws FileNotFoundException if the file for absolutePath is not found.
     * @see #valueOf(String, DefaultConverter.DoxiaFormat, String) using WriterFactory.UTF_8
     */
    public static InputFileWrapper valueOf(String absolutePath, DefaultConverter.DoxiaFormat format)
            throws UnsupportedEncodingException, FileNotFoundException {
        return valueOf(absolutePath, format, WriterFactory.UTF_8);
    }

    /**
     * @param absolutePath for a wanted file or a wanted directory, not null.
     * @param format not null
     * @param charsetName could be null
     * @return a type safe input reader
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     * @throws FileNotFoundException if the file for absolutePath is not found.
     */
    public static InputFileWrapper valueOf(String absolutePath, DefaultConverter.DoxiaFormat format, String charsetName)
            throws UnsupportedEncodingException, FileNotFoundException {
        return new InputFileWrapper(absolutePath, format, charsetName);
    }

    public DefaultConverter.DoxiaFormat getFormat() {
        return format;
    }
}
