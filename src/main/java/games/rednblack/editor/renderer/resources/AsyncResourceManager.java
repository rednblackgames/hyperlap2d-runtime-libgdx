package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.GdxRuntimeException;
import games.rednblack.editor.renderer.data.ProjectInfoVO;

import java.io.File;
import java.util.HashSet;

public class AsyncResourceManager extends ResourceManager {

    @Override
    public ProjectInfoVO getProjectVO() {
        return super.getProjectVO();
    }

    public void setProjectInfoVO(ProjectInfoVO vo) {
        this.projectVO = vo;
    }

    public HashSet<String> getSpineAnimNamesToLoad() {
        return this.spineAnimNamesToLoad;
    }

    public void addAtlasPack(String name, TextureAtlas pack) {
        this.atlasesPack.put(name, pack);
    }

    /**
     * Spine Animations
     */

    @Override
    public void loadSpineAnimations() {
        throw new GdxRuntimeException("see loadSpineAnimations(AssetManager)");
    }

    @Override
    public void loadSpineAnimation(String name) {
        throw new GdxRuntimeException("see loadSpineAnimation(AssetManager, String)");
    }

    public void loadSpineAnimations(AssetManager manager) {
        for (String name : spineAnimNamesToLoad) {
            loadSpineAnimation(manager, name);
        }
    }

    public void loadSpineAnimation(AssetManager manager, String name) {
        skeletonJSON.put(name, Gdx.files.internal("orig" + File.separator + spineAnimationsPath + File.separator + name + File.separator + name + ".json"));
    }
}
