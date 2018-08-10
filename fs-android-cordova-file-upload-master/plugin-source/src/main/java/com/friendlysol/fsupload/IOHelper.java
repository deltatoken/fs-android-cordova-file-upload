package com.friendlysol.fsupload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * IO Helper
 *
 * @author mklimek
 */
public class IOHelper
{
    /**
     * Safe close handler
     *
     * @param closeable
     */
    public static void close(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (IOException e)
            {

            }
        }
    }

    public static long copyStream(InputStream istream, OutputStream ostream, int bufferLength) throws IOException
    {
        long totalBytes = 0;
        int numRead;
        final byte[] buffer = new byte[bufferLength];
        while ((numRead = istream.read(buffer)) != -1)
        {
            totalBytes += numRead;
            ostream.write(buffer, 0, numRead);
        }
        return totalBytes;
    }

    public static long copyFile(File src, File dest) throws IOException
    {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        final int bufferLength = 256 * 1024;
        final int copyBufferLength = bufferLength / 4;
        try
        {
            bis = new BufferedInputStream(new FileInputStream(src), bufferLength);
            bos = new BufferedOutputStream(new FileOutputStream(dest), bufferLength);
            return copyStream(bis, bos, copyBufferLength);
        }
        finally
        {
            close(bis);
            close(bos);
        }
    }
}
