package com.r800zz.r800zzbrowser.ui.widgets.menus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.r800zz.r800zzbrowser.BuildConfig;
import com.r800zz.r800zzbrowser.R;
import com.r800zz.r800zzbrowser.VRBrowserActivity;
import com.r800zz.r800zzbrowser.browser.SettingsStore;
import com.r800zz.r800zzbrowser.browser.adapter.ComponentsAdapter;
import com.r800zz.r800zzbrowser.browser.api.WSessionSettings;
import com.r800zz.r800zzbrowser.browser.engine.Session;
import com.r800zz.r800zzbrowser.browser.engine.SessionStore;
import com.r800zz.r800zzbrowser.databinding.HamburgerMenuBinding;
import com.r800zz.r800zzbrowser.ui.adapters.HamburgerMenuAdapter;
import com.r800zz.r800zzbrowser.ui.widgets.UIWidget;
import com.r800zz.r800zzbrowser.ui.widgets.WidgetManagerDelegate;
import com.r800zz.r800zzbrowser.ui.widgets.WidgetPlacement;
import com.r800zz.r800zzbrowser.ui.widgets.Windows;
import com.r800zz.r800zzbrowser.utils.AnimationHelper;
import com.r800zz.r800zzbrowser.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import mozilla.components.browser.state.state.BrowserState;
import mozilla.components.browser.state.state.SessionState;
import mozilla.components.browser.state.state.WebExtensionState;
import mozilla.components.concept.engine.webextension.Action;

