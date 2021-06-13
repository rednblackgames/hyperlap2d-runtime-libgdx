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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.ConeLight;
import games.rednblack.editor.renderer.box2dLight.PointLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.data.LightVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class LightComponentFactory extends ComponentFactory {

    public LightComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    @Override
    public void createComponents(int root, int entity, MainItemVO vo) {
        createCommonComponents(entity, vo, EntityFactory.LIGHT_TYPE);
        engine.edit(entity).remove(BoundingBoxComponent.class);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);
        createLightObjectComponent(entity, (LightVO) vo);
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo) {
        DimensionsComponent component = engine.edit(entity).create(DimensionsComponent.class);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float boundBoxSize = 50f;
        component.boundBox = new Rectangle((-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, (-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld);
        component.width = boundBoxSize / projectInfoVO.pixelToWorld;
        component.height = boundBoxSize / projectInfoVO.pixelToWorld;

        return component;
    }

    protected LightObjectComponent createLightObjectComponent(int entity, LightVO vo) {
        if (vo.softnessLength == -1f) {
            vo.softnessLength = vo.distance * 0.1f;
        }

        LightObjectComponent component = engine.edit(entity).create(LightObjectComponent.class);
        component.setType(vo.type);
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

        if (component.getType() == LightVO.LightType.POINT) {
            component.lightObject = new PointLight(rayHandler, component.rays);
        } else {
            component.lightObject = new ConeLight(rayHandler, component.rays, Color.WHITE, 1, 0, 0, 0, 0);
        }

        component.lightObject.setSoftnessLength(component.softnessLength);

        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class);
        transformComponent.originX = 0;
        transformComponent.originY = 0;
        return component;
    }
}
