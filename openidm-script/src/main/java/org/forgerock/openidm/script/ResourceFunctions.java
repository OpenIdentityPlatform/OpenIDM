/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.script;

import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;

/**
 * Exposes a function that can be provided to a script to invoke.
 *
 * @author Laszlo Hordos
 */
public final class ResourceFunctions {

    private ResourceFunctions() {
    }

    public static Function<JsonValue> newCreateFunction(ConnectionFactory connectionFactory) {
        return new CreateFunction(connectionFactory);
    }

    /**
     * <pre>
     * create(String resourceContainer, String newResourceId, Map content[, Map params][, List fieldFilter][, Map context])
     * </pre>
     */
    private static final class CreateFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private CreateFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                throws ResourceException, NoSuchMethodException {
            String resourceContainer = null;
            String newResourceId = null;
            JsonValue content = null;
            JsonValue params = new JsonValue(null);
            List<Object> fieldFilter = null;
            Context context = null;

            if (arguments.length < 3) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("create",
                        arguments));
            }

            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (i) {
                case 0:
                    if (value instanceof String) {
                        resourceContainer = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "create", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof String) {
                        newResourceId = (String) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "create", arguments));
                    }
                    break;
                case 2:
                    if (value instanceof Map) {
                        content = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        content = (JsonValue) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "create", arguments));
                    }
                    break;
                case 3:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else if (null != value && arguments.length > 4) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "create", arguments));
                    }
                    break;
                case 4:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 5) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "create", arguments));
                    }
                case 5:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "create", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
            }

            return create(scope, resourceContainer, newResourceId, content, params, fieldFilter, context,
                    callback).getContent();
        }

        public ResourceResponse create(final Parameter scope, String resourceContainer,
                String newResourceId, JsonValue content, JsonValue params, List<Object> fieldFilter,
                Context context, final Function<?> callback) throws ResourceException {
            CreateRequest cr =
                    Requests.newCreateRequest(resourceContainer, newResourceId, new JsonValue(
                            content));
            // add fieldFilter
            cr.addField(fetchFields(fieldFilter));
            for (String name : params.keys()) {
                setAdditionalParameter(cr, name, params.get(name));
            }

            return connectionFactory.getConnection().create(scope.getContext(context), cr);
        }

    }

    public static Function<JsonValue> newReadFunction(ConnectionFactory connectionFactory) {
        return new ReadFunction(connectionFactory);
    }

    /**
     * <pre>
     * read(String resourceName[, Map params][, List fieldFilter][,Map context])
     * </pre>
     */
    private static final class ReadFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private ReadFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(final Parameter scope, final Function<?> callback,
                Object... arguments) throws ResourceException, NoSuchMethodException {
            String resourceName = null;
            List<Object> fieldFilter = null;
            JsonValue params = new JsonValue(null);
            Context context = null;
            if (arguments.length < 1) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("read",
                        arguments));
            }

            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (i) {
                case 0:
                    if (value instanceof String) {
                        resourceName = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "read", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else if (null != value && arguments.length > 2) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "read", arguments));
                    }
                    break;
                case 2:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 3) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "read", arguments));
                    }
                case 3:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "read", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
            }

            JsonValue result = null;
			try {
				result = read(scope, resourceName, params, fieldFilter, context, callback).getContent();
			} catch (NotFoundException e) {
				// indicates no such record without throwing exception
				return null;
			}
            return result;
        }

        public ResourceResponse read(final Parameter scope, String resourceName, JsonValue params,
                List<Object> fieldFilter, Context context, final Function<?> callback)
                throws ResourceException {

            ReadRequest rr = Requests.newReadRequest(resourceName);
            // add fieldFilter
            rr.addField(fetchFields(fieldFilter));
            for (String name : params.keys()) {
                setAdditionalParameter(rr, name, params.get(name));
            }

            return connectionFactory.getConnection().read(scope.getContext(context), rr);
        }
    }

    public static Function<JsonValue> newUpdateFunction(ConnectionFactory connectionFactory) {
        return new UpdateFunction(connectionFactory);
    }

    /**
     * <pre>
     * update(String resourceName, String revision, Map content [, Map params][, List fieldFilter][,Map context])
     * </pre>
     */
    private static final class UpdateFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private UpdateFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(final Parameter scope, final Function<?> callback,
                final Object... arguments) throws ResourceException, NoSuchMethodException {

            String resourceName = null;
            String revision = null;
            JsonValue content = null;
            JsonValue params = new JsonValue(null);
            List<Object> fieldFilter = null;
            Context context = null;

            if (arguments.length < 3) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("update",
                        arguments));
            }

            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (i) {
                case 0:
                    if (value instanceof String) {
                        resourceName = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "update", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof String) {
                        revision = (String) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "update", arguments));
                    }
                    break;
                case 2:
                    if (value instanceof Map) {
                        content = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        content = (JsonValue) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "update", arguments));
                    }
                    break;
                case 3:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else if (null != value && arguments.length > 4) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "update", arguments));
                    }
                    break;
                case 4:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 5) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "update", arguments));
                    }
                case 5:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "update", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
            }

            return update(scope, resourceName, revision, content, params, fieldFilter, context, callback)
                    .getContent();
        }

        private final ResourceResponse update(final Parameter scope, String resourceName, String revision,
                JsonValue content, JsonValue params, List<Object> fieldFilter, Context context,
                final Function<?> callback) throws ResourceException {

            UpdateRequest ur = Requests.newUpdateRequest(resourceName, content);
            // add fieldFilter
            ur.addField(fetchFields(fieldFilter));
            // set revision
            ur.setRevision(revision);
            // set additional parameters
            for (String name : params.keys()) {
                setAdditionalParameter(ur, name, params.get(name));
            }

            return connectionFactory.getConnection().update(scope.getContext(context), ur);
        }
    }

    public static Function<JsonValue> newPatchFunction(ConnectionFactory connectionFactory) {
        return new PatchFunction(connectionFactory);
    }

    /**
     * <pre>
     * patch(String resourceName, String revision, Map patch[, Map params][, List fieldFilter][,Map context])
     * </pre>
     */
    private static final class PatchFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private PatchFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                throws ResourceException, NoSuchMethodException {
            String resourceName = null;
            String revision = null;
            JsonValue patch = null;
            JsonValue params = new JsonValue(null);
            List<Object> fieldFilter = null;
            Context context = null;

            if (arguments.length < 3) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("patch",
                        arguments));
            }

            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (i) {
                case 0:
                    if (value instanceof String) {
                        resourceName = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof String) {
                        revision = (String) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                    break;
                case 2:
                    if (value instanceof List) {
                        patch = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        patch = (JsonValue) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                    break;
                case 3:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else if (null != value && arguments.length > 4) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                    break;
                case 4:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 5) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                case 5:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
            }

            return patch(scope, resourceName, revision, patch, params, fieldFilter, context, callback).getContent();
        }

        private final ResourceResponse patch(Parameter scope, String resourceName, String revision,
                JsonValue patch, JsonValue params, List<Object> fieldFilter, Context context,
                final Function<?> callback) throws ResourceException {
            // create the request
            PatchRequest pr = Requests.newPatchRequest(resourceName);
            // add operations
            List<PatchOperation> ops = PatchOperation.valueOfList(patch);
            pr.addPatchOperation(ops.toArray(new PatchOperation[ops.size()]));
            // add fieldFilter
            pr.addField(fetchFields(fieldFilter));
            // set revision
            pr.setRevision(revision);
            // set additional params
            for (String name : params.keys()) {
                setAdditionalParameter(pr, name, params.get(name));
            }

            return connectionFactory.getConnection().patch(scope.getContext(context), pr);
        }
    }

    public static Function<JsonValue> newQueryFunction(ConnectionFactory connectionFactory) {
        return new QueryFunction(connectionFactory);
    }

    /**
     * <pre>
     * query(String resourceContainer, Map params [, List fieldFilter][,Map context])
     * </pre>
     */
    private static final class QueryFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private QueryFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(Parameter scope, final Function<?> callback, Object... arguments)
                throws ResourceException, NoSuchMethodException {

            String resourceContainer = null;
            JsonValue params = new JsonValue(null);
            List<Object> fieldFilter = null;
            Context context = null;

            if (arguments.length < 2) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("query",
                        arguments));
            }

            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (i) {
                case 0:
                    if (value instanceof String) {
                        resourceContainer = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "query", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "query", arguments));
                    }
                    break;
                case 2:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 3) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "query", arguments));
                    }
                case 3:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "query", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
            }

            // warning: if you dont use poll or peek and only iterator()
            // (+.remove()) it will leak memory.
            LinkedList<Object> results =
                    null != callback ? null : new LinkedList<Object>();

            QueryResponse queryResponse =
                    query(scope, resourceContainer, params, fieldFilter, context, results, callback);

            JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(3));
            if (null != queryResponse) {
                result.put("pagedResultsCookie", queryResponse.getPagedResultsCookie());
                result.put("totalPagedResults", queryResponse.getTotalPagedResults());
            }
            if (null != results) {
                result.put("result", results);
            }
            return result;
        }

        private final QueryResponse query(final Parameter scope, String resourceContainer,
                JsonValue params, List<Object> fieldFilter, Context context,
                final Collection<Object> results, final Function<?> callback)
                throws ResourceException {
            if (params.isDefined("_queryId") ^ params.isDefined("_queryExpression")
                    ^ params.isDefined("_queryFilter")) {

                QueryRequest qr = Requests.newQueryRequest(resourceContainer);
                // add fieldFilter
                qr.addField(fetchFields(fieldFilter));
                for (String name : params.keys()) {
                    if (name.equalsIgnoreCase("_fields")
                            && (null == fieldFilter || fieldFilter.isEmpty())) {
                        JsonValue fields = params.get(name);
                        if (fields.isString()) {
                            try {
                                qr.addField(fields.asString().split(","));
                            } catch (final IllegalArgumentException e) {
                                // FIXME: i18n.
                                throw new BadRequestException(
                                        "The value '"
                                                + fields
                                                + "' for parameter '"
                                                + name
                                                + "' could not be parsed as a comma separated list of JSON pointers");
                            }
                        } else if (fields.isList()) {
                            qr.addField(fields.asList().toArray(new String[fields.size()]));
                        }
                    } else if (name.equalsIgnoreCase("_sortKeys")) {
                        JsonValue sortKey = params.get(name);
                        if (sortKey.isString()) {
                            try {
                                qr.addSortKey(sortKey.asString().split(","));
                            } catch (final IllegalArgumentException e) {
                                // FIXME: i18n.
                                throw new BadRequestException("The value '" + sortKey
                                        + "' for parameter '" + name
                                        + "' could not be parsed as a comma "
                                        + "separated list of sort keys");
                            }
                        } else if (sortKey.isList()) {
                            qr.addSortKey(sortKey.asList().toArray(new String[sortKey.size()]));
                        }
                    } else if (name.equalsIgnoreCase("_queryId")) {
                        qr.setQueryId(params.get(name).required().asString());
                    } else if (name.equalsIgnoreCase("_queryExpression")) {
                        qr.setQueryExpression(params.get(name).required().asString());
                    } else if (name.equalsIgnoreCase("_pagedResultsCookie")) {
                        qr.setPagedResultsCookie(params.get(name).required().asString());
                    } else if (name.equalsIgnoreCase("_pagedResultsOffset")) {
                        qr.setPagedResultsOffset(params.get(name).required().asInteger());
                    } else if (name.equalsIgnoreCase("_pageSize")) {
                        qr.setPageSize(params.get(name).required().asInteger());
                    } else if (name.equalsIgnoreCase("_queryFilter")) {
                        final String s = params.get(name).required().asString();
                        try {
                            qr.setQueryFilter(QueryFilters.parse(s));
                        } catch (final IllegalArgumentException e) {
                            // FIXME: i18n.
                            throw new BadRequestException("The value '" + s + "' for parameter '"
                                    + name + "' could not be parsed as a valid query filter");

                        }
                    } else {
                        setAdditionalParameter(qr, name, params.get(name));
                    }
                }

                return connectionFactory.getConnection().query(scope.getContext(context), qr,
                        new QueryResourceHandler() {
                            @Override
                            public boolean handleResource(ResourceResponse resource) {
                                if (null != callback) {
                                    try {
                                        callback.call(scope, null, resource.getContent());
                                    } catch (ResourceException e) {
                                        // TODO log
                                        return false;
                                    } catch (NoSuchMethodException e) {
                                        // TODO log
                                        return false;
                                    }
                                } else {
                                    results.add(resource.getContent().getObject());
                                }
                                return true;
                            }
                        });
            } else {
                throw new BadRequestException(
                        "Only one of [_queryId, _queryExpression, _queryFilter] is supported; multiple detected");
            }
        }
    }

    public static Function<JsonValue> newDeleteFunction(ConnectionFactory connectionFactory) {
        return new DeleteFunction(connectionFactory);
    }

    /**
     * <pre>
     * delete(String resourceName, String revision [, Map params][, List fieldFilter][,Map context])
     * </pre>
     */
    private static final class DeleteFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private DeleteFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                throws ResourceException, NoSuchMethodException {
            String resourceName = null;
            String revision = null;
            JsonValue params = new JsonValue(null);
            List<Object> fieldFilter = null;
            Context context = null;

            if (arguments.length < 2) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("delete",
                        arguments));
            }

            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (i) {
                case 0:
                    if (value instanceof String) {
                        resourceName = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "delete", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof String) {
                        revision = (String) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "delete", arguments));
                    }
                    break;
                case 2:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else if (null != value && arguments.length > 4) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "patch", arguments));
                    }
                    break;
                case 3:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 4) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "delete", arguments));
                    }
                case 4:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "delete", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
            }
            return delete(scope, resourceName, revision, params, fieldFilter, context, callback)
                    .getContent();
        }

        private ResourceResponse delete(Parameter scope, String resourceName, String revision, JsonValue params,
                List<Object> fieldFilter, Context context, final Function<?> callback)
                throws ResourceException {

            DeleteRequest dr = Requests.newDeleteRequest(resourceName);
            // add fieldFilter
            dr.addField(fetchFields(fieldFilter));
            // set revision
            dr.setRevision(revision);
            // set additional parameters
            for (String name : params.keys()) {
                setAdditionalParameter(dr, name, params.get(name));
            }

            return connectionFactory.getConnection().delete(scope.getContext(context), dr);
        }
    }

    public static Function<JsonValue> newActionFunction(ConnectionFactory connectionFactory) {
        return new ActionFunction(connectionFactory);
    }

    /**
     * <pre>
     * action(String resourceName, [String actionId,] Map content, Map params [, List fieldFilter][,Map context])
     * </pre>
     */
    private static final class ActionFunction extends AbstractFunction {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        private ActionFunction(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                throws ResourceException, NoSuchMethodException {

            String resourceName = null;
            String actionId = null;
            JsonValue content = null;
            JsonValue params = new JsonValue(null);
            List<Object> fieldFilter = null;
            Context context = null;

            if (arguments.length < 3) {
                throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("action",
                        arguments));
            }

            int pointer = 0;
            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i];
                switch (pointer) {
                case 0:
                    if (value instanceof String) {
                        resourceName = (String) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "action", arguments));
                    }
                    break;
                case 1:
                    if (value instanceof String) {
                        actionId = (String) value;
                        break;
                    } else if (null == value) {
                        break;
                    } else if (!(value instanceof Map) && (value instanceof JsonValue && !((JsonValue) value).isMap())) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "action", arguments));
                    } else {
                        // shift the param pointer
                        pointer++;
                    }
                case 2:
                    if (value instanceof Map) {
                        content = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        content = (JsonValue) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "action", arguments));
                    }
                    break;
                case 3:
                    if (value instanceof Map) {
                        params = new JsonValue(value);
                    } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                        params = (JsonValue) value;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "action", arguments));
                    }
                    break;
                case 4:
                    if (value instanceof List) {
                        fieldFilter = (List<Object>) value;
                        break;
                    } else if (value instanceof JsonValue && ((JsonValue) value).isList()) {
                        fieldFilter = ((JsonValue) value).asList();
                        break;
                    } else if (null != value && arguments.length > 5) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "action", arguments));
                    }
                case 5:
                    if (value instanceof Context) {
                        context = (Context) value;
                    } else if (null != value) {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "action", arguments));
                    }
                    break;
                default: // TODO log unused arguments
                }
                pointer++;
            }

            return action(scope, resourceName, actionId, content, params, fieldFilter, context,
                    callback).getJsonContent();
        }

        public ActionResponse action(final Parameter scope, String resourceName, String actionId,
                JsonValue content, JsonValue params, List<Object> fieldFilter, Context context,
                final Function<?> callback) throws ResourceException {

            ActionRequest ar =
                    Requests.newActionRequest(resourceName,
                            null != actionId ? actionId : params.get("_action").required().asString());
            // add fieldFilter
            ar.addField(fetchFields(fieldFilter));
            // set additional parameters
            for (String name : params.keys()) {
                setAdditionalParameter(ar, name, params.get(name));
            }
            // set content
            ar.setContent(content);

            return connectionFactory.getConnection().action(scope.getContext(context), ar);
        }
    }

    private static abstract class AbstractFunction implements Function<JsonValue> {

        /** Serializable class a version number. */
        static final long serialVersionUID = 2L;

        final ConnectionFactory connectionFactory;

        AbstractFunction(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        protected String[] fetchFields(List<Object> fields) {
            if (null != fields && !fields.isEmpty()) {
                int idx = 0;
                String[] checkedFields = new String[fields.size()];
                for (Object o : fields) {
                    if (o instanceof String) {
                        checkedFields[idx++] = (String) o;
                    }
                }
                return Arrays.copyOfRange(checkedFields, 0, idx);
            }
            return new String[0];
        }

        protected void setAdditionalParameter(Request request, String name, JsonValue value) throws BadRequestException {
            if (value.isNull()) {
                // ignore null values
            } else if (value.isString()) {
                request.setAdditionalParameter(name, value.asString());
            } else if (value.isNumber()) {
                request.setAdditionalParameter(name, String.valueOf(value.asNumber()));
            } else if (value.isBoolean()) {
                request.setAdditionalParameter(name, String.valueOf(value.asBoolean()));
            } else {
                throw new BadRequestException("The value '" + String.valueOf(value.getObject())
                        + "' for additional parameter '" + name
                        + "' is not of expected type String");
            }
        }
    }

    /*
     *
     * action(String endPoint[, String id], String type, Map params, Map
     * content[, List fieldFilter][,Map context])
     *
     * create(String endPoint[, String id], Map content[, List fieldFilter][,Map
     * context])
     *
     * delete(String endPoint, String id[, String rev][, List fieldFilter][,Map
     * context])
     *
     * patch(String endPoint[, String id], Map content [, String rev][, List
     * fieldFilter][,Map context])
     *
     * query(String endPoint[, Map params][, String filter][, List
     * fieldFilter][,Map context])
     *
     * read(String endPoint[, String id][, List fieldFilter][,Map context])
     *
     * update(String endPoint[, String id], Map content [, String rev][, List
     * fieldFilter][,Map context])
     *
     * @return
     */
    public static Map<String, Function<JsonValue>> resourceFunctions(final ConnectionFactory connectionFactory) {
        return new LazyMap<>(
                new Factory<Map<String, Function<JsonValue>>>() {
                    @Override
                    public Map<String, Function<JsonValue>> newInstance() {
                        Map<String, Function<JsonValue>> functions = new HashMap<String, Function<JsonValue>>(7);

                        functions.put("create", ResourceFunctions.newCreateFunction(connectionFactory));
                        functions.put("read", ResourceFunctions.newReadFunction(connectionFactory));
                        functions.put("update", ResourceFunctions.newUpdateFunction(connectionFactory));
                        functions.put("patch", ResourceFunctions.newPatchFunction(connectionFactory));
                        functions.put("query", ResourceFunctions.newQueryFunction(connectionFactory));
                        functions.put("delete", ResourceFunctions.newDeleteFunction(connectionFactory));
                        functions.put("action", ResourceFunctions.newActionFunction(connectionFactory));

                        return functions;
                    }
                });
    }
}
