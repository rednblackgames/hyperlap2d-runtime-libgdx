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
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

/**
 * Created by azakhary on 10/21/2015.
 */
public class ColorPrimitiveComponentFactory extends ComponentFactory {

    protected static ComponentMapper<TextureRegionComponent> textureRegionCM;

    public ColorPrimitiveComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    @Override
    public void createComponents(int root, int entity, MainItemVO vo) {
        createCommonComponents(entity, vo, EntityFactory.COLOR_PRIMITIVE);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);

        createTextureRegionComponent(entity, vo);

        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
        PolygonComponent polygonComponent = polygonCM.get(entity);
        dimensionsComponent.setPolygon(polygonComponent);
        textureRegionComponent.setPolygonSprite(polygonComponent);
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo) {
        DimensionsComponent component = dimensionsCM.create(entity);
        component.setFromShape(vo.shape);

        return component;
    }

    protected TextureRegionComponent createTextureRegionComponent(int entity, MainItemVO vo) {
        TextureRegionComponent component = textureRegionCM.create(entity);

        component.region = rm.getTextureRegion("white-pixel");
        component.isRepeat = false;
        component.isPolygon = true;

        return component;
    }
}
