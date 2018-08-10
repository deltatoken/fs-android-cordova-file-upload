package com.friendlysol.fsupload;

/**
 * @author Michał
 */
final class Log
{
    private Log()
    {

    }


    public static void i(final String tag, final String format, Object... args)
    {
        final String message = String.format(format, args);
        android.util.Log.i(tag, message);
    }

    public static void v(final String tag, final String format, Object... args)
    {
        final String message = String.format(format, args);
        android.util.Log.v(tag, message);
    }

    public static void d(final String tag, final String format, Object... args)
    {
        final String message = String.format(format, args);
        android.util.Log.d(tag, message);
    }

    public static void i(final String tag, final String message)
    {
        android.util.Log.i(tag, message);
    }

    public static void w(final String tag, final Throwable exc)
    {
        android.util.Log.w(tag, exc);
    }

    public static void w(final String tag, final String format, Object... args)
    {
        final String message = String.format(format, args);
        android.util.Log.w(tag, message);
    }

    public static void w(final String tag, final String message)
    {
        android.util.Log.w(tag, message);
    }

    public static void d(final String tag, final String message)
    {
        android.util.Log.d(tag, message);
    }

    public static void v(final String tag, final String message)
    {
        android.util.Log.v(tag, message);
    }

    public static void e(final String tag, final String message)
    {
        android.util.Log.e(tag, message);
    }

    public static void e(final String tag, final String format, Object... args)
    {
        final String message = String.format(format, args);
        android.util.Log.e(tag, message);
    }
}
