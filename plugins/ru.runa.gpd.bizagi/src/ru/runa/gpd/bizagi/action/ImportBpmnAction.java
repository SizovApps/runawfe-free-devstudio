package ru.runa.gpd.bizagi.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import ru.runa.gpd.bizagi.wizard.ImportBpmnWizard;
import ru.runa.gpd.ui.action.BaseActionDelegate;
import ru.runa.gpd.ui.wizard.CompactWizardDialog;

public class ImportBpmnAction extends BaseActionDelegate {

    @Override
    public void run(IAction action) {
        ImportBpmnWizard wizard = new ImportBpmnWizard();
        wizard.init(PlatformUI.getWorkbench(), getStructuredSelection());
        CompactWizardDialog dialog = new CompactWizardDialog(wizard);
        dialog.open();
    }

}
