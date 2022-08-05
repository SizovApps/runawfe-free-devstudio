package ru.runa.gpd.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import java.lang.StackTraceElement;

public class DataTableUtils {
    private static final String PROJECT_NAME = "DataTables";
    public static final String FILE_EXTENSION = ".xml";

    public static IProject getDataTableProject() {

        ru.runa.gpd.PluginLogger.logInfo("getDataTableProject enter!");
        return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    }
}
