package ru.runa.gpd.bizagi.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import ru.runa.gpd.bizagi.resource.Messages;

public class ExportBpmnWizard extends Wizard implements IExportWizard {

    private ExportBpmnWizardPage mainPage;

    @Override
    public boolean performFinish() {
        return mainPage.finish();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(Messages.getString("ExportBpmnWizardPage.page.title"));
        mainPage = new ExportBpmnWizardPage(selection);
    }

    @Override
    public void addPages() {
        addPage(mainPage);
    }

}
