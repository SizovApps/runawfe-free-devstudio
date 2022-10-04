package ru.runa.gpd.bizagi.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import ru.runa.gpd.bizagi.resource.Messages;

public class ImportBizagiBpmnWizard extends Wizard implements IImportWizard {

    private ImportBizagiBpmnWizardPage mainPage;

    @Override
    public boolean performFinish() {
        return mainPage.performFinish();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(Messages.getString("ImportBpmnWizardPage.page.title"));
        mainPage = new ImportBizagiBpmnWizardPage(Messages.getString("ImportBpmnWizardPage.page.title"), selection);
    }

    @Override
    public void addPages() {
        addPage(mainPage);
    }

}
