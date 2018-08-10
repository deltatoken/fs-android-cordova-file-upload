package com.friendlysol.fsupload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;

interface StreamRequestListener
{
    int DEFAULT_CHUNK_BYTES = ApiUploadRequest.CHUNK_SIZE_BYTES;

    /**
     * Called when whole chunk has been processed
     *
     * @param chunk chunk array
     * @param count bytes valid in chunk - may be chunk length or less
     * @return true if want to break processing, onErroc will be called
     */
    boolean onChunkProcessed(@NonNull byte[] chunk, int count);

    /**
     * Called when operation is completed
     *
     * @param response optional response when uploading adta
     */
    void onStreamCompleted(int httpStatusCode, @NonNull String response);

    /**
     * Called on successful request
     *
     * @param length receved stream length - if negative - unknown
     * @return desired chunk size in bytes - must be between 0 and 1024KB
     */
    int onStreamStarted(long length);

    void onError(@NonNull UploadError error);
}
