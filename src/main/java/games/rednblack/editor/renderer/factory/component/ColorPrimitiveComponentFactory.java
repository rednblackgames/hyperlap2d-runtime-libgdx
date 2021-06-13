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

import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

/**
 * Created by azakhary on 10/21/2015.
 */
public class ColorPrimitiveComponentFactory extends ComponentFactory {

    public ColorPrimitiveComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    @Override
    public void createComponents(int root, int entity, MainItemVO vo) {
        createCommonComponents(entity, vo, EntityFactory.COLOR_PRIMITIVE);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);

        createTextureRegionComponent(entity, vo);

        TextureRegionComponent textureRegionComponent = ComponentRetriever.get(entity, TextureRegionComponent.class);
        DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class);
        PolygonComponent polygonComponent = ComponentRetriever.get(entity, PolygonComponent.class);
        dimensionsComponent.setPolygon(polygonComponent);
        textureRegionComponent.setPolygonSprite(polygonComponent);
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo) {
        DimensionsComponent component = engine.edit(entity).create(DimensionsComponent.class);
        component.setFromShape(vo.shape);

        return component;
    }

    protected TextureRegionComponent createTextureRegionComponent(int entity, MainItemVO vo) {
        TextureRegionComponent component = engine.edit(entity).create(TextureRegionComponent.class);

        component.region = rm.getTextureRegion("white-pixel");
        component.isRepeat = false;
        component.isPolygon = true;

        return component;
    }
}
