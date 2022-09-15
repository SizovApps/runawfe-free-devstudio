package ru.runa.gpd.bot;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.dom4j.Document;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.ModalContext;

import ru.runa.gpd.BotCache;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.ui.wizard.ExportGlbWizardPage;
import ru.runa.gpd.util.BotScriptUtils;
import ru.runa.gpd.util.BotTaskUtils;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.gpd.editor.ProcessSaveHistory;
import ru.runa.gpd.util.WizardPageUtils;
import ru.runa.gpd.aspects.UserActivity;

public class BotExportCommand extends BotSyncCommand {
    protected final OutputStream outputStream;
    protected final IResource exportResource;

    public BotExportCommand(IResource exportResource, OutputStream outputStream) {
        this.outputStream = outputStream;
        this.exportResource = exportResource;
    }

    @Override
    protected void execute(IProgressMonitor progressMonitor) throws InvocationTargetException {
        try {
            int totalWork = 1;
            progressMonitor.beginTask("", totalWork);
            ByteArrayOutputStream botStream = new ByteArrayOutputStream();
            getBotStream(botStream, getBotFolder());
            botStream.close();
            outputStream.write(botStream.toByteArray());
            progressMonitor.worked(1);
            ModalContext.checkCanceled(progressMonitor);
            progressMonitor.done();
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    protected void getBotStream(OutputStream out, IFolder botFolder) throws IOException, CoreException {
        ZipOutputStream zipStream = new ZipOutputStream(out);


        getBotGlobalSectionStream(botFolder, zipStream);



        zipStream.putNextEntry(new ZipEntry("script.xml"));
        List<BotTask> botTaskForExport = getBotTasksForExport(botFolder);
        for (BotTask botTask : botTaskForExport) {
            WorkspaceOperations.saveBotTask(botFolder.getFile(botTask.getName()), botTask);
        }
        Document document = BotScriptUtils.createScriptForBotLoading(botFolder.getName(), botTaskForExport);
        XmlUtil.writeXml(document, zipStream);
        writeConfigurationFiles(botFolder, zipStream);
        writeEmbeddedFiles(botFolder, zipStream);
        zipStream.close();
        out.flush();
    }

    protected void getBotGlobalSectionStream(IFolder botFolder, ZipOutputStream zipStream) throws IOException, CoreException {
        zipStream.putNextEntry(new ZipEntry(botFolder.getName() + "_global.glb"));
        try {
            IFolder globalFolder = null;
            IResource[] members = botFolder.members();
            for (IResource resource : members) {
                if (resource instanceof IFolder) {
                    globalFolder = (IFolder) resource;
                }
            }

            List<IFile> resourcesToExport = new ArrayList<IFile>();
            IResource[] globalMembers = globalFolder.members();
            for (IResource resource : globalMembers) {
                if (resource instanceof IFile) {
                    ru.runa.gpd.PluginLogger.logInfo("Global file from bot: " + ((IFile) resource).getName());
                    resourcesToExport.add((IFile) resource);
                }
            }

            if (ProcessSaveHistory.isActive()) {
                Map<String, File> savepoints = ProcessSaveHistory.getSavepoints(globalFolder);
                if (savepoints.size() > 0) {
                    List<File> filesToExport = new ArrayList<>();
                    for (Map.Entry<String, File> savepoint : savepoints.entrySet()) {
                        ru.runa.gpd.PluginLogger.logInfo("Savepoint: " + savepoint.getValue().getName());
                        filesToExport.add(savepoint.getValue());
                    }
                    filesToExport.add(new File(botFolder.getName() + "_global.glb"));
                    String oldestSavepointName = ((NavigableMap<String, File>) savepoints).lastEntry().getValue().getName();
                    String oldestTimestamp = oldestSavepointName.substring(oldestSavepointName.lastIndexOf("_") + 1,
                            oldestSavepointName.lastIndexOf("."));
                    Map<String, File> uaLogs = UserActivity.getLogs(globalFolder);
                    for (Map.Entry<String, File> uaLog : uaLogs.entrySet()) {
                        if (oldestTimestamp.compareTo(uaLog.getKey()) <= 0) {
                            filesToExport.add(uaLog.getValue());
                        }
                    }
                    for (File file : filesToExport) {
                        ru.runa.gpd.PluginLogger.logInfo("filesToExport: " + file.getName());
                    }
                    WizardPageUtils.zip(filesToExport, zipStream);
                }
            }
        } catch (Exception e) {}
    }

    protected IFolder getBotFolder() {
        return (IFolder) exportResource;
    }

    protected List<BotTask> getBotTasksForExport(IFolder botFolder) throws CoreException, IOException {
        return BotCache.getBotTasks(botFolder.getName());
    }

    protected void writeConfigurationFiles(IFolder botFolder, ZipOutputStream zipStream) throws CoreException, IOException {
        for (IResource resource : botFolder.members()) {
            if (resource instanceof IFile && BotCache.CONFIGURATION_FILE_EXTENSION.equals(resource.getFileExtension())) {
                write(zipStream, new ZipEntry(resource.getName()), (IFile) resource);
            }
        }
    }

    protected void writeEmbeddedFiles(IFolder botFolder, ZipOutputStream zipStream) throws CoreException, IOException {
        for (IResource resource : botFolder.members()) {
            // TODO must be replaced to IBotFileSupportProvider.getEmbeddedFileName(BotTask)
            if (resource instanceof IFile && resource.getName().contains(BotTaskUtils.EMBEDDED_SUFFIX)) {
                write(zipStream, new ZipEntry(resource.getName()), (IFile) resource);
            }
        }
    }

}
