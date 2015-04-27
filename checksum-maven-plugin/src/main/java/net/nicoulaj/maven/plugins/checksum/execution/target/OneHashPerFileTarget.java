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

/**
 * An {@link ExecutionTarget} that writes digests to separate files.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public class OneHashPerFileTarget
    implements ExecutionTarget
{
    /**
     * Encoding to use for generated files.
     */
    protected String encoding;

    /**
     * The files output directory.
     */
    protected File outputDirectory;

    /**
     * Build a new instance of {@link OneHashPerFileTarget}.
     *
     * @param encoding        the encoding to use for generated files.
     * @param outputDirectory the files output directory.
     */
    public OneHashPerFileTarget( String encoding, File outputDirectory )
    {
        this.encoding = encoding;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Build a new instance of {@link OneHashPerFileTarget}.
     *
     * @param encoding the encoding to use for generated files.
     */
    public OneHashPerFileTarget( String encoding )
    {
        this( encoding, null );
    }

    /**
     * {@inheritDoc}
     */
    public void init()
        throws ExecutionTargetInitializationException
    {
        // Make sure the output directory exists or can be created.
        if ( outputDirectory != null )
        {
            if ( outputDirectory.exists() && !outputDirectory.isDirectory() )
            {
                throw new ExecutionTargetInitializationException(
                    "'" + outputDirectory.getPath() + "' already exists and is not a directory." );
            }
            else
            {
                outputDirectory.mkdirs();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write( String digest, File file, String algorithm )
        throws ExecutionTargetWriteException
    {
        try
        {
            File outputFileDirectory = ( outputDirectory != null ) ? outputDirectory : file.getParentFile();
            String outputFileName =
                file.getName() + DigesterFactory.getInstance().getFileDigester( algorithm ).getFileExtension();
            FileUtils.fileWrite( outputFileDirectory.getPath() + File.separator + outputFileName, digest );
        }
        catch ( IOException e )
        {
            throw new ExecutionTargetWriteException( e.getMessage() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new ExecutionTargetWriteException( e.getMessage() );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
        // Nothing to do
    }
}
