package ru.runa.gpd.bot;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.dom4j.Document;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.ModalContext;
import com.google.common.base.Throwables;


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


        zipStream.putNextEntry(new ZipEntry("script.xml"));
        List<BotTask> botTaskForExport = getBotTasksForExport(botFolder);
        for (BotTask botTask : botTaskForExport) {
            WorkspaceOperations.saveBotTask(botFolder.getFile(botTask.getName()), botTask);
        }
        Document document = BotScriptUtils.createScriptForBotLoading(botFolder.getName(), botTaskForExport);
        XmlUtil.writeXml(document, zipStream);
        writeConfigurationFiles(botFolder, zipStream);
        writeEmbeddedFiles(botFolder, zipStream);

        ru.runa.gpd.PluginLogger.logInfo("End bot write!!!");
        zipStream.putNextEntry(new ZipEntry(botFolder.getName() + ".glb"));
        ByteArrayOutputStream globalStream = new ByteArrayOutputStream();
        getGlobalSectionStream(globalStream, botFolder);
        globalStream.close();
        zipStream.write(globalStream.toByteArray());
        ru.runa.gpd.PluginLogger.logInfo("End global write!!!");

        zipStream.close();
        out.flush();
    }

    protected void getGlobalSectionStream(OutputStream out, IFolder botFolder) throws IOException, CoreException {
        ZipOutputStream zipStream = new ZipOutputStream(out);
        getBotGlobalSectionStream(botFolder, new ZipOutputStream(out));
    }

    protected void getBotGlobalSectionStream(IFolder botFolder, ZipOutputStream zipOutputStream) throws IOException, CoreException {
        try {
            IFolder globalFolder = null;
            IResource[] members = botFolder.members();
            for (IResource resource : members) {
                if (resource instanceof IFolder) {
                    globalFolder = (IFolder) resource;
                }
            }

            List<IFile> resourcesToExport = new ArrayList<IFile>();
            List<File> files = new ArrayList<>();
            IResource[] globalMembers = globalFolder.members();
            for (IResource resource : globalMembers) {
                if (resource instanceof IFile) {
                    IPath path = resource.getFullPath();
                    ru.runa.gpd.PluginLogger.logInfo("Global file from bot: " + ((IFile) resource).getName() + " | " + path.toString());
                    File file = path.toFile();
                    ru.runa.gpd.PluginLogger.logInfo("File: " + file.getName());
                    files.add(file);
                    resourcesToExport.add((IFile) resource);
                }
            }
            ru.runa.gpd.PluginLogger.logInfo("Files size: " + files.size());


            new ExportGlbWizardPage.ParExportOperationFromBot(resourcesToExport, zipOutputStream, zipOutputStream).run(null);

        } catch (Throwable th) {
            ru.runa.gpd.PluginLogger.logErrorWithoutDialog(ru.runa.gpd.Localization.getString("ExportParWizardPage.error.export"), th);
            ru.runa.gpd.PluginLogger.logErrorWithoutDialog((Throwables.getRootCause(th).getMessage()));
        }
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


