package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.SceneVO;

/**
 * Created by azakhary on 9/9/2014.
 */
public interface IResourceRetriever {

    TextureRegion getTextureRegion(String name);
    TextureAtlas getTextureAtlas(String atlasName);
    boolean hasTextureRegion(String name);
    ParticleEffect getParticleEffect(String name);
    Array<TextureAtlas.AtlasRegion> getSpriteAnimation(String name);

    BitmapFont getBitmapFont(String name, int size, boolean mono);
    ShaderProgram getShaderProgram(String shaderName);

    SceneVO getSceneVO(String sceneName);
    ProjectInfoVO getProjectVO();

    ResolutionEntryVO getLoadedResolution();

    FileHandle getExternalItemType(int itemType, String name);
}
