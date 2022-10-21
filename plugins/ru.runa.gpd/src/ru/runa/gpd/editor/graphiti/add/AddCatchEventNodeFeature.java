package ru.runa.gpd.editor.graphiti.add;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.impl.LocationContext;
import org.eclipse.graphiti.mm.algorithms.Image;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import ru.runa.gpd.editor.GEFConstants;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.bpmn.CatchEventNode;

public class AddCatchEventNodeFeature extends AddEventNodeFeature implements GEFConstants {

    @Override
    public boolean canAdd(IAddContext context) {
        if (super.canAdd(context)) {
            return true;
        }
        GraphElement container = (GraphElement) getBusinessObjectForPictogramElement(context.getTargetContainer());
        return CatchEventNode.isBoundaryEventInParent(container);
    }

    @Override
    public PictogramElement add(IAddContext context) {
        GraphElement parent = (GraphElement) getBusinessObjectForPictogramElement(context.getTargetContainer());
        CatchEventNode catchEventNode = (CatchEventNode) context.getNewObject();
        if (CatchEventNode.isBoundaryEventInParent(parent)) {
            Dimension bounds = getBounds(context);
            ((LocationContext) context).setX(((Node) parent).getConstraint().width - catchEventNode.getConstraint().width / 2);
            ((LocationContext) context).setY(((Node) parent).getConstraint().height - catchEventNode.getConstraint().height / 2);
            ContainerShape parentShape = context.getTargetContainer();
            IPeCreateService createService = Graphiti.getPeCreateService();
            IGaService gaService = Graphiti.getGaService();
            ContainerShape containerShape = createService.createContainerShape(parentShape, true);

            String imageId = "graph/" + catchEventNode.getEventNodeType().getImageName(true, true, catchEventNode.isInterruptingBoundaryEvent());
            Image image = gaService.createImage(containerShape, imageId);
            gaService.setLocationAndSize(image, context.getX(), context.getY(), bounds.width, bounds.height);

            link(containerShape, catchEventNode);
            createService.createChopboxAnchor(containerShape);
            layoutPictogramElement(containerShape);
            return containerShape;
        }
        return super.add(context);
    }

}
