module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#settings/audit/";
    },
    elements: {
        globalAlert: {
            selector: ".alert-system"
        },
        modal: {
            selector: ".modal-content"
        }
    },
    sections: {
        eventHandlers: {
            selector: "#AuditEventHandlersBody",
            elements: {
                editHandlerButton: {
                    selector: ".editEventHandler"
                },
                useForQueriesRadio: {
                    selector: 'input[name="useForQueries"]'
                },
                csvEditButton: {
                    selector: ".editEventHandler[data-name=csv]"
                },
                eventHandlerSelect: {
                    selector: "#addAuditModuleSelect"
                },
                eventHandlerCSVOption: {
                    selector: "#addAuditModuleSelect option[value='org.forgerock.audit.handlers.csv.CsvAuditEventHandler']"
                },
                eventHandlerJMSOption: {
                    selector: "#addAuditModuleSelect option[value='org.forgerock.audit.handlers.jms.JmsAuditEventHandler']"
                },
                addEventHandlerButton: {
                    selector: ".addEventHandler"
                },
                csvUseForQueries: {
                    selector: "#AuditEventHandlersView tbody tr:nth-child(1) td:nth-child(4) input"
                },
                repoUseForQueries: {
                    selector: "#AuditEventHandlersView tbody tr:nth-child(2) td:nth-child(4) input"
                },
                jmsUseForQueries: {
                    selector: "#AuditEventHandlersView tbody tr:nth-child(3) td:nth-child(4) input"
                }
            }
        },
        eventHandlersDialog: {
            selector: ".modal-content",
            elements: {
                title: {
                    selector: ".bootstrap-dialog-title"
                },
                enableField: {
                    selector: "#auditEventHandlersForm > div.form-group:nth-child(3) p"
                },
                name: {
                    selector: "#eventHandlerName"
                },
                enabledCheck: {
                    selector: "#enabled"
                },
                closeButton: {
                    selector: ".close"
                },
                submitAuditEventHandlers: {
                    selector: "#submitAuditEventHandlers"
                },
                signatureIntervalInput:{
                    selector: "[data-schemapath='root.security.signatureInterval'] input"
                },
                propertiesContainerFirstChild: {
                    selector: "#auditEventHandlerConfig div:first-child"
                }
            }
        }
    }
};
