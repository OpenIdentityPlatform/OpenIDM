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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * A Helper to interact with the OrientDB
 * @author aegloff
 */
public class DBHelper {
    final static Logger logger = LoggerFactory.getLogger(DBHelper.class);

    /**
     * Initialize the DB pool.
     * @param dbURL the orientdb URL
     * @param user the orientdb user to connect
     * @param password the orientdb password to connect
     * @param minSize the orientdb pool minimum size
     * @param maxSize the orientdb pool maximum size
     * @return the initialized pool
     */
    public static ODatabaseDocumentPool initPool(String dbURL, String user, String password, int minSize, int maxSize) {
        logger.trace("Initializing DB Pool");
        
        checkDB(dbURL, user, password);
        
        ODatabaseDocumentPool pool = ODatabaseDocumentPool.global();
        pool.setup(minSize, maxSize);
        warmUpPool(pool, dbURL, user, password, minSize);
        
        return pool;
    }
    
    /**
     * Ensure the min size pool entries are initilized.
     * Cuts down on some (small) initial latency with lazy init
     * Do not call with a min past the real pool max, it will block.
     */
    private static void warmUpPool(ODatabaseDocumentPool pool, String dbURL, String user, String password, int minSize) {
        logger.trace("Warming up pool up to minSize {}", Integer.valueOf(minSize));
        List<ODatabaseDocumentTx> list = new ArrayList<ODatabaseDocumentTx>();
        for (int count=0; count < minSize; count++) {
            logger.trace("Warming up entry {}", Integer.valueOf(count));
            list.add(pool.acquire(dbURL, user, password));
        }
        for (ODatabaseDocumentTx entry : list) {
            pool.release(entry);
        }
    }
    
    /**
     * Ensures the DB is present in the expected form.
     */
    private static void checkDB(String dbURL, String user, String password) {
        // TODO: Creation/opening of db may be not be necessary if we require this managed externally
        ODatabaseDocumentTx db = null;
        try {
            db = new ODatabaseDocumentTx(dbURL); 
            if (db.exists()) {
                logger.info("Using DB at {}", dbURL);
                db.open(user, password); 
            } else { 
                logger.info("DB does not exist, creating {}", dbURL);
                db.create(); 	       
                populateSample(db);
            } 
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // TODO: This is temporary until we have the mechanisms in place to laod default and test data
    private static void populateSample(ODatabaseDocumentTx db) {
        OSchema schema = db.getMetadata().getSchema();
        OClass user = schema.createClass("User", OStorage.CLUSTER_TYPE.PHYSICAL); 
        schema.save(); 
        
        // Sample test data
        int counter = 0;
        for(int i = 0; i < 10; i++, counter++) {
            ODocument usr = new ODocument(db, "User");
            usr.field( "firstname", "John-" + counter );
            usr.field( "lastname", "Doe-" + counter );
            usr.field( "address", "Somewhere Land " + counter );
            usr.field( "zip", "12345");
            usr.field( "_schema_id", "http://forgerock.com/schema/some/sample");
            usr.field( "_schema_rev", Integer.valueOf(0));
            usr.field( "time", "Time: " + new java.util.Date());
            usr.save();
            //System.out.println(usr.getIdentity());
        }
        System.out.println("DB populated with test data."); // output to console so we don't forget this is still here
        
/*
        long start = System.currentTimeMillis();
        java.util.Map result = this.get("5:50000");
        long end = System.currentTimeMillis();
        System.out.println("ms: " + (end - start));
        System.out.println("result: " + result);
        
        start = System.currentTimeMillis();
        System.out.println(((ODocument) db.load(new ORecordId("5:50000"))).toJSON());
        result = DocumentUtil.toMap((ODocument) db.load(new ORecordId("5:50000")));
        end = System.currentTimeMillis();
        System.out.println("ms: " + (end - start));
        System.out.println("result: " + result);
        
        start = System.currentTimeMillis();
        result = this.get("5:10");
        end = System.currentTimeMillis();
        System.out.println("ms: " + (end - start));
        System.out.println("result: " + result);
        
        start = System.currentTimeMillis();
        result = this.get("5:99999");
        end = System.currentTimeMillis();
        System.out.println("ms: " + (end - start));
        System.out.println("result: " + result);
        */
    }
}