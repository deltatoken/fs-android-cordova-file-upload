var argscheck = require('cordova/argscheck');
var exec = require('cordova/exec');

var FsCompletionStatus = {
    ALL: "all",
    NOT_COMPLETED: "not_completed",
    COMPLETED: "completed",
    COMPLETED_WITH_ERROR: "completed_with_error",
    COMPLETED_WITH_ERROR_ATTEMPTS_EXCEED: "completed_with_error_attempts_exceed",
    COMPLETED_WITH_SUCCESS: "completed_with_success"
};

module.exports = FsCompletionStatus;