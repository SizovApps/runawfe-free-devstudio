package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.lang.model.bpmn.EndEventType;

public class ChangeEndEventTypeFeature extends AbstractCustomFeature {

    private EndEventType newType;

    public ChangeEndEventTypeFeature(IFeatureProvider fp, EndEventType newType) {
        super(fp);
        this.newType = newType;
    }

    @Override
    public boolean canExecute(ICustomContext context) {
        return getFeatureProvider().getBusinessObjectForPictogramElement(context.getPictogramElements()[0]) instanceof EndTokenState;
    }

    @Override
    public void execute(ICustomContext context) {
        EndTokenState endTokenNode = (EndTokenState) getFeatureProvider().getBusinessObjectForPictogramElement(context.getPictogramElements()[0]);
        endTokenNode.setEventType(newType);
    }
}
