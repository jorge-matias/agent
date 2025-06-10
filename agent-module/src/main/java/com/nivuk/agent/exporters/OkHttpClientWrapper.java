package com.nivuk.agent.exporters;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OkHttpClientWrapper implements HttpClient {
    private final OkHttpClient client;

    public OkHttpClientWrapper(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public Call newCall(Request request) {
        return client.newCall(request);
    }
}
