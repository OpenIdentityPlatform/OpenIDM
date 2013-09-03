/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.script;

import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;

/**
 * Hold a script and additional parameters
 * 
 * @author ckienle
 */
public class RegisteredScript {
    
    JsonValue parameters;
    Script script;
    
    public RegisteredScript(Script script, JsonValue scriptConfig) {
        this.parameters = filterParameters(scriptConfig);
        this.script = script;
    }
    
    public void addParameter(String key, Object value) {
        this.parameters.add(key, value);
    }
    
    public void removeParameter(String key) {
        this.parameters.remove(key);
    }
    
    public void setParameters(JsonValue parameters) {
        this.parameters = parameters;
    }
    
    public JsonValue getParameters() {
        return this.parameters;
    }
    
    public Script getScript() {
        return this.script;
    }
    
    /**
     * Filters the script parameters to pass to the script
     * 
     * @param val the full configuration
     * @return the parameters
     */
    private JsonValue filterParameters(JsonValue val) {
        JsonValue filtered = new JsonValue(Utils.deepCopy(val.asMap()));
        // Filter the script definition itself
        filtered.remove("type");
        filtered.remove("source");
        filtered.remove("file");
        return filtered;
    }
}
