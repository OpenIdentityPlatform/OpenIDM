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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.osgi.framework.BundleContext;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import org.forgerock.openidm.test.module.ModuleFactory;
import org.forgerock.openidm.test.module.OpenIDMTestModule;
import org.testng.internal.Nullable;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
@Guice(moduleFactory = ModuleFactory.class)
public class TaskSchedulerITCase {

    @Inject
    @Named(OpenIDMTestModule.ROUTER)
    private JsonResourceAccessor accessor;

    @Inject
    private BundleContext context;

    @Test()
    public void restTest() {
        List<String> ids = null;
        String json =
        given().
                headers("X-OpenIDM-Username", "openidm-admin", "X-OpenIDM-Password", "openidm-admin").
        expect().
                statusCode(200).
        when().
                get("/openidm/managed/user/?_query-id=query-all-ids").asString();
        JsonPath jp = new JsonPath(json);
        jp.setRoot("result");
        ids = jp.getList("_id");
        if(null == ids ) return;
        ListIterator<String> idList = ids.listIterator();
        while ( idList.hasNext()) {
            String id = idList.next();
            System.out.println("Got id " + id);
            given().headers("X-OpenIDM-Username", "openidm-admin","X-OpenIDM-Password", "openidm-admin", "If-Match", "*").pathParam("id", id).expect().statusCode(204).when().delete("/openidm/managed/user/{id}");
        }
        idList = null;
        ids = null;
        //given().headers("X-OpenIDM-Username", "openidm-admin","X-OpenIDM-Password", "openidm-admin","Content-Type", "application/json").request().body("{\"username\":\"nicolas\" }").expect().statusCode(201).when().put("/openidm/managed/user/nicolas").asString();
    }


}
