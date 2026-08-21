package com.r800zz.r800zzbrowser.ui.callbacks;

import com.r800zz.r800zzbrowser.ui.widgets.menus.library.HistoryContextMenuWidget;

public interface HistoryContextMenuCallback extends LibraryContextMenuCallback {
    void onAddToBookmarks(HistoryContextMenuWidget.LibraryContextMenuItem item);
    void onRemoveFromBookmarks(HistoryContextMenuWidget.LibraryContextMenuItem item);
}
