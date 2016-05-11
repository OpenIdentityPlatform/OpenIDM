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
package org.forgerock.openidm.util;

import static org.forgerock.json.resource.ResourceException.*;

import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.util.Reject;

/**
 * Utility methods.
 */
public class Utils {

    /**
     * Adapt any Exception to a ResourceException.
     *
     * @param ex the Exception
     * @return the converted ResourceException
     */
    public static ResourceException adapt(Exception ex) {
        Reject.ifNull(ex);

        short resourceResultCode;
        try {
            throw ex;
        } catch (ResourceException e) {
            return e;
        } catch (JsonValueException e) {
            resourceResultCode = 400;
        } catch (ScriptThrownException e) {
            return e.toResourceException(500, e.getMessage());
        } catch (Exception e) {
            resourceResultCode = 500;
        }

        return newResourceException(resourceResultCode, ex.getMessage(), ex);
    }
}
