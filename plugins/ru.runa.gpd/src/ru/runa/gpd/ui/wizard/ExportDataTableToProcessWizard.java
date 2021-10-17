package ru.runa.gpd.ui.wizard;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import ru.runa.gpd.Localization;

public class ExportDataTableToProcessWizard extends Wizard implements IExportWizard {
    private ExportDataTableToProcessWizardPage page;
    private IStructuredSelection selection;
    private IFile dataTable;

    public ExportDataTableToProcessWizard(IFile dataTable) {
        super();
        this.dataTable = dataTable;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
        setWindowTitle(Localization.getString("ExportDataTableToProcessWizard.wizard.title"));
    }

    @Override
    public void addPages() {
        page = new ExportDataTableToProcessWizardPage(selection, dataTable);
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        return page.finish();
    }
}
