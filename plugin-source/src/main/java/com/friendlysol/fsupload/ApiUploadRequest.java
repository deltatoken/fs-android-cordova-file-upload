package com.friendlysol.fsupload;

import android.support.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

class ApiUploadRequest implements Runnable
{
    static final int CHUNK_SIZE_BYTES = 16 * 1024;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String TAG = ApiUploadRequest.class.getSimpleName();

    private final List<NameValuePair> mVariables;

    private final List<NameValuePair> mHeaders;

    private final StreamRequestListener mStreamRequestListener;

    private final InputStream mStream;

    private final String mMimeType;

    private final String mFileName;

    private final String mUrl;

    private static final String HTTP_LINE_END = "\r\n";

    private static final String HTTP_BOUNDARY = UUID.randomUUID().toString();

    private static final String HTTP_TWO_DASHES = "--";

    private static final String HTTP_BOUNDARY_MARK = HTTP_TWO_DASHES + HTTP_BOUNDARY + HTTP_LINE_END;

    private static final String HTTP_DATA_END = HTTP_TWO_DASHES + HTTP_BOUNDARY + HTTP_TWO_DASHES + HTTP_LINE_END;

    private static final byte[] DATA_SUFFIX = (HTTP_LINE_END + HTTP_DATA_END).getBytes(UTF8);

    private final int mStreamLength;

    private final int mConnectTimeout;

    private final int mSocketTimeout;

    ApiUploadRequest(StreamRequestListener streamRequestListener,
                     String url,
                     List<NameValuePair> vars,
                     List<NameValuePair> headers,
                     InputStream stream,
                     int streamLength,
                     String fileName,
                     String mimeType,
                     int connectTimeout,
                     int socketTimeout)
    {
        mUrl = url;
        mVariables = vars;
        mStreamRequestListener = streamRequestListener;
        mStream = stream;
        mMimeType = mimeType;
        mFileName = fileName;
        mStreamLength = streamLength;
        mHeaders = headers;
        mConnectTimeout = connectTimeout;
        mSocketTimeout = socketTimeout;
    }

