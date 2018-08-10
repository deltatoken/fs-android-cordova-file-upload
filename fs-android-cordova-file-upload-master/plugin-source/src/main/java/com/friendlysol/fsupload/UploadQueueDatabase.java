package com.friendlysol.fsupload;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class UploadQueueDatabase extends SQLiteOpenHelper
{
    private static final String UPLOAD_QUEUE_DATABASE = "UploadQueueDatabase";

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE = "upload_queue";

    private static final String C_ID = "id";

    private static final String C_ATTEMPTS = "attempts";

    private static final String C_LAST_ATTEMPT = "last_attempt";

    private static final String C_UPLOAD_REQUEST = "upload_request";

    private static final String C_LAST_ERROR = "last_error";

    private static final String C_COMPLETED = "completion_status";

    private static final String C_SUCCESS_RESULT = "success_result";

    private static final String C_CREATED = "created_at";

    private static final String[] Q_COLS = { C_ID, C_CREATED, C_LAST_ATTEMPT, C_ATTEMPTS, C_LAST_ERROR, C_UPLOAD_REQUEST, C_COMPLETED, C_SUCCESS_RESULT };

    private static final String ATTEMPTS_NUMBER_PLACEHOLDER = "%max_attempts%";

    private static final String WHERE_ATTEMPTS_EXCEED = '(' + C_ATTEMPTS + " >= " + ATTEMPTS_NUMBER_PLACEHOLDER + ')';

    private static final String WHERE_COMPLETED = '(' + C_COMPLETED + " = 1)";

    private static final String WHERE_NOT_COMPLETED = '(' + C_COMPLETED + " = 0)";

    private static final String WHERE_COMPLETED_WITH_SUCCESS = WHERE_COMPLETED + " AND (" + C_LAST_ERROR + " IS NULL)";

    private static final String WHERE_COMPLETED_WITH_ERROR = WHERE_COMPLETED + " AND (" + C_LAST_ERROR + " IS NOT NULL)";

    private static final String WHERE_COMPLETED_WITH_ERROR_AND_ATTEMPTS_EXCEED = WHERE_COMPLETED_WITH_ERROR + " AND " + WHERE_ATTEMPTS_EXCEED;

    private static final String SQL_CREATE = "CREATE TABLE " + TABLE + //
                                             '(' + C_ID + " TEXT UNIQUE NOT NULL, " + //
                                             C_ATTEMPTS + " INTEGER DEFAULT 0, " + //
                                             C_CREATED + " INTEGER NOT NULL, " + //
                                             C_LAST_ATTEMPT + " INTEGER DEFAULT 0, " + //
                                             C_LAST_ERROR + " TEXT, " + //
                                             C_COMPLETED + " INTEGER DEFAULT 0, " + //
                                             C_SUCCESS_RESULT + " TEXT, " + //
                                             C_UPLOAD_REQUEST + " BLOB NOT NULL)";

    private static final String WHERE_ID = '(' + C_ID + "=?)";

    private static final String SQL_MARK_ATTEMPT = "UPDATE " + TABLE + //
                                                   " SET " + C_ATTEMPTS + "=" + C_ATTEMPTS + " + 1" + ", " + C_LAST_ATTEMPT + "= ?" + //
                                                   " WHERE " + C_ID + "= ?";

    private static final String ID_IN_PLACEHOLDER = "%placeholder%";

    private static final String WHERE_ID_IN = '(' + C_ID + " IN (" + ID_IN_PLACEHOLDER + "))";

    private static final String WHERE_ID_IN_AND_COMPLETED = WHERE_ID_IN + " AND " + WHERE_COMPLETED;

    private static final String WHERE_ID_IN_AND_NOT_COMPLETED = WHERE_ID_IN + " AND " + WHERE_NOT_COMPLETED;

    private static final String WHERE_ID_IN_AND_COMPLETED_WITH_SUCCESS = WHERE_ID_IN + " AND " + WHERE_COMPLETED_WITH_SUCCESS;

    private static final String WHERE_ID_IN_AND_COMPLETED_WITH_ERROR = WHERE_ID_IN + " AND " + WHERE_COMPLETED_WITH_ERROR;

    private static final String WHERE_ID_IN_AND_COMPLETED_WITH_ERROR_AND_ATTEMPTS_EXCEEDED = WHERE_ID_IN_AND_COMPLETED_WITH_ERROR + " AND " + WHERE_ATTEMPTS_EXCEED;


    private static String[] where(String id)
    {
        return new String[]{ id };
    }

    private static UploadQueueDatabase instance;

    private static int referenceCounter;


    public static synchronized UploadQueueDatabase requestInstance(Context context)
    {
        if (instance == null)
        {
            instance = new UploadQueueDatabase(context.getApplicationContext());
        }
        referenceCounter++;
        return instance;
    }

    @Override
    public void close()
    {
        synchronized (UploadQueueDatabase.class)
        {
            if (referenceCounter > 0)
            {
                referenceCounter--;
                if (referenceCounter == 0)
                {
                    super.close();
                    instance = null;
                }
            }
        }
    }

    private final Context mContext;

    private UploadQueueDatabase(Context context)
    {
        super(context, UPLOAD_QUEUE_DATABASE, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }

    public void insertRequest(FileUploadRequest request) throws DBException
    {
        try
        {
            final ContentValues cv = new ContentValues();
            cv.put(C_ID, request.id);
            cv.put(C_CREATED, System.currentTimeMillis());
            cv.put(C_UPLOAD_REQUEST, request.toByteArray());
            getWritableDatabase().insertOrThrow(TABLE, null, cv);
        }
        catch (RuntimeException e)
        {
            throw new DBException(e);
        }
    }

    public void markNextAttempt(String id) throws DBException
    {
        try
        {
            getWritableDatabase().execSQL(SQL_MARK_ATTEMPT, new Object[]{ System.currentTimeMillis(), id });
        }
        catch (RuntimeException e)
        {
            throw new DBException(e);
        }
    }

    public void markError(String id, JSONObject error, boolean isFatal) throws DBException
    {
        try
        {
            ContentValues cv = new ContentValues();
            cv.put(C_LAST_ERROR, error.toString());
            if (isFatal)
            {
                cv.put(C_COMPLETED, 1);
            }
            getWritableDatabase().update(TABLE, cv, WHERE_ID, where(id));
        }
        catch (RuntimeException e)
        {
            throw new DBException(e);
        }
    }

    public void markSuccess(String id, JSONObject responseContent) throws DBException
    {
        try
        {
            ContentValues cv = new ContentValues();
            cv.put(C_COMPLETED, 1);
            cv.putNull(C_LAST_ERROR);
            cv.put(C_SUCCESS_RESULT, responseContent.toString());
            getWritableDatabase().update(TABLE, cv, WHERE_ID, where(id));
        }
        catch (RuntimeException e)
        {
            throw new DBException(e);
        }
    }

    /**
     * Deletes uploads in transaction, and returns deleted items
     */
    public List<String> deleteUploads(List<String> ids, CompletionStatus completionStatus) throws DBException
    {

        try
        {
            final SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try
            {
                final List<String> selectedIds = queryItemsIds(ids, completionStatus);
                final QuerySelection selection = createQuerySelectionForIds(mContext, selectedIds);
                final int rows = db.delete(TABLE, selection.first, selection.second);
                db.setTransactionSuccessful();
                return selectedIds;
            }
            finally
            {
                db.endTransaction();
            }
        }
        catch (RuntimeException e)
        {
            throw new DBException(e);
        }
    }

    public List<String> resetUploads(List<String> ids, CompletionStatus completionStatus) throws DBException
    {
        try
        {
            final SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try
            {
                final List<String> selectedIds = queryItemsIds(ids, completionStatus);
                final QuerySelection selection = createQuerySelectionForIds(mContext, selectedIds);
                final ContentValues cv = new ContentValues();
                cv.put(C_ATTEMPTS, 0);
                cv.put(C_LAST_ATTEMPT, 0);
                cv.put(C_COMPLETED, 0);
                cv.putNull(C_SUCCESS_RESULT);
                cv.putNull(C_LAST_ERROR);
                db.update(TABLE, cv, selection.first, selection.second);
                db.setTransactionSuccessful();
                return selectedIds;
            }
            finally
            {
                db.endTransaction();
            }
        }
        catch (RuntimeException e)
        {
            throw new DBException(e);
        }
    }

    private ArrayList<String> queryItemsIds(List<String> ids, CompletionStatus completionStatus) throws DBException
    {
        return extractIds(queryItems(ids, completionStatus));
    }

    public ArrayList<QueueItem> queryItems(List<String> ids,
                                           CompletionStatus completionStatus) throws DBException
    {
        return queryItems(completionStatus.getSelectionAndArgs(mContext, ids));
    }

    private ArrayList<QueueItem> queryItems(QuerySelection params) throws DBException
    {
        return queryItems(params.first, params.second);
    }

    private ArrayList<QueueItem> queryItems(String selection, String[] selectionArgs) throws DBException
    {
        Cursor cursor = null;
        try
        {
            cursor = getWritableDatabase().query(TABLE, Q_COLS, selection, selectionArgs, null, null, C_CREATED, null);
            final ArrayList<QueueItem> list = new ArrayList<QueueItem>();
            while (cursor.moveToNext())
            {
                list.add(new QueueItem(cursor));
            }
            return list;
        }
        catch (Exception e)
        {
            throw new DBException(e);
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
    }

    @NonNull
    private static QuerySelection createQuerySelectionForIds(@NonNull Context context,
                                                             @NonNull List<String> ids)
    {
        return CompletionStatus.ALL.getSelectionAndArgs(context, ids);
    }

    @NonNull
    private static ArrayList<String> extractIds(@NonNull List<QueueItem> items)
    {
        final ArrayList<String> ids = new ArrayList<String>(items.size());
        for (QueueItem item : items)
        {
            ids.add(item.id);
        }
        return ids;
    }

    public enum CompletionStatus
    {
        ALL(WHERE_ID_IN, null, false),
        NOT_COMPLETED(WHERE_ID_IN_AND_NOT_COMPLETED, WHERE_NOT_COMPLETED, false),
        COMPLETED(WHERE_ID_IN_AND_COMPLETED, WHERE_COMPLETED, false),
        COMPLETED_WITH_ERROR(WHERE_ID_IN_AND_COMPLETED_WITH_ERROR, WHERE_COMPLETED_WITH_ERROR, false),
        COMPLETED_WITH_ERROR_ATTEMPTS_EXCEED(WHERE_ID_IN_AND_COMPLETED_WITH_ERROR_AND_ATTEMPTS_EXCEEDED, WHERE_COMPLETED_WITH_ERROR_AND_ATTEMPTS_EXCEED, true),
        COMPLETED_WITH_SUCCESS(WHERE_ID_IN_AND_COMPLETED_WITH_SUCCESS, WHERE_COMPLETED_WITH_SUCCESS, false),;

        private final String mQueryWithIds;

        private final String mQuery;

        private final boolean mIncludeAttemptMax;

        CompletionStatus(String queryWithIds, String query, boolean includeAttemptMax)
        {
            mQueryWithIds = queryWithIds;
            mQuery = query;
            mIncludeAttemptMax = includeAttemptMax;
        }

        private static String getQueryForMaxAttempts(Context context, String query, boolean include)
        {
            if (include)
            {
                return query.replace(ATTEMPTS_NUMBER_PLACEHOLDER,
                                     String.valueOf(FsUploadPlugin.getPluginPreferences(context)
                                                                  .getMaxAttemptsPerUpload()));
            }
            else
            {
                return query;
            }
        }

        public QuerySelection getSelectionAndArgs(Context context, List<String> ids)
        {
            if (ids == null)
            {
                return new QuerySelection(getQueryForMaxAttempts(context,
                                                                 mQuery,
                                                                 mIncludeAttemptMax),
                                          null);
            }
            else if (ids.isEmpty())
            {
                return new QuerySelection(getQueryForMaxAttempts(context,
                                                                 mQueryWithIds.replace(ID_IN_PLACEHOLDER, ""),
                                                                 mIncludeAttemptMax),
                                          null);
            }
            else
            {
                final int length = ids.size();
                final StringBuilder sb = new StringBuilder();
                final String[] args = new String[length];
                for (int i = 0; i < length; ++i)
                {
                    args[i] = ids.get(i);
                    if (i > 0)
                    {
                        sb.append(',');
                    }
                    sb.append('?');
                }
                return new QuerySelection(getQueryForMaxAttempts(context,
                                                                 mQueryWithIds.replace(ID_IN_PLACEHOLDER, sb.toString()),
                                                                 mIncludeAttemptMax), args);
            }
        }
    }

    static final class QuerySelection extends Pair<String, String[]>
    {
        public QuerySelection(String first, String[] second)
        {
            super(first, second);
        }
    }
}