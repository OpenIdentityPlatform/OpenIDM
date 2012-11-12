var params = {
    "_query-id": "for-userName",
    "uid": request.params.login
}, res = {};
      
ret = openidm.query("managed/user", params);

if(ret && ret.result && ret.result[0] && ret.result[0].passPhrase && ret.result[0].siteImage) {
    res = {
        "passPhrase": ret.result[0].passPhrase, 
        "siteImage": ret.result[0].siteImage
    };
} else {
    var code = new java.lang.String(request.params.login).hashCode();
    code = java.lang.Math.abs(code);        

    ret = openidm.read("config/ui/configuration")

    var passPhrases = [
        "human",
        "letter",
        "bird"
    ];
    
    res = {
        "siteImage": ret.configuration.siteImages[code % ret.configuration.siteImages.length],
        "passPhrase": passPhrases[code % passPhrases.length]
    };
}

res