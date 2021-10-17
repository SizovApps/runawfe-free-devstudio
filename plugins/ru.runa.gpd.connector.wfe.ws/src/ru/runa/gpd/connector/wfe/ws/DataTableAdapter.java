package ru.runa.gpd.connector.wfe.ws;

import ru.runa.wfe.var.UserType;

public class DataTableAdapter {

    public static UserType toDTO(ru.runa.wfe.webservice.UserType userType) {
        UserType result = new UserType(userType.getName());
        for (ru.runa.wfe.webservice.VariableDefinition variableDefinition : userType.getAttributes()) {
            result.addAttribute(VariableDefinitionAdapter.toDTO(variableDefinition));
        }
        return result;
    }
}