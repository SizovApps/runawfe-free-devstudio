package ru.runa.gpd.ui.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.editor.DataTableEditor;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.WorkspaceOperations;

public class NewDataTableWizard extends Wizard implements INewWizard {
    private NewDataTableWizardPage page;
    private IStructuredSelection selection;
    private IWorkbench workbench;
    private static final String DATA_TABLES_PROJECT_NAME = "DataTables";
    private static final String DATA_TABLE_FILE_EXTENSION = ".xml";

    public NewDataTableWizard() {
    }

    @Override
    public void addPages() {
        super.addPages();
        page = new NewDataTableWizardPage(selection);
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        monitor.beginTask("processing", 4);
                        IProject dtProject = ResourcesPlugin.getWorkspace().getRoot().getProject(DATA_TABLES_PROJECT_NAME);
                        IFile dataTableFile = dtProject.getFile(page.getDataTableName() + DATA_TABLE_FILE_EXTENSION);
                        VariableUserType dataTable = new VariableUserType(page.getDataTableName());
                        WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                        monitor.worked(1);
                        IDE.openEditor(getActivePage(), dataTableFile, DataTableEditor.ID, true);
                        monitor.done();
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            PluginLogger.logError("datatable.error.creation", e.getTargetException());
            return false;
        } catch (InterruptedException e) {
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
