package ru.runa.gpd.util;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.gef.EditPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import ru.runa.gpd.BotCache;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.SubprocessMap;
import ru.runa.gpd.editor.BotTaskEditor;
import ru.runa.gpd.editor.DataTableEditor;
import ru.runa.gpd.editor.ProcessEditorBase;
import ru.runa.gpd.editor.GlobalSectionEditorBase;
import ru.runa.gpd.editor.ProcessSaveHistory;
import ru.runa.gpd.editor.gef.GEFProcessEditor;
import ru.runa.gpd.editor.graphiti.GraphitiProcessEditor;
import ru.runa.gpd.editor.graphiti.GraphitiGlobalSectionEditor;
import ru.runa.gpd.extension.DelegableProvider;
import ru.runa.gpd.extension.HandlerRegistry;
import ru.runa.gpd.extension.bot.IBotFileSupportProvider;
import ru.runa.gpd.lang.Language;
import ru.runa.gpd.lang.BpmnSerializer;
import ru.runa.gpd.lang.ProcessSerializer;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.BotTaskType;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Subprocess;
import ru.runa.gpd.lang.model.SubprocessDefinition;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.lang.model.GlobalSectionDefinition;
import ru.runa.gpd.lang.par.ParContentProvider;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.dialog.DataSourceDialog;
import ru.runa.gpd.ui.dialog.ProcessSaveHistoryDialog;
import ru.runa.gpd.ui.dialog.RenameBotDialog;
import ru.runa.gpd.ui.dialog.RenameBotStationDialog;
import ru.runa.gpd.ui.dialog.RenameBotTaskDialog;
import ru.runa.gpd.ui.dialog.RenameProcessDefinitionDialog;
import ru.runa.gpd.ui.dialog.RenameUserTypeDialog;
import ru.runa.gpd.ui.wizard.CompactWizardDialog;
import ru.runa.gpd.ui.wizard.CompareProcessDefinitionWizard;
import ru.runa.gpd.ui.wizard.CopyBotTaskWizard;
import ru.runa.gpd.ui.wizard.CopyDataTableWizard;
import ru.runa.gpd.ui.wizard.CopyProcessDefinitionWizard;
import ru.runa.gpd.ui.wizard.ExportBotElementWizardPage;
import ru.runa.gpd.ui.wizard.ExportBotWizard;
import ru.runa.gpd.ui.wizard.ExportDataSourceWizard;
import ru.runa.gpd.ui.wizard.ExportDataTableWizard;
import ru.runa.gpd.ui.wizard.ExportParWizard;
import ru.runa.gpd.ui.wizard.ExportGlbWizard;
import ru.runa.gpd.ui.wizard.ImportBotElementWizardPage;
import ru.runa.gpd.ui.wizard.ImportBotWizard;
import ru.runa.gpd.ui.wizard.ImportDataSourceWizard;
import ru.runa.gpd.ui.wizard.ImportDataTableWizard;
import ru.runa.gpd.ui.wizard.ImportParWizard;
import ru.runa.gpd.ui.wizard.ImportGlbWizard;
import ru.runa.gpd.ui.wizard.NewBotStationWizard;
import ru.runa.gpd.ui.wizard.NewBotTaskWizard;
import ru.runa.gpd.ui.wizard.NewBotWizard;
import ru.runa.gpd.ui.wizard.NewDataTableWizard;
import ru.runa.gpd.ui.wizard.NewFolderWizard;
import ru.runa.gpd.ui.wizard.NewProcessDefinitionWizard;
import ru.runa.gpd.ui.wizard.NewGlobalSectionDefinitionWizard;
import ru.runa.gpd.ui.wizard.NewProcessProjectWizard;
import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.datasource.DataSourceStuff;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import ru.runa.gpd.form.FormCSSTemplate;
import ru.runa.gpd.form.FormCSSTemplateRegistry;
import com.google.common.collect.Maps;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.extension.handler.ParamDef;
import ru.runa.gpd.extension.handler.ParamDefGroup;

public class AutomaticCreationUtils {

