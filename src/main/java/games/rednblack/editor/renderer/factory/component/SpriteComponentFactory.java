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
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

/**
 * Created by azakhary on 5/22/2015.
 */
public class SpriteComponentFactory extends ComponentFactory {

    protected static ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
    protected static ComponentMapper<SpriteAnimationStateComponent> spriteAnimationStateCM;
    protected static ComponentMapper<TextureRegionComponent> textureRegionCM;

    public SpriteComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    @Override
    public void createComponents(int root, int entity, MainItemVO vo) {
        createCommonComponents(entity, vo, EntityFactory.SPRITE_TYPE);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);
        createSpriteAnimationDataComponent(entity, (SpriteAnimationVO) vo);
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo) {
        DimensionsComponent component = dimensionsCM.create(entity);

        SpriteAnimationVO sVo = (SpriteAnimationVO) vo;
        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(sVo.animationName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);
        component.width = (float) regions.get(0).getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        component.height = (float) regions.get(0).getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;

        return component;
    }

    protected SpriteAnimationComponent createSpriteAnimationDataComponent(int entity, SpriteAnimationVO vo) {
        SpriteAnimationComponent spriteAnimationComponent = spriteAnimationCM.create(entity);
        spriteAnimationComponent.animationName = vo.animationName;

        for (int i = 0; i < vo.frameRangeMap.size(); i++) {
            spriteAnimationComponent.frameRangeMap.put(vo.frameRangeMap.get(i).name, vo.frameRangeMap.get(i));
        }
        spriteAnimationComponent.fps = vo.fps;
        spriteAnimationComponent.currentAnimation = vo.currentAnimation;

        if (vo.playMode == 0) spriteAnimationComponent.playMode = Animation.PlayMode.NORMAL;
        if (vo.playMode == 1) spriteAnimationComponent.playMode = Animation.PlayMode.REVERSED;
        if (vo.playMode == 2) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP;
        if (vo.playMode == 3) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP_REVERSED;
        if (vo.playMode == 4) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP_PINGPONG;
        if (vo.playMode == 5) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP_RANDOM;
        if (vo.playMode == 6) spriteAnimationComponent.playMode = Animation.PlayMode.NORMAL;

        // filtering regions by name
        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(spriteAnimationComponent.animationName);

        SpriteAnimationStateComponent stateComponent = spriteAnimationStateCM.create(entity);
        stateComponent.setAllRegions(regions);

        if (spriteAnimationComponent.frameRangeMap.isEmpty()) {
            spriteAnimationComponent.frameRangeMap.put("Default", new FrameRange("Default", 0, regions.size - 1));
        }
        if (spriteAnimationComponent.currentAnimation == null) {
            spriteAnimationComponent.currentAnimation = (String) spriteAnimationComponent.frameRangeMap.keySet().toArray()[0];
        }
        if (spriteAnimationComponent.playMode == null) {
            spriteAnimationComponent.playMode = Animation.PlayMode.LOOP;
        }

        stateComponent.set(spriteAnimationComponent);

        TextureRegionComponent textureRegionComponent = textureRegionCM.create(entity);
        textureRegionComponent.region = regions.get(0);

        return spriteAnimationComponent;
    }
}
