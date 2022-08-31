package ru.runa.gpd.office.store.externalstorage;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.office.store.QueryType;
import ru.runa.gpd.office.store.StorageConstraintsModel;
import ru.runa.wfe.var.UserTypeMap;

abstract class AbstractConstraintsCompositeBuilder extends Composite implements ConstraintsCompositeBuilder {

    protected final StorageConstraintsModel constraintsModel;
    protected final VariableProvider variableProvider;
    protected String variableTypeName;

    public AbstractConstraintsCompositeBuilder(Composite parent, int style, StorageConstraintsModel constraintsModel,
            VariableProvider variableProvider, String variableTypeName) {
        super(parent, style);
        setLayout(new GridLayout(2, false));
        ru.runa.gpd.PluginLogger.logInfo("Construct AbstractConstraintsCompositeBuilder: " + variableTypeName);
        this.constraintsModel = constraintsModel;
        this.variableTypeName = variableTypeName;
        this.variableProvider = variableProvider;
    }

    @Override
    public void onChangeVariableTypeName(String variableTypeName) {
        this.variableTypeName = variableTypeName;
        constraintsModel.setVariableName("");
        constraintsModel.setQueryString("");
    }

    @Override
    public void clearConstraints() {
        ru.runa.gpd.PluginLogger.logInfo("Enter clearConstraints abstract");
        if (!Strings.isNullOrEmpty(constraintsModel.getVariableName())
                && variableNamesByVariableTypeName(variableTypeName).noneMatch(s -> s.equals(constraintsModel.getVariableName()))) {
            constraintsModel.setVariableName("");
        }
        ru.runa.gpd.PluginLogger.logInfo("Pass clearConstraints abstract");
        if (QueryType.INSERT.equals(constraintsModel.getQueryType())) {
            constraintsModel.setQueryString("");
        }
    }

    protected Stream<String> variableNamesByVariableTypeName(String variableTypeName) {
        ru.runa.gpd.PluginLogger.logInfo("variableNamesByVariableTypeName: " + variableTypeName + " | " + variableProvider.toString());
        if (variableTypeName.equals("")) {
            ru.runa.gpd.PluginLogger.logInfo("Exit variableNamesByVariableTypeName");
            String[] classes = {""};
            ru.runa.gpd.PluginLogger.logInfo("Return fake var!!!");
            return Arrays.stream(classes);
        }
        for(String cur : getTypeNameFilters()) {
            ru.runa.gpd.PluginLogger.logInfo(cur);
        }
        Stream<String> stream = variableProvider.getVariables(false, false, getTypeNameFilters()).stream().filter(getFilterPredicate(variableTypeName))
                .map(Variable::getName);
//        String[] stringArray = stream.toArray(String[]::new);
//        ru.runa.gpd.PluginLogger.logInfo("End variableNamesByVariableTypeName: " + stringArray.toString() + " | " + stringArray.length);
        return stream;
    }

    protected String[] getTypeNameFilters() {
        return new String[] { UserTypeMap.class.getName() };
    }

    protected abstract Predicate<? super Variable> getFilterPredicate(String variableTypeName);
}