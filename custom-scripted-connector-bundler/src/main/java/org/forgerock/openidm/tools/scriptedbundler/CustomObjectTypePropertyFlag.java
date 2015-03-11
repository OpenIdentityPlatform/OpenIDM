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

import java.util.Arrays;
import java.util.List;

/**
 * A single objectType property flag, contained in {@link CustomObjectTypeProperty}.
 *
 * Represented in a JSON configuration list as:
 * <pre><blockquote>
 *      "flags" : [
 *          "NOT_READABLE",
 *          "NOT_RETURNED_BY_DEFAULT"
 *      ],
 * </blockquote></pre>
 */
public class CustomObjectTypePropertyFlag extends CustomBaseObject {
    /**
     * Supported flags for objectType properties.
     */
    private static final List<String> SUPPORTED_FLAGS = Arrays.asList(
        "NOT_READABLE",
        "NOT_RETURNED_BY_DEFAULT",
        "REQUIRED",
        "MULTIVALUED",
        "NOT_CREATABLE",
        "NOT_UPDATEABLE"
    );

    private String flag;

    public CustomObjectTypePropertyFlag(String flag) {
        setFlag(flag);
    }

    /**
     * Return the flag.
     *
     * @return
     */
    public String getFlag() { return flag; }

    /**
     * Set the flag. Enhanced setter to enforce supported flags.
     *
     * @param flag
     */
    public void setFlag(String flag) {
        if (SUPPORTED_FLAGS.contains(flag)) {
            this.flag = flag;
        } else {
            throw new UnsupportedOperationException("objectType property flag '" + flag + "' is not supported");
        }
    }
}
