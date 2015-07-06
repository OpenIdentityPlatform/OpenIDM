/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

# Sample 1 - One-Way XML -> Internal Repository

The documentation at http://openidm.forgerock.org/doc/install-guide/index.html#chap-sample
describes how to get started with this sample.

The sample demonstrates reconciliation between an external XML file and the
OpenIDM internal repository, with data flowing from the XML file into the
internal repository.

The configuration files used in this sample are as follows:

* samples/sample1/conf/provisioner.openicf-xml.json shows the XML connector
  configuration.
* samples/sample1/conf/schedule-reconcile_systemXmlAccounts_managedUser.json
  includes a schedule configuration you can use to run reconciliation
  periodically.
* samples/sample1/conf/sync.json describes how identities in the XML file
  source map to identities in the internal repository target.

Data for this sample is stored in samples/sample1/data/xmlConnectorData.xml.
The initial XML file contains two identities.

## Starting the sample
To run the sample, start OpenIDM with the configuration for sample 1:

    $ cd /path/to/openidm
    $ ./startup.sh -p samples/sample1

## Reconciling the sample data
Initiate a reconciliation operation over the REST interface, as follows:

    $ curl -k -H "Content-type: application/json" -u \
        "openidm-admin:openidm-admin" -X             \
        POST "https://localhost:8443/openidm/recon?_action=recon&mapping=systemXmlfileAccounts_managedUser"

Alternatively, edit

    samples/sample1/conf/schedule-reconcile_systemXmlAccounts_managedUser.json

to enable scheduled reconciliation:

    "enabled" : true,

## Retrieving sample data from the Internal Repository
The following curl command requests all identifiers in OpenIDM's internal
repository. Use it to see the results after reconciliation for example.


    $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/user?_queryId=query-all&fields=*&_prettyPrint=true"

    {
      "result" : [ {
        "mail" : "bjensen@example.com",
        "sn" : "Jensen",
        "passwordAttempts" : "0",
        "lastPasswordAttempt" : "Thu Oct 16 2014 21:06:21 GMT-0500 (CDT)",
        "address2" : "",
        "givenName" : "Barbara",
        "effectiveRoles" : [ ],
        "country" : "",
        "city" : "",
        "lastPasswordSet" : "",
        "postalCode" : "",
        "_id" : "bjensen",
        "_rev" : "6",
        "description" : "Created By XML1",
        "accountStatus" : "active",
        "telephoneNumber" : "1234567",
        "roles" : [ "openidm-authorized" ],
        "postalAddress" : "",
        "stateProvince" : "",
        "userName" : "bjensen@example.com",
        "effectiveAssignments" : null
      }, {
        "mail" : "scarter@example.com",
        "sn" : "Carter",
        "passwordAttempts" : "0",
        "lastPasswordAttempt" : "Thu Oct 16 2014 21:06:21 GMT-0500 (CDT)",
        "address2" : "",
        "givenName" : "Steven",
        "effectiveRoles" : [ ],
        "country" : "",
        "city" : "",
        "lastPasswordSet" : "",
        "postalCode" : "",
        "_id" : "scarter",
        "_rev" : "6",
        "description" : "Created By XML1",
        "accountStatus" : "active",
        "telephoneNumber" : "1234567",
        "roles" : [ "openidm-admin", "openidm-authorized" ],
        "postalAddress" : "",
        "stateProvince" : "",
        "userName" : "scarter@example.com",
        "effectiveAssignments" : null
      } ],
      "resultCount" : 2,
      "pagedResultsCookie" : null,
      "remainingPagedResults" : -1
    }

# Sample 1 through the Administration UI

By using the Admin UI you can go through the same exact steps depicted above, but through a point and click experience.
While the command line examples above are very useful to Developers, the visual experience provided by the Admin UI
is very powerful and easily understood by the Business Users of the system.

## Starting the sample
The initial steps are identical...
To run the sample, start OpenIDM with the configuration for sample 1:

    $ cd /path/to/openidm
    $ ./startup.sh -p samples/sample1

Now, though once OpenIDM has started, point your web browser to :
    https://localhost:8443/admin

In order to login you will need to provide the "default" credentials :

    username : openidm-admin
    password : openidm-admin
    
After successfully logging in you will be asked to change your password for security reasons (as the default password
is insecure). Note that you will be asked initially to accept the self-signed certificate created during OpenIDM's
initial startup.

The Resources tab shows the XML Connector in the Active state. By clicking on the little pencil you can take a look
at the connector details (the XML file path, etc.). Click "Cancel".

Next go to the Mappings tab. Click on the mapping representing the mapping definition between the XML file connector
and the Internal Repository (Managed Users). The details of the Mapping will be displayed and you can see the attribute
mapping as defined in the sync.json included with the sample.

## Reconciling the sample data
In the Mappings tab, click on the "Sync Now" button. You should briefly see a progress bar showing the reconciliation
taking place. Once done, you can click on the "Last Synced...." link to see the result of the reconciliation.

In order to see the User Association results click on the Correlation sub-menu in the Mappings tab. This table shows
the outcome of the reconciliation by grouping entries in different situation categories and the matching entries which
have been linked, based on the association query that was defined -- in this case you can see the inline script that
was defined as part of the sample :

    var query = {'_queryId' : 'for-userName', 'uid' : source.name};query;

This association rule can be changed to use the Expression Builder by specifying the following expression :

    (select) All of the below fields ==> (add) userName
    
which will translate to the following in the sync.json :

                "correlationQuery" : {
                    "type" : "text/javascript",
                    "expressionTree" : {
                        "all" : [
                            "userName"
                        ]
                    },
                    "mapping" : "systemXmlfileAccounts_managedUser",
                    "file" : "ui/correlateTreeToQueryFilter.js"
                },
   
## Retrieving sample data from the Internal Repository
In order to see the data which was extracted from the XML file and pushed into the Internal Repository, just click on
the User View link at the top right of the Admin UI. Your web browser should now be showing the User
Management screen and by clicking on the Users tab you should see the 2 users from the XML file (bjensen and
scarter).