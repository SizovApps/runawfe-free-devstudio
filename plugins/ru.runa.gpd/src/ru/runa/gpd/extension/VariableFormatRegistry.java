package ru.runa.gpd.extension;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import ru.runa.gpd.Activator;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.IOUtils;
import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.var.UserTypeMap;

public class VariableFormatRegistry extends ArtifactRegistry<VariableFormatArtifact> {
    private static final String XML_FILE_NAME = "variableFormats.xml";
    private static final VariableFormatRegistry instance = new VariableFormatRegistry();
    private List<VariableFormatArtifact> filterArtifacts;

    public static VariableFormatRegistry getInstance() {
        return instance;
    }

    public VariableFormatRegistry() {
        super(new ArtifactContentProvider<VariableFormatArtifact>());
    }

    @Override
    protected File getContentFile() {
        return new File(Activator.getPreferencesFolder(), XML_FILE_NAME);
    }

    @Override
    protected void loadDefaults(List<VariableFormatArtifact> list) {
        IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint("ru.runa.gpd.formats").getExtensions();
        Map<String, List<String>> mappingByTypeClass = Maps.newHashMap();
        for (IExtension extension : extensions) {
            IConfigurationElement[] configElements = extension.getConfigurationElements();
            for (IConfigurationElement configElement : configElements) {
                boolean enabled = Boolean.valueOf(configElement.getAttribute("enabled"));
                String className = configElement.getAttribute("className");
                String label = configElement.getAttribute("label");
                String javaClassName = configElement.getAttribute("javaClassName");
                list.add(new VariableFormatArtifact(enabled, className, label, javaClassName));
                if ("ru.runa.wfe.var.format.ProcessIdFormat".equals(className)) {
                    continue;
                }
                if (!mappingByTypeClass.containsKey(javaClassName)) {
                    mappingByTypeClass.put(javaClassName, new ArrayList<String>());
                }
                mappingByTypeClass.get(javaClassName).add(label);
            }
        }
        filterArtifacts = Lists.newArrayList();
        filterArtifacts.add(new VariableFormatArtifact(true, null, Localization.getString("label.any"), Object.class.getName()));
        for (Map.Entry<String, List<String>> entry : mappingByTypeClass.entrySet()) {
            String label = Joiner.on(", ").join(entry.getValue());
            filterArtifacts.add(new VariableFormatArtifact(true, null, label, entry.getKey()));
        }
        Collections.sort(filterArtifacts);
    }

    public static boolean isAssignableFrom(String superClassName, String className) {
        try {
            return isAssignableFrom(Class.forName(superClassName), className);
        } catch (ClassNotFoundException e) {
            // UserType
            return false;
        } catch (Throwable th) {
            PluginLogger.logErrorWithoutDialog(superClassName, th);
            return false;
        }
    }

    public static boolean isAssignableFrom(Class<?> superClass, String className) {
        try {
            Class<?> testingClass = Class.forName(className);
            return superClass.isAssignableFrom(testingClass);
        } catch (ClassNotFoundException e) {
            // UserType
            return false;
        } catch (Throwable th) {
            PluginLogger.logErrorWithoutDialog(className, th);
            return false;
        }
    }

    public static boolean isApplicable(Variable variable, String... classNameFilters) {
        boolean applicable = classNameFilters == null || classNameFilters.length == 0;
        if (!applicable) {
            for (String typeClassNameFilter : classNameFilters) {
                if (VariableFormatRegistry.isApplicable(variable, typeClassNameFilter)) {
                    applicable = true;
                    break;
                }
            }
        }
        return applicable;
    }

