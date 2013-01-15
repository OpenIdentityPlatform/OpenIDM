
/*global source */

(function () {
    var GN = source.firstName,
        FN = source.lastName,
        UID = source.name,
        displayName = "";
    
    if (GN !== null) { displayName = GN + " "; }
    if (FN !== null) { displayName = displayName + FN; }
    if (GN === null && FN === null) { displayName = UID; }
    
    displayName.replace(/(^\s*)|(\s*$)/g, "");
    
    return displayName;
}());