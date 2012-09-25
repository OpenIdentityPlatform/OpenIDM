var parent = request.parent;

if (parent.type !== 'root') {
    params = {  '_query-id' : 'filtered-query',
                '_var-lnkId' : request.id.split("/").slice(-1)[0]
            }

    result = openidm.query("workflow/processinstance", params);

    if (result.result && result.result.length > 0) {
        var i;
        for (i = 0 ; i < result.result.length; i++) {
            openidm['delete']("workflow/processinstance/"+result.result[i]._id);
        }
    }
}


