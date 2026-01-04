package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class ParticleEffectVO extends MainItemVO {
	public String particleName = "";
	public boolean transform = true;
	public boolean autoStart = true;

	public ParticleEffectVO() {
		super();
	}
	
	public ParticleEffectVO(ParticleEffectVO vo) {
		super(vo);
		particleName = vo.particleName;
		transform = vo.transform;
		autoStart = vo.autoStart;
	}

	@Override
	public void loadFromEntity(int entity, Engine engine, EntityFactory entityFactory) {
		super.loadFromEntity(entity, engine, entityFactory);

		ParticleComponent particleComponent = ComponentRetriever.get(entity, ParticleComponent.class, engine);
		particleName = particleComponent.particleName;
		transform = particleComponent.transform;
		autoStart = particleComponent.autoStart;
	}

	@Override
	public String getResourceName() {
		return particleName;
	}
}
