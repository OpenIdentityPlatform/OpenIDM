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
import java.util.*;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.scheduler.ExecutionException;
import org.forgerock.openidm.scheduler.ScheduledService;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SystemObjectSetService implement the {@link ObjectSet}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner", policy = ConfigurationPolicy.IGNORE, description = "OpenIDM System Object Set Service")
@Service(value = {ObjectSet.class, ScheduledService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = "ForgeRock AS"),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = "openidm.router.prefix", value = "system") // internal object set router
})
public class SystemObjectSetService implements ObjectSet, SynchronizationListener, ScheduledService {
    private final static Logger TRACE = LoggerFactory.getLogger(SystemObjectSetService.class);
    public static final String PROVISIONER_SERVICE_REFERENCE_NAME = "ProvisionerServiceReference";
    public static final String SYNCHRONIZATIONLISTENER_SERVICE_REFERENCE_NAME = "SynchronizationListenerServiceReference";

    @Reference(name = PROVISIONER_SERVICE_REFERENCE_NAME, referenceInterface = ProvisionerService.class, bind = "bind",
            unbind = "unbind", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT)
    private Map<SystemIdentifier, ProvisionerService> provisionerServices = new HashMap<SystemIdentifier, ProvisionerService>();

    @Reference(name = SYNCHRONIZATIONLISTENER_SERVICE_REFERENCE_NAME, referenceInterface = SynchronizationListener.class,
            bind = "bindSynchronizationListener", unbind = "unbindSynchronizationListener",
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT)
    private Set<ServiceReference> synchronizationListeners = new HashSet<ServiceReference>(1);

    @Reference(referenceInterface = ObjectSet.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    private ObjectSet router;
    
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


    protected void bind(ProvisionerService service, Map properties) {
        provisionerServices.put(service.getSystemIdentifier(), service);
        TRACE.info("ProvisionerService {} is bound with system identifier {}.",
                properties.get(ComponentConstants.COMPONENT_ID),
                service.getSystemIdentifier());
    }

    protected void unbind(ProvisionerService service, Map properties) {
        SystemIdentifier id = null;
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (service.equals(entry.getValue())) {
                provisionerServices.remove(entry.getKey());
                break;
            }
        }
        TRACE.info("ProvisionerService {} is unbound.", properties.get(ComponentConstants.COMPONENT_ID));
    }

    // Handle SynchronizationListener Service References

    protected void bindSynchronizationListener(ServiceReference service) {
        Object prefix = service.getProperty("openidm.router.prefix");
        if (prefix instanceof String && "system".equalsIgnoreCase((String) prefix)) {
            //Ignore self registration!!!
            return;
        }
        synchronizationListeners.add(service);
        TRACE.info("SynchronizationListener {} is bound.",
                service.getProperty(ComponentConstants.COMPONENT_ID));
    }

