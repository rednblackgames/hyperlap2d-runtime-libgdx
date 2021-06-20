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

import com.artemis.Component;
import com.artemis.World;
import com.badlogic.gdx.utils.IntSet;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.scripts.IScript;

import java.util.HashMap;

/**
 * Created by azakhary on 7/8/2015.
 */
public class ItemWrapper {

    private int entity;

    private NodeComponent nodeComponent;
    private final HashMap<String, Integer> childrenMap = new HashMap<>();
    private final HashMap<String, IntSet> childrenTagsMap = new HashMap<>();

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

    public IntSet getChildrenByTag(String tagName) {
        if (childrenTagsMap.get(tagName) == null)
            childrenTagsMap.put(tagName, new IntSet());

        return childrenTagsMap.get(tagName);
    }

    public <T extends Component> T getComponent(Class<T> clazz) {
        return ComponentRetriever.get(entity, clazz);
    }

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

    public int getType() {
        MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
        return mainItemComponent.entityType;
    }

    public int getEntity() {
        return entity;
    }

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
}
