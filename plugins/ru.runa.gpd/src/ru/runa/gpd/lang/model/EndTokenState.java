package ru.runa.gpd.lang.model;

import com.google.common.base.Objects;
import java.util.List;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.bpmn.AbstractEndTextDecorated;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;

public class EndTokenState extends AbstractEndTextDecorated implements EventThrower {

    private EndTokenSubprocessDefinitionBehavior subprocessDefinitionBehavior = EndTokenSubprocessDefinitionBehavior.BACK_TO_BASE_PROCESS;

    @Override
    protected void populateCustomPropertyDescriptors(List<IPropertyDescriptor> descriptors) {
        super.populateCustomPropertyDescriptors(descriptors);
        descriptors.add(
                new ComboBoxPropertyDescriptor(PROPERTY_EVENT_TYPE, Localization.getString("property.eventType"), EventTrigger.EVENT_TYPE_NAMES));
        if (getProcessDefinition() instanceof SubprocessDefinition) {
            descriptors.add(new ComboBoxPropertyDescriptor(PROPERTY_END_TOKEN_BEHAVIOR, Localization.getString("EndTokenState.property.behaviour"),
                    EndTokenSubprocessDefinitionBehavior.getLabels()));
        }
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_EVENT_TYPE.equals(id)) {
            if (eventTrigger.getEventType() == null) {
                return Integer.valueOf(0);
            }
            return eventTrigger.getEventType().ordinal() + 1;
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
            eventTrigger.setEventType(index > 0 ? EventNodeType.values()[index - 1] : null);
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

    protected final EventTrigger eventTrigger = new EventTrigger(this);

    @Override
    public EventTrigger getEventTrigger() {
        return eventTrigger;
    }

    @Override
    public boolean testAttribute(Object target, String name, String value) {
        if ("isEventTypeDefined".equals(name)) {
            return Objects.equal(value, String.valueOf(getEventTrigger().getEventType() != null));
        }
        return super.testAttribute(target, name, value);
    }

}
