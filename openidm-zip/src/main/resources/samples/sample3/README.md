    /**
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
     *
     * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
     *
     * The contents of this file are subject to the terms
     * of the Common Development and Distribution License
     * (the License). You may not use this file except in
     * compliance with the License.
     *
     * You can obtain a copy of the License at
     * http://forgerock.org/license/CDDLv1.0.html
     * See the License for the specific language governing
     * permission and limitations under the License.
     *
     * When distributing Covered Code, include this CDDL
     * Header Notice in each file and include the License file
     * at http://forgerock.org/license/CDDLv1.0.html
     * If applicable, add the following below the CDDL Header,
     * with the fields enclosed by brackets [] replaced by
     * your own identifying information:
     * "Portions Copyrighted [year] [name of copyright owner]"
     */

Sample 3 - Scripted SQL
=======================

This sample demonstrates creating a new custom scriptedSQL connector, using the
custom-scripted-connector-bundler-4.0.0-SNAPSHOT.jar that is included in the
tools directory of the OpenIDM zip file. The sample relies on the new custom
connector that you will create with the connector bundler. It provides an
example configuration and a handful of Groovy scripts that are used to
communicate with an SQL server.

This example requires a fresh installation of OpenIDM. It also requires that you 
have Maven installed. 

For documentation pertaining to this example see:
https://forgerock.org/openidm/doc/bootstrap/samples-guide/index.html#more-sample3

This sample also demonstrates the use of complex data types. Complex types can
be stored, retrieved and synced like any other property of an object. These
types can be mapped to your external data sources in any way you choose but are
generally stored in the managed data as JSON represented as a String. This may
be customized further to do additional work with or transformation on that data.

The sync.json file demonstrates the use of event hooks to perform an action. In
this example there are two hooks, one for the onCreate event and another for onUpdate,
both for the managed user to external repo mapping. In both events this sample
will log a statement to OpenIDM's log file (see the logs directory) when a managed
user is created or updated in the external repo. In both cases the script is
explicitly included in the sync.json file but could just as easily have referenced
an external file for the script source instead. For more information see:

https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#chap-scripting

The scripted connector supports any number of custom scripted endpoints. These are
configured via the provisioner script and currently support only Groovy. See
provisioner.openicf-scriptedsql.json and tools/ResetDatabaseScript.groovy for a
sample implementation. Step 5 below executes this script.

CAVEAT: Because MySQL cannot "un-hash" user passwords, there is no way for a recon
to retrieve and store the password in the managed user object in OpenIDM. This may
impact configurations that support multiple external repositories in that
passwords are unlikely to be in sync immediately after a mysql -> managed recon.
Despite creating any missing users in the managed repo during reconciliation,
their passwords are empty, so those same users synchronized to the other
external repositories will have blank passwords. Some additional scripting may
be required to handle this situation, depending on the requirements of your
deployment.


Setup the Database
------------------

1. Copy the MySQL Connector/J .jar to the OpenIDM bundle/ directory.

    $ cp mysql-connector-java-<version>-bin.jar /path/to/openidm/bundle/

2. Set up MySQL to listen on localhost:3306, connecting as root:password.

3. Create the initial database OpenIDM will sync with.

    mysql> CREATE DATABASE hrdb CHARACTER SET utf8 COLLATE utf8_bin;


Create the CustomScriptedSQL Connector
--------------------------------------

Description: In this section, you generate the classes and files necessary to
build a custom connector. Using these generated files, you build the custom
ScriptedSQL connector that will be used in the rest of this sample.

1. Create a sample3/create-connector directory, and run the following command from
   that directory, using  the custom config provided in sample3/data:

   $ mkdir path/to/openidm/samples/sample3/create-connector
   $ cd path/to/openidm/samples/sample3/create-connector
   $ java -jar ../../../tools/custom-scripted-connector-bundler-4.0.0-SNAPSHOT.jar -c ../data/scriptedsql.json

