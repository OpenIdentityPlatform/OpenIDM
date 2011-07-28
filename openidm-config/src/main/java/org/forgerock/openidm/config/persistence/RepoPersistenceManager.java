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
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoPersistenceManager implements PersistenceManager {

    // The ID URI prefix for configuration
    private static final String CONFIG_CONTEXT_PREFIX = "config/";
    
    // Meta-data when converting from array to list to enable later conversion to original form
    private static final String OPENIDM_ORIG_ARRAY = "_openidm_orig_array";
    private static final String OPENIDM_ORIG_ARRAY_TYPE = "_openidm_orig_array_type=";

    final static Logger logger = LoggerFactory.getLogger(RepoPersistenceManager.class);
    
    private BundleContext ctx;
    private RepoBootService repo;
    
    // Fall-back is in-memory store of configurations
    Map<String, Dictionary> tempStore = new HashMap<String, Dictionary>();
    
    public RepoPersistenceManager(final BundleContext ctx) {
        this.ctx = ctx;
        logger.debug("Bootstrapping Repository Persistence Manager");
        bootstrapRepo(); // This happens asynchronously, i.e. repo may not be initialized when this returns
    }
    
    void bootstrapRepo() {
        try {
            
            Filter filter = ctx.createFilter("(" + Constants.OBJECTCLASS + "=" + RepoBootService.class.getName() + ")");
            final ServiceTracker repoTracker = new ServiceTracker(ctx, filter, null);
            repoTracker.open();
            
            new Thread() {
                public void run() {
                    try {
                        while (repo == null) {
                            
                            logger.debug("Bootstrapping repository");
                            repo = (RepoBootService) repoTracker.waitForService(5000);
                            if (repo != null) {
                                logger.debug("Bootstrap obtained repository");
                            }
                        }

                        ConfigurationAdmin configAdmin = null;
                        logger.debug("Proceed to handling configuration files");
                        while (configAdmin == null) {
                            Filter admFilter = ctx.createFilter("(" + Constants.OBJECTCLASS + "=" + ConfigurationAdmin.class.getName() + ")");
                            final ServiceTracker adminTracker = new ServiceTracker(ctx, admFilter, null);
                            adminTracker.open();
                            configAdmin = (ConfigurationAdmin) adminTracker.waitForService(5000);
                        }
                        
                        // Proceed to configuration file handling once repo bootstrapped 
                        // and configuration admin service is available
                        ConfigBootstrapHelper.installAllConfig(configAdmin);
                        logger.debug("Enabled handling of configuration files");
                    } catch (InterruptedException ex) {
                        logger.warn("Failed to bootstrap " + ex.getMessage(), ex);
                    } catch (Exception ex) {
                        logger.warn("Failed to bootstrap " + ex.getMessage(), ex);
                    }
                }
            }.start();
        } catch (InvalidSyntaxException ex) {
            logger.warn("Failed to bootstrap repo " + ex.getMessage(), ex);
        }        
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

        if (repo != null) {
            String id = pidToId(pid);
            try {
                Map existing = repo.read(id);
                exists = (existing != null);
            } catch (NotFoundException ex) {
                exists = false;
            } catch (ObjectSetException ex) {
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
    public Dictionary load(String pid) throws IOException {
        logger.debug("Config load call for {}", pid);
        Dictionary result = null;
        
        try {
            if (repo != null) {
                String id = pidToId(pid);
                Map existing = repo.read(id);
                logger.debug("Config loaded {} {}", pid, existing);
                result = mapToDict(existing);
            } 
        } catch (NotFoundException ex) {
            result = tempStore.get(pid);
            if (result == null) {
                throw new IOException("No entry for " + pid + " exists.");
            }
            
            logger.debug("Config loaded from temporary store {} {}", pid, result);
        } catch (ObjectSetException ex) {
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
    public Enumeration getDictionaries() throws IOException {
        if (repo == null) {
            logger.debug("Config getDictionaries call from temporary store");
        } else {
            logger.debug("Config getDictionaries call from repository");
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
                        if (repo != null && dbIter == null) {
                            Map params = new HashMap();
                            params.put(QueryConstants.QUERY_ID, "query-all-ids");
                            Map<String, Object> result = repo.query("config", params);
                            List<Map<String, Object>> queryResult = (List<Map<String, Object>>) result.get(QueryConstants.QUERY_RESULT);
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
                        String entryId = (String) entry.get(RepositoryService.ID);
                        Dictionary fullEntry = load(entryId);

                        return fullEntry;
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
    public void store(String pid, Dictionary properties) throws IOException {
        logger.debug("Store call for {} {}", pid, properties);
        
        // Store config handling settings in memory
        if (pid.startsWith("org.apache.felix.fileinstall")) {
            tempStore.put(pid, properties);
            return;
        }
        
        try {
            if (repo != null) {
                String id = pidToId(pid);
                Map obj = dictToMap(properties);
                Map existing = null;
                try {
                    existing = repo.read(id);
                } catch (NotFoundException ex) {
                    // Just detect that it doesn't exist
                }
                if (existing != null) {
                    String rev = (String) existing.get(RepositoryService.REV);
                    
                    existing.remove(RepositoryService.REV);
                    existing.remove(RepositoryService.ID);
                    obj.remove(RepositoryService.REV); // beware, this means _id and _rev should not be in config file
                    obj.remove(RepositoryService.ID); // beware, this means _id and _rev should not be in config file
                    if(!existing.equals(obj)) {
                        logger.trace("Not matching {} {}", existing, obj);
                        boolean retry;
                        do {
                            retry = false;
                            try {
                                repo.update(id, rev, obj);
                            } catch (PreconditionFailedException ex) {
                                logger.debug("Concurrent change during update, retrying {} {}", pid, rev);
                                existing = repo.read(id);
                                retry = true;
                            }
                        } while (retry);
                        logger.debug("Updated existing config {} {} {}", new Object[] {pid, rev, obj});
                    } else {
                        logger.debug("Existing config same as store request, ignoring {} {} {}", new Object[] {pid, rev, obj});
                    }
                } else {
                    logger.trace("Creating: {} {} ", id, obj);
                    repo.create(id, obj);
                    logger.debug("Stored new config in repository {} {}", pid, obj);
                }
            } else {
                tempStore.put(pid, properties);
                logger.debug("Stored in memory {} {}", pid, properties);
            }
        } catch (ObjectSetException ex) {
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
            if (repo != null) {
                String id = pidToId(pid);
                boolean retry;
                String rev = null;
                do {
                    retry = false;
                    try {
                        Map<String, Object> existing = repo.read(id);
                        if (existing != null) {
                            rev = (String) existing.get(RepositoryService.REV);
                            repo.delete(id, rev);
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
        } catch (ObjectSetException ex) {
            throw new IOException("Failed to delete configuration + " + pid + " in repository: " + ex.getMessage(), ex);
        }
    }
     
    /**
     * Convert OSGi pid to an ID suitable for addressing the repository
     * @param pid the OSGi pid
     * @return the qualified objectset id
     */
    String pidToId(String pid) {
        return CONFIG_CONTEXT_PREFIX + pid;
    }
    
    /**
     * Convert dictionary and contents to a Map, compatible with the JSON model.
     * Arrays are encoded with special meta-data entries marking their origin as array.
     * 
     * @param dict the (OSGi configuration) dictionary
     * @return the converted map
     */
    Map dictToMap(Dictionary dict) {
        Map convert = new HashMap();
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
}
