/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.tools.scriptedbundler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Base object for configuration -> object tree [de]serialization. This object tree is used by the
 * Handlebars template processor to process templates into output files with variable substitution.
 * It is constructed from a JSON configuration file.
 */
public class CustomBaseObject {
    private boolean last = false;  // helper for the templates, no bean accessors

    /**
     * Set whether this object is the last in a container of similar objects. This is a helper method
     * for Handlebars to help it decide whether to continue a JSON list (",") or close it ("]" or "}").
     * It is not set via deserialization of the configuration.
     *
     * @param last
     */
    @JsonIgnore
    public void setIsLast(boolean last) {
        this.last = last;
    }

    /**
     * Return whether this is the last object in a collection of similar objects.  Handlebars helper method.
     * It is not serialized with other bean properties.
     *
     * @return
     */
    @JsonIgnore
    public boolean getIsLast() {
        return last;
    }

    /**
     * Utility method to flag the last item in a collection as being the last item. This is
     * then used in our templates to help with formatting the output.
     *
     * @param list
     * @param <O>
     * @return
     */
    public <O extends CustomBaseObject> List<O> flagLast(List<O> list) {
        if (list != null && !list.isEmpty()) {
            list.get(list.size() - 1).setIsLast(true);
        }
        return list;
    }
}