    public static ProcessDefinition createNewGlobalSectionDefinitionAutomatic (BotTask botTask) {

        if (!BotCache.isBotTaskFile(botTask)) {
            return null;
        }
        IFolder parentProcessDefinitionFolder = BotCache.getBotTaskFolder(botTask);

        PluginLogger.logInfo("parentProcessDefinitionFolder: " + parentProcessDefinitionFolder.getName());
        try {
            IFolder folder = isAlreadyCreatedGlobalSetion(parentProcessDefinitionFolder);
            boolean wasGlobalCreatedBefore = true;
            if (folder == null) {
                wasGlobalCreatedBefore = false;
                folder = getProcessFolderByCreateFromBot(parentProcessDefinitionFolder.getName());;
                folder.create(true, true, null);
            }
            PluginLogger.logInfo("Create from start: " + folder.getName());
            IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);
            PluginLogger.logInfo("definitionFile: " + definitionFile.getName());
            String processName = folder.getName();
            ru.runa.gpd.lang.Language language = ru.runa.gpd.lang.Language.valueOf("BPMN");
            Map<String, String> properties = Maps.newHashMap();
            if (language == ru.runa.gpd.lang.Language.BPMN) {
                properties.put(ru.runa.gpd.lang.BpmnSerializer.SHOW_SWIMLANE, ru.runa.gpd.util.SwimlaneDisplayMode.values()[0].name());
            }
            properties.put(ru.runa.gpd.lang.ProcessSerializer.ACCESS_TYPE, "Process");
            Document document = language.getSerializer().getInitialProcessDefinitionDocument(processName, properties);
            byte[] bytes = XmlUtil.writeXml(document);
            if (!wasGlobalCreatedBefore) {
                definitionFile.create(new ByteArrayInputStream(bytes), true, null);
            } else {
                definitionFile.setContents(new ByteArrayInputStream(bytes), IResource.FORCE, null);
            }

            PluginLogger.logInfo("Enter to end!!!");
            ProcessCache.newProcessDefinitionWasCreated(definitionFile);
            setGlobalVariables(definitionFile, botTask);
            WorkspaceOperations.openGlobalSectionDefinition(definitionFile);
        }
        catch (Exception e){
            PluginLogger.logError(e);
        }

        return null;
    }

    private static IFolder getProcessFolderByCreateFromBot(String projectName) {
        IContainer container = null;
        PluginLogger.logInfo("Enter file!");
        try {

            for (IProject botStationProject : IOUtils.getAllBotStationProjects()) {
                IContainer botStationFolder = botStationProject.getFolder("src/botstation");
                for (IResource botResource : botStationFolder.members()) {
                    if (botResource instanceof IFolder) {
                        if (botResource.getName().equals(projectName)) {
                            PluginLogger.logInfo("Set file!");
                            container = (IFolder) botResource;
                            break;
                        }
                    }
                }
            }
        }
        catch (CoreException e) {

        }

        PluginLogger.logInfo("BotFolderName: " + container.getName());
        projectName += " global";
        return IOUtils.getProcessFolder(container, projectName);
    }

    private static IFolder isAlreadyCreatedGlobalSetion(IFolder parentFolder) {
        try {
            for (IResource resource : parentFolder.members()) {
                if (resource instanceof IFolder) {
                    PluginLogger.logInfo("Child folder: " + ((IFolder) resource).getName());
                    return (IFolder) resource;
                }
            }
        }catch (Exception e) {
            return null;
        }
        return null;
    }

    private static void setGlobalVariables(IFile processFile, BotTask botTask) {
        List<ParamDef> botParams = getBotTaskParams(botTask);

        ProcessDefinition processDefinition = ProcessCache.getProcessDefinition(processFile);
        PluginLogger.logInfo("Return proc def: " + processDefinition.getFile().getName());
        for (VariableUserType userType : processDefinition.getVariableUserTypes()) {
            PluginLogger.logInfo("user type: " + userType.getName());
        }

        int countOfAddedParam = 0;
        for (ParamDef paramDef : botParams) {
            Variable variable = new Variable("Переменная" + countOfAddedParam, "Переменная" + countOfAddedParam, paramDef.getFormatFilters().get(0), processDefinition.getVariableUserType(paramDef.getFormatFilters().get(0)));
            PluginLogger.logInfo("Created var: " + variable.getScriptingName() + " | " + variable.getFormatClassName());
            processDefinition.addGlobalVariable(variable);
            countOfAddedParam += 1;
        }

    }

    private static List<ParamDef> getBotTaskParams(BotTask botTask) {
        List<ParamDef> paramDefs = new ArrayList<>();
        for (ParamDefGroup group : botTask.getParamDefConfig().getGroups()) {
            PluginLogger.logInfo("ParamDefGroup: " + group.getName());
            for (ParamDef paramDef : group.getParameters()) {
                PluginLogger.logInfo("Paramdef: " + paramDef.getFormatFilters().get(0));
                paramDefs.add(paramDef);
            }
        }
        return paramDefs;
    }



}
