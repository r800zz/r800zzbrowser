package com.r800zz.r800zzbrowser.ui.callbacks;

import android.view.View;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.ui.adapters.FileUploadItem;

public interface FileUploadItemCallback {
    void onClick(@NonNull View view, @NonNull FileUploadItem item);
}
