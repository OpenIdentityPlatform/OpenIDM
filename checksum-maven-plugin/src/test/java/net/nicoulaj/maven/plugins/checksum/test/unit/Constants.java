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
package net.nicoulaj.maven.plugins.checksum.test.unit;

import java.io.File;

/**
 * Constants used by unit test classes.
 *
 * @author <a href="mailto:julien.nicoulaud@gmail.com">Julien Nicoulaud</a>
 * @since 1.0
 */
public class Constants
{
    /**
     * Path of the test resources directory.
     */
    public static final String RESOURCES_PATH = "target" + File.separator + "test-classes";

    /**
     * Path of the sample test files directory.
     */
    public static final String SAMPLE_FILES_PATH = RESOURCES_PATH + File.separator + "files";

    /**
     * Path of the sample files hashcodes directory.
     */
    public static final String SAMPLE_FILES_HASHCODES_PATH = RESOURCES_PATH + File.separator + "hashcodes";
}
