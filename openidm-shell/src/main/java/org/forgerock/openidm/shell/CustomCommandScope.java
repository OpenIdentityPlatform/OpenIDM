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
package org.forgerock.openidm.shell;

import java.util.Map;

/**
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface CustomCommandScope {
    /**
     * Get the {@link org.apache.felix.service.command.CommandProcessor#COMMAND_FUNCTION} value.
     * <p/>
     * TODO add description
     *
     * @return retrun a new map where the key is the command name and the value is the description.
     */
    public Map<String, String> getFunctionMap();

    /**
     * Get the {@link org.apache.felix.service.command.CommandProcessor#COMMAND_SCOPE} value.
     * <p/>
     * TODO add description
     *
     * @return
     */
    public String getScope();

    /**
     * Execute the command scope in interactive mode
     * TODO reconsider to have this command
     *
     * @param args
     */
    public void execute(String[] args);

}
