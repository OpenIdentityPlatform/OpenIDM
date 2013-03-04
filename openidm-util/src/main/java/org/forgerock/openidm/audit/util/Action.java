/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.audit.util;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.UpdateRequest;

public enum Action {
    create(1), read(2), update(4), patch(8), query(16), delete(32), action(64);

    private final int value;

    private Action(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Action getAction(final Request request) {
        if (request instanceof CreateRequest) {
            return create;
        } else if (request instanceof ReadRequest) {
            return read;
        } else if (request instanceof UpdateRequest) {
            return update;
        } else if (request instanceof PatchRequest) {
            return patch;
        } else if (request instanceof QueryRequest) {
            return query;
        } else if (request instanceof DeleteRequest) {
            return delete;
        } else if (request instanceof ActionRequest) {
            return action;
        } else {
            throw new NullPointerException();
        }
    }
}
