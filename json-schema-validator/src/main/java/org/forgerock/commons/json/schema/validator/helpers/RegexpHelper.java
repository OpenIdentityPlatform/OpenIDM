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
package org.forgerock.commons.json.schema.validator.helpers;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.validators.SimpleValidator;

/**
 * RegexpHelper encapsulates all predictable regular expressions.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class RegexpHelper implements SimpleValidator<String> {

    private String options = "";
    private String regex = ".*";
    private Pattern pattern = null;

    public Pattern createPattern(String pattern) throws MalformedPatternException {
        Perl5Compiler compiler = new Perl5Compiler();
        return compiler.compile(pattern);
    }

    public Perl5Matcher getMatcher() {
        return new Perl5Matcher();
    }

    public void validate(String node, String at, ErrorHandler handler) throws SchemaException {
        if (null != node && !getMatcher().matches(node, pattern)) {
            handler.error(new ValidationException(at + ": does not match the regex pattern " + pattern.getPattern(), at));
        }
    }
}
