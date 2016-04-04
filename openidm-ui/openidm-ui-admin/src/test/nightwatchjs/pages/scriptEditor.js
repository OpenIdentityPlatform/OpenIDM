module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#settings/audit/";
    },
    elements: {
        "auditEventTopics" : {
            selector: "#AuditTopicsView"
        },
        "authenticationEvent" : {
            selector: "#AuditTopicsView .editEvent[data-name=authentication]"
        },
        "scriptTab" : {
            selector: "#scriptTab"
        },
        "scriptTabLink" : {
            selector: "#scriptTab a"
        },
        "scriptType" : {
            selector: "#auditEventsForm .event-select"
        },
        "submitAuditEvent" : {
            selector: "#submitAuditEvent"
        }
    }
};