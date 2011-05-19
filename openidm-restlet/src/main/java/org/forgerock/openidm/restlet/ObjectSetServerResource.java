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

package org.forgerock.openidm.restlet;

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Restlet Framework
import org.restlet.data.Conditions;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.ext.jackson.JacksonRepresentation;

// ForgeRock OpenIDM
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.MethodNotAllowedException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.ServiceUnavailableException;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
public class ObjectSetServerResource extends ExtendedServerResource {

    /** Identifier of resource being accessed, or {@code null} to represent the object set itself. */
    private String id;

    /** Object set this resource is operating on. */
    private ObjectSet objectSet;

    /** TODO: Description. */
    private Conditions conditions;

    /**
     * TODO: Description.
     *
     * @param map TODO.
     * @return TODO.
     */
    private JacksonRepresentation jacksonRepresentation(Map<String, Object> map) {
        JacksonRepresentation representation = new JacksonRepresentation(map);
        representation.setObjectClass(Object.class); // probably superfluous
        return representation;
    }

    /**
     * Throws a {@link PreconditionFailedException} if there are any unsupported preconditions
     * specified in the request.
     */
    private void enforceSupportedConditions() throws PreconditionFailedException {
        if (conditions.getModifiedSince() != null) {
            throw new PreconditionFailedException("If-Modified-Since is not supported");
        }
        if (conditions.getUnmodifiedSince() != null) {
            throw new PreconditionFailedException("If-Unmodified-Since is not supported");
        }
    }

    /**
     * Returns the revision that should be provided in the object set method. The revision
     * is derived from the request. This may result in the retrieval of the object in order
     * to apply the precondition(s).
     */
    private String useRev() throws ObjectSetException {
        enforceSupportedConditions();
        String result = null; // default: no revision
        if (conditions.getMatch().size() > 1 || conditions.getNoneMatch().size() > 0) {
            result = getTag(readObject()).getName(); // derive from existing object
        } else if (conditions.getMatch().size() == 1) {
            result = conditions.getMatch().get(0).getName(); // derive directly from request
        }
        return result;
    }

