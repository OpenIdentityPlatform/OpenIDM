var groupsList = source.groups;
var groups = [];
var i = 0;
for (i = 0; i < groupsList.length; i += 1) {
    var groupArray = groupsList[i].split("|");
    groups.push({value: groupArray[0], display : groupArray[1]});
}
groups;
