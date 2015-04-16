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


#### Groovy Script Location

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


#### OpenIDM Provisioner Configuration

The OpenIDM provisioner file must be extracted to the filesystem as it presently cannot be detected and used
directly from within the jar.  To identify the filename grep the jar's catalog:

    jar -tvf <filename>.jar | grep "provisioner*json"

The file should appear in a conf/ directory.  Next extract the file to the conf directory on your local filesystem:

    jar -xvf <filename>.jar <provisioner filename>
    

#### OpenIDM UI Connector Template

The Admin UI provides a default template for configuration of connectors that lack a specialized template. If
you prefer to have a specialized template to use with your new connector there is one in the connector bundle
for this purpose. Because the UI template is named after this connector you must first identify the file:

    jar -tvf <filename>.jar | grep "1.4.html"
    
Using the sample configuration this will be named:

    ui/org.forgerock.openicf.connectors.awesome.AwesomeConnector_1.4.html
    
Next extract the file from the connector jar (using the example above):

    jar -xvf ui/org.forgerock.openicf.connectors.awesome.AwesomeConnector_1.4.html
    
Then move it to the correct directory for the UI to find it:

    mv ui/org.forgerock.openicf.connectors.awesome.AwesomeConnector_1.4.html \
    ui/default/admin/public/templates/admin/connector/


### Sample configuration file

    {
        "packageName" : "Awesome",
        "displayName" : "Awesome Connector",
        "description" : "This is my super awesome connector",
        "baseConnectorType" : "GROOVY",
        "version" : "1.0",
        "author" : "Coder McLightningfingers",
        "providedProperties" : [ {
            "name" : "provided1",
            "value" : "default",
            "type" : "String"
        }, {
            "name" : "provided2",
            "value" : 2,
            "type" : "Integer"
        } ],
        "properties" : [ {
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
        } ],
        "objectTypes" : [ {
            "name" : "group",
            "id" : "__GROUP__",
            "type" : "object",
            "nativeType" : "__GROUP__",
            "objectClass" : "ObjectClass.GROUP",
            "properties" : [ {
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
            } ]
        },{
            "name" : "account",
            "id" : "__ACCOUNT__",
            "type" : "object",
            "nativeType" : "__ACCOUNT__",
            "objectClass" : "ObjectClass.ACCOUNT",
            "properties" : [ {
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
            } ]
        } ]
    }
    
#### Base connector types 
    The possible values for the param baseConnectorType are:
         1. GROOVY              Non-pooled Groovy connector
         2. POOLABLEGROOVY      Poolable Groovy connector
         2. CREST               CREST-based connector
         3. REST                REST-based connector
         4. SQL                 Connector with SQL support

#### ProvidedProperties vs. Properties
    ProvidedProperties are those already provided by the base configuration class for your connector.  For example,
    if you choose SQL as your connector type then ScriptedSQLConfiguration will be your base configuration class. This
    base class already provides properties for "username", "password", etc.  There is no need for you to define them 
    yourself but you will need to provide a default value if you want them to show up in the provisioner file.  To do
    so simply add them as ProvidedProperties.
    
    The other type, Properties, defines new and unique properties you want your connector to support. This is where you
    define the attributes of each new custom property your connector will have that the base configuration class did
    not provide.

#### ObjectType -> ObjectClass
    The possible values for the objectClass in each objectType are:
        1. ObjectClass.ACCOUNT
        2. ObjectClass.GROUP
        3. ObjectClass.ALL
        4. The bundler will accept any arbitrary string but you may need to edit the resulting provisioner and
            Groovy scripts to work with this arbitrary string.

### Other resources

1. [Groovy](http://groovy-lang.org)
2. [OpenICF](http://openicf.forgerock.org)








