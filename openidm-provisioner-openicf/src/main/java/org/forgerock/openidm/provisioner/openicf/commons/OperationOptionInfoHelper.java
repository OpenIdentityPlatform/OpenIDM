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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.commons;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OperationOptionInfoHelper {


    public static final String OPERATION_OPTION_DENIED = "denied";
    public static final String OPERATION_OPTION_ON_DENY = "onDeny";
    public static final String OPERATION_OPTION_OPERATION_OPTION_INFO = "operationOptionInfo";

    public static enum OnDenyAction {
        THROW_ERROR,
        DO_NOTHING
    }

    private boolean denied;
    private OnDenyAction onDeny;
    private final Set<AttributeInfoHelper> attributes;


    public OperationOptionInfoHelper(JsonNode configuration) throws JsonNodeException, SchemaException {
        denied = configuration.get(OPERATION_OPTION_DENIED).defaultTo(false).asBoolean();
        JsonNode onDenyNode = configuration.get(OPERATION_OPTION_ON_DENY);
        onDeny = onDenyNode.isNull() ? OperationOptionInfoHelper.OnDenyAction.DO_NOTHING : onDenyNode.asEnum(OnDenyAction.class);
        JsonNode operationOptionInfo = configuration.get(OPERATION_OPTION_OPERATION_OPTION_INFO);
        if (operationOptionInfo.isMap()) {
            attributes = new HashSet<AttributeInfoHelper>();
            for (Map.Entry<String, Object> entry : operationOptionInfo.get(Constants.PROPERTIES).expect(Map.class).asMap().entrySet()) {
                attributes.add(new AttributeInfoHelper(entry.getKey(), (Map<String, Object>) entry.getValue()));
            }
        } else {
            attributes = Collections.emptySet();
        }
    }

    public OperationOptionInfoHelper(JsonNode configuration, OperationOptionInfoHelper globalOption) throws JsonNodeException, SchemaException {
        denied = configuration.get(OPERATION_OPTION_DENIED).defaultTo(globalOption.isDenied()).asBoolean();
        JsonNode onDenyNode = configuration.get(OPERATION_OPTION_ON_DENY);
        onDeny = onDenyNode.isNull() ? globalOption.getOnDeny() : onDenyNode.asEnum(OnDenyAction.class);
        attributes = new HashSet<AttributeInfoHelper>(globalOption.getAttributes());
        JsonNode operationOptionInfo = configuration.get(OPERATION_OPTION_OPERATION_OPTION_INFO);
        if (operationOptionInfo.isMap()) {
            for (Map.Entry<String, Object> entry : operationOptionInfo.get(Constants.PROPERTIES).expect(Map.class).asMap().entrySet()) {
                attributes.add(new AttributeInfoHelper(entry.getKey(), (Map<String, Object>) entry.getValue()));
            }
        }
    }

    public boolean isDenied() {
        return denied;
    }

    public OnDenyAction getOnDeny() {
        return onDeny;
    }

    Set<AttributeInfoHelper> getAttributes() {
        return attributes;
    }

    public OperationOptionsBuilder build(Map<String, Object> source, ObjectClassInfoHelper objectClassInfoHelper) throws IOException {
        OperationOptionsBuilder builder = new OperationOptionsBuilder();
        if (null != source) {
            for (AttributeInfoHelper helper : attributes) {
                helper.build(builder, source.get(helper.getName()));
            }
        }
        return builder;
    }

}
