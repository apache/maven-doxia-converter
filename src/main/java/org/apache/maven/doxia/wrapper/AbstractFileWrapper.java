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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Objects;

import org.codehaus.plexus.util.StringUtils;

import com.ibm.icu.text.CharsetDetector;

import static org.codehaus.plexus.util.StringUtils.isEmpty;
import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

/**
 * Abstract File wrapper for Doxia converter.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
abstract class AbstractFileWrapper
    extends AbstractWrapper
{
    public static final String AUTO_ENCODING = "auto";

    private File file;

    private String encoding;

    /**
     *
     * @param absolutePath not null
     * @param format could be null
     * @param encoding could be null
     * @param supportedFormat not null
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     * @throws IllegalArgumentException if any
     */
    AbstractFileWrapper( String absolutePath, String format, String encoding, String[] supportedFormat )
        throws UnsupportedEncodingException
    {
        super( format, supportedFormat );

        if ( isEmpty( absolutePath ) )
        {
            throw new IllegalArgumentException( "absolutePath is required" );
        }

        File filetoset = new File( absolutePath );
        if ( !filetoset.isAbsolute() )
        {
            filetoset = new File( new File( "" ).getAbsolutePath(), absolutePath );
        }
        this.file = filetoset;

        if ( isNotEmpty( encoding ) && !encoding.equalsIgnoreCase( encoding )
            && !Charset.isSupported( encoding ) )
        {
            throw new UnsupportedEncodingException( "The encoding '" + encoding
                    + "' is not a valid one. The supported charsets are: "
                    + StringUtils.join( CharsetDetector.getAllDetectableCharsets(), ", " ) );
        }
        this.encoding = ( isNotEmpty( encoding ) ? encoding : AUTO_ENCODING );
    }

    /**
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    /**
     * @param file new file.
     */
    void setFile( File file )
    {
        this.file = file;
    }

    /**
     * @return the encoding used for the file or <code>null</code> if not specified.
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @param encoding new encoding.
     */
    void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        if ( !super.equals( o ) )
        {
            return false;
        }
        AbstractFileWrapper that = (AbstractFileWrapper) o;
        return Objects.equals( getFile(), that.getFile() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( super.hashCode(), getFile() );
    }

    /** {@inheritDoc} */
    @Override
    public java.lang.String toString()
    {
        return super.toString() + "\n" + "file= '" + getFile() + "'";
    }
}
