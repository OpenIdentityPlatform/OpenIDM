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

package org.forgerock.openidm.config.manage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.config.JSONEnhancedConfig;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to OSGi configuration
 *
 * @author aegloff
 */
@Component(
    name = "org.forgerock.openidm.config",
    immediate = true,
    policy = ConfigurationPolicy.OPTIONAL
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM configuration service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "config"),
    @Property(name = "openidm.restlet.path", value = "/config")
})
@Service
public class ConfigObjectService implements ObjectSet {
    
    final static Logger logger = LoggerFactory.getLogger(ConfigObjectService.class);

    @Reference
    ConfigurationAdmin configAdmin; 
    
    private ComponentContext context;

    /**
     * Gets an object from the object set by identifier. 
     * 
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency supported by OpenIDM.
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     * @return the requested object.
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        Map<String, Object> result = null;
        
        try {
            if (fullId == null) {
                // List all configurations
                result = new HashMap<String, Object>();
                Configuration[] rawConfigs = configAdmin.listConfigurations(null);
                List configList = new ArrayList();
                for (Configuration conf : rawConfigs) {
                    Map configEntry = new LinkedHashMap<String, Object>();
                    configEntry.put("_id", conf.getPid());
                    configEntry.put("factoryPid", conf.getFactoryPid());
                    configList.add(configEntry);
                }
                result.put("configurations", configList);
            } else {
                // Get the details of a configuration
                String pidFilter = "(" + Constants.SERVICE_PID + "=" + fullId + ")";
                Configuration[] configs = configAdmin.listConfigurations(pidFilter);
                if (configs == null || configs.length < 1) {
                    throw new NotFoundException("No configuration exists for id " + fullId);
                }
                //Configuration config = configAdmin.getConfiguration(fullId, null);
                Dictionary props = configs[0].getProperties();
                JSONEnhancedConfig enhancedConfig = new JSONEnhancedConfig();
                result = enhancedConfig.getConfiguration(props);
            }
        } catch (ObjectSetException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failure to load configuration for {}", fullId, ex);
            throw new InternalServerErrorException("Failure to load configuration for " + fullId + ": " + ex.getMessage(), ex);
        } 
        return result;
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved. 
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
// TODO: implement
        throw new UnsupportedOperationException();
    }
    
    /**
     * Updates the specified object in the object set. 
     * <p>
     * This implementation requires MVCC and hence enforces that clients state what revision they expect 
     * to be updating
     * 
     * If successful, this method updates metadata properties within the passed object,
     * including: a new {@code _rev} value for the revised object's version
     *
     * @param id the identifier of the object to be put, or {@code null} to request a generated identifier.
     * @param rev the version of the object to update; or {@code null} if not provided.
     * @param object the contents of the object to put in the object set.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException if the passed identifier is invalid
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
// TODO: implement
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param id the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
// TODO: implement
        throw new UnsupportedOperationException();
    }

    /**
     * Currently not supported by this implementation.
     * 
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id the identifier of the object to be patched.
     * @param rev the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws ConflictException if patch could not be applied object state or if version is required.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types. 
     * 
     * The returned map is structured as follow: 
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param id identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO
        return null;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }
    
    @Activate
    protected void activate(ComponentContext context) {
        logger.info("Activating configuration management service");
        this.context = context;
    }

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
    }
}
