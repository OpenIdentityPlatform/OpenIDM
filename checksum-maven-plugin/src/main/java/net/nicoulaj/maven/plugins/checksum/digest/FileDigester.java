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

import java.io.File;

/**
 * Class for computing checksums digests from files.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public interface FileDigester
{
    /**
     * The size of the buffer used to stream the files contents while calculating their hashcode.
     */
    int STREAMING_BUFFER_SIZE = 32768;

    /**
     * Get the algorithm used to compute checksum digests.
     *
     * @return the algorithm.
     */
    String getAlgorithm();

    /**
     * The filename extension for this digester.
     *
     * @return the filename extension.
     */
    String getFileExtension();

    /**
     * Calculate a checksum for a file.
     *
     * @param file the file to compute the checksum for.
     * @return the current checksum.
     * @throws DigesterException if there was a problem computing the hashcode.
     */
    String calculate( File file )
        throws DigesterException;
}
