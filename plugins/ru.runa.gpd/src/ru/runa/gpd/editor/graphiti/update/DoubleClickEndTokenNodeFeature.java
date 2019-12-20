package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.jface.window.Window;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;

public class DoubleClickEndTokenNodeFeature extends DoubleClickElementFeature {

    @Override
    public boolean canExecute(ICustomContext context) {
        return fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement()) instanceof EndTokenState && super.canExecute(context);
    }

    @Override
    public void execute(ICustomContext context) {
        EndTokenState endNode = (EndTokenState) fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement());
        if (endNode.isEndWithEvent()) {
            MessageNodeDialog dialog = new MessageNodeDialog(endNode.getProcessDefinition(), endNode.getVariableMappings(), true, endNode.getName());
            if (dialog.open() != Window.CANCEL) {
                endNode.setVariableMappings(dialog.getVariableMappings());
            }
        }
    }

}
