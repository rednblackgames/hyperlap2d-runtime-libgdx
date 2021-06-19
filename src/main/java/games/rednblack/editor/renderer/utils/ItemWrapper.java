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

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;

/**
 * Created by azakhary on 7/8/2015.
 */
public class ItemWrapper {

    private Entity entity;

    private NodeComponent nodeComponent;
    private final ObjectMap<String, Entity> childrenMap = new ObjectMap<>();
    private final ObjectMap<String, ObjectSet<Entity>> childrenTagsMap = new ObjectMap<>();

    public ItemWrapper() {
        // empty wrapper is better then null pointer
    }

    public ItemWrapper(Entity entity) {
        this.entity = entity;
        nodeComponent = ComponentRetriever.get(entity, NodeComponent.class);
        if(nodeComponent != null) {
            for (Entity child : nodeComponent.children) {
                mapEntity(child);
            }
        }
    }

    private void mapEntity(Entity entity) {
        MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
        childrenMap.put(mainItemComponent.itemIdentifier, entity);

        for (String tag : mainItemComponent.tags) {
            mapTagEntity(tag, entity);
        }
    }

    private void mapTagEntity(String tag, Entity entity) {
        if (childrenTagsMap.get(tag) == null)
            childrenTagsMap.put(tag, new ObjectSet<Entity>());

        childrenTagsMap.get(tag).add(entity);
    }

    public ItemWrapper getChild(String id) {
        Entity entity = childrenMap.get(id);
        if(entity == null) return new ItemWrapper();

        return new ItemWrapper(entity);
    }

    /**
     * Get a child set from this composite using which contains a tag
     * @param tagName tag to find
     */
    public ObjectSet<Entity> getChildrenByTag(String tagName) {
        if (childrenTagsMap.get(tagName) == null)
            childrenTagsMap.put(tagName, new ObjectSet<Entity>());

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
    public ItemWrapper addChild(Entity child) {
        if(nodeComponent != null) {
            ParentNodeComponent parentNodeComponent = ComponentRetriever.get(child, ParentNodeComponent.class);
            if (parentNodeComponent.parentEntity != null) {
                //Remove child from its parent
                NodeComponent parentNode = ComponentRetriever.get(parentNodeComponent.parentEntity, NodeComponent.class);
                if (parentNode != null)
                    parentNode.removeChild(child);
            }
            parentNodeComponent.parentEntity = entity;
            nodeComponent.children.add(child);

            mapEntity(child);

            return  new ItemWrapper(child);
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

    public Entity getEntity() {
        return entity;
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent}
     * @param script script instance
     * @param engine PooledEngine instance
     * @return same input script instance
     */
    public IScript addScript(IScript script, PooledEngine engine) {
        ScriptComponent component = ComponentRetriever.get(entity, ScriptComponent.class);
        if(component == null) {
            component = engine.createComponent(ScriptComponent.class);
            entity.add(component);
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
    public <T extends BasicScript> T addScript(Class<T> scriptClazz, PooledEngine engine) {
        ScriptComponent component = ComponentRetriever.get(entity, ScriptComponent.class);
        if(component == null) {
            component = engine.createComponent(ScriptComponent.class);
            entity.add(component);
        }
        T script = component.addScript(scriptClazz);
        script.init(entity);

        return script;
    }
}
