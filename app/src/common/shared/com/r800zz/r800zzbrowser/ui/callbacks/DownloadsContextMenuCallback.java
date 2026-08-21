package com.r800zz.r800zzbrowser.ui.callbacks;

import com.r800zz.r800zzbrowser.ui.widgets.menus.library.DownloadsContextMenuWidget;

public interface DownloadsContextMenuCallback extends LibraryContextMenuCallback {
    void onDelete(DownloadsContextMenuWidget.DownloadsContextMenuItem item);
}
