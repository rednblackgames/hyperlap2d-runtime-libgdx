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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.ConeLight;
import games.rednblack.editor.renderer.box2dLight.PointLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.data.LightVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class LightComponentFactory extends ComponentFactory {

    protected ComponentMapper<LightObjectComponent> lightObjectCM;

    private final EntityTransmuter transmuter;

    public LightComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(ParentNodeComponent.class)
                .add(LightObjectComponent.class)
                .remove(BoundingBoxComponent.class)
                .build();
    }

    @Override
    public int createSpecialisedEntity(int root, MainItemVO vo) {
        int entity = createGeneralEntity(vo, EntityFactory.LIGHT_TYPE);
        transmuter.transmute(entity);

        adjustNodeHierarchy(root, entity);

        initializeLightObjectComponent(lightObjectCM.get(entity), (LightVO) vo);

        return entity;
    }

    protected void initializeDimensionsComponent(int entity, DimensionsComponent component, MainItemVO vo) {
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float boundBoxSize = 50f;
        component.boundBox = new Rectangle((-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, (-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld);
        component.width = boundBoxSize / projectInfoVO.pixelToWorld;
        component.height = boundBoxSize / projectInfoVO.pixelToWorld;
    }

    @Override
    protected void initializeTransformComponent(TransformComponent component, MainItemVO vo, DimensionsComponent dimensionsComponent) {
        super.initializeTransformComponent(component, vo, dimensionsComponent);
        component.originX = 0;
        component.originY = 0;
    }

    protected void initializeLightObjectComponent(LightObjectComponent component, LightVO vo) {
        if (vo.softnessLength == -1f) {
            vo.softnessLength = vo.distance * 0.1f;
        }

        component.type = vo.type;
        component.coneDegree = vo.coneDegree;
        component.directionDegree = vo.directionDegree;
        component.distance = vo.distance;
        component.height = vo.height;
        component.intensity = vo.intensity;
        component.softnessLength = vo.softnessLength;
        component.isStatic = vo.isStatic;
        component.isXRay = vo.isXRay;
        component.rays = vo.rays;
        component.isActive = vo.isActive;
        component.isSoft = vo.isSoft;

        if (component.type == LightObjectComponent.LightType.POINT) {
            component.lightObject = new PointLight(rayHandler, component.rays);
        } else {
            component.lightObject = new ConeLight(rayHandler, component.rays, Color.WHITE, 1, 0, 0, 0, 0);
        }

        component.lightObject.setSoftnessLength(component.softnessLength);
    }
}
