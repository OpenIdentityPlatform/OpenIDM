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

/**
 * Base class for implementations of {@link FileDigester}.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public abstract class AbstractFileDigester
    implements FileDigester
{
    /**
     * The algorithm used to compute checksum digests.
     */
    protected final String algorithm;

    /**
     * Build a new instance of {@link AbstractFileDigester}.
     *
     * @param algorithm the algorithm used to compute checksum digests.
     */
    protected AbstractFileDigester( String algorithm )
    {
        this.algorithm = algorithm;
    }

    /**
     * {@inheritDoc}
     */
    public String getAlgorithm()
    {
        return algorithm;
    }

    /**
     * {@inheritDoc}
     */
    public String getFileExtension()
    {
        return "." + algorithm.toLowerCase().replace( "-", "" );
    }
}
