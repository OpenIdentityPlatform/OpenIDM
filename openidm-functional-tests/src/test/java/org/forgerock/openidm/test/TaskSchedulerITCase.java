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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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


    @BeforeClass
    public void setupClass(ITestContext context) {
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = 8080;
        RestAssured.basePath = "/openidm";
        RestAssured.authentication = basic("openidm-admin", "openidm-admin");
        RestAssured.requestContentType(ContentType.JSON);
        RestAssured.urlEncodingEnabled = true;
    }

    @Test
    public void creatUser(ITestContext context) throws Exception {
        Assert.assertNotNull(accessor);
        JsonValue response = accessor.create("managed/user", getUser());
        Assert.assertNotNull(response.get("_id").getObject());
    }

    @Test
    public void restTest() {
        given().auth().basic("openidm-admin", "openidm-admin");
        given().headers("X-OpenIDM-Password", "openidm-admin", "X-OpenIDM-Username", "openidm-admin");
        get("/config").getBody().prettyPrint();
       /* given().contentType(ContentType.JSON);
        given().body(getUser().getObject());
        given().pathParameter("_action","create");
        post("managed/user");*/
    }

    @Test
    public void keepOpenIDMRunning() throws Exception {
        //Thread.sleep(TimeUnit.MINUTES.toMillis(5));
    }

    protected JsonValue getUser() {
        JsonValue user = new JsonValue(new HashMap<String, Object>());
        user.put("userName","DDOE");
        return user;
    }

}
