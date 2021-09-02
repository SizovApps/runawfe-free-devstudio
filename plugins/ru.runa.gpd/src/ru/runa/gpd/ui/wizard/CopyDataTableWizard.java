package ru.runa.gpd.ui.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;

public class CopyDataTableWizard extends Wizard implements INewWizard {
    private IStructuredSelection selection;
    private NewDataTableWizardPage page;
    private static final String DATA_TABLE_FILE_EXTENSION = ".xml";

    public CopyDataTableWizard() {
        setWindowTitle(Localization.getString("CopyDataTableWizard.wizard.title"));
    }

    @Override
    public void init(IWorkbench w, IStructuredSelection currentSelection) {
        this.selection = currentSelection;
    }

    @Override
    public void addPages() {
        IFile sourceDataTable = (IFile) selection.getFirstElement();
        page = new NewDataTableWizardPage(selection, IOUtils.getWithoutExtension(sourceDataTable.getName()));
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        monitor.beginTask(Localization.getString("CopyDataTableWizard.monitor.title"), 3);
                        monitor.worked(1);
                        IFile sourceDataTableFile = (IFile) selection.getFirstElement();
                        VariableUserType userType = DataTableCache.getDataTable(IOUtils.getWithoutExtension(sourceDataTableFile.getName()));
                        userType.setName(page.getDataTableName());
                        IProject project = sourceDataTableFile.getProject();
                        IFile destDataTableFile = project.getFile(page.getDataTableName() + DATA_TABLE_FILE_EXTENSION);
                        WorkspaceOperations.saveDataTable(destDataTableFile, userType);
                        monitor.worked(1);
                        monitor.done();
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (Exception e) {
            PluginLogger.logError(e);
            return false;
        }
        return true;
    }
}
