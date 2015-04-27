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

import net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for {@link Execution} implementations.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @see net.nicoulaj.maven.plugins.checksum.execution.Execution
 * @since 1.0
 */
public abstract class AbstractExecution
    implements Execution
{
    /**
     * The list of files used for the execution.
     */
    protected List<File> files;

    /**
     * The list of algorithms used for execution.
     */
    protected List<String> algorithms;

    /**
     * The list of targets used for the execution.
     */
    protected List<ExecutionTarget> targets;

    /**
     * {@inheritDoc}
     */
    public List<File> getFiles()
    {
        return files;
    }

    /**
     * {@inheritDoc}
     */
    public void setFiles( List<File> files )
    {
        this.files = files;
    }

    /**
     * {@inheritDoc}
     */
    public void addFile( File file )
    {
        if ( files == null )
        {
            files = new LinkedList<File>();
        }
        files.add( file );
    }

    /**
     * {@inheritDoc}
     */
    public void removeFile( File file )
    {
        if ( files != null )
        {
            files.remove( file );
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getAlgorithms()
    {
        return algorithms;
    }

    /**
     * {@inheritDoc}
     */
    public void setAlgorithms( List<String> algorithms )
    {
        this.algorithms = algorithms;
    }

    /**
     * {@inheritDoc}
     */
    public void addAlgorithm( String algorithm )
    {
        if ( algorithms == null )
        {
            algorithms = new LinkedList<String>();
        }
        algorithms.add( algorithm );
    }

    /**
     * {@inheritDoc}
     */
    public void removeAlgorithm( String algorithm )
    {
        if ( algorithms != null )
        {
            algorithms.remove( algorithm );
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ExecutionTarget> getTargets()
    {
        return targets;
    }

    /**
     * {@inheritDoc}
     */
    public void addTarget( ExecutionTarget target )
    {
        if ( targets == null )
        {
            targets = new LinkedList<ExecutionTarget>();
        }
        targets.add( target );
    }

    /**
     * {@inheritDoc}
     */
    public void removeTarget( ExecutionTarget target )
    {
        if ( targets != null )
        {
            targets.remove( target );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTargets( List<ExecutionTarget> targets )
    {
        this.targets = targets;
    }

    /**
     * {@inheritDoc}
     */
    public void checkParameters()
        throws ExecutionException
    {
        if ( files == null || files.isEmpty() )
        {
            throw new ExecutionException( "No file to process." );
        }
        if ( algorithms == null || algorithms.isEmpty() )
        {
            throw new ExecutionException( "No checksum algorithm defined." );
        }
        if ( targets == null || targets.isEmpty() )
        {
            throw new ExecutionException( "No output target defined." );
        }
    }
}
