var meta = target.meta;
var currentDate = new Date();
java.lang.System.out.println(typeof meta);
if (typeof meta !== 'object' | typeof meta === null) {
    java.lang.System.out.println("Need to create new object");
    var newMeta = {};
    newMeta.lastModified = currentDate.toString();
    newMeta.created = currentDate.toString();
    target.meta = newMeta;
} else {
    java.lang.System.out.println("Object Reused");
    target.meta.lastModified = currentDate.toString();
}
