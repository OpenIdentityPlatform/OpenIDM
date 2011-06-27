package org.forgerock.openidm.repo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.BadRequestException;
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
    public String replaceTokensWithValues(String queryString, Map<String, Object> params) 
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
     * @return the query with all tokens replaced 
     */
    public String replaceTokens(String queryString, String replacement) {
        Matcher matcher = tokenPattern.matcher(queryString);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String origToken = matcher.group(1);
            //TODO: the size check seems invalid
            if (origToken != null) {
                // OrientDB token is of format :token-name
                String newToken = replacement;
                matcher.appendReplacement(buf, "");
                buf.append(newToken);
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }
    
    /**
     * Extracts all the token names in the query string  of format ${token-name} 
     * 
     * @param queryString the query with tokens
     * @return the list of token names, in the order they appear in the queryString
     */
    public List<String> extractTokens(String queryString) {
        List<String> tokens = new ArrayList();
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