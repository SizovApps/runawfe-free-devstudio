package ru.runa.gpd.util;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.core.resources.IFile;

import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;

public class UserTypeXmlContentPorvider {

    private static final String ROOT_TAG = "usertype";
    private static final String TAG_NAME_ATTRIBUTE = "name";
    private static final String TAG_SCRIPTING_NAME_ATTRIBUTE = "scriptingName";
    private static final String TAG_FORMAT_ATTRIBUTE = "format";
    private static final String TAG_DEFAULT_ATTRIBUTE = "defaultValue";
    private static final String USERTYPE_ATTRIBUTE_TAG_NAME = "variable";

    public static Document save(IFile file, VariableUserType dataTable) {
        Document document = XmlUtil.createDocument(ROOT_TAG);
        Element root = document.getRootElement();
        root.addAttribute(TAG_NAME_ATTRIBUTE, file.getName().substring(0, file.getName().lastIndexOf('.')));
        for (Variable userType : dataTable.getAttributes()) {
            Element newUserTypeAttribute = root.addElement(USERTYPE_ATTRIBUTE_TAG_NAME);
            newUserTypeAttribute.addAttribute(TAG_NAME_ATTRIBUTE, userType.getName());
            newUserTypeAttribute.addAttribute(TAG_SCRIPTING_NAME_ATTRIBUTE, userType.getScriptingName());
            newUserTypeAttribute.addAttribute(TAG_FORMAT_ATTRIBUTE, userType.getFormat());
            newUserTypeAttribute.addAttribute(TAG_DEFAULT_ATTRIBUTE, userType.getDefaultValue());
        }
        return document;
    }

    public static VariableUserType read(Document document) {
        VariableUserType dataTable = new VariableUserType(document.getRootElement().attributeValue(TAG_NAME_ATTRIBUTE));
        List<Variable> userTypeAttributes = dataTable.getAttributes();
        List<Element> userTypeAttributesElements = document.getRootElement().elements(USERTYPE_ATTRIBUTE_TAG_NAME);
        for (Element element : userTypeAttributesElements) {
            String variableName = element.attributeValue(TAG_NAME_ATTRIBUTE);
            String scriptingName = element.attributeValue(TAG_SCRIPTING_NAME_ATTRIBUTE, variableName);
            String format = element.attributeValue(TAG_FORMAT_ATTRIBUTE);
            String defaultValue = element.attributeValue(TAG_DEFAULT_ATTRIBUTE);
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
