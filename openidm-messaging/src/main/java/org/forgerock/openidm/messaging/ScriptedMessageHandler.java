/*
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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.messaging;

import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;

/**
 * Generic message handler that will pass the received message down to the configured script.
 * The script is expected to fully parse the message and execute any logic that the message is requesting or represents.
 */
public class ScriptedMessageHandler<T> implements MessageHandler<T> {

    private static final String SCRIPT = "script";
    private static final String MESSAGE = "message";
    private final ScriptEntry scriptEntry;

    /**
     * Constructs a scripted handler with the provided script configuration.
     *
     * @param propertiesConfig handler json that is expected to have a script object defined.
     * @param scriptRegistry the registry to take the script from.
     */
    public ScriptedMessageHandler(final JsonValue propertiesConfig, final ScriptRegistry scriptRegistry) {
        try {
            scriptEntry = scriptRegistry.takeScript(propertiesConfig.get(SCRIPT).required());
        } catch (ScriptException e) {
            throw new InvalidException("Failure preparing script to handle messages", e);
        }
    }

    /**
     * Handles the message by passing the message to the script as the "message" attribute.
     *
     * @param message the message that has been received.
     * @throws ResourceException If there is a problem invoking the script, or if the script throws an exception.
     */
    @Override
    public void handleMessage(final T message) throws ResourceException {
        if (scriptEntry.isActive()) {
            Script script = scriptEntry.getScript(ContextUtil.createInternalContext());
            try {
                script.put(MESSAGE, message);
                script.eval();
            } catch (ScriptException e) {
                throw new InternalServerErrorException("Failure running script " + script, e);
            }
        } else {
            throw new InternalServerErrorException("Script is not active: " + scriptEntry);
        }
    }
}
