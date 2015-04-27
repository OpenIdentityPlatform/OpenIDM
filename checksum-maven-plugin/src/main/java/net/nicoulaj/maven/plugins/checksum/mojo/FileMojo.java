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
package net.nicoulaj.maven.plugins.checksum.mojo;

import net.nicoulaj.maven.plugins.checksum.Constants;
import net.nicoulaj.maven.plugins.checksum.execution.Execution;
import net.nicoulaj.maven.plugins.checksum.execution.ExecutionException;
import net.nicoulaj.maven.plugins.checksum.execution.FailOnErrorExecution;
import net.nicoulaj.maven.plugins.checksum.execution.target.MavenLogTarget;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Arrays;

/**
 * Compute specified file checksum digest for all supported checksum algorithms.
 * <p/>
 * <p>This goal is a facility for invoking checksum-maven-plugin through the command line.</p>
 * <p/>
 * <p>Here is an example of use:<pre>
 * mvn checksum:file -Dfile=some-file.zip
 * </pre></p>
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
@Mojo(
    name = FileMojo.NAME,
    threadSafe = true )
public class FileMojo
    extends AbstractMojo
{
    /**
     * The mojo name.
     */
    public static final String NAME = "file";

    /**
     * The file to process.
     *
     * @since 1.0
     */
    @Parameter( property = "file", required = true )
    protected String file;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Prepare an execution.
        Execution execution = new FailOnErrorExecution();
        execution.setAlgorithms( Arrays.asList( Constants.SUPPORTED_ALGORITHMS ) );
        execution.addFile( new File( file ) );
        execution.addTarget( new MavenLogTarget( getLog() ) );

        // Run the execution.
        try
        {
            execution.run();
        }
        catch ( ExecutionException e )
        {
            getLog().error( e.getMessage() );
            throw new MojoFailureException( e.getMessage() );
        }
    }
}
