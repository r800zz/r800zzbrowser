package com.r800zz.r800zzbrowser.browser.api;

import com.r800zz.r800zzbrowser.browser.api.impl.SessionStateImpl;

/*
 * Interface representing a saved session state.
 */
public interface WSessionState {
    boolean isEmpty();
    String toJson();

    static WSessionState fromJson(String json) {
        return SessionStateImpl.fromJson(json);
    }
}
