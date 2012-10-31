if (request.method != "read") {
     throw "Unsupported operation on ping info service: " + request.method
}
healthinfo
