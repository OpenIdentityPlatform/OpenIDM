package org.forgerock.openidm.security.impl;

import java.security.cert.Certificate;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection resource provider servicing requests on certificate entries in a keystore
 * 
 * @author ckienle
 */
public class CertificateResourceProvider extends EntryResourceProvider {

    private final static Logger logger = LoggerFactory.getLogger(CertificateResourceProvider.class);
    
	public CertificateResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager, ServerContext accessor) {
        super(resourceName, store, manager, accessor);
    }

    @Override
    protected void storeEntry(JsonValue value, String alias) throws Exception {
    	String type = value.get("type").defaultTo(DEFAULT_CERTIFICATE_TYPE).asString();
    	String certString = value.get("cert").required().asString();
    	Certificate cert = readCertificate(certString, type);
    	store.getStore().setCertificateEntry(alias, cert);
        store.store();
    }

    @Override
    protected JsonValue readEntry(String alias) throws Exception {
    	Certificate cert = store.getStore().getCertificate(alias);
    	return returnCertificate(alias, cert);
    }
}
