### Copyright

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright (c) 2015 ForgeRock AS. All Rights Reserved

The contents of this file are subject to the terms
of the Common Development and Distribution License
(the License). You may not use this file except in
compliance with the License.

You can obtain a copy of the License at
http://forgerock.org/license/CDDLv1.0.html
See the License for the specific language governing
permission and limitations under the License.

When distributing Covered Code, include this CDDL
Header Notice in each file and include the License file
at http://forgerock.org/license/CDDLv1.0.html
If applicable, add the following below the CDDL Header,
with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

### About this package

This is a custom scripted connector bundler used to generate the boilerplate necessary to produce a
custom connector based on the ForgeRock OpenICF Groovy connector.  Having this code pre-generated for
you will save a lot of time and effort, accelerating the process of creating your own custom 
connector.  

When given a JSON configuration file this will produce all of the required source code to then build an 
OSGi-compatible jar file usable in any OpenICF-compliant project such as OpenIDM.  See below for a template 
example.  

For the connector to serve any useful purpose the generated Groovy templates must be populated with
code appropriate to the resource to be accessed.  The templates contain notes and, where appropriate,
"starter code" to aid in this process.

### Step by step

First obtain a copy of this project either in source or binary form.  If in source form first build 
the binary jar with 'mvn install'.

Place your JSON configuration file in a clean directory and execute the jar:

    $ java -jar custom-scripted-connector-bundler-<version>.jar -c <config.json>
    OpenICF Scripted Groovy Connector SourceGenerator v<version>
    Generating connector sources for <connector name>

This will create a directory tree of source files that can be used to build your custom connector.

Edit the Groovy templates found at ./src/main/resources/script/<connectorname>/*.groovy. For the connector
to do anything useful these must be enhanced with code to do the real work.

Now it's time to build your custom connector:

    $ mvn install

This will produce an OSGi-compatible jar in the ./target directory.  Copy this jar to your OpenICF-compatible
project or distribute it for others to use.

This jar contains all of the Groovy scripts which are executed directly from the jar.  These may optionally
be extracted to the filesystem and run from there with a change to the provisioner JSON file. Change:

        "scriptRoots" : [
            "jar:file:&{launcher.install.location}/connectors/awesome-connector-1.0.jar!/script/awesome/"
        ],
        "classpath" : [
            "jar:file:&{launcher.install.location}/connectors/awesome-connector-1.0.jar!/script/awesome/"
        ],

to:

        "scriptRoots" : [
            "file:&{launcher.project.location}/<your_extracted_script_location>/"
        ],
        "classpath" : [
            "file:&{launcher.project.location}/<your_extracted_script_location>/"
        ],

The OpenIDM provisioner file must be extracted to the filesystem as it presently cannot be detected and used
directly from within the jar:

    jar -xvf <filename>.jar conf/provisioner.openicf-*.json

### Sample configuration file

    {
        "packageName" : "Awesome",
        "displayName" : "Awesome Connector",
        "description" : "This is my super awesome connector",
        "baseConfigurationClass" : "ScriptedConfiguration",
        "baseConnectorClass" : "ScriptedConnectorBase",
        "version" : "1.0",
        "author" : "Coder McLightningfingers",
        "properties" : [
            {
                "order" : 0,
                "type" : "String",
                "name" : "FirstProperty",
                "value" : "firstValue",
                "required" : true,
                "confidential" : false,
                "displayMessage" : "This is my first property",
                "helpMessage" : "This should be a String value",
                "group" : "default"
            }, {
                "order" : 1,
                "type" : "Double",
                "name" : "SecondProperty",
                "value" : 1.234,
                "required" : false,
                "confidential" : false,
                "displayMessage" : "This is my second property",
                "helpMessage" : "This should be a Double value",
                "group" : "default"
            }
        ],
        "objectTypes" : [
            {
                "name" : "group",
                "id" : "__GROUP__",
                "type" : "object",
                "nativeType" : "__GROUP__",
                "objectClass" : "ObjectClass.GROUP_NAME",
                "properties" : [
                    {
                        "name" : "name",
                        "type" : "string",
                        "required" : true,
                        "nativeName" : "__NAME__",
                        "nativeType" : "string"
                    },{
                        "name" : "gid",
                        "type" : "string",
                        "required" : true,
                        "nativeName" : "gid",
                        "nativeType" : "string"
                    },{
                        "name" : "description",
                        "type" : "string",
                        "required" : false,
                        "nativeName" : "description",
                        "nativeType" : "string"
                    },{
                        "name" : "users",
                        "type" : "array",
                        "nativeName" : "users",
                        "nativeType" : "object",
                        "items" : [
                            {
                                "type" : "object",
                                "properties" : [{
                                    "name" : "uid",
                                    "type" : "string"
                                }]
                            }
                        ]
                    }
                ]
            },{
                "name" : "account",
                "id" : "__ACCOUNT__",
                "type" : "object",
                "nativeType" : "__ACCOUNT__",
                "objectClass" : "ObjectClass.ACCOUNT_NAME",
                "properties" : [
                    {
                        "name" : "firstName",
                        "type" : "string",
                        "nativeName" : "firstname",
                        "nativeType" : "string",
                        "required" : true
                    },{
                        "name" : "email",
                        "type" : "string",
                        "nativeName" : "email",
                        "nativeType" : "string"
                    },{
                        "name" : "password",
                        "type" : "string",
                        "nativeName" : "password",
                        "nativeType" : "string",
                        "flags" : [
                            "NOT_READABLE",
                            "NOT_RETURNED_BY_DEFAULT"
                        ]
                    },{
                        "name" : "uid",
                        "type" : "string",
                        "nativeName" : "__NAME__",
                        "required" : true,
                        "nativeType" : "string"
                    },{
                        "name" : "fullName",
                        "type" : "string",
                        "nativeName" : "fullname",
                        "nativeType" : "string"
                    },{
                        "name" : "lastName",
                        "type" : "string",
                        "required" : true,
                        "nativeName" : "lastname",
                        "nativeType" : "string"
                    }
                ]
            }
        ]
    }
    
### Base classes 
    The possible values for the param baseConfigurationClass are:
         1. ScriptedConfiguration
         2. ScriptedCRESTConfiguration
         3. ScriptedRESTConfiguration
         4. ScriptedSQLConfiguration

    The possible values for the param baseConnectorClass are:
         1. ScriptedConnector
         2. ScriptedCRESTConnector
         3. ScriptedRESTConnector
         4. ScriptedSQLConnector

    Keep in mind that the configuration base classes should match the connector base classes. So for example, if you use a
    baseConfigurationClass as "ScriptedCRESTConfiguration" then the only option that will work for baseConnectorClass is the
    corresponding ScriptedCRESTConnector base class.

### Other resources

1. [Groovy](http://groovy-lang.org)
2. [OpenICF](http://openicf.forgerock.org)








