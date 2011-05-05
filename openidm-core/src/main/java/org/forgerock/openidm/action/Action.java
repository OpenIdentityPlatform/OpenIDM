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

import java.util.Map;

/**
 * An interface that needs to be implemented for each defined Action that can be
 * executed for a given {@link org.forgerock.openidm.recon.Situation}. Scripts return Action strings that are
 * mapped using {@link org.forgerock.openidm.recon.impl.ActionMap}
 */
public interface Action {

    /**
     * Execute this action against the {@code sourceObject}value provided. If there
     * are any underlying exceptions they are wrapped and re-thrown.
     *
     * @param sourceObject the action should execute on
     * @return result of the action, this value will be context dependant
     * @throws ActionException if there are any underlying exceptions generated
     */
    Object execute(Map<String, Object> sourceObject) throws ActionException;

}