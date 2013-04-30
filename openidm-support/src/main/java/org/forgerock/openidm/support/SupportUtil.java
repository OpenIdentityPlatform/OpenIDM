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

package org.forgerock.openidm.support;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class SupportUtil {

    private static final Logger logger = LoggerFactory.getLogger(SupportUtil.class);

    private static final Logger supportLogger = LoggerFactory.getLogger("com.forgerock.support");

    private static ObjectMapper mapper;

    private static AtomicInteger sequence = new AtomicInteger(-1);

    static {
        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        supportLogger.error("Start support tool for {}:{}", ServerConstants.getVersion(),
                ServerConstants.getRevision());

    }

    private SupportUtil() {
    }

    public static void trace(Object request) {
        trace(request, null);
    }

    public static void trace(Object request, Object responseOrError) {
        if (supportLogger.isWarnEnabled()) {
            int seq = sequence.incrementAndGet();

            String requestString = null;
            if (null != request) {
                try {
                    requestString = mapper.writeValueAsString(request);
                } catch (IOException e) {
                    logger.error("Failed to serialise REQUEST", e);
                }
            }

            String responseOrErrorString = null;
            if (null != responseOrError) {
                try {
                    responseOrErrorString = mapper.writeValueAsString(responseOrError);
                } catch (IOException e) {
                    logger.error("Failed to serialise RESPONSE or ERROR", e);
                }
            }
            if (null != responseOrErrorString) {
                supportLogger
                        .warn("****** REQUEST START: {} ******\n {} \n ------------------------ RESPONSE ------------------------\n {} \n ****** REQUEST END ****** ",
                                new Object[]{seq, String.valueOf(requestString),
                                        responseOrErrorString});
            } else {
                supportLogger.warn(
                        "****** REQUEST START: {} ******\n {} \n ****** REQUEST END ****** ", seq,
                        String.valueOf(requestString));
            }
        }
    }
}
