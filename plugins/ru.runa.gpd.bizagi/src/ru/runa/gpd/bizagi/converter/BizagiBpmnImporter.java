package ru.runa.gpd.bizagi.converter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.editor.GEFConstants;
import ru.runa.gpd.editor.graphiti.GraphitiEntry;
import ru.runa.gpd.lang.BpmnSerializer;
import ru.runa.gpd.lang.Language;
import ru.runa.gpd.lang.NodeRegistry;
import ru.runa.gpd.lang.NodeTypeDefinition;
import ru.runa.gpd.lang.ProcessSerializer;
import ru.runa.gpd.lang.model.EndState;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.MultiSubprocess;
import ru.runa.gpd.lang.model.NamedGraphElement;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.Subprocess;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.SwimlanedNode;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.lang.model.Timer;
import ru.runa.gpd.lang.model.Transition;
import ru.runa.gpd.lang.model.bpmn.CatchEventNode;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;
import ru.runa.gpd.lang.model.bpmn.ExclusiveGateway;
import ru.runa.gpd.lang.model.bpmn.ParallelGateway;
import ru.runa.gpd.lang.model.bpmn.ScriptTask;
import ru.runa.gpd.lang.model.bpmn.TextAnnotation;
import ru.runa.gpd.lang.model.bpmn.ThrowEventNode;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;

@SuppressWarnings("unchecked")
public class BizagiBpmnImporter implements GEFConstants {

    private static final double SCALE_FACTOR = 2;

    private static final String TARGET_NAMESPACE = "targetNamespace";
    private static final String BPMN_TARGET_NAMESPACE = "http://bpmn.io/schema/bpmn";

    private static final String DEFAULT = "default";
    private static final String ASSOCIATION = "association";
    private static final String EMPTY_ROLE = "*";
    private static final String Y = "y";
    private static final String X = "x";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";
    private static final String BOUNDS = "Bounds";
    private static final String WAYPOINT = "waypoint";
    private static final String TARGET_REF = "targetRef";
    private static final String SOURCE_REF = "sourceRef";
    private static final String ATTACHED_TO_REF = "attachedToRef";
    private static final String DOCUMENTATION = "documentation";
    private static final String LANE = "lane";
    private static final String LANE_SET = "laneSet";
    private static final String IS_HORIZONTAL = "isHorizontal";
    private static final String TRUE = "true";
    private static final String ID = "id";
    private static final String PROCESS = "process";
    private static final String PROCESS_REF = "processRef";
    private static final String NAME = "name";
    private static final String IS_MAIN_PARTICIPANT = "isMainParticipant";
    private static final String EXTENSION_ELEMENTS = "extensionElements";
    private static final String PARTICIPANT = "participant";
    private static final String COLLABORATION = "collaboration";
    private static final String BPMN_ELEMENT = "bpmnElement";
    private static final String BPMN_PLANE = "BPMNPlane";
    private static final String BPMN_DIAGRAM = "BPMNDiagram";
    private static final String MESSAGE_EVENT_DEFINITION = "messageEventDefinition";
    private static final String ERROR_EVENT_DEFINITION = "errorEventDefinition";
    private static final String TIMER_EVENT_DEFINITION = "timerEventDefinition";
    private static final String TERMINATE_EVENT_DEFINITION = "terminateEventDefinition";
    private static final String MULTI_INSTANCE_LOOP_CHARACTERISTICS = "multiInstanceLoopCharacteristics";
    private static final String BIZAGI_NAMESPACE = "http://www.bizagi.com/bpmn20";
    private static final String BIZAGI_PROPERTY = "BizagiProperty";
    private static final String BIZAGI_PROPERTIES = "BizagiProperties";
    private static final String BIZAGI_EXTENSIONS = "BizagiExtensions";

    private static final Dimension MINIMIZED_NODE_SIZE = new Dimension(3 * GRID_SIZE, 3 * GRID_SIZE);

    private static Map<String, Element> planeMap = new HashMap<>();
    private static Map<String, GraphElement> geMap = new HashMap<>();
    private static Map<Rectangle, Swimlane> swimlaneBoundMap = new HashMap<>();
    private static Set<Subprocess> embeddedSubprocesses = new HashSet<>();
    private static Set<String> defaultIds = new HashSet<>();

