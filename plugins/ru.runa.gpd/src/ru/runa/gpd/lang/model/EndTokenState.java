package ru.runa.gpd.lang.model;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.bpmn.AbstractEndTextDecorated;
import ru.runa.gpd.lang.model.bpmn.EndEventType;
import ru.runa.gpd.util.Duration;
import ru.runa.gpd.util.VariableMapping;

public class EndTokenState extends AbstractEndTextDecorated {

    private EndTokenSubprocessDefinitionBehavior subprocessDefinitionBehavior = EndTokenSubprocessDefinitionBehavior.BACK_TO_BASE_PROCESS;

    @Override
    protected void populateCustomPropertyDescriptors(List<IPropertyDescriptor> descriptors) {
        super.populateCustomPropertyDescriptors(descriptors);
        descriptors.add(
                new ComboBoxPropertyDescriptor(PROPERTY_EVENT_TYPE, Localization.getString("property.eventType"), EndEventType.LABELS));
        if (getProcessDefinition() instanceof SubprocessDefinition) {
            descriptors.add(new ComboBoxPropertyDescriptor(PROPERTY_END_TOKEN_BEHAVIOR, Localization.getString("EndTokenState.property.behaviour"),
                    EndTokenSubprocessDefinitionBehavior.getLabels()));
        }
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_EVENT_TYPE.equals(id)) {
            return getEventType().ordinal();
        }
        if (PROPERTY_END_TOKEN_BEHAVIOR.equals(id)) {
            return subprocessDefinitionBehavior.ordinal();
        }
        return super.getPropertyValue(id);
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        if (PROPERTY_EVENT_TYPE.equals(id)) {
            int index = ((Integer) value).intValue();
            setEventType(EndEventType.values()[index]);
        } else if (PROPERTY_END_TOKEN_BEHAVIOR.equals(id)) {
            setSubprocessDefinitionBehavior(EndTokenSubprocessDefinitionBehavior.values()[(Integer) value]);
        } else {
            super.setPropertyValue(id, value);
        }
    }

    public EndTokenSubprocessDefinitionBehavior getSubprocessDefinitionBehavior() {
        return subprocessDefinitionBehavior;
    }

    public void setSubprocessDefinitionBehavior(EndTokenSubprocessDefinitionBehavior subprocessDefinitionBehavior) {
        EndTokenSubprocessDefinitionBehavior old = this.subprocessDefinitionBehavior;
        this.subprocessDefinitionBehavior = subprocessDefinitionBehavior;
        firePropertyChange(PROPERTY_END_TOKEN_BEHAVIOR, old, this.subprocessDefinitionBehavior);
    }

    @Override
    protected void fillCopyCustomFields(GraphElement copy) {
        super.fillCopyCustomFields(copy);
        ((EndTokenState) copy).setSubprocessDefinitionBehavior(getSubprocessDefinitionBehavior());
    }

    @Override
    public boolean testAttribute(Object target, String name, String value) {
        if ("isEventTypeDefined".equals(name)) {
            return Objects.equal(value, String.valueOf(isEndWithEvent()));
        }
        return super.testAttribute(target, name, value);
    }

    public boolean isEndWithEvent() {
        return eventType != EndEventType.blank;
    }

    private EndEventType eventType = EndEventType.blank;

    public EndEventType getEventType() {
        return eventType;
    }

    public void setEventType(EndEventType eventType) {
        if (eventType != this.eventType) {
            EndEventType old = this.eventType;
            this.eventType = eventType;
            firePropertyChange(PROPERTY_EVENT_TYPE, old, this.eventType);
        }
    }

    private final List<VariableMapping> variableMappings = new ArrayList<VariableMapping>();

    public List<VariableMapping> getVariableMappings() {
        return variableMappings;
    }

    public void setVariableMappings(List<VariableMapping> variablesList) {
        this.variableMappings.clear();
        this.variableMappings.addAll(variablesList);
        setDirty();
    }

    private Duration ttlDuration = new Duration("1 days");

    public Duration getTtlDuration() {
        return ttlDuration;
    }

    public void setTtlDuration(Duration ttlDuration) {
        Duration old = this.ttlDuration;
        this.ttlDuration = ttlDuration;
        firePropertyChange(PROPERTY_TTL, old, ttlDuration);
    }

}
