/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.r800zz.r800zzbrowser.ui.widgets.settings;

import static com.r800zz.r800zzbrowser.db.SitePermission.SITE_PERMISSION_TRACKING;

import android.content.Context;

import com.r800zz.r800zzbrowser.browser.content.TrackingProtectionStore;
import com.r800zz.r800zzbrowser.browser.engine.SessionStore;
import com.r800zz.r800zzbrowser.ui.widgets.WidgetManagerDelegate;

class TrackingPermissionsOptionsView extends SitePermissionsOptionsView {

    private TrackingProtectionStore mTrackingProtectionStore;

    public TrackingPermissionsOptionsView(Context aContext, WidgetManagerDelegate aWidgetManager) {
        super(aContext, aWidgetManager, SITE_PERMISSION_TRACKING);

        mTrackingProtectionStore = SessionStore.get().getTrackingProtectionStore();
    }

    protected void initialize(Context aContext) {
        mCallback = item -> mTrackingProtectionStore.remove(item);

        super.initialize(aContext);
    }

    @Override
    protected boolean reset() {
        mTrackingProtectionStore.removeAll();
        return true;
    }
}
