package ru.runa.gpd.util;

import java.io.ByteArrayInputStream;
import java.util.Map;
import ru.runa.gpd.util.VariableUtils;
import org.dom4j.Document;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import ru.runa.gpd.BotCache;
import ru.runa.gpd.lang.Language;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.editor.GlobalSectionEditorBase;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.lang.BpmnSerializer;
import ru.runa.gpd.lang.ProcessSerializer;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import com.google.common.collect.Maps;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.extension.handler.ParamDef;
import ru.runa.gpd.extension.handler.ParamDefGroup;

public class AutomaticCreationUtils {

    public static ProcessDefinition createNewGlobalSectionDefinitionAutomatic(BotTask botTask) {
        if (!BotCache.isBotTaskFile(botTask)) {
            return null;
        }
        IFolder parentProcessDefinitionFolder = BotCache.getBotTaskFolder(botTask);

        try {
            IFolder folder = isAlreadyCreatedGlobalSetion(parentProcessDefinitionFolder);
            boolean wasGlobalCreatedBefore = true;
            if (folder == null) {
                wasGlobalCreatedBefore = false;
                folder = getProcessFolderByCreateFromBot(parentProcessDefinitionFolder.getName());
                folder.create(true, true, null);
            }
            IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);
            String processName = folder.getName();
            Language language = Language.BPMN;
            Map<String, String> properties = Maps.newHashMap();
            if (language == Language.BPMN) {
                properties.put(BpmnSerializer.SHOW_SWIMLANE, SwimlaneDisplayMode.values()[0].name());
            }
            properties.put(ProcessSerializer.ACCESS_TYPE, "Process");
            Document document = language.getSerializer().getInitialProcessDefinitionDocument(processName, properties);
            byte[] bytes = XmlUtil.writeXml(document);
            if (!wasGlobalCreatedBefore) {
                definitionFile.create(new ByteArrayInputStream(bytes), true, null);
            } else {
                definitionFile.setContents(new ByteArrayInputStream(bytes), IResource.FORCE, null);
            }
            ProcessCache.newProcessDefinitionWasCreated(definitionFile);
            setGlobalVariables(definitionFile, botTask);
            try {
                GlobalSectionEditorBase processEditorBase = WorkspaceOperations.openGlobalSectionDefinition(definitionFile);
                if (processEditorBase != null) {
                    processEditorBase.setIsFromBot(true);
                }
                if (processEditorBase != null && processEditorBase.getVariableEditorPage() != null) {
                    processEditorBase.getVariableEditorPage().updateViewer();
                }
            } catch (NullPointerException ex) {
                PluginLogger.logError(ex.getMessage(), ex);
            }
        } catch (Exception e) {
            PluginLogger.logError(e);
        }
        return null;
    }

    private static IFolder getProcessFolderByCreateFromBot(String projectName) {
        IContainer container = null;
        try {
            for (IProject botStationProject : IOUtils.getAllBotStationProjects()) {
                IContainer botStationFolder = botStationProject.getFolder("src/botstation");
                for (IResource botResource : botStationFolder.members()) {
                    if (botResource instanceof IFolder) {
                        if (botResource.getName().equals(projectName)) {
                            container = (IFolder) botResource;
                            break;
                        }
                    }
                }
            }
        } catch (CoreException e) {
            PluginLogger.logError(e);
        }

        projectName += " global";
        projectName = "." + projectName;
        return IOUtils.getProcessFolder(container, projectName);
    }

    private static IFolder isAlreadyCreatedGlobalSetion(IFolder parentFolder) {
        try {
            for (IResource resource : parentFolder.members()) {
                if (resource instanceof IFolder) {
                    return (IFolder) resource;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static void setGlobalVariables(IFile processFile, BotTask botTask) {
        ProcessDefinition processDefinition = ProcessCache.getProcessDefinition(processFile);
        for (Variable variable : processDefinition.getGlobalVariables()) {
            processDefinition.removeGlobalVariable(variable);
        }
        for (ParamDefGroup group : botTask.getParamDefConfig().getGroups()) {
            for (ParamDef paramDef : group.getParameters()) {
                String variableName = paramDef.getLabel();
                if (group.getName() == "output") {
                    variableName += "_out";
                }
                String scriptingName = VariableUtils.generateNameForScripting(processDefinition.getGlobalVariables(), variableName, null);
                String artifactName = VariableFormatRegistry.getInstance().getNameFromJavaClassName(paramDef.getFormatFilters().get(0));
                Variable variable = new Variable(variableName, scriptingName, artifactName,
                        processDefinition.getVariableUserType(paramDef.getFormatFilters().get(0)));
                processDefinition.addChild(variable);
            }
        }
        try {
            WorkspaceOperations.saveProcessDefinition(processDefinition);
        } catch (Exception e) {
            PluginLogger.logError(e.getMessage(), e);
        }

    }
}
