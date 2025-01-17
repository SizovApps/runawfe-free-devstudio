package ru.runa.gpd.lang.model.bpmn;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import ru.runa.gpd.Localization;

public enum EventNodeType {
    message,
    signal,
    cancel,
    error;

    public String getImageName(boolean isCatch, boolean boundary) {
        return getImageName(null, isCatch, boundary, true);
    }

    public String getImageName(boolean isCatch, boolean boundary, boolean interrupting) {
        return getImageName(null, isCatch, boundary, interrupting);
    }

    public String getImageName(String style, boolean isCatch, boolean boundary, boolean interrupting) {
        return (Strings.isNullOrEmpty(style) ? "" : style + "/") + (boundary ? "boundary_" : "") + (isCatch ? "catch" : "throw") + "_" + name()
                + (interrupting ? "" : "_non_interrupting") + ".png";
    }

    private String label = Localization.getString("event.node.type." + name().toLowerCase());

    public static String[] LABELS;

    static {
        List<String> eventTypeLabels = Lists.newArrayList();
        for (EventNodeType eventType : EventNodeType.values()) {
            eventTypeLabels.add(eventType.label);
        }
        LABELS = eventTypeLabels.toArray(new String[eventTypeLabels.size()]);
    }
}
