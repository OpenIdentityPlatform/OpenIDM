/*global java*/
/*global exception*/
function format() {
    var sw, pw;
    sw = new java.io.StringWriter();
    pw = new java.io.PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
}
format();