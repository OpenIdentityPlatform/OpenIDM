/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.config.persistence;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.felix.cm.PersistenceManager;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.config.installer.JSONPrettyPrint;
import org.forgerock.openidm.repo.RepoBootService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class RepoPersistenceManager implements PersistenceManager, ConfigPersisterMarker {

    // The ID URI prefix for configuration
    private static final String CONFIG_CONTEXT_PREFIX = "config/";

    // Meta-data when converting from array to list to enable later conversion to original form
    private static final String OPENIDM_ORIG_ARRAY = "_openidm_orig_array";
    private static final String OPENIDM_ORIG_ARRAY_TYPE = "_openidm_orig_array_type=";

    private static final String BUNDLE_LOCATION = "service__bundleLocation";
    private static final String FELIX_FILEINSTALL_FILENAME = "felix__fileinstall__filename";

    final static Logger logger = LoggerFactory.getLogger(RepoPersistenceManager.class);

    private static JSONPrettyPrint prettyPrint = new JSONPrettyPrint();
    private static ObjectMapper mapper = new ObjectMapper();

    private BundleContext ctx;

    /**
     * The Repository Service Accessor
     */
    private RepoBootService repo = null;


    //Rapid development may require only memory store.
    private final boolean requireRepository = Boolean.valueOf(System.getProperty("openidm.config.repo.enabled", "true"));

    // Fall-back is in-memory store of configurations
    @SuppressWarnings("rawtypes")
    Map<String, Dictionary> tempStore = new HashMap<>();

    public RepoPersistenceManager(final BundleContext ctx) {
        this.ctx = ctx;
        logger.debug("Bootstrapping Repository Persistence Manager");
    }

    /**
     * Handle the system notifying that it's ready to install configs.
     */
    public void checkReady() throws BootstrapFailure {
        if (requireRepository) {
            ServiceTracker<?, ?> repoTracker = null;
            try {
                if (repo == null) {
                    Filter filter = ctx.createFilter("(" + Constants.OBJECTCLASS + "=" + RepoBootService.class.getName() + ")");
                    repoTracker = new ServiceTracker<>(ctx, filter, null);
                    repoTracker.open();
                    logger.debug("Bootstrapping repository");
                    RepoBootService rawRepo = (RepoBootService) repoTracker.waitForService(5000);
                    if (rawRepo != null) {
                        logger.debug("Bootstrap obtained repository");   
                        repo = rawRepo;
                    } else {
                        logger.info("Failed to bootstrap repo, returned null");
                    }
                }
            } catch (InterruptedException ex) {
                logger.warn("Failed to bootstrap repo " + ex.getMessage(), ex);
            } catch (InvalidSyntaxException ex) {
                logger.warn("Failed to bootstrap repo " + ex.getMessage(), ex);
            } finally {
                if (repoTracker != null) {
                    repoTracker.close();
                }
            }                
            if (repo == null) {
                throw new BootstrapFailure("Failed to acquire the bootstrap repository service to access configuration persistence.");
            }
        }
    }

    private boolean isReady(int retries) {
        try {
            checkReady();
        } catch (BootstrapFailure e) {
            if (retries > 0) isReady(--retries);
        }
        return requireRepository ? repo != null : true;
    }

    /**
     * Returns <code>true</code> if a persisted <code>Dictionary</code> exists
     * for the given <code>pid</code>. 
     * 
     * @param pid The identifier for the dictionary to test.
     */
    public boolean exists(String pid) {
        logger.debug("Config exists call for {}", pid);

        boolean exists = false;

        if (isReady(0) && requireRepository) {
            String id = pidToId(pid);
            try {
                ReadRequest readRequest = Requests.newReadRequest(id);
                ResourceResponse existing = repo.read(readRequest);
                exists = (existing != null);
            } catch (NotFoundException ex) {
                exists = false;
            } catch (ResourceException ex) {
                throw new RuntimeException("Failed to check if configuration exists in repository: " + ex.getMessage(), ex);
            }
        } 
        if (!exists) {
            exists = tempStore.containsKey(pid);
            if (exists) {
                logger.debug("Entry exists in temporary store for '{}'", pid);
            }
        } else {
            logger.debug("Entry exists for '{}'", pid);
        }

        if (!exists) {
            logger.debug("Entry does not exist for '{}'", pid);
        }
        return exists;
    }

    /**
     * Returns the <code>Dictionary</code> for the given <code>pid</code>.
     * 
     * @param pid The identifier for the dictionary to load.
     * 
     * @return The dictionary for the identifier. This must not be
     *      <code>null</code> but may be empty.
     *      
     * @throws IOException If an error occurrs loading the dictionary. An
     *      <code>IOException</code> must also be thrown if no dictionary
     *      exists for the given identifier. 
     */
    @SuppressWarnings("rawtypes")
    public Dictionary load(String pid) throws IOException {
        logger.debug("Config load call for {}", pid);
        Dictionary result = null;

        try {
            if (isReady(0) && requireRepository) {
                String id = pidToId(pid);
                ReadRequest readRequest = Requests.newReadRequest(id);
                ResourceResponse existing = repo.read(readRequest);
                Map<String, Object> existingConfig = existing.getContent().asMap();
                Object configMap = existingConfig.get(JSONEnhancedConfig.JSON_CONFIG_PROPERTY);
                if (configMap != null) {
                    ((Map)configMap).remove(ResourceResponse.FIELD_CONTENT_ID);
                }
                String configString = serializeConfig(configMap);
                existingConfig.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, configString);
                logger.debug("Config loaded {} {}", pid, existing);
                result = mapToDict(existingConfig);
            } else if (!requireRepository) {
                result = tempStore.get(pid);
                if (result == null) {
                    throw new IOException("No entry for " + pid + " exists.");
                }

                logger.debug("Config loaded from temporary store {} {}", pid, result);
            }
        } catch (NotFoundException ex) {
            result = tempStore.get(pid);
            if (result == null) {
                throw new IOException("No entry for " + pid + " exists.");
            }

            logger.debug("Config loaded from temporary store {} {}", pid, result);
        } catch (ResourceException ex) {
            throw new IOException("Failed to load configuration in repository: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * Returns an enumeration of all <code>Dictionary</code> objects known to 
     * this persistence manager.Dictionary
     * <p>
     * Implementations of this method are allowed to return lazy enumerations.
     * That is, it is allowable for the enumeration to not return a dictionary
     * if loading it results in an error.
     * 
     * @return A possibly empty Enumeration of all dictionaries.
     * 
     * @throws IOException If an error occurrs getting the dictionaries.
     */
    @SuppressWarnings("rawtypes")
    public Enumeration getDictionaries() throws IOException {
        if (isReady(5)) {
            logger.debug("Config getDictionaries call from repository");
        } else {
            logger.debug("Config getDictionaries call from temporary store");
        }
        return new Enumeration() {
            java.util.Iterator memIter = tempStore.values().iterator();
            java.util.Iterator dbIter = null;
            List<String[]> returnedIds = new ArrayList<String[]>();

            @Override
            public boolean hasMoreElements() {
                boolean hasMore = false;
                try {
                    hasMore = memIter.hasNext();

                    if (!hasMore) {
                        if (requireRepository && repo != null && dbIter == null) {
                            QueryRequest r = Requests.newQueryRequest("/config");
                            r.setQueryId("query-all-ids");
                            logger.debug("Attempt query query-all-ids");
                            final List<Map<String, Object>> queryResult = new ArrayList<Map<String, Object>>();
                            List<ResourceResponse> results = repo.query(r);
                            for (ResourceResponse resource : results) {
                                queryResult.add(resource.getContent().asMap());
                            }
                            dbIter = queryResult.iterator();
                        }
                        if (dbIter != null) {
                            hasMore = dbIter.hasNext();
                        }
                    }
                } catch (RuntimeException ex) {
                    logger.warn("Failure getting configuration dictionaries for hasMoreElements " + ex.getMessage(), ex);
                    throw ex;
                } catch (Exception ex) {
                    logger.warn("Failure getting configuration dictionaries for hasMoreElements " + ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }

                return hasMore;
            }

            public Object nextElement() {
                try {
                    if (memIter.hasNext()) {
                        return memIter.next();
                    } else {
                        Map entry = (Map) dbIter.next();
                        String entryId = (String) entry.get("_id");
                        return load(entryId);
                    }
                } catch (RuntimeException ex) {
                    logger.warn("Failure getting configuration dictionaries for nextElement " + ex.getMessage(), ex);
                    throw ex;
                } catch (Exception ex) {
                    logger.warn("Failure getting configuration dictionaries for nextElement " + ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    /**
     * Stores the <code>Dictionary</code> under the given <code>pid</code>.
     * 
     * @param pid The identifier of the dictionary.
     * @param properties The <code>Dictionary</code> to store.
     * 
     * @throws IOException If an error occurrs storing the dictionary. If this
     *      exception is thrown, it is expected, that
     *      {@link #exists(String) exists(pid} returns <code>false</code>.
     */
    @SuppressWarnings("rawtypes")
    public void store(String pid, Dictionary properties) throws IOException {
        logger.debug("Store call for {} {}", pid, properties);

        // Store config handling settings in memory
        if (pid.startsWith("org.apache.felix.fileinstall")) {
            tempStore.put(pid, properties);
            return;
        }

        try {
            if (isReady(0) && requireRepository) {
                String id = pidToId(pid);

                Map<String,Object> obj = dictToMap(properties);
                JsonValue content = new JsonValue(obj);
                String configResourceId = ConfigBootstrapHelper.getId(content.get(ConfigBootstrapHelper.CONFIG_ALIAS).asString(), 
                        content.get(ConfigBootstrapHelper.SERVICE_PID).asString(), 
                        content.get(ConfigBootstrapHelper.SERVICE_FACTORY_PID).asString());
                String configString = (String)obj.get(JSONEnhancedConfig.JSON_CONFIG_PROPERTY);
                Map<Object, Object> configMap = deserializeConfig(configString);
                if (configMap != null) {
                    configMap.put("_id", configResourceId);
                }
                obj.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, configMap);

                Map<String,Object> existing = null;
                try {
                    ReadRequest readRequest = Requests.newReadRequest(id);
                    existing = repo.read(readRequest).getContent().asMap();
                } catch (NotFoundException ex) {
                    // Just detect that it doesn't exist
                }
                if (existing != null) {
                    String rev = (String) existing.get("_rev");

                    existing.remove("_rev");
                    existing.remove("_id");
                    obj.remove("_rev"); // beware, this means _id and _rev should not be in config file
                    obj.remove("_id"); // beware, this means _id and _rev should not be in config file
                    obj.remove(RepoPersistenceManager.BUNDLE_LOCATION);
                    obj.remove(RepoPersistenceManager.FELIX_FILEINSTALL_FILENAME);
                    if(!existing.equals(obj)) {
                        logger.trace("Not matching {} {}", existing, obj);
                        boolean retry;
                        do {
                            retry = false;
                            try {
                                UpdateRequest r = Requests.newUpdateRequest(id, new JsonValue(obj));
                                r.setRevision(rev);
                                repo.update(r);
                            } catch (PreconditionFailedException ex) {
                                logger.debug("Concurrent change during update, retrying {} {}", pid, rev);
                                ReadRequest readRequest = Requests.newReadRequest(id);
                                existing = repo.read(readRequest).getContent().asMap();
                                rev = (String) existing.get("_rev");
                                retry = true;
                            }
                        } while (retry);
                        logger.debug("Updated existing config {} {} {}", new Object[] {pid, rev, obj});
                    } else {
                        logger.debug("Existing config same as store request, ignoring {} {} {}", new Object[] {pid, rev, obj});
                    }
                } else {
                    logger.trace("Creating: {} {} ", id, obj);
                    // This may create a new (empty) configuration, which felix marks with _felix___cm__newConfiguration=true
                    String newResourceId = id.substring(CONFIG_CONTEXT_PREFIX.length());
                    CreateRequest createRequest = Requests.newCreateRequest(CONFIG_CONTEXT_PREFIX, new JsonValue(obj));
                    createRequest.setNewResourceId(newResourceId);
                    obj = repo.create(createRequest).getContent().asMap();
                    logger.debug("Stored new config in repository {} {}", pid, obj);
                }
            } else {
                tempStore.put(pid, properties);
                logger.debug("Stored in memory {} {}", pid, properties);
            }
        } catch (ResourceException ex) {
            throw new IOException("Failed to store configuration in repository: " + ex.getMessage(), ex);
        }
    }

    /**
     * Removes the <code>Dictionary</code> for the given <code>pid</code>. If
     * such a dictionary does not exist, this method has no effect.
     * 
     * @param pid The identifier of the dictionary to delet.
     * 
     * @throws IOException If an error occurrs deleting the dictionary. This
     *      exception must not be thrown if no dictionary with the given
     *      identifier exists.
     */
    public void delete(String pid) throws IOException {
        logger.debug("delete call for {}", pid);
        Object removed = tempStore.remove(pid);
        if (removed != null) {
            logger.debug("Deleted {} from temporary store", pid);
        }
        try {
            if (isReady(0) && requireRepository) {
                String id = pidToId(pid);
                boolean retry;
                String rev = null;
                do {
                    retry = false;
                    try {

                        ReadRequest readRequest = Requests.newReadRequest(id);
                        Map<String, Object> existing = repo.read(readRequest).getContent().asMap();
                        if (existing != null) {
                            rev = (String) existing.get("_rev");
                            DeleteRequest r = Requests.newDeleteRequest(id);
                            r.setRevision(rev);
                            repo.delete(r);
                            logger.debug("Deleted {}", pid);
                        }
                    } catch (PreconditionFailedException ex) {
                        logger.debug("Concurrent change during delete, retrying {} {}", pid, rev);
                        retry = true;
                    } catch (NotFoundException ex) {
                        // If it doesn't exists (anymore) that's fine
                    }
                } while (retry);

            }
        } catch (ResourceException ex) {
            throw new IOException("Failed to delete configuration + " + pid + " in repository: " + ex.getMessage(), ex);
        }
    }

    /**
     * Convert OSGi pid to an ID suitable for addressing the repository
     * @param pid the OSGi pid
     * @return the qualified objectset id
     */
    String pidToId(String pid) {
        if (pid.indexOf("|") > -1) {
            return CONFIG_CONTEXT_PREFIX + pid.substring(0, pid.indexOf("|"));
        }
        return CONFIG_CONTEXT_PREFIX + pid;
    }

    /**
     * Convert dictionary and contents to a Map, compatible with the JSON model.
     * Arrays are encoded with special meta-data entries marking their origin as array.
     * 
     * @param dict the (OSGi configuration) dictionary
     * @return the converted map
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Map<String,Object> dictToMap(Dictionary dict) {
        Map<String,Object> convert = new HashMap<String,Object>();
        Enumeration keysEnum = dict.keys();
        while(keysEnum.hasMoreElements()) {
            String key = (String) keysEnum.nextElement();
            Object rawValue = dict.get(key);
            // Deal with outdated collection classes
            Object putValue = rawValue;
            if (rawValue instanceof Dictionary) {
                putValue = dictToMap((Dictionary) rawValue); // Beware, does not support recursive dictionaries
            } else if (rawValue instanceof Vector) {
                putValue = new ArrayList((Vector) rawValue);
            } else if (rawValue instanceof Object[]) {
                List convList = new ArrayList(Arrays.asList((Object[]) rawValue));

                // Add marker entries to mark this as coming from an array
                convList.add(OPENIDM_ORIG_ARRAY); 
                convList.add(OPENIDM_ORIG_ARRAY_TYPE + rawValue.getClass().getComponentType().getName());

                putValue = convList;
            }
            convert.put(toEscapedKey(key), putValue);
        }
        return convert;
    }

    /**
     * Convert the JSON model map and contents to types and contents 
     * compatible with OSGi configuration Dictionary
     * e.g. using legacy types (Hashtables, Vectors), 
     * Encoded property names decoded.
     * 
     * @param properties map to convert
     * @return converted to legacy types
     * @throws IOException if the conversion failed 
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Dictionary mapToDict(Map properties) throws IOException {
        if (properties instanceof Dictionary) {
            return (Dictionary) properties;
        } else {
            Hashtable converted = new Hashtable();
            for(Object entry : properties.keySet()) {
                Object key = entry;
                Object value = properties.get(entry);
                if (entry instanceof String) {
                    key = fromEscapedKey((String) entry);
                }
                if (value instanceof List) {
                    List listToInspect = (List) value;
                    if (listToInspect.contains(OPENIDM_ORIG_ARRAY)) {
                        listToInspect.remove(OPENIDM_ORIG_ARRAY); // remove this marker entry
                        String typeListItem = null;
                        for (Object listItem : listToInspect) {
                            if (listItem instanceof String && ((String)listItem).startsWith(OPENIDM_ORIG_ARRAY_TYPE)) {
                                typeListItem = (String) listItem;
                            }
                        }
                        if (typeListItem == null) {
                            throw new IOException("Found list containing " + OPENIDM_ORIG_ARRAY
                                    + ", but no element starting with " + OPENIDM_ORIG_ARRAY_TYPE);
                        }
                        String origType = typeListItem.substring(OPENIDM_ORIG_ARRAY_TYPE.length());
                        listToInspect.remove(typeListItem); // remove this marker/meta-data entry

                        try {
                            Class origArrayClazz = null; 
                            origArrayClazz = ctx.getBundle().loadClass(origType);
                            value = listToInspect.toArray((Object[])Array.newInstance(origArrayClazz, 0));
                        } catch (Exception ex) {
                            logger.warn("Failed to convert back to original array type " + origType + " " + ex.getMessage(), ex);
                            throw new IOException("Failed to convert back to original array type " + origType + " " + ex.getMessage(), ex);
                        }
                    } else {
                        value = new java.util.Vector((List)value);
                    }
                }
                converted.put(key, value);
            }
            return converted;
        }
    }

    /*
     * Convert to a key (property name) compatible with storing in the repository
     * 
     * Note: does not support keys that contain a sequence of "_.", which should not occur in our configuration files
     */
    String toEscapedKey(String rawKey) {
        return rawKey.replaceAll("\\.", "__");
    }

    /*
     * Convert from encoded version for repository storage to the original key
     */
    String fromEscapedKey(String escapedKey) {
        return escapedKey.replaceAll("__", ".");
    }

    private String serializeConfig(Object configMap) throws InternalErrorException {
        try {
            ObjectWriter writer = prettyPrint.getWriter();
            return writer.writeValueAsString(configMap);
        } catch (Exception ex) {
            throw new InternalErrorException("Failure in writing formatted and encrypted configuration " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> deserializeConfig(String configString) throws InvalidException {
        try {
            if (configString != null && configString.trim().length() > 0) {
                return mapper.readValue(configString, Map.class);
            }
        } catch (Exception ex) {
            throw new InvalidException("Configuration could not be parsed and may not be valid JSON : " + ex.getMessage(), ex);
        }
        return null;
    }
}
