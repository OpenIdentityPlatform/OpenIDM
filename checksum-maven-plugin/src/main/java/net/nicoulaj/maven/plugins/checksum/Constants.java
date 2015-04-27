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
package net.nicoulaj.maven.plugins.checksum;

/**
 * Constants used by checksum-maven-plugin.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public class Constants
{
    /**
     * The CRC/checksum digest algorithms supported by checksum-maven-plugin.
     */
    public static final String[] SUPPORTED_ALGORITHMS =
        { "Cksum", "CRC32", "MD2", "MD4", "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "RIPEMD128", "RIPEMD160",
            "RIPEMD256", "RIPEMD320", "GOST3411", "Tiger", "Whirlpool" };

    /**
     * The algorithms used by default for a mojo execution.
     */
    public static final String[] DEFAULT_EXECUTION_ALGORITHMS = { "MD5", "SHA-1" };

    /**
     * The file encoding used by default.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
}
