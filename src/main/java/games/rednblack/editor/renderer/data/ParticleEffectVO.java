package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class ParticleEffectVO extends MainItemVO {
	public String particleName = "";
	public boolean transform = true;

	public ParticleEffectVO() {
		super();
	}
	
	public ParticleEffectVO(ParticleEffectVO vo) {
		super(vo);
		particleName = vo.particleName;
		transform = vo.transform;
	}

	@Override
	public void loadFromEntity(int entity) {
		super.loadFromEntity(entity);

		ParticleComponent particleComponent = ComponentRetriever.get(entity, ParticleComponent.class);
		particleName = particleComponent.particleName;
		transform = particleComponent.transform;
	}
}
