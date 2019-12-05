package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.jface.window.Window;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.lang.model.EventTrigger;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;

public class DoubleClickEndTokenNodeFeature extends DoubleClickElementFeature {

    @Override
    public boolean canExecute(ICustomContext context) {
        return fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement()) instanceof EndTokenState && super.canExecute(context);
    }

    @Override
    public void execute(ICustomContext context) {
        EndTokenState endNode = (EndTokenState) fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement());
        EventTrigger eventTrigger = endNode.getEventTrigger();
        if (eventTrigger.getEventType() != null) {
            MessageNodeDialog dialog = new MessageNodeDialog(endNode.getProcessDefinition(), eventTrigger.getVariableMappings(), true,
                    endNode.getName());
            if (dialog.open() != Window.CANCEL) {
                eventTrigger.setVariableMappings(dialog.getVariableMappings());
            }
        }
    }

}
