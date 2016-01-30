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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.osgi;

import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Utilties for augmenting ComponentContext behavior.
 */
public class ComponentContextUtil {

    public static Dictionary<String, Object> getModifiableProperties(ComponentContext context) {
        final Dictionary<String, Object> properties = context.getProperties();
        final Dictionary<String, Object> copy = new Hashtable<>(properties.size());
        Enumeration<String> keys = context.getProperties().keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            copy.put(key, properties.get(key));
        }
        return copy;
    }

    private ComponentContextUtil() {
        // prevent instantiation
    }
}
