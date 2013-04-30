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

package org.forgerock.openidm.internal.link;

import java.util.Collection;

import org.forgerock.openidm.internal.recon.ReconUtil.Triplet.Edge;
import org.forgerock.openidm.internal.recon.ReconUtil.Triplet.Vertex;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public interface LinkDefinition {

    public Collection<Edge> queryByTarget(String targetId);

    public Collection<Edge> queryByTarget(Vertex target);

    public Collection<Edge> queryBySource(String sourceId);

    public Collection<Edge> queryBySource(Vertex source);


    /**
     * Build full resourceName prefixed with the targetResourceCollection
     * name.
     *
     * @param targetId
     * @return null if the {@code target} parametr is null else the full
     *         resourceName.
     */
    public String targetResourceName(String targetId);

}
