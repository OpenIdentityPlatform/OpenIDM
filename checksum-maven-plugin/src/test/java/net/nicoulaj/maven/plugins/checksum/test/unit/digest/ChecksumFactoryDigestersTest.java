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

import net.nicoulaj.maven.plugins.checksum.Constants;
import net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory;
import net.nicoulaj.maven.plugins.checksum.digest.FileDigester;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tests for each checksum algorithm supported by the {@link DigesterFactory} class.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @see net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory
 * @since 1.0
 */
@RunWith( Parameterized.class )
public class ChecksumFactoryDigestersTest
{
    /**
     * The algorithm for which the {@link net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory} is tested.
     */
    private String algorithm;

    /**
     * Generate the list of arguments with which the test should be run.
     *
     * @return the list of supported algorithms.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters()
    {
        List<Object[]> data = new ArrayList<Object[]>();
        for ( String algorithm : Constants.SUPPORTED_ALGORITHMS )
        {
            data.add( new String[]{ algorithm } );
        }
        return data;
    }

    /**
     * Build a new {@link ChecksumFactoryDigestersTest}.
     *
     * @param algorithm the target checksum algorithm to run the test for.
     */
    public ChecksumFactoryDigestersTest( String algorithm )
    {
        this.algorithm = algorithm;
    }

    /**
     * Assert the returned {@link net.nicoulaj.maven.plugins.checksum.digest.FileDigester} object is not null.
     *
     * @throws NoSuchAlgorithmException should never happen.
     */
    @Test
    public void testDigesterIsNotNull()
        throws NoSuchAlgorithmException
    {
        FileDigester digester = DigesterFactory.getInstance().getFileDigester( algorithm );
        Assert.assertNotNull( "The returned digester for  '" + algorithm + "' algorithm is null.", digester );
    }

    /**
     * Assert the returned {@link net.nicoulaj.maven.plugins.checksum.digest.FileDigester} object is a singleton.
     *
     * @throws NoSuchAlgorithmException should never happen.
     */
    @Test
    public void testDigesterIsSingleton()
        throws NoSuchAlgorithmException
    {
        FileDigester digester1 = DigesterFactory.getInstance().getFileDigester( algorithm );
        FileDigester digester2 = DigesterFactory.getInstance().getFileDigester( algorithm );
        Assert.assertEquals( "The returned digester for  '" + algorithm + "' algorithm is not a singleton.", digester1,
                             digester2 );
    }

    /**
     * Assert the returned {@link net.nicoulaj.maven.plugins.checksum.digest.FileDigester} object is the good one..
     *
     * @throws NoSuchAlgorithmException should never happen.
     */
    @Test
    public void testDigesterIsRightOne()
        throws NoSuchAlgorithmException
    {
        FileDigester digester = DigesterFactory.getInstance().getFileDigester( algorithm );
        Assert.assertEquals( "The returned digester for  '" + algorithm + "' algorithm is not the right one.",
                             digester.getAlgorithm(), algorithm );
    }
}