    protected void unbindSynchronizationListener(ServiceReference service) {
        synchronizationListeners.remove(service);
        TRACE.info("SynchronizationListener {} is unbound.", service.getProperty(ComponentConstants.COMPONENT_ID));
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
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        // Work-around to ensure id starts with system/
        id = ensureQualified(id);

        locateService(id).create(id, object);
        // Append the system created local identifier
        String newId = id + "/" + (String) object.get("_id");
        ActivityLog.log(getRouter(), Action.CREATE, "", newId, null, object, Status.SUCCESS);
        try {
            onCreate(id, object);
        } catch (SynchronizationException e) {
            //TODO What to do with this exception
            throw new ObjectSetException(e);
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
    public Map<String, Object> read(String id) throws ObjectSetException {
        // Work-around to ensure id starts with system/
        id = ensureQualified(id);
        Map<String, Object> object = locateService(id).read(id);
        ActivityLog.log(getRouter(), Action.READ, "", id, object, null, Status.SUCCESS);
        return object;
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
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        // Work-around to ensure id starts with system/
        id = ensureQualified(id);
        ProvisionerService service = locateService(id);

        Map<String, Object> oldValue = service.read(id);
        service.update(id, rev, object);
        ActivityLog.log(getRouter(), Action.UPDATE, "", id, oldValue, object, Status.SUCCESS);
        
        try {
            onUpdate(id, oldValue, object);
        } catch (SynchronizationException e) {
            //TODO What to do with this exception
            throw new ObjectSetException(e);
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
    public void delete(String id, String rev) throws ObjectSetException {
        // Work-around to ensure id starts with system/
        id = ensureQualified(id);
        ProvisionerService service = locateService(id);
        Map<String, Object> oldValue = service.read(id);
        
        locateService(id).delete(id, rev);
        ActivityLog.log(getRouter(), Action.DELETE, "", id, oldValue, null, Status.SUCCESS);

        try {
            onDelete(id);
        } catch (SynchronizationException e) {
            //TODO What to do with this exception
            throw new ObjectSetException(e);
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
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        // Work-around to ensure id starts with system/
        id = ensureQualified(id);

        ProvisionerService service = locateService(id);

        Map<String, Object> oldValue = service.read(id);
        service.patch(id, rev, patch);
        
        Map<String, Object> newValue = service.read(id);
        ActivityLog.log(getRouter(), Action.UPDATE, "", id, oldValue, newValue, Status.SUCCESS);

        try {
            onUpdate(id, oldValue, newValue);
        } catch (SynchronizationException e) {
            //TODO What to do with this exception
            throw new ObjectSetException(e);
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
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        // Work-around to ensure id starts with system/
        id = ensureQualified(id);

        Map<String, Object> result = locateService(id).query(id, params);        
        ActivityLog.log(getRouter(), Action.QUERY, "Query parameters " + params, id, result, null, Status.SUCCESS);
        
        return result;
    }


    /**
     * Called when a source object has been created.
     *
     * @param id     the fully-qualified identifier of the object that was created.
     * @param object the value of the object that was created.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onCreate(String id, Map<String, Object> object) throws SynchronizationException {
        for (ServiceReference serviceReference : synchronizationListeners) {
            SynchronizationListener service = (SynchronizationListener) context.locateService
                    (SYNCHRONIZATIONLISTENER_SERVICE_REFERENCE_NAME, serviceReference);
            service.onCreate(id, object);
        }
    }

    /**
     * Called when a source object has been updated.
     *
     * @param id       the fully-qualified identifier of the object that was updated.
     * @param newValue the new value of the object after the update.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onUpdate(String id, Map<String, Object> newValue) throws SynchronizationException {
        for (ServiceReference serviceReference : synchronizationListeners) {
            SynchronizationListener service = (SynchronizationListener) context.locateService
                    (SYNCHRONIZATIONLISTENER_SERVICE_REFERENCE_NAME, serviceReference);
            service.onUpdate(id, newValue);
        }
    }

    /**
     * Called when a source object has been updated.
     *
     * @param id       the fully-qualified identifier of the object that was updated.
     * @param oldValue the old value of the object prior to the update.
     * @param newValue the new value of the object after the update.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onUpdate(String id, Map<String, Object> oldValue, Map<String, Object> newValue) throws SynchronizationException {
        for (ServiceReference serviceReference : synchronizationListeners) {
            SynchronizationListener service = (SynchronizationListener) context.locateService
                    (SYNCHRONIZATIONLISTENER_SERVICE_REFERENCE_NAME, serviceReference);
            service.onUpdate(id, oldValue, newValue);
        }
    }

    /**
     * Called when a source object has been deleted.
     *
     * @param id the fully-qualified identifier of the object that was deleted.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onDelete(String id) throws SynchronizationException {
        for (ServiceReference serviceReference : synchronizationListeners) {
            SynchronizationListener service = (SynchronizationListener) context.locateService
                    (SYNCHRONIZATIONLISTENER_SERVICE_REFERENCE_NAME, serviceReference);
            service.onDelete(id);
        }
    }
    
    /**
     * Invoked by the scheduler when the scheduler triggers.
     *
     * @param schedulerContext Context information passed by the scheduler service
     * @throws org.forgerock.openidm.scheduler.ExecutionException
     *          if execution of the scheduled work failed.
     *          Implementations can also throw RuntimeExceptions which will get logged.
     */
    @Override
    public void execute(Map<String, Object> schedulerContext) throws ExecutionException {
        Object action = schedulerContext.get("action");
        Object targetSystem = schedulerContext.get("targetSystem");
        if ("activeSync".equals(action)) {
            if (targetSystem instanceof String) {
                String id = ensureQualified((String) targetSystem);
                try {
                    ProvisionerService service = locateService(id);
                    //TODO: temporary - add proper execute method to the ProvisionerService
                    if (service instanceof ScheduledService) {
                        schedulerContext.put("source", id);
                        ((ScheduledService) service).execute(schedulerContext);
                    }
                } catch (ObjectSetException e) {
                    throw new ExecutionException(e);
                }
            }
        }
    }

    private ObjectSet getRouter() {
        return router;
    }
    
    // TODO: temporary - ensure qualified with system/
    // Remove once router and system object approach in sync
    private String ensureQualified(String id) {
        if (!id.startsWith("system/")) {
            return "system/" + id;
        } else {
            return id;
        }
    }

    private ProvisionerService locateService(String id) throws ObjectSetException {
        try {
            //@TODO use the proper API to get the system name
            URI baseContext = new URI(id);

            for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
                if (entry.getKey().is(baseContext)) {
                    return entry.getValue();
                }
            }
        } catch (URISyntaxException e) {
            TRACE.error("Invalid ID: {}", id, e);
            throw new BadRequestException("Invalid ID in request: + " + id + " " + e.getMessage(), e);
        }
        throw new ObjectSetException("System: " + id + " is not available.");
    }
}
