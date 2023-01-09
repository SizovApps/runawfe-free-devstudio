package ru.runa.gpd.extension.handler;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.DataTableNature;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.extension.VariableFormatArtifact;
import ru.runa.gpd.extension.handler.ParamDef.Presentation;
import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.lang.par.VariablesXmlContentProvider;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.UserTypeXmlContentProvider;
import ru.runa.gpd.util.VariableUtils;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.wfe.InternalApplicationException;

@SuppressWarnings("unchecked")
public class ParamDefConfig {
    public static final String NAME_CONFIG = "config";
    private static final String ROOT_ELEMENT_NAME = "usertype";
    private static final String NAME = "name";
    private static final String SCRIPTING_NAME = "scriptingName";
    private static final String FORMAT = "format";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String ATTRIBUTE_ELEMENT_NAME = "variable";
    private static final String USER_TYPE = "usertype";
    private static final String USER_TYPE_SECTION = "usertypes";
    private static final String GLOBAL = "global";
    private static final String GLOBAL_SECTION = "globals";
    private static final String TABLE_NAME = "tableName";
    private static final String SELECTED_TABLE = "selectedTable";
    private static final Pattern VARIABLE_REGEXP = Pattern.compile("\\$\\{(.*?[^\\\\])\\}");
    private final String name;
    private String selectedTableName;
    private final List<ParamDefGroup> groups = new ArrayList<ParamDefGroup>();

    public ParamDefConfig(String name) {
        this.name = name;
    }

    public ParamDefConfig() {
        this(NAME_CONFIG);
    }

    public static ParamDefConfig parse(String xml) {
        return parse(XmlUtil.parseWithoutValidation(xml));
    }

    public String getSelectedTableName() {
        return selectedTableName;
    }

    public static ParamDefConfig parse(Document document) {
        Element rootElement = document.getRootElement();
        return parse(rootElement);
    }

    public static ParamDefConfig parse(Element rootElement) {
        ParamDefConfig config = new ParamDefConfig();
        List<Element> groupElements = rootElement.elements();
        for (Element groupElement : groupElements) {
            ParamDefGroup group = new ParamDefGroup(groupElement);
            if (groupElement.getName() == "input" || groupElement.getName() == "output") {
                List<Element> inputParamElements = groupElement.elements("param");
                for (Element element : inputParamElements) {
                    group.getParameters().add(new ParamDef(element));
                }
                config.getGroups().add(group);
            } else if (groupElement.getName() == USER_TYPE_SECTION) {
                List<Element> usertypesParamElements = groupElement.elements(ROOT_ELEMENT_NAME);
                for (Element element : usertypesParamElements) {
                    String nameOfTable = element.attributeValue(NAME);
                    IProject dtProject = DataTableUtils.getDataTableProject();
                    try {
                        if (!dtProject.exists()) {
                            IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(dtProject.getName());
                            description.setNatureIds(new String[] { DataTableNature.NATURE_ID });
                            dtProject.create(description, null);
                            dtProject.open(IResource.BACKGROUND_REFRESH, null);
                            dtProject.refreshLocal(IResource.DEPTH_ONE, null);
                        }
                    } catch (CoreException ex) {
                        throw new InternalApplicationException(ex);
                    }
                    IFile dataTableFile = dtProject.getFile(nameOfTable + DataTableUtils.FILE_EXTENSION);
                    VariableUserType dataTable = new VariableUserType(nameOfTable);
                    List<Element> variables = element.elements();
                    for (Element variable : variables) {
                        String name = variable.attributeValue(NAME);
                        String scriptingName = variable.attributeValue(SCRIPTING_NAME);
                        String format = variable.attributeValue(FORMAT);
                        Variable newVariable = new Variable(name, scriptingName, format, dataTable);
                        newVariable.setFormat(format);
                        dataTable.addAttribute(newVariable);
                    }
                    Document document = UserTypeXmlContentProvider.save(dataTableFile, dataTable);
                    try {
                        IOUtils.createOrUpdateFile(dataTableFile, new ByteArrayInputStream(XmlUtil.writeXml(document)));
                    } catch (CoreException e) {
                        throw new InternalApplicationException(e);
                    }
                    DataTableCache.reload();
                    group.getParameters().add(new ParamDef(element));
                }
            } else if (groupElement.getName() == GLOBAL_SECTION) {
                if (groupElement.elements().size() == 0) {
                    continue;
                }
                VariablesXmlContentProvider variablesXmlContentProvider = new VariablesXmlContentProvider();
                try {
                    variablesXmlContentProvider.readGlobalElements(groupElement, ProcessCache.getSelectedProcessForImportUserTypesFromBot());
                } catch (Exception e) {
                    PluginLogger.logError("Not found any processes in project", e);
                }
            } else if (groupElement.getName() == SELECTED_TABLE) {
                String nameOfTable = groupElement.attributeValue("tableName");
                config.selectedTableName = nameOfTable;
            }
        }
        return config;
    }

