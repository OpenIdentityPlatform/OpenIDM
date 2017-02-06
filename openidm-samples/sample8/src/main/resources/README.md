/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2017 ForgeRock AS.
 */

Logging in Scripts Sample
=========================

This sample demonstrates logging capabilities available to OpenIDM scripts,
providing you an alternative method for debugging your scripts.

For documentation pertaining to this example see:
https://forgerock.org/openidm/doc/bootstrap/samples-guide/#more-sample-8

To try the sample, follow these steps.

1. Start OpenIDM with the configuration for sample 8.

        $ cd /path/to/openidm
        $ ./startup.sh -p samples/sample8

2. Run reconciliation.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST "https://localhost:8443/openidm/recon?_action=recon&mapping=systemCsvfileAccounts_managedUser"

3. Observe messages from your scripts that are logged to the OpenIDM (Felix) console:

        2017-02-09 09:15:42:748 WARN Case no Source: the source object contains: = null  [9A00348661C6790E7881A7170F747F26513E0FB0]
        2017-02-09 09:15:42:750 WARN Case emptySource: the source object contains: = {roles=openidm-authorized, lastname=Jensen, firstname=Barbara, _id=bjensen, name=bjensen, email=bjensen@example.com}  [1CCB687F8B6FC7071BCCC8ABF8CD502F8147D31C]
        2017-02-09 09:15:42:750 WARN Case no Source: the source object contains: = null  [9A00348661C6790E7881A7170F747F26513E0FB0]
        2017-02-09 09:15:42:751 WARN Case emptySource: the source object contains: = {roles=openidm-admin,openidm-authorized, lastname=Carter, firstname=Steven, _id=scarter, name=scarter, email=scarter@example.com}  [1CCB687F8B6FC7071BCCC8ABF8CD502F8147D31C]
        2017-02-09 09:15:42:751 WARN Case sourceDescription: the source object contains: = null  [EEE2FF4BCE9748927A18327683CF560B33ED3F00]
        2017-02-09 09:15:42:752 WARN Case sourceDescription: the source object contains: = null  [EEE2FF4BCE9748927A18327683CF560B33ED3F00]
        2017-02-09 09:15:42:777 WARN Case onCreate: the source object contains: = {roles=openidm-admin,openidm-authorized, lastname=Carter, firstname=Steven, _id=scarter, name=scarter, email=scarter@example.com}  [9187A6862B4535E68FE75E21E7158E63BB435505]
        2017-02-09 09:15:42:777 WARN Case onCreate: the source object contains: = {roles=openidm-authorized, lastname=Jensen, firstname=Barbara, _id=bjensen, name=bjensen, email=bjensen@example.com}  [9187A6862B4535E68FE75E21E7158E63BB435505]
        2017-02-09 09:15:43:096 WARN Case result: the source object contains: = {SOURCE_IGNORED={count=0, ids=[]}, FOUND_ALREADY_LINKED={count=0, ids=[]}, UNQUALIFIED={count=0, ids=[]}, ABSENT={count=2, ids=[scarter, bjensen]}, TARGET_IGNORED={count=0, ids=[]}, MISSING={count=0, ids=[]}, duration=487, NOTVALID={count=0, ids=[]}, processed=2, UNASSIGNED={count=0, ids=[]}, entries=2, AMBIGUOUS={count=0, ids=[]}, CONFIRMED={count=0, ids=[]}, name=system/csvfile/account, startTime=2017-02-09T17:15:42.603Z, SOURCE_MISSING={count=0, ids=[]}, endTime=2017-02-09T17:15:43.090Z, entryListDuration=19, FOUND={count=0, ids=[]}}  [1F60DF2E4760B496FFD5716FBC87C1A071798F2E]
        
Read the short scripts inline within samples/sample8/conf/sync.json to see examples of how to use the logger object.
