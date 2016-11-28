define(["org/forgerock/openidm/ui/admin/connector/AddConnectorView"], function(AddConnectorView) {
    QUnit.module('AddConnectorView Tests');

    QUnit.test("build connector type selection", function() {
        var connectors = [
            {
                "systemType": "provisioner.openicf",
                "bundleName": "org.forgerock.openicf.connectors.groovy-connector",
                "connectorName": "org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector",
                "displayName": "Scripted SQL Connector",
                "bundleVersion": "1.4.2.1"
            }, {
                "systemType": "provisioner.openicf",
                "bundleName": "org.forgerock.openicf.connectors.groovy-connector",
                "connectorName": "org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector",
                "displayName": "Scripted REST Connector",
                "bundleVersion": "1.4.2.1"
            }, {
                "systemType": "provisioner.openicf",
                "bundleName": "org.forgerock.openicf.connectors.databasetable-connector",
                "connectorName": "org.identityconnectors.databasetable.DatabaseTableConnector",
                "displayName": "Database Table Connector",
                "bundleVersion": "1.1.0.2"
            }, {
                "systemType": "provisioner.openicf",
                "bundleName": "org.forgerock.openicf.connectors.csvfile-connector",
                "connectorName": "org.forgerock.openicf.csvfile.CSVFileConnector",
                "displayName": "CSV File Connector",
                "bundleVersion": "1.5.1.4"
            }
            ],
            expected = [
                {
                    "groupName": "CSV File Connector",
                    "versions": [
                        {
                            "systemType": "provisioner.openicf",
                            "bundleName": "org.forgerock.openicf.connectors.csvfile-connector",
                            "connectorName": "org.forgerock.openicf.csvfile.CSVFileConnector",
                            "displayName": "CSV File Connector",
                            "bundleVersion": "1.5.1.4"
                        }
                    ]
                }, {
                    "groupName": "Database Table Connector",
                    "versions": [
                        {
                            "systemType": "provisioner.openicf",
                            "bundleName": "org.forgerock.openicf.connectors.databasetable-connector",
                            "connectorName": "org.identityconnectors.databasetable.DatabaseTableConnector",
                            "displayName": "Database Table Connector",
                            "bundleVersion": "1.1.0.2"
                        }
                    ]
                }, {
                    "groupName": "Scripted REST Connector",
                    "versions": [
                        {
                            "systemType": "provisioner.openicf",
                            "bundleName": "org.forgerock.openicf.connectors.groovy-connector",
                            "connectorName": "org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector",
                            "displayName": "Scripted REST Connector",
                            "bundleVersion": "1.4.2.1"
                        }
                    ]
                }, {
                    "groupName": "Scripted SQL Connector",
                    "versions": [
                        {
                            "systemType": "provisioner.openicf",
                            "bundleName": "org.forgerock.openicf.connectors.groovy-connector",
                            "connectorName": "org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector",
                            "displayName": "Scripted SQL Connector",
                            "bundleVersion": "1.4.2.1"
                        }
                    ]
                }
            ],
            expectedFilter = [
                {
                    "groupName": "CSV File Connector",
                    "versions": [
                        {
                            "systemType": "provisioner.openicf",
                            "bundleName": "org.forgerock.openicf.connectors.csvfile-connector",
                            "connectorName": "org.forgerock.openicf.csvfile.CSVFileConnector",
                            "displayName": "CSV File Connector",
                            "bundleVersion": "1.5.1.4"
                        }
                    ]
                }, {
                    "groupName": "Database Table Connector",
                    "versions": [
                        {
                            "systemType": "provisioner.openicf",
                            "bundleName": "org.forgerock.openicf.connectors.databasetable-connector",
                            "connectorName": "org.identityconnectors.databasetable.DatabaseTableConnector",
                            "displayName": "Database Table Connector",
                            "bundleVersion": "1.1.0.2"
                        }
                    ]
                }
            ],
            actual = AddConnectorView.buildConnectorTypes(connectors),
            actualFilter = AddConnectorView.filterConnectorTypes(expected);

        QUnit.deepEqual(actual, expected, "connector versions array created");
        QUnit.deepEqual(actualFilter, expectedFilter, "connector versions array filtered");
    });
});
