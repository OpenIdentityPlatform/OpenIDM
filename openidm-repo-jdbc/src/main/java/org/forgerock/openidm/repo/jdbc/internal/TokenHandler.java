/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.jdbc.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenHandler {

    // The OpenIDM query token is of format ${token-name}
    Pattern tokenPattern = Pattern.compile("\\$\\{(.+?)\\}");

    /**
     * Replaces a query string with tokens of format ${token-name} with the
     * specified replacement string for all tokens.
     *
     * @param queryString
     *            the query with tokens to replace
     * @param replacement
     * @return the query with all tokens replaced
     */
    public String replaceTokens(String queryString, String replacement) {
        Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String origToken = matcher.group(1);
            // TODO: the size check seems invalid
            if (origToken != null) {
                // OrientDB token is of format :token-name
                matcher.appendReplacement(buf, "");
                buf.append(replacement);
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    /**
     * Extracts all the token names in the query string of format ${token-name}
     *
     * @param queryString
     *            the query with tokens
     * @return the list of token names, in the order they appear in the
     *         queryString
     */
    public List<String> extractTokens(String queryString) {
        List<String> tokens = new ArrayList<String>();
        Matcher matcher = tokenPattern.matcher(queryString);
        while (matcher.find()) {
            String origToken = matcher.group(1);
            tokens.add(origToken);
        }
        return tokens;
    }

    /**
     * Replaces some tokens in a query string with tokens of format
     * ${token-name} with the given replacements, which may again be tokens
     * (e.g. in another format) or values. Tokens that have no replacement
     * defined stay in the original token format.
     *
     *
     * @param queryString
     *            the query with OpenIDM format tokens ${token}
     * @param replacements
     *            the replacement values/tokens, where the key is the token name
     *            in the query string, and the value is the String to replace it
     *            with.
     * @return the query with any defined replacement values/tokens replaced,
     *         and the remaining tokens left in the original format
     */
    public String replaceSomeTokens(String queryString, Map<String, String> replacements) {
        Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String origToken = matcher.group(1);
            if (origToken != null) {
                String replacement = replacements.get(origToken);
                if (replacement == null) {
                    // if not replacement specified, keep the original token.
                    replacement = "${" + origToken + "}";
                }
                matcher.appendReplacement(buf, "");
                buf.append(replacement);
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }
}
