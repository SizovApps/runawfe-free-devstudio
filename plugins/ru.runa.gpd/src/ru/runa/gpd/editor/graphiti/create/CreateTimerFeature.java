package ru.runa.gpd.editor.graphiti.create;

import org.eclipse.graphiti.features.context.ICreateContext;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.ITimed;
import ru.runa.gpd.lang.model.Timer;
import ru.runa.gpd.lang.model.bpmn.IBoundaryEventContainer;

public class CreateTimerFeature extends CreateElementFeature {
    @Override
    public boolean canCreate(ICreateContext context) {
        if (super.canCreate(context)) {
            return true;
        }
        GraphElement parentObject = (GraphElement) getBusinessObjectForPictogramElement(context.getTargetContainer());
        return parentObject instanceof ITimed && parentObject.getChildrenRecursive(Timer.class).isEmpty()
                && !(parentObject.getUiParentContainer() instanceof IBoundaryEventContainer);
    }
}
