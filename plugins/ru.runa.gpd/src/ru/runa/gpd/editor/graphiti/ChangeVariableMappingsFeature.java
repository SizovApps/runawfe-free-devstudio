package ru.runa.gpd.editor.graphiti;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import ru.runa.gpd.lang.model.VariableMappingsHolder;
import ru.runa.gpd.util.VariableMapping;

public class ChangeVariableMappingsFeature extends ChangePropertyFeature<VariableMappingsHolder, List<VariableMapping>> {

    public ChangeVariableMappingsFeature(VariableMappingsHolder target, List<VariableMapping> newValue) {
        super(target, new ArrayList<>(target.getVariableMappings()), newValue);
    }

    @Override
    protected void undo(IContext context) {
        target.setVariableMappings(oldValue);
    }

    @Override
    public void execute(ICustomContext context) {
        target.setVariableMappings(newValue);
    }

}
