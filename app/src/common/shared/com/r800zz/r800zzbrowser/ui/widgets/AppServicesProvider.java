package com.r800zz.r800zzbrowser.ui.widgets;

import com.r800zz.r800zzbrowser.AppExecutors;
import com.r800zz.r800zzbrowser.browser.Accounts;
import com.r800zz.r800zzbrowser.browser.Addons;
import com.r800zz.r800zzbrowser.browser.LoginStorage;
import com.r800zz.r800zzbrowser.browser.Places;
import com.r800zz.r800zzbrowser.browser.Services;
import com.r800zz.r800zzbrowser.browser.engine.SessionStore;
import com.r800zz.r800zzbrowser.db.AppDatabase;
import com.r800zz.r800zzbrowser.db.DataRepository;
import com.r800zz.r800zzbrowser.downloads.DownloadsManager;
import com.r800zz.r800zzbrowser.speech.SpeechRecognizer;
import com.r800zz.r800zzbrowser.utils.BitmapCache;
import com.r800zz.r800zzbrowser.utils.ConnectivityReceiver;
import com.r800zz.r800zzbrowser.utils.EnvironmentsManager;
import com.r800zz.r800zzbrowser.utils.DictionariesManager;

public interface AppServicesProvider {

    SessionStore getSessionStore();
    Services getServices();
    Places getPlaces();
    AppDatabase getDatabase();
    AppExecutors getExecutors();
    DataRepository getRepository();
    BitmapCache getBitmapCache();
    Accounts getAccounts();
    DownloadsManager getDownloadsManager();
    SpeechRecognizer getSpeechRecognizer();
    EnvironmentsManager getEnvironmentsManager();
    DictionariesManager getDictionariesManager();
    LoginStorage getLoginStorage();
    Addons getAddons();
    ConnectivityReceiver getConnectivityReceiver();
}
