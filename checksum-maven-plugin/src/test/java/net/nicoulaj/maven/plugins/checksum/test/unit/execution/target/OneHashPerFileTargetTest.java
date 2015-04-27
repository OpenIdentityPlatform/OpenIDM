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

import net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory;
import net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget;
import net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTargetWriteException;
import net.nicoulaj.maven.plugins.checksum.execution.target.OneHashPerFileTarget;
import net.nicoulaj.maven.plugins.checksum.test.unit.Constants;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Tests for the {@link net.nicoulaj.maven.plugins.checksum.execution.target.OneHashPerFileTarget} {@link
 * net.nicoulaj.maven.plugins.checksum.execution.target.ExecutionTarget}.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 */
public class OneHashPerFileTargetTest
{
    /**
     * The instance of {@link net.nicoulaj.maven.plugins.checksum.execution.target.OneHashPerFileTarget} used for the
     * test.
     */
    ExecutionTarget target = new OneHashPerFileTarget( net.nicoulaj.maven.plugins.checksum.Constants.DEFAULT_ENCODING );

    /**
     * Assert the target writes the right content to the right file.
     *
     * @throws ExecutionTargetWriteException should never happen.
     * @throws IOException                   should never happen.
     * @throws NoSuchAlgorithmException      should never happen.
     * @see net.nicoulaj.maven.plugins.checksum.execution.target.OneHashPerFileTarget#write(String, java.io.File,
     *      String)
     */
    @Test
    public void testOneHashPerFileTargetWrite()
        throws ExecutionTargetWriteException, IOException, NoSuchAlgorithmException
    {
        List<File> testFiles = FileUtils.getFiles( new File( Constants.SAMPLE_FILES_PATH ), null, null );
        for ( File testFile : testFiles )
        {
            // Prepare the test
            String testAlgorithm = "MD5";
            String hashcodeFile =
                testFile.getPath() + DigesterFactory.getInstance().getFileDigester( testAlgorithm ).getFileExtension();

            // Call write()
            target.write( "hash-file-content", testFile, testAlgorithm );

            // Assert the file has been created with the right content
            Assert.assertTrue( new File( hashcodeFile ).exists() );
            Assert.assertEquals( FileUtils.fileRead( new File( hashcodeFile ) ), "hash-file-content" );

            // Delete the file created
            FileUtils.fileDelete( hashcodeFile );
        }
    }
}
