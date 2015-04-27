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
package net.nicoulaj.maven.plugins.checksum.test.unit.digest;

import net.nicoulaj.maven.plugins.checksum.digest.DigesterException;
import net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory;
import net.nicoulaj.maven.plugins.checksum.digest.FileDigester;
import net.nicoulaj.maven.plugins.checksum.test.unit.Constants;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tests for each implementation of {@link net.nicoulaj.maven.plugins.checksum.digest.FileDigester}.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @see net.nicoulaj.maven.plugins.checksum.digest.FileDigester
 * @since 1.0
 */
@RunWith( Parameterized.class )
public class DigestersTest
{
    /**
     * The {@link net.nicoulaj.maven.plugins.checksum.digest.FileDigester} tested.
     */
    private FileDigester digester;

    /**
     * Rule used to specify per-test expected exceptions.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Generate the list of arguments with which the test should be run.
     *
     * @return the list of tested {@link net.nicoulaj.maven.plugins.checksum.digest.FileDigester} implementations.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters()
    {
        List<Object[]> data = new ArrayList<Object[]>();
        for ( String algorithm : net.nicoulaj.maven.plugins.checksum.Constants.SUPPORTED_ALGORITHMS )
        {
            data.add( new String[]{ algorithm } );
        }
        return data;
    }

    /**
     * Build a new {@link DigestersTest}.
     *
     * @param algorithm the target checksum algorithm to run the test for.
     * @throws NoSuchAlgorithmException should never happen.
     */
    public DigestersTest( String algorithm )
        throws NoSuchAlgorithmException
    {
        this.digester = DigesterFactory.getInstance().getFileDigester( algorithm );
    }

    /**
     * Assert the algorithm name is not null/empty.
     */
    @Test
    public void testAlgorithmNameDefined()
    {
        String algorithmName = digester.getAlgorithm();
        Assert.assertNotNull( "The algorithm name is null.", algorithmName );
        Assert.assertTrue( "The algorithm name is empty.", algorithmName.length() > 0 );
    }

    /**
     * Assert the file name extension is not null/empty.
     */
    @Test
    public void testFilenameExtensionDefined()
    {
        String filenameExtension = digester.getFileExtension();
        Assert.assertNotNull( "The file name extension is null.", filenameExtension );
        Assert.assertTrue( "The file name extension is empty.", filenameExtension.length() > 0 );
    }

    /**
     * Check the calculated checksum for a specific file is valid against a pre-calculated checksum.
     *
     * @throws DigesterException if there was a problem while calculating the checksum.
     * @throws IOException       if there was a problem reading the file containing the pre-calculated checksum.
     * @see net.nicoulaj.maven.plugins.checksum.digest.FileDigester#calculate(java.io.File)
     */
    @Test
    public void testCalculate()
        throws DigesterException, IOException
    {
        List<File> testFiles = FileUtils.getFiles( new File( Constants.SAMPLE_FILES_PATH ), null, null );
        for ( File testFile : testFiles )
        {
            String calculatedHash = digester.calculate( testFile );
            String correctHash = FileUtils.fileRead(
                Constants.SAMPLE_FILES_HASHCODES_PATH + File.separator + testFile.getName()
                    + digester.getFileExtension() );
            Assert.assertEquals(
                "The calculated " + digester.getAlgorithm() + " hashcode for " + testFile.getName() + " is incorrect.",
                correctHash, calculatedHash );
        }
    }

    /**
     * Check an exception is thrown when attempting to calculate the checksum of a file that does not exist.
     *
     * @throws DigesterException should always happen.
     * @see net.nicoulaj.maven.plugins.checksum.digest.FileDigester#calculate(java.io.File)
     */
    @Test
    public void testCalculateExceptionThrownOnFileNotFound()
        throws DigesterException
    {
        exception.expect( DigesterException.class );
        digester.calculate( new File( "some/path/that/does/not/exist" ) );
    }
}
