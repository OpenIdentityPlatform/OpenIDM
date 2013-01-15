
/*global target */

var meta = target.meta,
    currentDate = new Date(),
    newMeta = {};

if (typeof meta !== 'object') {
    java.lang.System.out.println("Need to create new object");
    newMeta.lastModified = currentDate.toDateString();
    newMeta.created = currentDate.toDateString();
} else {
    newMeta.lastModified = currentDate.toDateString();
}

target.meta = newMeta;