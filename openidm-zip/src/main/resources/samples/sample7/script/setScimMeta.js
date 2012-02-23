var meta = target.meta;
var currentDate = new Date();

if (typeof meta !== 'object') {
    java.lang.System.out.println("Need to create new object");
    var newMeta = {};
    newMeta.lastModified = currentDate.toDateString();
    newMeta.created = currentDate.toDateString();
} else {
    newMeta.lastModified = currentDate.toDateString();
}

target.meta = newMeta;





