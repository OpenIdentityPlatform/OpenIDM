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
package net.nicoulaj.maven.plugins.checksum.test.unit.execution.target;

import net.nicoulaj.maven.plugins.checksum.Constants;
import net.nicoulaj.maven.plugins.checksum.execution.target.CsvSummaryFileTarget;
import net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget;
import net.nicoulaj.maven.plugins.checksum.execution.target.MavenLogTarget;
import net.nicoulaj.maven.plugins.checksum.execution.target.OneHashPerFileTarget;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests for {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} implementations.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
@RunWith( Parameterized.class )
public class ExecutionTargetsTest
{
    /**
     * An instance of the {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget} implementation to
     * test.
     */
    private ExecutionTarget executionTarget;

    /**
     * Generate the list of arguments with which the test should be run.
     *
     * @return instances of {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget}
     */
    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters()
    {
        Object[][] data =
            new Object[][]{ { new MavenLogTarget( null ) }, { new OneHashPerFileTarget( Constants.DEFAULT_ENCODING ) },
                { new CsvSummaryFileTarget( new File( "" ), Constants.DEFAULT_ENCODING ) } };
        return Arrays.asList( data );
    }

    /**
     * Build a new {@link ExecutionTargetsTest}.
     *
     * @param executionTarget the {@link net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget}
     *                        implementation to test.
     */
    public ExecutionTargetsTest( ExecutionTarget executionTarget )
    {
        this.executionTarget = executionTarget;
    }

    /**
     * TODO add Javadoc comment.
     */
    @Test
    public void testSomething()
    {
        // TODO implement this test.
    }
}
