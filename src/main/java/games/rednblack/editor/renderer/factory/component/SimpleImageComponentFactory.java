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
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.data.SimpleImageVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ABAtlasRegion;

/**
 * Created by azakhary on 5/22/2015.
 */
public class SimpleImageComponentFactory extends ComponentFactory {

    protected ComponentMapper<TextureRegionComponent> textureRegionCM;
    protected ComponentMapper<NormalMapRendering> normalMapRenderingCM;

    private final EntityTransmuter transmuter;

    public SimpleImageComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(ParentNodeComponent.class)
                .add(TextureRegionComponent.class)
                .add(NormalMapRendering.class)
                .build();
    }

    public int createSpecialisedEntity(int root, MainItemVO vo) {
        int entity = createGeneralEntity(vo, EntityFactory.IMAGE_TYPE);
        transmuter.transmute(entity);

        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        initializeTextureRegionComponent(entity, textureRegionComponent, (SimpleImageVO) vo);

        adjustNodeHierarchy(root, entity);
        updatePolygons(entity);

        return entity;
    }

    private void updatePolygons(int entity) {
        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
        PolygonComponent polygonComponent = polygonCM.get(entity);

        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        if (textureRegionComponent.isPolygon && polygonComponent != null && polygonComponent.vertices != null) {
            textureRegionComponent.setPolygonSprite(polygonComponent);
            dimensionsComponent.setPolygon(polygonComponent);
        }
    }

    @Override
    protected void initializeDimensionsComponent(int entity, DimensionsComponent component, MainItemVO vo) {
        SimpleImageVO sVo = (SimpleImageVO) vo;
        TextureRegion region = rm.getTextureRegion(sVo.imageName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.width = (float) region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        component.height = (float) region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;
    }

    protected void initializeTextureRegionComponent(int entity, TextureRegionComponent component, SimpleImageVO vo) {
        engine.inject(component);
        component.regionName = vo.imageName;
        if (rm.hasTextureRegion(vo.imageName + ".normal")) {
            TextureAtlas.AtlasRegion regionDiffuse = (TextureAtlas.AtlasRegion) rm.getTextureRegion(vo.imageName);
            TextureAtlas.AtlasRegion normalRegion = (TextureAtlas.AtlasRegion) rm.getTextureRegion(vo.imageName + ".normal");
            component.region = new ABAtlasRegion(regionDiffuse, normalRegion, normalMapRenderingCM.get(entity));
        } else {
            normalMapRenderingCM.remove(entity);
            component.region = rm.getTextureRegion(vo.imageName);
        }
        component.isRepeat = vo.isRepeat;
        component.isPolygon = vo.isPolygon;
    }

}
