package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import ru.runa.gpd.lang.model.EventHolder;
import ru.runa.gpd.lang.model.FormNode;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;

public class ChangeEventTypeFeature extends AbstractCustomFeature {

    private EventNodeType newType;

    public ChangeEventTypeFeature(IFeatureProvider fp, EventNodeType newType) {
        super(fp);
        this.newType = newType;
    }

    @Override
    public boolean canExecute(ICustomContext context) {
        return getFeatureProvider().getBusinessObjectForPictogramElement(context.getPictogramElements()[0]) instanceof EventHolder;
    }

    @Override
    public void execute(ICustomContext context) {
        EventHolder eventHolder = (EventHolder) getFeatureProvider().getBusinessObjectForPictogramElement(context.getPictogramElements()[0]);
        if (eventHolder instanceof StartState && newType == null) {
            StartState startNode = (StartState) eventHolder;
            startNode.getProcessDefinition().setDefaultStartNode(startNode);
        } else {
            eventHolder.getEventTrigger().setEventType(newType);
        }
        if (eventHolder instanceof FormNode) {
            ((FormNode) eventHolder).deleteFiles();
        }
    }

}
