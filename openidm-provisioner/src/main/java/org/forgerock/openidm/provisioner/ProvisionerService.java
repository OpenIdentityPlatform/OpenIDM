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

import org.forgerock.openidm.objset.ObjectSet;

import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface ProvisionerService extends ObjectSet {

    /**
     * Gets the unique {@link SystemIdentifier} of this instance.
     * <p/>
     * The service which refers to this service instance can distinguish between multiple instances by this value.
     * @return
     */
    public SystemIdentifier getSystemIdentifier();

    /**
     * Gets a brief stats report about the current status of this service instance.
     * </p/>
     * TODO Provide a sample object
     * @return
     */
    public Map<String, Object> getStatus();
}
