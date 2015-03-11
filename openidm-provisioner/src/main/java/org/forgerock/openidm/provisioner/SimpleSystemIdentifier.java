/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * SimpleSystemIdentifier class helps to match the id against the name of the
 * system configuration.
 * <p/>
 * Matching id pattern: system/{@code name}/*
 *
 */
public class SimpleSystemIdentifier implements SystemIdentifier {

    private String name;

    public SimpleSystemIdentifier(JsonValue configuration) throws JsonValueException {
        name = configuration.get("name").required().expect(String.class).asString().trim();

        if (StringUtils.isBlank(name)) {
            throw new JsonValueException(configuration, "Failed to determine system name from configuration.");
        }
        if (!StringUtils.isAlphanumeric(name)) {
            throw new JsonValueException(configuration, "System name is not alphanumeric: " + name);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean is(SystemIdentifier other) {
        return equals(other);
    }

    /**
     * {@inheritDoc}
     */
    public boolean is(Id uri) {
        return name.equals(uri.getSystemName());
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleSystemIdentifier that = (SimpleSystemIdentifier) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SystemIdentifier{ uri='system/" + name + "/'}";
    }
}
