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
import java.util.Map;

// OpenIDM
import org.forgerock.openidm.objset.ObjectSet;

/**
 * Generates scopes for use in scripts.
 *
 * @author Paul C. Bryan
 */
public interface ScopeFactory {

    /**
     * Sets the router that the scope factory uses to expose resources to scripts.
     */
    void setRouter(ObjectSet router);

    /**
     * Returns a new scope instance.
     */
    Map<String, Object> newInstance();
}
