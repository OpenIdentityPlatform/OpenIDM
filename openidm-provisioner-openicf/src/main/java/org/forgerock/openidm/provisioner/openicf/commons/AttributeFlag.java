/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.provisioner.openicf.commons;

/**
 * Enum of modifier flags to use for attributes. Note that
 * this enum is designed for configuration by exception such that
 * an empty set of flags are the defaults:
 * <ul>
 * <li>updateable</li>
 * <li>creatable</li>
 * <li>returned by default</li>
 * <li>readable</li>
 * <li>single-valued</li>
 * <li>optional</li>
 * </ul>
 *
 * @version $Revision$ $Date$
 */
public enum AttributeFlag {

    /**
     * I will add more enums later. This is a string list so anytime can be
     * extended without any change.
     */
    AUDITED("AUDITED"),
    NOT_QUERYABLE("NOT_QUERYABLE"),
    PASSWORD("PASSWORD"),
    /**
     * The attribute will be stored in repository
     */
    STORE_IN_REPOSITORY("STORE_IN_REPOSITORY"),
    /**
     * Default OpenICF Flag
     */
    NOT_CREATABLE("NOT_CREATABLE"),
    /**
     * Default OpenICF Flag
     */
    NOT_UPDATEABLE("NOT_UPDATEABLE"),
    /**
     * Default OpenICF Flag
     */
    NOT_READABLE("NOT_READABLE"),
    /**
     * Default OpenICF Flag
     */
    NOT_RETURNED_BY_DEFAULT("NOT_RETURNED_BY_DEFAULT"),
    /**
     * Default OpenICF Flag
     */
    MULTIVALUED("MULTIVALUED"),
    /**
     * Custom attribute that is not belong to the resource but the business
     * requires this
     */
    IGNORE_ATTRIBUTE("IGNORE"),
    /**
     * The attribute value is generated outside the OpenIDM and it must be read
     * before it sent to the resource
     */
    REMOTE_ATTRIBUTE("REMOTE");

    private final String value;


    private AttributeFlag(String _key) {
        this.value = _key;
    }

    public String getKey() {
        return value;

    }

    public static AttributeFlag findByKey(String value) {
        for (AttributeFlag f : AttributeFlag.values()) {
            if (f.getKey().equals(value)) {
                return f;
            }
        }
        return null;
    }
}
