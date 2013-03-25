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

package org.forgerock.openidm.internal.recon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.source.SourceUnit;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ConfigurationProvider {

    private static final String CASE_SENSITIVE_ID_ATTR = "caseSensitiveId";
    private static final String ON_LINK_ATTR = "onLink";
    private static final String CORRELATION_QUERY_ATTR = "correlationQuery";
    private static final String ON_CONFIRMATION_ATTR = "onConfirmation";
    private static final String REVERSE_LINK_ATTR = "reverse";

    static enum Mode {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE;

        public static Mode from(JsonValue reconMode) {
            String mode = null == reconMode ? null : reconMode.asString();
            if (mode == null || mode.length() == 10) {
                return ONE_TO_ONE;
            } else {
                String m = mode.toLowerCase();
                if ("one-to-many".equals(m)) {
                    return ONE_TO_MANY;
                } else if ("many-to-one".equals(m)) {
                    return MANY_TO_ONE;
                } else {
                    return reconMode.defaultTo(ONE_TO_ONE.name()).asEnum(Mode.class);
                }
            }
        }
    }

    /**
     * Name of the Reconciliation configuration.
     */
    protected final String name;

    /**
     * The {@link Mode} defines the relation of {@code source} and
     * {@code target} collections.
     * <p/>
     * This defines the further optimization and required configuration.
     */
    protected final Mode relation;

    public ConfigurationProvider(String name, JsonValue relation) {
        this.name = name;
        this.relation = Mode.from(relation);
    }

    public Mode getRelation() {
        return relation;
    }

    /**
     * Whether to link source IDs in a case sensitive fashion. Default to
     * {@code TRUE}
     */
    protected Boolean sourceIdCaseSensitive = true;

    /**
     * Query used to get the source resource set.
     */
    protected QueryRequest sourceQuery = null;

    /**
     * Script is called to generate the link if links are not queried.
     */
    protected JsonValue sourceLinkScript = null;

    /**
     * Query used to correlate the source and target resource. Later support
     * query per linkType but preferred to use the script.
     */
    protected QueryRequest correlationQuery = null;

    // protected ScriptEntry correlationScript = null;
    /**
     * Script is called to generate the query, or query the target.
     */
    protected JsonValue correlationScript = null;

    /**
     * Script is called to confirm the one of the multiple target resources.
     */
    protected JsonValue confirmationScript = null;

    public QueryRequest getSource() {
        return sourceQuery;
    }

    /**
     * This depends on setTarget because the correlation resourceName must be
     * set
     * 
     * @param source
     */
    public void setSource(JsonValue source) {
        // TODO Replace
        source.put("requestType", "query");
        Request request = ResourceUtil.requestFromJsonValue(source);
        if (request instanceof QueryRequest) {
            sourceQuery = (QueryRequest) request;
            sourceIdCaseSensitive = source.get(CASE_SENSITIVE_ID_ATTR).defaultTo(true).asBoolean();

            if (source.isDefined(ON_LINK_ATTR)) {
                JsonValue script = source.get(ON_LINK_ATTR).required().expect(Map.class);
                sourceLinkScript = expectScript(script);
            }

            if (source.isDefined(ON_CONFIRMATION_ATTR)) {
                JsonValue confirmation =
                        source.get(ON_CONFIRMATION_ATTR).required().expect(Map.class);
                // Script ?
                confirmationScript = expectScript(confirmation);
            }

            if (source.isDefined(CORRELATION_QUERY_ATTR)) {
                JsonValue correlation =
                        source.get(CORRELATION_QUERY_ATTR).required().expect(Map.class);
                // Script or Query?
                if (correlation.isDefined(SourceUnit.ATTR_NAME)
                        && correlation.isDefined(SourceUnit.ATTR_TYPE)) {
                    correlationScript = correlation;
                } else {
                    // TODO Replace
                    correlation.put("requestType", "query");
                    if (null != targetQuery) {
                        correlation.put("resourceName", targetQuery.getResourceName());
                    } else if (null != targetRead) {
                        correlation.put("resourceName", targetRead.getResourceName());
                    }
                    request = ResourceUtil.requestFromJsonValue(correlation);
                    if (request instanceof QueryRequest) {
                        correlationQuery = (QueryRequest) request;
                    } else {
                        throw new JsonValueException(correlation,
                                "Script or QueryRequest structure was expected");
                    }
                }
            }
        } else {
            new JsonValueException(source, "Source is not a QueryRequest");
        }
    }

    /*
     * The target collection can be pre-fetched with this query if necessary or
     * read one-by-one.
     */

    /**
     * Whether to link target IDs in a case sensitive fashion. Default to
     * {@code TRUE}
     */
    protected Boolean targetIdCaseSensitive = true;

    /**
     * Query used to get the source resource set. The default is
     * {@code targetCollection?_queryId=query-all-ids}
     */
    protected QueryRequest targetQuery = null;

    protected ReadRequest targetRead = null;

    // protected ScriptEntry targetLinkScript = null;
    /**
     * Script is called to generate the link if links are not queried.
     */
    protected JsonValue targetLinkScript = null;

    public Request getTarget() {
        return null != targetQuery ? targetQuery : targetRead;
    }

    public void setTarget(JsonValue source) {
        Request request = ResourceUtil.requestFromJsonValue(source);
        targetIdCaseSensitive = source.get(CASE_SENSITIVE_ID_ATTR).defaultTo(true).asBoolean();

        if (request instanceof QueryRequest) {
            targetQuery = (QueryRequest) request;

        } else if (request instanceof ReadRequest) {
            targetRead = (ReadRequest) request;

        } else {
            new JsonValueException(source, "Source is not a QueryRequest");
        }

        if (source.isDefined(ON_LINK_ATTR)) {
            JsonValue script = source.get(ON_LINK_ATTR).required().expect(Map.class);
            targetLinkScript = expectScript(script);
        }
    }

    /**
     * First part of the recon to get the links. This considered the smallest
     * data necessary to pre-load. Optimizations are the following:
     * 
     * <pre>
     * Linking is enabled: 
     *  -Query all and cache Link data in memory 
     *  -Query one-by-one the Link data and cache it in the memory if the target is pre-loaded too
     * 
     * Linking is not enabled: 
     *  -Generate the link on-the-fly with the same _id (only for {@link Mode#ONE_TO_ONE})
     *  -Execute a script to generate the link on-the-fly
     * </pre>
     */

    protected boolean reverseLink = false;

    /**
     * Query used to get the existing links for the {@code sourceQuery}.
     */
    protected QueryRequest linkQuery = null;

    /**
     * Query used to find the link for with source object.
     */
    protected QueryRequest linkQueryWithSource = null;

    /**
     * Query used to find the link for with target object.
     */
    protected QueryRequest linkQueryWithTarget = null;

    public void setLink(JsonValue link) {
        // TODO Replace
        link.put("requestType", "query");
        reverseLink = link.get(REVERSE_LINK_ATTR).defaultTo(false).asBoolean();

        Request request = null;
        try {
            request = ResourceUtil.requestFromJsonValue(link);
            if (request instanceof QueryRequest) {
                linkQuery = (QueryRequest) request;
            } else {
                /*
                 * Links are not preloaded
                 */
            }
        } catch (JsonValueException e) {
            if (link.isDefined("resourceName")) {
                throw e;
            }
        }

        if (link.isDefined("queryForSource")) {
            /*
             * Links are loaded per request.
             */
            JsonValue queryJson = link.get("queryForSource");
            // TODO Replace
            queryJson.put("requestType", "query");
            request = ResourceUtil.requestFromJsonValue(queryJson);
            if (request instanceof QueryRequest) {
                linkQueryWithSource = (QueryRequest) request;
                if (isPreLoadTargetCollection()) {
                    queryJson = link.get("queryForTarget").required();
                    // TODO Replace
                    queryJson.put("requestType", "query");
                    request = ResourceUtil.requestFromJsonValue(queryJson);
                    if (request instanceof QueryRequest) {
                        linkQueryWithTarget = (QueryRequest) request;

                    } else {
                        throw new JsonValueException(queryJson, "Query structure was expected");
                    }
                }

            } else {
                throw new JsonValueException(queryJson, "Query structure was expected");
            }
        }
    }

    private EnumMap<ReconSituation, List<Object>> policyMap = null;

    public void setPolicies(JsonValue policy) {
        if (!policy.isNull()) {
            policyMap = new EnumMap<ReconSituation, List<Object>>(ReconSituation.class);
            for (ReconSituation situation : ReconSituation.values()) {
                JsonValue value = policy.get(situation.name());
                if (value.isNull()) {
                    policyMap.put(situation, null);
                    continue;
                }
                if (value.isMap()) {
                    policyMap.put(situation, Arrays.<Object> asList(checkPolicy(value)));
                } else if (value.isList()) {
                    List<Object> policyList = new ArrayList<Object>(value.size());
                    for (JsonValue policyValue : value) {
                        policyList.add(checkPolicy(policyValue));
                    }
                    policyMap.put(situation, policyList);
                } else {
                    throw new JsonValueException(value, "Expecting List or Map");
                }
            }
        }
    }

    //TODO rename the method
    private Object checkPolicy(JsonValue policyValue) {
        // Script or Query?
        if (policyValue.isDefined(SourceUnit.ATTR_NAME)
                && policyValue.isDefined(SourceUnit.ATTR_TYPE)) {
            return policyValue;
        } else {
            return ResourceUtil.requestFromJsonValue(policyValue);
        }
    }

    EnumMap<ReconSituation, List<Object>> getPolicies() {
        return policyMap;
    }

    /**
     * Indicates if the entire {@code target} collection should be preloaded
     * into memory before the {@code source} reconciliation phase.
     * 
     * @return if the {@link #targetQuery} is set.
     */
    protected boolean isPreLoadTargetCollection() {
        return getTarget() instanceof QueryRequest;
    }

    /**
     * Indicates that the links between the {@code source} and {@code target}
     * collections are persisted and should be loaded before the reconciliation.
     * 
     * @return {@literal true} if the links should be loaded.
     */
    public boolean isPreLoadLinkingData() {
        return linkQuery != null;
    }

    /**
     * Indicates that the links between the {@code source} and {@code target}
     * collections are persisted and should be queried during the
     * reconciliation.
     * 
     * @return {@literal true} if the links should be queried.
     */
    public boolean hasPersistedLink() {
        return isPreLoadLinkingData() || null != linkQueryWithSource;
    }

    private JsonValue expectScript(JsonValue script) {
        if (script.isDefined(SourceUnit.ATTR_NAME) && script.isDefined(SourceUnit.ATTR_TYPE)) {
            return script;
        } else {
            throw new JsonValueException(script, "Script structure was expected");
        }
    }
}