    @Override
    public void run()
    {
        HttpURLConnection conn = null;
        try
        {
            if (Config.DEBUG)
            {
                Log.d(TAG, "Filename: %s", mFileName);
            }

            // generate prefix
            final ByteArrayOutputStreamEx prefix;
            try
            {
                prefix = createPrefixContent();
            }
            catch (IOException e)
            {
                callListenerError(UploadErrors.INVALID_REQUEST_PARAMS
                                          .buildUpon()
                                          .setPluginStackTrace(e)
                                          .build());
                return;
            }

            final int contentLength = prefix.size() + mStreamLength + DATA_SUFFIX.length;

            if (Config.DEBUG)
            {
                Log.v(TAG, "Content length: %d", contentLength);
            }

            int chunkSize = CHUNK_SIZE_BYTES;

            try
            {
                chunkSize = mStreamRequestListener.onStreamStarted(contentLength);
                if (chunkSize <= 0)
                {
                    chunkSize = CHUNK_SIZE_BYTES;
                }
            }
            catch (RuntimeException e)
            {
                if (Config.DEBUG)
                {
                    Log.w(TAG, e);
                }
            }

            if (Config.DEBUG)
            {
                Log.v(TAG, "Prepare connection trasport");
            }

            conn = (HttpURLConnection) (new URL(mUrl)).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(mConnectTimeout);
            conn.setReadTimeout(mSocketTimeout);
            conn.setFixedLengthStreamingMode(contentLength);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + HTTP_BOUNDARY);
            if (Config.DEBUG)
            {
                Log.v(TAG, "Add request headers");
            }
            for (NameValuePair header : mHeaders)
            {
                conn.setRequestProperty(header.getName(), header.getValue());
            }
            sendRequestPayload(conn, prefix, chunkSize);
            if (Config.DEBUG)
            {
                Log.v(TAG, "Reading response");
            }
            int respCode;
            try
            {
                respCode = conn.getResponseCode();
            }
            catch (EOFException e)
            {
                respCode = conn.getResponseCode();
            }

            if (Config.DEBUG)
            {
                Log.v(TAG, "Response code: " + respCode);
            }

            if (respCode == -1)
            {
                callListenerError(UploadErrors.UPLOAD_IO_FAILURE
                                          .buildUpon()
                                          .setHttpStatus(respCode)
                                          .build());
                return;
            }

            if (respCode >= HttpURLConnection.HTTP_OK && respCode < HttpURLConnection.HTTP_MULT_CHOICE)
            {
                processValidResponse(conn, respCode);
            }
            else
            {
                processErrorResponse(conn, respCode);
            }
        }
        catch (Exception exc)
        {
            processException(exc);
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
            if (Config.DEBUG)
            {
                Log.v(TAG, "Disconnected");
            }
        }
    }

    private void processException(Exception exc)
    {
        if (exc instanceof MalformedURLException)
        {
            callListenerError(UploadErrors.MALFORMED_URL
                                      .buildUpon()
                                      .setPluginStackTrace(exc)
                                      .build());
        }
        else if(exc instanceof IOException)
        {
            callListenerError(UploadErrors.UPLOAD_IO_FAILURE
                                      .buildUpon()
                                      .setPluginStackTrace(exc)
                                      .build());
        }
        else
        {
            callListenerError(UploadErrors.PLUGIN_ERROR
                                      .buildUpon()
                                      .setPluginStackTrace(exc)
                                      .build());
        }
    }

    private void processErrorResponse(HttpURLConnection conn, int respCode)
    {
        final UploadError.Builder builder = UploadErrors.RESPONSE_ERROR
                .buildUpon()
                .setHttpStatus(respCode);
        final InputStream errorStream = conn.getErrorStream();
        if (errorStream != null)
        {
            try
            {
                final String errorText = streamToString(errorStream);
                Log.w(TAG, errorText);
                callListenerError(builder.setHttpResponse(errorText)
                                         .build());
            }
            catch (IOException e)
            {
                if (Config.DEBUG)
                {
                    Log.w(TAG, e);
                }
                callListenerError(builder.setPluginStackTrace(e).build());
            }
            finally
            {
                IOHelper.close(errorStream);
            }
        }
        else
        {
            callListenerError(builder.build());
        }
    }

    private void processValidResponse(HttpURLConnection conn, int respCode)
    {
        InputStream is = null;
        try
        {
            is = conn.getInputStream();
            final String rawResponse = streamToString(is);
            if (Config.DEBUG)
            {
                Log.d(TAG, "Response content:\n" + rawResponse);
            }
            callListenerCompleted(respCode, rawResponse);
        }
        catch (IOException exc)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, exc);
            }
            callListenerError(UploadErrors.RESPONSE_READ_FAILURE
                                      .buildUpon()
                                      .setHttpStatus(respCode)
                                      .setPluginStackTrace(exc)
                                      .build());
        }
        finally
        {
            IOHelper.close(is);
        }
    }

    @NonNull
    private ByteArrayOutputStreamEx createPrefixContent() throws IOException
    {
        Log.v(TAG, "Prepare content variables");
        final ByteArrayOutputStreamEx prefix = new ByteArrayOutputStreamEx(1024);
        {
            DataOutputStream vars = new DataOutputStream(prefix);
            for (NameValuePair pair : mVariables)
            {
                if (!pair.getName().equals("type")) {
                  final byte[] contentBytes = pair.getValue().getBytes("utf-8");
                  vars.writeBytes(HTTP_BOUNDARY_MARK);
                  vars.writeBytes(String.format("Content-Disposition: form-data; name=\"%s\"%s", pair.getName(), HTTP_LINE_END));
                  vars.writeBytes(String.format("Content-Type: text/plain; charset=utf-8%s", HTTP_LINE_END));
                  vars.writeBytes(String.format("Content-Length: %d%s", contentBytes.length, HTTP_LINE_END));
                  vars.writeBytes(String.format("Content-Transfer-Encoding: binary%s", HTTP_LINE_END));
                  vars.writeBytes(HTTP_LINE_END);
                  vars.write(contentBytes);
                  vars.writeBytes(HTTP_LINE_END);
                }
            }
            vars.writeBytes(HTTP_BOUNDARY_MARK);
            vars.writeBytes(String.format("Content-Disposition: form-data; name=\"file\"; filename=\"%s\"%s", mFileName, HTTP_LINE_END));
            vars.writeBytes(String.format("Content-Type: %s%s", mMimeType, HTTP_LINE_END));
            vars.writeBytes(String.format("Content-Length: %d%s", mStreamLength, HTTP_LINE_END));
            vars.writeBytes(String.format("Content-Transfer-Encoding: binary%s", HTTP_LINE_END));
            vars.writeBytes(HTTP_LINE_END);
        }
        if (Config.DEBUG)
        {
            Log.v(TAG, "Variables prepared.");
        }
        return prefix;
    }

    private void sendRequestPayload(HttpURLConnection connection, ByteArrayOutputStreamEx prefix, int chunkSize) throws IOException
    {
        if (Config.DEBUG)
        {
            Log.v(TAG, "Opening stream");
        }
        OutputStream output = new BufferedOutputStream(connection.getOutputStream(), chunkSize * 2);
        try
        {
            if (Config.DEBUG)
            {
                Log.v(TAG, "Writing prefix: %d bytes", prefix.size());
            }
            sendChunks(prefix.openAsInputStream(), output, chunkSize);
            if (Config.DEBUG)
            {
                Log.v(TAG, "Writing stream: %d bytes", mStreamLength);
            }
            sendChunks(mStream, output, chunkSize);
            if (Config.DEBUG)
            {
                Log.v(TAG, "Writing suffix: %d bytes", DATA_SUFFIX.length);
            }
            sendChunks(DATA_SUFFIX, output, chunkSize);
            if (Config.DEBUG)
            {
                Log.v(TAG, "Flushing");
            }
            output.flush();
            if (Config.DEBUG)
            {
                Log.v(TAG, "Write completed");
            }
        }
        finally
        {
            IOHelper.close(output);
        }
    }

    private void callListenerCompleted(int respCode, @NonNull String rawResponse)
    {
        try
        {
            mStreamRequestListener.onStreamCompleted(respCode, rawResponse);
        }
        catch (Exception e)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, e);
            }
        }
    }

    private void callListenerError(UploadError error)
    {
        try
        {
            mStreamRequestListener.onError(error);
        }
        catch (Exception e2)
        {
            if (Config.DEBUG)
            {
                Log.w(TAG, e2);
            }
        }
    }

    private void sendChunks(InputStream is, OutputStream os, int chunkSize) throws IOException
    {
        final byte[] chunk = new byte[chunkSize];
        int numRead;
        while ((numRead = is.read(chunk)) != -1)
        {
            os.write(chunk, 0, numRead);
            try
            {
                mStreamRequestListener.onChunkProcessed(chunk, numRead);
            }
            catch (Exception e)
            {
                if (Config.DEBUG)
                {
                    Log.w(TAG, e);
                }
            }
        }
    }

    private void sendChunks(byte[] array, OutputStream os, int chunkSize) throws IOException
    {
        sendChunks(new ByteArrayInputStream(array), os, chunkSize);
    }

    static String streamToString(final InputStream stream) throws IOException
    {
        if(stream == null)
        {
            throw new IOException("stream can not be null");
        }
        final char[] chunk = new char[4 * 1024];
        final StringBuilder sb = new StringBuilder(16 * 1024);
        final InputStreamReader reader = new InputStreamReader(stream, Charset.defaultCharset());
        int numRead;
        while ((numRead = reader.read(chunk)) != -1)
        {
            sb.append(chunk, 0, numRead);
        }
        return sb.toString();
    }

    private static final class ByteArrayOutputStreamEx extends ByteArrayOutputStream
    {
        public ByteArrayOutputStreamEx(int size)
        {
            super(size);
        }

        public InputStream openAsInputStream()
        {
            return new ByteArrayInputStream(buf, 0, count);
        }
    }

}
