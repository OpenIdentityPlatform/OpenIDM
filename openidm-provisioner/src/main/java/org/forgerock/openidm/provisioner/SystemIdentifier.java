/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-15 ForgeRock AS. All rights reserved.
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

/**
 * SystemIdentifier is a composite key to identify the {@link ProvisionerService} instance.
 *
 * @version $Revision$ $Date$
 */
public interface SystemIdentifier {
    /**
     * Compare this and the {@code other} instance and returns true if both identifies the same
     * {@link ProvisionerService} instance.
     *
     * @param other
     * @return
     */
    boolean is(SystemIdentifier other);

    /**
     * Checks the {@code uri} and return true if the {@link ProvisionerService} instance is responsible to handle
     * the request.
     *
     * @param id
     * @return
     */
    boolean is(Id id);
}
