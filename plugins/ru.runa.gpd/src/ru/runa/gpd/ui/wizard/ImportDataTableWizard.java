package ru.runa.gpd.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import ru.runa.gpd.Localization;

public class ImportDataTableWizard extends Wizard implements IImportWizard {

    private ImportDataTableWizardPage mainPage;

    @Override
    public boolean performFinish() {
        return mainPage.finish();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(Localization.getString("ImportDataTableWizard.wizard.title"));
        mainPage = new ImportDataTableWizardPage(selection);
        mainPage.setWorkbench(workbench);
    }

    @Override
    public void addPages() {
        addPage(mainPage);
    }

}
