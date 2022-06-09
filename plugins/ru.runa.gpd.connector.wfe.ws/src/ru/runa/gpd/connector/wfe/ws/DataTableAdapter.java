package ru.runa.gpd.connector.wfe.ws;

import ru.runa.wfe.var.UserType;
import ru.runa.wfe.var.VariableDefinition;

public class DataTableAdapter {

    public static UserType toDto(ru.runa.wfe.webservice.UserType userType) {
        UserType result = new UserType(userType.getName());
        for (ru.runa.wfe.webservice.VariableDefinition variableDefinition : userType.getAttributes()) {
            result.addAttribute(toDto(variableDefinition));
        }
        return result;
    }

    private static VariableDefinition toDto(ru.runa.wfe.webservice.VariableDefinition variableDefinition) {
        VariableDefinition result = new VariableDefinition(variableDefinition.getName(), null);
        result.setFormat(variableDefinition.getFormat());
        return result;
    }
}