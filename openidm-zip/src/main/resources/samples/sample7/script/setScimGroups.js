
/*global source */

(function () {
    var groupsList = source.groups,
        groups = [],
        groupArray,
        i = 0;
    
    for (i = 0; i < groupsList.length; i += 1) {
        groupArray = groupsList[i].split("|");
        groups.push({value: groupArray[0], display : groupArray[1]});
    }
    return groups;
}());