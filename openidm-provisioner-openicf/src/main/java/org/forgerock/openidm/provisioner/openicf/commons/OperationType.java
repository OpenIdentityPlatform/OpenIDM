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

package org.forgerock.openidm.provisioner.openicf.commons;

import org.identityconnectors.framework.api.operations.*;

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
 * @version $Revision$ $Date$
 */
public enum OperationType {


    /**
     * CreateApiOp.class
     */
    CREATE(CreateApiOp.class),

    /**
     * UpdateApiOp.class
     */
    UPDATE(UpdateApiOp.class),

    /**
     * DeleteApiOp.class
     */
    DELETE(DeleteApiOp.class),

    /**
     * TestApiOp.class
     */
    TEST(TestApiOp.class),

    /**
     * ScriptOnConnectorApiOp.class
     */
    SCRIPT_ON_CONNECTOR(ScriptOnConnectorApiOp.class),

    /**
     * ScriptOnResourceApiOp.class
     */
    SCRIPT_ON_RESOURCE(ScriptOnResourceApiOp.class),

    /**
     * GetApiOp.class
     */
    GET(GetApiOp.class),

    /**
     * ResolveUsernameApiOp.class
     */
    RESOLVEUSERNAME(ResolveUsernameApiOp.class),

    /**
     * AuthenticationApiOp.class
     */
    AUTHENTICATE(AuthenticationApiOp.class),

    /**
     * SearchApiOp.class
     */
    SEARCH(SearchApiOp.class),

    /**
     * ValidateApiOp.class
     */
    VALIDATE(ValidateApiOp.class),

    /**
     * SyncApiOp.class
     */
    SYNC(SyncApiOp.class),

    /**
     * SchemaApiOp.class
     */
    SCHEMA(SchemaApiOp.class);
    private final Class<? extends APIOperation> clazz;


    OperationType(Class<? extends APIOperation> v) {
        clazz = v;
    }

    public Class<? extends APIOperation> getValue() {
        return clazz;
    }

    public static OperationType fromValue(Class<? extends APIOperation> v) {
        OperationType op = null;
        for (OperationType c : OperationType.values()) {
            if (c.clazz.equals(v)) {
                op = c;
            }
        }
        return op;
    }

}

