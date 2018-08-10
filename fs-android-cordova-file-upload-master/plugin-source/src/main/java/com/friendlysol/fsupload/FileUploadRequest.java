package com.friendlysol.fsupload;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * POJO of file upload request
 *
 * @author mklimek
 */
public class FileUploadRequest implements Parcelable
{

    /**
     * Creator for {@link Parcelable} interface
     */
    public static final Creator<FileUploadRequest> CREATOR = new Creator<FileUploadRequest>()
    {

        @Override
        public FileUploadRequest[] newArray(int size)
        {
            return new FileUploadRequest[size];
        }

        @Override
        public FileUploadRequest createFromParcel(Parcel source)
        {
            return new FileUploadRequest(source);
        }
    };

    /**
     * Uploaded filepath
     */
    public final String filepath;

    /**
     * Url-style file path (from javascript)
     */
    private final String filepathUrl;

    /**
     * Filename of content file
     */
    public final String filename;

    public final String mimetype;

    /**
     * Uploaded file description
     */
    private final String description;

    /**
     * API action for upload (method of upload)
     */
    public final String url;

    /**
     * Unique identifier for uploaded file
     */
    public final String id;

    private final ArrayList<NameValuePair> mExtraParams = new ArrayList<NameValuePair>();

    private final ArrayList<NameValuePair> mExtraHeaders = new ArrayList<NameValuePair>();

    public FileUploadRequest(String id, String url, String filepath, String filePathUrl, String filename, String mimetype, String description)
    {
        this.id = id;
        this.url = url;
        this.filepath = filepath;
        this.filepathUrl = filePathUrl;
        this.filename = filename;
        this.description = description;
        this.mimetype = mimetype;
    }

    private FileUploadRequest(Parcel source)
    {
        id = source.readString();
        url = source.readString();
        filepath = source.readString();
        filepathUrl = source.readString();
        filename = source.readString();
        mimetype = source.readString();
        if (source.readInt() == 1)
        {
            description = source.readString();
        }
        else
        {
            description = null;
        }
        readList(source, mExtraParams);
        readList(source, mExtraHeaders);
    }

    public FileUploadRequest(DataInput source) throws IOException
    {
        id = source.readUTF();
        url = source.readUTF();
        filepath = source.readUTF();
        filepathUrl = source.readUTF();
        filename = source.readUTF();
        mimetype = source.readUTF();
        if (source.readBoolean())
        {
            description = source.readUTF();
        }
        else
        {
            description = null;
        }
        readList(source, mExtraParams);
        readList(source, mExtraHeaders);
    }

    public FileUploadRequest(byte[] array) throws IOException
    {
        this(new DataInputStream(new ByteArrayInputStream(array)));
    }

    public void addParam(String name, String value)
    {
        mExtraParams.add(new BasicNameValuePair(name, value));
    }

    public void addParam(String name, int value)
    {
        addParam(name, String.valueOf(value));
    }

    public void addHeader(String name, String value)
    {
        mExtraHeaders.add(new BasicNameValuePair(name, value));
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    /**
     * Checks if provided data is valid (it does not check file existance however)
     */
    public boolean isValid()
    {
        return !TextUtils.isEmpty(url) && !TextUtils.isEmpty(filepath);
    }

    /**
     * Checks if provided file is of type video
     */
    public boolean isVideo(){
      final ArrayList<NameValuePair> params = getParams();
      for (NameValuePair pair : params) {
        if (pair.getName().equals("type") && pair.getValue().equals("video")) {
          return true;
        }
      }
      return false;
    }

    public String getDescriptionSafe()
    {
        return description != null
               ? description
               : filename;
    }

    public File getFile()
    {
        return new File(filepath);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(id);
        dest.writeString(url);
        dest.writeString(filepath);
        dest.writeString(filepathUrl);
        dest.writeString(filename);
        dest.writeString(mimetype);
        dest.writeInt(description != null
                      ? 1
                      : 0);
        if (description != null)
        {
            dest.writeString(description);
        }
        writeList(dest, mExtraParams);
        writeList(dest, mExtraHeaders);
    }

    public void writeToDataOutput(DataOutput dest) throws IOException
    {
        dest.writeUTF(id);
        dest.writeUTF(url);
        dest.writeUTF(filepath);
        dest.writeUTF(filepathUrl);
        dest.writeUTF(filename);
        dest.writeUTF(mimetype);
        dest.writeBoolean(description != null);
        if (description != null)
        {
            dest.writeUTF(description);
        }
        writeList(dest, mExtraParams);
        writeList(dest, mExtraHeaders);
    }

    private static void readList(DataInput source, ArrayList<NameValuePair> list) throws IOException
    {
        final int size = source.readInt();
        if (size > 0)
        {
            for (int i = 0; i < size; ++i)
            {
                list.add(new BasicNameValuePair(source.readUTF(), source.readUTF()));
            }
        }
    }

    private static void readList(Parcel source, ArrayList<NameValuePair> list)
    {
        final int size = source.readInt();
        if (size > 0)
        {
            for (int i = 0; i < size; ++i)
            {
                list.add(new BasicNameValuePair(source.readString(), source.readString()));
            }
        }
    }

    private static void writeList(DataOutput dest, ArrayList<NameValuePair> list) throws IOException
    {
        final int size = list.size();
        dest.writeInt(size);
        if (size > 0)
        {
            NameValuePair pair;
            for (int i = 0; i < size; ++i)
            {
                pair = list.get(i);
                dest.writeUTF(pair.getName());
                dest.writeUTF(pair.getValue());
            }
        }
    }

    private static void writeList(Parcel dest, ArrayList<NameValuePair> list)
    {
        final int size = list.size();
        dest.writeInt(size);
        if (size > 0)
        {
            NameValuePair pair;
            for (int i = 0; i < size; ++i)
            {
                pair = list.get(i);
                dest.writeString(pair.getName());
                dest.writeString(pair.getValue());
            }
        }
    }

    public JSONObject toJSON(boolean includeData, boolean includeHeaders)
    {
        final JSONObject obj = new JSONObject();
        try
        {
            obj.put("id", id);
            obj.put("url", url);
            obj.put("filepath", filepathUrl); //js side needs url
            obj.put("filename", filename);
            obj.put("mimetype", mimetype);
            obj.put("description", description != null
                                   ? description
                                   : JSONObject.NULL);
            if (includeData)
            {
                storeNameValuePairs(obj, mExtraParams, "data");
            }
            if (includeHeaders)
            {
                storeNameValuePairs(obj, mExtraHeaders, "headers");
            }
            return obj;
        }
        catch (JSONException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static void storeNameValuePairs(JSONObject obj, ArrayList<NameValuePair> list, String fieldName) throws JSONException
    {
        final int size = list.size();
        if (size > 0)
        {
            JSONObject pairs = new JSONObject();
            NameValuePair pair;
            for (int i = 0; i < size; ++i)
            {
                pair = list.get(i);
                pairs.put(pair.getName(), pair.getValue());
            }
            obj.put(fieldName, pairs);
        }
    }

    @Override
    public String toString()
    {
        return toJSON(true, true).toString();
    }

    public byte[] toByteArray()
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        final DataOutputStream dos = new DataOutputStream(baos);
        try
        {
            writeToDataOutput(dos);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        return baos.toByteArray();
    }

    /**
     * Returns copy of extra parameters
     */
    public ArrayList<NameValuePair> getParams()
    {
        return new ArrayList<NameValuePair>(mExtraParams);
    }

    public ArrayList<NameValuePair> getHeaders()
    {
        return new ArrayList<NameValuePair>(mExtraHeaders);
    }
}
