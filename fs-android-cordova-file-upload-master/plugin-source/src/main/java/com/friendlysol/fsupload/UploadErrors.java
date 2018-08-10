package com.friendlysol.fsupload;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created by mklimek on 09.04.16.
 */
class UploadErrors
{
    public static final UploadError MALFORMED_URL = new UploadError(true, "malformed_url", null, null, null, "Request's URL is malformed");

    public static final UploadError REQUEST_WRITE_FAILURE = new UploadError(true, "request_write_failure", null, null, null, "Request could not be written into database");

    public static final UploadError MAX_ATTEMPTS_REACHED = new UploadError(true, "max_attempts_reached", null, null, null, "Max number of upload attemps reached");

    public static final UploadError FILE_NOT_FOUND = new UploadError(true, "file_not_found", null, null, null, "The file to upload was not found");

    public static final UploadError FILE_READ_FAILURE = new UploadError(true, "file_read_failure", null, null, null, "File read failure");

    public static final UploadError INVALID_REQUEST = new UploadError(true, "invalid_request", null, null, null, "Invalid request: url or file is empty");

    public static final UploadError INVALID_REQUEST_PARAMS = new UploadError(true, "invalid_request_params", null, null, null, "Request have invalid form field parameters");

    public static final UploadError UPLOAD_IO_FAILURE = new UploadError(false, "upload_io_failure", null, null, null, "Upload IO transport failure. Automatic retry applied");

    public static final UploadError RESPONSE_READ_FAILURE = new UploadError(true, "response_read_failure", null, null, null, "Response status code was OK, but response could to be read to the end");

    public static final UploadError RESPONSE_ERROR = new UploadError(true, "response_error", null, null, null, "Respose status code is not OK");

    public static final UploadError PLUGIN_ERROR = new UploadError(true, "internal_plugin_error", null, null, null, "Internal plugin error. Contact with tech support.");

    public static String getStackTrace(Throwable exc)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream stream = new PrintStream(baos);
            exc.printStackTrace(stream);
            stream.flush();
            return baos.toString();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
