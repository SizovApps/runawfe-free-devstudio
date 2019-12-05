package ru.runa.gpd.lang.model;

import com.google.common.base.Objects;
import org.eclipse.ui.IActionFilter;

public interface EventHolder extends IActionFilter {

    EventTrigger getEventTrigger();

    @Override
    default boolean testAttribute(Object target, String name, String value) {
        if ("isEventTypeDefined".equals(name)) {
            return Objects.equal(value, String.valueOf(getEventTrigger().getEventType() != null));
        }
        return false;
    }

}