2. Copy the provided sample scripts into the connector src directory; these will
   become part of the custom connector.

    $ cp ../tools/* src/main/resources/script/hrdb/
    
   If you need to modify the scripts provided, now is the time to do so.

3. Build the custom connector. (You should be in the sample3/create-connector
   directory.)

    $ mvn install

4. Change directory up one directory to the sample's main directory, /path/to/openidm/samples/sample3.

    $ cd ..

5. Copy the connector that you created from the create-connector directory to
   the connectors directory of OpenIDM. If you are in the sample3 directory, run
   this command:
   
   $ cp create-connector/target/hrdb-connector-1.4.1.0.jar ../../connectors/
   
   At this point you have a connector that is ready to be used in OpenIDM. This
   connector has all the necessary files that will allow it to be displayed in
   the UI. It also has both the scripts and provisioner config that allow it to
   be used.

6. Load the provisioner.openicf-hrdb.json file into the sample3/conf directory
   so that it can be used.

    $ jar -xvf ../../connectors/hrdb-connector-1.4.1.0.jar conf/provisioner.openicf-hrdb.json
    
    The output should show that the file has been inflated:
    
        inflated: conf/provisioner.openicf-hrdb.json
        
7. Replace the "systemActions" value inside the sample3/conf/provisioner.openicf-hrdb.json
   with the following value:
        [
            {
                "scriptId" : "ResetDatabase",
                "actions" : [
                    {
                        "systemType" :  ".*HRDBConnector",
                        "actionType" : "Groovy",
                        "actionFile" : "tools/ResetDatabaseScript.groovy"
                    }
                ]
            }
        ]

8. For the purposes of demonstrating this sample, copy the generated html
   template file associated with this connector. This file will be used to
   display the connector in the UI.
    
   Inside the connector jar that you created, search for a file containing
   "1.4.html".
    
        $ cd path/to/openidm
        $ jar -tvf connectors/hrdb-connector-1.4.1.0.jar | grep "1.4.html"
    
   Create a new extension directory for the connector template and extract 
   the file that you found in the preceding step into that directory.
        
        $ mkdir -p ui/extension/templates/admin/connector
        $ jar -xvf connectors/hrdb-connector-1.4.1.0.jar ui/org.forgerock.openicf.connectors.hrdb.HRDBConnector_1.4.html


Starting up the sample
----------------------

1. Start OpenIDM with the configuration for sample 3.

    $ /path/to/openidm/startup.sh -p samples/sample3

2. Populate the MySQL database with sample data. Use REST to execute a custom
   script that, in this case, resets and populates the database.  This script
   may be re-run at any time to reset the database.

   $ curl -k --header "X-OpenIDM-Username: openidm-admin" --header "X-OpenIDM-Password: openidm-admin" --header "Content-Type: application/json" --request POST "https://localhost:8443/openidm/system/hrdb?_action=script&scriptId=ResetDatabase"
       {
         "actions": [
           {
             "result": "Database reset successful."
           }
         ]
       }

   At this point the MySQL database should be fully populated.

   mysql> USE hrdb;
   Database changed
   mysql> SELECT * FROM users;
   +----+--------+------------------------------------------+-----------+----------+---------------+---------------------------+--------------+---------------------+
   | id | uid    | password                                 | firstname | lastname | fullname      | email                     | organization | timestamp           |
   +----+--------+------------------------------------------+-----------+----------+---------------+---------------------------+--------------+---------------------+
   |  1 | bob    | e38ad214943daad1d64c102faec29de4afe9da3d | Bob       | Fleming  | Bob Fleming   | Bob.Fleming@example.com   | HR           | 2014-10-30 08:55:41 |
   |  2 | rowley | 2aa60a8ff7fcd473d321e0146afd9e26df395147 | Rowley    | Birkin   | Rowley Birkin | Rowley.Birkin@example.com | SALES        | 2014-10-30 08:55:41 |
   |  3 | louis  | 1119cfd37ee247357e034a08d844eea25f6fd20f | Louis     | Balfour  | Louis Balfour | Louis.Balfor@example.com  | SALES        | 2014-10-30 08:55:41 |
   |  4 | john   | a1d7584daaca4738d499ad7082886b01117275d8 | John      | Smith    | John Smith    | John.Smith@example.com    | SUPPORT      | 2014-10-30 08:55:41 |
   |  5 | jdoe   | edba955d0ea15fdef4f61726ef97e5af507430c0 | John      | Doe      | John Doe      | John.Doe@example.com      | ENG          | 2014-10-30 08:55:41 |
   +----+--------+------------------------------------------+-----------+----------+---------------+---------------------------+--------------+---------------------+
   5 rows in set (0.00 sec)

   * Note that these passwords are hashed, and not available to be read into
     OpenIDM as cleartext.
   * sha1 is used to hash these passwords for compatibility reasons; in
     production, use more secure algorithms.


Run the Sample
--------------

1. Run reconciliation:

    $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST "https://localhost:8443/openidm/recon?_action=recon&mapping=systemHrdb_managedUser"

2. Retrieve the list of users from OpenIDM's internal repository:

    $ curl -k -u "openidm-admin:openidm-admin" --request GET "https://localhost:8443/openidm/managed/user/?_queryId=query-all-ids&_prettyPrint=true"

    {
      "remainingPagedResults": -1,
      "pagedResultsCookie": null,
      "resultCount": 5,
      "result": [
        {
          "_rev": "0",
          "_id": "9ee75d24-899c-4221-97a0-0aca298febd6"
        },
        {
          "_rev": "0",
          "_id": "25be1c4c-0c2a-48b6-96f6-58d2e4d2357d"
        },
        {
          "_rev": "0",
          "_id": "2850c77b-f51a-4fb2-8cc4-4c1d03108ac2"
        },
        {
          "_rev": "0",
          "_id": "126d74e1-1c03-4774-b18d-bd4d1dfdf884"
        },
        {
          "_rev": "0",
          "_id": "46570045-0644-45c6-af10-c88ad3df93cc"
        }
      ]
    }

3. Query for an individual user (by userName):

    $ curl -k -u "openidm-admin:openidm-admin" --request GET "https://localhost:8443/openidm/managed/user?_queryId=for-userName&uid=rowley&_prettyPrint=true"

    {
      "result" : [ {
        "mail" : "Rowley.Birkin@example.com",
        "sn" : "Birkin",
        "passwordAttempts" : "0",
        "lastPasswordAttempt" : "Wed Oct 22 2014 09:51:31 GMT-0700 (PDT)",
        "address2" : "",
        "givenName" : "Rowley",
        "effectiveRoles" : [ ],
        "country" : "",
        "city" : "",
        "lastPasswordSet" : "",
        "organization" : "SALES",
        "postalCode" : "",
        "_id" : "ed8bbe46-08a1-4716-9d5f-3cc5d09e2a7c",
        "_rev" : "1",
        "cars" : [ {
          "make" : "BMW",
          "year" : "2013",
          "model" : "328ci"
        }, {
          "make" : "Lexus",
          "year" : "2010",
          "model" : "ES300"
        } ],
        "accountStatus" : "active",
        "telephoneNumber" : "",
        "roles" : [ "openidm-authorized" ],
        "effectiveAssignments" : [ ],
        "postalAddress" : "",
        "userName" : "rowley",
        "stateProvince" : ""
      } ],
      "resultCount" : 1,
      "pagedResultsCookie" : null,
      "remainingPagedResults" : -1
    }

    Note the "cars" list containing multiple objects. This structure is
    displayed in the admin UI as well. The name "cars" was used to help
    differentiate what matters to the complex type versus what is required
    by OpenIDM/OpenICF in the Groovy scripts.

    In the database the 'car' table joins to the 'users' table via the
    cars.users_id column. The Groovy scripts are responsible for reading this
    data from MySQL and repackaging it in a way that OpenIDM can understand.
    With support for complex types this data is passed through to OpenIDM in
    the same form: as a list of 'car' objects. Likewise, data is synced from
    OpenIDM to MySQL in the same form.

    Group membership (not shown here) is maintained with a traditional
    "join table" in MySQL ('groups_users'). OpenIDM does not maintain group
    membership this way so the Groovy scripts do the work to translate between
    the two. This demonstrates another form of complex object though the sky
    is the limit. Complex objects may also be nested to any depth.

4. Show paging results with page size of 2

    $ curl -k -u "openidm-admin:openidm-admin" --request GET 'https://localhost:8443/openidm/system/hrdb/account?_queryFilter=uid+sw+%22%22&_pageSize=2&_sortKeys=timestamp,id'

    {
      "result":[
        {
          "uid":"bob",
          "_id":"1"
        },
        {
          "uid":"rowley",
          "_id":"2"
        } ],
      "resultCount":2,
      "pagedResultsCookie":"2014-09-11 10:07:57.0,2",
      "remainingPagedResults":-1
    }

5. Use the pagedResultsCookie from the result in step 10 for the next query to
   retrieve the next result set. The value of the pagedResultsCookie must be 
   URL-encoded.

    $ curl -k -u "openidm-admin:openidm-admin" --request GET 'https://localhost:8443/openidm/system/hrdb/account?_queryFilter=uid+sw+%22%22&_pageSize=2&_sortKeys=timestamp,id&_pagedResultsCookie=2014-09-11%2010%3A07%3A57.0%2C2'

    {
      "result":[
        {
          "uid":"louis",
          "_id":"3"
        },
        {
          "uid":"john",
          "_id":"4"
        }],
      "resultCount":2,
      "pagedResultsCookie":"2014-09-11 10:07:57.0,4",
      "remainingPagedResults":-1
    }