    /**
     * TODO: Description.
     *
     * @returns TODO.
     * @throws ObjectSetException TODO.
     */
    private Map<String, Object> readObject() throws ResourceException {
        try {
            Map<String, Object> object = objectSet.read(id);
            Status status = conditions.getStatus(getMethod(), true, getTag(object), null);
            if (status != null && status.isError()) { // precondition failed
                throw new ResourceException(status);
            }
            return object;
        } catch (ObjectSetException ose) {
            throw new ResourceException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param object TODO.
     * @return TODO.
     */
    private Tag getTag(Map<String, Object> map) {
        Object rev = (map != null ? map.get("_rev") : null);
        return (rev != null && rev instanceof String ? new Tag((String)rev, false) : null);
    }

    /**
     * TODO: Description.
     *
     * @param entity TODO.
     * @return TODO.
     * @throws BadRequestException TODO.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> entityObject(Representation entity) throws BadRequestException {
// TODO: Will this ever already be a JacksonRepresentation?
        JacksonRepresentation jr = (entity instanceof JacksonRepresentation ?
         (JacksonRepresentation)entity : new JacksonRepresentation<Object>(entity, Object.class));
        Object object = jr.getObject();
        if (object == null || !(object instanceof Map)) {
            throw new BadRequestException("Failed to parse JSON entity");
        }
        return (Map)object;
    }

    /**
     * TODO: Description.
     *
     * @param entity TODO.
     * @return TODO.
     * @throws ObjectSetException TODO.
     */
    private Representation create(Map<String, Object> object) throws ObjectSetException {
        objectSet.create(id, object);
        setStatus(Status.SUCCESS_CREATED);
        return cuResponse(object);
    }

    /**
     * TODO: Description.
     *
     * @param entity TODO.
     * @return TODO.
     * @throws ObjectSetException TODO.
     */
    private Representation update(Map<String, Object> object) throws ObjectSetException {
        objectSet.update(id, useRev(), object);
        setStatus(Status.SUCCESS_OK);
        return cuResponse(object);
    }

    /**
     * TODO: Description.
     *
     * @param object TODO.
     * @return TODO.
     * @throws InternalServerErrorException TODO.
     */
    private Representation cuResponse(Map<String, Object> object) throws InternalServerErrorException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Object id = object.get("_id");
        if (id != null && !(id instanceof String)) {
            throw new InternalServerErrorException("Object did not return a valid _id property");
        }
// TODO: Should a successful update to the _id property result in a redirect to the new resource?
        map.put("_id", (id != null ? (String)id : this.id));
        Object rev = object.get("_rev");
        if (rev != null && !(rev instanceof String)) {
            throw new InternalServerErrorException("Object returned invalid _rev property");
        }
        if (rev != null) {
            map.put("_rev", (String)rev);
        }
        Representation result = jacksonRepresentation(map);
        if (rev != null) {
            result.setTag(new Tag((String)rev, false)); // set ETag
        }
        return result;
    }

    /**
     * Overrides the response to provide a JSON error structure in the entity if a
     * {@link ResourceException} is being thrown.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (throwable instanceof ResourceException && cause instanceof ObjectSetException) {
            Status status;
            if (cause instanceof NotFoundException) {
                status = Status.CLIENT_ERROR_NOT_FOUND;
            } else if (cause instanceof PreconditionFailedException) {
                status = Status.CLIENT_ERROR_PRECONDITION_FAILED;
            } else if (cause instanceof BadRequestException) {
                status = Status.CLIENT_ERROR_BAD_REQUEST;
            } else if (cause instanceof ForbiddenException) {
                status = Status.CLIENT_ERROR_FORBIDDEN;
            } else if (cause instanceof ConflictException) {
                status = Status.CLIENT_ERROR_CONFLICT;
            } else if (cause instanceof MethodNotAllowedException) {
// FIXME: RFC 2616 §10.4.6: "The response MUST include an Allow header containing a list of valid methods for the requested resource."
                status = Status.CLIENT_ERROR_METHOD_NOT_ALLOWED;
            } else if (cause instanceof InternalServerErrorException) {
                status = Status.SERVER_ERROR_INTERNAL;
            } else if (cause instanceof ServiceUnavailableException) {
                status = Status.SERVER_ERROR_SERVICE_UNAVAILABLE;
            } else { // default
                status = Status.SERVER_ERROR_INTERNAL;
            }
            String description = (cause != null ? cause.getMessage() : null);
            throwable = new ResourceException(status, description, cause);
        }
        if (throwable instanceof ResourceException) {
            ResourceException re = (ResourceException)throwable;
            Map<String, Object> entity = new HashMap<String, Object>();
            Status status = re.getStatus();
// TODO: Design more useful error output format? Perhaps standardize in its own class?
            entity.put("error", status != null ? status.getName() : "Unclassified");
            entity.put("description", status != null ? status.getDescription() : re.getMessage());
            setStatus(status);
            getResponse().setEntity(jacksonRepresentation(entity));
        }
    }

    @Override
    public void doInit() {
        setAnnotated(false); // using method names, not annotations
        setNegotiated(false); // we speak all-JSON for the time being
        setConditional(false); // need to handle conditional requests in implementation
        objectSet = (ObjectSet)getRequestAttributes().get(ObjectSet.class.getName());
        String rp = getReference().getRemainingPart(false, false);
        if (rp != null && rp.length() > 0) {
            id = rp; // default: null (object set itself is being operated on)
        }
        conditions = getConditions();
    }

    @Override
    public Representation get() throws ResourceException {
        try {
            enforceSupportedConditions();
            Representation result = null;
            Form query = getQuery();
            if (query == null || query.size() == 0) { // read
                Map<String, Object> object = readObject();
                result = jacksonRepresentation(object);
                result.setTag(getTag(object)); // set ETag
            } else { // query
                if (conditions.hasSome()) { // queries with conditions are invalid
                    throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED);
                }
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.putAll(query.getValuesMap()); // copy values to cope with generics
                result = jacksonRepresentation(objectSet.query(id, map));
            }
            return result;
        } catch (ObjectSetException ose) {
            throw new ResourceException(ose);
        }
    }

    @Override
    public Representation put(Representation entity) throws ResourceException {
        try {
            enforceSupportedConditions();
            List<Tag> match = conditions.getMatch();
            List<Tag> noneMatch = conditions.getNoneMatch();
            Map<String, Object> object = entityObject(entity);
            if (match.size() == 0 && noneMatch.size() == 1 && noneMatch.get(0).equals(Tag.ALL)) { // unambiguous create
                return create(object);
            } else if (noneMatch.size() == 0 && match.size() == 1 && !match.get(0).equals(Tag.ALL)) { // unambiguous update
                return update(object);
            } else { // ambiguous whether object is being created or updated
                try {
                    return update(object); // attempt to update first
                } catch (NotFoundException nfe) {
                    return create(object); // fallback to create if no object to update
                }
            }
        } catch (ObjectSetException ose) {
            throw new ResourceException(ose);
        }
    }

    @Override 
    public Representation post(Representation entity) throws ResourceException {
        Form query = getQuery();
        String action = query.getFirstValue("action");
        if (action == null) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Expecting query parameter: action");
        }
        if (!action.equals("create")) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unknown action: "  + action);
        }
        try {
            Map<String, Object> object = entityObject(entity);
            return create(object);
        } catch (ObjectSetException ose) {
            throw new ResourceException(ose);
        }
    }

    @Override
    public Representation delete() throws ResourceException {
        try {
            objectSet.delete(id, useRev());
            setStatus(Status.SUCCESS_NO_CONTENT);
            return null; // no content
        } catch (ObjectSetException ose) {
            throw new ResourceException(ose);
        }
    }
}
