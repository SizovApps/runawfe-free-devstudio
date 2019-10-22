package ru.runa.gpd.editor.graphiti.update;

import com.google.common.base.Objects;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Image;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.services.Graphiti;
import ru.runa.gpd.lang.model.ITimed;
import ru.runa.gpd.lang.model.Timer;

public class UpdateTimerFeature extends UpdateFeature {

    @Override
    public IReason updateNeeded(IUpdateContext context) {
        Timer timer = (Timer) getBusinessObjectForPictogramElement(context.getPictogramElement());
        if (timer.getParent() instanceof ITimed) {
            if (!Objects.equal(((Image) ((ContainerShape) context.getPictogramElement()).getGraphicsAlgorithm()).getId(), getImageId(timer))) {
                return Reason.createTrueReason();
            }
        }
        return Reason.createFalseReason();
    }

    @Override
    public boolean update(IUpdateContext context) {
        ContainerShape containerShape = (ContainerShape) context.getPictogramElement();
        Timer timer = (Timer) getBusinessObjectForPictogramElement(containerShape);
        String imageId = getImageId(timer);
        if (!Objects.equal(((Image) containerShape.getGraphicsAlgorithm()).getId(), imageId)) {
            Image oldImage = (Image) containerShape.getGraphicsAlgorithm();
            Image newImage = Graphiti.getGaService().createImage(containerShape, imageId);
            Graphiti.getGaService().setLocationAndSize(newImage, oldImage.getX(), oldImage.getY(), oldImage.getWidth(), oldImage.getHeight());
        }
        return true;
    }

    private String getImageId(Timer timer) {
        return "graph/boundary_timer" + (!timer.isInterruptingBoundaryEvent() ? "_non_interrupting" : "") + ".png";
    }
}
