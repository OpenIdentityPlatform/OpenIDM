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
package net.nicoulaj.maven.plugins.checksum.execution;

import net.nicoulaj.maven.plugins.checksum.digest.DigesterException;
import net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory;
import net.nicoulaj.maven.plugins.checksum.digest.FileDigester;
import net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget;
import net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTargetWriteException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.security.NoSuchAlgorithmException;

/**
 * An implementation of {@link net.nicoulaj.maven.plugins.checksum.execution.Execution} that tries to finish as much as
 * possible even if it encounters errors.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @see net.nicoulaj.maven.plugins.checksum.execution.FailOnErrorExecution
 * @since 1.0
 */
public class NeverFailExecution
    extends AbstractExecution
{
    /**
     * The {@link org.apache.maven.plugin.logging.Log} used instead of throwing exceptions on error.
     */
    protected final Log logger;

    /**
     * Build a new {@link net.nicoulaj.maven.plugins.checksum.execution.NeverFailExecution} instance.
     *
     * @param logger the logger used instead of throwing exceptions on error.
     */
    public NeverFailExecution( Log logger )
    {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    public void run()
    {
        // Check parameters are initialized.
        try
        {
            checkParameters();
        }
        catch ( ExecutionException e )
        {
            logger.error( e.getMessage() );
            return;
        }

        // Initialize targets
        for ( ExecutionTarget target : getTargets() )
        {
            try
            {
                target.init();
            }
            catch ( Exception e )
            {
                // Remove the target in fault
                removeTarget( target );
                logger.warn( e.getMessage() );

                // If this was the last target, cancel the execution.
                if ( getTargets().isEmpty() )
                {
                    logger.warn( "No output target for computed hashcodes, execution canceled." );
                    return;
                }
            }
        }

        // Process files
        for ( File file : files )
        {
            for ( String algorithm : getAlgorithms() )
            {
                try
                {
                    // Calculate the hash for the file/algo
                    FileDigester digester = DigesterFactory.getInstance().getFileDigester( algorithm );
                    String hash = digester.calculate( file );

                    // Write it to each target defined
                    for ( ExecutionTarget target : getTargets() )
                    {
                        try
                        {
                            target.write( hash, file, algorithm );
                        }
                        catch ( ExecutionTargetWriteException e )
                        {
                            logger.warn( e.getMessage() );
                        }
                    }
                }
                catch ( NoSuchAlgorithmException e )
                {
                    logger.warn( "Unsupported algorithm " + algorithm + "." );
                }
                catch ( DigesterException e )
                {
                    logger.warn(
                        "Unable to calculate " + algorithm + " hash for " + file.getName() + ": " + e.getMessage() );
                }
            }
        }

        // Close targets
        for ( ExecutionTarget target : getTargets() )
        {
            try
            {
                target.close();
            }
            catch ( Exception e )
            {
                logger.warn( e.getMessage() );
            }
        }
    }
}
