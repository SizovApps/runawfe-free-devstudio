package ru.runa.gpd.lang.model;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import ru.runa.gpd.Localization;
import ru.runa.gpd.editor.graphiti.HasTextDecorator;
import ru.runa.gpd.editor.graphiti.TextDecoratorEmulation;
import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;

public class StartState extends FormNode implements HasTextDecorator, EventCatcher {

    protected TextDecoratorEmulation decoratorEmulation;
    protected String timerEventDefinition;

    public StartState() {
        decoratorEmulation = new TextDecoratorEmulation(this);
    }

    @Override
    protected boolean allowArrivingTransition(Node source, List<Transition> transitions) {
        return false;
    }

    @Override
    protected boolean allowLeavingTransition(List<Transition> transitions) {
        return true;
    }

    @Override
    public boolean isSwimlaneDisabled() {
        return getProcessDefinition() instanceof SubprocessDefinition || !isStartByTimer() && isStartByEvent();
    }

    @Override
    public void validate(List<ValidationError> errors, IFile definitionFile) {
        super.validate(errors, definitionFile);
        if (hasForm()) {
            if (getProcessDefinition() instanceof SubprocessDefinition) {
                errors.add(ValidationError.createLocalizedError(this, "startState.formIsNotUsableInEmbeddedSubprocess"));
            } else if (getProcessDefinition().getAccessType() == ProcessDefinitionAccessType.OnlySubprocess) {
                errors.add(ValidationError.createLocalizedError(this, "startState.formIsNotUsableInSubprocess"));
            }
        }
        if (hasFormScript()) {
            if (getProcessDefinition() instanceof SubprocessDefinition) {
                errors.add(ValidationError.createLocalizedError(this, "startState.formScriptIsNotUsableInEmbeddedSubprocess"));
            } else if (getProcessDefinition().getAccessType() == ProcessDefinitionAccessType.OnlySubprocess) {
                errors.add(ValidationError.createLocalizedError(this, "startState.formScriptIsNotUsableInSubprocess"));
            }
        }
        if (hasFormValidation() && getProcessDefinition() instanceof SubprocessDefinition) {
            errors.add(ValidationError.createLocalizedError(this, "startState.formValidationIsNotUsableInEmbeddedSubprocess"));
        }
        if (isSwimlaneDisabled() && getSwimlane() != null) {
            errors.add(ValidationError.createLocalizedError(this, "startState.swimlaneIsNotUsableInEmbeddedSubprocess"));
        }
        if (isStartByTimer() && hasFormValidation() && getValidation(getProcessDefinition().getFile()).getRequiredVariableNames().size() > 0) {
            errors.add(ValidationError.createLocalizedError(this, "startState.startNodeHasBothTimerDefinitionAndRequiredVariables"));
        }
    }

    @Override
    public TextDecoratorEmulation getTextDecoratorEmulation() {
        return decoratorEmulation;
    }

    protected final EventTrigger eventTrigger = new EventTrigger(this);

    @Override
    public EventTrigger getEventTrigger() {
        return eventTrigger;
    }

    @Override
    protected void populateCustomPropertyDescriptors(List<IPropertyDescriptor> descriptors) {
        super.populateCustomPropertyDescriptors(descriptors);
        if (!isStartByTimer()) {
            descriptors.add(
                    new ComboBoxPropertyDescriptor(PROPERTY_EVENT_TYPE, Localization.getString("property.eventType"), EventTrigger.EVENT_TYPE_NAMES));
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
        return super.getPropertyValue(id);
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        if (PROPERTY_EVENT_TYPE.equals(id)) {
            int index = ((Integer) value).intValue();
            if (index == 0) {
                getProcessDefinition().setDefaultStartNode(this);
            }
            eventTrigger.setEventType(index > 0 ? EventNodeType.values()[index - 1] : null);
            deleteFiles();
        } else {
            super.setPropertyValue(id, value);
        }
    }

    public boolean isStartByEvent() {
        return getEventTrigger().getEventType() != null;
    }

    @Override
    public boolean testAttribute(Object target, String name, String value) {
        if ("isEventTypeDefined".equals(name)) {
            return Objects.equal(value, String.valueOf(!isStartByTimer() && isStartByEvent()));
        }
        return super.testAttribute(target, name, value);
    }

    public String getTimerEventDefinition() {
        return timerEventDefinition;
    }

    public void setTimerEventDefinition(String timerEventDefinition) {
        if (timerEventDefinition != this.timerEventDefinition) {
            String oldTimerEventDefinition = this.timerEventDefinition;
            this.timerEventDefinition = timerEventDefinition;
            firePropertyChange(PROPERTY_TIMER_EVENT_DEFINITION, oldTimerEventDefinition, this.timerEventDefinition);
        }
    }

    public boolean isStartByTimer() {
        return !Strings.isNullOrEmpty(timerEventDefinition);
    }
}
