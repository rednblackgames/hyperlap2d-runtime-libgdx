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

package games.rednblack.editor.renderer.factory.component;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.data.CompositeItemVO;
import games.rednblack.editor.renderer.data.LayerItemVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

/**
 * Created by azakhary on 5/22/2015.
 */
public class CompositeComponentFactory extends ComponentFactory {

    protected ComponentMapper<CompositeTransformComponent> compositeTransformCM;
    protected ComponentMapper<LayerMapComponent> layerMapCM;

    private final EntityTransmuter transmuter;

    public CompositeComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(ParentNodeComponent.class)
                .add(NodeComponent.class)
                .add(CompositeTransformComponent.class)
                .add(LayerMapComponent.class)
                .build();
    }

    @Override
    public int createSpecialisedEntity(int root, MainItemVO vo) {
        int entity = createGeneralEntity(vo, EntityFactory.COMPOSITE_TYPE);
        transmuter.transmute(entity);

        adjustNodeHierarchy(root,entity);

        initializeCompositeComponents(compositeTransformCM.get(entity), (CompositeItemVO) vo);
        initializeLayerMapComponents(layerMapCM.get(entity), (CompositeItemVO) vo);

        return entity;
    }

    protected void initializeDimensionsComponent(int entity, DimensionsComponent component, MainItemVO vo) {
        component.width = ((CompositeItemVO) vo).width;
        component.height = ((CompositeItemVO) vo).height;
        component.boundBox = new Rectangle(0, 0, component.width, component.height);
    }

    @Override
    protected void adjustNodeHierarchy(int root, int entity) {
        // It it self is the rootEntity, we don't require a ParentNodeComponent in here and we only need a NodeComponent, no other initialization
        if (root == -1) {
            parentNodeCM.remove(entity);
            return;
        }

        // Else, if it has a parent (root) we need to add it to the entity's parent reference and we need to add it to it's parent reference
        NodeComponent rootComponent = nodeCM.get(root);
        rootComponent.addChild(entity);

        ParentNodeComponent entityComponent = parentNodeCM.get(entity);
        entityComponent.parentEntity = root;
    }

    protected void initializeCompositeComponents(CompositeTransformComponent component, CompositeItemVO vo) {
        component.automaticResize = vo.automaticResize;
        component.scissorsEnabled = vo.scissorsEnabled;
        component.renderToFBO = vo.renderToFBO;
    }

    protected void initializeLayerMapComponents(LayerMapComponent component, CompositeItemVO vo) {
        if (vo.composite.layers.size() == 0) {
            vo.composite.layers.add(LayerItemVO.createDefault());
        }
        component.setLayers(vo.composite.layers);
    }
}
