package ru.runa.gpd.bot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ru.runa.gpd.BotCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.GlobalSectionDefinition;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.util.BotScriptUtils;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;

import com.google.common.base.Preconditions;

public class BotImportCommand extends BotSyncCommand {

    private InputStream inputStream;

    private String botName;

    private String botStationName;

    public BotImportCommand() {
    }

    public BotImportCommand(InputStream inputStream, String botName, String botStationName) {
        init(inputStream, botName, botStationName);
    }

    public void init(InputStream inputStream, String botName, String botStationName) {
        this.inputStream = inputStream;
        this.botName = cleanBotName(botName);
        this.botStationName = botStationName;
    }

    protected void importBot(IProgressMonitor progressMonitor) throws IOException, CoreException {
        validate();
        Map<String, byte[]> files = IOUtils.getArchiveFiles(inputStream, true);
        Map<String, byte[]> globalSections = new HashMap<>();
        byte[] scriptXml = files.remove("script.xml");
        Preconditions.checkNotNull(scriptXml, "No script.xml");

        List<VariableUserType> variableUserTypes = BotScriptUtils.getVariableUserTypesFromScript(scriptXml, files);
        for (VariableUserType variableUserType : variableUserTypes) {
            WorkspaceOperations.saveDataTable(
                    DataTableUtils.getDataTableProject().getFile(variableUserType.getName() + DataTableUtils.FILE_EXTENSION), variableUserType);
        }

        List<BotTask> botTasks = BotScriptUtils.getBotTasksFromScript(botStationName, botName, scriptXml, files);

        IPath path = new Path(botStationName).append("/src/botstation/").append(botName);
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);

        if (BotCache.getBotNames(botStationName).contains(botName)) {
            Display display = new Display();
            Shell shell = new Shell(display);
            shell.setLayout(new GridLayout());
            shell.setText(Localization.getString("ImportBotStationWizardPage.page.confirmDeletion"));
            shell.setSize(500, 200);

            if (Dialogs.confirm2(Localization.getString("ImportBotStationWizardPage.page.sameBotName"), botName, shell)) {
                if (folder.exists()) {
                    Dialogs.information(Localization.getString("ImportBotStationWizardPage.page.removeOldBot"), shell);
                    return;
                }
            } else {
                return;
            }
        }

        for (String fileName : files.keySet()) {
            if (fileName.endsWith(GlobalSectionDefinition.FILE_EXTENSION)) {
                globalSections.put(fileName, files.get(fileName));
                files.remove(fileName);
            }
        }

        folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
        if (!folder.exists()) {
            folder.create(true, true, null);
        }

        for (BotTask botTask : botTasks) {
            IFile file = folder.getFile(botTask.getName());
            WorkspaceOperations.saveBotTask(file, botTask);

            // Save embedded files too.
            for (String fileToSave : botTask.getFilesToSave()) {
                if (files.get(fileToSave) == null) {
                    continue;
                }
                IOUtils.createOrUpdateFile(folder.getFile(fileToSave), new ByteArrayInputStream(files.get(fileToSave)));
            }
            botTask.getFilesToSave().clear();
        }
    }

    @Override
    protected void execute(IProgressMonitor progressMonitor) throws InvocationTargetException {
        try {
            importBot(progressMonitor);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    private String cleanBotName(String botName) {
        return botName == null ? null : botName.replaceAll(Pattern.quote(".bot"), "");
    }

    private void validate() {
        for (String testBotStationName : BotCache.getAllBotStationNames()) {
            if (testBotStationName.equals(botStationName)) {
                continue;
            }
            Set<String> botNames = BotCache.getBotNames(testBotStationName);
            if (botNames != null && botNames.contains(botName)) {
                throw new UniqueBotException(Localization.getString("ImportBotStationWizardPage.error.botWithSameNameExists", botName));
            }
        }
    }
}
