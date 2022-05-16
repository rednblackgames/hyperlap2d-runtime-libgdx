package games.rednblack.editor.renderer.factory;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.data.GraphConnectionVO;
import games.rednblack.editor.renderer.data.GraphNodeVO;
import games.rednblack.editor.renderer.data.GraphVO;
import games.rednblack.editor.renderer.systems.action.ActionEventListener;
import games.rednblack.editor.renderer.systems.action.ActionRunnable;
import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.*;
import games.rednblack.editor.renderer.utils.InterpolationMap;

import java.util.*;

public class ActionFactory {
    private final Map<String, GraphVO> actionsLibrary;
    private final Map<String, GraphCache> actionsCache = new HashMap<String, GraphCache>();

    public ActionFactory(Map<String, GraphVO> actions) {
        actionsLibrary = actions;
    }

    public ActionData loadFromLibrary(String actionName) {
        return loadFromLibrary(actionName, true, null);
    }

    public ActionData loadFromLibrary(String actionName, ActionEventListener listener) {
        return loadFromLibrary(actionName, true, null, listener);
    }

    public ActionData loadFromLibrary(String actionName, ObjectMap<String, Object> params) {
        return loadFromLibrary(actionName, true, params);
    }

    public ActionData loadFromLibrary(String actionName, ObjectMap<String, Object> params, ActionEventListener listener) {
        return loadFromLibrary(actionName, true, params, listener);
    }

    public ActionData loadFromLibrary(String actionName, boolean autoPoolable, ObjectMap<String, Object> params) {
        return loadFromLibrary(actionName, autoPoolable, params, null);
    }

    public ActionData loadFromLibrary(String actionName, boolean autoPoolable, ObjectMap<String, Object> params, ActionEventListener listener) {
        if (actionsLibrary.get(actionName) == null)
            throw new IllegalArgumentException("The action '" + actionName + "' does not exists.");

        GraphCache graphCache = getOrCreateGraphData(actionName);

        ActionData data;
        try {
            data = getActionData(graphCache.rootNode, graphCache.toNodeConnections, graphCache.nodes, autoPoolable, params, listener);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("The action '" + actionName + "' has not a valid format.");
        }
        return data;
    }

    private GraphCache getOrCreateGraphData(String actionName) {
        if (actionsCache.get(actionName) != null)
            return actionsCache.get(actionName);

        GraphVO actionGraph = actionsLibrary.get(actionName);

        Map<String, List<GraphConnectionVO>> toNodeConnections = new HashMap<>();

        Map<String, GraphNodeVO> nodes = new HashMap<>();
        for (GraphNodeVO node : actionGraph.nodes) {
            toNodeConnections.put(node.id, new ArrayList<GraphConnectionVO>());
            nodes.put(node.id, node);
        }

        String actionNode = "";
        for (GraphConnectionVO connection : actionGraph.connections) {
            String fromNode = connection.fromNode;
            String toNode = connection.toNode;
            String toField = connection.toField;

            toNodeConnections.get(toNode).add(connection);
            Collections.sort(toNodeConnections.get(toNode));

            if (toNode.equals("end") && toField.equals("action")) {
                actionNode = fromNode;
            }
        }

        GraphCache graphCache = new GraphCache();
        graphCache.rootNode = nodes.get(actionNode);
        graphCache.nodes = nodes;
        graphCache.toNodeConnections = toNodeConnections;

        actionsCache.put(actionName, graphCache);

        return graphCache;
    }

    private ActionData getActionData(GraphNodeVO node, Map<String, List<GraphConnectionVO>> toNodeConnections,
                                     Map<String, GraphNodeVO> nodes, boolean autoPoolable, ObjectMap<String, Object> params,
                                     ActionEventListener listener) {
        ActionData actionData = mapTypeToActionData(node.type, autoPoolable);

        for (GraphConnectionVO inConnection : toNodeConnections.get(node.id)) {
            if (inConnection.toField.contains("action")) {
                ActionData subAction = getActionData(nodes.get(inConnection.fromNode), toNodeConnections,
                        nodes, autoPoolable, params, listener);
                addSubAction(actionData, subAction);
            }

            addActionDataParameter(actionData, nodes, toNodeConnections.get(node.id), params);
        }

        addActionDataValueParameter(node, actionData, listener);
        return actionData;
    }

