package ru.runa.gpd.sync;

import ru.runa.gpd.Localization;
import ru.runa.gpd.ui.custom.Dialogs;

class DataImporterConnectionStatusVisitor implements ConnectionStatusVisitor<Boolean, Void> {

    @Override
    public Boolean onEstablishFailed(Void context) {
        Dialogs.error(Localization.getString("error.ConnectionFailed"));
        return false;
    }

    @Override
    public Boolean onFreeEdition(Void context) {
        Dialogs.warning(Localization.getString("wrong.server.product"));
        return false;
    }

    @Override
    public Boolean onIndustrialEdition(Void context) {
        return true;
    }

}
