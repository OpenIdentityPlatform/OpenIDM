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

import net.nicoulaj.maven.plugins.checksum.Constants
import net.nicoulaj.maven.plugins.checksum.test.integration.PostBuildScriptHelper

try
{
  // Instantiate a helper.
  PostBuildScriptHelper helper = new PostBuildScriptHelper( basedir, localRepositoryPath, context )

  // Fail if no traces of checksum-maven-plugin invocation.
  helper.assertBuildLogContains( "checksum-maven-plugin" );

  // Check a line has been printed in the log for each algorithm.
  for ( String algorithm : Constants.SUPPORTED_ALGORITHMS )
  {
    helper.assertBuildLogContains( "[INFO] invoker.properties - " + algorithm + " : " );
  }

}
catch ( Exception e )
{
  System.err.println( e.getMessage() )
  return false;
}
