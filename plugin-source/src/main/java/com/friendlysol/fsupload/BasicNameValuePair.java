package com.friendlysol.fsupload;

/**
 * Created by mklimek on 22.03.16.
 */
final class BasicNameValuePair implements Cloneable, NameValuePair
{
    private final String mName;

    private final String mValue;

    public BasicNameValuePair(String name, String value)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Name may not be null");
        }
        mName = name;
        mValue = value;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getValue()
    {
        return mValue;
    }

    @Override
    protected BasicNameValuePair clone()
    {
        try
        {
            return (BasicNameValuePair) super.clone();
        }
        catch (CloneNotSupportedException exc)
        {
            throw new IllegalStateException(exc);
        }

    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof BasicNameValuePair))
        {
            return false;
        }

        final BasicNameValuePair that = (BasicNameValuePair) o;

        if (mName != null
            ? !mName.equals(that.mName)
            : that.mName != null)
        {
            return false;
        }
        return !(mValue != null
                 ? !mValue.equals(that.mValue)
                 : that.mValue != null);

    }

    @Override
    public int hashCode()
    {
        int result = mName != null
                     ? mName.hashCode()
                     : 0;
        result = 31 * result + (mValue != null
                                ? mValue.hashCode()
                                : 0);
        return result;
    }

    @Override
    public String toString()
    {
        if (this.mValue == null)
        {
            return mName;
        }
        else
        {
            final StringBuilder buffer = new StringBuilder(mName.length() + 1 + mValue.length());
            buffer.append(this.mName);
            buffer.append('=');
            buffer.append(this.mValue);
            return buffer.toString();
        }
    }
}
