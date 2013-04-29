/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.salesforce.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public final class ResultHandler {

    private JsonResourceException exception;
    private JsonValue result;

    /**
     * Invoked when the asynchronous request has failed.
     * 
     * @param error
     *            The resource exception indicating why the asynchronous request
     *            has failed.
     */
    public void handleError(JsonResourceException error) {
        this.exception = error;
    }

    /**
     * Invoked when the asynchronous request has completed successfully.
     * 
     * @param result
     *            The result of the asynchronous request.
     */
    public void handleResult(JsonValue result) {
        this.result = result;
    }

    public boolean handleResource(JsonValue resource) {
        List<Object> results = null;
        if (result == null) {
            result = new JsonValue(new HashMap<String, Object>(1));
            results = new ArrayList<Object>();
            result.put("result", results);
        } else {
            results = result.get("result").required().asList();
        }
        return results.add(resource.getObject());
    }

    public JsonValue getResult() throws JsonResourceException {
        if (null != exception) {
            throw exception;
        }
        return result;
    }
}
