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

package org.forgerock.openidm.action.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.openidm.action.Action;
import org.forgerock.openidm.action.ActionException;
import org.forgerock.openidm.action.ActionFactory;

/**
 * Default {@link ActionFactory} implementation that Instantiate new
 * {@link Action} instances using Class.forName.
 */
public class ActionFactoryImpl implements ActionFactory {

    final static Logger logger = LoggerFactory.getLogger(ActionFactoryImpl.class);

    /**
     * Instantiate a new {@link Action} instance from the given configuration value that
     * must be a fully qualified class name.
     *
     * @param configurationValue FQCN indicating the action that needs instantiated
     * @return action instance
     * @throws ActionException if the class was not found, or there was a lower level error
     */
    @Override
    public Action newInstance(String configurationValue) throws ActionException {
        Action action = null;
        try {
            action = (Action) Class.forName(configurationValue).newInstance();
        } catch (ClassNotFoundException cnfe) {
            logger.error("Class for Action implementation was not found {}", configurationValue);
            throw new ActionException(cnfe);
        } catch (Exception ex) {
            //re-throw lower level exceptions
            logger.error(ex.getLocalizedMessage());
            throw new ActionException(ex);
        }
        return action;
    }
}
