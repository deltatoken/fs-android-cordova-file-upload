var argscheck = require('cordova/argscheck');
var exec = require('cordova/exec');

var FsUploadErrorType = {
    MALFORMED_URL: "malformed_url",
    REQUEST_WRITE_FAILURE: "request_write_failure",
    MAX_ATTEMPTS_REACHED: "max_attempts_reached",
    FILE_NOT_FOUND: "file_not_found",
    FILE_READ_FAILURE: "file_read_failure",
    INVALID_REQUEST: "invalid_request",
    INVALID_REQUEST_PARAMS: "invalid_request_params",
    UPLOAD_IO_FAILURE: "upload_io_failure",
    RESPONSE_READ_FAILURE: "response_read_failure",
    RESPONSE_ERROR: "response_error",
    INTERNAL_PLUGIN_ERROR: "internal_plugin_error"
};

module.exports = FsUploadErrorType;