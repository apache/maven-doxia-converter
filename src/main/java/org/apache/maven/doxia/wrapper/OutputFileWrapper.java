package org.apache.maven.doxia.wrapper;

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

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import org.apache.maven.doxia.DefaultConverter;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Wrapper for an output file.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class OutputFileWrapper
    extends AbstractFileWrapper
{
    /** serialVersionUID */
    static final long serialVersionUID = 804499615902780116L;

    private final DefaultConverter.DoxiaFormat format;

    /**
     * Private constructor.
     *
     * @param absolutePath not null
     * @param format not null
     * @param charsetName could be null
     * @param supportedFormat not null.
     * @throws IllegalArgumentException if any.
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     */
    private OutputFileWrapper( String absolutePath, DefaultConverter.DoxiaFormat format, String charsetName )
        throws UnsupportedEncodingException
    {
        super( absolutePath, charsetName );
        this.format = Objects.requireNonNull( format, "format is required" );
    }

    public DefaultConverter.DoxiaFormat getFormat()
    {
        return format;
    }

    /**
     * @param absolutePath not null
     * @param format not null
     * @return a type safe output writer
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     */
    public static OutputFileWrapper valueOf( String absolutePath, DefaultConverter.DoxiaFormat format )
        throws UnsupportedEncodingException
    {
        return valueOf( absolutePath, format, WriterFactory.UTF_8 );
    }

    /**
     * @param absolutePath not null
     * @param format not null
     * @param charsetName could be null
     * @return a type safe output writer
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     */
    public static OutputFileWrapper valueOf( String absolutePath, DefaultConverter.DoxiaFormat format,
            String charsetName )
        throws UnsupportedEncodingException
    {
        return new OutputFileWrapper( absolutePath, format, charsetName );
    }
}
