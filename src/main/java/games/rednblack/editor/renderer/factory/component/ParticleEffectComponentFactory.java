package games.rednblack.editor.renderer.factory.component;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ParticleEffectVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class ParticleEffectComponentFactory extends ComponentFactory {
    protected ComponentMapper<ParticleComponent> particleCM;

    private final EntityTransmuter transmuter;

    public ParticleEffectComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(ParticleComponent.class)
                .remove(BoundingBoxComponent.class)
                .build();
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        DimensionsComponent component = dimensionsCM.get(entity);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float boundBoxSize = 70f;
        if (component.boundBox == null)
            component.boundBox = new Rectangle((-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, (-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld);
        component.width = boundBoxSize / projectInfoVO.pixelToWorld;
        component.height = boundBoxSize / projectInfoVO.pixelToWorld;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        ParticleComponent component = particleCM.get(entity);

        ParticleEffect particleEffect = rm.getParticleEffect(component.particleName);
        component.particleEffect = particleEffect;
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        component.worldMultiplier = 1f / projectInfoVO.pixelToWorld;
        component.scaleEffect(1f);

        particleEffect.reset(false);
        if (component.autoStart)
            particleEffect.start();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.PARTICLE_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        ParticleComponent component = particleCM.get(entity);
        component.particleName = (String) data;
    }

    @Override
    public Class<ParticleEffectVO> getVOType() {
        return ParticleEffectVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        ParticleEffectVO vo = (ParticleEffectVO) voG;
        ParticleComponent particleComponent = particleCM.get(entity);
        particleComponent.particleName = vo.particleName;
        particleComponent.transform = vo.transform;
        particleComponent.autoStart = vo.autoStart;
    }
}
