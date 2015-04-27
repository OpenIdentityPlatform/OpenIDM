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

import net.nicoulaj.maven.plugins.checksum.test.integration.PostBuildScriptHelper

try
{
  // Instantiate a helper.
  PostBuildScriptHelper helper = new PostBuildScriptHelper( basedir, localRepositoryPath, context )

  // Fail if no traces of checksum-maven-plugin invocation.
  helper.assertBuildLogContains( "checksum-maven-plugin" );

  // Assert the file has been created.
  helper.assertFileIsNotEmpty( "target/dependencies-checksums.csv" )

  // Check there is a line for each dependency that matches the included scope(s).
  helper.assertFileContains( "target/dependencies-checksums.csv", "maven-plugin-api" )
  helper.assertFileContains( "target/dependencies-checksums.csv", "plexus-utils" )

  // Check the other ones are excluded.
  helper.assertFileDoesNotContain( "target/dependencies-checksums.csv", "maven-project" )
  helper.assertFileDoesNotContain( "target/dependencies-checksums.csv", "junit" )

}
catch ( Exception e )
{
  System.err.println( e.getMessage() )
  return false;
}
