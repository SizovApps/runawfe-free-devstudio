package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.jface.window.Window;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;
import ru.runa.gpd.ui.dialog.StartStateTimerDialog;

public class DoubleClickStartNodeFeature extends DoubleClickFormNodeFeature {

    @Override
    public boolean canExecute(ICustomContext context) {
        return fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement()) instanceof StartState && super.canExecute(context);
    }

    @Override
    public void execute(ICustomContext context) {
        StartState startNode = (StartState) fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement());
        if (startNode.isStartByEvent()) {
            if (startNode.isStartByTimer()) {
                String newTimerDefinition = new StartStateTimerDialog(startNode.getTimerEventDefinition()).openDialog();
                if (newTimerDefinition != null) {
                    startNode.setTimerEventDefinition(newTimerDefinition);
                }
            } else {
                MessageNodeDialog dialog = new MessageNodeDialog(startNode.getProcessDefinition(), startNode.getVariableMappings(), false,
                        startNode.getName());
                if (dialog.open() != Window.CANCEL) {
                    startNode.setVariableMappings(dialog.getVariableMappings());
                }
            }
        } else {
            super.execute(context);
        }
    }

}
