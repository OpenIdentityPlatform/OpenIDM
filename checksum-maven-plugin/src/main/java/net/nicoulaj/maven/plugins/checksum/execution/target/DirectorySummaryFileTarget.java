/*
 * Copyright 2010-2012 Julien Nicoulaud <julien.nicoulaud@gmail.com>
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
package net.nicoulaj.maven.plugins.checksum.execution.target;

import net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} that writes digests to separate
 * files per directory.
 *
 * @since 1.3
 */
public class DirectorySummaryFileTarget
    implements ExecutionTarget
{
    /**
     * The line separator character.
     */
    public static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    /**
     * The CSV column separator character.
     */
    public static final String CSV_COLUMN_SEPARATOR = ",";

    /**
     * The CSV comment marker character.
     */
    public static final String CSV_COMMENT_MARKER = "#";

    /**
     * Encoding to use for generated files.
     */
    protected String encoding;

    /**
     * Summary filename to create in each directory.
     */
    protected String summaryFilename;

    /**
     * Summary path => hashed file => (algorithm,hashcode).
     */
    protected Map<File, Map<File, Map<String, String>>> filesHashcodes;

    /**
     * The set of algorithms encountered.
     */
    protected SortedSet<String> algorithms;

    /**
     * Build a new instance of {@link net.nicoulaj.maven.plugins.checksum.execution.target.DirectorySummaryFileTarget}.
     *
     * @param encoding the encoding to use for generated files.
     */
    public DirectorySummaryFileTarget(String encoding, String summaryFilename)
    {
        this.encoding = encoding;
        this.summaryFilename = summaryFilename;
    }

    /**
     * {@inheritDoc}
     */
    public void init()
        throws ExecutionTargetInitializationException
    {
        filesHashcodes = new HashMap<File, Map<File, Map<String, String>>>();
        algorithms = new TreeSet<String>();
    }

    /**
     * {@inheritDoc}
     */
    public void write( String digest, File file, String algorithm )
        throws ExecutionTargetWriteException
    {
        File path = new File(file.getPath().substring(0, file.getPath().length() - file.getName().length()));

        // Initialize an entry for the file if needed.
        if ( !filesHashcodes.containsKey( path ) )
        {
            filesHashcodes.put( path, new HashMap<File, Map<String, String>>() );
        }

        // Store the algorithm => hashcode mapping for this file.
        Map<File, Map<String, String>> fileHashcodes = filesHashcodes.get( path );
        if ( !fileHashcodes.containsKey( file ))
        {
            fileHashcodes.put( file, new HashMap<String, String>() );
        }
        Map<String, String> hashcodes = fileHashcodes.get( file );
        hashcodes.put( algorithm, digest );

        // Store the algorithm.
        algorithms.add( algorithm );
    }

    /**
     * {@inheritDoc}
     */
    public void close()
            throws ExecutionTargetCloseException
    {
        // Write a file for each directory.
        for (File summaryPath : filesHashcodes.keySet()) {
            StringBuilder sb = new StringBuilder();

            // Write the CSV file header.
            sb.append( CSV_COMMENT_MARKER ).append( "File" );
            for ( String algorithm : algorithms )
            {
                sb.append( CSV_COLUMN_SEPARATOR ).append( algorithm );
            }

            // Write a line for each file.
            for ( File file : filesHashcodes.get(summaryPath).keySet() )
            {
                sb.append( LINE_SEPARATOR ).append( file.getName() );
                Map<String, String> fileHashcodes = filesHashcodes.get(summaryPath).get( file );
                for ( String algorithm : algorithms )
                {
                    sb.append( CSV_COLUMN_SEPARATOR );
                    if ( fileHashcodes.containsKey( algorithm ) )
                    {
                        sb.append( fileHashcodes.get( algorithm ) );
                    }
                }
            }

            // Write the result to the summary file.
            try
            {
                FileUtils.fileWrite( summaryPath + "/" + summaryFilename, encoding, sb.toString() );
            }
            catch ( IOException e )
            {
                throw new ExecutionTargetCloseException( e.getMessage() );
            }
        }
    }
}
