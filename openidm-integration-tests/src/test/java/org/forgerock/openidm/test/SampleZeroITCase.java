/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.test;


import java.util.*;


import org.testng.annotations.Test;
import com.jayway.restassured.path.json.JsonPath;
import static com.jayway.restassured.RestAssured.*;

/**
 * A NAME does ...
 *
 */
@Test(groups = { "sample0" })
public class SampleZeroITCase {

    @Test(dependsOnGroups = "readUser", groups="sampleZero")
    public void sampleZero() throws Exception {
        /* Query and Delete all the users */
        List<String> ids;
        String json =
                given().
                        headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
                        expect().
                        statusCode(200).
                        when().
                        get("/openidm/managed/user/?_queryId=query-all-ids").asString();
        JsonPath jp = new JsonPath(json);
        jp.setRoot("result");
        ids = jp.getList("_id");
        if(ids == null ) return;
        ListIterator<String> idList = ids.listIterator();
        while ( idList.hasNext()) {
            String id = idList.next();
            given().
                    headers("X-OpenIDM-Username", "openidm-admin","X-OpenIDM-Password", "openidm-admin", "If-Match", "*").
                    pathParam("id", id).
                    expect().
                    statusCode(204).
                    when().
                    delete("/openidm/managed/user/{id}");
        }

        // Launch Recon through REST API
        String recon =
                given().
                headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
        expect().
                statusCode(200).
        when().
                post("/openidm/recon?_action=recon&mapping=systemXmlfileAccounts_managedUser").asString();
        System.out.println(recon);
        //TODO parse the return value

        // Expect to find 2 users in the local repo
        String result =
                given().
                        headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
                expect().
                        statusCode(200).
                when().
                        get("/openidm/managed/user/?_queryId=query-all-ids").asString();
        System.out.println(result);


        jp = new JsonPath(result);
        jp.setRoot("result");
        ids = jp.getList("_id");

        //int size = ids.size();
        //samples/sample0/data not yet there, so recon does not add the 2 users in the repo
        //waiting for Laszlo modification to OpenIDM launcher to go on with the samples
    }

}
