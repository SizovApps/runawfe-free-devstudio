package ru.runa.gpd.util;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableContainer;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.wfe.var.UserType;
import ru.runa.wfe.var.format.ListFormat;
import ru.runa.wfe.var.format.MapFormat;

public class VariableUtils {
    public static final String CURRENT_PROCESS_ID = "${currentProcessId}";
    public static final String CURRENT_PROCESS_DEFINITION_NAME = "${currentDefinitionName}";
    public static final String CURRENT_NODE_NAME = "${currentNodeName}";
    public static final String CURRENT_NODE_ID = "${currentNodeId}";

    public static final List<String> SELECTOR_SPECIAL_NAMES = Lists.newArrayList(VariableUtils.CURRENT_PROCESS_ID,
            VariableUtils.CURRENT_PROCESS_DEFINITION_NAME, VariableUtils.CURRENT_NODE_NAME, VariableUtils.CURRENT_NODE_ID);

    @Deprecated
    public static final String getListVariableComponentFormat(Variable variable) {
        return variable.getFormatComponentClassNames()[0];
    }

    public static Map<String, Variable> toMap(List<Variable> variables) {
        Map<String, Variable> result = Maps.newHashMapWithExpectedSize(variables.size());
        for (Variable variable : variables) {
            result.put(variable.getName(), variable);
        }
        return result;
    }

    public static boolean isValidScriptingName(String name) {
        return Objects.equal(name, toScriptingName(name));
    }

