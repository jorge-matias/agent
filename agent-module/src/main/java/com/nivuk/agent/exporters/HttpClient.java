package com.nivuk.agent.exporters;

import okhttp3.Call;
import okhttp3.Request;

public interface HttpClient {
    Call newCall(Request request);
}
