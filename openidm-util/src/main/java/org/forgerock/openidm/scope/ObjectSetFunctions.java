/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.scope;

// Java Standard Edition
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// JSON Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonPointer;

// OpenIDM
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.script.Function;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
public class ObjectSetFunctions {

    /**
     * TODO: Description.
     *
     * @param params TODO.
     * @return TODO.
     */
    private static JsonNode paramsNode(List<Object> params) {
        return new JsonNode(params, new JsonPointer("params"));
    }

    /**
     * TODO: Description.
     *
     * @param scope TODO.
     * @param router TODO.
     * @return the scope object parameter, to which the functions were added.
     */
    public static Map<String, Object> addToScope(Map<String, Object> scope, final ObjectSet router) {

        HashMap<String, Object> openidm = new HashMap<String, Object>();

        openidm.put("create", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                router.create(node.get(0).required().asString(), node.get(1).required().asMap());
                return null; // no news is good news
            }
        });

        openidm.put("read", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                try {
                    return router.read(node.get(0).required().asString());
                } catch (NotFoundException nfe) {
                    return null; // indicates no such record without throwing exception
                }
            }
        });

        openidm.put("update", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                router.update(node.get(0).required().asString(), node.get(1).asString(), node.get(2).required().asMap());
                return null; // no news is good news
            }
        });

        openidm.put("delete", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                router.delete(node.get(0).required().asString(), node.get(1).asString());
                return null; // no news is good news
            }
        });

        openidm.put("query", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                return router.query(node.get(0).required().asString(), node.get(1).required().asMap());
            }
        });

        openidm.put("action", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                return router.action(node.get(0).required().asString(), node.get(1).required().asMap());
            }
        });

        scope.put("openidm", openidm);
        return scope;
    }
}
