/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */
package org.forgerock.openidm.provisioner.salesforce.internal;

import org.forgerock.json.fluent.JsonValue;
import org.osgi.framework.Bundle;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Provides "connector" info for the Salesforce "connector".
 */
public class SalesforceConnectorInfo {
    static JsonValue getConnectorInfo(Bundle bundle) {
        return json(object(
                field("bundleName", bundle.getSymbolicName()),
                field("bundleVersion", bundle.getVersion().toString()),
                field("displayName", "Salesforce Connector"),
                field("connectorName", "org.forgerock.openidm.salesforce.Salesforce")
        ));
    }
}
