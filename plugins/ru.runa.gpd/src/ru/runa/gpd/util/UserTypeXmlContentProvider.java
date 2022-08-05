package ru.runa.gpd.util;

import java.util.List;
import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.core.resources.IFile;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.extension.VariableFormatArtifact;

public class UserTypeXmlContentProvider {

    private static final String ROOT_ELEMENT_NAME = "usertype";
    private static final String NAME = "name";
    private static final String SCRIPTING_NAME = "scriptingName";
    private static final String FORMAT = "format";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String ATTRIBUTE_ELEMENT_NAME = "variable";

    public static Document save(IFile file, VariableUserType dataTable) {
        Document document = XmlUtil.createDocument(ROOT_ELEMENT_NAME);
        Element root = document.getRootElement();
        root.addAttribute(NAME, file.getName().substring(0, file.getName().lastIndexOf('.')));
        for (Variable variable : dataTable.getAttributes()) {
            Element newUserTypeAttribute = root.addElement(ATTRIBUTE_ELEMENT_NAME);
            newUserTypeAttribute.addAttribute(NAME, variable.getName());
            newUserTypeAttribute.addAttribute(SCRIPTING_NAME, variable.getScriptingName());
            newUserTypeAttribute.addAttribute(FORMAT, variable.getFormat());
            newUserTypeAttribute.addAttribute(DEFAULT_VALUE, variable.getDefaultValue());
        }
        VariableFormatArtifact variableFormatArtifact = new VariableFormatArtifact(true, dataTable.getName(), dataTable.getName(), dataTable.getName());
        VariableFormatRegistry.getInstance().add(variableFormatArtifact);
        return document;
    }

    public static VariableUserType read(Document document) {
        VariableUserType dataTable = new VariableUserType(document.getRootElement().attributeValue(NAME));
        List<Variable> userTypeAttributes = dataTable.getAttributes();
        List<Element> userTypeAttributesElements = document.getRootElement().elements(ATTRIBUTE_ELEMENT_NAME);
        for (Element element : userTypeAttributesElements) {
            String variableName = element.attributeValue(NAME);
            String scriptingName = element.attributeValue(SCRIPTING_NAME, variableName);
            String format = element.attributeValue(FORMAT);
            String defaultValue = element.attributeValue(DEFAULT_VALUE);
            Variable newAttribute = new Variable();
            newAttribute.setName(variableName);
            newAttribute.setScriptingName(scriptingName);
            newAttribute.setFormat(format);
            newAttribute.setDefaultValue(defaultValue);
            userTypeAttributes.add(newAttribute);
        }
        return dataTable;
    }

}
