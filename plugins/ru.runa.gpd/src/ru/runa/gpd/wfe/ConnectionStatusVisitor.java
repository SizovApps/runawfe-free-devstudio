package ru.runa.gpd.wfe;

public interface ConnectionStatusVisitor<TResult, TContext> {
    TResult onEstablishFailed(TContext context);

    TResult onFreeEdition(TContext context);

    TResult onIndustrialEdition(TContext context);
}
