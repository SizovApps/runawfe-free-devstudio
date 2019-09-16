package ru.runa.gpd.lang.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.ui.dialog.StartStateTimerDialog;

public class EditStartStateTimerAction extends BaseModelActionDelegate {

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        super.selectionChanged(action, selection);
        StartState startState = getSelection();
        if (startState != null) {
            action.setChecked(startState.isStartByTimer());
        }
    }

    @Override
    public void run(IAction action) {
        if (action.isChecked()) {
            StartState startState = getSelection();
            String newTimerDefinition = new StartStateTimerDialog(startState.getTimerEventDefinition()).openDialog();
            if (newTimerDefinition != null) {
                startState.setTimerEventDefinition(newTimerDefinition);
            }
            action.setChecked(startState.getTimerEventDefinition() != null);
        }
    }
}
