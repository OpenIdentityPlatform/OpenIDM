// Jscript example for the "result" script that deals with the recon stats. See:
// https://wikis.forgerock.org/confluence/display/openidm/Reconciliation+reports
// First build the HTML body with the recon statistics
// Then use the external email service to email the report
// 3 objects are passed to the script:
// 
// global: contains general information about reconciliation (mapping name,
// recon id, start date, end date)
// 
// source: contains detailed information about source system processing
// (system name, start date, end date, duration, situations/entries)
// 
// target: contains detailed information about target system processing
// (system name, start date, end date, duration, situations/entries)
// 
// Author: Gael.Allioux@forgerock.com
// Date: 11/30/2011

// Modifications by Jake.Feasel@forgerock.com
// Date: 4/8/2014

/*global global,source,target */

var params = {
        //UPDATE THESE VALUES
        _from : "openidm@example.com",
        _to : "idmadmin1@example.com",
        _cc : "idmadmin2@example.com,idmadmin3@example.com",
        _subject : "Recon stats for " + global.reconName,
        _type : "text/html"
    },
    template,
    Handlebars;

// if there is a configuration found, assume that it has been properly configured
if (openidm.read("config/external.email")) {

    source.durationMinutes = Math.floor(source.duration/60000);
    source.entryListDurationSeconds = Math.floor(source.allIds/1000);

    target.durationMinutes = Math.floor(target.duration/60000);
    target.entryListDurationSeconds = Math.floor(target.allIds/1000);

    load(identityServer.getInstallLocation() + "/bin/defaults/script/lib/handlebars.js");

    template = Handlebars.compile(readFile(identityServer.getProjectLocation() + "/script/reconStatTemplate.html"));

    params._body = template({
        "global": global,
        "source": source,
        "target": target
    });

    openidm.action("external/email", "noop", {}, params);

} else {
    console.log("Email service not configured; report not generated. ");
}

