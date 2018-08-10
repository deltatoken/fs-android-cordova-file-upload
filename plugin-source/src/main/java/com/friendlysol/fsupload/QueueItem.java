package com.friendlysol.fsupload;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Represents queued file upload request
 *
 * @author mklimek
 */
public final class QueueItem implements Comparable<QueueItem>
{
    /**
     * Record identifier
     */
    public final String id;
    /**
     * UTC timestamp of request creation
     */
    public final long createdAt;

    /**
     * UTC timestamp of last attempt or zero if no attempts were made
     */
    public final long lastAttempt;

    /**
     * Number of attempts that took place
     */
    public final int attempts;

    /**
     * Last error (null if none)
     */
    public final String lastError;

    /**
     * Completion response (if success)
     */
    public final String completionResponse;

    /**
     * Queued upload request object with request data
     */
    public final FileUploadRequest request;

    /**
     * Tells if queued item upload is completed - either because of success or final failure
     */
    public final boolean isCompleted;

    public boolean isFinalSuccess()
    {
        return isCompleted && TextUtils.isEmpty(lastError);
    }

    public boolean isAttemptsExceeded(Context context)
    {
        return attempts >= FsUploadPlugin.getPluginPreferences(context).getMaxAttemptsPerUpload();
    }

    QueueItem(Cursor cursor) throws IOException
    {
        id = cursor.getString(0);
        createdAt = cursor.getLong(1);
        lastAttempt = cursor.getLong(2);
        attempts = cursor.getInt(3);
        if (cursor.isNull(4))
        {
            lastError = null;
        }
        else
        {
            lastError = cursor.getString(4);
        }
        request = new FileUploadRequest(cursor.getBlob(5));
        isCompleted = cursor.getInt(6) == 1;
        if (cursor.isNull(7))
        {
            completionResponse = null;
        }
        else
        {
            completionResponse = cursor.getString(7);
        }
    }

    @Override
    public int compareTo(QueueItem another)
    {
        if (lastAttempt > another.lastAttempt)
        {
            return -1;
        }
        else if (another.lastAttempt > lastAttempt)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    /**
     * Converts item to json. The json content is based on FileUploadRequest.toJSON()
     * @return
     */
    public JSONObject toJSON()
    {
        try
        {

            return request.toJSON(false, false)
                          .put("created", createdAt)
                          .put("lastAttempt", lastAttempt)
                          .put("attempts", attempts)
                          .put("completed", isCompleted)
                          .put("success", isFinalSuccess())
                          .put("error", !TextUtils.isEmpty(lastError)
                                        ? new JSONObject(lastError)
                                        : JSONObject.NULL)
                          .put("response", !TextUtils.isEmpty(completionResponse)
                                           ? new JSONObject(completionResponse)
                                           : JSONObject.NULL);

        }
        catch (JSONException exc)
        {
            throw new IllegalStateException(exc); // will never be thrown
        }
    }


}
