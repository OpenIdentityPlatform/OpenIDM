package org.forgerock.openidm.security.impl;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.apache.commons.lang3.tuple.Pair;
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

    @Override
    public void createDefaultEntry(String alias) throws Exception {
        Pair<X509Certificate, PrivateKey> pair = generateCertificate("local.openidm.forgerock.org", 
                "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
        Certificate cert = pair.getKey();
        store.getStore().setCertificateEntry(alias, cert);
        store.store();
    }
}
