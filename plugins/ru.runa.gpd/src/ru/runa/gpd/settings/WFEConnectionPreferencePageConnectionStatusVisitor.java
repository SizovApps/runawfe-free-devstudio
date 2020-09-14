package ru.runa.gpd.settings;

import ru.runa.gpd.Localization;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.wfe.ConnectionStatusVisitor;

class WFEConnectionPreferencePageConnectionStatusVisitor
        implements ConnectionStatusVisitor<Void, WFEConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback> {

    @Override
    public Void onEstablishFailed(WFEConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback industrialEditionCallback) {
        Dialogs.error(Localization.getString("error.ConnectionFailed"));
        return null;
    }

    @Override
    public Void onFreeEdition(WFEConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback industrialEditionCallback) {
        Dialogs.warning(Localization.getString("wrong.server.product"));
        return null;
    }

    @Override
    public Void onIndustrialEdition(WFEConnectionPreferencePageConnectionStatusVisitor.OnIndustrialEditionCallback industrialEditionCallback) {
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
