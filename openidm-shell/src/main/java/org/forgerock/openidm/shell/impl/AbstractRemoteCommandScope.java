/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.shell.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.openidm.shell.CustomCommandScope;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public abstract class AbstractRemoteCommandScope implements CustomCommandScope {

    private JsonResource router;

    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * @return HttpRemoteJsonResource instance
     */
    protected JsonResource getRouter() {
        return router;
    }
}
