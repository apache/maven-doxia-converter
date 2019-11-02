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

import java.io.Serializable;
import java.util.Objects;

import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

/**
 * Abstract wrapper for Doxia converter.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
abstract class AbstractWrapper
    implements Serializable
{
    public static final String AUTO_FORMAT = "auto";

    private String format;

    private String[] supportedFormat;

    /**
     * @param format could be null.
     * @param supportedFormat not null.
     * @throws IllegalArgumentException if supportedFormat is null.
     */
    AbstractWrapper( String format, String[] supportedFormat )
    {
        this.format = ( isNotEmpty( format ) ? format : AUTO_FORMAT );
        if ( supportedFormat == null )
        {
            throw new IllegalArgumentException( "supportedFormat is required" );
        }
        this.supportedFormat = supportedFormat;
    }

    /**
     * @return the wanted format.
     */
    public String getFormat()
    {
        return this.format;
    }

    /**
     * @param format The wanted format.
     */
    void setFormat( String format )
    {
        this.format = format;
    }

    /**
     * @return the supportedFormat
     */
    public String[] getSupportedFormat()
    {
        return supportedFormat;
    }

    /**
     * @param supportedFormat the supportedFormat to set
     */
    void setSupportedFormat( String[] supportedFormat )
    {
        this.supportedFormat = supportedFormat;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }
        if ( other == null || getClass() != other.getClass() )
        {
            return false;
        }

        AbstractWrapper that = (AbstractWrapper) other;
        return Objects.equals( getFormat(), that.getFormat() );
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode()
    {
        return Objects.hash( getFormat() );
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "format = '" + getFormat() + "'";
    }
}
