package ru.runa.gpd.editor.graphiti.update;

import com.google.common.base.Objects;
import java.util.List;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import ru.runa.gpd.editor.graphiti.ChangeVariableMappingsFeature;
import ru.runa.gpd.editor.graphiti.UndoRedoUtil;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;
import ru.runa.gpd.util.VariableMapping;

public class DoubleClickEndTokenNodeFeature extends DoubleClickElementFeature {

    @Override
    public boolean canExecute(ICustomContext context) {
        return fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement()) instanceof EndTokenState && super.canExecute(context);
    }

    @Override
    public void execute(ICustomContext context) {
        EndTokenState endNode = (EndTokenState) fp.getBusinessObjectForPictogramElement(context.getInnerPictogramElement());
        if (endNode.isEndWithEvent()) {
            List<VariableMapping> oldMappings = endNode.getVariableMappings();
            MessageNodeDialog dialog = new MessageNodeDialog(endNode.getProcessDefinition(), endNode.getVariableMappings(), true, endNode.getName());
            if (dialog.open() != Window.CANCEL) {
                if (!Objects.equal(dialog.getVariableMappings(), oldMappings)) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            UndoRedoUtil.executeFeature(new ChangeVariableMappingsFeature(endNode, dialog.getVariableMappings()));
                        }
                    });
                }
            }
        }
    }

}
