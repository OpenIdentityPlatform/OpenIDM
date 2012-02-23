var imAddresses = source.ims;
var ims = [];
var i = 0;
for (i = 0; i < imAddresses.length; i += 1) {
    var imArray = imAddresses[i].split(":");Ê
    ims.push({value: imArray[1], type : imArray[0]});
}
ims;
