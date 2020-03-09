package ru.runa.gpd.bizagi.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import ru.runa.gpd.bizagi.wizard.ExportBpmnWizard;
import ru.runa.gpd.ui.action.BaseActionDelegate;
import ru.runa.gpd.ui.wizard.CompactWizardDialog;

public class ExportBpmnAction extends BaseActionDelegate {

    @Override
    public void run(IAction action) {
        ExportBpmnWizard wizard = new ExportBpmnWizard();
        wizard.init(PlatformUI.getWorkbench(), getStructuredSelection());
        CompactWizardDialog dialog = new CompactWizardDialog(wizard);
        dialog.open();
    }

}
