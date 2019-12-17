package ru.runa.gpd.lang.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;

public class ClearStartStateTimerAction extends BaseModelActionDelegate {
    
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        super.selectionChanged(action, selection);
        StartState startState = getSelection();
        if (startState != null) {
            action.setChecked(!startState.isStartByTimer());
        }
    }

    @Override
    public void run(IAction action) {
        StartState startState = (StartState) getSelection();
        startState.setTimerEventDefinition(null);
        StartState defaultStartState = startState.getProcessDefinition().getDefaultStartNode();
        if (defaultStartState != null) {
            if (startState != defaultStartState) {
                if (!startState.isStartByEvent()) {
                    startState.getEventTrigger().setEventType(EventNodeType.message);
                }
                startState.deleteFiles();
            }
        }
    }
}
