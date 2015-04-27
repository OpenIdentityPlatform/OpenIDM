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

  // Check files have been created and are not empty.
  String summaryFile = "target/artifacts-checksums.csv";
  helper.assertFileIsNotEmpty( summaryFile )

  // Check there are traces of each algorithm.
  for ( String algorithm : Constants.DEFAULT_EXECUTION_ALGORITHMS )
  {
    helper.assertFileContains( summaryFile, algorithm );
  }

  // Check there are traces of each file.
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT.jar" )
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT-bin.tar.bz2" )
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT-bin.tar.gz" )
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT-bin.zip" )
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT-src.tar.bz2" )
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT-src.tar.gz" )
  helper.assertFileContains( summaryFile, "attached-artifacts.artifacts.csv-summary-file-1.0-SNAPSHOT-src.zip" )

}
catch ( Exception e )
{
  System.err.println( e.getMessage() )
  return false;
}
