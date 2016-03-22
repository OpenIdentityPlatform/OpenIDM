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
                csvEditButton: {
                    selector: ".editEventHandler[data-name=csv]"
                },
                eventHandlerSelect: {
                    selector: "#addAuditModuleSelect"
                },
                eventHandlerCSVOption: {
                    selector: "#addAuditModuleSelect option[value='org.forgerock.audit.handlers.csv.CsvAuditEventHandler']"
                },
                addEventHandlerButton: {
                    selector: ".addEventHandler"
                }
            }
        },
        eventHandlersDialog: {
            selector: ".modal-content",
            elements: {
                title: {
                    selector: ".bootstrap-dialog-title"
                },
                enabledCheck: {
                    selector: "#enabled"
                },
                closeButton: {
                    selector: ".close"
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
