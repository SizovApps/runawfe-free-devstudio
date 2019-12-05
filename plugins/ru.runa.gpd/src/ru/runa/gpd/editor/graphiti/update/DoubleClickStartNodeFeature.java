package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.jface.window.Window;
import ru.runa.gpd.lang.model.EventTrigger;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;

public class DoubleClickStartNodeFeature extends DoubleClickFormNodeFeature {

    @Override
    public boolean canExecute(ICustomContext context) {
        return fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement()) instanceof StartState && super.canExecute(context);
    }

    @Override
    public void execute(ICustomContext context) {
        StartState startNode = (StartState) fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement());
        EventTrigger eventTrigger = startNode.getEventTrigger();
        if (eventTrigger.getEventType() != null) {
            MessageNodeDialog dialog = new MessageNodeDialog(startNode.getProcessDefinition(), eventTrigger.getVariableMappings(), false,
                    startNode.getName());
            if (dialog.open() != Window.CANCEL) {
                eventTrigger.setVariableMappings(dialog.getVariableMappings());
            }
        } else {
            super.execute(context);
        }
    }

}