    public static void go(IContainer dstFolder, String srcFileName, boolean showSwimlanes, boolean ignoreBendPoints)
            throws Exception {
        try (InputStream is = new FileInputStream(new File(srcFileName))) {
            Document bpmnDocument = XmlUtil.parseWithoutValidation(is);
            Element definitionsElement = bpmnDocument.getRootElement();
            scaleFactor = (BPMN_TARGET_NAMESPACE.equals(definitionsElement.attributeValue(TARGET_NAMESPACE)) ? 1 : SCALE_FACTOR);
            planeMap.clear();
            for (Element bpmnDiagram : (List<Element>) definitionsElement.elements(BPMN_DIAGRAM)) {
                Element bpmnPlane = bpmnDiagram.element(BPMN_PLANE);
                if (bpmnPlane != null) {
                    for (Element element : (List<Element>) bpmnPlane.elements()) {
                        planeMap.put(element.attributeValue(BPMN_ELEMENT), element);
                    }
                }
            }
            Element processElement = null;
            SwimlaneDisplayMode swimlaneDisplayMode = SwimlaneDisplayMode.none;
            Element collaborationElement = definitionsElement.element(COLLABORATION);
            String processName = collaborationElement.attributeValue(NAME);
            nextParticipant:
            for (Element participant : (List<Element>) collaborationElement.elements(PARTICIPANT)) {
                if (Strings.isNullOrEmpty(processName)) {
                    processName = participant.attributeValue(NAME);
                }
                Element extensionElements = participant.element(EXTENSION_ELEMENTS);
                if (extensionElements != null) {
                    Element bizagiExtensionElements = extensionElements.element(QName.get(BIZAGI_EXTENSIONS, BIZAGI_NAMESPACE));
                    if (bizagiExtensionElements != null) {
                        Element bizagiProperties = bizagiExtensionElements.element(QName.get(BIZAGI_PROPERTIES, BIZAGI_NAMESPACE));
                        if (bizagiProperties != null) {
                            List<Element> properties = bizagiProperties.elements(QName.get(BIZAGI_PROPERTY, BIZAGI_NAMESPACE));
                            for (Element property : properties) {
                                if (IS_MAIN_PARTICIPANT.equals(property.attributeValue(NAME))) {
                                    continue nextParticipant;
                                }
                            }
                        }
                    }
                }
                String processRef = participant.attributeValue(PROCESS_REF);
                for (Element process : (List<Element>) definitionsElement.elements(PROCESS)) {
                    if (processRef.equals(process.attributeValue(ID))) {
                        processElement = process;
                        Element participantShape = planeMap.get(participant.attributeValue(ID));
                        if (participantShape != null) {
                            swimlaneDisplayMode = showSwimlanes
                                    ? (TRUE.equals(participantShape.attributeValue(IS_HORIZONTAL)) ? SwimlaneDisplayMode.horizontal
                                            : SwimlaneDisplayMode.vertical)
                                    : SwimlaneDisplayMode.none;
                        }
                        break;
                    }
                }
                break;
            }
            IFolder folder = IOUtils.getProcessFolder(dstFolder, processName);
            folder.create(true, true, null);
            IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);
            Map<String, String> properties = Maps.newHashMap();
            properties.put(BpmnSerializer.SHOW_SWIMLANE, swimlaneDisplayMode.name());
            properties.put(ProcessSerializer.ACCESS_TYPE, ProcessDefinitionAccessType.Process.name());
            Document document = Language.BPMN.getSerializer().getInitialProcessDefinitionDocument(folder.getName(), properties);
            byte[] bytes = XmlUtil.writeXml(document);
            definitionFile.create(new ByteArrayInputStream(bytes), true, null);
            ProcessDefinition definition = ProcessCache.getProcessDefinition(definitionFile);
            definition.setName(processName);
            definition.setLanguage(Language.BPMN);
            swimlaneBoundMap.clear();
            Element laneSet = processElement.element(LANE_SET);
            if (laneSet != null) {
                for (Element lane : (List<Element>) laneSet.elements(LANE)) {
                    Swimlane swimlane = new Swimlane();
                    String laneName = lane.attributeValue(NAME);
                    swimlane.setName(Strings.isNullOrEmpty(laneName) ? EMPTY_ROLE : laneName);
                    Rectangle bounds = bounds(lane.attributeValue(ID));
                    if (showSwimlanes) {
                        swimlane.setConstraint(bounds);
                    }
                    definition.addChild(swimlane);
                    swimlaneBoundMap.put(bounds, swimlane);
                }
            }
            geMap.clear();
            embeddedSubprocesses.clear();
            defaultIds.clear();
            List<Element> sequenceFlows = new ArrayList<>();
            List<Element> elements = processElement.elements();
            int n= - 1; 
            
            for (Element element : elements) {
                try {
                    String elementName = element.getName();
                    String id = element.attributeValue(ID);
                    String name = (Strings.isNullOrEmpty(element.attributeValue(NAME))) ? "N" + n++ : element.attributeValue(NAME);
                    String documentation = element.elementText(DOCUMENTATION);
                    defaultIds.add(element.attributeValue(DEFAULT));
                    switch (elementName) {
                    case "startEvent": {
                        StartState start = new StartState();
                        start.setName(name);
                        start.setDescription(documentation);
                        setConstraint(start, id);
                        Swimlane swimlane = swimlane(start);
                        start.setSwimlane(swimlane);
                        if (showSwimlanes && swimlane != null) {
                            start.setParentContainer(swimlane);
                            start.getConstraint().translate(swimlane.getConstraint().getTopLeft().negate());
                        }
                        definition.addChild(start);
                        geMap.put(id, start);
                        break;
                    }
                    case "serviceTask":
                    case "task":
                    case "manualTask":
                    case "userTask": {
                        TaskState task = new TaskState();
                        task.setName(name);
                        task.setDescription(documentation);
                        task.setConstraint(bounds(id));
                        adjustMinimized(task);
                        Swimlane swimlane = swimlane(task);
                        task.setSwimlane(swimlane);
                        if (showSwimlanes && swimlane != null) {
                            task.setParentContainer(swimlane);
                            task.getConstraint().translate(swimlane.getConstraint().getTopLeft().negate());
                        }
                        definition.addChild(task);
                        geMap.put(id, task);
                        break;
                    }
                    case "eventBasedGateway":
                    case "inclusiveGateway":
                    case "exclusiveGateway": {
                        ExclusiveGateway eg = new ExclusiveGateway();
                        eg.setName(name);
                        eg.setDescription(documentation);
                        setConstraint(eg, id);
                        definition.addChild(eg);
                        geMap.put(id, eg);
                        break;
                    }
                    case "parallelGateway": {
                        ParallelGateway pg = new ParallelGateway();
                        pg.setName(name);
                        pg.setDescription(documentation);
                        setConstraint(pg, id);
                        definition.addChild(pg);
                        geMap.put(id, pg);
                        break;
                    }
                    case "subProcess": { // Embedded subprocess
                        Subprocess sp = null;
                        if (element.element(MULTI_INSTANCE_LOOP_CHARACTERISTICS) != null) {
                            sp = new MultiSubprocess();
                        } else {
                            sp = new Subprocess();
                        }
                        sp.setEmbedded(true);
                        sp.setName(name);
                        sp.setSubProcessName(name);
                        sp.setDescription(documentation);
                        sp.setConstraint(bounds(id));
                        adjustMinimized(sp);
                        definition.addChild(sp);
                        geMap.put(id, sp);
                        embeddedSubprocesses.add(sp);
                        break;
                    }
                    case "callActivity": { // External subprocess
                        Subprocess sp = new Subprocess();
                        sp.setEmbedded(false);
                        sp.setName(name);
                        sp.setSubProcessName(name);
                        sp.setDescription(documentation);
                        sp.setConstraint(bounds(id));
                        adjustMinimized(sp);
                        definition.addChild(sp);
                        geMap.put(id, sp);
                        break;
                    }
                    case "adHocSubProcess": {
                        Subprocess sp = new Subprocess();
                        sp.setEmbedded(false);
                        sp.setName(name);
                        sp.setSubProcessName(name);
                        sp.setDescription("?" + elementName + "?" + (Strings.isNullOrEmpty(documentation) ? "" : documentation));
                        sp.setConstraint(bounds(id));
                        adjustMinimized(sp);
                        definition.addChild(sp);
                        geMap.put(id, sp);
                        break;
                    }
                    case "endEvent": {
                        NamedGraphElement end = null;
                        if (element.element(TERMINATE_EVENT_DEFINITION) == null) {
                            end = new EndTokenState();
                        } else {
                            end = new EndState();
                        }
                        end.setName(name);
                        end.setDescription(documentation);
                        setConstraint(end, id);
                        definition.addChild(end);
                        geMap.put(id, end);
                        break;
                    }
                    case "intermediateCatchEvent": {
                        NamedGraphElement nge = null;
                        if (element.element(TIMER_EVENT_DEFINITION) != null) {
                            nge = new Timer();
                        } else {
                            nge = new CatchEventNode();
                        }
                        nge.setName(name);
                        nge.setDescription(documentation);
                        setConstraint(nge, id);
                        definition.addChild(nge);
                        geMap.put(id, nge);
                        break;
                    }
                    case "intermediateThrowEvent": {
                        ThrowEventNode event = new ThrowEventNode();
                        event.setName(name);
                        event.setDescription(documentation);
                        setConstraint(event, id);
                        definition.addChild(event);
                        geMap.put(id, event);
                        break;
                    }
                    case "boundaryEvent": {
                        CatchEventNode event = new CatchEventNode();
                        event.setName(name);
                        event.setDescription(documentation);
                        setConstraint(event, id);
                        String attachedToRefId = element.attributeValue(ATTACHED_TO_REF);
                        GraphElement parent = geMap.get(attachedToRefId);
                        parent.addChild(event);
                        event.setParent(parent);
                        event.setParentContainer(parent);
                        if (element.element(ERROR_EVENT_DEFINITION) != null) {
                            event.setEventNodeType(EventNodeType.error);
                        } else if (element.element(MESSAGE_EVENT_DEFINITION) != null) {
                            event.setEventNodeType(EventNodeType.message);
                        }
                        geMap.put(id, event);
                        break;
                    }
                    case "scriptTask": {
                        ScriptTask task = new ScriptTask();
                        task.setName(name);
                        task.setDescription(documentation);
                        task.setConstraint(bounds(id));
                        adjustMinimized(task);
                        definition.addChild(task);
                        geMap.put(id, task);
                        break;
                    }
                    case "textAnnotation": {
                        TextAnnotation annotation = new TextAnnotation();
                        annotation.setConstraint(bounds(id));
                        annotation.setDescription(element.elementText("text"));
                        definition.addChild(annotation);
                        geMap.put(id, annotation);
                        break;
                    }
                    case "sequenceFlow": {
                        sequenceFlows.add(element);
                        break;
                    }
                    case DOCUMENTATION:
                    case ASSOCIATION:
                    case EXTENSION_ELEMENTS:
                    case LANE_SET: {
                        // Do nothing
                        break;
                    }
                    // Undefined elements
                    default: {
                        TextAnnotation annotation = new TextAnnotation();
                        annotation.setConstraint(bounds(id));
                        annotation.setDescription("?" + elementName + "? - " + name);
                        definition.addChild(annotation);
                        geMap.put(id, annotation);
                    }
                    }
                } catch (Exception e) {
                    PluginLogger.logErrorWithoutDialog(e.getMessage(), e);
                }
            }
            for (Element element : sequenceFlows) {
                try {
                    String id = element.attributeValue(ID);
                    String name = element.attributeValue(NAME);
                    String sourceRef = element.attributeValue(SOURCE_REF);
                    String targetRef = element.attributeValue(TARGET_REF);
                    GraphElement target = geMap.get(targetRef);
                    if (target instanceof Node) {
                        NodeTypeDefinition transitionDefinition = NodeRegistry.getNodeTypeDefinition(Transition.class);
                        Node source = (Node) geMap.get(sourceRef);
                        Transition transition = transitionDefinition.createElement(source, false);
                        transition.setName(Strings.isNullOrEmpty(name) ? source.getNextTransitionName(transitionDefinition) : name);
                        transition.setTarget((Node) target);
                        if (defaultIds.contains(id)) {
                            transition.setDefaultFlow(true);
                            source.setDelegationConfiguration("return \"" + transition.getName() + "\";");
                        }
                        source.addLeavingTransition(transition);
                        if (!ignoreBendPoints) {
                            Rectangle sourceBounds = source.getConstraint().getCopy();
                            Rectangle targetBounds = target.getConstraint().getCopy();
                            if (showSwimlanes) {
                                if (source.getParentContainer() != null) {
                                    sourceBounds.translate(source.getParentContainer().getConstraint().getTopLeft());
                                }
                                if (target.getParentContainer() != null) {
                                    targetBounds.translate(target.getParentContainer().getConstraint().getTopLeft());
                                }
                            }
                            transition.setBendpoints(waypoints(id, sourceBounds, targetBounds));
                        }
                    }
                } catch (Exception e) {
                    PluginLogger.logErrorWithoutDialog(e.getMessage(), e);
                }
            }
            WorkspaceOperations.saveProcessDefinition(definition);
            ProcessCache.newProcessDefinitionWasCreated(definitionFile);
            WorkspaceOperations.openProcessDefinition(definitionFile);
            createEmbeddedSubprocesses();
        }
    }

    private static void createEmbeddedSubprocesses() {
        for (Subprocess sp : embeddedSubprocesses) {
            // TODO something like ru.runa.gpd.ui.wizard.NewProcessDefinitionWizard.CreateEmbeddedSubprocessOperation
        }
    }

    private static void setConstraint(GraphElement ge, String id) {
        NodeTypeDefinition typeDefinition = NodeRegistry.getNodeTypeDefinition(ge.getClass());
        GraphitiEntry entry = typeDefinition.getGraphitiEntry();
        Dimension defaultSize = entry.getDefaultSize();
        Rectangle bounds = bounds(id);
        if (bounds.width < defaultSize.width || entry.isFixedSize()) {
            bounds.x -= (defaultSize.width - bounds.width) / 2;
            bounds.width = defaultSize.width;
        }
        if (bounds.height < defaultSize.height || entry.isFixedSize()) {
            bounds.y -= (defaultSize.height - bounds.height) / 2;
            bounds.height = defaultSize.height;
        }
        ge.setConstraint(bounds);
    }

    private static double scaleFactor = 1;

    private static Rectangle bounds(String id) {
        Element shape = planeMap.get(id);
        if (shape != null) {
            Element bounds = shape.element(BOUNDS);
            if (bounds != null) {
                return new PrecisionRectangle(Double.parseDouble(bounds.attributeValue(X)), Double.parseDouble(bounds.attributeValue(Y)),
                        Double.parseDouble(bounds.attributeValue(WIDTH)), Double.parseDouble(bounds.attributeValue(HEIGHT))).scale(scaleFactor);
            }
        }
        throw new IllegalStateException("id: " + id + " does not exist");
    }

    private static void adjustMinimized(Node node) {
        if (node.getConstraint().getSize().equals(MINIMIZED_NODE_SIZE)) {
            node.setMinimizedView(true);
        }
    }

    private static List<Point> waypoints(String id, Rectangle sourceBounds, Rectangle targetBounds) {
        Element shape = planeMap.get(id);
        if (shape != null) {
            List<Point> bendPoints = Lists.newArrayList();
            List<Element> waypoints = shape.elements(WAYPOINT);
            if (waypoints != null) {
                for (Element waypoint : waypoints) {
                    Point point = new PrecisionPoint(Double.parseDouble(waypoint.attributeValue(X)), Double.parseDouble(waypoint.attributeValue(Y)))
                            .scale(scaleFactor);
                    if (!sourceBounds.contains(point.translate(-1, -1)) && !targetBounds.contains(point.translate(-1, -1))) {
                        bendPoints.add(point);
                    }
                }
            }
            return bendPoints;
        }
        throw new IllegalStateException("id: " + id + " does not exist");
    }

    private static Swimlane swimlane(SwimlanedNode node) {
        for (Map.Entry<Rectangle, Swimlane> entry : swimlaneBoundMap.entrySet()) {
            if (entry.getKey().contains(node.getConstraint().getTopLeft())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private BizagiBpmnImporter() {
        // All-static class
    }

}
