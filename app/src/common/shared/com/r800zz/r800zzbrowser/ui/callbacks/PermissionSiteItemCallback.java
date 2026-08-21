package com.r800zz.r800zzbrowser.ui.callbacks;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.db.SitePermission;

public interface PermissionSiteItemCallback {
    void onDelete(@NonNull SitePermission item);
}
