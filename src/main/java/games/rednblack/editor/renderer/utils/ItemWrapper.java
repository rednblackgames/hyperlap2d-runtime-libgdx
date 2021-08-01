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
import com.artemis.World;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;

import java.util.HashMap;

/**
 * Created by azakhary on 7/8/2015.
 */
public class ItemWrapper {

    private int entity;

    private NodeComponent nodeComponent;
    private final ObjectMap<String, Integer> childrenMap = new ObjectMap<>();
    private final ObjectMap<String, IntSet> childrenTagsMap = new ObjectMap<>();

    public ItemWrapper() {
        // empty wrapper is better then null pointer
    }

    public ItemWrapper(int entity) {
        this.entity = entity;
        nodeComponent = ComponentRetriever.get(entity, NodeComponent.class);
        if (nodeComponent != null) {
            for (int child : nodeComponent.children) {
                mapEntity(child);
            }
        }
    }

    private void mapEntity(int entity) {
        MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
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
        int entity = childrenMap.get(id);
        if (entity == -1) return new ItemWrapper();

        return new ItemWrapper(entity);
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
        return ComponentRetriever.get(entity, clazz);
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
            ParentNodeComponent parentNodeComponent = ComponentRetriever.get(child, ParentNodeComponent.class);
            if (parentNodeComponent.parentEntity != -1) {
                //Remove child from its parent
                NodeComponent parentNode = ComponentRetriever.get(parentNodeComponent.parentEntity, NodeComponent.class);
                if (parentNode != null)
                    parentNode.removeChild(child);
            }
            parentNodeComponent.parentEntity = entity;
            nodeComponent.children.add(child);

            mapEntity(child);

            return new ItemWrapper(child);
        }

        return new ItemWrapper();
    }

    /**
     * Get the type of the current entity from {@link MainItemComponent},
     * see {@link games.rednblack.editor.renderer.factory.EntityFactory} for possible values
     */
    public int getType() {
        MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
        return mainItemComponent.entityType;
    }

    public int getEntity() {
        return entity;
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent}
     * @param script script instance
     * @param engine PooledEngine instance
     * @return same input script instance
     */
    public IScript addScript(IScript script, World engine) {
        ScriptComponent component = ComponentRetriever.get(entity, ScriptComponent.class);
        if (component == null) {
            component = engine.edit(entity).create(ScriptComponent.class);
            component.engine = engine;
        }
        component.addScript(script);
        script.init(entity);

        return script;
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent},
     * Scripts will be automatically pooled and must extends {@link BasicScript}
     *
     * @param scriptClazz script class definition
     * @param engine PooledEngine instance
     * @return instance of the script obtained
     */
    public <T extends BasicScript> T addScript(Class<T> scriptClazz, World engine) {
        BaseComponentMapper<ScriptComponent> mapper = ComponentRetriever.getMapper(ScriptComponent.class);
        ScriptComponent component = mapper.get(entity);
        if(component == null) {
            component = mapper.create(entity);
        }
        T script = component.addScript(scriptClazz);
        script.init(entity);

        return script;
    }
}
