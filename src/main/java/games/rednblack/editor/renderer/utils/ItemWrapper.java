/*
 * ******************************************************************************
 *  * Copyright 2015 See AUTHORS file.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package games.rednblack.editor.renderer.utils;

import com.artemis.BaseComponentMapper;
import com.artemis.Component;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.ActionData;

import java.util.HashMap;

/**
 * Created by azakhary on 7/8/2015.
 */
public class ItemWrapper {

    private int entity = -1;

    private com.artemis.World engine;

    private NodeComponent nodeComponent;
    private final ObjectMap<String, Integer> childrenMap = new ObjectMap<>();
    private final ObjectMap<String, IntSet> childrenTagsMap = new ObjectMap<>();

    public ItemWrapper() {
        // empty wrapper is better then null pointer
    }

    public ItemWrapper(int entity, com.artemis.World engine) {
        this.entity = entity;
        this.engine = engine;
        nodeComponent = ComponentRetriever.get(entity, NodeComponent.class, engine);
        if (nodeComponent != null) {
            for (int child : nodeComponent.children) {
                mapEntity(child);
            }
        }
    }

    private void mapEntity(int entity) {
        MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class, engine);
        childrenMap.put(mainItemComponent.itemIdentifier, entity);

        for (String tag : mainItemComponent.tags) {
            mapTagEntity(tag, entity);
        }
    }

    private void mapTagEntity(String tag, int entity) {
        if (childrenTagsMap.get(tag) == null)
            childrenTagsMap.put(tag, new IntSet());

        childrenTagsMap.get(tag).add(entity);
    }

    public ItemWrapper getChild(String id) {
        Integer entity = childrenMap.get(id);
        if (entity == null) return new ItemWrapper();

        return new ItemWrapper(entity, engine);
    }

    /**
     * Get a child set from this composite using which contains a tag
     * @param tagName tag to find
     */
    public IntSet getChildrenByTag(String tagName) {
        if (childrenTagsMap.get(tagName) == null)
            childrenTagsMap.put(tagName, new IntSet());

        return childrenTagsMap.get(tagName);
    }

    /**
     * Return a component from the current entity using mappers. See {@link ComponentRetriever}
     * @param clazz component class
     */
    public <T extends Component> T getComponent(Class<T> clazz) {
        return ComponentRetriever.get(entity, clazz, engine);
    }

    /**
     * Add new child entity to the current Composite (must have the {@link NodeComponent}).
     * If child already has a parent it will be removed from its node list
     *
     * @param child Entity child
     * @return new {@link ItemWrapper} instance for the child param
     */
    public ItemWrapper addChild(int child) {
        if (nodeComponent != null) {
            ParentNodeComponent parentNodeComponent = ComponentRetriever.get(child, ParentNodeComponent.class, engine);
            if (parentNodeComponent.parentEntity != -1) {
                //Remove child from its parent
                NodeComponent parentNode = ComponentRetriever.get(parentNodeComponent.parentEntity, NodeComponent.class, engine);
                if (parentNode != null)
                    parentNode.removeChild(child);
            }
            parentNodeComponent.parentEntity = entity;
            nodeComponent.children.add(child);

            mapEntity(child);

            return new ItemWrapper(child, engine);
        }

        return new ItemWrapper();
    }

    /**
     * Get the type of the current entity from {@link MainItemComponent},
     * see {@link games.rednblack.editor.renderer.factory.EntityFactory} for possible values
     */
    public int getType() {
        MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class, engine);
        return mainItemComponent.entityType;
    }

    public int getEntity() {
        return entity;
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent}
     * @param script script instance
     * @return same input script instance
     */
    public IScript addScript(IScript script) {
        ScriptComponent component = ComponentRetriever.get(entity, ScriptComponent.class, engine);
        if (component == null) {
            component = engine.edit(entity).create(ScriptComponent.class);
            component.engine = engine;
        }
        component.addScript(script);

        return script;
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent},
     * Scripts will be automatically pooled and must extends {@link BasicScript}
     *
     * @param scriptClazz script class definition
     * @return instance of the script obtained
     */
    public <T extends BasicScript> T addScript(Class<T> scriptClazz) {
        BaseComponentMapper<ScriptComponent> mapper = ComponentRetriever.getMapper(ScriptComponent.class, engine);
        ScriptComponent component = mapper.get(entity);
        if(component == null) {
            component = mapper.create(entity);
            component.engine = engine;
        }
        T script = component.addScript(scriptClazz);

        return script;
    }

    public void addAction(ActionData actionData) {
        Actions.addAction(entity, actionData, engine);
    }
}
