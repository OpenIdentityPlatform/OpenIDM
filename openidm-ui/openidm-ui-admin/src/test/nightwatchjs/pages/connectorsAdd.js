module.exports = {
    url: function() {
        return this.api.globals.baseUrl + '#connectors/add/';
    },
    elements: {
        href: {
            selector: '#connectors/add/'
        },
        connectorForm: {
            selector: '#connectorForm'
        },
        errorMessage: {
            selector: '#connectorErrorMessage'
        },
        generalDetails: {
            selector: '#connectorGeneralDetails'
        },
        connectorDropdown: {
            selector: '#connectorType'
        },
        connectorName: {
            selector: '#connectorName'
        },
        footer: {
            selector: 'div.panel-footer'
        },
        addConnectorButton: {
            selector: '#submitConnector'
        },
        cancelButton: {
            selector: 'a[href="#connectors/"] button.btn'
        }
    },
    sections: {
        generalDetails: {
            selector: '#connectorGeneralDetails',
            elements: {
                enabledToggle: {
                    selector: 'label #connectorEnabled'
                },
                csvDropdown: {
                    selector: 'option[connectortypename="org.forgerock.openicf.csvfile.CSVFileConnector"]'
                },
                dbTableDropdown: {
                    selector: 'option[connectortypename="org.identityconnectors.databasetable.DatabaseTableConnector"]'
                },
                ldapDropdown: {
                    selector: 'option[connectortypename="org.identityconnectors.ldap.LdapConnector"]'
                },
                xmlDropdown: {
                    selector: 'option[connectortypename="org.forgerock.openicf.connectors.xml.XMLConnector"]'
                }
            }
        },
        csvDetails: {
            selector: '#forCsvFileConnector',
            elements: {
                csvFile: {
                    selector: '#csvFile'
                },
                uid: {
                    selector: '#headerUid'
                },
                username: {
                    selector: '#headerName'
                }
            }
        },
        dbTableDetails: {
            selector: '#forDatabaseTableConnector',
            elements: {
                host: {
                    selector: '#host'
                },
                port: {
                    selector: '#port'
                },
                username: {
                    selector: '#user'
                },
                password: {
                    selector: '#password'
                },
                dbName: {
                    selector: '#database'
                },
                table: {
                    selector: '#table'
                },
                keyColumn: {
                    selector: '#keyColumn'
                },
                jdbcDriver: {
                    selector: '#jdbcDriver'
                },
                jdbcUrl: {
                    selector: '#jdbcUrlTemplate'
                },
                emptyStringToggle: {
                    selector: 'label #enableEmptyString'
                },
                rethrowSQLToggle: {
                    selector: 'label #rethrowAllSQLExceptions'
                },
                nativeTimeToggle: {
                    selector: 'label #nativeTimestamps'
                },
                allNativeToggle: {
                    selector: 'label #allNative'
                }
            }
        },
        ldapDetails: {
            selector: '#connectorDetails',
            elements: {
                hostName: {
                    selector: '#ldapHost'
                },
                port: {
                    selector: '#port'
                },
                accountDN: {
                    selector: '#ldapPrincipal'
                },
                password: {
                    selector: '#credentials'
                },
                baseDN: {
                    selector: '#baseDN'
                }
            }
        },
        xmlDetails: {
            selector: '#forXMLConnector',
            elements: {
                xsdPath: {
                    selector: '#xmlxsdFilePath'
                },
                xmlPath: {
                    selector: '#xmlFilePath'
                },
                createIfToggle: {
                    selector: '#xmlCreateIfNotExists'
                }
            }
        }
    }
};
