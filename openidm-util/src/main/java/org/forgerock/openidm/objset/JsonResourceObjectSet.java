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
import java.util.Map;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// JSON Resource
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.json.resource.JsonResourceException;

// OpenIDM
import org.forgerock.openidm.patch.JsonPatchWrapper;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @deprecated Implement {@code JsonResource} (or extend {@code SimpleJsonResource}) instead.
 */
@Deprecated
public final class JsonResourceObjectSet implements ObjectSet {

    /** TODO: Description. */
    private JsonResource resource;

    /**
     * TODO: Description.
     *
     * @param resource TODO.
     */
    public JsonResourceObjectSet(JsonResource resource) {
        this.resource = resource;
    }

    /**
     * TODO: Description.
     */
    private JsonResourceAccessor getAccessor() {
        JsonValue context = ObjectSetContext.get();
        if (context == null) {
            context = JsonResourceContext.newRootContext();
        }
        return new JsonResourceAccessor(resource, context);
    }

    /**
     * TODO: Description.
     *
     * @param exception TODO.
     * @return TODO.
     */
    private ObjectSetException convertException(JsonResourceException exception) {
        if (exception instanceof ObjectSetException) {
            return (ObjectSetException)exception;
        }
        String message = exception.getMessage();
        switch (exception.getCode()) {
        case 400: return new BadRequestException(message, exception);
        case 403: return new ForbiddenException(message, exception);
        case 404: return new NotFoundException(message, exception);
        case 409: return new ConflictException(message, exception);
        case 412: return new PreconditionFailedException(message, exception);
        case 503: return new ServiceUnavailableException(message, exception);
        default: return new InternalServerErrorException(message, exception);
        }
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        try {
            JsonValue response = getAccessor().create(id, new JsonValue(object));
// FIXME: Commented-out because not all ObjectSet implementations adhere to the specification.
//            object.put("_id", response.get("_id").required().asString());
            JsonValue _id = response.get("_id");
            if (_id.isString()) {
                object.put("_id", response.get("_id").asString());
            }
            JsonValue _rev = response.get("_rev");
            if (_rev.isString()) {
                object.put("_rev", _rev.asString());
            }
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        } catch (JsonValueException jve) {
            throw new InternalServerErrorException(jve);
        }
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        Map<String, Object> object;
        try {
            object = getAccessor().read(id).asMap();
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        } catch (JsonValueException jve) {
            throw new InternalServerErrorException(jve);
        }
        return object;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        try {
            JsonValue response = getAccessor().update(id, rev, new JsonValue(object));
System.out.println("RESPONSE = " + response);
            object.put("_id", response.get("_id").required().asString());
            JsonValue _rev = response.get("_rev");
            if (_rev.isString()) {
                object.put("_rev", _rev.asString());
            }
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        } catch (JsonValueException jve) {
            throw new InternalServerErrorException(jve);
        }
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        try {
            getAccessor().delete(id, rev);
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        }
    }

    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        if (!(patch instanceof JsonPatchWrapper)) {
            throw new BadRequestException();
        }
        try {
            getAccessor().patch(id, rev, ((JsonPatchWrapper)patch).getDiff());
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        }
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        Map<String, Object> result;
        try {
            result = getAccessor().query(id, new JsonValue(params)).asMap();
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        } catch (JsonValueException jve) {
            throw new InternalServerErrorException(jve);
        }
        return result;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        Map<String, Object> result;
        try {
            JsonValue parameters = new JsonValue(params).copy();
            JsonValue value = null;
            if (parameters.isDefined("_entity")) {
                value = parameters.get("_entity");
                parameters.remove("_entity");
            }
            result = getAccessor().action(id, parameters, value).asMap();
        } catch (JsonResourceException jre) {
            throw convertException(jre);
        } catch (JsonValueException jve) {
            throw new InternalServerErrorException(jve);
        }
        return result;
    }
}
