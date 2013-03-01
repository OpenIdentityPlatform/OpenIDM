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

/*global global,source,target */
/*jslint vars:true*/ 

var globalSummary = "<h2>Global Statistics</h2><p>\n"+
"<b>Mapping Name:</b>" + global.reconName+"<br>\n"+
"<b>Recon Id:</b>" + global.reconId +"<br>\n"+
"<b>Start Time:</b>" + global.startTime +"<br>\n"+
"<b>End Time:</b>" + global.endTime+"<br>\n";
                
var sourceSummary = "<h2>Source System Statistics</h2><p>\n"+
"<b>System Name:</b>" + source.reconName+"<br>\n"+
"<b>Start Time:</b>" + source.startTime +"<br>\n"+
"<b>End Time:</b>" + source.endTime+"<br>\n"+
"<b>Duration:</b>" + Math.floor(source.duration/60000)+" minutes<br>\n"+
"<b>Entries:</b>" + source.entries+" read<br>\n"+
"<b>Entry List Query duration:</b>" + Math.floor(source.allIds/1000)+" seconds<br>\n"+
"<h4> SITUATIONS </h4>\n"+
" <ul><li>CONFIRMED: "+source.CONFIRMED.count+"\n"+
" </li><li>MISSING: "+source.MISSING.count+"\n"+
" </li><li>FOUND: "+source.FOUND.count+"\n"+
" </li><li>ABSENT: "+source.ABSENT.count+"\n"+
" </li><li>UNQUALIFIED: "+source.UNQUALIFIED.count+"\n"+
" </li><li>UNASSIGNED: "+source.UNASSIGNED.count+"\n"+
" </ul><br>\n"+
" <b>Invalid entries:</b>" + source.NOTVALID.count+
" <br><br>\n";
                
var targetSummary = "<h2>Target System Statistics</h2><p>\n"+
"<b>System Name:</b>" + target.reconName+"<br>\n"+
"<b>Start Time:</b>" + target.startTime +"<br>\n"+
"<b>End Time:</b>" + target.endTime+"<br>\n"+
"<b>Duration:</b>" + Math.floor(target.duration/60000)+" minutes<br>\n"+
"<b>Entries:</b>" + target.entries+" read<br>\n"+
"<b>Entry List Query duration:</b>" + Math.floor(target.allIds/1000)+" seconds<br>\n"+
"<h4> SITUATIONS </h4>\n"+
" <ul><li>CONFIRMED: "+target.CONFIRMED.count+"\n"+
" </li><li>MISSING: "+target.MISSING.count+"\n"+
" </li><li>FOUND: "+target.FOUND.count+"\n"+
" </li><li>ABSENT: "+target.ABSENT.count+"\n"+
" </li><li>UNQUALIFIED: "+target.UNQUALIFIED.count+"\n"+
" </li><li>UNASSIGNED: "+target.UNASSIGNED.count+"\n"+
" </ul><br>\n"+
" <b>Invalid entries:</b>" + target.NOTVALID.count+
" <br><br>\n";

var sourceIds = "<hr><h2>Detailed Source Statistics</h2><p>\n",
    targetIds = "<hr><h2>Detailed Target Statistics</h2><p>\n",
    params = {},i;

if (source.CONFIRMED.ids.length > 0){
    sourceIds+= "<b><u>Confirmed ids:</b></u><br>\n";
    for(i=0;i<source.CONFIRMED.ids.length;i++) {
        sourceIds+=source.CONFIRMED.ids[i]+"<br>\n";
    }
}
if (source.MISSING.ids.length > 0){
    sourceIds+= "</p><p><b><u>Missing ids:</b></u><br>\n";
    for(i=0;i<source.MISSING.ids.length;i++) {
        sourceIds+=source.MISSING.ids[i]+"<br>\n";
    }
}
if (source.FOUND.ids.length > 0){
    sourceIds+= "</p><p><b><u>Found ids:</b></u><br>\n";
    for(i=0;i<source.FOUND.ids.length;i++) {
        sourceIds+=source.FOUND.ids[i]+"<br>\n";
    }
}

if (source.ABSENT.ids.length > 0){
    sourceIds+= "</p><p><b><u>Absent ids:</b></u><br>\n";
    for(i=0;i<source.ABSENT.ids.length;i++) {
        sourceIds+=source.ABSENT.ids[i]+"<br>\n";
    }
}

if (source.UNQUALIFIED.ids.length > 0){
    sourceIds+= "</p><p><b><u>Unqualified ids:</b></u><br>\n";
    for(i=0;i<source.UNQUALIFIED.ids.length;i++) {
        sourceIds+=source.UNQUALIFIED.ids[i]+"<br>\n";
    }
}

if (source.UNASSIGNED.ids.length > 0){
    sourceIds+= "</p><p><b><u>Unassigned ids:</b></u><br>\n";
    for(i=0;i<source.UNASSIGNED.ids.length;i++) {
        sourceIds+=source.UNASSIGNED.ids[i]+"<br>\n";
    }
}

if (target.CONFIRMED.ids.length > 0){
    targetIds+= "<b><u>Confirmed ids:</b></u><br>\n";
    for(i=0;i<target.CONFIRMED.ids.length;i++) {
        targetIds+=target.CONFIRMED.ids[i]+"<br>\n";
    }
}
if (target.UNQUALIFIED.ids.length > 0){
    targetIds+= "</p><p><b><u>Unqualified ids:</b></u><br>\n";
    for(i=0;i<target.UNQUALIFIED.ids.length;i++) {
        targetIds+=target.UNQUALIFIED.ids[i]+"<br>\n";
    }
}

params._from = "openidm@example.com";
params._to = "idmadmin1@example.com";
params._cc = "idmadmin2@example.com,idmadmin3@example.com";
params._subject = "Recon stats for "+global.reconName;
params._type = "text/html";
params._body = globalSummary+sourceSummary+targetSummary+sourceIds+targetIds;

openidm.action("external/email", params);
