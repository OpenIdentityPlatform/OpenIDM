newUserApplicationLnk = {
    "userId" : response._id,
    "state" : "B65FA6A2-D43D-49CB-BEA0-CE98E275A8CD"
}

result = openidm.read("config/ui/applications")
defaultApps = [];

if (result.availableApps) {
    for(app in result.availableApps) {  
        if(result.availableApps[app].isDefault == true) {
            defaultApps.push(result.availableApps[app]._id);
        }
    }
}

for (appId in defaultApps) {
    newUserApplicationLnk.applicationId = defaultApps[appId];
    openidm.create("managed/user_application_lnk", newUserApplicationLnk);
}
