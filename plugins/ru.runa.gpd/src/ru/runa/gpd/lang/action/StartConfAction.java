package ru.runa.gpd.lang.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;

public class StartConfAction extends BaseModelActionDelegate {

    @Override
    public void run(IAction action) {
        StartState startNode = getSelection();
        MessageNodeDialog dialog = new MessageNodeDialog(startNode.getProcessDefinition(), startNode.getEventTrigger().getVariableMappings(), false,
                startNode.getName());
        if (dialog.open() != Window.CANCEL) {
            startNode.getEventTrigger().setVariableMappings(dialog.getVariableMappings());
        }
    }
}
