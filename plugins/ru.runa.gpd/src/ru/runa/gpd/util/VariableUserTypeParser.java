package ru.runa.gpd.util;

import java.util.List;
import java.util.ArrayList;
import org.dom4j.Document;
import org.dom4j.Element;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.extension.handler.ParamDefConfig;

public class VariableUserTypeParser {
    private static final String USER_TYPE_SECTION = "usertypes";
    private static final String USER_TYPE = "usertype";
    private static final String TABLE_NAME = "tableName";
    private static final String SELECTED_TABLE = "selectedTable";

    public static List<VariableUserType> parseVariableUserTypesFromTask(Element configElement) {
        List<VariableUserType> variableUserTypes = new ArrayList<>();
        List<Element> groupElements = configElement.elements();
        for (Element groupElement : groupElements) {
            if (groupElement.getName() == USER_TYPE_SECTION) {
                List<Element> userTypeElements = groupElement.elements(USER_TYPE);
                for (Element userTypeElement : userTypeElements) {
                    String userTypeName = userTypeElement.attributeValue(ParamDefConfig.NAME);
                    VariableUserType variableUserType = new VariableUserType(userTypeName);

                    List<Element> parameters = userTypeElement.elements();
                    for (Element parameter : parameters) {
                        String name = parameter.attributeValue(ParamDefConfig.NAME);
                        String scriptingName = parameter.attributeValue(ParamDefConfig.SCRIPTING_NAME);
                        String format = parameter.attributeValue(ParamDefConfig.FORMAT);
                        // TODO проверить нужен ли variableUserType
                        Variable newVariable = new Variable(name, scriptingName, format, variableUserType);
                        newVariable.setFormat(format);
                        variableUserType.addAttribute(newVariable);
                    }
                    variableUserTypes.add(variableUserType);
                }
            }
        }
        return variableUserTypes;
    }

    public static void addVariableUserTypes(List<Document> dataTableDocuments, Element config) {
        Element userTypesElement = config.addElement(USER_TYPE_SECTION);
        for (Document document : dataTableDocuments) {
            Element userTypeElement = document.getRootElement();
            userTypesElement.add(userTypeElement);
        }
    }

    public static void addTableNameForInternalStorage(String dataTableName, Element config) {
        if (!dataTableName.equals("")) {
            Element table = config.addElement(SELECTED_TABLE);
            table.addAttribute(TABLE_NAME, dataTableName);
        }
    }
}
