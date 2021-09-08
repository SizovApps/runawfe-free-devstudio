package ru.runa.gpd.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import ru.runa.wfe.InternalApplicationException;

public class DataTableUtils {
    private static final String DATA_TABLES_PROJECT_NAME = "DataTables";
    public static final String DATA_TABLE_FILE_EXTENSION = ".xml";

    public static IProject getDataTableProject() {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(DATA_TABLES_PROJECT_NAME);
    }

    public static List<IFile> getAllDataTables() {
        List<IFile> fileList = new ArrayList<>();
        IProject dtProject = getDataTableProject();
        if (dtProject.exists()) {
            try {
                for (IResource resource : dtProject.members()) {
                    if (resource instanceof IFile && ((IFile) resource).getName().endsWith(DATA_TABLE_FILE_EXTENSION)) {
                        fileList.add((IFile) resource);
                    }
                }
            } catch (CoreException e) {
                throw new InternalApplicationException(e);
            }
        }
        return fileList;
    }

    private DataTableUtils() {
        // All-static class
    }
}