    public static String toScriptingName(String variableName) {
        char[] chars = variableName.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i == 0) {
                if (!Character.isJavaIdentifierStart(chars[i])) {
                    chars[i] = '_';
                }
            } else {
                if (!Character.isJavaIdentifierPart(chars[i])) {
                    chars[i] = '_';
                }
            }
            if ('$' == chars[i]) {
                chars[i] = '_';
            }
        }
        String scriptingName = new String(chars);
        return scriptingName;
    }

    public static String generateNameForScripting(VariableContainer variableContainer, String variableName, Variable excludedVariable) {
        String scriptingName = toScriptingName(variableName);
        if (excludedVariable != null) {
            if (excludedVariable.getScriptingName() == null || Objects.equal(excludedVariable.getScriptingName(), scriptingName)) {
                return scriptingName;
            }
        }
        while (getVariableByScriptingName(variableContainer.getVariables(false, true), scriptingName) != null) {
            scriptingName += "_";
        }
        return scriptingName;
    }

    public static String generateNameForScripting(List<Variable> variableList, String variableName, Variable excludedVariable) {
        String scriptingName = toScriptingName(variableName);
        if (excludedVariable != null) {
            if (excludedVariable.getScriptingName() == null || Objects.equal(excludedVariable.getScriptingName(), scriptingName)) {
                return scriptingName;
            }
        }
        while (getVariableByScriptingName(variableList, scriptingName) != null) {
            scriptingName += "_";
        }
        return scriptingName;
    }

    public static List<String> getVariableNamesForScripting(List<Variable> variables) {
        List<String> result = Lists.newArrayListWithExpectedSize(variables.size());
        for (Variable variable : variables) {
            if (variable.getScriptingName() != null) {
                result.add(variable.getScriptingName());
            } else {
                // this is here due to strange NPE
                PluginLogger.logErrorWithoutDialog("No scriptingName attribute in " + variable.getName());
            }
        }
        return result;
    }

    public static List<String> getVariableNamesForScripting(Delegable delegable, String... typeClassNameFilters) {
        if (delegable instanceof GraphElement) {
            List<Variable> variables = ((GraphElement) delegable).getVariables(true, true, typeClassNameFilters);
            return getVariableNamesForScripting(variables);
        } else {
            List<String> list = delegable.getVariableNames(true, typeClassNameFilters);
            for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
                String string = iterator.next();
                if (!isValidScriptingName(string)) {
                    iterator.remove();
                }
            }
            return list;
        }
    }

    public static List<String> getVariableNames(List<? extends Variable> variables) {
        List<String> result = Lists.newArrayList();
        for (Variable variable : variables) {
            result.add(variable.getName());
        }
        return result;
    }

    /**
     * @return variable or <code>null</code>
     */
    public static Variable getVariableByScriptingName(List<Variable> variables, String name) {
        for (Variable variable : variables) {
            if (Objects.equal(variable.getScriptingName(), name)) {
                return variable;
            }
        }
        return null;
    }

    /**
     * @return variable or <code>null</code>
     */
    public static Variable getVariableByName(VariableContainer variableContainer, String name) {
        if (null == variableContainer) {
            return null;
        }
        List<Variable> variables = variableContainer.getVariables(false, true);
        for (Variable variable : variables) {
            if (Objects.equal(variable.getName(), name)) {
                return variable;
            }
        }
        if (name != null && name.contains(VariableUserType.DELIM)) {
            int index = name.indexOf(VariableUserType.DELIM);
            String complexVariableName = name.substring(0, index);
            Variable complexVariable = getVariableByName(variableContainer, complexVariableName);
            if (complexVariable == null) {
                return null;
            }
            String scriptingName = complexVariable.getScriptingName();
            String attributeName = name.substring(index + 1);
            while (attributeName.contains(VariableUserType.DELIM)) {
                index = attributeName.indexOf(VariableUserType.DELIM);
                complexVariableName = attributeName.substring(0, index);
                complexVariable = getVariableByName(complexVariable.getUserType(), complexVariableName);
                if (complexVariable == null) {
                    return null;
                }
                scriptingName += VariableUserType.DELIM + complexVariable.getScriptingName();
                attributeName = attributeName.substring(index + 1);
            }
            Variable attribute = getVariableByName(complexVariable.getUserType(), attributeName);
            if (attribute != null) {
                scriptingName += VariableUserType.DELIM + attribute.getScriptingName();
                return new Variable(name, scriptingName, attribute);
            }
        }
        return null;
    }

    public static String wrapVariableName(String variableName) {
        return "${" + variableName + "}";
    }

    public static boolean isVariableNameWrapped(String value) {
        return value.length() > 3 && "${".equals(value.substring(0, 2)) && value.endsWith("}");
    }

    public static String unwrapVariableName(String value) {
        if (value.length() > 3) {
            return value.substring(2, value.length() - 1);
        }
        return "";
    }

    private static void searchInVariables(List<Variable> result, VariableUserType searchType, Variable searchAttribute, Variable parent,
            List<Variable> children) {
        for (Variable variable : children) {
            if (variable.getUserType() == null) {
                continue;
            }
            String syntheticName = (parent != null ? (parent.getName() + VariableUserType.DELIM) : "") + variable.getName();
            String syntheticScriptingName = (parent != null ? (parent.getScriptingName() + VariableUserType.DELIM) : "")
                    + variable.getScriptingName();
            if (Objects.equal(variable.getUserType(), searchType)) {
                Variable syntheticVariable = new Variable(syntheticName + VariableUserType.DELIM + searchAttribute.getName(),
                        syntheticScriptingName + VariableUserType.DELIM + searchAttribute.getScriptingName(), searchAttribute);
                result.add(syntheticVariable);
            } else {
                Variable syntheticVariable = new Variable(syntheticName, syntheticScriptingName, variable);
                searchInVariables(result, searchType, searchAttribute, syntheticVariable, variable.getUserType().getAttributes());
            }
        }
    }

    public static List<Variable> findVariablesOfTypeWithAttributeExpanded(VariableContainer variableContainer, VariableUserType searchType,
            Variable searchAttribute) {
        List<Variable> result = Lists.newArrayList();
        searchInVariables(result, searchType, searchAttribute, null, variableContainer.getVariables(false, false));
        return result;
    }

    public static List<Variable> expandComplexVariable(Variable superVariable, Variable complexVariable) {
        Preconditions.checkArgument(complexVariable.isComplex(), "User type variable expected");
        List<Variable> result = Lists.newArrayList();
        for (Variable attribute : complexVariable.getUserType().getAttributes()) {
            String name = superVariable.getName() + VariableUserType.DELIM + attribute.getName();
            String scriptingName = superVariable.getScriptingName() + VariableUserType.DELIM + attribute.getScriptingName();
            Variable variable = new Variable(name, scriptingName, attribute);
            result.add(variable);
            if (variable.isComplex()) {
                result.addAll(expandComplexVariable(variable, attribute));
            }
        }
        return result;
    }

    public static Set<VariableUserType> getUsedUserTypes(VariableContainer variableContainer, String variableName) {
        String[] nameParts = variableName.split(Pattern.quote(VariableUserType.DELIM));
        Set<VariableUserType> variableUserTypes = Sets.newHashSet();
        for (int i = 0; i < nameParts.length; i++) {
            Variable variablePart = getVariableByName(variableContainer, nameParts[i]);
            VariableUserType variableUserType = variablePart.getUserType();
            if (variableUserType != null) {
                variableUserTypes.add(variableUserType);
                variableContainer = variableUserType;
            }
        }
        return variableUserTypes;
    }

    public static Variable getComplexVariableByExpandedAttribute(VariableContainer variableContainer, String variableName) {
        return getVariableByName(variableContainer, variableName.split(Pattern.quote(VariableUserType.DELIM))[0]);
    }

    public static boolean isContainerVariable(Variable v) {
        String fcn = v.getFormatClassName();
        return fcn.equals(ListFormat.class.getName()) || fcn.equals(MapFormat.class.getName());
    }

    public static boolean isValidUserTypeName(String value) {
        return value.indexOf(".") < 0;
    }

    public static List<String> getUserTypeExpandedAttributeNames(VariableUserType userType) {
        List<String> result = Lists.newArrayList();
        if (userType != null) {
            for (Variable variable : userType.getAttributes()) {
                result.add(variable.getName());
                if (variable.isComplex()) {
                    for (String childAttributeName : getUserTypeExpandedAttributeNames(variable.getUserType())) {
                        result.add(variable.getName() + UserType.DELIM + childAttributeName);
                    }
                }
            }
        }
        return result;
    }

    public static void renameUserType(ProcessDefinition pd, VariableUserType type, String newTypeName) {
        final String[] typeUsages = { "\\({0}\\)", "\\({0},", ", {0}\\)", ", {0}," };
        String oldTypeName = type.getName();
        List<VariableUserType> userTypes = pd.getVariableUserTypes();
        for (VariableUserType userType : userTypes) {
            List<Variable> attributes = userType.getAttributes();
            for (Variable attribute : attributes) {
                if (attribute.getFormat().contains(oldTypeName)) {
                    for (String typeUsage : typeUsages) {
                        attribute.setFormat(attribute.getFormat().replaceAll(MessageFormat.format(typeUsage, oldTypeName),
                                MessageFormat.format(typeUsage, newTypeName)));
                    }
                }
            }
        }
        List<Variable> complexVariables = Lists.newArrayList();
        List<Variable> variables = pd.getVariables(false, true);
        for (Variable variable : variables) {
            if (variable.isComplex()) {
                if (variable.getUserType().getName().equals(oldTypeName)) {
                    complexVariables.add(variable);
                }
            } else if (variable.getFormat().contains(oldTypeName)) {
                for (String typeUsage : typeUsages) {
                    variable.setFormat(variable.getFormat().replaceAll(MessageFormat.format(typeUsage, oldTypeName),
                            MessageFormat.format(typeUsage, newTypeName)));
                }
            }
        }
        type.setName(newTypeName);
        for (Variable variable : complexVariables) {
            variable.setUserType(type);
        }
    }

    public static boolean variableExists(String variableName, ProcessDefinition processDefinition) {
        return processDefinition.getVariableNames(true, true).contains(variableName);
    }

}
