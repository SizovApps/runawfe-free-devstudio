package ru.runa.gpd.lang.model;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PropertyNames;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;
import ru.runa.gpd.util.Duration;
import ru.runa.gpd.util.VariableMapping;

public class EventTrigger implements PropertyNames {

    public static String[] EVENT_TYPE_NAMES;

    static {
        List<String> eventNodeTypeNames = Lists.newArrayList(Localization.getString("event.type.noevent"));
        for (EventNodeType eventNodeType : EventNodeType.values()) {
            eventNodeTypeNames.add(Localization.getString("event.node.type." + eventNodeType.name().toLowerCase()));
        }
        EVENT_TYPE_NAMES = eventNodeTypeNames.toArray(new String[eventNodeTypeNames.size()]);
    }

    private final GraphElement owner;
    private EventNodeType eventNodeType;
    private Duration ttlDuration = new Duration("1 days");
    private final List<VariableMapping> variableMappings = new ArrayList<VariableMapping>();

    EventTrigger(GraphElement owner) {
        this.owner = owner;
    }

    public EventNodeType getEventType() {
        return eventNodeType;
    }

    public void setEventType(EventNodeType eventNodeType) {
        if (eventNodeType != this.eventNodeType) {
            EventNodeType old = this.eventNodeType;
            this.eventNodeType = eventNodeType;
            owner.firePropertyChange(PROPERTY_EVENT_TYPE, old, this.eventNodeType);
        }
    }

    public Duration getTtlDuration() {
        return ttlDuration;
    }

    public void setTtlDuration(Duration ttlDuration) {
        Duration old = this.ttlDuration;
        this.ttlDuration = ttlDuration;
        owner.firePropertyChange(PROPERTY_TTL, old, ttlDuration);
    }

    public List<VariableMapping> getVariableMappings() {
        return variableMappings;
    }

    public void setVariableMappings(List<VariableMapping> variablesList) {
        this.variableMappings.clear();
        this.variableMappings.addAll(variablesList);
        owner.setDirty();
    }

}
