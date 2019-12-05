package ru.runa.gpd.lang.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;

public class EndTokenConfAction extends BaseModelActionDelegate {

    @Override
    public void run(IAction action) {
        EndTokenState endNode = getSelection();
        MessageNodeDialog dialog = new MessageNodeDialog(endNode.getProcessDefinition(), endNode.getEventTrigger().getVariableMappings(), true,
                endNode.getName());
        if (dialog.open() != Window.CANCEL) {
            endNode.getEventTrigger().setVariableMappings(dialog.getVariableMappings());
        }
    }
}
