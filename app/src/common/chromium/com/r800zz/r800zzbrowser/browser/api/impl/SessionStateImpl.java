package com.r800zz.r800zzbrowser.browser.api.impl;

import com.r800zz.r800zzbrowser.browser.api.WSessionState;

public class SessionStateImpl implements WSessionState {

    public SessionStateImpl() {}

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toJson() {
        return "{}";
    }

    public static SessionStateImpl fromJson(String json) {
        // TODO
        return new SessionStateImpl();
    }
}