package org.forgerock.openidm.crypto.factory;

import java.security.KeyStore;

public interface CryptoUpdateService {

    public void updateKeySelector(KeyStore ks, String password);
}
