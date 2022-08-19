package ru.runa.gpd.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.editor.DataTableEditor;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.WorkspaceOperations;

public class NewDataTableWizard extends Wizard implements INewWizard {
    private NewDataTableWizardPage page;
    private IStructuredSelection selection;
    private IWorkbench workbench;

    public NewDataTableWizard() {
    }

    @Override
    public void addPages() {
        super.addPages();
        page = new NewDataTableWizardPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(false, false, monitor -> {
                try {
                    monitor.beginTask("processing", 4);
                    IProject dtProject = DataTableUtils.getDataTableProject();
                    IFile dataTableFile = dtProject.getFile(page.getDataTableName() + DataTableUtils.FILE_EXTENSION);
                    VariableUserType dataTable = new VariableUserType(page.getDataTableName());
                    WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                    monitor.worked(1);
                    IDE.openEditor(getActivePage(), dataTableFile, DataTableEditor.ID, true);
                    monitor.done();
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            PluginLogger.logError("datatable.error.creation", e);
            return false;
        }
        return true;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
        this.workbench = workbench;
    }

    private IWorkbenchPage getActivePage() {
        return workbench.getActiveWorkbenchWindow().getActivePage();
    }
}
