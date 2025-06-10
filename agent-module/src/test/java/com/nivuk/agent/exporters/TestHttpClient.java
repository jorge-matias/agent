package com.nivuk.agent.exporters;

import okhttp3.*;
import okio.Buffer;
import okio.Timeout;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestHttpClient implements HttpClient {
    private final List<RequestWrapper> requests = new ArrayList<>();
    private Response nextResponse;
    private IOException nextError;

    @Override
    @NotNull
    public Call newCall(@NotNull Request request) {
        RequestWrapper wrapper = new RequestWrapper(request);
        requests.add(wrapper);
        return new Call() {
            private final Timeout timeout = new Timeout();

            @Override
            @NotNull
            public Request request() {
                return wrapper.getOriginalRequest();
            }

            @Override
            @NotNull
            public Response execute() throws IOException {
                if (nextError != null) {
                    throw nextError;
                }
                if (nextResponse == null) {
                    throw new IOException("No response configured");
                }
                return nextResponse;
            }

            @Override
            public void enqueue(@NotNull Callback callback) {
                try {
                    Response response = execute();
                    callback.onResponse(this, response);
                } catch (IOException e) {
                    callback.onFailure(this, e);
                }
            }

            @Override
            public void cancel() {}

            @Override
            public boolean isExecuted() {
                return false;
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            @NotNull
            public Timeout timeout() {
                return timeout;
            }

            @Override
            @NotNull
            public Call clone() {
                return this;
            }
        };
    }

    public void setNextResponse(Response response) {
        this.nextResponse = response;
    }

    public void setNextError(IOException error) {
        this.nextError = error;
    }

    public List<RequestWrapper> getRequests() {
        return requests;
    }

    public static class RequestWrapper {
        private final Request original;
        private final String bodyContent;

        RequestWrapper(Request original) {
            this.original = original;
            this.bodyContent = readBodyContent(original);
        }

        private String readBodyContent(Request request) {
            try {
                if (request.body() == null) return "";
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                return buffer.readUtf8();
            } catch (IOException e) {
                return "Error reading body: " + e.getMessage();
            }
        }

        @Override
        public String toString() {
            return bodyContent;
        }

        public Request getOriginalRequest() {
            return original;
        }

        public HttpUrl url() { return original.url(); }
        public String method() { return original.method(); }
        public Headers headers() { return original.headers(); }
        public RequestBody body() { return original.body(); }
        public Request.Builder newBuilder() { return original.newBuilder(); }
        public CacheControl cacheControl() { return original.cacheControl(); }
    }
}
