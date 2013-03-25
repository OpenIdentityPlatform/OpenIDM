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

import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.Script;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ReconUtil {

    public static final String LINK_SOURCE_ID_FIELD = "sourceId";
    public static final String LINK_TARGET_ID_FIELD = "targetId";
    public static final String LINK_TYPE_FIELD = "linkType";
    public static final String AMBIGUOUS_FIELD = "ambiguous";
    public static final String LINK_FIELD = "link";
    public static final String SOURCE_FIELD = "source";
    public static final String DEFAULT_LINK_TYPE = "#";
    public static final String TARGET_FIELD = "target";
    public static final String SOMETHING_TO_NAME = "something";
    public static final String ERROR_FIELD = "error";

    static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Setup logging for the {@link ReconUtil}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ReconUtil.class);

    private ReconUtil() {
        // Prevent instantiation.
    }

    // ----- Start CREST Common methods //TODO Migrate over

    public static Map<String, Object> resourceToMap(final Resource resource, boolean caseSensitiveId) {
        if (null != resource) {
            Map<String, Object> result = new HashMap<String, Object>(3);
            if (caseSensitiveId && null != resource.getId()) {
                result.put("id", resource.getId());
            } else if (null != resource.getId()) {
                result.put("id", resource.getId().toLowerCase());
            }
            if (null != resource.getRevision()) {
                result.put("revision", resource.getRevision());
            }
            if (null != resource.getContent()) {
                result.put("content", resource.getContent().getObject());
            }
            return result;
        }
        return null;
    }

    // ----- End CREST Common methods //TODO Migrate over

    public static String tripletToJSON(Triplet triplet) {
        if (null != triplet) {
            final StringWriter writer = new StringWriter();
            try {
                final JsonGenerator g = JSON_MAPPER.getJsonFactory().createJsonGenerator(writer);
                g.writeStartObject();

                Triplet.Vertex source = triplet.source();
                Triplet.Vertex target = triplet.target();

                if (source.exits()) {
                    g.writeFieldName(SOURCE_FIELD);
                    g.writeString(source.getId());
                }

                Triplet.Edge edge = triplet.link();
                if (edge.exits()) {
                    g.writeFieldName(LINK_FIELD);
                    g.writeStartObject();
                    if (null != edge.linkType()) {
                        g.writeStringField(LINK_TYPE_FIELD, edge.linkType());
                    }

                    if (!source.exits()) {
                        g.writeStringField(LINK_SOURCE_ID_FIELD, edge.sourceId());
                    }
                    if (!target.exits()) {
                        g.writeStringField(LINK_TARGET_ID_FIELD, edge.sourceId());
                    }
                    g.writeEndObject();
                } else {
                    g.writeNullField(LINK_FIELD);
                }

                if (target.exits()) {
                    g.writeFieldName(TARGET_FIELD);
                    g.writeString(target.getId());
                }

                g.writeEndObject();
                g.flush();
            } catch (IOException e) {
                /* ignore */
            } finally {
                try {
                    writer.close();
                    return writer.toString();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }

    public static Set<Resource> executeQuery(final ServerContext context, final QueryRequest query,
            final Triplet triplet, JsonValue defaultProperties) throws ResourceException,
            JsonValueException {

        defaultProperties.asMap().putAll(triplet.map());

        JsonValue jsonQuery = ResourceUtil.requestToJsonValue(query);
        jsonQuery.getTransformers().add(
                JsonUtil.getPropertyJsonTransformer(defaultProperties, true));
        Request request = ResourceUtil.requestFromJsonValue(jsonQuery.copy());

        return executeQuery(context, (QueryRequest) request);
    }

    public static Set<Resource> executeQuery(final ServerContext context, final QueryRequest request)
            throws ResourceException {
        Set<Resource> result = new HashSet<Resource>(1);
        do {
            QueryResult queryResult = context.getConnection().query(context, request, result);
            if (queryResult == null) {
                /*
                 * Likely the query failed.
                 */
                break;
            } else if (queryResult.getRemainingPagedResults() < 1) {
                // We did fetch all resources
                break;
            } else {
                // Continue to fetch all
                request.setPagedResultsCookie(queryResult.getPagedResultsCookie());
            }
        } while (true);
        return result;
    }

    public static Set<Map<String, Object>> getLinksWithScript(final Script script)
            throws ResourceException {
        /*
         * Call a script to return with the sourceId and linkType
         */
        final Set<Map<String, Object>> generatedLinks = new HashSet<Map<String, Object>>();

        /*
         * defineLink(String resourceId, String linkType, Map linkData)
         */
        script.putSafe("defineLink", new Function<Void>() {
            @Override
            public Void call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length != 3) {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                            "defineLink", arguments));
                }

                String resourceId = null;
                String linkType = null;
                JsonValue content = null;

                for (int i = 0; i < arguments.length; i++) {
                    Object value = arguments[i];
                    switch (i) {
                    case 0:
                        if (value instanceof String) {
                            resourceId = (String) value;
                        } else {
                            throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                    "defineLink", arguments));
                        }
                        break;
                    case 1:
                        if (value instanceof String) {
                            linkType = (String) value;
                        } else if (null != value) {
                            throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                    "defineLink", arguments));
                        }
                        break;
                    case 2:
                        if (value instanceof Map) {
                            content = new JsonValue(value);
                        } else if (value instanceof JsonValue && ((JsonValue) value).isMap()) {
                            content = (JsonValue) value;
                        } else if (null != value) {
                            throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                    "defineLink", arguments));
                        }
                    }
                }

                Map<String, Object> link = new HashMap<String, Object>(2);
                // generatedLinks.add(link);

                // TODO Return with links

                return null;
            }
        });
        // /script.put("target", resource.getContent());
        try {
            script.eval();

            /*
             * This is a fatal exception because if the target object is not
             * well assessed then it leads to wrong situation.
             */
        } catch (ScriptThrownException e) {
            // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
            // name,
            // scriptPair.snd.getName(), scriptPair.fst, e);
            throw e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
        } catch (ScriptException e) {
            // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
            // name,
            // scriptPair.snd.getName(), scriptPair.fst, e);
            throw new InternalServerErrorException(e);
        } catch (Exception e) {
            // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
            // name,
            // scriptPair.snd.getName(), scriptPair.fst, e);
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        return generatedLinks;
    }

    // /**
    // * Executes the given script with the appropriate context information
    // *
    // * @param type
    // * The script hook name
    // * @param script
    // * The script to execute
    // * @throws ResourceException
    // * TODO.
    // */
    // private void execScript(final Context context,
    // final Utils.Pair<JsonPointer, ScriptEntry> scriptPair) throws
    // ResourceException {
    // if (scriptPair != null) {
    // if (scriptPair.snd.isActive()) {
    // throw new
    // ServiceUnavailableException("Failed to execute inactive script: "
    // + scriptPair.snd.getName());
    // }
    // Script script = scriptPair.snd.getScript(context);
    //
    // // TODO: Once script engine can do on-demand get replace these
    // // forced loads
    // if (getSourceObjectId() != null) {
    // script.put("source", getSourceObject().asMap());
    // }
    // // Target may not have ID yet, e.g. an onCreate with the target
    // // object defined,
    // // but not stored/id assigned.
    // if (isTargetLoaded() || getTargetObjectId() != null) {
    // if (getTargetObject() != null) {
    // script.put("target", getTargetObject().asMap());
    // }
    // }
    // if (situation != null) {
    // script.put("situation", situation.toString());
    // }
    // try {
    // script.eval();
    // } catch (ScriptThrownException e) {
    // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
    // name,
    // scriptPair.snd.getName(), scriptPair.fst, e);
    // throw e.toResourceException(ResourceException.INTERNAL_ERROR,
    // e.getMessage());
    // } catch (ScriptException e) {
    // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
    // name,
    // scriptPair.snd.getName(), scriptPair.fst, e);
    // throw new InternalServerErrorException(e);
    // } catch (Exception e) {
    // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
    // name,
    // scriptPair.snd.getName(), scriptPair.fst, e);
    // throw new InternalServerErrorException(e.getMessage(), e);
    // }
    // }
    // }

    public static class Triplet {

        private final Map<String, Object> triplet;
        private Vertex source = new Vertex(SOURCE_FIELD);
        private Vertex target = new Vertex(TARGET_FIELD);
        private Edge link = new Edge();

        public static Triplet fromTriplet(final Map<String, Object> triplet) {
            return new Triplet(triplet);
        }
        public static Triplet buildWithTarget(final Map<String, Object> triplet) {
            Triplet t = new Triplet(new HashMap<String, Object>(3));
            t.triplet.put(TARGET_FIELD, triplet);
            return t;
        }

        public static Edge fromLink(final Map<String, Object> link) {
            return fromTriplet(new HashMap<String, Object>(1)).link().setLink(link);
        }

        public static Vertex fromTarget(final Resource resource, boolean caseSensitiveId) {
            return fromTriplet(new HashMap<String, Object>(1)).target().setResource(resource,
                    caseSensitiveId);
        }

        public static Vertex fromSource(final Resource resource, boolean caseSensitiveId) {
            return fromTriplet(new HashMap<String, Object>(1)).source().setResource(resource,
                    caseSensitiveId);
        }

        private Triplet(final Map<String, Object> triplet) {
            this.triplet = triplet;
        }

        public Map<String, Object> map() {
            return triplet;
        }

        public ReconSituation assess() {
            int situation = hasError() ? 16 : 0;
            if (isAmbiguous()) {
                situation = situation + 8;
            }
            if (target().exits()) {
                situation = situation + 4;
            }
            if (link().exits()) {
                situation = situation + 2;
            }
            if (source().exits()) {
                situation = situation + 1;
            }
            return ReconSituation.from(situation);
        }

        public Vertex source() {
            return source;
        }

        public Vertex target() {
            return target;
        }

        public Edge link() {
            return link;
        }

        boolean isAmbiguous() {
            return triplet.containsKey(AMBIGUOUS_FIELD);
        }

        boolean hasError() {
            return triplet.containsKey(ERROR_FIELD);
        }

        public Triplet ambiguous(final Map<String, Object> ambiguous) {
            if (null == ambiguous) {
                return this;
            }
            List<Map<String, Object>> _am =
                    (List<Map<String, Object>>) triplet.get(AMBIGUOUS_FIELD);
            if (null == _am) {
                _am = new ArrayList<Map<String, Object>>(1);
                triplet.put(AMBIGUOUS_FIELD, _am);
            }
            _am.add(ambiguous);
            return this;
        }

        public Triplet error(ResourceException exception) {
            if (null != exception) {
                Object error = exception.toJsonValue().asMap();
                if (hasError()) {
                    Object o = triplet.get(ERROR_FIELD);
                    if (o instanceof Map) {
                        List<Object> errors = new ArrayList<Object>(2);
                        errors.add(o);
                        errors.add(error);
                        triplet.put(ERROR_FIELD, errors);
                    } else if (o instanceof ResourceException) {
                        List<Object> errors = new ArrayList<Object>(2);
                        errors.add(((ResourceException) o).toJsonValue().asMap());
                        errors.add(error);
                        triplet.put(ERROR_FIELD, errors);
                    } else if (o instanceof Collection) {
                        ((Collection) o).add(error);
                    } else {
                        triplet.put(ERROR_FIELD, error);
                    }
                } else {
                    triplet.put(ERROR_FIELD, error);
                }
            }
            return this;
        }

        public Triplet match(final Map<String, Object> match) {
            if (null != match) {
                Object o = triplet.get(SOURCE_FIELD);
                if (o instanceof Collection) {
                    ((Collection) o).add(match);
                } else {
                    List<Map<String, Object>> l = new ArrayList<Map<String, Object>>(1);
                    l.add(match);
                    triplet.put(SOURCE_FIELD, l);
                }
            }
            return this;
        }

        public Triplet confirm(final Map<String, Object> target) {
            triplet.remove(SOURCE_FIELD);
            target().setResource(target);
            return this;
        }

        @Override
        public String toString() {
            return tripletToJSON(this);
        }


        public class Vertex {
            private String top_field;

            private Vertex(String top_field) {
                this.top_field = top_field;
            }

            public String getId() {
                if (triplet.containsKey(top_field)) {
                    return (String) ((Map) triplet.get(top_field)).get("id");
                } else {
                    return null;
                }
            }

            public Vertex setResource(final Resource resource, boolean caseSensitiveId) {
                Map<String, Object> r = resourceToMap(resource, caseSensitiveId);
                triplet.put(top_field, r);
                return this;
            }

            public Vertex setResource(final Vertex resource) {
                triplet.put(top_field, resource.map());
                return this;
            }

            private Vertex setResource(final Map<String, Object> target) {
                triplet.put(top_field, target);
                return this;
            }

            public Triplet triplet() {
                return Triplet.this;
            }

            public boolean exits() {
                return triplet.get(top_field) instanceof Map
                        || triplet.get(top_field) instanceof SoftReference;
            }

            public boolean notFound() {
                return triplet.containsKey(top_field) && null == triplet.get(top_field);
            }

            public Map<String, Object> map() {
                Object o = triplet.get(top_field);
                if (o instanceof Map) {
                    return (Map<String, Object>) o;
                } else if (o instanceof SoftReference) {
                    Object ref = ((SoftReference) o).get();
                    if (ref == null) {
                        triplet().error(
                                ResourceException.getException(ResourceException.INTERNAL_ERROR,
                                        "Recon engine failed to resolve internal reference"));
                        // Error !!! Reference is broken
                    }
                    if (ref == null || ref instanceof Map) {
                        triplet.put(top_field, ref);
                        return (Map<String, Object>) ref;
                    }
                }
                return (Map<String, Object>) o;
            }
        }

        public class Edge {
            String sourceId() {
                if (triplet.containsKey(LINK_FIELD)) {
                    return (String) ((Map) triplet.get(LINK_FIELD)).get(LINK_SOURCE_ID_FIELD);
                } else {
                    return null;
                }
            }

            String targetId() {
                if (triplet.containsKey(LINK_FIELD)) {
                    return (String) ((Map) triplet.get(LINK_FIELD)).get(LINK_TARGET_ID_FIELD);
                } else {
                    return null;
                }
            }

            public boolean exits() {
                return triplet.get(LINK_FIELD) instanceof Map;
            }

            String linkType() {
                if (triplet.containsKey(LINK_FIELD)) {
                    return (String) ((Map) triplet.get(LINK_FIELD)).get(LINK_TYPE_FIELD);
                } else {
                    return null;
                }
            }

            public Edge setLink(final Map<String, Object> link) {
                triplet.put(LINK_FIELD, link);
                return this;
            }

            public Triplet triplet() {
                return Triplet.this;
            }

            public Map<String, Object> map() {
                return (Map<String, Object>) triplet.get(LINK_FIELD);
            }
        }
    }

}
