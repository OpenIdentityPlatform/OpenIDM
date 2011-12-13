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

package org.forgerock.openidm.objset;

// Java SE
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;

// JSON Resource
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;

// OpenIDM
import org.forgerock.openidm.patch.JsonPatchWrapper;

/**
 * An adapter class that allows existing implementations of the deprecated {@code ObjectSet}
 * interface to implement the replacement {@code JsonResource} interface with minimal
 * changes. It is strongly recommended that new classes implement the {@code JsonResource}
 * interface rather than continue to implement {@code ObjectSet} and rely on this adapter. 
 * <p>
 * There are some "impedence mismatches" between the two interfaces, the most notable being
 * the fact that {@code JsonResource} implements rename in the {@code "patch"} method and
 * provides version information in the response, while {@code ObjectSet} returns silently.
 * If a patch renames an object, this implementation will return the old {@code "_id"} and
 * no {@code "_rev"} member. 
 *
 * @author Paul C. Bryan
 * @deprecated Implement {@code JsonResource} (or extend {@code SimpleJsonResource}) instead.
 */
@Deprecated
public class ObjectSetJsonResource extends SimpleJsonResource implements ObjectSet {

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ObjectSetJsonResource.class);

    /**
     * Returns a {@code JsonResourceException} that corresponds to the specified
     * {@code ObjectSetException}.
     *
     * @param exception the object set exception to convert to JSON resource exception.
     * @return the JSON resource exception corresponding to the object set exception.
     */
    private JsonResourceException convertException(ObjectSetException exception) {
        JsonResourceException prototype;
        if (exception instanceof BadRequestException) {
            prototype = JsonResourceException.BAD_REQUEST;
        } else if (exception instanceof ConflictException) {
            prototype = JsonResourceException.CONFLICT;
        } else if (exception instanceof ForbiddenException) {
            prototype = JsonResourceException.FORBIDDEN;
        } else if (exception instanceof NotFoundException) {
            prototype = JsonResourceException.NOT_FOUND;
        } else if (exception instanceof PreconditionFailedException) {
            prototype = JsonResourceException.VERSION_MISMATCH;
        } else if (exception instanceof ServiceUnavailableException) {
            prototype = JsonResourceException.UNAVAILABLE;
        } else {
            prototype = JsonResourceException.INTERNAL_ERROR;
        }
        return new JsonResourceException(prototype, exception.getMessage(), exception.getCause());
    }

    @Override // ObjectSet
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override // JsonResourceHandler
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        String id = request.get("id").asString();
        Map<String, Object> object = request.get("value").copy().asMap(); // copy; request should not be modified
        ObjectSetContext.push(request);
        try {
            create(id, object);
        } catch (ObjectSetException ose) {
            throw convertException(ose);
        } finally {
            ObjectSetContext.pop();
        }
        JsonValue response = new JsonValue(new LinkedHashMap<String, Object>());
        response.put("_id", object.get("_id"));
        if (object.containsKey("_rev")) {
            response.put("_rev", object.get("_rev"));
        }
        return response;
    }

    @Override // ObjectSet
    public Map<String, Object> read(String id) throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override // JsonResourceHandler
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        JsonValue response;
        String id = request.get("id").asString();
        ObjectSetContext.push(request);
        try {
            response = new JsonValue(read(id));
        } catch (ObjectSetException ose) {
            throw convertException(ose);
        } finally {
            ObjectSetContext.pop();
        }
        return response;
    }

    @Override // ObjectSet
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override // JsonResourceHandler
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        JsonValue response = new JsonValue(new LinkedHashMap<String, Object>());
        String id = request.get("id").asString();
        String rev = request.get("rev").asString();
        Map<String, Object> object = request.get("value").copy().asMap(); // copy; request should not be modified
        ObjectSetContext.push(request);
        try {
            update(id, rev, object);
        } catch (ObjectSetException ose) {
            throw convertException(ose);
        } finally {
            ObjectSetContext.pop();
        }
        response.put("_id", object.containsKey("_id") ? object.get("_id") : id);
        if (object.containsKey("_rev")) {
            response.put("_rev", object.get("_rev"));
        }
        return response;
    }

    @Override // ObjectSet
    public void delete(String id, String rev) throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override // JsonResourceHandler
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        String id = request.get("id").asString();
        String rev = request.get("rev").asString();
        ObjectSetContext.push(request);
        try {
            delete(id, rev);
        } catch (ObjectSetException ose) {
            throw convertException(ose);
        } finally {
            ObjectSetContext.pop();
        }
        return new JsonValue(null);
    }

    @Override // ObjectSet
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException();
    }

/*
 * This method in (deprecated!) ObjectSet does not cope with patching the value of _id
 * (i.e. rename). This is one of the reasons the response to "patch" method in JsonResource
 * now provides "_id" and "_rev" members. Without major work and loss of abstraction, it's
 * not feasible to resolve the issue here. Better to run the gauntlet and migrate your
 * implementation to JsonResource than spend time solving such an edge case in this adapter.
 */
    @Override // JsonResourceHandler
    protected JsonValue patch(JsonValue request) throws JsonResourceException {
        JsonValue response = new JsonValue(new LinkedHashMap<String, Object>());
        String id = request.get("id").asString();
        String rev = request.get("rev").asString();
        Patch patch = new JsonPatchWrapper(request.get("value"));
        ObjectSetContext.push(request);
        try {
            patch(id, rev, patch);
            try {
                response.put("_id", id);
                Map<String, Object> object = read(id);
                if (object.containsKey("_rev")) {
                    response.put("_rev", object.get("_rev"));
                }
            } catch (ObjectSetException ose1) {
                LOGGER.warn("Deprecated ObjectSet patch renamed object; missing _rev in response");
            }
        } catch (ObjectSetException ose2) {
            throw convertException(ose2);
        } finally {
            ObjectSetContext.pop();
        }
        return response;
    }

    @Override // ObjectSet
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override // JsonResourceHandler
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        JsonValue response;
        String id = request.get("id").asString();
        Map<String, Object> params = request.get("params").asMap();
        ObjectSetContext.push(request);
        try {
            response = new JsonValue(query(id, params));
        } catch (ObjectSetException ose) {
            throw convertException(ose);
        } finally {
            ObjectSetContext.pop();
        }
        return response;
    }

    @Override // ObjectSet
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override // JsonResourceHandler
    protected JsonValue action(JsonValue request) throws JsonResourceException {
        JsonValue result;
        String id = request.get("id").asString();
        Map<String, Object> params = request.get("params").copy().asMap(); // copy; gonna add _entity to it
        Map<String, Object> value = request.get("value").asMap();
        if (value != null) {
            if (null == params) {
                params = new HashMap<String, Object>(1);
            }
            params.put("_entity", value);
        }
        ObjectSetContext.push(request);
        try {
            result = new JsonValue(action(id, params));
        } catch (ObjectSetException ose) {
            throw convertException(ose);
        } finally {
            ObjectSetContext.pop();
        }
        return new JsonValue(null);
    }
}