    private void addSubAction(ActionData actionData, ActionData subAction) {
        if (actionData instanceof DelegateData) {
            ((DelegateData) actionData).setDelegatedAction(subAction);
        }

        if (actionData instanceof ParallelData) {
            ((ParallelData) actionData).actionsData.add(subAction);
        }
    }

    private void addActionDataParameter(ActionData actionData, Map<String, GraphNodeVO> nodes,
                                        List<GraphConnectionVO> connections, ObjectMap<String, Object> params) {
        if (actionData instanceof TemporalData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "duration":
                        ((TemporalData) actionData).setDuration((Float) getValue(nodes.get(connection.fromNode), params));
                        break;
                    case "interpolation":
                        ((TemporalData) actionData).setInterpolation((Interpolation) getValue(nodes.get(connection.fromNode), params));
                        break;
                }
            }
        }

        if (actionData instanceof MoveToData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "position":
                        Vector2 pos = (Vector2) getValue(nodes.get(connection.fromNode), params);
                        ((MoveToData) actionData).setEndX(pos.x);
                        ((MoveToData) actionData).setEndY(pos.y);
                        break;
                }
            }
        }

        if (actionData instanceof MoveByData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "position":
                        Vector2 pos = (Vector2) getValue(nodes.get(connection.fromNode), params);
                        ((MoveByData) actionData).setAmountX(pos.x);
                        ((MoveByData) actionData).setAmountY(pos.y);
                        break;
                }
            }
        }

        if (actionData instanceof RotateToData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "degree":
                        ((RotateToData) actionData).setEnd((Float) getValue(nodes.get(connection.fromNode), params));
                        break;
                }
            }
        }

        if (actionData instanceof RotateByData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "degree":
                        ((RotateByData) actionData).setAmount((Float) getValue(nodes.get(connection.fromNode), params));
                        break;
                }
            }
        }

        if (actionData instanceof SizeToData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "size":
                        Vector2 pos = (Vector2) getValue(nodes.get(connection.fromNode), params);
                        ((SizeToData) actionData).setEndWidth(pos.x);
                        ((SizeToData) actionData).setEndHeight(pos.y);
                        break;
                }
            }
        }

        if (actionData instanceof SizeByData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "size":
                        Vector2 pos = (Vector2) getValue(nodes.get(connection.fromNode), params);
                        ((SizeByData) actionData).setAmountWidth(pos.x);
                        ((SizeByData) actionData).setAmountHeight(pos.y);
                        break;
                }
            }
        }

        if (actionData instanceof ScaleToData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "scale":
                        Vector2 pos = (Vector2) getValue(nodes.get(connection.fromNode), params);
                        ((ScaleToData) actionData).setEndX(pos.x);
                        ((ScaleToData) actionData).setEndY(pos.y);
                        break;
                }
            }
        }

        if (actionData instanceof ScaleByData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "scale":
                        Vector2 pos = (Vector2) getValue(nodes.get(connection.fromNode), params);
                        ((ScaleByData) actionData).setAmountX(pos.x);
                        ((ScaleByData) actionData).setAmountY(pos.y);
                        break;
                }
            }
        }

        if (actionData instanceof ColorData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "color":
                        ((ColorData) actionData).setEndColor((Color) getValue(nodes.get(connection.fromNode), params));
                        break;
                }
            }
        }

        if (actionData instanceof AlphaData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "alpha":
                        ((AlphaData) actionData).setEnd((Float) getValue(nodes.get(connection.fromNode), params));
                        break;
                }
            }
        }

        if (actionData instanceof DelayData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "delay":
                        ((DelayData) actionData).setDuration((Float) getValue(nodes.get(connection.fromNode), params));
                        break;
                }
            }
        }

        if (actionData instanceof RepeatData) {
            for (GraphConnectionVO connection : connections) {
                switch (connection.toField) {
                    case "count":
                        float count = (Float) getValue(nodes.get(connection.fromNode), params);
                        ((RepeatData) actionData).setRepeatCount((int)count);
                        break;
                }
            }
        }
    }

    private void addActionDataValueParameter(GraphNodeVO node, ActionData actionData, final ActionEventListener listener) {
        if (actionData instanceof RunnableData) {
            final String eventName = (String) node.data.get("v");
            if (listener != null) {
                ((RunnableData) actionData).setRunnable(new ActionRunnable() {
                    @Override
                    public void run(int entity) {
                        listener.onActionEvent(entity, eventName);
                    }
                });
            }
        }
    }

    private Object getValue(GraphNodeVO node, ObjectMap<String, Object> params) {
        switch (node.type) {
            case "ValueBoolean":
                return node.data.get("v") != null;
            case "ValueColor":
                return Color.valueOf(node.data.get("color"));
            case "ValueFloat":
                return Float.parseFloat(node.data.get("v1"));
            case "ValueVector2":
                return new Vector2(Float.parseFloat(node.data.get("v1")), Float.parseFloat(node.data.get("v2")));
            case "ValueInterpolation":
                return InterpolationMap.map.get(node.data.get("interpolation"));
            case "ValueParam":
                if (params == null || params.get(node.data.get("v")) == null)
                    throw new IllegalArgumentException("Custom parameter '" + node.data.get("v") + "' not found.");
                return params.get(node.data.get("v"));
            default:
                return null;
        }
    }

    private ActionData mapTypeToActionData(String nodeType, boolean autoPoolable) {
        switch (nodeType) {
            case "AlphaAction":
                return Actions.actionData(AlphaData.class, autoPoolable);
            case "ColorAction":
                return Actions.actionData(ColorData.class, autoPoolable);
            case "DelayAction":
                return Actions.actionData(DelayData.class, autoPoolable);
            case "FadeInAction":
                AlphaData fadeIn = Actions.actionData(AlphaData.class, autoPoolable);
                fadeIn.setEnd(1);
                return fadeIn;
            case "FadeOutAction":
                AlphaData fadeOut = Actions.actionData(AlphaData.class, autoPoolable);
                fadeOut.setEnd(0);
                return fadeOut;
            case "ForeverAction":
                RepeatData actionData = Actions.actionData(RepeatData.class, autoPoolable);
                actionData.setRepeatCount(RepeatData.FOREVER);
                return actionData;
            case "MoveByAction":
                return Actions.actionData(MoveByData.class, autoPoolable);
            case "MoveToAction":
                return Actions.actionData(MoveToData.class, autoPoolable);
            case "ParallelAction":
                return Actions.actionData(ParallelData.class, autoPoolable);
            case "RepeatAction":
                return Actions.actionData(RepeatData.class, autoPoolable);
            case "RotateByAction":
                return Actions.actionData(RotateByData.class, autoPoolable);
            case "RotateToAction":
                return Actions.actionData(RotateToData.class, autoPoolable);
            case "ScaleByAction":
                return Actions.actionData(ScaleByData.class, autoPoolable);
            case "SequenceAction":
                return Actions.actionData(SequenceData.class, autoPoolable);
            case "SizeByAction":
                return Actions.actionData(SizeByData.class, autoPoolable);
            case "SizeToAction":
                return Actions.actionData(SizeToData.class, autoPoolable);
            case "ScaleToAction":
                return Actions.actionData(ScaleToData.class, autoPoolable);
            case "EventAction":
                return Actions.actionData(RunnableData.class, autoPoolable);
            default:
                return null;
        }
    }

    public void invalidateCache() {
        actionsCache.clear();
    }

    private static class GraphCache {
        GraphNodeVO rootNode;
        Map<String, List<GraphConnectionVO>> toNodeConnections;
        Map<String, GraphNodeVO> nodes;
    }
}
