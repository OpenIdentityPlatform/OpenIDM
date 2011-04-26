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
 * <p>Java class for OperationType.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;simpleType name="OperationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="create"/>
 *     &lt;enumeration value="update"/>
 *     &lt;enumeration value="delete"/>
 *     &lt;enumeration value="test"/>
 *     &lt;enumeration value="scriptOnConnector"/>
 *     &lt;enumeration value="scriptOnResource"/>
 *     &lt;enumeration value="get"/>
 *     &lt;enumeration value="authenticate"/>
 *     &lt;enumeration value="search"/>
 *     &lt;enumeration value="validate"/>
 *     &lt;enumeration value="sync"/>
 *     &lt;enumeration value="schema"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public enum OperationType {


    /**
     * CreateApiOp.class
     */
    CREATE("create"),

    /**
     * UpdateApiOp.class
     */
    UPDATE("update"),

    /**
     * DeleteApiOp.class
     */
    DELETE("delete"),

    /**
     * TestApiOp.class
     */
    TEST("test"),

    /**
     * ScriptOnConnectorApiOp.class
     */
    SCRIPT_ON_CONNECTOR("scriptOnConnector"),

    /**
     * ScriptOnResourceApiOp.class
     */
    SCRIPT_ON_RESOURCE("scriptOnResource"),

    /**
     * GetApiOp.class
     */
    GET("get"),

    /**
     * AuthenticationApiOp.class
     */
    AUTHENTICATE("authenticate"),

    /**
     * SearchApiOp.class
     */
    SEARCH("search"),

    /**
     * ValidateApiOp.class
     */
    VALIDATE("validate"),

    /**
     * SyncApiOp.class
     */
    SYNC("sync"),

    /**
     * SchemaApiOp.class
     */
    SCHEMA("schema");
    private final String value;

    OperationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OperationType fromValue(String v) {
        for (OperationType c : OperationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

