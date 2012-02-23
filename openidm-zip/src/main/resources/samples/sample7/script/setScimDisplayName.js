var GN = source.firstName;
var FN = source.lastName;
var UID = source.name;

var displayName = "";
if (GN !== null) {displayName = GN + " "; }
if (FN !== null) {displayName = displayName + FN; }
if (GN === null && FN === null) {displayName = UID; }

displayName.replace(/(^\s*)|(\s*$)/g, "");

displayName;
