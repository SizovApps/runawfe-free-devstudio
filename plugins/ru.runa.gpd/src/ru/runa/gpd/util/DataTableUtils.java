package ru.runa.gpd.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

public class DataTableUtils {
    private static final String DATA_TABLES_PROJECT_NAME = "DataTables";
    public static final String DATA_TABLE_FILE_EXTENSION = ".xml";

    public static IProject getDataTableProject() {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(DATA_TABLES_PROJECT_NAME);
    }
}
