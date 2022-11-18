package ru.runa.gpd.office.store.externalstorage;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.lang.model.BotTask;


public class BotTaskVariableProvider implements VariableProvider{
    private final BotTask botTask;

    public BotTaskVariableProvider(BotTask botTask) {
        this.botTask = botTask;
    }


    @Override
    public Stream<? extends VariableUserType> complexUserTypes(Predicate<? super VariableUserType> predicate) {
        Stream<? extends VariableUserType> stream = botTask.getVariableUserTypes().stream()
                .filter(VariableUserType::isStoreInExternalStorage);
        if (predicate != null) {
            stream = stream.filter(predicate);
        }
        return stream;
    }

    @Override
    public Stream<String> complexUserTypeNames() {
        Stream<String> stream = botTask.getVariableUserTypesNames().stream();
        return stream;
    }

    @Override
    public VariableUserType getUserType(String name) {
        return botTask.getVariableUserType(name);
    }

    @Override
    public List<Variable> getVariables(boolean expandComplexTypes, boolean includeSwimlanes, String... typeClassNameFilters) {
        return botTask.getVariabels(expandComplexTypes, includeSwimlanes, typeClassNameFilters);
    }
}
