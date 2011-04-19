package org.forgerock.openidm.repo.orientdb.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredefinedQueries {

    final static Logger logger = LoggerFactory.getLogger(PredefinedQueries.class);
    
    /**
     * Query by primary key, the OpenIDM identifier. This identifier is different from the OrientDB record id.
     * 
     * @param id the OpenIDM identifier for an object
     * @param type the OrientDB class
     * @param database a handle to the OrientDB database object. No other thread must operate on this concurrently.
     * @return The ODocument if found, null if not found.
     * @throws IllegalArgumentException if the passed identifier or type are invalid
     */
    public ODocument getByID(final String id, final String type, ODatabaseDocumentTx database) throws IllegalArgumentException {

        if (id == null) {
            throw new IllegalArgumentException("Query by id the passed id was null.");
        } else if (type == null) {
            throw new IllegalArgumentException("Query by id the passed type was null.");
        }
        
        // TODO: convert into a prepared statement
        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>("select * from " + type + " where " + DocumentUtil.ORIENTDB_PRIMARY_KEY + " = '" + id + "'");
        List<ODocument> result = database.query(query);
        logger.trace("Query: {} Result: {}", query, result);
        ODocument first = null;
        if (result.size() > 0) {
            first = result.get(0); // ID is of type unique index, there must only be one at most
        }
        return first;
    }

}
