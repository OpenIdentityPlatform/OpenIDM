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
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public interface GraphConnectionFactory {

    /**
     * Returns a connection to the Graph associated with this
     * connection factory. The connection returned by this method can be used
     * immediately.
     *
     * @return A connection to the Graph associated with this
     *         connection factory.
     * @throws ResourceException
     *             If the connection request failed for some reason.
     */
    Graph getConnection() throws ResourceException;

    /**
     * Asynchronously obtains a connection to the Graph
     * associated with this connection factory. The returned
     * {@code FutureResult} can be used to retrieve the completed connection.
     * Alternatively, if a {@code ResultHandler} is provided, the handler will
     * be notified when the connection is available and ready for use.
     *
     * @param handler
     *            The completion handler, or {@code null} if no handler is to be
     *            used.
     * @return A future which can be used to retrieve the connection.
     */
    FutureResult<Graph> getConnectionAsync(ResultHandler<Graph> handler);

}
