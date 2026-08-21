package com.r800zz.r800zzbrowser.browser.api.impl;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.browser.api.WPanZoomController;

import org.mozilla.geckoview.GeckoSession;

class PanZoomControllerImpl implements WPanZoomController {
    GeckoSession mSession;

    public PanZoomControllerImpl(GeckoSession session) {
        mSession = session;
    }

    @Override
    public void onTouchEvent(@NonNull MotionEvent event) {
        mSession.getPanZoomController().onTouchEvent(event);
    }

    @Override
    public void onMotionEvent(@NonNull MotionEvent event) {
        mSession.getPanZoomController().onMotionEvent(event);
    }
}
