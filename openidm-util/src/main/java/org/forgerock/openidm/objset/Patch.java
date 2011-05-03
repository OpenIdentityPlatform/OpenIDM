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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.objset;

// Java Standard Edition
import java.util.Map;

/**
 * The interface that provides the ability to apply partial changes to objects. Implementations
 * interpret a specific patch document representation.
 *
 * @author Paul C. Bryan
 */
public interface Patch {

    /**
     * Applies the patch to the specified object.
     *
     * @param object the object to apply the partial change to.
     * @throws ConflictException if the patch could not be applied, given the state of the resource.
     */
    void patch(Map<String, Object> object) throws ConflictException;
}
