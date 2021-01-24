package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.talosvfx.talos.runtime.ParticleEffectDescriptor;
import com.talosvfx.talos.runtime.ParticleEffectInstance;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.utils.MySkin;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.SceneVO;

/**
 * Created by azakhary on 9/9/2014.
 */
public interface IResourceRetriever {

    TextureRegion getTextureRegion(String name);
    ParticleEffect getParticleEffect(String name);
    TextureAtlas getSkeletonAtlas(String name);
    FileHandle getSkeletonJSON(String name);
    ParticleEffectDescriptor getTalosVFX(String name);
    TextureAtlas getSpriteAnimation(String name);
    BitmapFont getBitmapFont(String name, int size);
    MySkin getSkin();

    SceneVO getSceneVO(String sceneName);
    ProjectInfoVO getProjectVO();

    ResolutionEntryVO getLoadedResolution();
    ShaderProgram getShaderProgram(String shaderName);
}
