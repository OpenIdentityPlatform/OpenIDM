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
 * Portions copyright 2/24/17 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import static org.forgerock.opendj.ldap.LdapException.newLdapException;
import static org.forgerock.opendj.ldap.ResultCode.UNDEFINED;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServer;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServerException;
import org.forgerock.util.promise.Promise;

public class EmbeddedServerConnectionFactory implements ConnectionFactory {
    private final EmbeddedDirectoryServer embeddedDirectoryServer;

    EmbeddedServerConnectionFactory(final EmbeddedDirectoryServer embeddedDirectoryServer) {
        this.embeddedDirectoryServer = embeddedDirectoryServer;
    }

    @Override
    public void close() {
        // nothing to close. EmbeddedDirectoryServer should be closed externally
    }

    @Override
    public Promise<Connection, LdapException> getConnectionAsync() {
        try {
            return newResultPromise(embeddedDirectoryServer.getInternalConnection());
        } catch (EmbeddedDirectoryServerException e) {
            return newExceptionPromise(newLdapException(UNDEFINED, "Failed to acquire embedded connection", e));
        }
    }

    @Override
    public Connection getConnection() throws LdapException {
        try {
            return embeddedDirectoryServer.getInternalConnection();
        } catch (EmbeddedDirectoryServerException e) {
            throw newLdapException(UNDEFINED, "Failed to acquire embedded connection", e);
        }
    }
}
