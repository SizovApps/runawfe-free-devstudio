package ru.runa.gpd.sync;

import java.util.List;

import ru.runa.wfe.var.UserType;

public class WfeServerDataTableImporter extends WfeServerConnectorDataImporter<List<String>> {
    private static WfeServerDataTableImporter instance = new WfeServerDataTableImporter();

    public static WfeServerDataTableImporter getInstance() {
        return instance;
    }

    @Override
    protected List<String> loadRemoteData() throws Exception {
        return getConnector().getDataTableNames();
    }

    public UserType getDataTable(String name) {
        return getConnector().getDataTable(name);
    }

}
