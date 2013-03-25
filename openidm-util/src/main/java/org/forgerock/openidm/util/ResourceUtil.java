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

package org.forgerock.openidm.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ResourceUtil {

    /**
     * <p>
     * {@code ContextUtil} instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * {@code ContextUtil.parseResourceName(" /foo/bar/ ");}.
     * </p>
     * 
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * newBuilder to operate.
     * </p>
     */
    public ResourceUtil() {
        super();
    }

    /**
     * Create a default internal {@link SecurityContext} newBuilder used for
     * internal trusted calls.
     * <p/>
     * 
     * If the request is initiated in a non-authenticated location (
     * {@code BundleActivator}, {@code Scheduler}, {@code ConfigurationAdmin})
     * this contest should be used. The AUTHORIZATION module grants full access
     * to this context.
     * 
     * @param bundleContext
     *            the context of the OSGi Bundle.
     * @return new {@code SecurityContext} newBuilder.
     */
    public static SecurityContext createInternalSecurityContext(final BundleContext bundleContext) {

        // TODO Finalise the default system context
        Map<String, Object> authzid = new HashMap<String, Object>();
        authzid.put(SecurityContext.AUTHZID_COMPONENT, bundleContext.getBundle().getSymbolicName());
        authzid.put(SecurityContext.AUTHZID_ROLES, "system");
        authzid.put(SecurityContext.AUTHZID_GROUPS, "system");
        authzid.put(SecurityContext.AUTHZID_DN, "system");
        authzid.put(SecurityContext.AUTHZID_REALM, "system");
        authzid.put(SecurityContext.AUTHZID_ID, "system");
        return new SecurityContext(new RootContext(), bundleContext
                .getProperty(Constants.BUNDLE_SYMBOLICNAME), authzid);

    }

    /**
     * Retrieve the {@code UriTemplateVariables} from the context.
     * <p/>
     * 
     * @param context
     * 
     * @return an unmodifiableMap or null if the {@code context} does not
     *         contains {@link RouterContext}
     */
    public static Map<String, String> getUriTemplateVariables(Context context) {
        RouterContext routerContext =
                context.containsContext(RouterContext.class) ? context
                        .asContext(RouterContext.class) : null;
        if (null != routerContext) {
            return Collections.unmodifiableMap(routerContext.getUriTemplateVariables());
        }
        return null;
    }

    /**
     * Parse the given resource name into {@code resourceName} and possible
     * {@code resourceId}.
     * <p/>
     * Parser ignores the trailing {@code '/'} character in favour of
     * {@link org.forgerock.json.resource.servlet.HttpServletAdapter}. The
     * {@link org.forgerock.json.resource.Router} expects routes that matches
     * {@code '/resourceName/ id}/'} template <b>with</b> trailing {@code '/'}.
     * <p/>
     * The code removes the trailing and leading spaces only!
     * <p>
     * Examples:
     * <ul>
     * <li>{@code null} return null</li>
     * <li>{@code '/'} return {""}</li>
     * <li>{@code '/resourceName'} return {"resourceName"}</li>
     * <li>{@code '  /resourceName  '} return {"resourceName"}</li>
     * <li>{@code '/resourceName/'} return {"resourceName"}</li>
     * <li>{@code '/resourceName/resourceId'} return
     * {"resourceName","resourceId"}</li>
     * <li>{@code '/resourceName/type/resourceId'} return
     * {"resourceName/type","resourceId"}</li>
     * </ul>
     * </p>
     * 
     * 
     * @param resourceName
     *            The name of the JSON resource to which this request should be
     *            targeted.
     * @return The resource name spited at the last {@code '/'}
     */
    public static String[] parseResourceName(String resourceName) {
        if (StringUtils.isBlank(resourceName)) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(resourceName.trim(), "/", false);

        String lastNonBlank = null;
        StringBuilder builder = null;

        while (tokenizer.hasMoreElements()) {
            String next = tokenizer.nextToken();
            if (StringUtils.isNotBlank(next)) {
                if (null != lastNonBlank) {
                    if (null == builder) {
                        builder = new StringBuilder(lastNonBlank);
                    } else {
                        builder.append("/").append(lastNonBlank);
                    }
                }
                lastNonBlank = next;
            }
        }

        if (null != builder) {
            return new String[] { builder.toString(), lastNonBlank };
        } else if (null != lastNonBlank) {
            return new String[] { lastNonBlank };
        }
        return null;
    }

    public static class URLParser {

        private final StringTokenizer tokenizer;
        private String value;
        private int index = 0;
        private URLParser prev, next;

        public static URLParser parse(String resourceName) {
            return new URLParser(resourceName);
        }

        private URLParser(final URLParser parent) {
            prev = parent;
            tokenizer = null;
            index = parent.index + 1;
            StringTokenizer tk = getTokenizer();
            try {
                value = URLDecoder.decode(tk.nextToken(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                value = null;
            }
        }

        private StringTokenizer getTokenizer() {
            if (null != tokenizer) {
                return tokenizer;
            }
            return prev.getTokenizer();
        }

        private StringBuilder getStringBuilder() {
            if (this == prev) {
                return new StringBuilder("/").append(value());
            } else {
                return previous().getStringBuilder().append("/").append(value());
            }
        }

        public String resourceCollection() {
            if (this == prev) {
                return "/";
            }
            return previous().getStringBuilder().toString();
        }

        public URLParser(String resourceName) {
            if (null == resourceName) {
                throw new NullPointerException();
            }
            this.tokenizer = new StringTokenizer(resourceName.trim(), "/", false);
            if (tokenizer.hasMoreTokens()) {
                value = tokenizer.nextToken();
            } else {
                value = "";
            }
            // This is the head
            prev = this;
        }

        public URLParser previous() {
            return prev;
        }

        public URLParser next() {
            if (null == next) {
                if (getTokenizer().hasMoreTokens()) {
                    next = new URLParser(this);
                } else {
                    // This is the tail
                    next = this;
                }
            }
            return next;
        }

        public int index() {
            return index;
        }

        public URLParser get(int idx) {
            if (idx == index) {
                return this;
            } else if (idx < 0) {
                return first();
            } else if (idx < index) {
                return previous().get(idx);
            } else if (idx > index && next() == this) {
                return this;
            } else {
                return next().get(idx);
            }
        }

        public URLParser last() {
            return next() != this ? next().last() : next();
        }

        public URLParser first() {
            return previous() != this ? previous().first() : previous();
        }

        public String value() {
            return value;
        }

        public String resourceName() {
            String collection = resourceCollection();
            if (null == value || value.isEmpty()) {
                return collection;
            } else {
                if (collection.endsWith("/")) {
                    return collection + value;
                } else {
                    return collection + "/" + value;
                }
            }
        }
    }

    public static Resource resourceFromJsonValue(JsonValue resource) {
        if (null != resource && !resource.isNull()) {
            return new Resource(resource.expect(Map.class).get(Resource.FIELD_ID).required()
                    .asString(), resource.get(Resource.FIELD_REVISION).asString(), resource
                    .get(Resource.FIELD_CONTENT));
        } else {
            return null;
        }
    }

    public static JsonValue resourceToJsonValue(Resource resource) {
        final JsonValue wrapper = new JsonValue(new LinkedHashMap<String, Object>(3));
        wrapper.add(Resource.FIELD_ID, resource.getId());
        if (null != resource.getRevision()) {
            wrapper.add(Resource.FIELD_REVISION, resource.getRevision());
        }
        if (null != resource.getContent()) {
            wrapper.add(Resource.FIELD_CONTENT, resource.getContent().getObject());
        }
        return wrapper;
    }

    public static Request requestFromJsonValue(JsonValue request) {
        Request result = null;
        if (null != request) {
            String resourceName =
                    request.required().expect(Map.class).get(Request.FIELD_RESOURCE_NAME)
                            .required().asString();
            switch (request.get("requestType").required().asEnum(RequestType.class)) {
            case READ:
                result = Requests.newReadRequest(resourceName);
                break;
            case UPDATE:
                UpdateRequest ur =
                        Requests.newUpdateRequest(resourceName, request.get(
                                UpdateRequest.FIELD_NEW_CONTENT).required());
                ur.setRevision(request.get(UpdateRequest.FIELD_REVISION).asString());
                result = ur;
                break;
            case PATCH:
                throw new IllegalArgumentException("Patch is not supported");
                // break;
            case QUERY:
                QueryRequest qr = Requests.newQueryRequest(resourceName);
                if (request.isDefined(QueryRequest.FIELD_QUERY_FILTER)
                        ^ request.isDefined(QueryRequest.FIELD_QUERY_ID)
                        ^ request.isDefined(QueryRequest.FIELD_QUERY_EXPRESSION)) {
                    if (request.isDefined(QueryRequest.FIELD_QUERY_FILTER)) {
                        qr.setQueryFilter(QueryFilter.valueOf(request.get(
                                QueryRequest.FIELD_QUERY_FILTER).required().asString()));
                    } else if (request.isDefined(QueryRequest.FIELD_QUERY_ID)) {
                        qr.setQueryId(request.get(QueryRequest.FIELD_QUERY_ID).required()
                                .asString());
                    } else if (request.isDefined(QueryRequest.FIELD_QUERY_EXPRESSION)) {
                        qr.setQueryExpression(request.get(QueryRequest.FIELD_QUERY_EXPRESSION)
                                .required().asString());
                    }

                    if (request.isDefined(QueryRequest.FIELD_SORT_KEYS)) {
                        for (JsonValue sortKey : request.get(QueryRequest.FIELD_SORT_KEYS)) {
                            qr.addSortKey(sortKey.asString());
                        }
                    }
                    if (request.isDefined(QueryRequest.FIELD_PAGED_RESULTS_COOKIE)) {
                        qr.setPagedResultsCookie(request.get(
                                QueryRequest.FIELD_PAGED_RESULTS_COOKIE).asString());
                    }
                    if (request.isDefined(QueryRequest.FIELD_PAGED_RESULTS_OFFSET)) {
                        qr.setPagedResultsOffset(request.get(
                                QueryRequest.FIELD_PAGED_RESULTS_OFFSET).asInteger());
                    }
                    if (request.isDefined(QueryRequest.FIELD_PAGE_SIZE)) {
                        qr.setPageSize(request.get(QueryRequest.FIELD_PAGE_SIZE).asInteger());
                    }
                } else {
                    StringBuilder sb =
                            new StringBuilder("The query request must contain only one of [");
                    sb.append(QueryRequest.FIELD_QUERY_ID).append(",").append(
                            QueryRequest.FIELD_QUERY_EXPRESSION).append(",").append(
                            QueryRequest.FIELD_QUERY_FILTER).append("]");
                    throw new JsonValueException(request, sb.toString());
                }
                result = qr;
                break;
            case DELETE:
                DeleteRequest dr = Requests.newDeleteRequest(resourceName);
                dr.setRevision(request.get(DeleteRequest.FIELD_REVISION).asString());
                result = dr;
                break;
            case ACTION:
                String action = request.get(ActionRequest.FIELD_ACTION).required().asString();
                if (!ActionRequest.ACTION_ID_CREATE.equalsIgnoreCase(action)) {
                    ActionRequest ar = Requests.newActionRequest(resourceName, action);
                    ar.setContent(request.get(ActionRequest.FIELD_CONTENT));
                    JsonValue params =
                            request.get(ActionRequest.FIELD_ADDITIONAL_ACTION_PARAMETERS);
                    for (String key : params.keys()) {
                        ar.setAdditionalActionParameter(key, params.get(key).asString());
                    }
                    result = ar;
                    break;
                }
            case CREATE:
                result =
                        Requests.newCreateRequest(resourceName, request.get(
                                CreateRequest.FIELD_NEW_RESOURCE_ID).asString(), request
                                .get(CreateRequest.FIELD_CONTENT));
                break;
            default:
                throw new JsonValueException(request.get("requestType"), "Unknown requestType");
            }
            JsonValue _fields = request.get(Request.FIELD_FIELDS);
            if (_fields.isList()) {
                for (JsonValue field : _fields) {
                    result.addField(field.asPointer());
                }
            }
        }
        return result;
    }

    public static JsonValue requestToJsonValue(Request request) {
        if (null == request) {
            throw new NullPointerException();
        }
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        result.put("requestType", request.getRequestType().name());
        parseCommonParameter(result, request);
        switch (request.getRequestType()) {
        case READ: {
            break;
        }
        case PATCH: {
            break;
        }
        case UPDATE: {
            UpdateRequest ur = (UpdateRequest) request;
            if (null != ur.getRevision()) {
                result.put(UpdateRequest.FIELD_REVISION, ur.getRevision());
            }
            result.put(UpdateRequest.FIELD_NEW_CONTENT, ur.getNewContent().getObject());
            break;
        }
        case DELETE: {
            DeleteRequest dr = (DeleteRequest) request;
            if (null != dr.getRevision()) {
                result.put(DeleteRequest.FIELD_REVISION, dr.getRevision());
            }
            break;
        }
        case QUERY: {
            QueryRequest qr = (QueryRequest) request;
            if (null != qr.getQueryFilter() ^ null != qr.getQueryId()
                    ^ null != qr.getQueryExpression()) {
                if (null != qr.getQueryFilter()) {
                    result.put(QueryRequest.FIELD_QUERY_FILTER, qr.getQueryFilter().toString());
                } else if (null != qr.getQueryId()) {
                    result.put(QueryRequest.FIELD_QUERY_ID, qr.getQueryId());
                } else if (null != qr.getQueryExpression()) {
                    result.put(QueryRequest.FIELD_QUERY_EXPRESSION, qr.getQueryExpression());
                }
                if (null != qr.getSortKeys() && !qr.getSortKeys().isEmpty()) {
                    List<String> _sortKeys = new ArrayList<String>(qr.getSortKeys().size());
                    for (SortKey sortKey : qr.getSortKeys()) {
                        _sortKeys.add(sortKey.toString());
                    }
                    result.put(QueryRequest.FIELD_SORT_KEYS, _sortKeys);
                }
                if (null != qr.getPagedResultsCookie()) {
                    result.put(QueryRequest.FIELD_PAGED_RESULTS_COOKIE, qr.getPagedResultsCookie());
                }
                result.put(QueryRequest.FIELD_PAGED_RESULTS_OFFSET, qr.getPagedResultsOffset());
                result.put(QueryRequest.FIELD_PAGE_SIZE, qr.getPageSize());
            } else {
                StringBuilder sb =
                        new StringBuilder("The query request must contain only one of [");
                sb.append(QueryRequest.FIELD_QUERY_ID).append(",").append(
                        QueryRequest.FIELD_QUERY_EXPRESSION).append(",").append(
                        QueryRequest.FIELD_QUERY_FILTER).append("]");
                throw new JsonValueException(result, sb.toString());
            }
            break;
        }
        case ACTION: {
            ActionRequest ar = (ActionRequest) request;
            if (!ActionRequest.ACTION_ID_CREATE.equalsIgnoreCase(ar.getAction())) {

                result.put(ActionRequest.FIELD_ACTION, ar.getAction());

                if (null != ar.getAdditionalActionParameters()
                        && !ar.getAdditionalActionParameters().isEmpty()) {
                    result.put(ActionRequest.FIELD_ADDITIONAL_ACTION_PARAMETERS, ar
                            .getAdditionalActionParameters());
                }

                if (null != ar.getContent() && !ar.getContent().isNull()) {
                    result.put(ActionRequest.FIELD_CONTENT, ar.getContent().getObject());
                }
                break;
            } else {
                result.put(CreateRequest.FIELD_CONTENT, ar.getContent().getObject());
                result.put("requestType", RequestType.CREATE.name());
            }
        }
        case CREATE: {
            CreateRequest cr = (CreateRequest) request;
            result.put(CreateRequest.FIELD_NEW_RESOURCE_ID, cr.getNewResourceId());
            result.put(CreateRequest.FIELD_CONTENT, cr.getContent().getObject());
            break;
        }
        default:
            throw new IllegalArgumentException("Unknown request type");
        }
        return result;
    }

    private static void parseCommonParameter(final JsonValue json, final Request request) {
        json.put(Request.FIELD_RESOURCE_NAME, request.getResourceName());
        if (null != request.getFields() && !request.getFields().isEmpty()) {
            List<String> _fields = new ArrayList<String>(request.getFields().size());
            for (JsonPointer pointer : request.getFields()) {
                _fields.add(pointer.toString());
            }
            json.put(Request.FIELD_FIELDS, _fields);
        }
    }

    /**
     * Adapts a {@code Throwable} to a {@code ResourceException}. If the
     * {@code Throwable} is an JSON {@code JsonValueException} then an
     * appropriate {@code ResourceException} is returned, otherwise an
     * {@code InternalServerErrorException} is returned.
     * 
     * @param t
     *            The {@code Throwable} to be converted.
     * @return The equivalent resource exception.
     */
    public static ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        try {
            throw t;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (final Throwable tmp) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return ResourceException.getException(resourceResultCode, t.getMessage(), t);
    }
}