    public static void createTablesForInternalStorageHandler(Document document, String nameOfTable) {
        Element rootElement = document.getRootElement();
        createTablesForInternalStorageHandler(rootElement, nameOfTable);
    }

    public static void createTablesForInternalStorageHandler(Element rootElement, String nameOfTable) {
        List<Element> groupElements = rootElement.elements();
        for (Element groupElement : groupElements) {
            ParamDefGroup group = new ParamDefGroup(groupElement);
            if (groupElement.getName() == "input") {
                List<Element> inputParamElements = groupElement.elements("param");
                IProject dtProject = DataTableUtils.getDataTableProject();
                try {
                    if (!dtProject.exists()) {
                        IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(dtProject.getName());
                        description.setNatureIds(new String[] { DataTableNature.NATURE_ID });
                        dtProject.create(description, null);
                        dtProject.open(IResource.BACKGROUND_REFRESH, null);
                        dtProject.refreshLocal(IResource.DEPTH_ONE, null);
                    }
                } catch (CoreException ex) {
                    throw new InternalApplicationException(ex);
                }

                IFile dataTableFile = dtProject.getFile(nameOfTable + DataTableUtils.FILE_EXTENSION);
                VariableUserType dataTable = new VariableUserType(nameOfTable);

                for (Element variable : inputParamElements) {
                    String name = variable.attributeValue(NAME);
                    String scriptingName = variable.attributeValue("label");
                    ru.runa.gpd.extension.VariableFormatRegistry variableFormatRegistry = new VariableFormatRegistry();
                    String format = variable.attributeValue("formatFilter");
                    String correctFormat = variableFormatRegistry.getFilterLabel(format);
                    Variable newVariable = new Variable(name, scriptingName, correctFormat, dataTable);
                    newVariable.setFormat(correctFormat);
                    dataTable.addAttribute(newVariable);
                }

                Document document = UserTypeXmlContentProvider.save(dataTableFile, dataTable);
                try {
                    IOUtils.createOrUpdateFile(dataTableFile, new ByteArrayInputStream(XmlUtil.writeXml(document)));
                } catch (CoreException e) {
                    throw new InternalApplicationException(e);
                }

                DataTableCache.reload();
            }
        }
    }

    public String getName() {
        return name;
    }

    public List<ParamDefGroup> getGroups() {
        return groups;
    }

    /**
     * @param groupName
     * @return group or null if none found
     */
    public ParamDefGroup getGroupByName(String groupName) {
        for (ParamDefGroup group : groups) {
            if (groupName.equals(group.getName())) {
                return group;
            }
        }

        return null;
    }

