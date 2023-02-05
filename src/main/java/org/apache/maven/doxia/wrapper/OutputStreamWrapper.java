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

import java.io.OutputStream;
import java.util.Objects;

import org.apache.maven.doxia.DefaultConverter;

import static org.codehaus.plexus.util.StringUtils.isEmpty;

/**
 * Wrapper for an output stream.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class OutputStreamWrapper
{
    /** serialVersionUID */
    static final long serialVersionUID = 3329037527245430610L;

    private final OutputStream out;

    private final String encoding;

    private final DefaultConverter.DoxiaFormat format;

    /**
     * Private constructor.
     *
     * @param format not null
     * @param supportedFormat not null
     * @throws IllegalArgumentException if any.
     */
    private OutputStreamWrapper( OutputStream out, String format, String encoding )
    {
        this.format = DefaultConverter.DoxiaFormat.valueOf( format.toUpperCase() );
        this.out = out;
        this.encoding = encoding;
    }

    public DefaultConverter.DoxiaFormat getFormat()
    {
        return format;
    }

    /**
     * @return the output stream
     */
    public OutputStream getOutputStream()
    {
        return this.out;
    }

    /**
     * @return the encoding
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @param out not null
     * @param format not null
     * @param encoding not null
     * @return a type safe output stream wrapper
     */
    public static OutputStreamWrapper valueOf( OutputStream out, String format, String encoding )
    {
        Objects.requireNonNull( out, "output writer is required" );
        if ( isEmpty( format ) )
        {
            throw new IllegalArgumentException( "output format is required" );
        }

        return new OutputStreamWrapper( out, format, encoding );
    }
}
