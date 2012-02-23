var fn = source.firstName;
var ln = source.lastName;
var mn = source.middleName;
var mi = source.middleName.substring(0,1);
var hp = source.honorificPrefix;
var hs = source.honorificSuffix;
var formatted = "";
if (hp != null) {formatted = hp + " "}
if (fn != null) {formatted = formatted + fn + " "}
if (mi != null) {formatted = formatted + mi + " "}
if (ln != null) {formatted = formatted + ln + " "}
if (hs != null) {formatted = formatted + hs}

var name = {};
name.formatted = formatted.replace(/(^\s*)|(\s*$)/g, "");
if (ln != null) {name.familyName = ln}
if (fn != null) {name.givenName = fn}
if (mn != null) {name.middleName = mn}
if (hp != null) {name.honorificPrefix = hp}
if (hs != null) {name.honorificSuffix = hs}

name;
