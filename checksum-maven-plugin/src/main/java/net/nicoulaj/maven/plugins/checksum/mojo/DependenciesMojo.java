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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Compute project dependencies checksum digests and store them in a summary file.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
@Mojo(
    name = DependenciesMojo.NAME,
    defaultPhase = LifecyclePhase.VERIFY,
    requiresProject = true,
    inheritByDefault = false,
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true )
public class DependenciesMojo
    extends AbstractChecksumMojo
{
    /**
     * The mojo name.
     */
    public static final String NAME = "dependencies";

    /**
     * Indicates whether the build will store checksums in separate files (one file per algorithm per artifact).
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "false" )
    protected boolean individualFiles;

    /**
     * The directory where output files will be stored. Leave unset to have each file next to the source file.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${project.build.directory}" )
    protected String individualFilesOutputDirectory;

    /**
     * Indicates whether the build will store checksums to per-directory summary files.
     *
     * @since 1.3
     */
    @Parameter( defaultValue = "false" )
    protected boolean directoryFiles;

    /**
     * The root of the path to be stored in the summary file(s).
     *
     * @since 1.3
     */
    @Parameter
    protected String summaryRoot;

    /**
     * Indicates whether the build will store checksums to a single CSV summary file.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "true" )
    protected boolean csvSummary;

    /**
     * The name of the summary file created if the option is activated.
     *
     * @see #csvSummary
     * @since 1.0
     */
    @Parameter( defaultValue = "dependencies-checksums.csv" )
    protected String csvSummaryFile;

    /**
     * Indicates whether the build will store checksums to a single XML summary file.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "false" )
    protected boolean xmlSummary;

    /**
     * The name of the summary file created if the option is activated.
     *
     * @see #xmlSummary
     * @since 1.0
     */
    @Parameter( defaultValue = "dependencies-checksums.xml" )
    protected String xmlSummaryFile;

    /**
     * The dependency scopes to include.
     * <p/>
     * <p>Allowed values are compile, test, runtime, provided and system.<br/>All scopes are included by default.</p>
     * <p/>
     * <p> Use the following syntax:
     * <pre>&lt;scopes&gt;
     *   &lt;scope&gt;compile&lt;scope&gt;
     *   &lt;scope&gt;runtime&lt;scope&gt;
     * &lt;/scopes&gt;</pre>
     * </p>
     *
     * @since 1.0
     */
    @Parameter
    protected List<String> scopes;

    /**
     * The dependency types to include.
     * <p/>
     * <p>All types are included by default.</p>
     * <p/>
     * <p> Use the following syntax:
     * <pre>&lt;types&gt;
     *   &lt;type&gt;jar&lt;type&gt;
     *   &lt;type&gt;zip&lt;type&gt;
     * &lt;/types&gt;</pre>
     * </p>
     *
     * @since 1.0
     */
    @Parameter
    protected List<String> types;

    /**
     * Build the list of files from which digests should be generated.
     * <p/>
     * <p>The list is composed of the project dependencies.</p>
     *
     * @return the list of files that should be processed.
     */
    protected List<File> getFilesToProcess()
    {
        List<File> files = new LinkedList<File>();

        for ( Artifact artifact : (Set<Artifact>) project.getDependencyArtifacts() )
        {
            if ( ( scopes == null || scopes.contains( artifact.getScope() ) ) && ( types == null || types.contains(
                artifact.getType() ) ) )
            {
                files.add( artifact.getFile() );
            }
        }

        return files;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isIndividualFiles()
    {
        return individualFiles;
    }

    /**
     * {@inheritDoc}
     */
    protected String getIndividualFilesOutputDirectory()
    {
        return individualFilesOutputDirectory;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isDirectoryFiles() {
        return directoryFiles;
    }

    /**
     * {@inheritDoc}
     */
    protected String getSummaryRoot() {
        return summaryRoot;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isCsvSummary()
    {
        return csvSummary;
    }

    /**
     * {@inheritDoc}
     */
    protected String getCsvSummaryFile()
    {
        return csvSummaryFile;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isXmlSummary()
    {
        return xmlSummary;
    }

    /**
     * {@inheritDoc}
     */
    protected String getXmlSummaryFile()
    {
        return xmlSummaryFile;
    }
}
