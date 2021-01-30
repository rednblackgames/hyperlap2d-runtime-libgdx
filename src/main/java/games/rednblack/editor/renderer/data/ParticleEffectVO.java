package games.rednblack.editor.renderer.data;

import com.badlogic.ashley.core.Entity;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;

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
	public void loadFromEntity(Entity entity) {
		super.loadFromEntity(entity);

		ParticleComponent particleComponent = entity.getComponent(ParticleComponent.class);
		particleName = particleComponent.particleName;
		transform = particleComponent.transform;
	}
}
