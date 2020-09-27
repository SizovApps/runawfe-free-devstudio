package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IMoveConnectionDecoratorContext;
import org.eclipse.graphiti.features.impl.DefaultMoveConnectionDecoratorFeature;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import ru.runa.gpd.editor.graphiti.CustomUndoRedoFeature;
import ru.runa.gpd.editor.graphiti.GaProperty;
import ru.runa.gpd.editor.graphiti.PropertyUtil;
import ru.runa.gpd.lang.model.Transition;

public class MoveTransitionLabelFeature extends DefaultMoveConnectionDecoratorFeature implements CustomUndoRedoFeature {

    private Point undoDecoratorPoint;
    private Point redoDecoratorPoint;

    public MoveTransitionLabelFeature(IFeatureProvider fp) {
        super(fp);
    }

    @Override
    public void moveConnectionDecorator(IMoveConnectionDecoratorContext context) {
        super.moveConnectionDecorator(context);
        Connection connection = context.getConnectionDecorator().getConnection();
        Transition transition = (Transition) getBusinessObjectForPictogramElement(connection);
        if (transition.getLabelLocation() != null) {
            undoDecoratorPoint = transition.getLabelLocation().getCopy();
        }
        transition.setLabelLocation(new Point(context.getX(), context.getY()));
    }

    @Override
    public boolean canMoveConnectionDecorator(IMoveConnectionDecoratorContext context) {
        ConnectionDecorator decorator = context.getConnectionDecorator();
        return !(PropertyUtil.hasProperty(decorator, GaProperty.CLASS, GaProperty.ACTION_ICON)
                || PropertyUtil.hasProperty(decorator, GaProperty.ID, GaProperty.TRANSITION_NUMBER)
                || PropertyUtil.hasProperty(decorator, GaProperty.ID, GaProperty.TRANSITION_COLOR_MARKER))
                && super.canMoveConnectionDecorator(context);
    }


    @Override
    public void postUndo(IContext context) {
        if (context instanceof IMoveConnectionDecoratorContext) {
            Connection connection = ((IMoveConnectionDecoratorContext) context).getConnectionDecorator().getConnection();
            Transition transition = (Transition) getBusinessObjectForPictogramElement(connection);
            redoDecoratorPoint = transition.getLabelLocation() == null ? null : transition.getLabelLocation().getCopy();
            transition.setLabelLocation(undoDecoratorPoint);
        }
    }

    @Override
    public boolean canRedo(IContext context) {
        return redoDecoratorPoint != null;
    }

    @Override
    public void postRedo(IContext context) {
        if (context instanceof IMoveConnectionDecoratorContext) {
            Connection connection = ((IMoveConnectionDecoratorContext) context).getConnectionDecorator().getConnection();
            Transition transition = (Transition) getBusinessObjectForPictogramElement(connection);
            undoDecoratorPoint = transition.getLabelLocation() == null ? null : transition.getLabelLocation().getCopy();
            transition.setLabelLocation(redoDecoratorPoint);
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

}
