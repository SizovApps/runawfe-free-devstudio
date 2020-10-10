package ru.runa.gpd.editor.graphiti.update;

import com.google.common.base.Objects;
import java.util.List;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import ru.runa.gpd.editor.graphiti.ChangeTimerDefinitionFeature;
import ru.runa.gpd.editor.graphiti.ChangeVariableMappingsFeature;
import ru.runa.gpd.editor.graphiti.UndoRedoUtil;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.ui.dialog.MessageNodeDialog;
import ru.runa.gpd.ui.dialog.StartStateTimerDialog;
import ru.runa.gpd.util.VariableMapping;

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
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            UndoRedoUtil.executeFeature(new ChangeTimerDefinitionFeature(startNode, newTimerDefinition));
                        }
                    });
                }
            } else {
                List<VariableMapping> oldMappings = startNode.getVariableMappings();
                MessageNodeDialog dialog = new MessageNodeDialog(startNode.getProcessDefinition(), startNode.getVariableMappings(), false,
                        startNode.getName());
                if (dialog.open() != Window.CANCEL) {
                    if (!Objects.equal(dialog.getVariableMappings(), oldMappings)) {
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                UndoRedoUtil.executeFeature(new ChangeVariableMappingsFeature(startNode, dialog.getVariableMappings()));
                            }
                        });
                    }
                }
            }
        } else {
            super.execute(context);
        }
    }

}
