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
import java.util.List;

/**
 * Effective execution of plugin goals used {@link org.apache.maven.plugin.Mojo} implementations.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @see net.nicoulaj.maven.plugins.checksum.mojo
 * @since 1.0
 */
public interface Execution
{
    /**
     * Get the list of files to be processed by the execution.
     *
     * @return the files configured for this execution.
     */
    List<File> getFiles();

    /**
     * Set the list of files to be processed by the execution.
     *
     * @param files the value to set.
     */
    void setFiles( List<File> files );

    /**
     * Add a file to the list of files to be processed by the execution.
     *
     * @param file the file to add.
     */
    void addFile( File file );

    /**
     * Remove a file from the list of files to be processed by the execution.
     *
     * @param file the file to remove.
     */
    void removeFile( File file );

    /**
     * Get the list of checksum algorithms to be used by the execution.
     *
     * @return the algorithms configured for this execution.
     */
    List<String> getAlgorithms();

    /**
     * Set the list of checksum algorithms to be used by the execution.
     *
     * @param algorithms the value to set.
     */
    void setAlgorithms( List<String> algorithms );

    /**
     * Add an algorithm to the list of checksum algorithms to be used by the execution.
     *
     * @param algorithm the algorithm to add.
     */
    void addAlgorithm( String algorithm );

    /**
     * Remove an algorithm from the list of checksum algorithms to be used by the execution.
     *
     * @param algorithm the algorithm to remove.
     */
    void removeAlgorithm( String algorithm );

    /**
     * Get the list of {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} to be used by the
     * execution.
     *
     * @return the targets configured for this execution.
     */
    List<ExecutionTarget> getTargets();

    /**
     * Set the list of {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} to be used by the
     * execution.
     *
     * @param targets the value to set.
     */
    void setTargets( List<ExecutionTarget> targets );

    /**
     * Add a target to the list of {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} to be
     * used by the execution.
     *
     * @param target the target to add.
     */
    void addTarget( ExecutionTarget target );

    /**
     * Remove a target from the list of {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} to
     * be used by the execution.
     *
     * @param target the target to remove.
     */
    void removeTarget( ExecutionTarget target );

    /**
     * Check that an execution can be run with the {#run} method.
     *
     * @throws ExecutionException if some parameters are not initialized or invalid.
     */
    void checkParameters()
        throws ExecutionException;

    /**
     * Run the execution using for the files, algorithms and targets set.
     *
     * @throws ExecutionException if an error happens while running the execution.
     */
    void run()
        throws ExecutionException;
}
