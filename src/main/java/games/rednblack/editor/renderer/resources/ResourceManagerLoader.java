package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.ExternalTypesConfiguration;
import games.rednblack.editor.renderer.data.ProjectInfoVO;

import java.io.File;

public class ResourceManagerLoader extends AsynchronousAssetLoader<AsyncResourceManager, ResourceManagerLoader.AsyncResourceManagerParam> {

    private final AsyncResourceManager asyncResourceManager;
    private AsyncResourceManager result = null;
    private ProjectInfoVO projectInfoVO;

    public ResourceManagerLoader(FileHandleResolver resolver) {
        this(null, resolver);
    }

    public ResourceManagerLoader(ExternalTypesConfiguration externalTypesConfiguration, FileHandleResolver resolver) {
        super(resolver);
        this.asyncResourceManager = new AsyncResourceManager(externalTypesConfiguration);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, AsyncResourceManagerParam parameter) {
        result = null;
        if (!fileName.equals("project.dt")) {
            throw new GdxRuntimeException("fileName must be project.dt");
        }

        for (String pack : projectInfoVO.imagesPacks.keySet()) {
            String name = pack.equals("main") ? "pack.atlas" : pack + ".atlas";
            FileHandle packFile = Gdx.files.internal(this.asyncResourceManager.packResolutionName + File.separator + name);
            this.asyncResourceManager.addAtlasPack(pack, manager.get(packFile.path(), TextureAtlas.class));
        }

        for (String pack : projectInfoVO.animationsPacks.keySet()) {
            String name = pack.equals("main") ? "pack.atlas" : pack + ".atlas";
            FileHandle packFile = Gdx.files.internal(this.asyncResourceManager.packResolutionName + File.separator + name);
            this.asyncResourceManager.addAtlasPack(pack, manager.get(packFile.path(), TextureAtlas.class));
        }

        this.asyncResourceManager.loadReverseAtlasMap();
        this.asyncResourceManager.loadSpriteAnimations();
        this.asyncResourceManager.loadParticleEffects();
        this.asyncResourceManager.loadBitmapFonts();

        this.asyncResourceManager.loadExternalTypesAsync();

        result = this.asyncResourceManager;
    }

    @Override
    public AsyncResourceManager loadSync(AssetManager manager, String fileName, FileHandle file, AsyncResourceManagerParam parameter) {
        if (!fileName.equals("project.dt")) {
            throw new GdxRuntimeException("fileName must be project.dt");
        }

        if (result != null) {
            this.asyncResourceManager.loadFonts();
            this.asyncResourceManager.loadShaders();

            this.asyncResourceManager.loadExternalTypesSync();
        }

        return result;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, AsyncResourceManagerParam parameter) {
        if (!fileName.equals("project.dt")) {
            throw new GdxRuntimeException("fileName must be project.dt");
        }

        this.projectInfoVO = this.asyncResourceManager.loadProjectVO();
        for (int i = 0; i < this.projectInfoVO.scenes.size(); i++) {
            String sceneName = this.projectInfoVO.scenes.get(i).sceneName;

            if (parameter == null || (parameter.loadAllScenes || parameter.scenes.contains(sceneName, false))) {
                this.asyncResourceManager.loadSceneVO(sceneName);
                this.asyncResourceManager.scheduleScene(sceneName);
            }
        }
        this.asyncResourceManager.prepareAssetsToLoad();

        //Prepare additional assets not included in any scenes
        if (parameter != null) {
            for (String name : parameter.particleEffects) {
                this.asyncResourceManager.prepareParticleEffect(name);
            }
            for (int type : parameter.externals.keys()) {
                for (String asset : parameter.externals.get(type))
                    this.asyncResourceManager.prepareExternalType(type, asset);
            }
            for (String name : parameter.spriteAnims) {
                this.asyncResourceManager.prepareSprite(name);
            }
            for (FontSizePair name : parameter.fonts) {
                this.asyncResourceManager.prepareFont(name);
            }
            for (String name : parameter.shaders) {
                this.asyncResourceManager.prepareShader(name);
            }
        }

        //Build dependency list
        Array<AssetDescriptor> deps = new Array<>();

        for (String pack : projectInfoVO.imagesPacks.keySet()) {
            String name = pack.equals("main") ? "pack.atlas" : pack + ".atlas";
            FileHandle packFile = Gdx.files.internal(this.asyncResourceManager.packResolutionName + File.separator + name);
            if (packFile.exists()) {
                deps.add(new AssetDescriptor(packFile, TextureAtlas.class));
            }
        }

        for (String pack : projectInfoVO.animationsPacks.keySet()) {
            String name = pack.equals("main") ? "pack.atlas" : pack + ".atlas";
            FileHandle packFile = Gdx.files.internal(this.asyncResourceManager.packResolutionName + File.separator + name);
            if (packFile.exists()) {
                deps.add(new AssetDescriptor(packFile, TextureAtlas.class));
            }
        }

        return deps;
    }

    public static class AsyncResourceManagerParam extends AssetLoaderParameters<AsyncResourceManager> {
        public final Array<String> spriteAnims = new Array<>();
        public final Array<String> particleEffects = new Array<>();
        public final Array<FontSizePair> fonts = new Array<>();
        public final Array<String> shaders = new Array<>();
        private final ObjectMap<Integer, Array<String>> externals = new ObjectMap<>();

        public boolean loadAllScenes = true;
        public final Array<String> scenes = new Array<>();

        public void addExternalResType(int type, String name) {
            Array<String> assets = externals.get(type);
            if (assets == null) {
                assets = new Array<>();
                externals.put(type, assets);
            }
            assets.add(name);
        }
    }
}

