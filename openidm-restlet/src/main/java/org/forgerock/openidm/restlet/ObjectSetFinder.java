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

package org.forgerock.openidm.restlet;

// Restlet Framework
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;

// ForgeRock OpenIDM Core
import org.forgerock.openidm.objset.ObjectSet;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
public class ObjectSetFinder extends Finder {

    /** TODO: Description. */
    private ObjectSet objectSet;

    /**
     * TODO: Description.
     */
    public ObjectSetFinder(ObjectSet objectSet) {
        this.objectSet = objectSet;
        setTargetClass(ObjectSetServerResource.class);
    }

    @Override
    public void handle(Request request, Response response) {
        request.getAttributes().put(ObjectSet.class.getName(), objectSet);
        super.handle(request, response);
    }
}
    
