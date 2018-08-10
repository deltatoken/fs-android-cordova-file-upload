package com.friendlysol.fsupload;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mklimek on 09.04.16.
 */
class UploadError
{
    private final boolean fatal;

    private final String errorType;

    private final Integer httpStatus;

    private final String httpResponse;

    private final String pluginStackTrace;

    private final String humanDescription;

    public UploadError(boolean fatal, String errorType, Integer httpStatus, String httpResponse, String pluginStackTrace, String humanDescription)
    {
        this.fatal = fatal;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
        this.httpResponse = httpResponse;
        this.pluginStackTrace = pluginStackTrace;
        this.humanDescription = humanDescription;
    }

    public boolean isFatal()
    {
        return fatal;
    }

    public String getErrorType()
    {
        return errorType;
    }

    public Integer getHttpStatus()
    {
        return httpStatus;
    }

    public String getHttpResponse()
    {
        return httpResponse;
    }

    public String getPluginStackTrace()
    {
        return pluginStackTrace;
    }

    public String getHumanDescription()
    {
        return humanDescription;
    }

    public JSONObject toJSON()
    {
        try
        {
            return new JSONObject()
                    .put("fatal", fatal)
                    .put("type", errorType)
                    .put("status", httpStatus != null
                                       ? httpStatus
                                       : JSONObject.NULL)
                    .put("response", httpResponse != null
                                         ? httpResponse
                                         : JSONObject.NULL)
                    .put("stacktrace", pluginStackTrace != null
                                             ? pluginStackTrace
                                             : JSONObject.NULL)
                    .put("description", humanDescription);
        }
        catch (JSONException exc)
        {
            throw new IllegalStateException(exc); // never thrown actually
        }
    }

    public Builder buildUpon()
    {
        return new Builder(this);
    }

    public static final class Builder
    {
        private boolean fatal;

        private String errorType;

        private Integer httpStatus;

        private String httpResponse;

        private String pluginStackTrace;

        private String humanDescription;

        private Builder(UploadError parent)
        {
            fatal = parent.isFatal();
            errorType = parent.getErrorType();
            httpStatus = parent.getHttpStatus();
            httpResponse = parent.getHttpResponse();
            pluginStackTrace = parent.getPluginStackTrace();
            humanDescription = parent.getHumanDescription();
        }

        public Builder setFatal(boolean fatal)
        {
            this.fatal = fatal;
            return this;
        }

        public Builder setErrorType(String errorType)
        {
            this.errorType = errorType;
            return this;
        }

        public Builder setHttpStatus(Integer httpStatus)
        {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder setHttpResponse(String httpResponse)
        {
            this.httpResponse = httpResponse;
            return this;
        }

        public Builder setPluginStackTrace(String pluginStackTrace)
        {
            this.pluginStackTrace = pluginStackTrace;
            return this;
        }

        public Builder setPluginStackTrace(Throwable throwable)
        {
            this.pluginStackTrace = UploadErrors.getStackTrace(throwable);
            return this;
        }

        public Builder setHumanDescription(String humanDescription)
        {
            this.humanDescription = humanDescription;
            return this;
        }

        public UploadError build()
        {
            return new UploadError(fatal,
                                   errorType,
                                   httpStatus,
                                   httpResponse,
                                   pluginStackTrace,
                                   humanDescription);
        }
    }
}
