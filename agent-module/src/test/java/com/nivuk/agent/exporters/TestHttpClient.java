package com.nivuk.agent.exporters;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.Timeout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestHttpClient implements HttpClient {
    private final List<Request> requests = new ArrayList<>();
    private Response nextResponse;
    private IOException nextError;

    @Override
    public Call newCall(Request request) {
        requests.add(request);
        return new Call() {
            private final Timeout timeout = new Timeout();

            @Override
            public Request request() {
                return request;
            }

            @Override
            public Response execute() throws IOException {
                if (nextError != null) {
                    throw nextError;
                }
                return nextResponse;
            }

            @Override
            public void enqueue(Callback callback) {
                try {
                    Response response = execute();
                    callback.onResponse(this, response);
                } catch (IOException e) {
                    callback.onFailure(this, e);
                }
            }

            @Override
            public Timeout timeout() {
                return timeout;
            }

            // Other Call interface methods - not used in our tests
            @Override public void cancel() {}
            @Override public boolean isCanceled() { return false; }
            @Override public boolean isExecuted() { return false; }
            @Override public Call clone() { return this; }
        };
    }

    public List<Request> getRequests() {
        return requests;
    }

    public void setNextResponse(Response response) {
        this.nextResponse = response;
        this.nextError = null;
    }

    public void setNextError(IOException error) {
        this.nextError = error;
        this.nextResponse = null;
    }
}
