package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.impl.AbstractDirectEditingFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import ru.runa.gpd.editor.graphiti.CustomUndoRedoFeature;
import ru.runa.gpd.lang.model.GraphElement;

public class DirectEditDescriptionFeature extends AbstractDirectEditingFeature implements CustomUndoRedoFeature {
    private String undoDescription;
    private String redoDescription;

    public DirectEditDescriptionFeature(final IFeatureProvider fp) {
        super(fp);
    }

    @Override
    public int getEditingType() {
        return TYPE_MULTILINETEXT;
    }

    @Override
    public String getInitialValue(final IDirectEditingContext context) {
        PictogramElement pe = context.getPictogramElement();
        GraphElement element = (GraphElement) getBusinessObjectForPictogramElement(pe);
        return element.getDescription();
    }

    @Override
    public boolean canDirectEdit(IDirectEditingContext context) {
        PictogramElement pe = context.getPictogramElement();
        Object bo = getBusinessObjectForPictogramElement(pe);
        return bo instanceof GraphElement;
    }

    @Override
    public void setValue(String value, IDirectEditingContext context) {
        PictogramElement pe = context.getPictogramElement();
        GraphElement element = (GraphElement) getBusinessObjectForPictogramElement(pe);
        undoDescription = element.getDescription();
        element.setDescription(value);
        updatePictogramElement(((Shape) pe).getContainer());
    }

    @Override
    public boolean canUndo(IContext context) {
        return undoDescription != null;
    }

    @Override
    public void postUndo(IContext context) {
        if (context instanceof IDirectEditingContext) {
            PictogramElement pe = ((IDirectEditingContext) context).getPictogramElement();
            GraphElement element = (GraphElement) getBusinessObjectForPictogramElement(pe);
            redoDescription = element.getDescription();
            element.setDescription(undoDescription);
        }

    }

    @Override
    public boolean canRedo(IContext context) {
        return redoDescription != null;
    }

    @Override
    public void postRedo(IContext context) {
        if (context instanceof IDirectEditingContext) {
            PictogramElement pe = ((IDirectEditingContext) context).getPictogramElement();
            GraphElement element = (GraphElement) getBusinessObjectForPictogramElement(pe);
            element.setDescription(redoDescription);
        }

    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

}
