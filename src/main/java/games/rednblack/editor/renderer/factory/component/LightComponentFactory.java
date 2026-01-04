package games.rednblack.editor.renderer.factory.component;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EntityTransmuter;
import games.rednblack.editor.renderer.ecs.EntityTransmuterFactory;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
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

    public LightComponentFactory(Engine engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(LightObjectComponent.class)
                .remove(BoundingBoxComponent.class)
                .build();
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        LightObjectComponent component = lightObjectCM.get(entity);

        if (component.softnessLength == -1f) {
            component.softnessLength = component.distance * 0.1f;
        }
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        DimensionsComponent component = dimensionsCM.get(entity);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float boundBoxSize = 50f;
        if (component.boundBox == null)
            component.boundBox = new Rectangle((-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, (-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld);
        component.width = boundBoxSize / projectInfoVO.pixelToWorld;
        component.height = boundBoxSize / projectInfoVO.pixelToWorld;
    }

    @Override
    protected void postEntityInitialization(int entity) {
        super.postEntityInitialization(entity);

        TransformComponent component = transformCM.get(entity);
        component.originX = 0;
        component.originY = 0;
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.LIGHT_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        LightObjectComponent component = lightObjectCM.get(entity);
        component.type = (LightObjectComponent.LightType) data;
        component.distance = component.distance / projectInfoVO.pixelToWorld;
    }

    @Override
    public Class<LightVO> getVOType() {
        return LightVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        LightVO vo = (LightVO) voG;
        TransformComponent transformComponent = transformCM.get(entity);
        transformComponent.originX = 0;
        transformComponent.originY = 0;

        LightObjectComponent lightObjectComponent = lightObjectCM.get(entity);
        lightObjectComponent.type = vo.type;
        lightObjectComponent.coneDegree = vo.coneDegree;
        lightObjectComponent.directionDegree = vo.directionDegree;
        lightObjectComponent.distance = vo.distance;
        lightObjectComponent.height = vo.height;
        lightObjectComponent.intensity = vo.intensity;
        lightObjectComponent.falloff = vo.falloff;
        lightObjectComponent.softnessLength = vo.softnessLength;
        lightObjectComponent.isStatic = vo.isStatic;
        lightObjectComponent.isXRay = vo.isXRay;
        lightObjectComponent.rays = vo.rays;
        lightObjectComponent.isActive = vo.isActive;
        lightObjectComponent.isSoft = vo.isSoft;
    }
}
