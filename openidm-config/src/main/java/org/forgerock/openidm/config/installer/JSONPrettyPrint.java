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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.config.installer;

import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.jackson.impl.Indenter;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Helper to generate customer formatted (pretty print) Jackson JSON output
 * 
 *
 */
public class JSONPrettyPrint {
    
    ObjectWriter writer;
    
    public JSONPrettyPrint() {
        ObjectMapper mapper = new ObjectMapper();
        Indenter indenter = new PrettyIndenter();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(indenter);
        prettyPrinter.indentArraysWith(indenter);
        writer = mapper.prettyPrintingWriter(prettyPrinter);
    }
    
    public ObjectWriter getWriter() {
        return writer;
    }
}
