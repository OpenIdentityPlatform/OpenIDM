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

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An {@link ExecutionTarget} that writes digests to a CSV file.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public class CsvSummaryFileTarget
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
     * The association file => (algorithm,hashcode).
     */
    protected Map<File, Map<String, String>> filesHashcodes;

    /**
     * The set of algorithms encountered.
     */
    protected SortedSet<String> algorithms;

    /**
     * The target file where the summary is written.
     */
    protected File summaryFile;

    /**
     * The root path to be removed from summary files.
     */
    protected String summaryRoot;

    /**
     * Build a new instance of {@link CsvSummaryFileTarget}.
     *
     * @param summaryFile the file to which the summary should be written.
     * @param encoding    the encoding to use for generated files.
     */
    public CsvSummaryFileTarget( File summaryFile, String encoding )
    {
        this.summaryFile = summaryFile;
        this.encoding = encoding;
    }

    /**
     * Build a new instance of {@link CsvSummaryFileTarget}.
     *
     * @param summaryFile the file to which the summary should be written.
     * @param encoding    the encoding to use for generated files.
     * @param summaryRoot the root path to remove from filepaths prior to writing to the summary file.
     */
    public CsvSummaryFileTarget( File summaryFile, String encoding, String summaryRoot )
    {
        this.summaryFile = summaryFile;
        this.encoding = encoding;
        this.summaryRoot = summaryRoot;
    }

    /**
     * {@inheritDoc}
     */
    public void init()
    {
        filesHashcodes = new HashMap<File, Map<String, String>>();
        algorithms = new TreeSet<String>();
    }

    /**
     * {@inheritDoc}
     */
    public void write( String digest, File file, String algorithm )
    {
        // Initialize an entry for the file if needed.
        if ( !filesHashcodes.containsKey( file ) )
        {
            filesHashcodes.put( file, new HashMap<String, String>() );
        }

        // Store the algorithm => hashcode mapping for this file.
        Map<String, String> fileHashcodes = filesHashcodes.get( file );
        fileHashcodes.put( algorithm, digest );

        // Store the algorithm.
        algorithms.add( algorithm );
    }

    /**
     * {@inheritDoc}
     */
    public void close()
        throws ExecutionTargetCloseException
    {
        StringBuilder sb = new StringBuilder();

        // Write the CSV file header.
        sb.append( CSV_COMMENT_MARKER ).append( "File" );
        for ( String algorithm : algorithms )
        {
            sb.append( CSV_COLUMN_SEPARATOR ).append( algorithm );
        }

        // Write a line for each file.
        for ( File file : filesHashcodes.keySet() )
        {
            sb.append( LINE_SEPARATOR ).append( stripSummaryRoot(file) );
            Map<String, String> fileHashcodes = filesHashcodes.get( file );
            for ( String algorithm : algorithms )
            {
                sb.append( CSV_COLUMN_SEPARATOR );
                if ( fileHashcodes.containsKey( algorithm ) )
                {
                    sb.append( fileHashcodes.get( algorithm ) );
                }
            }
        }

        // Make sure the parent directory exists.
        FileUtils.mkdir( summaryFile.getParent() );

        // Write the result to the summary file.
        try
        {
            FileUtils.fileWrite( summaryFile.getPath(), encoding, sb.toString() );
        }
        catch ( IOException e )
        {
            throw new ExecutionTargetCloseException( e.getMessage() );
        }
    }

    private String stripSummaryRoot(File file) {
        if (summaryRoot != null && file.getPath().startsWith(summaryRoot)) {
            return file.getPath().substring(summaryRoot.length());
        }
        return file.getPath();
    }
}
