package ru.runa.gpd.office.store.externalstorage;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;

public class ProcessDefinitionVariableProvider implements VariableProvider {
    private final ProcessDefinition processDefinition;

    public ProcessDefinitionVariableProvider(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

    @Override
    public List<Variable> getVariables(boolean expandComplexTypes, boolean includeSwimlanes, String... typeClassNameFilters) {
        return processDefinition.getVariables(expandComplexTypes, includeSwimlanes, typeClassNameFilters);
    }

    @Override
    public VariableUserType getUserType(String name) {
        return processDefinition.getVariableUserType(name);
    }

    @Override
    public Stream<? extends VariableUserType> complexUserTypes(Predicate<? super VariableUserType> predicate) {
        Stream<? extends VariableUserType> stream = processDefinition.getVariableUserTypes().stream();
        if (predicate != null) {
            stream = stream.filter(predicate);
        }
        return stream;
    }

}
