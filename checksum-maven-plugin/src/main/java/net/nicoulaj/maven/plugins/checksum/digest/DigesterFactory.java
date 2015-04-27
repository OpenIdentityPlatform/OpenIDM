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

import net.nicoulaj.maven.plugins.checksum.Constants;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class used to get instances of {@link FileDigester}.
 * <p/>
 * <p>Each {@link FileDigester} object is a singleton itself.</p>
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @see FileDigester
 * @since 1.0
 */
public class DigesterFactory
{
    /**
     * The instance of {@link net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory}.
     */
    private static DigesterFactory instance;

    /**
     * The map (algorithm, digester).
     */
    protected Map<String, FileDigester> digesters =
        new HashMap<String, FileDigester>( Constants.SUPPORTED_ALGORITHMS.length );

    /**
     * Build a new {@link net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory}.
     *
     * @see #getInstance()
     */
    private DigesterFactory()
    {
    }

    /**
     * Get the instance of {@link net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory}.
     *
     * @return the only instance of {@link net.nicoulaj.maven.plugins.checksum.digest.DigesterFactory}.
     */
    public static synchronized DigesterFactory getInstance()
    {
        if ( instance == null )
        {
            instance = new DigesterFactory();
        }
        return instance;
    }

    /**
     * Get an instance of {@link FileDigester} for the given checksum algorithm.
     *
     * @param algorithm the target checksum algorithm.
     * @return an instance of {@link FileDigester}.
     * @throws NoSuchAlgorithmException if the checksum algorithm is not supported or invalid.
     * @see FileDigester
     */
    public synchronized FileDigester getFileDigester( String algorithm )
        throws NoSuchAlgorithmException
    {
        FileDigester digester = digesters.get( algorithm );

        if ( digester == null )
        {
            // Algorithms with custom digesters
            if ( CRC32FileDigester.ALGORITHM.equals( algorithm ) )
            {
                digester = new CRC32FileDigester();
            }
            
            else if ( CksumFileDigester.ALGORITHM.equals( algorithm ) )
            {
                digester = new CksumFileDigester();
            }

            // Default case: try to use Java Security providers.
            else
            {
                // Try with the current providers.
                try
                {
                    digester = new MessageDigestFileDigester( algorithm );
                }
                catch ( NoSuchAlgorithmException e )
                {
                    // If the algorithm is not supported by default providers, try with Bouncy Castle.
                    if ( Security.getProvider( BouncyCastleProvider.PROVIDER_NAME ) == null )
                    {
                        Security.addProvider( new BouncyCastleProvider() );
                        digester = new MessageDigestFileDigester( algorithm );
                    }

                    // If Bouncy Castle was already used, fail.
                    else
                    {
                        throw e;
                    }
                }
            }

            digesters.put( algorithm, digester );
        }

        return digester;
    }
}
