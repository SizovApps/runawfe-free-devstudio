package ru.runa.gpd.sync;

public interface ConnectionStatusVisitor<TResult, TContext> {
    TResult onEstablishFailed(TContext context);

    TResult onFreeEdition(TContext context);

    TResult onIndustrialEdition(TContext context);
}