    public static boolean isApplicable(Variable variable, String classNameFilter) {
        if (variable.isComplex()) {
            if (Objects.equal(Object.class.getName(), classNameFilter)) {
                return true;
            }
            if (Objects.equal(UserTypeMap.class.getName(), classNameFilter)) {
                return true;
            }
            return Objects.equal(variable.getUserType().getName(), classNameFilter);
        }
        if (List.class.getName().equals(variable.getJavaClassName())
                && classNameFilter.contains(Variable.FORMAT_COMPONENT_TYPE_START)
                && classNameFilter.contains(Variable.FORMAT_COMPONENT_TYPE_END)) {
            String[] formatComponentClassNames = variable.getFormatComponentClassNames();
            if (formatComponentClassNames.length == 1) {
                VariableFormatArtifact variableFormatArtifact = getInstance().getArtifact(formatComponentClassNames[0]);
                if (variableFormatArtifact != null) {
                    String componentClassNameFilter = classNameFilter.substring(classNameFilter.indexOf(Variable.FORMAT_COMPONENT_TYPE_START) + 1,
                            classNameFilter.indexOf(Variable.FORMAT_COMPONENT_TYPE_END));
                    String componentClassName = variableFormatArtifact.getJavaClassName();
                    return isAssignableFrom(componentClassNameFilter, componentClassName)
                            || isAssignableFrom(componentClassName, componentClassNameFilter);
                } else {
                    String userTypeClassName = variable.getJavaClassName() + Variable.FORMAT_COMPONENT_TYPE_START + formatComponentClassNames[0]
                            + Variable.FORMAT_COMPONENT_TYPE_END;
                    return Objects.equal(classNameFilter, userTypeClassName);
                }
            }
        }
        return isAssignableFrom(classNameFilter, variable.getJavaClassName()) || isAssignableFrom(variable.getJavaClassName(), classNameFilter);
    }

    public VariableFormatArtifact getArtifactByJavaClassName(String javaClassName) {
        for (VariableFormatArtifact artifact : getAll()) {
            if (Objects.equal(javaClassName, artifact.getJavaClassName())) {
                return artifact;
            }
        }
        return null;
    }

    public VariableFormatArtifact getArtifactNotNullByJavaClassName(String javaClassName) {
        VariableFormatArtifact artifact = getArtifactByJavaClassName(javaClassName);
        if (artifact == null) {
            throw new RuntimeException("Artifact javaClassName='" + javaClassName + "' does not exist");
        }
        return artifact;
    }

    public List<VariableFormatArtifact> getFilterArtifacts() {
        return filterArtifacts;
    }

    public String getFilterLabel(String javaClassName) {
        PluginLogger.logInfo("Find getFilterLabel: " + javaClassName);
        String addTableList = "";
        if (javaClassName.contains("(")) {
            addTableList = javaClassName.substring(javaClassName.indexOf('('));
            javaClassName = javaClassName.substring(0, javaClassName.indexOf('('));
        }
        for (VariableFormatArtifact artifact : filterArtifacts) {
            PluginLogger.logInfo("artifact.getJavaClassName(): " + artifact.getJavaClassName());
            if (Objects.equal(javaClassName, artifact.getJavaClassName())) {
                if (addTableList != "") {
                    return artifact.getLabel() + addTableList;
                }
                return artifact.getLabel();
            }
        }
        String finalJavaClassName = javaClassName;
        return getVariableName(javaClassName).orElseThrow(() -> new InternalApplicationException("No filter found by type " + finalJavaClassName));
    }

    public String getFilterJavaClassName(String label) {
        PluginLogger.logInfo("Find getFilterJavaClassName: " + label);
        String addTableList = "";
        if (label.contains("(")) {
            addTableList = label.substring(label.indexOf('('));
            label = label.substring(0, label.indexOf('('));
        }
        for (VariableFormatArtifact artifact : filterArtifacts) {
            PluginLogger.logInfo("artifact.getLabel(): " + artifact.getLabel());

            if (Objects.equal(label, artifact.getLabel()) || artifact.getLabel().contains(label)) {
                if (addTableList != "") {
                    return artifact.getJavaClassName() + addTableList;
                }
                return artifact.getJavaClassName();
            }

        }
        String finalLabel = label;
        return getVariableName(label).orElseThrow(() -> new InternalApplicationException("No filter found by label " + finalLabel));
    }

    private static Optional<String> getVariableName(String name) {
        Optional<String> variableName = Optional.empty();
        try {
            variableName = Arrays.stream(DataTableUtils.getDataTableProject().members())
                    .filter(r -> r instanceof IFile && r.getName().endsWith(DataTableUtils.FILE_EXTENSION))
                    .map(r -> IOUtils.getWithoutExtension(r.getName()))
                    .filter(n -> n.equals(name))
                    .findFirst();
        } catch (Exception ignored) {
        }
        return variableName.isPresent() ? variableName : ProcessCache.getAllProcessDefinitions().stream()
                .map(ProcessDefinition::getVariableUserTypes).flatMap(List::stream)
                .map(VariableUserType::getName)
                .filter(n -> n.equals(name))
                .findFirst();
    }

    public String getUserTypeOfList(String userTypeName) {
        String usertype = userTypeName.substring(userTypeName.indexOf('(') + 1, userTypeName.indexOf(')'));
        PluginLogger.logInfo("Inner usertype: " + usertype);
        return usertype;
    }
}
