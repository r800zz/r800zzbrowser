package com.r800zz.r800zzbrowser.ui.callbacks;

import android.view.View;

import com.r800zz.r800zzbrowser.ui.adapters.SystemNotification;

public interface SystemNotificationItemCallback {
    void onClick(View view, SystemNotification item);
    void onDelete(View view, SystemNotification item);
}
