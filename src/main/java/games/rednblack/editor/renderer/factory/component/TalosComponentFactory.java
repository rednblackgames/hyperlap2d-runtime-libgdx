package games.rednblack.editor.renderer.factory.component;

import box2dLight.RayHandler;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import com.talosvfx.talos.runtime.ParticleEffectDescriptor;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.additional.TalosComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.TalosVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class TalosComponentFactory extends ComponentFactory {

    public TalosComponentFactory(PooledEngine engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    @Override
    public void createComponents(Entity root, Entity entity, MainItemVO vo) {
        createCommonComponents(entity, vo, EntityFactory.TALOS_TYPE);
        entity.remove(BoundingBoxComponent.class);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);
        createParticleComponent(entity, (TalosVO) vo);
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(Entity entity, MainItemVO vo) {
        DimensionsComponent component = engine.createComponent(DimensionsComponent.class);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float boundBoxSize = 70f;
        component.boundBox = new Rectangle((-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, (-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld);
        component.width = boundBoxSize / projectInfoVO.pixelToWorld;
        component.height = boundBoxSize / projectInfoVO.pixelToWorld;

        entity.add(component);
        return component;
    }

    protected TalosComponent createParticleComponent(Entity entity, TalosVO vo) {
        TalosComponent component = engine.createComponent(TalosComponent.class);
        component.particleName = vo.particleName;
        component.transform = vo.transform;
        ParticleEffectDescriptor effectDescriptor = rm.getTalosVFX(vo.particleName);
        component.effect = effectDescriptor.createEffectInstance();

        entity.add(component);
        return component;
    }
}
