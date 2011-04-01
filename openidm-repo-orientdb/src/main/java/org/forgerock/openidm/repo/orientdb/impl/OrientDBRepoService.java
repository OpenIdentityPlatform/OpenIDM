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
package org.forgerock.openidm.repo.orientdb.impl;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.forgerock.openidm.repo.RepositoryService; 
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.id.ORecordId;
//import com.orientechnologies.orient.core.record.ORecord;
//import com.orientechnologies.orient.core.query.nativ.ONativeSynchQuery;
//import com.orientechnologies.orient.core.query.nativ.OQueryContextNativeSchema;

/**
 * Repository service implementation using OrientDB
 * @author aegloff
 */
@Component(name = "repo-service-orientdb", immediate=true)
@Service(value = RepositoryService.class) 
@Properties({
    @Property(name = "service.description", value = "Repository Service using OrientDB"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "db.type", value = "OrientDB")
})
public class OrientDBRepoService implements RepositoryService {
	final static Logger logger = LoggerFactory.getLogger(OrientDBRepoService.class);
	
	ODatabaseDocumentPool pool;
	
	// TODO make configurable
    String dbURL = "local:./db/openidm"; 
    String user = "admin";
    String password = "admin";
	int poolMinSize = 5;     				 
	int poolMaxSize = 20;

	/**
	 * Retrieve a complete document by identifier
	 * @param id the record identifier. Present in the _id property of documents. 
	 * @return the document in simple binding format
	 */
	public Map<String, Object> get(String id) {
		ORecordId recordId = new ORecordId(id); 
		ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
		
		ODocument doc = (ODocument) db.load(recordId);
		doc.fieldNames(); // TODO TEMPORARY work-around: force the desiralization of the fields (fixed in SVN revision: 2568) Remove this line when fixed in OrientDB.
        Map<String, Object> result = DocumentUtil.toMap(doc);
        pool.release(db);
        logger.trace("get id: {} result: {}", id, result);        
        
        return result;
    }

	/*
	public void update(Map<String, Object> doc) {
		String id = doc.get("_id")
	    if (id == null) {
	    //	throw new PermanentRepoException("Identifier missing in update");
	    }
		ORecordId recordId = new ORecordId(id); 
		// handle OConcurrentModificationException too
		try{
	        db.begin();
			ODocument doc = (ODocument) db.load(recordId);
            db.commit();
	    } catch (Exception e){
	    	db.rollback();
	    	throw e;
	    }
    }
	*/
	
    @Activate
    private void activate(java.util.Map<String, Object> config) {
        logger.trace("Activating Service with configuration {}", config);
        try {
        	pool = DBHelper.initPool(dbURL, user, password, poolMinSize, poolMaxSize);
            logger.info("Repository started.");
        } catch (RuntimeException ex) {
        	logger.warn("Initializing Database Pool failed", ex);
        	throw ex;
        } finally {
        	if (pool != null) {
        		pool.close();
        	}
        }
    }
    
    @Deactivate
    private void deactivate(Map<String, Object> config) {
        logger.trace("Deactivating Service {}", config);
        if (pool != null) {
        	pool.close();
        }
        logger.info("Repository stopped.");
    }
   
}