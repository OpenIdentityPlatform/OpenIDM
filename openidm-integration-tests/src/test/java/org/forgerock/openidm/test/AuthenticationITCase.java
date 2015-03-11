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
import static com.jayway.restassured.RestAssured.*;
import com.jayway.restassured.response.*;


import org.testng.annotations.Test;
import static org.hamcrest.Matchers.*;


import static com.jayway.restassured.RestAssured.given;

/**
 * Integration tests for Authentication
 *
 */
@Test(groups = { "common" })
public class AuthenticationITCase {

    @Test(groups="authentication")
    public void anonymousGet() throws Exception {
        // Anonymous credential can not be used to GET
        given().
                auth().basic("anonymous", "anonymous").
        expect().
                body("error", equalTo(401)).
        when().
                get("/openidm/managed/user?_queryId=query-all-ids");

    }

    @Test(groups="authentication")
    public void anonymousPost() throws Exception {
        // Anonymous credential can be used to POST in the repo
        String json =
                given().
                        headers("X-OpenIDM-Username", "anonymous","X-OpenIDM-Password", "anonymous","Content-Type", "application/json").
                request().
                        body("{\"userName\":\"djoe\", \"givenName\":\"Joe\",\"familyName\":\"Doe\", \"email\":\"joe@forgerock.com\",\"password\":\"ldap12345\"}").
                expect().
                        statusCode(201).
                when().
                        post("/openidm/managed/user?_action=create").asString();
        JsonPath jp = new JsonPath(json);
        String userID = jp.get("_id");
        if(userID == null) {
            throw new IllegalArgumentException("Test Failed(AnonyPost): No result was returned");
        }
        // we clean the repo after the test
        given().
                headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin", "If-Match", "*").
        pathParam("id", userID).
        expect().
                statusCode(204).
        when().
                delete("/openidm/managed/user/{id}");
    }

    @Test(groups="authentication")
    public void noCredential() throws Exception {
        // W/o authentication, query should fail.
        expect().
                body("error", equalTo(401)).
        get("/openidm/managed/user?_queryId=query-all-ids");
    }

    @Test(groups="authentication")
    public void basicAuthentication() throws Exception {
        // basic authentication should return an empty list of users
            expect().
                    statusCode(200).
            when().
            with().
                    headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password","openidm-admin").
            get("/openidm/managed/user?_queryId=query-all-ids").asString();
    }

    @Test(groups="authentication")
    public void useCookie() throws Exception {
        // authentication with cookie
        Response response =
                given().
                        headers("X-OpenIDM-Username", "openidm-admin","X-OpenIDM-Password", "openidm-admin").
                when().
                        get("/openidm/managed/user?_queryId=query-all-ids");

        // Get a single cookie value:
        String cookieValue = response.cookie("JSESSIONID");

        given().
                cookie("JSESSIONID",cookieValue).
        expect().
                statusCode(200).
        when().
                get("/openidm/managed/user?_queryId=query-all-ids").asString();


    }

}
