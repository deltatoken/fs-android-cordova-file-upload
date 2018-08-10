var argscheck = require('cordova/argscheck');
var exec = require('cordova/exec');

// from webkit source + prevent empty header values
// See RFC 7230, Section 3.2.3.
function isValidHTTPHeaderValue(value)
{
    var c = value.charAt(0);
    var cc;
    if (c == ' ' || c == '\t')
        return false;
    c = value.charAt(value.length - 1);
    if (c == ' ' || c == '\t')
        return false;
    for (var i = 0; i < value.length; ++i) {
        c = value.charAt(i);
        cc = value.charCodeAt(i);
        if (cc == 0x7F || cc > 0xFF || (cc < 0x20 && c != '\t'))
            return false;
    }
    return true;
}

// See RFC 7230, Section 3.2.6.
function isValidHTTPToken(value)
{
    if (value.length == 0)
        return false;
    var c;
    var cc;
    for (var i = 0; i < value.length; ++i) {
        cc = value.charCodeAt(i);
        c = value.charAt(i);
        if (cc <= 0x20 || cc >= 0x7F
            || c == '(' || c == ')' || c == '<' || c == '>' || c == '@'
            || c == ',' || c == ';' || c == ':' || c == '\\' || c == '"'
            || c == '/' || c == '[' || c == ']' || c == '?' || c == '='
            || c == '{' || c == '}')
        return false;
    }
    return true;
}

var FsUploadRequest = function(id, url, filepath, filename){
    argscheck.checkArgs("SSSS", "FsUploadRequest.constructor", arguments);
    this.id = id;
    this.url = url;
    this.filepath = filepath;
    this.filename = filename;
    this.data = {};
    this.headers = {};
    this.mimetype = "application/octet-stream";
    this.description = null;
};

FsUploadRequest.prototype.addParam = function(name, value){
    if (name == null){
        throw new TypeError("name can not be null");
    }
    if (value == null){
        throw new TypeError("value can not be null");
    }
    argscheck.checkArgs("SS", "FsUploadRequest.addParam", arguments);
    if(!isValidHTTPToken(name)){
        throw new TypeError("invalid param name: " + name);
    }
    if(value.length == 0){
        throw new TypeError("param value can not be empty");
    }
    this.data[name] = value;
};

FsUploadRequest.prototype.addHeader = function(name, value){
    if (name == null){
        throw new TypeError("name can not be null");
    }
    if (value == null){
        throw new TypeError("value can not be null");
    }
    argscheck.checkArgs("SS", "FsUploadRequest.addHeader", arguments);
    if(!isValidHTTPToken(name)){
        throw new TypeError("invalid header name: " + name);
    }
    if(value.length == 0){
        throw new TypeError("header value can not be empty");
    }
    if(!isValidHTTPHeaderValue(value)){
        throw new TypeError("invalid header value: " + value);
    }
    this.headers[name] = value;
};

FsUploadRequest.prototype.start = function(successCallback, errorCallback){
    argscheck.checkArgs("FF", "FsUploadRequest.start", arguments);
    exec(successCallback, errorCallback, "FsUpload", "startRequest", [this]);
};

module.exports = FsUploadRequest;