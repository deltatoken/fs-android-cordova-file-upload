package com.friendlysol.fsupload;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class FileUploader extends IntentService
{
    /**
     * Broadcast receiver wakelock
     */
    protected static WakeLock CPU_WAKELOCK;

    private static final class RemoveNotificationEvent implements Runnable
    {

        private final int id;

        private final Context appContext;

        public RemoveNotificationEvent(Context context, int id)
        {
            this.id = id;
            this.appContext = context.getApplicationContext();
        }

        @Override
        public void run()
        {
            try
            {
                ((NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
            }
            catch (Exception e)
            {
                // pass
            }
        }

    }

    /**
     * Broadcast receiver woken up by {@link AlarmManager}
     *
     * @author mklimek
     */
    public static final class AlarmReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Config.DEBUG)
            {
                Log.d(TAG, "AlarmReceiver.onReceive(%s)", intent.getAction());
            }
            if (CPU_WAKELOCK == null)
            {
                CPU_WAKELOCK = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                CPU_WAKELOCK.setReferenceCounted(false);
            }
            CPU_WAKELOCK.acquire();
            context.startService(new Intent(context, FileUploader.class));
        }

    }

    protected static class MD5Result
    {
        public final int streamLength;

        public final byte[] md5;

        public MD5Result(int streamLength, byte[] md5)
        {
            this.streamLength = streamLength;
            this.md5 = md5;
        }

    }

    static class UploadResult
    {
        public final JSONObject json;

        public UploadResult(int httpStatus, String response)
        {
            JSONObject jsonResponse = null;
            try
            {
                jsonResponse = new JSONObject(response);
            }
            catch (JSONException exc)
            {
                // skip - will threat response as raw string instead of json
            }
            try
            {
                json = new JSONObject().put("status", httpStatus);
                if (jsonResponse != null)
                {
                    json.put("data", jsonResponse);
                }
                else
                {
                    json.put("data", response);
                }
            }
            catch (JSONException exc)
            {
                throw new IllegalStateException(exc); // never thrown actually
            }
        }
    }

    private final static String TAG = FileUploader.class.getSimpleName();

    public FileUploader()
    {
        super(TAG);
    }

    private UploadQueueDatabase mDB;

    private WakeLock mWakeLock;

    private Handler mHandler;

    private FsUploadPlugin.Preferences mPreferences;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mHandler = new Handler();
        mWakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        mDB = UploadQueueDatabase.requestInstance(this);
        mPreferences = FsUploadPlugin.getPluginPreferences(this);
        if (Config.DEBUG)
        {
            Log.i(TAG, "onCreate()");
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mDB.close();
        mWakeLock.release();
        if (Config.DEBUG)
        {
            Log.i(TAG, "onDestroy()");
        }
    }

    private static final Random RANDOM = new Random();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        final WakeLock wl = CPU_WAKELOCK;
        if (wl != null)
        {
            wl.release();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        int passCounter = 1;
        while (processQueue())
        {
            if (Config.DEBUG)
            {
                Log.d(TAG, "Pass %d completed.", passCounter++);
            }
        }
    }

    private boolean processQueue()
    {
        if (Config.DEBUG)
        {
            Log.d(TAG, "processQueue()");
        }
        try
        {
            final List<QueueItem> items = mDB.queryItems(null, UploadQueueDatabase.CompletionStatus.NOT_COMPLETED);
            int size = items.size();
            if (Config.DEBUG)
            {
                Log.v(TAG, "Not completed items: %d", size);
            }
            if (size > 0)
            {
                final long now = System.currentTimeMillis();
                boolean maxAttemptsExceed = true;
                for (Iterator<QueueItem> iter = items.iterator(); iter.hasNext(); )
                {
                    final QueueItem item = iter.next();
                    final boolean maxAttemptsExceedCheck = item.attempts > mPreferences.getMaxAttemptsPerUpload();
                    if (maxAttemptsExceedCheck || ((now - item.lastAttempt) < mPreferences.getMinTimeBetweenAttempts()))
                    {
                        if (maxAttemptsExceedCheck)
                        {
                            final JSONObject json = UploadErrors.MAX_ATTEMPTS_REACHED.toJSON();
                            try
                            {
                                mDB.markError(item.request.id, json, true);
                            }
                            finally
                            {
                                final FsUploadPlugin pl = FsUploadPlugin.self;
                                if (pl != null)
                                {
                                    pl.callUploadFailure(item.request, json);
                                }
                            }
                        }
                        iter.remove();
                        if (Config.DEBUG)
                        {
                            Log.v(TAG, "%s not passed.", item.request.id);
                        }
                    }
                    if (maxAttemptsExceed)
                    {
                        maxAttemptsExceed = maxAttemptsExceedCheck;
                    }
                }
                size = items.size();
                if (Config.DEBUG)
                {
                    Log.v(TAG, "Passed items: %d, attemptsExceeded: %b", size, maxAttemptsExceed);
                }
                if (size > 0)
                {
                    final boolean networkAvailable = isNetworkAvailable();
                    final boolean isWifiActive = isActiveNetworkAWifiNetwork();
                    final boolean uploadVideosOnlyOnWifi = isVideoUploadRestrictedToWifi();

                    if (Config.DEBUG)
                    {
                        Log.v(TAG, "Network available: %b", networkAvailable);
                        Log.v(TAG, "Video uploads wifi only: %b", uploadVideosOnlyOnWifi);
                        Log.v(TAG, "Wifi active: %b", isWifiActive);
                    }
                    if (networkAvailable)
                    {
                        final QueueItem item = items.get(RANDOM.nextInt(size));

                        Log.v(TAG, "Is video: %b", item.request.isVideo());

                        if (item.request.isVideo()) {
                          if (uploadVideosOnlyOnWifi && isWifiActive) {
                            processUpload(item.request);
                          } else if (!uploadVideosOnlyOnWifi) {
                            processUpload(item.request);
                          }
                        } else {
                          processUpload(item.request);
                        }
                        return true;
                    }
                    else
                    {
                        scheduleFor(mPreferences.getMinTimeBetweenConnectionCheck());
                    }
                }
                else if (!maxAttemptsExceed)
                {
                    scheduleFor(mPreferences.getMinTimeBetweenAttempts());
                }
            }
        }
        catch (DBException e)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, e);
            }
        }
        return false;
    }

    private void scheduleFor(long timeMs)
    {
        scheduleFor(this, timeMs);
    }

    public static void scheduleFor(Context context, long timeMs)
    {
        if (Config.DEBUG)
        {
            Log.v(TAG, "scheduleFor(%dms)", timeMs);
        }
        final AlarmManager man = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        final PendingIntent pintent = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            man.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeMs, pintent);
        }
        else
        {
            man.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeMs, pintent);
        }

    }

    private boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null) && activeNetworkInfo.isConnected();
    }

    private boolean isActiveNetworkAWifiNetwork(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null) && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private boolean isUploadRestrictedToWifi(){
      final int wifiOnlyUploads = mPreferences.getWifiOnly();
      return wifiOnlyUploads > 0;
    }

    private boolean isVideoUploadRestrictedToWifi(){
      final int videoWifiOnlyUploads = mPreferences.getUploadVideosOnWifiOnly();
      return videoWifiOnlyUploads > 0;
    }

    private void processUpload(final FileUploadRequest request)
    {
        try
        {
            mDB.markNextAttempt(request.id);
        }
        catch (DBException ex)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, ex);
            }
        }
        if (Config.DEBUG)
        {
            Log.i(TAG, "Upload: %s", request.id);
            Log.v(TAG, "Data: %s", request);
        }
        try
        {
            if (request.isValid())
            {
                final File uFile = request.getFile();
                BufferedInputStream bis = null;
                try
                {
                    if (Config.DEBUG)
                    {
                        Log.v(TAG, "Calculating file MD5");
                    }
                    final MD5Result md5 = calculateMD5(uFile);
                    if (Config.DEBUG)
                    {
                        Log.v(TAG, "MD5 Caculated.");
                        Log.v(TAG, "File size: %d, MD5: %s", md5.streamLength, toHexString(md5.md5));
                    }
                    //request.addParam("md5", toHexString(md5.md5));
                    bis = new BufferedInputStream(new FileInputStream(uFile), 256 * 1024);
                    final RequestStreamListener listener = new RequestStreamListener(request);
                    if (Config.DEBUG)
                    {
                        Log.v(TAG, "Streaming request");
                    }
                    notifyUploading(listener.ntfId, request.getDescriptionSafe(), 0, 0);
                    new ApiUploadRequest(listener,
                                         request.url,
                                         request.getParams(),
                                         request.getHeaders(),
                                         bis,
                                         md5.streamLength,
                                         request.filename,
                                         request.mimetype,
                                         mPreferences.getConnectTimeout(),
                                         mPreferences.getSocketTimeout()).run();
                    if (listener.success)
                    {
                        notifyUploadFinished(request.id.hashCode(), request.getDescriptionSafe(), true);
                        final UploadResult uploadResult = new UploadResult(listener.successHttpStatus,
                                                                           listener.successResponse);
                        try
                        {
                            mDB.markSuccess(request.id, uploadResult.json);
                            if (Config.DEBUG)
                            {
                                Log.i(TAG, "Marked as completed");
                            }
                        }
                        catch (DBException e)
                        {
                            if (Config.DEBUG)
                            {
                                Log.w(TAG, e);
                            }
                        }
                        final FsUploadPlugin pl = FsUploadPlugin.self;
                        if (pl != null)
                        {
                            pl.callUploadSuccess(request, uploadResult.json);
                        }
                        if (Config.DEBUG)
                        {
                            Log.v(TAG, "Streaming completed");
                        }
                    }
                    else
                    {
                        markError(request, listener.uploadError);
                    }
                }
                finally
                {
                    IOHelper.close(bis);
                }
            }
            else
            {
                if (Config.DEBUG)
                {
                    Log.w(TAG, "Invalid request");
                }
                markError(request, UploadErrors.INVALID_REQUEST);
            }
        }
        catch (FileNotFoundException fnex)
        {
            markError(request, UploadErrors.FILE_NOT_FOUND
                    .buildUpon()
                    .setPluginStackTrace(fnex)
                    .build());
        }
        catch (MD5CalculationException md5exc)
        {
            markError(request, UploadErrors.FILE_READ_FAILURE
                    .buildUpon()
                    .setPluginStackTrace(md5exc)
                    .build());
        }
        catch (RuntimeException e)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, e);
            }
            markError(request, UploadErrors.PLUGIN_ERROR
                    .buildUpon()
                    .setPluginStackTrace(e)
                    .build());
        }
    }

    private void markError(FileUploadRequest request, UploadError error)
    {
        final JSONObject json = error.toJSON();
        final boolean fatal = error.isFatal();
        if (Config.DEBUG)
        {
            Log.w(TAG, "markError(%s, fatal: %b)", json, fatal);
        }
        notifyUploadFinished(request.id.hashCode(), request.getDescriptionSafe(), false);
        try
        {
            mDB.markError(request.id, json, fatal);
        }
        catch (DBException e)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, e);
            }
        }
        if (fatal)
        {
            final FsUploadPlugin ps = FsUploadPlugin.self;
            if (ps != null)
            {
                ps.callUploadFailure(request, json);
            }
        }
    }

    private void notifyUploading(int id, String description, int max, int progress)
    {
        final Notification ntf = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setTicker("Starting upload of " + description)
                .setWhen(System.currentTimeMillis())
                .setProgress(max, progress, max == 0 || max == progress)
                .setContentTitle(progress == max
                                 ? "Waiting for response:"
                                 : "Uploading:")
                .setContentText(description)
                .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(), 0))
                .build();
        ntf.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(id, ntf);
    }

    private void notifyUploadFinished(int id, String description, boolean success)
    {
        final String statusMessage = success
                                     ? "Upload completed"
                                     : "Upload failed";
        final Notification ntf = new NotificationCompat.Builder(this)
                .setSmallIcon(success
                              ? android.R.drawable.stat_sys_upload_done
                              : android.R.drawable.stat_notify_error)
                .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(), 0))
                .setContentTitle(statusMessage)
                .setContentText(success
                                ? description
                                : description + ": the file will be reuploaded soon.")

                .build();

        ntf.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        ntf.defaults = Notification.DEFAULT_ALL;
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(id, ntf);
        if (success)
        {
            mHandler.postDelayed(new RemoveNotificationEvent(this, id), 15000);
        }
    }

    public static String toHexString(byte[] bytes)
    {
        char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] hexChars = new char[bytes.length << 1];
        int v;
        for (int j = 0, mul = 0; j < bytes.length; j++, mul = j << 1)
        {
            v = bytes[j] & 0xFF;
            hexChars[mul] = hexArray[v >>> 4];
            hexChars[mul + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static MD5Result calculateMD5(File file) throws MD5CalculationException
    {
        DigestInputStream dis = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            final int bufferSize = 256 * 1024;
            dis = new DigestInputStream(new BufferedInputStream(new FileInputStream(file), bufferSize), md);
            final byte[] buffer = new byte[bufferSize / 4];
            int totalBytes = 0;
            int numRead;
            while ((numRead = dis.read(buffer)) != -1)
            {
                totalBytes += numRead;
            }
            return new MD5Result(totalBytes, md.digest());
        }
        catch (NoSuchAlgorithmException exc)
        {
            throw new MD5CalculationException(exc.toString());
        }
        catch (FileNotFoundException fnex)
        {
            throw new MD5CalculationException(fnex.toString());
        }
        catch (IOException ioexc)
        {
            throw new MD5CalculationException(ioexc.toString());
        }
        finally
        {
            IOHelper.close(dis);
        }
    }

    static final class MD5CalculationException extends IOException
    {
        public MD5CalculationException(String detailMessage)
        {
            super(detailMessage);
        }
    }

    private final class RequestStreamListener implements StreamRequestListener
    {
        private final FileUploadRequest request;

        final int ntfId;

        int fileLength;

        int progress;

        boolean success;

        int successHttpStatus;

        String successResponse;

        UploadError uploadError;

        public RequestStreamListener(FileUploadRequest request)
        {
            this.request = request;
            ntfId = request.id.hashCode();
        }

        @Override
        public int onStreamStarted(long length)
        {
            fileLength = (int) length;
            notifyUploading(ntfId, request.getDescriptionSafe(), fileLength, 0);
            final FsUploadPlugin plugin = FsUploadPlugin.self;
            if (plugin != null)
            {
                plugin.executeProgressCallback(request, fileLength, progress);
            }
            return DEFAULT_CHUNK_BYTES * 2;
        }

        @Override
        public void onStreamCompleted(int httpStatusCode, @NonNull String response)
        {
            success = true;
            successResponse = response;
            successHttpStatus = httpStatusCode;
        }

        @Override
        public void onError(@NonNull UploadError error)
        {
            success = false;
            uploadError = error;
        }

        @Override
        public boolean onChunkProcessed(@NonNull byte[] chunk, int count)
        {
            if (Config.DEBUG)
            {
                Log.v(TAG, "Chunk processed: %d bytes", count);
            }
            progress += count;
            notifyUploading(ntfId, request.getDescriptionSafe(), fileLength, progress);
            final FsUploadPlugin plugin = FsUploadPlugin.self;
            if (plugin != null)
            {
                plugin.executeProgressCallback(request, fileLength, progress);
            }
            return false;
        }
    }
}