public class HamburgerMenuWidget extends UIWidget implements
        WidgetManagerDelegate.FocusChangeListener,
        ComponentsAdapter.StoreUpdatesListener {

    public interface MenuDelegate {
        void onSendTab();
        void onResize();
        void onFindInPage();
        void onSwitchMode();
        void onAddons();
        void onSaveWebApp();
        void onVrWebgl(); //r800zz
        void onPassthrough();
        void onAiPassthrough(); //r800zz
        boolean isPassthroughEnabled();
        void onGreenChromaKey(); //r800zz
        void onBlackChromaKey(); //r800zz
        void onWhiteChromaKey(); //r800zz
        void onSkyBlueChromaKey(); //r800zz
        void onAutoColorChromaKey(); //r800zz
        boolean isChromaKeyEnabled(); //r800zz
        int getChromaKeyMode(); //r800zz
        void onExitR800zzBrowser(); //r800zz
        void onLoadVideoFile(); //r800zz
        void onVideoPlayer(); //r800zz
        void onFileManager(); //r800zz
        void onVr180g(); //r800zz
        void onR800zz(); //r800zz
        void onPmxViewer(); //r800zz
        void onVrmViewer(); //r800zz
        void onGalShooting(); //r800zz
        void onVolleyRally(); //r800zz
        void onPageZoomIn();
        void onPageZoomOut();
        int getCurrentZoomLevel();
    }

    public static final int SWITCH_ITEM_ID = 0;

    private HamburgerMenuAdapter mAdapter;
    boolean mSendTabEnabled = false;
    private ArrayList<HamburgerMenuAdapter.MenuItem> mItems;
    private MenuDelegate mDelegate;
    private int mCurrentUAMode;

    public HamburgerMenuWidget(@NonNull Context aContext) {
        super(aContext);

        mItems = new ArrayList<>();
        mCurrentUAMode = SettingsStore.getInstance(aContext).getUaMode();

        updateUI();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void updateUI() {
        removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Inflate this data binding layout
        HamburgerMenuBinding binding = DataBindingUtil.inflate(inflater, R.layout.hamburger_menu, this, true);
        binding.setLifecycleOwner((VRBrowserActivity) getContext());
        mAdapter = new HamburgerMenuAdapter(getContext());
        binding.list.setAdapter(mAdapter);
        binding.list.setVerticalScrollBarEnabled(false);
        binding.list.setOnTouchListener((v, event) -> {
            v.requestFocusFromTouch();
            return false;
        });
        binding.list.addOnScrollListener(mScrollListener);
        binding.list.setHasFixedSize(true);
        binding.list.setItemViewCacheSize(20);
        // Drawing Cache is deprecated in API level 28: https://developer.android.com/reference/android/view/View#getDrawingCache().
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            binding.list.setDrawingCacheEnabled(true);
            binding.list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        }

        updateItems();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateUI();
    }

    private void internalShow(boolean proxifyLayer) {
        mWidgetPlacement.proxifyLayer = proxifyLayer;

        if (mWidgetManager != null) {
            mWidgetManager.addFocusChangeListener(this);
        }

        ComponentsAdapter.get().addStoreUpdatesListener(this);

        AnimationHelper.scaleIn(findViewById(R.id.menuContainer), 100, 0, null);
    }

    @Override
    public void show(int aShowFlags) {
        super.show(aShowFlags);
        if (mWidgetManager == null) {
            internalShow(false);
        } else {
            mWidgetManager.checkCompositionLayersSupported(supported -> {
                internalShow(supported);
            });
        }
    }

    @Override
    public void hide(int aHideFlags) {
        hide(aHideFlags, true);
    }

    public void hide(int aHideFlags, boolean anim) {
        if (anim) {
            AnimationHelper.scaleOut(findViewById(R.id.menuContainer), 100, 0, () -> HamburgerMenuWidget.super.hide(aHideFlags));

        } else {
            HamburgerMenuWidget.super.hide(aHideFlags);
        }

        mWidgetPlacement.proxifyLayer = false;

        if (mWidgetManager != null) {
            mWidgetManager.removeFocusChangeListener(this);
        }

        ComponentsAdapter.get().removeStoreUpdatesListener(this);
    }

    @Override
    protected void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        aPlacement.visible = false;
        aPlacement.width =  WidgetPlacement.dpDimension(getContext(), R.dimen.hamburger_menu_width);
        aPlacement.parentAnchorX = 1.0f;
        aPlacement.parentAnchorY = 1.0f;
        aPlacement.anchorX = 1.0f;
        aPlacement.anchorY = 0.0f;
        aPlacement.translationX = 20;
        aPlacement.translationY = 10;
        aPlacement.translationZ = WidgetPlacement.unitFromMeters(getContext(), R.dimen.context_menu_z_distance);
    }

    public void setUAMode(int uaMode) {
        mCurrentUAMode = uaMode;
        HamburgerMenuAdapter.MenuItem item = getSwitchModeIndex();
        if (item != null) {
            switch (uaMode) {
                case WSessionSettings.USER_AGENT_MODE_DESKTOP: {
                    item.setIcon(R.drawable.ic_icon_ua_desktop);
                }
                break;

                case WSessionSettings.USER_AGENT_MODE_MOBILE:
                case WSessionSettings.USER_AGENT_MODE_VR: {
                    item.setIcon(R.drawable.ic_icon_ua_default);
                }
                break;

            }

            mAdapter.notifyItemChanged(mItems.indexOf(item));
        }
    }

    public void setMenuDelegate(@Nullable MenuDelegate delegate) {
        mDelegate = delegate;
    }

    private void updateItems() {
        mItems = new ArrayList<>();

        // In kiosk mode, only resize, find in page and passthrough are available.
        if (!mWidgetManager.getFocusedWindow().isKioskMode()) {
            final Session activeSession = SessionStore.get().getActiveSession();

            if (!BuildConfig.FLAVOR_backend.equals("chromium")) {
                mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                        HamburgerMenuAdapter.MenuItem.TYPE_ADDONS_SETTINGS,
                        (menuItem) -> {
                            if (mDelegate != null) {
                                mDelegate.onAddons();
                            }
                            return null;
                        }).build());

                String url = activeSession.getCurrentUri();
                boolean showAddons = (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) && !mWidgetManager.getFocusedWindow().isNativeContentVisible();
                final SessionState tab = ComponentsAdapter.get().getSessionStateForSession(activeSession);
                if (tab != null && showAddons) {
                    final List<WebExtensionState> extensions = ComponentsAdapter.get().getSortedEnabledExtensions();
                    extensions.forEach((extension) -> {
                        if (!extension.getAllowedInPrivateBrowsing() && activeSession.isPrivateMode()) {
                            return;
                        }

                        // Do not show builtin extensions in the hamburger menu. As they are inside the APK, their URLs
                        // always start with "resource://android".
                        if (extension.getUrl().startsWith("resource://android")) {
                            return;
                        }

                        final WebExtensionState tabExtensionState = tab.getExtensionState().get(extension.getId());
                        if (extension.getBrowserAction() != null) {
                            addOrUpdateAddonMenuItem(
                                    extension,
                                    extension.getBrowserAction(),
                                    tabExtensionState != null ? tabExtensionState.getBrowserAction() : null);
                        }
                        if (extension.getPageAction() != null) {
                            addOrUpdateAddonMenuItem(
                                    extension,
                                    extension.getPageAction(),
                                    tabExtensionState != null ? tabExtensionState.getPageAction() : null);
                        }
                    });
                }
            }


            if (activeSession.getWebAppManifest() != null) {
                mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                        (menuItem) -> {
                            if (mDelegate != null) {
                                mDelegate.onSaveWebApp();
                            }
                            return null;
                        })
                        .withTitle(getContext().getString(R.string.hamburger_menu_save_web_app))
                        .withIcon(R.drawable.ic_web_app_registration)
                        .build());
            }

            if (mSendTabEnabled) {
                mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                        (menuItem) -> {
                            if (mDelegate != null) {
                                mDelegate.onSendTab();
                            }
                            return null;
                        })
                        .withTitle(getContext().getString(R.string.hamburger_menu_send_tab))
                        .withIcon(R.drawable.ic_icon_tabs_sendtodevice)
                        .build());
            }

            HamburgerMenuAdapter.MenuItem item = new HamburgerMenuAdapter.MenuItem.Builder(
                    HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                    (menuItem) -> {
                        if (mDelegate != null) {
                            mDelegate.onSwitchMode();
                        }
                        return null;
                    })
                    .withId(SWITCH_ITEM_ID)
                    .withTitle(getContext().getString(R.string.hamburger_menu_switch_to_desktop))
                    .build();
            switch (mCurrentUAMode) {
                case WSessionSettings.USER_AGENT_MODE_DESKTOP: {
                    item.setIcon(R.drawable.ic_icon_ua_desktop);
                }
                break;

                case WSessionSettings.USER_AGENT_MODE_MOBILE:
                case WSessionSettings.USER_AGENT_MODE_VR: {
                    item.setIcon(R.drawable.ic_icon_ua_default);
                }
                break;
            }
            mItems.add(item);
        }

        if (mWidgetManager.getFocusedWindow().getCurrentContentType() == Windows.ContentType.WEB_CONTENT) {
            mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                    HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                    (menuItem) -> {
                        if (mDelegate != null) {
                            mDelegate.onFindInPage();
                        }
                        return null;
                    })
                    .withTitle(getContext().getString(R.string.hamburger_menu_find_in_page))
                    .withIcon(R.drawable.ic_icon_search)
                    .build());
        }

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                (menuItem) -> {
                    if (mDelegate != null) {
                        mDelegate.onResize();
                    }
                    return null;
                })
                .withTitle(getContext().getString(R.string.hamburger_menu_resize))
                .withIcon(R.drawable.ic_icon_resize)
                .build());

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                (menuItem) -> {
                    if (mDelegate != null) {
                        mDelegate.onVrWebgl();
                    }
                    return null;
                })
                .withTitle(getContext().getString(R.string.hamburger_menu_vr_webgl))
                .withIcon(R.drawable.ic_icon_vr_projection)
                .build());

        if (mWidgetManager != null && mWidgetManager.isPassthroughSupported()) {
            mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                    HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT,
                    (menuItem) -> {
                        if (mDelegate != null) {
                            mDelegate.onPassthrough();
                            updateItems(); //r800zz
                        }
                        return null;
                    })
                    .withTitle(getContext().getString(R.string.hamburger_menu_toggle_passthrough))
                    .withIcon(mDelegate != null && mDelegate.isPassthroughEnabled() ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24)
                    .build());
        }

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
        (menuItem) -> { //r800zz
            if (mDelegate != null) { //r800zz
                mDelegate.onAiPassthrough(); //r800zz
                updateItems(); //r800zz
            } //r800zz
            return null; //r800zz
        }) //r800zz
        .withTitle(getContext().getString(R.string.hamburger_menu_toggle_ai_passthrough)) //r800zz
        .withIcon(mDelegate != null && mDelegate.getChromaKeyMode()==6 ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24) //r800zz
        .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
        (menuItem) -> { //r800zz
            if (mDelegate != null) { //r800zz
                mDelegate.onGreenChromaKey(); //r800zz
                updateItems(); //r800zz
            } //r800zz
            return null; //r800zz
        }) //r800zz
        .withTitle(getContext().getString(R.string.hamburger_menu_toggle_green_chroma_key)) //r800zz
        .withIcon(mDelegate != null && mDelegate.getChromaKeyMode()==1 ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24) //r800zz
        .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
        (menuItem) -> { //r800zz
            if (mDelegate != null) { //r800zz
                mDelegate.onBlackChromaKey(); //r800zz
                updateItems(); //r800zz
            } //r800zz
            return null; //r800zz
        }) //r800zz
        .withTitle(getContext().getString(R.string.hamburger_menu_toggle_black_chroma_key)) //r800zz
        .withIcon(mDelegate != null && mDelegate.getChromaKeyMode()==3 ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24) //r800zz
        .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
        (menuItem) -> { //r800zz
            if (mDelegate != null) { //r800zz
                mDelegate.onWhiteChromaKey(); //r800zz
                updateItems(); //r800zz
            } //r800zz
            return null; //r800zz
        }) //r800zz
        .withTitle(getContext().getString(R.string.hamburger_menu_toggle_white_chroma_key)) //r800zz
        .withIcon(mDelegate != null && mDelegate.getChromaKeyMode()==4 ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24) //r800zz
        .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
        (menuItem) -> { //r800zz
            if (mDelegate != null) { //r800zz
                mDelegate.onSkyBlueChromaKey(); //r800zz
                updateItems(); //r800zz
            } //r800zz
            return null; //r800zz
        }) //r800zz
        .withTitle(getContext().getString(R.string.hamburger_menu_toggle_sky_blue_chroma_key)) //r800zz
        .withIcon(mDelegate != null && mDelegate.getChromaKeyMode()==5 ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24) //r800zz
        .build()); //r800zz


        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
        HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
        (menuItem) -> { //r800zz
            if (mDelegate != null) { //r800zz
                mDelegate.onAutoColorChromaKey(); //r800zz
                updateItems(); //r800zz
            } //r800zz
            return null; //r800zz
        }) //r800zz
        .withTitle(getContext().getString(R.string.hamburger_menu_toggle_auto_color_chroma_key)) //r800zz
        .withIcon(mDelegate != null && mDelegate.getChromaKeyMode()==2 ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24) //r800zz
        .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onLoadVideoFile(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_load_video_file)) //r800zz
                //.withIcon(R.drawable.ic_icon_downloads) //r800zz
                .withIcon(R.drawable.ic_icon_media_play) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onVideoPlayer(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_video_player)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onFileManager(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_file_manager)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz


        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onPmxViewer(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_pmx_viewer)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onVrmViewer(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_vrm_viewer)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onGalShooting(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_gal_shooting)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onVolleyRally(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_volley_rally)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onVr180g(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_vr180g)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onR800zz(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_r800zz)) //r800zz
                .withIcon(R.drawable.ic_icon_search) //r800zz
                .build()); //r800zz

        mItems.add(new HamburgerMenuAdapter.MenuItem.Builder( //r800zz
                HamburgerMenuAdapter.MenuItem.TYPE_DEFAULT, //r800zz
                (menuItem) -> { //r800zz
                    if (mDelegate != null) { //r800zz
                        mDelegate.onExitR800zzBrowser(); //r800zz
                    } //r800zz
                    return null; //r800zz
                }) //r800zz
                .withTitle(getContext().getString(R.string.hamburger_menu_exit_r800zz_browser)) //r800zz
                .withIcon(R.drawable.ic_icon_exit) //r800zz
                .build()); //r800zz


        if (mWidgetManager != null && mWidgetManager.isPageZoomEnabled() && mDelegate != null) {
            mItems.add(new HamburgerMenuAdapter.MenuItem.Builder(
                    HamburgerMenuAdapter.MenuItem.TYPE_ZOOM, null)
                    .withZoom(Integer.toString(mDelegate.getCurrentZoomLevel()) + "%",
                            (isZoomOut) -> {
                        if (isZoomOut) mDelegate.onPageZoomOut();
                        else mDelegate.onPageZoomIn();
                        updateItems();
                        return null;
                    })
                    .build());
        }

        mAdapter.setItems(mItems);
        mAdapter.notifyDataSetChanged();

        mWidgetPlacement.height = mItems.size() * WidgetPlacement.dpDimension(getContext(), R.dimen.hamburger_menu_item_height);
        mWidgetPlacement.height += mBorderWidth * 2;
        mWidgetPlacement.height += WidgetPlacement.dpDimension(getContext(), R.dimen.hamburger_menu_triangle_height);

        updateWidget();
    }

    private void addOrUpdateAddonMenuItem(final WebExtensionState extension,
                                          final @NonNull Action globalAction,
                                          final @Nullable Action tabAction
    ) {
        HamburgerMenuAdapter.MenuItem menuItem = mItems.stream().filter(item -> item.getAddonId().equals(extension.getId())).findFirst().orElse(null);
        if (menuItem == null) {
            menuItem = new HamburgerMenuAdapter.MenuItem.Builder(
                    HamburgerMenuAdapter.MenuItem.TYPE_ADDON,
                    (item) -> {
                        globalAction.getOnClick().invoke();
                        onDismiss();
                        return null;
                    })
                    .withAddonId(extension.getId())
                    .withTitle(extension.getName())
                    .withIcon(R.drawable.ic_icon_addons)
                    .withAction(globalAction)
            .build();
            mItems.add(menuItem);
        }
        if (tabAction != null) {
            menuItem.setAction(globalAction.copyWithOverride(tabAction));
        }
    }

    private HamburgerMenuAdapter.MenuItem getSwitchModeIndex() {
        return mItems.stream().filter(item -> item.getId() == SWITCH_ITEM_ID).findFirst().orElse(null);
    }

    public void setSendTabEnabled(boolean value) {
        mSendTabEnabled = value;
        updateItems();
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        if (!ViewUtils.isEqualOrChildrenOf(this, newFocus) && isVisible()) {
            onDismiss();
        }
    }

    protected RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_SETTLING) {
                recyclerView.requestFocus();
            }
        }
    };

    @Override
    public void onTabSelected(@NonNull BrowserState state, @Nullable mozilla.components.browser.state.state.SessionState tab) {
        updateItems();
    }

}
