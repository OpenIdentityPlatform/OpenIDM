
/*global source */

(function () {
        
    var fn = source.firstName,
        ln = source.lastName,
        mn = source.middleName,
        mi = source.middleName.substring(0,1),
        hp = source.honorificPrefix,
        hs = source.honorificSuffix,
        formatted = "",
        name = {};
    
    if (hp !== null) {formatted = hp + " "; }
    if (fn !== null) {formatted = formatted + fn + " "; }
    if (mi !== null) {formatted = formatted + mi + " "; }
    if (ln !== null) {formatted = formatted + ln + " "; }
    if (hs !== null) {formatted = formatted + hs; }
    
    name.formatted = formatted.replace(/(^\s*)|(\s*$)/g, "");
    if (ln !== null) {name.familyName = ln; }
    if (fn !== null) {name.givenName = fn; }
    if (mn !== null) {name.middleName = mn; }
    if (hp !== null) {name.honorificPrefix = hp; }
    if (hs !== null) {name.honorificSuffix = hs; }
    
    return name;

}());