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

# Sample 4 - CSV File to XML File

This sample demonstrates using both a comma-separated value file and an XML
file as external resources. The synchronization mapping is directly from the 
CSV file to the XML file, without using the OpenIDM internal repository. 
Changes flow only from the CSV to XML. Data exists in both files, and
correlation is used to find matching records.

For documentation pertaining to this example see : [Sample 4](http://openidm.forgerock.org/doc/install-guide/index.html#more-sample4)

To try the sample, follow these steps.

#####Step 1
CSV data for this sample is in the file samples/sample4/data/hr.csv.

    $ cd /path/to/openidm
    $ cat samples/sample4/data/hr.csv
    "firstName", "uid", "lastName", "email", "employeeNumber"
    "Don", "DDOE", "Doe", "doe@example.org", "123456"
    "Stephen", "SCARTER", "Carter", "scarter@example.com", "654321"

XML data for this sample is in the file samples/sample4/data/xmlConnectorData.xml

    $ cd /path/to/openidm
    $ cat samples/sample4/data/xmlConnectorData.xml

    <?xml version="1.0" encoding="UTF-8"?>
    <icf:OpenICFContainer xmlns:icf="http://openidm.forgerock.com/xml/ns/public/resource/openicf/resource-schema-1.xsd"
                          xmlns:ri="http://openidm.forgerock.com/xml/ns/public/resource/instances/resource-schema-extension"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://openidm.forgerock.com/xml/ns/public/resource/instances/resource-schema-extension data/resource-schema-extension.xsd http://openidm.forgerock.com/xml/ns/public/resource/openicf/resource-schema-1.xsd data/resource-schema-1.xsd">
       <ri:__ACCOUNT__>
          <icf:__UID__>bjensen</icf:__UID__>
          <icf:__NAME__>bjensen@example.com</icf:__NAME__>
          <ri:firstname>Barbara</ri:firstname>
          <ri:lastname>Jensen</ri:lastname>
          <ri:email>bjensen@example.com</ri:email>
          <ri:mobileTelephoneNumber>1234567</ri:mobileTelephoneNumber>
          <ri:roles>openidm-authorized</ri:roles>
          <icf:__DESCRIPTION__>Created By XML1</icf:__DESCRIPTION__>
       </ri:__ACCOUNT__>
       <ri:__ACCOUNT__>
          <icf:__UID__>scarter</icf:__UID__>
          <icf:__NAME__>scarter@example.com</icf:__NAME__>
          <ri:firstname>Steven</ri:firstname>
          <ri:lastname>Carter</ri:lastname>
          <ri:email>scarter@example.com</ri:email>
          <ri:mobileTelephoneNumber>1234567</ri:mobileTelephoneNumber>
          <ri:roles>openidm-admin,openidm-authorized</ri:roles>
          <icf:__DESCRIPTION__>Created By XML1</icf:__DESCRIPTION__>
       </ri:__ACCOUNT__>
    </icf:OpenICFContainer>


Note that the last entry in that file has the same email address (scarter@example.com) as an entry in the CSV. This will be used by correlation to find and update the record.

#####Step 2
Start OpenIDM with the configuration for sample 4.

    $ ./startup.sh -p samples/sample4

#####Step 3
Run reconciliation.

    $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST "https://localhost:8443/openidm/recon?_action=recon&mapping=csv_xmlfile"
    {"_id":"84b55592-a2d5-438a-ba71-c5e9a7a93938","state":"ACTIVE"}

#####Step 4
See the data updated in the XML file:

    $ cd /path/to/openidm
    $ cat samples/sample4/data/xmlConnectorData.xml

    <?xml version="1.0" encoding="UTF-8"?>
    <icf:OpenICFContainer xmlns:icf="http://openidm.forgerock.com/xml/ns/public/resource/openicf/resource-schema-1.xsd"
                          xmlns:ri="http://openidm.forgerock.com/xml/ns/public/resource/instances/resource-schema-extension"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://openidm.forgerock.com/xml/ns/public/resource/instances/resource-schema-extension data/resource-schema-extension.xsd http://openidm.forgerock.com/xml/ns/public/resource/openicf/resource-schema-1.xsd data/resource-schema-1.xsd">
       <ri:__ACCOUNT__>
          <icf:__UID__>scarter</icf:__UID__>
          <ri:mobileTelephoneNumber>N/A</ri:mobileTelephoneNumber>
          <ri:firstname>Stephen</ri:firstname>
          <icf:__DESCRIPTION__>Created By XML1</icf:__DESCRIPTION__>
          <ri:roles>openidm-admin,openidm-authorized</ri:roles>
          <icf:__NAME__>scarter@example.com</icf:__NAME__>
          <ri:email>scarter@example.com</ri:email>
          <ri:lastname>Carter</ri:lastname>
       </ri:__ACCOUNT__>
       <ri:__ACCOUNT__>
          <ri:mobileTelephoneNumber>N/A</ri:mobileTelephoneNumber>
          <ri:firstname>Don</ri:firstname>
          <icf:__DESCRIPTION__/>
          <ri:roles>openidm-authorized</ri:roles>
          <icf:__UID__>492f28f8-dc03-43e1-aaed-abd33f265ecb</icf:__UID__>
          <icf:__NAME__>doe@example.org</icf:__NAME__>
          <ri:email>doe@example.org</ri:email>
          <ri:lastname>Doe</ri:lastname>
       </ri:__ACCOUNT__>
    </icf:OpenICFContainer>


Note that Carter got updated, Doe got created, and Jensen got deleted. This is all based on the policies declared in sync.json.

You can also try changing data in either the CSV or the XML and running recon again. You should see the changes you make in the CSV used as authoritative, overriding any changes you make in the XML and the two remaining in sync.

These users will not be visible from the OpenIDM UI, since they are mapped directly rather than via the internal OpenIDM repository.

# Sample 4 through the Administration UI

Similar to Sample 1, the steps in this sample can be performed through the Administration UI. For more details please consult the [Installation Guide](http://openidm.forgerock.org/doc/install-guide/index.html).