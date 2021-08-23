package ru.runa.gpd;

import java.util.Map;

import org.dom4j.Document;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.UserTypeXmlContentPorvider;
import ru.runa.gpd.util.XmlUtil;

public class DataTableCache {

    private static final Map<VariableUserType, IFile> DATA_TABLES_FILES = Maps.newHashMap();
    private static final Map<String, VariableUserType> DATA_TABLES = Maps.newHashMap();

    static {
        reload();
    }

    public static synchronized void reload() {
        try {
            DATA_TABLES_FILES.clear();
            DATA_TABLES.clear();
            IProject dtProject = ResourcesPlugin.getWorkspace().getRoot().getProject("DataTables");
            for (IResource resource : dtProject.members()) {
                if (!(resource instanceof IFile)) {
                    continue;
                }
                IFile dataTableFile = (IFile) resource;
                try {
                    cacheDataTable(dataTableFile);
                } catch (Exception e) {
                    PluginLogger.logError(e);
                }
            }
            
        } catch (final Throwable th) {
            try {
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        PluginLogger.logError(Localization.getString("DataTableCache.unabletoload"), th);
                    }
                });
            } catch (Exception e) {
                PluginLogger.logErrorWithoutDialog("DataTableCache.unabletoload", e);
            }
        }
    }

    private static void cacheDataTable(IFile dataTableFile) {
        try {
            Document document = XmlUtil.parseWithoutValidation(dataTableFile.getContents(true));
            VariableUserType dataTable = UserTypeXmlContentPorvider.read(document);
            DATA_TABLES_FILES.put(dataTable, dataTableFile);
            DATA_TABLES.put(dataTable.getName(), dataTable);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    public static synchronized VariableUserType getDataTable(String fileName) {
        if (DATA_TABLES.containsKey(fileName)) {
            return DATA_TABLES.get(fileName);
        }
        return null;
    }
}
