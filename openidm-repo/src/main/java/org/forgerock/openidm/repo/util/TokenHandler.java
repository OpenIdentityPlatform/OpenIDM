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
 */
package org.forgerock.openidm.repo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.json.resource.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenHandler {

    final static Logger logger = LoggerFactory.getLogger(TokenHandler.class);
    
    // The OpenIDM query token is of format ${token-name}
    Pattern tokenPattern = Pattern.compile("\\$\\{(.+?)\\}");

    /**
     * Replaces a query string with tokens of format ${token-name} with the values from the
     * passed in map, where the token-name must be the key in the map
     * 
     * @param queryString the query with tokens
     * @param params the parameters to replace the tokens. Values can be String or List.
     * @return the query with all tokens replace with their found values
     * @throws BadRequestException if token in the query is not in the passed parameters
     */
    public String replaceTokensWithValues(String queryString, Map<String, ? extends Object> params)
            throws BadRequestException {
        java.util.regex.Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String tokenKey = matcher.group(1);       
            if (!params.containsKey(tokenKey)) {
                // fail with an exception if token not found
                throw new BadRequestException("Missing entry in params passed to query for token " + tokenKey);
            } else {
                Object replacement = params.get(tokenKey);
                if (replacement instanceof List) {
                    StringBuffer commaSeparated = new StringBuffer();
                    boolean first = true;
                    for (Object entry : ((List) replacement)) {
                        if (!first) {
                            commaSeparated.append(",");
                        } else {
                            first = false;
                        }
                        commaSeparated.append(entry.toString());
                    }
                    replacement = commaSeparated.toString();
                }
                if (replacement == null) {
                    replacement = "";
                }
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Replaces a query string with tokens of format ${token-name} with the 
     * specified replacement string for all tokens.
     * 
     * @param queryString the query with tokens to replace
     * @param replacement the replacement string
     * @return the query with all tokens replaced
     */
    public String replaceTokens(String queryString, String replacement) {
        return replaceTokens(queryString, replacement, new String[] {});
    }

    /**
     * Replaces a query string with tokens of format ${token-name} with the
     * specified replacement string for all tokens.
     *
     * @param queryString the query with tokens to replace
     * @param replacement the replacement string
     * @param nonReplacementTokenPrefixes optional array of prefixes that, if found as part of a token,
     *      will not be replaced
     * @return the query with all tokens replaced
     */
    public String replaceTokens(String queryString, String replacement, String... nonReplacementTokenPrefixes) {
        Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String origToken = matcher.group(1);
            //TODO: the size check seems invalid
            if (origToken != null) {
                // OrientDB token is of format :token-name
                matcher.appendReplacement(buf, "");
                // if token has one of the "non-replacement" prefixes, leave it alone
                if (tokenStartsWithPrefix(origToken, nonReplacementTokenPrefixes)) {
                    buf.append("${" + origToken + "}");
                }
                else {
                    buf.append(replacement);
                }
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    /**
     * Returns whether the token starts with one of the prefixes passed.
     *
     * @param token the token to interrogate
     * @param prefixes a list of prefixes
     * @return whether the passed token starts with one of the prefixes
     */
    private boolean tokenStartsWithPrefix(String token, String... prefixes) {
        String[] tokenParts = token.split(":", 2);
        if (tokenParts.length == 2) {
            for (String prefix : prefixes) {
                if (prefix.equals(tokenParts[0])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extracts all the token names in the query string  of format ${token-name} 
     * 
     * @param queryString the query with tokens
     * @return the list of token names, in the order they appear in the queryString
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
     * Replaces some tokens in a query string with tokens of format ${token-name}  
     * with the given replacements, which may again be tokens (e.g. in another format)
     * or values. Tokens that have no replacement defined stay in the original token format.
     * 
     * 
     * @param queryString the query with OpenIDM format tokens ${token}
     * @param replacements the replacement values/tokens, where the key is the token name in the query string,
     * and the value is the String to replace it with.
     * @return the query with any defined replacement values/tokens replaced, and the remaining tokens 
     * left in the original format
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

    /**
     * Replaces some tokens in a query string with tokens of format ${token-name}
     * where token-name represents a list of values.  The numberOfReplacements Map tells
     * how many replacements to produce (comma-separated) for each token.  The replacement
     * (for all tokens) is provided. Tokens that have no replacement defined stay in the
     * original token format.
     *
     * @param queryString the query with OpenIDM format tokens ${token}
     * @param numberOfReplacements the number of replacements to replace a ${token} with
     * @param replacement the replacement values/tokens
     * @return the query with any defined replacement values/tokens replaced, and the remaining tokens
     * left in the original format
     */
    public String replaceListTokens(String queryString, Map<String, Integer> numberOfReplacements, String replacement) {
        Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String origToken = matcher.group(1);
            if (origToken != null) {
                matcher.appendReplacement(buf, "");
                Integer length = numberOfReplacements.get(origToken);
                if (length != null) {
                    for (int i = 0; i < length; i++) {
                        buf.append(replacement);
                        if (i != length - 1) {
                            buf.append(", ");
                        }
                    }
                }
                else {
                    buf.append("${" + origToken + "}");
                }
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    /**
     * Replaces a query string with tokens of format ${token-name} with the 
     * token format in OrientDB, which is of the form :token-name.
     * 
     * OrientDB tokens has some limitations, e.g. they can currently only be used
     * in the where clause, and hence the returned string is not guaranteed to be
     * valid for use in a prepared statement. If the parsing fails the system may
     * have to fall back onto non-prepared statements and manual token replacement.
     * 
     * @param queryString the query with OpenIDM format tokens ${token}
     * @return the query with all tokens replaced with the OrientDB style tokens :token
     */
    public String replaceTokensWithOrientToken(String queryString) {
        Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String origToken = matcher.group(1);
            //TODO: the size check seems invalid
            if (origToken != null && origToken.length() > 3) {
                // OrientDB token is of format :token-name
                String newToken = ":" + origToken;
                matcher.appendReplacement(buf, "");
                buf.append(newToken);
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }
}