package ru.runa.gpd.settings;

import ru.runa.gpd.Localization;
import ru.runa.gpd.sync.ConnectionStatusVisitor;
import ru.runa.gpd.ui.custom.Dialogs;

class WfeServerConnectionPreferencePageConnectionStatusVisitor
        implements ConnectionStatusVisitor<Void, WfeServerConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback> {

    @Override
    public Void onEstablishFailed(WfeServerConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback industrialEditionCallback) {
        Dialogs.error(Localization.getString("error.ConnectionFailed"));
        return null;
    }

    @Override
    public Void onFreeEdition(WfeServerConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback industrialEditionCallback) {
        Dialogs.warning(Localization.getString("wrong.server.product"));
        return null;
    }

    @Override
    public Void onIndustrialEdition(WfeServerConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback industrialEditionCallback) {
        try {
            industrialEditionCallback.run();
            Dialogs.information(Localization.getString("test.Connection.Ok"));
        } catch (Throwable th) {
            Dialogs.error(Localization.getString("error.ConnectionFailed"), th);
        }
        return null;
    }

    @FunctionalInterface
    interface OnIndustrialEditionCallback {
        void run() throws Exception;
    }

}
