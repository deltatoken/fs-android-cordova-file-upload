var argscheck = require('cordova/argscheck');
var exec = require('cordova/exec');

var FsUpload = function(){
};

FsUpload.prototype.setUploadCallback = function(successCallback, errorCallback){
    argscheck.checkArgs("FFF", "FsUpload.setUploadCallback", arguments);
    exec(successCallback, errorCallback, "FsUpload", "setUploadCallback", []);
};

FsUpload.prototype.removeUploadCallback = function(successCallback, errorCallback){
    argscheck.checkArgs("FF", "FsUpload.removeUploadCallback", arguments);
    exec(successCallback, errorCallback, "FsUpload", "removeUploadCallback", []);
};

FsUpload.prototype.getUploads = function(ids, completionStatus, successCallback, errorCallback){
    argscheck.checkArgs("*SFF", "FsUpload.getUploads", arguments);
    exec(successCallback, errorCallback, "FsUpload", "getUploads", [ids, completionStatus]);
};

FsUpload.prototype.resetUploads = function(ids, completionStatus, successCallback, errorCallback){
    argscheck.checkArgs("*SFF", "FsUpload.resetUploads", arguments);
    exec(successCallback, errorCallback, "FsUpload", "resetUploads", [ids, completionStatus]);
};

FsUpload.prototype.deleteUploads = function(ids, completionStatus, successCallback, errorCallback){
    argscheck.checkArgs("*SFF", "FsUpload.deleteUploads", arguments);
    exec(successCallback, errorCallback, "FsUpload", "deleteUploads", [ids, completionStatus]);
};

FsUpload.prototype.setConnectTimeout = function(timeoutMillis, successCallback, errorCallback){
    argscheck.checkArgs("NFF", "FsUpload.setConnectTimeout", arguments);
    exec(successCallback, errorCallback, "FsUpload", "setConnectTimeout", [timeoutMillis]);
};

FsUpload.prototype.setSocketTimeout = function(timeoutMillis, successCallback, errorCallback){
    argscheck.checkArgs("NFF", "FsUpload.setSocketTimeout", arguments);
    exec(successCallback, errorCallback, "FsUpload", "setSocketTimeout", [timeoutMillis]);
};

FsUpload.prototype.setWifiOnly = function(wifiOnly, successCallback, errorCallback){
    argscheck.checkArgs("NFF", "FsUpload.setWifiOnly", arguments);
    exec(successCallback, errorCallback, "FsUpload", "setWifiOnly", [wifiOnly]);
};

FsUpload.prototype.getWifiOnly = function(successCallback, errorCallback){
    argscheck.checkArgs("FF", "FsUpload.getWifiOnly", arguments);
    exec(successCallback, errorCallback, "FsUpload", "getWifiOnly", []);
};

FsUpload.prototype.setUploadVideosOnWifiOnly = function(wifiOnly, successCallback, errorCallback){
    argscheck.checkArgs("NFF", "FsUpload.setUploadVideosOnWifiOnly", arguments);
    exec(successCallback, errorCallback, "FsUpload", "setUploadVideosOnWifiOnly", [wifiOnly]);
};

FsUpload.prototype.getUploadVideosOnWifiOnly = function(successCallback, errorCallback){
    argscheck.checkArgs("FF", "FsUpload.getUploadVideosOnWifiOnly", arguments);
    exec(successCallback, errorCallback, "FsUpload", "getUploadVideosOnWifiOnly", []);
};

FsUpload.prototype.getConnectTimeout = function(successCallback, errorCallback){
    argscheck.checkArgs("FF", "FsUpload.getConnectTimeout", arguments);
    exec(successCallback, errorCallback, "FsUpload", "getConnectTimeout", []);
};

FsUpload.prototype.getSocketTimeout = function(successCallback, errorCallback){
    argscheck.checkArgs("FF", "FsUpload.getSocketTimeout", arguments);
    exec(successCallback, errorCallback, "FsUpload", "getSocketTimeout", []);
};

FsUpload.prototype.version = function(){
    return "0.0.1";
};

module.exports = new FsUpload();
