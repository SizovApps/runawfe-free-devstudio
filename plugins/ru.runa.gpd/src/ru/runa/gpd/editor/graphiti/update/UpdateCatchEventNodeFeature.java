package ru.runa.gpd.editor.graphiti.update;

import com.google.common.base.Objects;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Image;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import ru.runa.gpd.lang.model.bpmn.CatchEventNode;

public class UpdateCatchEventNodeFeature extends UpdateEventNodeFeature {

    @Override
    public IReason updateNeeded(IUpdateContext context) {
        CatchEventNode catchEventNode = (CatchEventNode) getBusinessObjectForPictogramElement(context.getPictogramElement());
        if (catchEventNode.isBoundaryEvent()) {
            if (!Objects.equal(((Image) ((ContainerShape) context.getPictogramElement()).getGraphicsAlgorithm()).getId(),
                    getImageId(catchEventNode))) {
                return Reason.createTrueReason();
            }
        }
        return super.updateNeeded(context);
    }

}
