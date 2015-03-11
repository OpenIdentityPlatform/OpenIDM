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


import com.jayway.restassured.path.json.JsonPath;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


import java.util.List;
import java.util.ListIterator;

import static com.jayway.restassured.RestAssured.given;

/**
 * Integration tests for Authentication
 *
 */
@Test(groups = { "common" })
public class ObjectManagementITCase {


    @BeforeClass
    @AfterClass
    public void cleanupUsers() throws Exception {
        /* Query and Delete all the users */
        List<String> ids;
        String json =
                given().
                        headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
                expect().
                        statusCode(200).
                when().
                        get("/openidm/managed/user/?_queryId=query-all-ids").asString();        JsonPath jp = new JsonPath(json);
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
    }

    @Test(dependsOnGroups = "authentication", groups = "addUser")
    public void createUserPut() throws Exception {
        // Addition of a user in the local repo using PUT
        given().
                headers("X-OpenIDM-Username", "openidm-admin","X-OpenIDM-Password", "openidm-admin").
        request().
                body("{\"userName\":\"joe\", \"givenName\":\"joe\", \"familyName\":\"smith\", \"email\":[\"joe@example.com\"], \"description\":\"My first user\"}").
        expect().
                statusCode(201).
        when().
                put("/openidm/managed/user/joe");
        //TODO parse the return value

    }

    @Test(dependsOnGroups = "authentication", groups = "addUser")
    public void createUserPost() throws Exception {
        // Addition of a user in the local repo using POST
        given().
                headers("X-OpenIDM-Username", "openidm-admin","X-OpenIDM-Password", "openidm-admin").
        request().
                body("{\"userName\":\"bill\", \"givenName\":\"bill\", \"familyName\":\"parker\", \"email\":[\"bill@example.com\"], \"description\":\"My second user\"}").
        expect().
                statusCode(201).
        when().
                post("/openidm/managed/user?_action=create");
        //TODO parse the return value
    }

    @Test(dependsOnGroups = "authentication", groups = "addUser")
    public void readAllUsers() throws Exception {
        // read users in the local repo
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
        int size = ids.size();
        assertEquals(size, 2);
    }

    @Test(dependsOnGroups = "addUser", groups = "readUser")
    public void readOneUser() throws Exception {
        // read a user in the local repo
                given().
                        headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
                expect().
                        statusCode(200).
                when().
                        get("/openidm/managed/user/joe").asString();
        //TODO parse the return value

    }

    @Test(dependsOnGroups = "addUser", groups = "readUser")
    public void readOneNonExistingUser() throws Exception {
        // read a user in the local repo
                given().
                        headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
                expect().
                        statusCode(404).
                when().
                        get("/openidm/managed/user/nonexisting").asString();
        //TODO parse the return value

    }



}
