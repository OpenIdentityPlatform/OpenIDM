/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.idp.config;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.api.annotations.Description;
import org.forgerock.json.JsonValue;

/**
 * Configuration for a single transform in an identity provider mapping
 */
@Description("Property transformation")
public class Transform {

    private String type;
    private String source;
    private String file;

    /**
     * Gets script type (e.g., text/javascript, groovy).
     *
     * @return Script type (e.g., text/javascript, groovy)
     */
    @Description("Script type (e.g., text/javascript, groovy)")
    public String getType() {
        return type;
    }

    /**
     * Sets script type (e.g., text/javascript, groovy).
     *
     * @param type Script type (e.g., text/javascript, groovy)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets inlined script.
     *
     * @return Inlined script
     */
    @Description("Inlined script")
    public String getSource() {
        return source;
    }

    /**
     * Sets inlined script.
     *
     * @param source Inlined script
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets path to file containing the script.
     *
     * @return Path to file containing the script
     */
    @Description("Path to file containing the script")
    public String getFile() {
        return file;
    }

    /**
     * Sets path to file containing the script.
     *
     * @param file Path to file containing the script
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Serializes this object as a {@link JsonValue}.
     *
     * @return {@link JsonValue} representation
     */
    public JsonValue asJsonValue() {
        JsonValue retval = json(object(
                field("type", type)
        ));
        if (source != null) {
            retval.add("source", source);
        } else if (file != null) {
            retval.add("file", file);
        }
        return retval;
    }
}
