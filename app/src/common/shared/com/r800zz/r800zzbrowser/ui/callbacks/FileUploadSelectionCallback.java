package com.r800zz.r800zzbrowser.ui.callbacks;

import android.net.Uri;

import androidx.annotation.NonNull;

public interface FileUploadSelectionCallback {
    void onSelection(@NonNull Uri[] uris);
}
