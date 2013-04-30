/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.graph;

import com.tinkerpop.blueprints.Graph;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.Parameter;

/**
 * A GraphFunction does ...
 *
 * @author Laszlo Hordos
 */
public class GraphFunction implements Function<Graph> {

    private final GraphConnectionFactory connectionFactory;

    public GraphFunction(final GraphConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Graph call(Parameter scope, Function<?> callback, Object... arguments) throws ResourceException, NoSuchMethodException {
        return connectionFactory.getConnection();
    }
}
