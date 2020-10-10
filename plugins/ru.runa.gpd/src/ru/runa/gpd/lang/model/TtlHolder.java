package ru.runa.gpd.lang.model;

import ru.runa.gpd.util.Duration;

public interface TtlHolder {

    Duration getTtlDuration();

    void setTtlDuration(Duration ttlDuration);

}
