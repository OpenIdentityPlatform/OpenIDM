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

package org.forgerock.openidm.audit.internal;

import java.io.File;
import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.openidm.audit.AuditEvent;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class CSVRecordPublisherTest {

    @Test
    public void testOnEvent() throws Exception {
        JsonValue configuration = new JsonValue(new HashMap<String, Object>());
        // TODO check with space in path
        configuration.put(CSVRecordPublisher.CONFIG_LOG_LOCATION, new File(
                CSVRecordPublisherTest.class.getResource("/").toURI()).getAbsolutePath());
        CSVRecordPublisher publisher = new CSVRecordPublisher(configuration);
        publisher.onStart();

        SecurityContext context = new SecurityContext(new RootContext(),"test", null);

        AuditEvent.Builder.build(context).build();

        EventHolder eventHolder = new EventHolder();
        eventHolder.setEvent(AuditEvent.Builder.build(context).build());

        publisher.onEvent(eventHolder, 1, false);

        publisher.onShutdown();
    }
}
