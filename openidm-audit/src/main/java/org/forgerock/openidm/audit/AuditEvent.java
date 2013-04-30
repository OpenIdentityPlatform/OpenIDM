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

package org.forgerock.openidm.audit;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Stack;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.util.DateUtil;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
@JsonInclude
public class AuditEvent {

    @JsonProperty("serverName")
    private final String servername = null;

    private final String instancename = null;

    private final String workProcessType = null;

    private  String userId = null;

    private final String clientIP = null;

    private final String workProcessNumber = null;

    private final String transactionCode = null;

    private final String programName = null;

    private final String client = null;

    private final String messageText = null;

    private final String messageGroup = null;

    private final String subName = null;

    private final String auditClass = null;

    private final String securityLevel = null;

    private final Map<String, Object> parameters = null;

    @JsonRawValue
    private String before = null;

    @JsonRawValue
    private String after = null;

    @JsonIgnore
    private AuditEvent tampering = null;

    public static class Builder {

        private final DateTime now;
        private final AuditEvent event = new AuditEvent();

        private Builder(DateTime now) {
            this.now = now;
        }

        public static Builder build(Context context) {
            Stack<String> callStack = new Stack<String>();
            Context parent = context;
            while (parent.getParent() != null) {
               callStack.push(context.getId());
               parent = parent.getParent();
            }
            return new Builder(DateUtil.getDateUtil().currentDateTime());
        }

        public Builder with(SecurityContext context) {
             event.userId = context.getAuthenticationId();
            return this;
        }

        public AuditEvent build() {
            return event;
        }

    }

    public static class DateSerializer extends JsonSerializer<Date> {

        @Override
        public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            if (value != null) {
                jgen.writeObject(DateUtil.getDateUtil().formatDateTime(value));
            }
        }

    }
}
