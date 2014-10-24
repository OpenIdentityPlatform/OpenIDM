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

#Sample 1 - One-Way XML -> Internal Repository

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

To run the sample, start OpenIDM with the configuration for sample 1:

    $ cd /path/to/openidm
    $ ./startup.sh -p samples/sample1

Initiate a reconciliation operation over the REST interface, as follows:

    $ curl -k -H "Content-type: application/json" -u \
        "openidm-admin:openidm-admin" -X             \
        POST "https://localhost:8443/openidm/recon?_action=recon&mapping=systemXmlfileAccounts_managedUser"

Alternatively, edit

    samples/sample1/conf/schedule-reconcile_systemXmlAccounts_managedUser.json

to enable scheduled reconciliation:

    "enabled" : true,

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
        "effectiveRoles" : [ "openidm-authorized" ],
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
        "effectiveRoles" : [ "openidm-admin", "openidm-authorized" ],
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
    