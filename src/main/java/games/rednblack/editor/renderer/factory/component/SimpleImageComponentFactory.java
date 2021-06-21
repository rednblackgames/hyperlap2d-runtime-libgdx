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
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
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

/**
 * Created by azakhary on 5/22/2015.
 */
public class SimpleImageComponentFactory extends ComponentFactory {

    protected static ComponentMapper<TextureRegionComponent> textureRegionCM;
    protected static ComponentMapper<NormalTextureRegionComponent> normalTextureRegionCM;

    TextureRegionComponent textureRegionComponent;

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

        textureRegionComponent = textureRegionCM.get(entity);
        initializeTextureRegionComponent(textureRegionComponent, (SimpleImageVO) vo);
        checkNormalTextureRegionComponent(entity, (SimpleImageVO) vo);

        // We need the dimension component created on basis of texture region component.
        // That's why we call it again, after creating a texture region component.
        initializeDimensionsComponent(dimensionsCM.get(entity), vo);

        adjustNodeHierarchy(root, entity);
        updatePolygons(entity);

        return entity;
    }

    private void updatePolygons(int entity) {
        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
        PolygonComponent polygonComponent = polygonCM.get(entity);

        if (textureRegionComponent.isPolygon && polygonComponent != null && polygonComponent.vertices != null) {
            textureRegionComponent.setPolygonSprite(polygonComponent);
            dimensionsComponent.setPolygon(polygonComponent);
        }
    }

    @Override
    protected void initializeDimensionsComponent(DimensionsComponent component, MainItemVO vo) {
        if (textureRegionComponent == null) return;

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.width = (float) textureRegionComponent.region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        component.height = (float) textureRegionComponent.region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;
    }

    protected void initializeTextureRegionComponent(TextureRegionComponent component, SimpleImageVO vo) {
        engine.inject(component);
        component.regionName = vo.imageName;
        component.region = rm.getTextureRegion(vo.imageName);
        component.isRepeat = vo.isRepeat;
        component.isPolygon = vo.isPolygon;
    }

    protected void checkNormalTextureRegionComponent(int entity, SimpleImageVO vo) {
        if (rm.hasTextureRegion(vo.imageName + ".normal")) {
            NormalTextureRegionComponent normalComponent = normalTextureRegionCM.get(entity);
            normalComponent.textureRegion = rm.getTextureRegion(vo.imageName + ".normal");
        } else {
            normalTextureRegionCM.remove(entity);
        }
    }
}
