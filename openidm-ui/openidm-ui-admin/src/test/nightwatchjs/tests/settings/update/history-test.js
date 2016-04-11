module.exports = {
    before: {
        // Login
        // navigate to page
    },

    after: {
        // end client
    },

    "It should only display history when history available": function(client) {
        // assert that no history appears
        // mock MaintenanceDelegate and stub repsonse
        // rerender page
        // assert that history appears and displays the correct info
    },

    "It should link out to report": function(client) {
        // click report button
        // wait for report page to load (like 10 seconds)
        // assert that the report does not show the blue text at the top
        // assert that date is displayed under the header
    },

    "Report should have the proper amount of files": function(client) {
        // assert that rows equal the amount of files i sent (.treegrid > .node)
    },

    "Report should link back to version/history page": function(client) {
        // click back to available updates button
        // assert that versions view renders and that history tab is there
    }
};
