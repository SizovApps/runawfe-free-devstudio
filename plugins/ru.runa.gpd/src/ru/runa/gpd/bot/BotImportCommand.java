package ru.runa.gpd.bot;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.custom.Dialogs;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.events.SelectionEvent;

import ru.runa.gpd.BotCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.util.BotScriptUtils;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;


import com.google.common.base.Preconditions;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.eclipse.jface.viewers.ListViewer;

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
        for (String cur : files.keySet()) {
            ru.runa.gpd.PluginLogger.logInfo("Import files: " + cur + " | " + new String(files.get(cur)));
        }
        byte[] scriptXml = files.remove("script.xml");

        Preconditions.checkNotNull(scriptXml, "No script.xml");

        List<BotTask> botTasks = BotScriptUtils.getBotTasksFromScript(botStationName, botName, scriptXml, files);

        // create bot
        IPath path = new Path(botStationName).append("/src/botstation/").append(botName);
        ru.runa.gpd.PluginLogger.logInfo("Imported folder path: " + path.toString());
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);


        ru.runa.gpd.PluginLogger.logInfo("importBotName: " + botName + " | " + botStationName);
        if (BotCache.getBotNames(botStationName).contains(botName)) {

            //final int needToDel = needToRewriteBot();


            Display display = new Display();
            ru.runa.gpd.PluginLogger.logInfo("Need to rewrite!");
            ru.runa.gpd.PluginLogger.logInfo("Display from command: " + display.toString());
            Shell shell = new Shell(display);
            ru.runa.gpd.PluginLogger.logInfo("Shell from command: " + shell.toString());

            shell.setLayout(new GridLayout());
            shell.setText("Подтвердить удаление");
            shell.setSize(500, 200);



//            if (res[0] == 1) {
            if (Dialogs.confirm2("Обнаружен бот с таким же именем! Хотите перезаписать старого бота?", botName, shell)) {
                if (folder.exists()) {
                    ru.runa.gpd.PluginLogger.logInfo("Need to delete old bot!");
                    Dialogs.information("Необходимо сначала удалить старого бота!", shell);
                    return;
//                    ArrayList<IResource> resourcesToDel = new ArrayList<IResource>();
//                    //Collections.addAll(resourcesToDel, folder.members());
//                    resourcesToDel.add(folder);
                    //WorkspaceOperations.deleteBotResources(resourcesToDel);
                }
            }
            else{
                ru.runa.gpd.PluginLogger.logInfo("Select not to delete!");
                return;
            }

        }

        for (String fileName: files.keySet()) {
            if (fileName.endsWith(".glb")) {
                globalSections.put(fileName, files.get(fileName));
                files.remove(fileName);
            }
        }

        ru.runa.gpd.PluginLogger.logInfo("After delete!");



        folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
        if (!folder.exists()) {
            folder.create(true, true, null);
        }

        for (BotTask botTask : botTasks) {
            ru.runa.gpd.PluginLogger.logInfo("Imported botTask: " + botTask.getName());
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

        for (String globalSectionName : globalSections.keySet()) {
            ru.runa.gpd.PluginLogger.logInfo("globalSectionName: " + globalSectionName);
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

    private int needToRewriteBot() {

        final int[] res = {0};
        Display display = new Display();
        ru.runa.gpd.PluginLogger.logInfo("Need to rewrite!");
        ru.runa.gpd.PluginLogger.logInfo("Display from command: " + display.toString());
        Shell shell = new Shell(display);
        ru.runa.gpd.PluginLogger.logInfo("Shell from command: " + shell.toString());

        shell.setLayout(new GridLayout());
        shell.setText("Подтвердить удаление");
        shell.setSize(500, 200);


        Label label = new Label(shell, SWT.NONE);
        label.setText("Заменить существующего бота?");

        Composite composite = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);



        Button okButton = new Button(composite, SWT.PUSH);
        okButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        okButton.setText("OK");

        okButton.addSelectionListener(new LoggingSelectionAdapter() {
            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                res[0] = 1;
                ru.runa.gpd.PluginLogger.logInfo("Res: " + res[0]);
                display.dispose();
            }
        });

        Button noButton = new Button(composite, SWT.PUSH);
        noButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        noButton.setText("NO");
        noButton.addSelectionListener(new LoggingSelectionAdapter() {
            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                res[0] = -1;
                ru.runa.gpd.PluginLogger.logInfo("Res: " + res[0]);
                display.dispose();
            }
        });


        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
        return res[0];
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
