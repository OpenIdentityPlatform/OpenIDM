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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2014-2015 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.util;

import org.forgerock.http.Context;
import org.forgerock.json.resource.ClientContext;

/**
 */
public class ContextUtil {

    /**
     * {@code ContextUtil} instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * {@code ContextUtil.createInternalSecurityContext(...);}.
     */
    private ContextUtil() {
        super();
    }

    /**
     * Tests whether the context represents an external routed request, by checking if the client context
     * is present and that it is external
     *
     * @param context the context to inspect
     * @return true if the context represents an external request;
     *      false otherwise, or if the {@link ClientContext} could not be found
     */
    public static boolean isExternal(Context context) {
        return context.containsContext(ClientContext.class)
                && context.asContext(ClientContext.class).isExternal();
    }
}
