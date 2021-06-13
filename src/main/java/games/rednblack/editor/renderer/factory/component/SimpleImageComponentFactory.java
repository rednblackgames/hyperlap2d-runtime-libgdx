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
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.components.normal.NormalTextureRegionComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.data.SimpleImageVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

/**
 * Created by azakhary on 5/22/2015.
 */
public class SimpleImageComponentFactory extends ComponentFactory {

    public SimpleImageComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    public void createComponents(int root, int entity, MainItemVO vo) {
        createTextureRegionComponent(entity, (SimpleImageVO) vo);
        createCommonComponents(entity, vo, EntityFactory.IMAGE_TYPE);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);
        updatePolygons(entity);
    }

    private void updatePolygons(int entity) {
        TextureRegionComponent textureRegionComponent = ComponentRetriever.get(entity, TextureRegionComponent.class);
        DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class);

        PolygonComponent polygonComponent = ComponentRetriever.get(entity, PolygonComponent.class);
        if (textureRegionComponent.isPolygon && polygonComponent != null && polygonComponent.vertices != null) {
            textureRegionComponent.setPolygonSprite(polygonComponent);
            dimensionsComponent.setPolygon(polygonComponent);
        }
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo) {
        DimensionsComponent component = engine.edit(entity).create(DimensionsComponent.class);

        TextureRegionComponent textureRegionComponent = ComponentRetriever.get(entity, TextureRegionComponent.class);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.width = (float) textureRegionComponent.region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        component.height = (float) textureRegionComponent.region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;

        return component;
    }

    protected TextureRegionComponent createTextureRegionComponent(int entity, SimpleImageVO vo) {
        TextureRegionComponent component = engine.edit(entity).create(TextureRegionComponent.class);
        component.regionName = vo.imageName;
        component.region = rm.getTextureRegion(vo.imageName);
        component.isRepeat = vo.isRepeat;
        component.isPolygon = vo.isPolygon;

        if (rm.hasTextureRegion(vo.imageName + ".normal")) {
            NormalTextureRegionComponent normalComponent = engine.edit(entity).create(NormalTextureRegionComponent.class);
            normalComponent.textureRegion = rm.getTextureRegion(vo.imageName + ".normal");
            engine.edit(entity).create(NormalMapRendering.class);
        }

        return component;
    }
}
