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

package org.forgerock.openidm.provisioner.openicf.impl;

import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationType;
import org.identityconnectors.framework.api.operations.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ConnectorObjectOptions {

    private OperationOptionInfoHelper defaultOperationOptionInfoHelper;
    private Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;

    public ConnectorObjectOptions(Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations) {
        this.defaultOperationOptionInfoHelper = new OperationOptionInfoHelper();
        this.operations = operations;
    }

    public ConnectorObjectOptions(OperationOptionInfoHelper defaultOperationOptionInfoHelper, Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations) {
        this.defaultOperationOptionInfoHelper = (null != defaultOperationOptionInfoHelper) ? defaultOperationOptionInfoHelper : new OperationOptionInfoHelper();
        this.operations = operations;
    }

    public OperationOptionInfoHelper find(Class<? extends APIOperation> operation) {
        if (null != operation && null != operations) {
            OperationOptionInfoHelper v = operations.get(operation);
            if (null != v) {
                return v;
            }
        }
        return defaultOperationOptionInfoHelper;
    }


}
