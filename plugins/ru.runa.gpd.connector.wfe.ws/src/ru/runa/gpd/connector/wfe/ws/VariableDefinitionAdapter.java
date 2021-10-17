package ru.runa.gpd.connector.wfe.ws;

import ru.runa.wfe.var.VariableDefinition;

public class VariableDefinitionAdapter {

    public static VariableDefinition toDTO(ru.runa.wfe.webservice.VariableDefinition variableDefinition) {
        VariableDefinition result = new VariableDefinition(variableDefinition.getName(), null);
        result.setFormat(variableDefinition.getFormat());
        return result;
    }
}