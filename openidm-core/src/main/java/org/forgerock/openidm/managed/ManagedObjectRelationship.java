package org.forgerock.openidm.managed;/*
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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

import org.forgerock.json.resource.ConnectionFactory;

/**
 * Class representing a relationship between two managed objects.
 */
public class ManagedObjectRelationship {
    /** Connectino factory used to access the repo router */
    private ConnectionFactory connectionFactory;

    /** Id (UUID) of this relationship object */
    private String id;

    /** Current revision of this object. Used for MVCC. */
    private String revision;

    /** Id of the first (source) object that initiated the relationship */
    private String firstId;

    /** Key in the first object containing the relationship */
    private String firstKey;

    /** Id of the second (target) object in the initial relationship */
    private String secondId;

    public ManagedObjectRelationship(final ConnectionFactory connectionFactory, String firstId, String firstKey, String secondId) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Persist the relationship to the repo.
     */
    public void create() {

    }

    /**
     * Update the relationship object
     */
    public void update() {

    }
}
