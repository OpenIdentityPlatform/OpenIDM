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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.*;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@Component(name = "org.forgerock.openidm.provisioner.SystemObjectSetService", policy = ConfigurationPolicy.IGNORE, description = "OpenIDM System Object Set Service", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = "ForgeRock AS"),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = "openidm.router.prefix", value = "system") // internal object set router
})
public class SystemObjectSetService implements ObjectSet {
    private final static Logger TRACE = LoggerFactory.getLogger(SystemObjectSetService.class);
    public static final String PROVISIONER_SERVICE_REFERENCE_NAME = "ProvisionerServiceReference";

    @Reference(name = PROVISIONER_SERVICE_REFERENCE_NAME, referenceInterface = ProvisionerService.class, bind = "bind",
            unbind = "unbind", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT)
    private Map<SystemIdentifier, ServiceReference> provisionerServices = new HashMap<SystemIdentifier, ServiceReference>();

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


    protected void bind(ServiceReference provisionerServiceReference) {
        ProvisionerService service = locateService(provisionerServiceReference);
        provisionerServices.put(service.getSystemIdentifier(), provisionerServiceReference);
        TRACE.info("ProvisionerService {} is bound.", provisionerServiceReference.getProperty(ComponentConstants.COMPONENT_ID));
    }

    protected void unbind(ServiceReference provisionerServiceReference) {
        SystemIdentifier id = null;
        for (Map.Entry<SystemIdentifier, ServiceReference> entry : provisionerServices.entrySet()) {
            if (provisionerServiceReference.equals(entry.getValue())) {
                provisionerServices.remove(entry.getKey());
                break;
            }
        }
        TRACE.info("ProvisionerService {} is unbound.", provisionerServiceReference.getProperty(ComponentConstants.COMPONENT_ID));
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
    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        locateService(id).create(id,object);
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
    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        return locateService(id).read(id);
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
    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        locateService(id).update(id,rev,object);
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
    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        locateService(id).delete(id,rev);
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
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        locateService(id).patch(id, rev, patch);
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
    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        return locateService(id).query(id, params);
    }


    private ProvisionerService locateService(String id) throws ObjectSetException {
        try {
            //@TODO use the proper API to get the system name
            URI baseContext = new URI(id);

            for (Map.Entry<SystemIdentifier, ServiceReference> entry : provisionerServices.entrySet()) {
                if (entry.getKey().is(baseContext)) {
                    return locateService(entry.getValue());
                }
            }
        } catch (URISyntaxException e) {
            TRACE.error("Invalid ID: {}", id, e);
        }
        throw new ObjectSetException("System: " + id + " is not available.");
    }

    private ProvisionerService locateService(ServiceReference reference) {
        Object service = context.locateService(PROVISIONER_SERVICE_REFERENCE_NAME, reference);
        return (ProvisionerService) service;
    }
}
