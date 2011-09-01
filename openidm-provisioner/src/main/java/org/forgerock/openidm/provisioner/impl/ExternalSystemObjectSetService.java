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
 * $Id$
 */
package org.forgerock.openidm.provisioner.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.objset.*;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ExternalSystemObjectSetService implement the {@link org.forgerock.openidm.objset.ObjectSet}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner.rest", policy = ConfigurationPolicy.IGNORE, description = "OpenIDM System Object Set REST Service")
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = "ForgeRock AS"),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service")
        //@Property(name = "openidm.router.prefix", value = "external-system"), // internal object set router
})
public class ExternalSystemObjectSetService implements ObjectSet {


    private final static Logger TRACE = LoggerFactory.getLogger(ExternalSystemObjectSetService.class);

    @Reference(referenceInterface = ObjectSet.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    private ObjectSet router;

    @Reference(referenceInterface = ObjectSet.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.provisioner)")
    private ObjectSet provisioner;

    private ComponentContext context = null;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        TRACE.info("Component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
        TRACE.info("Component is deactivated.");
    }

    /**
     * Creates a new object in the object set.
     * <p/>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version if optimistic concurrency
     * is supported.
     *
     * @param id     the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified id could not be resolve.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object or object set is forbidden.
     */
    @Override // ObjectSet
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        if (IdentityServer.isDevelopmentProfileEnabled()) {
            getProvisioner().create(id, object);
        } else {
            throw new ForbiddenException("Operation is only allowed in debug profile");
        }
    }

    /**
     * Reads an object from the object set.
     * <p/>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} if optimistic concurrency is supported. If optimistic
     * concurrency is not supported, then {@code _rev} must be absent or {@code null}.
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @return the requested object.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     */
    @Override // ObjectSet
    public Map<String, Object> read(String id) throws ObjectSetException {
        if (IdentityServer.isDevelopmentProfileEnabled()) {
            return getProvisioner().read(id);
        }
        throw new ForbiddenException("Operation is only allowed in debug profile");
    }

    /**
     * Updates an existing specified object in the object set.
     * <p/>
     * This method updates the {@code _rev} property to the revised object version on update
     * if optimistic concurrency is supported.
     *
     * @param id     the identifier of the object to be updated.
     * @param rev    the version of the object to update; or {@code null} if not provided.
     * @param object the contents of the object to updated in the object set.
     * @throws org.forgerock.openidm.objset.ConflictException
     *          if version is required but is {@code null}.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.PreconditionFailedException
     *          if version did not match the existing object in the set.
     */
    @Override // ObjectSet
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        if (IdentityServer.isDevelopmentProfileEnabled()) {
            getProvisioner().update(id, rev, object);
        } else {
            throw new ForbiddenException("Operation is only allowed in debug profile");
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param id  the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     * @throws org.forgerock.openidm.objset.ConflictException
     *          if version is required but is {@code null}.
     * @throws org.forgerock.openidm.objset.PreconditionFailedException
     *          if version did not match the existing object in the set.
     */
    @Override // ObjectSet
    public void delete(String id, String rev) throws ObjectSetException {
        if (IdentityServer.isDevelopmentProfileEnabled()) {
            getProvisioner().delete(id, rev);
        } else {
            throw new ForbiddenException("Operation is only allowed in debug profile");
        }
    }

    /**
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id    the identifier of the object to be patched.
     * @param rev   the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws org.forgerock.openidm.objset.ConflictException
     *          if patch could not be applied object state or if version is required.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.PreconditionFailedException
     *          if version did not match the existing object in the set.
     */
    @Override // ObjectSet
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        if (IdentityServer.isDevelopmentProfileEnabled()) {
            getProvisioner().patch(id, rev, patch);
        } else {
            throw new ForbiddenException("Operation is only allowed in debug profile");
        }
    }

    /**
     * Performs a query on the specified object and returns the associated results.
     * <p/>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     *
     * @param id     identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results object.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object or specified query is forbidden.
     */
    @Override // ObjectSet
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        if (IdentityServer.isDevelopmentProfileEnabled()) {
            return getProvisioner().query(id, params);
        }
        throw new ForbiddenException("Operation is only allowed in debug profile");
    }

    @Override // ObjectSet
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        JsonNode action = new JsonNode(params.get("_action"));
        if (action.isNull() || !action.isString()) {
            throw new BadRequestException("Missing required _action parameter");
        }
        TRACE.debug("action id={} _action", id, action.asString());
        Map<String, Object> result = null;
        //Throws org.forgerock.json.fluent.JsonNodeException if the action is not supported.
        switch (action.asEnum(SystemAction.class)) {
            case EXECUTESCRIPT:
                validateActionRequest(SystemAction.EXECUTESCRIPT, id, params);
                Object _scriptid = params.get("_script-id");
                if (_scriptid instanceof String) {
                    //TODO Use the _scriptid to retrieve the storedObject
                    Map<String, Object> storedScript = new HashMap<String, Object>();

                    for (Map.Entry<String, Object> e : params.entrySet()) {
                        if (storedScript.containsKey(e.getKey())) {
                            continue;
                        }
                        storedScript.put(e.getKey(), e.getValue());
                    }
                    result = getProvisioner().action(id, storedScript);
                }
                break;
            case DEBUGEXECUTESCRIPT:
                if (IdentityServer.isDevelopmentProfileEnabled()) {
                    Object _entity = params.get("_entity");
                    if (_entity instanceof Map) {
                        Map<String, Object> script = (Map<String, Object>) _entity;
                        validateActionRequest(SystemAction.DEBUGEXECUTESCRIPT, id, script);
                        result = getProvisioner().action(id, script);
                    }
                } else {
                    throw new ForbiddenException("Action is only allowed in debug mode");
                }
                break;
            case GETCONFIGURATION:
                //TODO resolve circular reference for ConnectorInfoProviderService via admin interface
                break;
        }
        ActivityLog.log(getRouter(), Action.ACTION, "Action parameters " + params, id, result, null, Status.SUCCESS);
        return result;
    }

    private ObjectSet getRouter() {
        return router;
    }

    private ObjectSet getProvisioner() {
        return provisioner;
    }

    private void validateActionRequest(SystemAction action, String id, Map<String, Object> params) throws ObjectSetException {
        if ((null == id) == action.isRequireId()) {
            throw new BadRequestException("Missing required id");
        }
        if (!params.keySet().containsAll(action.getRequiredAttributes())) {
            throw new BadRequestException("Missing required attribute(s) " + action.getRequiredAttributes().toString());
        }
    }


    private enum SystemAction {
        EXECUTESCRIPT(new String[]{"_script-id"}, true),
        DEBUGEXECUTESCRIPT(new String[]{"_script-type", "_script-expression"}, true),
        GETCONFIGURATION(null, true);

        private Set<String> requiredAttributes;
        private boolean requireId = true;

        private SystemAction(String[] requiredAttributes, boolean requireId) {
            this.requiredAttributes = null != requiredAttributes ?
                    Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(requiredAttributes))) :
                    Collections.<String>emptySet();
            this.requireId = requireId;
        }

        public Set<String> getRequiredAttributes() {
            return requiredAttributes;
        }

        public boolean isRequireId() {
            return requireId;
        }
    }
}
