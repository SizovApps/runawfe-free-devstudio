package ru.runa.gpd.editor.graphiti.update;

import com.google.common.base.Objects;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Image;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import ru.runa.gpd.editor.graphiti.GaProperty;
import ru.runa.gpd.editor.graphiti.PropertyUtil;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;

public class UpdateEndTokenNodeFeature extends UpdateFeatureWithTextDecorator {

    @Override
    public IReason updateNeeded(IUpdateContext context) {
        // retrieve name from pictogram element
        PictogramElement pe = context.getPictogramElement();
        // retrieve name from business model
        EndTokenState bo = (EndTokenState) getBusinessObjectForPictogramElement(pe);
        String name = PropertyUtil.findTextValueRecursive(pe, GaProperty.NAME);
        if (!Objects.equal(name, bo.getName())) {
            return Reason.createTrueReason();
        }
        if (!Objects.equal(((Image) pe.getGraphicsAlgorithm()).getId(), getImageId(bo))) {
            return Reason.createTrueReason();
        }
        return Reason.createFalseReason();
    }

    @Override
    public boolean update(IUpdateContext context) {
        super.update(context);
        ContainerShape containerShape = (ContainerShape) context.getPictogramElement();
        EndTokenState endNode = (EndTokenState) getBusinessObjectForPictogramElement(containerShape);
        String imageId = getImageId(endNode);
        if (!Objects.equal(((Image) containerShape.getGraphicsAlgorithm()).getId(), imageId)) {
            Image oldImage = (Image) containerShape.getGraphicsAlgorithm();
            Image newImage = Graphiti.getGaService().createImage(containerShape, imageId);
            Graphiti.getGaService().setLocationAndSize(newImage, oldImage.getX(), oldImage.getY(), oldImage.getWidth(), oldImage.getHeight());
        }
        return true;
    }

    private String getImageId(EndTokenState endNode) {
        EventNodeType eventType = endNode.getEventTrigger().getEventType();
        return "graph/" + (eventType == null ? "endtoken.png" : eventType.getImageName("end", false, false));
    }

}