    /**
     * Retrieves all founded parameter to variable mappings
     * 
     * @param configuration
     *            param-based xml or <code>null</code> or empty string
     */
    public static Map<String, String> getAllParameters(String configuration) {
        Map<String, String> properties = new HashMap<String, String>();
        if (Strings.isNullOrEmpty(configuration)) {
            return properties;
        }
        Document doc = XmlUtil.parseWithoutValidation(configuration);
        List<Element> groupElements = doc.getRootElement().elements();
        for (Element groupElement : groupElements) {
            List<Element> paramElements = groupElement.elements("param");
            for (Element element : paramElements) {
                String value;
                if (element.attributeValue("variable") != null) {
                    value = element.attributeValue("variable");
                } else {
                    value = element.attributeValue("value");
                }
                String name = element.attributeValue("name");
                properties.put(name, value);
            }
        }
        return properties;
    }

    /**
     * Retrieves all founded parameter to variable mappings based on this definition.
     * 
     * @param configuration
     *            valid param-based xml
     * @return not <code>null</code> parameters (empty parameters on parsing error)
     */
    public Map<String, String> parseConfiguration(String configuration) {
        Map<String, String> properties = new HashMap<String, String>();
        if (Strings.isNullOrEmpty(configuration)) {
            return properties;
        }
        try {
            Document doc = XmlUtil.parseWithoutValidation(configuration);
            Map<String, String> allProperties = new HashMap<String, String>();
            for (ParamDefGroup group : groups) {
                Element groupElement = doc.getRootElement().element(group.getName());
                if (groupElement != null) {
                    List<Element> pElements = groupElement.elements();
                    for (Element element : pElements) {
                        if ("param".equals(element.getName())) {
                            String value;
                            if (element.attributeValue("variable") != null) {
                                value = element.attributeValue("variable");
                            } else {
                                value = element.attributeValue("value");
                            }
                            String name = element.attributeValue("name");
                            allProperties.put(name, value);
                        } else {
                            allProperties.put(element.getName(), element.getTextTrim());
                        }
                    }
                }
            }
            for (ParamDefGroup group : groups) {
                Element groupElement = doc.getRootElement().element(group.getName());
                if (groupElement != null) {
                    List<Element> pElements = groupElement.elements();
                    for (Element element : pElements) {
                        String name = "param".equals(element.getName()) ? element.attributeValue("name") : element.getName();
                        String value = allProperties.get(name);
                        String fName = fixParamName(name, allProperties);
                        if (fName == null) {
                            group.getDynaProperties().put(name, value);
                        } else {
                            properties.put(fName, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            PluginLogger.logErrorWithoutDialog(configuration, e);
        }
        return properties;
    }

    public String fixParamName(String name, Map<String, String> properties) {
        for (ParamDefGroup group : groups) {
            for (ParamDef paramDef : group.getParameters()) {
                String paramName = paramDef.getName();
                if (name.equals(paramName)) {
                    return name;
                }
                paramName = substitute(paramName, properties);
                if (name.equals(paramName)) {
                    return paramDef.getName();
                }
            }
        }
        return null;
    }

    public ParamDef getParamDef(String name) {
        for (ParamDefGroup group : groups) {
            for (ParamDef paramDef : group.getParameters()) {
                String paramName = paramDef.getName();
                if (name.equals(paramName)) {
                    return paramDef;
                }
            }
        }
        return null;
    }

    public boolean validate(Delegable delegable, List<ValidationError> errors) {
        String configuration = delegable.getDelegationConfiguration();
        GraphElement graphElement = ((GraphElement) delegable);
        Map<String, String> props = parseConfiguration(configuration);
        for (ParamDefGroup group : groups) {
            for (ParamDef paramDef : group.getParameters()) {
                String value = props.get(paramDef.getName());
                if (paramDef.isOptional() && !isValid(value)) {
                    continue;
                }
                if (!paramDef.isOptional() && !isValid(value)) {
                    errors.add(ValidationError.createLocalizedError(graphElement, "parambased.requiredParamIsNotSet", paramDef.getLabel()));
                } else if (paramDef.isUseVariable() && paramDef.getPresentation() == Presentation.combo) {
                    String[] filters = paramDef.getFormatFilters().toArray(new String[paramDef.getFormatFilters().size()]);
                    List<String> variableNames = graphElement.getProcessDefinition().getVariableNames(true, filters);
                    if (!variableNames.contains(value)) {
                        if (VariableUtils.variableExists(value, graphElement.getProcessDefinition())) {
                            String localizedVariableType = Localization
                                    .getString(VariableUtils.getVariableByName(graphElement.getProcessDefinition(), value).getFormat());
                            errors.add(ValidationError.createLocalizedError(graphElement, "parambased.paramVariableTypeMismatch",
                                    localizedVariableType, value, paramDef.getLabel()));
                        } else {
                            errors.add(
                                    ValidationError.createLocalizedError(graphElement, "parambased.missedParamVariable", paramDef.getLabel(), value));
                        }
                    }
                }
            }
        }
        return true;
    }

    protected boolean isValid(String value) {
        return value != null && value.trim().length() > 0;
    }

    public String toConfiguration(Collection<String> variableNames, Map<String, String> properties) {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setSuppressDeclaration(true);
        return XmlUtil.toString(toConfigurationXml(variableNames, properties), format);
    }

    public Document toConfigurationXml(Collection<String> variableNames, Map<String, String> properties) {
        Document doc = DocumentHelper.createDocument();
        doc.add(DocumentHelper.createElement(name));
        Element root = doc.getRootElement();
        Element prevGroupElement = null;
        for (ParamDefGroup group : groups) {
            Element groupElement;
            if (prevGroupElement != null && prevGroupElement.getName().equals(group.getName())) {
                groupElement = prevGroupElement;
            } else {
                groupElement = DocumentHelper.createElement(group.getName());
                root.add(groupElement);
                for (String dName : group.getDynaProperties().keySet()) {
                    String dValue = group.getDynaProperties().get(dName);
                    Element paramElement = DocumentHelper.createElement("param");
                    paramElement.addAttribute("name", dName);
                    paramElement.addAttribute("value", dValue);
                    groupElement.add(paramElement);
                }
            }
            for (ParamDef param : group.getParameters()) {
                String value = properties.get(param.getName());
                if (value == null) {
                    continue;
                }
                String paramName = param.getName();
                paramName = substitute(paramName, properties);
                Element paramElement;
                if (param.getXmlNodeType() == ParamDef.XML_TYPE_ATTR) {
                    paramElement = DocumentHelper.createElement("param");
                    paramElement.addAttribute("name", paramName);
                    if (param.isUseVariable() && variableNames.contains(value)) {
                        paramElement.addAttribute("variable", value);
                    } else {
                        paramElement.addAttribute("value", value);
                    }
                } else {
                    paramElement = DocumentHelper.createElement(paramName);
                    paramElement.add(DocumentHelper.createText(value));
                }
                groupElement.add(paramElement);
            }
            prevGroupElement = groupElement;
        }
        return doc;
    }

    private String substitute(String value, Map<String, String> properties) {
        Matcher matcher = VARIABLE_REGEXP.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String pName = matcher.group(1);
            String parameter = properties.get(pName);
            if (parameter == null) {
                parameter = "";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(parameter));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public void writeXml(Branch parent) {
        Element root = parent.addElement("config");
        writeXMLFromRoot(root, "");
    }

    public void writeXml(Branch parent, String selectedTableName) {
        Element root = parent.addElement("config");
        writeXMLFromRoot(root, selectedTableName);
    }

    public void writeXMLFromRoot(Element root, String selectedTableName) {
        List<String> usedUserTypesNames = new ArrayList<String>();
        for (ParamDefGroup group : getGroups()) {
            if (USER_TYPE_SECTION.equals(group.getName())) {
                continue;
            }
            Element groupParamElement = root.addElement(group.getName());
            for (ParamDef param : group.getParameters()) {
                Element paramElement = groupParamElement.addElement("param");
                paramElement.addAttribute("name", param.getName());
                paramElement.addAttribute("label", param.getLabel());
                if (param.getFormatFilters().size() > 0) {
                    paramElement.addAttribute("formatFilter", param.getFormatFilters().get(0));
                    List<VariableFormatArtifact> artifacts = VariableFormatRegistry.getInstance().getFilterArtifacts();
                    boolean isUserType = true;
                    for (VariableFormatArtifact artifact : VariableFormatRegistry.getInstance().getFilterArtifacts()) {
                        if (artifact.getJavaClassName().equals(param.getFormatFilters().get(0))) {
                            isUserType = false;
                        }
                    }
                    if (!usedUserTypesNames.contains(param.getFormatFilters().get(0)) && isUserType) {
                        usedUserTypesNames.add(param.getFormatFilters().get(0));
                    }
                }
                if (param.isOptional()) {
                    paramElement.addAttribute("optional", "true");
                }
                if (!param.isUseVariable()) {
                    paramElement.addAttribute("variable", "false");
                }
            }
        }
        IProject dtProject = DataTableUtils.getDataTableProject();
        Element usertypes = root.addElement(USER_TYPE_SECTION);
        Element globals = root.addElement(GLOBAL_SECTION);

        if (!selectedTableName.equals("")) {
            Element table = root.addElement(SELECTED_TABLE);
            table.addAttribute(TABLE_NAME, selectedTableName);
        }

        VariablesXmlContentProvider variablesXmlContentProvider = new VariablesXmlContentProvider();
        if (!selectedTableName.equals("")) {
            return;
        }
        for (String usedTypeName : usedUserTypesNames) {
            boolean foundInGlobalSections = false;
            for (ProcessDefinition processDefinition : ProcessCache.getAllProcessDefinitions()) {
                for (VariableUserType variableUserType : processDefinition.getVariableUserTypes()) {
                    if (variableUserType.getName().equals(usedTypeName)) {
                        Element typeElement = globals.addElement(USER_TYPE);
                        typeElement.addAttribute(NAME, variableUserType.getName());
                        if (variableUserType.isStoreInExternalStorage()) {
                            typeElement.addAttribute(VariableUserType.PROPERTY_STORE_IN_EXTERNAL_STORAGE, Boolean.TRUE.toString());
                        }
                        if (variableUserType.isGlobal()) {
                            typeElement.addAttribute(GLOBAL, "true");
                        }
                        for (Variable variable : variableUserType.getAttributes()) {
                            variablesXmlContentProvider.writeGlobalVariable(typeElement, variable);
                        }
                        foundInGlobalSections = true;
                        break;
                    }
                }
            }
            if (!foundInGlobalSections) {
                try {
                    IFile dataTableFile = dtProject.getFile(usedTypeName + DataTableUtils.FILE_EXTENSION);
                    Document document = XmlUtil.parseWithoutValidation(dataTableFile.getContents(true));
                    Element currentUsertype = usertypes.addElement(ROOT_ELEMENT_NAME);
                    VariableUserType dataTable = UserTypeXmlContentProvider.read(document);
                    currentUsertype.addAttribute(NAME, usedTypeName);
                    for (Variable variable : dataTable.getAttributes()) {
                        Element newUserTypeAttribute = currentUsertype.addElement(ATTRIBUTE_ELEMENT_NAME);
                        newUserTypeAttribute.addAttribute(NAME, variable.getName());
                        newUserTypeAttribute.addAttribute(SCRIPTING_NAME, variable.getScriptingName());
                        newUserTypeAttribute.addAttribute(FORMAT, variable.getFormat());
                        newUserTypeAttribute.addAttribute(DEFAULT_VALUE, variable.getDefaultValue());
                    }
                } catch (Exception e) {
                    PluginLogger.logErrorWithoutDialog(e.getMessage());
                }
            }
        }
    }

    public Set<String> getAllParameterNames(boolean excludeOptional) {
        Set<String> result = Sets.newHashSet();
        for (ParamDefGroup group : getGroups()) {
            for (ParamDef param : group.getParameters()) {
                if (excludeOptional && param.isOptional()) {
                    continue;
                }
                result.add(param.getName());
            }
        }
        return result;
    }
}
