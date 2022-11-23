package ru.runa.gpd.bot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
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
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.ui.wizard.ExportGlbWizardPage;
import ru.runa.gpd.util.BotScriptUtils;
import ru.runa.gpd.util.BotTaskUtils;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;

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

        if (hasGlobalSection(botFolder)) {
            zipStream.putNextEntry(new ZipEntry(botFolder.getName() + ".glb"));
            ByteArrayOutputStream globalStream = new ByteArrayOutputStream();
            getGlobalSectionStream(globalStream, botFolder);
            globalStream.close();
            zipStream.write(globalStream.toByteArray());
        }

        zipStream.close();
        out.flush();
    }

    protected void getGlobalSectionStream(OutputStream out, IFolder botFolder) throws IOException, CoreException {
        ZipOutputStream zipStream = new ZipOutputStream(out);
        getBotGlobalSectionStream(botFolder, new ZipOutputStream(out));
    }

    protected void getBotGlobalSectionStream(IFolder botFolder, ZipOutputStream zipOutputStream) throws IOException, CoreException {
        try {
            boolean hasGlobalSection = false;
            IFolder globalFolder = null;
            IResource[] members = botFolder.members();
            for (IResource resource : members) {
                if (resource instanceof IFolder) {
                    hasGlobalSection = true;
                    globalFolder = (IFolder) resource;
                    break;
                }
            }
            if (!hasGlobalSection){
                return;
            }
            List<IFile> resourcesToExport = new ArrayList<IFile>();
            List<File> files = new ArrayList<>();
            IResource[] globalMembers = globalFolder.members();
            for (IResource resource : globalMembers) {
                if (resource instanceof IFile) {
                    IPath path = resource.getFullPath();
                    File file = path.toFile();
                    files.add(file);
                    resourcesToExport.add((IFile) resource);
                }
            }
            new ExportGlbWizardPage.ParExportOperationFromBot(resourcesToExport, zipOutputStream, zipOutputStream).run(null);

        } catch (Throwable th) {
            PluginLogger.logErrorWithoutDialog(ru.runa.gpd.Localization.getString("ExportParWizardPage.error.export"), th);
            PluginLogger.logErrorWithoutDialog((Throwables.getRootCause(th).getMessage()));
        }
    }

    private boolean hasGlobalSection(IFolder botFolder) {
        try {
            IResource[] members = botFolder.members();
            for (IResource resource : members) {
                if (resource instanceof IFolder) {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e) {
            return false;
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
