
/*global healthinfo */

if (request.method !== "read") {
     throw "Unsupported operation on ping info service: " + request.method;
}
(function () {
    
    healthinfo.sampleprop="Example customization";
    return healthinfo;
    
}());