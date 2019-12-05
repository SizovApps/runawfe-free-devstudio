package ru.runa.gpd.lang.action;

import org.eclipse.jface.action.IAction;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.ui.dialog.DurationEditDialog;
import ru.runa.gpd.util.Duration;

public class EndTokenEditTTLAction extends BaseModelActionDelegate {

    @Override
    public void run(IAction action) {
        EndTokenState endNode = getSelection();
        DurationEditDialog dialog = new DurationEditDialog(endNode.getProcessDefinition(), endNode.getEventTrigger().getTtlDuration());
        Duration result = (Duration) dialog.openDialog();
        if (result != null) {
            endNode.getEventTrigger().setTtlDuration(result);
        }
    }
}
