package ru.runa.gpd.lang.model.bpmn;

import com.google.common.base.Strings;

public enum EventNodeType {
    message,
    signal,
    cancel,
    error;

    public String getImageName(boolean isCatch, boolean boundary) {
        return getImageName(null, isCatch, boundary);
    }

    public String getImageName(String style, boolean isCatch, boolean boundary) {
        return (Strings.isNullOrEmpty(style) ? "" : style + "/") + (boundary ? "boundary_" : "") + (isCatch ? "catch" : "throw") + "_" + name() + ".png";
    }

}
