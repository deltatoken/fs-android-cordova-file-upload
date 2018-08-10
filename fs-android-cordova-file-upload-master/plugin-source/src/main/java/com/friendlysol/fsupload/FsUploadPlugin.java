package com.friendlysol.fsupload;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Patterns;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mklimek on 01.04.16.
 */
public class FsUploadPlugin extends CordovaPlugin
{
    static final String TAG = FsUploadPlugin.class.getSimpleName();

    static volatile FsUploadPlugin self;

    private final AtomicReference<CallbackContext> mGlobalCallback = new AtomicReference<CallbackContext>();

    private final HashMap<String, CallbackContext> mCallbacks = new HashMap<String, CallbackContext>();

    private final Object mLock = new Object();

    private volatile Preferences mPreferences;

    public static Preferences getPluginPreferences(Context context)
    {
        return new Preferences(context.getSharedPreferences("com.friendlysol.fsupload.prefs", Context.MODE_PRIVATE));
    }

    @Override
    protected void pluginInitialize()
    {
        super.pluginInitialize();
        self = this;
    }

    private Preferences getPluginPreferences()
    {
        if (mPreferences == null)
        {
            mPreferences = getPluginPreferences(cordova.getActivity().getApplicationContext());
        }
        return mPreferences;
    }

    @Override
    public boolean execute(String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException
    {
        if ("setUploadCallback".equals(action))
        {
            mGlobalCallback.set(callbackContext);
            PluginResult pres = new PluginResult(PluginResult.Status.OK, "ok");
            pres.setKeepCallback(true);
            callbackContext.sendPluginResult(pres);
            if(Config.DEBUG)
            {
                Log.i(TAG, "Global callback set");
            }
            return true;
        }
        else if ("removeUploadCallback".equals(action))
        {
            mGlobalCallback.set(null);
            callbackContext.success("ok");
            if(Config.DEBUG)
            {
                Log.i(TAG, "Global callback removed");
            }
            return true;
        }
        else if ("startRequest".equals(action))
        {
            startRequest(args, callbackContext);
            return true;
        }
        else if ("getUploads".equals(action))
        {
            final UploadDBQueryParams params = UploadDBQueryParams.validateQueryParams(args, callbackContext);
            if (params != null)
            {
                cordova.getThreadPool().execute(new UploadQueueReadAction(cordova.getActivity(),
                                                                          callbackContext,
                                                                          params));
            }
            return true;
        }
        else if ("deleteUploads".equals(action))
        {
            final UploadDBQueryParams params = UploadDBQueryParams.validateQueryParams(args, callbackContext);
            if (params != null)
            {
                cordova.getThreadPool().execute(new UploadQueueDeleteAction(cordova.getActivity(),
                                                                            callbackContext,
                                                                            params));
            }
            return true;
        }
        else if ("resetUploads".equals(action))
        {
            final UploadDBQueryParams params = UploadDBQueryParams.validateQueryParams(args, callbackContext);
            if (params != null)
            {
                cordova.getThreadPool().execute(new UploadQueueResetAction(cordova.getActivity(),
                                                                           callbackContext,
                                                                           params));
            }
            return true;
        }
        else if ("setConnectTimeout".equals(action))
        {
            final int timeout = args.getInt(0);
            if (timeout > 0)
            {
                getPluginPreferences().setConnectTimeout(timeout);
                callbackContext.success();
            }
            else
            {
                callbackContext.error("timeout must be greater than zero");
            }
            return true;
        }
        else if ("setWifiOnly".equals(action))
        {
          final int wifiOnly = args.getInt(0);
          getPluginPreferences().setWifiOnly(wifiOnly);
          callbackContext.success();
          return true;
        }
        else if ("getWifiOnly".equals(action))
        {
            callbackContext.success(getPluginPreferences().getWifiOnly());
            return true;
        }
        else if ("setUploadVideosOnWifiOnly".equals(action))
        {
          final int wifiOnly = args.getInt(0);
          getPluginPreferences().setUploadVideosOnWifiOnly(wifiOnly);
          callbackContext.success();
          return true;
        }
        else if ("getUploadVideosOnWifiOnly".equals(action))
        {
            callbackContext.success(getPluginPreferences().getUploadVideosOnWifiOnly());
            return true;
        }
        else if ("setSocketTimeout".equals(action))
        {
            final int timeout = args.getInt(0);
            if (timeout > 0)
            {
                getPluginPreferences().setSocketTimeout(timeout);
                callbackContext.success();
            }
            else
            {
                callbackContext.error("timeout must be greater than zero");
            }
            return true;
        }
        else if ("getSocketTimeout".equals(action))
        {
            callbackContext.success(getPluginPreferences().getSocketTimeout());
            return true;
        }
        else if ("getConnectTimeout".equals(action))
        {
            callbackContext.success(getPluginPreferences().getConnectTimeout());
            return true;
        }
        else
        {
            return false;
        }
    }

    private void startRequest(CordovaArgs args, CallbackContext callbackContext) throws JSONException
    {
        final JSONObject jRequest = args.getJSONObject(0);

        final String id;

        if (jRequest.isNull("id") || TextUtils.isEmpty(id = jRequest.getString("id")) || TextUtils.isEmpty(id.trim()))
        {
            callbackContext.error("empty request id is not allowed");
            return;
        }

        final String url;

        if (jRequest.isNull("url") || !Patterns.WEB_URL.matcher(url = jRequest.getString("url")).matches())
        {
            callbackContext.error("invalid url");
            return;
        }

        final String filePath;

        if (jRequest.isNull("filepath") || TextUtils.isEmpty(filePath = jRequest.getString("filepath")) || TextUtils.isEmpty(filePath.trim()))
        {
            callbackContext.error("filepath can not be empty or null");
            return;
        }

        File file;

        try
        {
            final URI javaUri = URI.create(filePath);
            file = new File(javaUri.getPath());
        }
        catch (Exception exc)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, exc);
            }
            callbackContext.error("invalid filepath");
            return;
        }

        if (!file.exists())
        {
            callbackContext.error("file does not exist");
            return;
        }

        if (!file.isFile())
        {
            callbackContext.error("non file path");
            return;
        }

        if (file.length() < 1)
        {
            callbackContext.error("can not upload zero length file");
            return;
        }

        final String fileName;

        if (jRequest.isNull("filename") || TextUtils.isEmpty(fileName = jRequest.getString("filename")) || TextUtils.isEmpty(fileName.trim()))
        {
            callbackContext.error("filename can not be empty");
            return;
        }

        final String mimeType;

        if (jRequest.isNull("mimetype") || TextUtils.isEmpty(mimeType = jRequest.getString("mimetype")) || TextUtils.isEmpty(mimeType.trim()))
        {
            callbackContext.error("mimetype can not be empty");
            return;
        }

        try
        {
            new ContentType(mimeType);
        }
        catch (Exception exc)
        {
            callbackContext.error("invalid mimetype");
            return;
        }

        final String description;
        if (!jRequest.isNull("description"))
        {
            description = jRequest.optString("description");
        }
        else
        {
            description = null;
        }

        final JSONObject data = jRequest.getJSONObject("data");

        try
        {
            validateNameValuePairs(data);
        }
        catch (InvalidNameValuePairException exc)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, exc);
            }
            callbackContext.error("invalid value for key `" + exc.name + "` in data object");
            return;
        }

        final JSONObject headers = jRequest.getJSONObject("headers");
        try
        {
            validateNameValuePairs(headers);
        }
        catch (InvalidNameValuePairException exc)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, exc);
            }
            callbackContext.error("invalid value for key `" + exc.name + "` in headers object");
            return;
        }

        final FileUploadRequest fur = new FileUploadRequest(id,
                                                            url,
                                                            file.getAbsolutePath(),
                                                            filePath,
                                                            fileName,
                                                            mimeType,
                                                            description);
        insertData(data, fur);
        insertHeaders(headers, fur);
        addUploadCallback(fur, callbackContext);
        cordova.getThreadPool().execute(new UploadQueueInsertNewRequestAction(cordova.getActivity(), callbackContext, fur));
    }

    public void executeProgressCallback(FileUploadRequest fur, float fileLength, float bytesUploaded)
    {
        final CallbackContext ctx = getGlobalCallbackContextForRequest();
        if (ctx != null)
        {
            try
            {
                final JSONObject json = fur.toJSON(false, false)
                                           .put("progress", Math.max(0, Math.min(1f, bytesUploaded / fileLength)));
                final PluginResult res = new PluginResult(PluginResult.Status.OK, json);
                res.setKeepCallback(true);
                ctx.sendPluginResult(res);
            }
            catch (Exception exc)
            {
                Log.w(TAG, exc);
            }
        }
    }

    private void addUploadCallback(FileUploadRequest fur, CallbackContext context)
    {
        synchronized (mLock)
        {
            mCallbacks.put(fur.id, context);
        }
    }

    public void callUploadSuccess(FileUploadRequest fur, JSONObject response)
    {
        try
        {
            final JSONObject data = fur.toJSON(false, false)
                                       .put("response", response);
            callCallbacks(fur, PluginResult.Status.OK, data);
        }
        catch (JSONException exc)
        {
            //never occur
        }
    }

    public void callUploadFailure(FileUploadRequest fur, JSONObject error)
    {
        try
        {
            final JSONObject data = fur.toJSON(false, false)
                                       .put("error", error);
            callCallbacks(fur, PluginResult.Status.ERROR, data);
        }
        catch (JSONException exc)
        {
            //never occur
        }

    }

    /**
     * This method retrieves callback context for particular request. The callback is of single-action type,
     * and it is removed after callback
     */
    @Nullable
    private CallbackContext getCallbackContextForRequest(FileUploadRequest fur)
    {
        CallbackContext context;
        synchronized (mLock)
        {
            context = mCallbacks.remove(fur.id);
            if (context != null)
            {
                if (!isValidCallbackContext(context))
                {
                    context = null;
                }
            }
        }
        return context;
    }

    @Nullable
    private CallbackContext getGlobalCallbackContextForRequest()
    {
        CallbackContext context = mGlobalCallback.get();
        if (context != null)
        {
            if (!isValidCallbackContext(context))
            {
                if (Config.DEBUG)
                {
                    Log.w(TAG, "Global progress callback reset");
                }
                mGlobalCallback.compareAndSet(context, null);
                context = null;
            }
        }
        return context;
    }

    private void callCallbacks(FileUploadRequest fur, PluginResult.Status status, JSONObject data)
    {
        final CallbackContext local = getCallbackContextForRequest(fur);
        final CallbackContext global = getGlobalCallbackContextForRequest();
        if (local != null)
        {
            local.sendPluginResult(new PluginResult(status, data));
        }
        if (global != null)
        {
            final PluginResult pres = new PluginResult(status, data);
            pres.setKeepCallback(true);
            global.sendPluginResult(pres);
        }
    }

    private static void insertHeaders(JSONObject json, FileUploadRequest fur) throws JSONException
    {
        final JSONArray names = json.names();
        if (names != null)
        {
            String name;
            for (int i = 0, len = names.length(); i < len; ++i)
            {
                name = names.getString(i);
                fur.addHeader(name, json.getString(name));
            }
        }
    }

    private static void insertData(JSONObject json, FileUploadRequest fur) throws JSONException
    {
        final JSONArray names = json.names();
        if (names != null)
        {
            String name;
            for (int i = 0, len = names.length(); i < len; ++i)
            {
                name = names.getString(i);
                fur.addParam(name, json.getString(name));
            }
        }
    }

    private static void validateNameValuePairs(JSONObject json) throws InvalidNameValuePairException
    {
        final JSONArray names = json.names();
        if (names != null)
        {
            for (int i = 0, len = names.length(); i < len; ++i)
            {
                final String name = names.optString(i); // should never throw
                try
                {
                    final Object value = json.get(name);
                    if (value instanceof JSONArray)
                    {
                        throw new InvalidNameValuePairException(name);
                    }
                    else if (value instanceof JSONObject)
                    {
                        throw new InvalidNameValuePairException(name);
                    }
                    else if (value == JSONObject.NULL)
                    {
                        throw new InvalidNameValuePairException(name);
                    }
                }
                catch (JSONException exc)
                {
                    throw new InvalidNameValuePairException(name);
                }
            }
        }
    }

    public static final class InvalidNameValuePairException extends Exception
    {
        public final String name;

        public InvalidNameValuePairException(String name)
        {
            this.name = name;
        }
    }

    private boolean isValidCallbackContext(final CallbackContext context)
    {
        boolean result = true; // default
        try
        {
            result = LazyLoader.WEB_VIEW_FIELD.get(context) == webView;
        }
        catch (NullPointerException exc)
        {
            // pass
        }
        catch (IllegalAccessException exc2)
        {
            // pass
        }
        return result;
    }

    static final class LazyLoader
    {
        public static final Field WEB_VIEW_FIELD;

        static
        {
            Field field = null;
            try
            {
                field = CallbackContext.class.getDeclaredField("webView");
                field.setAccessible(true);
            }
            catch (Exception exc)
            {
                // ignore it
            }
            WEB_VIEW_FIELD = field;
        }
    }

    static abstract class UploadQueueAction implements Runnable
    {
        protected final Context context;

        protected final CallbackContext callbackContext;

        public UploadQueueAction(Context context, CallbackContext callbackContext)
        {
            this.context = context.getApplicationContext();
            this.callbackContext = callbackContext;
        }

        @Override
        public final void run()
        {
            final UploadQueueDatabase db = UploadQueueDatabase.requestInstance(context);
            try
            {
                run(db);
            }
            catch (Exception exc)
            {
                if (Config.DEBUG)
                {
                    Log.w(TAG, exc);
                }
                onError(exc);
            }
            finally
            {
                db.close();
                postExecute();
            }
        }

        protected void onError(Exception exc)
        {
            callbackContext.error(UploadErrors.getStackTrace(exc));
        }

        protected void postExecute()
        {

        }

        protected abstract void run(UploadQueueDatabase db) throws DBException;
    }

    final class UploadQueueInsertNewRequestAction extends UploadQueueAction
    {
        private final FileUploadRequest mFileUploadRequest;

        public UploadQueueInsertNewRequestAction(Context context, CallbackContext callbackContext, FileUploadRequest fileUploadRequest)
        {
            super(context, callbackContext);
            mFileUploadRequest = fileUploadRequest;
        }

        @Override
        protected void run(UploadQueueDatabase db) throws DBException
        {
            db.insertRequest(mFileUploadRequest);
        }

        @Override
        protected void postExecute()
        {
            context.startService(new Intent(context, FileUploader.class));
        }

        @Override
        protected void onError(Exception exc)
        {
            try
            {
                final JSONObject value = UploadErrors.REQUEST_WRITE_FAILURE
                        .buildUpon()
                        .setPluginStackTrace(exc)
                        .build()
                        .toJSON();
                final JSONObject data = mFileUploadRequest.toJSON(false, false)
                                                          .put("error", value);
                callbackContext.error(data);
            }
            catch (JSONException jexc)
            {
                //never occur
            }
        }
    }

    static final class UploadQueueResetAction extends UploadQueueAction
    {
        private final UploadDBQueryParams mQueryParams;

        public UploadQueueResetAction(Context context, CallbackContext callbackContext, UploadDBQueryParams queryParams)
        {
            super(context, callbackContext);
            mQueryParams = queryParams;
        }

        @Override
        protected void run(UploadQueueDatabase db) throws DBException
        {
            if (Config.DEBUG)
            {
                Log.v(TAG, "ActionReset: %s", mQueryParams);
            }
            callbackContext.success(new JSONArray(db.resetUploads(mQueryParams.first, mQueryParams.second)));
        }

        @Override
        protected void postExecute()
        {
            context.startService(new Intent(context, FileUploader.class));
        }
    }

    static final class UploadQueueDeleteAction extends UploadQueueAction
    {
        private final UploadDBQueryParams mQueryParams;

        public UploadQueueDeleteAction(Context context, CallbackContext callbackContext, UploadDBQueryParams queryParams)
        {
            super(context, callbackContext);
            mQueryParams = queryParams;
        }

        @Override
        protected void run(UploadQueueDatabase db) throws DBException
        {
            if (Config.DEBUG)
            {
                Log.v(TAG, "ActionDelete: %s", mQueryParams);
            }
            callbackContext.success(new JSONArray(db.deleteUploads(mQueryParams.first, mQueryParams.second)));
        }

        @Override
        protected void postExecute()
        {
            context.startService(new Intent(context, FileUploader.class));
        }
    }

    static final class UploadQueueReadAction extends UploadQueueAction
    {
        private final UploadDBQueryParams mQueryParams;

        public UploadQueueReadAction(Context context, CallbackContext callbackContext,
                                     UploadDBQueryParams queryParams)
        {
            super(context, callbackContext);
            mQueryParams = queryParams;
        }

        @Override
        protected void run(UploadQueueDatabase db) throws DBException
        {
            if (Config.DEBUG)
            {
                Log.v(TAG, "ActionRead: %s", mQueryParams);
            }
            JSONArray items = new JSONArray();
            for (QueueItem item : db.queryItems(mQueryParams.first, mQueryParams.second))
            {
                items.put(item.toJSON());
            }
            callbackContext.success(items);
        }
    }

    public static final class Preferences
    {

        private static final String SOCKET_TIMEOUT_MS = "socket_timeout";

        private static final String CONNECT_TIMEOUT_MS = "connect_timeout";

        private static final String MAX_ATTEMPTS_PER_UPLOAD = "max_attempts_per_upload";

        private static final String MIN_TIME_BETWEEN_ATTEMPTS_MS = "min_time_between_attemps";

        private static final String MIN_TIME_BETWEEN_CONNECTION_CHECK_MS = "min_time_between_connection_check";

        private static final String WIFI_ONLY = "wifi_only";

        private static final String UPLOAD_VIDEOS_ON_WIFI_ONLY = "upload_videos_on_wifi_only";

        private final SharedPreferences prefs;

        private Preferences(SharedPreferences prefs)
        {
            this.prefs = prefs;
        }

        public int getSocketTimeout()
        {
            return prefs.getInt(SOCKET_TIMEOUT_MS, 60000);
        }

        public int getConnectTimeout()
        {
            return prefs.getInt(CONNECT_TIMEOUT_MS, 30000);
        }

        public void setWifiOnly(int wifiOnly)
        {
          prefs.edit().putInt(WIFI_ONLY, wifiOnly).apply();
        }

        public int getWifiOnly()
        {
            return prefs.getInt(WIFI_ONLY, 0);
        }

        public void setUploadVideosOnWifiOnly(int wifiOnly)
        {
          prefs.edit().putInt(UPLOAD_VIDEOS_ON_WIFI_ONLY, wifiOnly).apply();
        }

        public int getUploadVideosOnWifiOnly()
        {
            return prefs.getInt(UPLOAD_VIDEOS_ON_WIFI_ONLY, 0);
        }

        public void setConnectTimeout(int timeout)
        {
            if (timeout > 0)
            {
                prefs.edit().putInt(CONNECT_TIMEOUT_MS, timeout).apply();
            }
        }

        public void setSocketTimeout(int timeout)
        {
            if (timeout > 0)
            {
                prefs.edit().putInt(SOCKET_TIMEOUT_MS, timeout).apply();
            }
        }

        public int getMaxAttemptsPerUpload()
        {
            return prefs.getInt(MAX_ATTEMPTS_PER_UPLOAD, 15);
        }

        public void setMaxAttemptsPerUpload(int attempts)
        {
            prefs.edit().putInt(MAX_ATTEMPTS_PER_UPLOAD, attempts).apply();
        }

        public int getMinTimeBetweenAttempts()
        {
            return prefs.getInt(MIN_TIME_BETWEEN_ATTEMPTS_MS, 15000);
        }

        public void setMinTimeBetweenAttempts(int period)
        {
            prefs.edit().putInt(MIN_TIME_BETWEEN_ATTEMPTS_MS, period).apply();
        }

        public int getMinTimeBetweenConnectionCheck()
        {
            return prefs.getInt(MIN_TIME_BETWEEN_CONNECTION_CHECK_MS, 30000);
        }

        public void setMinTimeBetweenConnectionCheck(int period)
        {
            prefs.edit().putInt(MIN_TIME_BETWEEN_CONNECTION_CHECK_MS, period).apply();
        }
    }

    static final class UploadDBQueryParams extends Pair<List<String>, UploadQueueDatabase.CompletionStatus>
    {
        private UploadDBQueryParams(List<String> first, UploadQueueDatabase.CompletionStatus second)
        {
            super(first, second);
        }

        @Override
        public String toString()
        {
            return String.format(Locale.US, "%s, with ids: %s", second, first != null
                                                                        ? Arrays.toString(first.toArray())
                                                                        : null);
        }

        public static UploadDBQueryParams validateQueryParams(CordovaArgs args, CallbackContext callbackContext) throws JSONException
        {
            final Object idsObject = args.opt(0);
            final ArrayList<String> ids;
            if (idsObject == null || idsObject == JSONObject.NULL)
            {
                ids = null;
            }
            else if (idsObject instanceof JSONArray)
            {
                final JSONArray array = (JSONArray) idsObject;
                final int len = array.length();
                ids = new ArrayList<String>(len);
                for (int i = 0; i < len; ++i)
                {
                    ids.add(array.getString(i));
                }
            }
            else
            {
                callbackContext.error("ids not an array");
                return null;
            }

            final String completionStatusName = args.getString(1);
            final UploadQueueDatabase.CompletionStatus completionStatus;

            if (TextUtils.isEmpty(completionStatusName))
            {
                callbackContext.error("invalid completionStatus: empty string");
                return null;
            }

            try
            {
                completionStatus = UploadQueueDatabase.CompletionStatus.valueOf(completionStatusName.toUpperCase());
            }
            catch (IllegalArgumentException exc)
            {
                callbackContext.error("invalid completionStatus: " + completionStatusName);
                return null;
            }
            return new UploadDBQueryParams(ids, completionStatus);
        }
    }
}
