package com.r800zz.r800zzbrowser.browser;

import androidx.annotation.NonNull;

public interface VideoAvailabilityListener {
    default void onVideoAvailabilityChanged(@NonNull Media media, boolean aVideoAvailable) {}
}
