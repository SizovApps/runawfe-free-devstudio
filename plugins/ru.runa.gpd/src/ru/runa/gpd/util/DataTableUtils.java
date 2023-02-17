package ru.runa.gpd.util;

import java.util.ArrayList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.extension.handler.ParamDefConfig;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.bpmn.ScriptTask;
import ru.runa.gpd.lang.model.VariableUserType;

public class DataTableUtils {
    private static final String PROJECT_NAME = "DataTables";
    public static final String FILE_EXTENSION = ".xml";

    public static IProject getDataTableProject() {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    }

    public static boolean dataTableProjectExists() {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(PROJECT_NAME) != null;
    }

    public static List<VariableUserType> createVariableUserTypes(String configuration) {
        Document document = XmlUtil.parseWithoutValidation(configuration);
        Element configElement = document.getRootElement().element(BotTaskUtils.PARAMETERS_ELEMENT).element(ParamDefConfig.NAME_CONFIG);
        List<VariableUserType> variableUserTypes = VariableUserTypeParser.parseVariableUserTypesFromTask(configElement);
        return variableUserTypes;
    }

    public static String writeVariableUserTypesConfiguration(String botTaskConfiguration, BotTask botTask, String delegationClassName) {
        Document document = XmlUtil.parseWithoutValidation(botTaskConfiguration);
        Element configElement = document.getRootElement().element(BotTaskUtils.PARAMETERS_ELEMENT).element(ParamDefConfig.NAME_CONFIG);

        List<String> variableUserTypesNames = botTask.getVariableUserTypesNames();
        if (delegationClassName.equals(ScriptTask.INTERNAL_STORAGE_HANDLER_CLASS_NAME)) {
            variableUserTypesNames.add(botTask.getSelectedDataTableName());
            VariableUserTypeParser.addTableNameForInternalStorage(botTask.getSelectedDataTableName(), configElement);
        }

        List<Document> dataTablesDocuments = new ArrayList<>();
        for (String dataTableName : variableUserTypesNames) {
            if (!(DataTableCache.getDataTable(dataTableName) == null)) {
                try {
                    dataTablesDocuments.add(XmlUtil.parseWithoutValidation(
                            getDataTableProject().getFile(dataTableName + DataTableUtils.FILE_EXTENSION).getContents(true)));
                } catch (CoreException e) {
                    PluginLogger.logErrorWithoutDialog(e.getMessage());
                }
            }
        }
        VariableUserTypeParser.addVariableUserTypes(dataTablesDocuments, configElement);

        return XmlUtil.toString(document, OutputFormat.createPrettyPrint());

    }
}
