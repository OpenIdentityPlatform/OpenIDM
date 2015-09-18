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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.Map;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and caches the object only once on demand.
 * This class is not thread safe.
 *
 */
public class LazyObjectAccessor {
    private static final Logger logger = LoggerFactory.getLogger(ObjectMapping.class);

    public static final Name EVENT_READ_OBJ = Name.get("openidm/internal/discovery-engine/sync/read-object");

    private SynchronizationService service;
    private JsonValue object = null;       // The object once loaded, or null if not found
    private boolean loaded = false;        // Whether it considers its state as loaded/initialized
    private final String componentContext; // The qualifier for the id
    private final String localId;          // The local part of the id

    /**
     * Construct with a known value of the object. The object is considered loaded.
     * @param service the sync service
     * @param componentContext the component qualifier of the object id
     * @param localId the unqualified part of the object id
     * @param value the object value
     */
    public LazyObjectAccessor(SynchronizationService service, String componentContext,
                              String localId, JsonValue value) {
        this.service = service;
        this.object = value;
        this.componentContext = componentContext;
        this.localId = localId;
        this.loaded = true;
    }

    /**
     * Construct with just the identifier of the object. The object is not yet considered loaded.
     * @param service the sync service
     * @param componentContext the component qualifier of the object id
     * @param localId the unqualified part of the object id
     */
    public LazyObjectAccessor(SynchronizationService service, String componentContext,
                              String localId) {
        this.service = service;
        this.componentContext = componentContext;
        this.localId = localId;
    }

    /**
     * @return the object value, null if the object does not exist.
     * This may trigger a load on demand
     * if it has not been loaded already.
     *
     * @throws SynchronizationException if loading the object failed
     * for reasons other than just not finding the object.
     */
    public JsonValue getObject() throws SynchronizationException {
        if (!loaded) {
            try {
                // If not found, the object will be null
                object = rawReadObject(service.getContext(), service.getConnectionFactory(), componentContext, localId);
            } catch (SynchronizationException ex) {
                throw ex; // being explicit that this would not be considered loaded
            }
            loaded = true;
        }
        return object;
    }

    /**
     * @return whether the object state has been loaded/initialized.
     * This also is true if an object is not found, and the object value hence is null.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * @return The object representation as a map
     * @throws SynchronizationException if loading the object on demand failed
     */
    public Map<String, Object> asMap() throws SynchronizationException {
        return null != getObject() ? getObject().asMap() : null;
    }

    /**
     * @return The unqualified part of the object id
     */
    public String getLocalId() {
        return localId;
    }

    /**
     * Tries to retrieve the object directly from where it is stored.
     * Does not take into account any cached/lazy loaded object in this class.
     *
     * Typically access to objects should be through {@code getObject()} instead,
     * which can support the lazy/cached loading.
     *
     * @param resourceContainer location where the object is stored
     * @param resourceId the object identifier
     * @throws NullPointerException if {@code targetId} is {@code null}.
     * @throws SynchronizationException if retrieving the object failed
     * @return the object value if found, null if not found
     */
    public static JsonValue rawReadObject(Context router, ConnectionFactory connectionFactory, String resourceContainer, String resourceId) throws SynchronizationException {
        if (resourceId == null) {
            throw new NullPointerException("Identifier passed to readObject is null");
        }
        EventEntry measure = Publisher.start(EVENT_READ_OBJ, null, resourceId);
        try {
            ReadRequest r = Requests.newReadRequest(resourceContainer, resourceId);
            JsonValue result = connectionFactory.getConnection().read(router,r).getContent();
            measure.setResult(result);
            return result;
        } catch (NotFoundException nfe) { // target not found results in null
            return null;
        } catch (ResourceException ose) {
            logger.warn("Failed to read target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            measure.end();
        }
    }

    /**
     * @param componentContext the resource component name the localId is relative to
     * @param localId local identifier within the component context
     * @return The qualified identifier, qualified with the component context
     */
    public static String qualifiedId(String componentContext, String localId) {
        StringBuilder sb = new StringBuilder();
        sb.append(componentContext);
        if (componentContext != null) {
            sb.append('/').append(localId);
        }
        return sb.toString();
    }
}
