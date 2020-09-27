package ru.runa.gpd.util.docx;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.commons.GroovyScriptExecutor;
import ru.runa.wfe.commons.TypeConversionUtil;
import ru.runa.wfe.var.VariableProvider;
import ru.runa.wfe.var.dto.WfVariable;
import ru.runa.wfe.var.file.FileVariable;
import ru.runa.wfe.var.format.UserTypeFormat;
import ru.runa.wfe.var.format.VariableFormat;
import ru.runa.wfe.var.format.VariableFormatContainer;

public class DocxUtils {
    private static final Log log = LogFactory.getLog(DocxUtils.class);

    private static final String GROOVY = "groovy:";
    private static final String LINE_DELIMITER = "\n";
    private static final String ITERATOR_NAME_DELIMITER = " as ";
    private static final Pattern STRIP_HTML_TAGS_PATTERN = Pattern.compile("<.+?>");

    public static final String PLACEHOLDER_START = OfficeProperties.getDocxPlaceholderStart();
    public static final String PLACEHOLDER_END = OfficeProperties.getDocxPlaceholderEnd();
    public static final String CLOSING_PLACEHOLDER_START = PLACEHOLDER_START + "/";

    public static int getPictureType(DocxConfig config, String fileName) {
        if (fileName.endsWith(".emf")) {
            return XWPFDocument.PICTURE_TYPE_EMF;
        } else if (fileName.endsWith(".wmf")) {
            return XWPFDocument.PICTURE_TYPE_WMF;
        } else if (fileName.endsWith(".pict")) {
            return XWPFDocument.PICTURE_TYPE_PICT;
        } else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            return XWPFDocument.PICTURE_TYPE_JPEG;
        } else if (fileName.endsWith(".png")) {
            return XWPFDocument.PICTURE_TYPE_PNG;
        } else if (fileName.endsWith(".dib")) {
            return XWPFDocument.PICTURE_TYPE_DIB;
        } else if (fileName.endsWith(".gif")) {
            return XWPFDocument.PICTURE_TYPE_GIF;
        } else if (fileName.endsWith(".tiff")) {
            return XWPFDocument.PICTURE_TYPE_TIFF;
        } else if (fileName.endsWith(".eps")) {
            return XWPFDocument.PICTURE_TYPE_EPS;
        } else if (fileName.endsWith(".bmp")) {
            return XWPFDocument.PICTURE_TYPE_BMP;
        } else if (fileName.endsWith(".wpg")) {
            return XWPFDocument.PICTURE_TYPE_WPG;
        }
        config.reportProblem("Unsupported picture: " + fileName + ". Expected emf|wmf|pict|jpeg|png|dib|gif|tiff|eps|bmp|wpg");
        return -1;
    }

    public static void replaceInParagraphs(DocxConfig config, VariableConsumer variableProvider, List<XWPFParagraph> paragraphs,
            boolean scriptParseMode) {
        Stack<Operation> operations = new Stack<Operation>();
        for (XWPFParagraph paragraph : Lists.newArrayList(paragraphs)) {
            String paragraphText = paragraph.getText();
            LoopOperation loopOperation = parseIterationOperation(config, variableProvider, paragraphText, new LoopOperation(), scriptParseMode);
            if (loopOperation != null && loopOperation.isValid()) {
                loopOperation.setHeaderParagraph(paragraph);
                operations.push(loopOperation);
                continue;
            } else if (!operations.isEmpty()) {
                if (operations.peek() instanceof LoopOperation) {
                    if (operations.peek().isEndBlock(paragraphText)) {
                        XWPFDocument document = paragraph.getDocument();
                        int insertPosition = document.getParagraphPos(document.getPosOfParagraph(paragraph));
                        loopOperation = (LoopOperation) operations.pop();
                        variableProvider.remove(loopOperation.getIteratorVariableName());
                        continue;
                    }
                    ((LoopOperation) operations.peek()).getBodyParagraphs().add(paragraph);
                } else if (operations.peek() instanceof IfOperation) {
                }
            } else {
                replaceInParagraph(config, variableProvider, paragraph);
            }
        }
    }

    static <T extends AbstractIteratorOperation> T parseIterationOperation(DocxConfig config, VariableConsumer variableProvider, String string,
            T operation, boolean scriptParseMode) {
        if (Strings.isNullOrEmpty(string)) {
            return null;
        }
        string = string.trim();
        if (string.startsWith(CLOSING_PLACEHOLDER_START)) {
            return null;
        }
        if (string.startsWith(PLACEHOLDER_START) && string.endsWith(PLACEHOLDER_END)) {
            String placeholder = string.substring(PLACEHOLDER_START.length(), string.length() - PLACEHOLDER_END.length());
            if (placeholder.startsWith(GROOVY)) {
                return null;
            }
            int iteratorNameIndex = placeholder.lastIndexOf(ITERATOR_NAME_DELIMITER);
            String iteratorWithContainerVariable = placeholder;
            if (iteratorNameIndex != -1) {
                iteratorWithContainerVariable = iteratorWithContainerVariable.substring(0, iteratorNameIndex).trim();
            } else {
                iteratorWithContainerVariable = placeholder;
            }
            int colonIndex = iteratorWithContainerVariable.indexOf(":");
            if (colonIndex > 0) {
                try {
                    operation.setIterateBy(IterateBy.identifyByString(config, iteratorWithContainerVariable));
                } catch (Exception e) {
                    return null;
                }
                operation.setContainerVariableName(iteratorWithContainerVariable.substring(colonIndex + 1).trim());
            } else {
                return null;
            }
            if (iteratorNameIndex != -1) {
                String lexem = placeholder.substring(iteratorNameIndex + ITERATOR_NAME_DELIMITER.length()).trim();
                if (operation instanceof ColumnExpansionOperation) {
                    ((ColumnExpansionOperation) operation).setContainerSelector(lexem);
                }
                if (operation instanceof LoopOperation) {
                    ((LoopOperation) operation).setIteratorVariableName(lexem);
                }

                if (scriptParseMode) {
                    String variableName = operation.getContainerVariableName() + "." + lexem;
                    variableProvider.getVariable(variableName);
                }
            }
            if (operation.getContainerVariableName().contains(PLACEHOLDER_START)) {
                // this is the case of multiple replacements in one line
                return null;
            }
            if (operation.getContainerVariableName().startsWith(GROOVY)) {
                operation.setContainerValue(executeGroovy(variableProvider, operation.getContainerVariableName()));
            } else {
                WfVariable variable = variableProvider.getVariable(operation.getContainerVariableName(), true);
                if (variable != null) {
                    operation.setContainerVariable(variable);
                } else {
                    config.warn("not an iteration operation: Variable not found by '" + placeholder + "' (checked '"
                            + operation.getContainerVariableName() + "')");
                }
            }
            if (!operation.isValid()) {
                // config.reportProblem("Invalid " + operation + " for '" +
                // placeholder + "'");
                return null;
            }
            return operation;
        }
        return null;
    }

    public static void replaceInParagraph(DocxConfig config, VariableConsumer variableProvider, XWPFParagraph paragraph) {
        String paragraphText = paragraph.getParagraphText();
        if (!paragraphText.contains(PLACEHOLDER_START)) {
            return;
        }
        if (!paragraphText.contains(PLACEHOLDER_END)) {
            config.warn("No placeholder end '" + PLACEHOLDER_END + "' found for '" + PLACEHOLDER_START + "' in " + paragraphText);
            return;
        }
        List<XWPFRun> paragraphRuns = Lists.newArrayList(paragraph.getRuns());
        int whetherSingleRunContainsPlaceholderStart = 0;
        int whetherMultRunContainsPlaceholderStart = 0;
        int whetherSingleRunContainsPlaceholderEnd = 0;
        for (int i = 0; i < paragraphRuns.size(); i++) {
            XWPFRun run = paragraphRuns.get(i);
            XWPFRun next = i + 1 < paragraphRuns.size() ? paragraphRuns.get(i + 1) : null;
            if (run == null || run.getText(0) == null) {
                continue;
            }
            if (run.getText(0).contains(PLACEHOLDER_START)) {
                whetherSingleRunContainsPlaceholderStart++;
            }
            if (run.getText(0).contains(PLACEHOLDER_END)) {
                whetherSingleRunContainsPlaceholderEnd++;
            }
            if (next == null || next.getText(0) == null || PLACEHOLDER_START.length() < 2) {
                continue;
            }
            int j = 1;
            String test = PLACEHOLDER_START.substring(0, j);
            while (j < PLACEHOLDER_START.length() && !run.getText(0).endsWith(test)) {
                test = PLACEHOLDER_START.substring(0, ++j);
            }
            if (j == PLACEHOLDER_START.length() || !next.getText(0).startsWith(PLACEHOLDER_START.substring(j, PLACEHOLDER_START.length()))) {
                continue;
            }
            whetherMultRunContainsPlaceholderStart++;
        }
        if (whetherMultRunContainsPlaceholderStart > 0) {
            fixRunsToStateInWhichSingleRunContainsPlaceholder(config, paragraph, PLACEHOLDER_START);
        }
        if (whetherSingleRunContainsPlaceholderEnd < whetherSingleRunContainsPlaceholderStart + whetherMultRunContainsPlaceholderStart) {
            fixRunsToStateInWhichSingleRunContainsPlaceholder(config, paragraph, PLACEHOLDER_END);
        }
        List<ReplaceOperation> operations = Lists.newArrayList();
        for (XWPFRun run : Lists.newArrayList(paragraph.getRuns())) {
            if (run == null) {
                log.warn("Null run in paragraph " + paragraphText);
                continue;
            }
            CTR ctr = run.getCTR();
            int tArraySize = ctr.sizeOfTArray();
            if (tArraySize == 0) {
                log.warn("Null run CTR value in paragraph " + paragraphText);
                continue;
            }
            for (int index = 0; index < tArraySize; index++) {
                String text = ctr.getTArray(index).getStringValue();
                if (text == null) {
                    log.warn("Null run[" + index + "] value in paragraph " + paragraphText);
                    continue;
                }
                String replacedText = replaceText(config, variableProvider, operations, text);
                if (!Objects.equal(replacedText, text)) {
                    if (replacedText.contains(LINE_DELIMITER)) {
                        StringTokenizer tokenizer = new StringTokenizer(replacedText, LINE_DELIMITER);
                        while (tokenizer.hasMoreTokens()) {
                            run.setText(tokenizer.nextToken(), 0);
                            if (tokenizer.hasMoreTokens()) {
                                run.addBreak();
                                run = paragraph.insertNewRun(paragraph.getRuns().indexOf(run) + 1);
                            }
                        }
                    } else {
                        run.setText(replacedText, index);
                    }
                }
            }
            for (ReplaceOperation replaceOperation : Lists.newArrayList(operations)) {
                if (replaceOperation instanceof InsertImageOperation) {
                    InsertImageOperation imageOperation = (InsertImageOperation) replaceOperation;
                    FileVariable fileVariable = imageOperation.getFileVariable();
                    try {
                        run.addPicture(new ByteArrayInputStream(fileVariable.getData()), imageOperation.getImageType(), fileVariable.getName(),
                                imageOperation.getWidth(), imageOperation.getHeight());
                    } catch (Exception e) {
                        config.reportProblem(e);
                    }
                    operations.remove(replaceOperation);
                }
            }
        }
    }

    private static void fixRunsToStateInWhichSingleRunContainsPlaceholder(DocxConfig config, XWPFParagraph paragraph, String placeholder) {
        // config.warn("Restructuring runs for '" + placeholder + "' in '" + paragraph.getParagraphText() + "'");
        try {
            List<XWPFRun> runs = paragraph.getRuns();
            for (int i = 0; i < runs.size() - 1; i++) {
                PlaceholderMatch match = new PlaceholderMatch(placeholder.toCharArray());
                PlaceholderMatch.Status status = match.testRun(runs.get(i));
                int next = i + 1;
                while (status == PlaceholderMatch.Status.MOVE_NEXT_RUN) {
                    status = match.testRun(runs.get(next));
                    next++;
                }
                if (status == PlaceholderMatch.Status.MOVE_NEW_RUN) {
                    continue;
                }
                if (status == PlaceholderMatch.Status.COMPLETED) {
                    for (int n = 0; n < match.runs.size(); n++) {
                        if (n == 0) {
                            String newText = match.runs.get(n).getText(0);
                            newText = newText.substring(0, match.comparisonStartInFirstRunIndex) + placeholder;
                            match.runs.get(n).setText(newText, 0);
                        } else if (n == match.runs.size() - 1) {
                            String newText = match.runs.get(n).getText(0);
                            newText = newText.substring(match.comparisonEndInLastRunIndex);
                            match.runs.get(n).setText(newText, 0);
                        } else {
                            match.runs.get(n).setText("", 0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            config.reportProblem(new Exception("Unable to adjust runs for '" + PLACEHOLDER_START + "' in '" + paragraph.getParagraphText() + "'", e));
        }
    }

    private static class PlaceholderMatch {
        enum Status {
            MOVE_NEW_RUN,
            MOVE_NEXT_RUN,
            COMPLETED
        }

        final char[] placeholderChars;
        final List<XWPFRun> runs = Lists.newArrayList();
        int currentComparisonIndex = 0;
        int comparisonStartInFirstRunIndex = -1;
        int comparisonEndInLastRunIndex = -1;

        private PlaceholderMatch(char[] placeholderChars) {
            this.placeholderChars = placeholderChars;
        }

        Status testRun(XWPFRun run) {
            boolean firstRun = runs.size() == 0;
            runs.add(run);
            char[] runChars = run.getText(0).toCharArray();
            for (int j = 0; j < runChars.length; j++) {
                if (runChars[j] == placeholderChars[currentComparisonIndex]) {
                    if (firstRun && currentComparisonIndex == 0) {
                        comparisonStartInFirstRunIndex = j;
                    }
                    currentComparisonIndex++;
                    if (isMatchCompleted()) {
                        comparisonEndInLastRunIndex = j + 1;
                        return Status.COMPLETED;
                    }
                } else if (currentComparisonIndex != 0) {
                    if (firstRun) {
                        currentComparisonIndex = 0;
                    } else {
                        break;
                    }
                }
            }
            return currentComparisonIndex != 0 ? Status.MOVE_NEXT_RUN : Status.MOVE_NEW_RUN;
        }

        boolean isMatchCompleted() {
            return currentComparisonIndex == placeholderChars.length;
        }
    }

    private static String replaceText(DocxConfig config, VariableConsumer variableProvider, List<ReplaceOperation> operations, String text) {
        ReplaceOperation operation;
        if (operations.size() > 0 && !operations.get(operations.size() - 1).isPlaceholderRead()) {
            operation = operations.get(operations.size() - 1);
        } else {
            operation = new ReplaceOperation();
            operations.add(operation);
        }
        if (!operation.isStarted()) {
            // search start
            int placeholderStartIndex = text.indexOf(PLACEHOLDER_START);
            if (placeholderStartIndex >= 0) {
                String start = text.substring(0, placeholderStartIndex);
                operation.appendPlaceholder("");
                String remainder = text.substring(placeholderStartIndex + PLACEHOLDER_START.length());
                return start + replaceText(config, variableProvider, operations, remainder);
            }
            return text;
        } else {
            // search end
            int placeholderEndIndex = text.indexOf(PLACEHOLDER_END);
            if (placeholderEndIndex >= 0) {
                operation.appendPlaceholder(text.substring(0, placeholderEndIndex));
                operation.setEnded(true);
                String remainder = text.substring(placeholderEndIndex + PLACEHOLDER_END.length());
                Object value = getValue(config, variableProvider, null, operation.getPlaceholder());
                if (value == null) {
                    /*
                     * if (config.isStrictMode()) { config.reportProblem("No template variable defined in process: '" + operation.getPlaceholder() +
                     * "'"); }
                     */
                }
                if (value instanceof FileVariable) {
                    try {
                        operations.remove(operation);
                        FileVariable fileVariable = (FileVariable) value;
                        InsertImageOperation imageOperation = new InsertImageOperation(operation.getPlaceholder(), fileVariable);
                        imageOperation.setValue("");
                        int imageType = getPictureType(config, fileVariable.getName().toLowerCase());
                        if (imageType > 0) {
                            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileVariable.getData()));
                            // does not work without ooxml
                            imageOperation.setImageType(imageType);
                            imageOperation.setWidth(Units.toEMU(image.getWidth()));
                            imageOperation.setHeight(Units.toEMU(image.getHeight()));
                            operations.add(imageOperation);
                            operation = imageOperation;
                        }
                    } catch (Exception e) {
                        config.reportProblem(e);
                    }
                } else {
                    VariableFormat valueFormat = null;
                    String placeholder = operation.getPlaceholder();
                    if (placeholder.contains(VariableFormatContainer.COMPONENT_QUALIFIER_START)
                            && placeholder.endsWith(VariableFormatContainer.COMPONENT_QUALIFIER_END)) {
                        placeholder = placeholder.substring(0, placeholder.indexOf(VariableFormatContainer.COMPONENT_QUALIFIER_START));
                        WfVariable containerVariable = variableProvider.getVariable(placeholder);
                        if (containerVariable != null) {
                            int index = containerVariable.getValue() instanceof Map ? 1 : 0;
                            if (containerVariable.getDefinition().getFormatComponentUserTypes() != null
                                    && containerVariable.getDefinition().getFormatComponentUserTypes().length > index
                                    && containerVariable.getDefinition().getFormatComponentUserTypes()[index] != null) {
                                valueFormat = new UserTypeFormat(containerVariable.getDefinition().getFormatComponentUserTypes()[index]);
                            } else {
                                valueFormat = ClassLoaderUtil.instantiate(containerVariable.getDefinition().getFormatComponentClassNames()[index]);
                            }
                        }
                    } else {
                        if (!placeholder.startsWith(GROOVY)) {
                            WfVariable variable = variableProvider.getVariable(placeholder);
                            if (variable != null) {
                                valueFormat = variable.getDefinition().getFormatNotNull();
                            }
                        }
                    }
                    String replacement;
                    if (valueFormat != null) {
                        replacement = valueFormat.format(value);
                        if (replacement == null) {
                            replacement = "";
                        }
                    } else {
                        replacement = TypeConversionUtil.convertTo(String.class, value);
                    }
                    operation.setValue(replacement);
                }
                return operation.getValue() + replaceText(config, variableProvider, operations, remainder);
            } else {
                operation.appendPlaceholder(text);
                return "";
            }
        }
    }

    public static Object getValue(DocxConfig config, VariableConsumer variableProvider, Object value, String selector) {
        if (value == null) {
            if (selector.startsWith(GROOVY)) {
                String script = selector.substring(GROOVY.length());
                variableProvider.parseGroovy(script.trim());
                // return executeGroovy(variableProvider, selector);
                return "";
            }
            if (!Strings.isNullOrEmpty(selector)) {
                value = variableProvider.getValue(selector);
                if (value != null) {
                    return value;
                }
            }
        }
        if (!Strings.isNullOrEmpty(selector)) {
            StringTokenizer tokenizer = new StringTokenizer(selector, "\\.");
            while (tokenizer.hasMoreTokens()) {
                String variableName = tokenizer.nextToken();
                String keyName = null;
                int elementStartIndex = variableName.indexOf(VariableFormatContainer.COMPONENT_QUALIFIER_START);
                if (elementStartIndex > 0 && variableName.endsWith(VariableFormatContainer.COMPONENT_QUALIFIER_END)) {
                    keyName = variableName.substring(elementStartIndex + VariableFormatContainer.COMPONENT_QUALIFIER_START.length(),
                            variableName.length() - VariableFormatContainer.COMPONENT_QUALIFIER_END.length());
                    variableName = variableName.substring(0, elementStartIndex);
                }
                if (value == null) {
                    value = variableProvider.getValue(variableName);
                } else {
                    if (value instanceof Map) {
                        value = ((Map<?, ?>) value).get(variableName);
                    } else {
                        try {
                            value = variableProvider.getProperty(value, variableName);
                        } catch (Exception e) {
                            config.reportProblem(e);
                        }
                    }
                }
                if (value == null) {
                    config.warn("returning null for " + selector + " at stage " + variableName);
                    return null;
                }
                if (keyName != null) {
                    if (value instanceof Map) {
                        Object key = variableProvider.getValue(keyName);
                        if (key == null) {
                            key = keyName;
                            if (keyName.startsWith("\"") && keyName.endsWith("\"")) {
                                key = keyName.substring(1, keyName.length() - 1);
                            }
                        }
                        value = ((Map<?, ?>) value).get(key);
                    } else if (value instanceof List) {
                        Integer index;
                        try {
                            index = Integer.parseInt(keyName);
                        } catch (Exception e) {
                            index = variableProvider.getValue(Integer.class, keyName);
                        }
                        if (index == null) {
                            config.reportProblem("Null index for " + keyName);
                        }
                        value = TypeConversionUtil.getListValue(value, index);
                    } else {
                        config.reportProblem("Unable to get element '" + keyName + "' value from " + value);
                    }
                }
            }
        } else if (value instanceof String) {
            if (((String) value).startsWith(GROOVY)) {
                return executeGroovy(variableProvider, (String) value);
            }
            value = ((String) value).replaceAll(Pattern.quote("</p>"), "\n").replaceAll("&nbsp;", " ");
            Matcher m = STRIP_HTML_TAGS_PATTERN.matcher((String) value);
            return m.replaceAll("");
        }
        return value;
    }

    private static Object executeGroovy(VariableProvider variableProvider, String script) {
        script = script.substring(GROOVY.length());
        GroovyScriptExecutor executor = new GroovyScriptExecutor();
        return executor.evaluateScript(variableProvider, script);
    }

    public static void setCellText(XWPFTableCell cell, String text, XWPFTableCell templateCell) {
        // TODO Auto-generated method stub
        int t = 0;
        t++;
    }

    public static void setCellText(XWPFTableCell cell, String text0) {
        // TODO Auto-generated method stub
        int t = 0;
        t++;
    }

    public static char[] getValue(DocxConfig config, VariableProvider variableProvider, Object listItem, String containerSelector) {
        // TODO Auto-generated method stub
        int t = 0;
        t++;
        return null;
    }

}
