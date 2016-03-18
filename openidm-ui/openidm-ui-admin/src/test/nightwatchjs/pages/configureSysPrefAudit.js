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
                    selector: "#AuditEventHandlersBody tbody tr:first-child .editEventHandler"
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
                    selector: ".btn-default"
                }
            }
        }
    }
};
