package ru.runa.gpd.lang.model;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.property.DurationPropertyDescriptor;
import ru.runa.gpd.util.Duration;
import ru.runa.gpd.util.VariableMapping;

public abstract class MessageNode extends Node implements VariableMappingsValidator {
    protected final List<VariableMapping> variableMappings = new ArrayList<VariableMapping>();
    private Duration ttlDuration = new Duration("0 minutes");

    @Override
    public List<VariableMapping> getVariableMappings() {
        return variableMappings;
    }

    public void setVariableMappings(List<VariableMapping> variablesList) {
        this.variableMappings.clear();
        this.variableMappings.addAll(variablesList);
        setDirty();
    }

    public Duration getTtlDuration() {
        return ttlDuration;
    }

    public void setTtlDuration(Duration ttlDuration) {
        Duration old = this.ttlDuration;
        this.ttlDuration = ttlDuration;
        firePropertyChange(PROPERTY_TTL, old, ttlDuration);
    }

    @Override
    public void populateCustomPropertyDescriptors(List<IPropertyDescriptor> descriptors) {
        super.populateCustomPropertyDescriptors(descriptors);
        if (this instanceof ISendMessageNode) {
            descriptors.add(new DurationPropertyDescriptor(PROPERTY_TTL, getProcessDefinition(), getTtlDuration(),
                    Localization.getString("property.message.ttl")));
        }
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_TTL.equals(id)) {
            return ttlDuration;
        }
        return super.getPropertyValue(id);
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        if (PROPERTY_TTL.equals(id)) {
            setTtlDuration((Duration) value);
            return;
        }
        super.setPropertyValue(id, value);
    }

    @Override
    protected boolean allowLeavingTransition(List<Transition> transitions) {
        return super.allowLeavingTransition(transitions) && transitions.size() == 0;
    }

    @Override
    public void validate(List<ValidationError> errors, IFile definitionFile) {
        super.validate(errors, definitionFile);
        validate(errors, definitionFile, () -> this);
    }

    @Override
    protected void fillCopyCustomFields(GraphElement copy) {
        super.fillCopyCustomFields(copy);
        ((MessageNode) copy).setTtlDuration(getTtlDuration());
        for (VariableMapping mapping : getVariableMappings()) {
            ((MessageNode) copy).getVariableMappings().add(mapping.getCopy());
        }
    }

    @Override
    public void validateOnEmptyRules(List<ValidationError> errors) {
        errors.add(ValidationError.createLocalizedWarning(this, "message.selectorRulesEmpty"));
    }

}
