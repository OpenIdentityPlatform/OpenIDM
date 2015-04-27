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
package net.nicoulaj.maven.plugins.checksum.digest;

import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An implementation of {@link FileDigester} that streams the content of files to a {@link
 * java.security.MessageDigest}.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public class MessageDigestFileDigester
    extends AbstractFileDigester
{
    /**
     * The {@link java.security.MessageDigest} instance.
     */
    protected final MessageDigest messageDigest;

    /**
     * Build a new instance of {@link MessageDigestFileDigester}.
     *
     * @param algorithm the algorithm used to compute checksum digests.
     * @throws NoSuchAlgorithmException in case the given is not supported.
     */
    protected MessageDigestFileDigester( String algorithm )
        throws NoSuchAlgorithmException
    {
        super( algorithm );
        messageDigest = MessageDigest.getInstance( algorithm );
    }

    /**
     * {@inheritDoc}
     */
    public String calculate( File file )
        throws DigesterException
    {
        // Try to open the file.
        FileInputStream fis;
        try
        {
            fis = new FileInputStream( file );
        }
        catch ( Exception e )
        {
            throw new DigesterException( "Unable not read " + file.getPath() + ": " + e.getMessage() );
        }

        String result;

        try
        {
            // Recover from a previous execution if needed.
            messageDigest.reset();

            // Stream the file contents to the MessageDigest.
            byte[] buffer = new byte[STREAMING_BUFFER_SIZE];
            int size = fis.read( buffer, 0, STREAMING_BUFFER_SIZE );
            while ( size >= 0 )
            {
                messageDigest.update( buffer, 0, size );
                size = fis.read( buffer, 0, STREAMING_BUFFER_SIZE );
            }

            result = new String( Hex.encode( messageDigest.digest() ) );
        }
        catch ( IOException e )
        {
            throw new DigesterException(
                "Unable to calculate the " + getAlgorithm() + " hashcode for " + file.getPath() + ": "
                    + e.getMessage() );
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch ( IOException e )
            {
                // Don't take any chance, return an empty string if something went wrong.
                result = "";
            }
        }
        return result;
    }
}
