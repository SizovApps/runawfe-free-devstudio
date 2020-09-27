package ru.runa.gpd.sync;

public enum ConnectionStatus {
    ESTABLISH_FAILED {
        @Override
        public <TResult, TContext> TResult visit(ConnectionStatusVisitor<TResult, TContext> visitor, TContext context) {
            return visitor.onEstablishFailed(context);
        }
    },
    FREE_EDITION {
        @Override
        public <TResult, TContext> TResult visit(ConnectionStatusVisitor<TResult, TContext> visitor, TContext context) {
            return visitor.onFreeEdition(context);
        }
    },
    INDUSTRIAL_EDITION {
        @Override
        public <TResult, TContext> TResult visit(ConnectionStatusVisitor<TResult, TContext> visitor, TContext context) {
            return visitor.onIndustrialEdition(context);
        }
    };

    public abstract <TResult, TContext> TResult visit(ConnectionStatusVisitor<TResult, TContext> visitor, TContext context);
}
