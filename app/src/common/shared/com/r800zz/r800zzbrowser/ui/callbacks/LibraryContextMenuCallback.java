package com.r800zz.r800zzbrowser.ui.callbacks;

import com.r800zz.r800zzbrowser.ui.widgets.menus.library.LibraryContextMenuWidget;

public interface LibraryContextMenuCallback {
    void onOpenInNewWindowClick(LibraryContextMenuWidget.LibraryContextMenuItem item);
    void onOpenInNewTabClick(LibraryContextMenuWidget.LibraryContextMenuItem item);
}
