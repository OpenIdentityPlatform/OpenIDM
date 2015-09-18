/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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
 */
package org.forgerock.openidm.audit.impl;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.json.JsonValue;

/**
 * A Context used when auditing over the router.
 */
class AuditContext extends AbstractContext {
    public AuditContext(Context parent) {
        super(parent, "AuditContext");
    }

    public AuditContext(Context parent, String name) {
        super(parent, name);
    }

    public AuditContext(String id, String name, Context parent) {
        super(id, name, parent);
    }

    public AuditContext(JsonValue savedContext, ClassLoader classLoader) {
        super(savedContext, classLoader);
    }
}
