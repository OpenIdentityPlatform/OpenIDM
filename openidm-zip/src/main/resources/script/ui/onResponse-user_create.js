user = openidm.read("managed/user/" + response._id);
userName = user.userName;

params = {'_query-id' : 'query-default-applications'};

result = openidm.query("managed/application", params);

defaultApps = result.result;

newUserApplicationLnk = {
    "userName" : userName,
    "state" : "B65FA6A2-D43D-49CB-BEA0-CE98E275A8CD"
}

for (appId in defaultApps) {
    newUserApplicationLnk.applicationId = defaultApps[appId]._id;
    openidm.create("managed/user_application_lnk/1",newUserApplicationLnk);
}
