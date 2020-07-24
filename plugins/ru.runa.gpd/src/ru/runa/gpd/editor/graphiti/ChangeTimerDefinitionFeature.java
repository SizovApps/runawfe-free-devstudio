package ru.runa.gpd.editor.graphiti;

import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import ru.runa.gpd.lang.model.StartState;

public class ChangeTimerDefinitionFeature extends ChangePropertyFeature<StartState, String> {

    public ChangeTimerDefinitionFeature(StartState target, String newValue) {
        super(target, target.getTimerEventDefinition(), newValue);
    }

    public ChangeTimerDefinitionFeature(StartState target, String oldValue, String newValue) {
        super(target, oldValue, newValue);
    }

    @Override
    protected void undo(IContext context) {
        target.setTimerEventDefinition(oldValue);
    }

    @Override
    public void execute(ICustomContext context) {
        target.setTimerEventDefinition(newValue);
    }

}
