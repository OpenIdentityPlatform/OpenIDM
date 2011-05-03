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

package org.forgerock.openidm.action;

/**
 * Instantiates {@link Action} objects based on the configured value.
 */
public interface ActionFactory {

    /**
     * Instantiate a new {@link Action} instance based on the configuration
     * value.
     *
     * @param configurationValue indicating the action that needs instantiated
     * @return acton instance
     * @throws ActionException if there is an error in instantiation or configuration
     */
    Action newInstance(String configurationValue) throws ActionException;

}
