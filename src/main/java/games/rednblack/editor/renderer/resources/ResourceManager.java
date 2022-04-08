package games.rednblack.editor.renderer.resources;

import java.io.File;
import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.*;
import games.rednblack.editor.renderer.ExternalTypesConfiguration;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.utils.HyperJson;
import games.rednblack.editor.renderer.utils.ShaderCompiler;

/**
 * Default ResourceManager that you can reuse or extend
 * Generally is good to load all the assets that are exported from editor
 * using default settings (The paths and file structure should be exact)
 * If changed by you manually, please override this class methods in order to keep it working.
 * <p>
 * The main logic is to prepare list of resources that needs to be load for specified scenes, and then loaded.
 * <p>
 * Created by azakhary on 9/9/2014.
 */
public class ResourceManager implements IResourceLoader, IResourceRetriever, Disposable {

    public static int PARTICLE_POOL_SIZE = 100;

    /**
     * Paths (please change if different) this is the default structure exported from editor
     */
    public String packResolutionName = "orig";
    public String scenesPath = "scenes";
    public String particleEffectsPath = "particles";
    public String fontsPath = "freetypefonts";
    public String shadersPath = "shaders";
    public String bitmapFontsPath = "bitmapfonts";

    protected float resMultiplier;

    protected ProjectInfoVO projectVO;

    protected ArrayList<String> preparedSceneNames = new ArrayList<String>();
    protected HashMap<String, SceneVO> loadedSceneVOs = new HashMap<String, SceneVO>();

    protected ObjectSet<String> particleEffectNamesToLoad = new ObjectSet<>();
    protected ObjectSet<String> spriteAnimNamesToLoad = new ObjectSet<>();
    protected ObjectSet<FontSizePair> fontsToLoad = new ObjectSet<>();
    protected ObjectSet<String> shaderNamesToLoad = new ObjectSet<>();
    protected ObjectSet<String> bitmapFontsToLoad = new ObjectSet<>();
    protected ObjectMap<Integer, ObjectSet<String>> externalItemsToLoad = new ObjectMap<>();

    protected HashMap<String, String> reverseAtlasMap = new HashMap<>();
    protected HashMap<String, TextureAtlas> atlasesPack = new HashMap<>();
    protected HashMap<String, ParticleEffectPool> particleEffects = new HashMap<>();
    protected HashMap<String, ShaderProgram> shaderPrograms = new HashMap<>();
    protected HashMap<String, Array<TextureAtlas.AtlasRegion>> spriteAnimations = new HashMap<>();
    protected HashMap<FontSizePair, BitmapFont> fonts = new HashMap<>();
    protected HashMap<String, BitmapFont> bitmapFonts = new HashMap<>();
    protected IntMap<HashMap<String, Object>> externalItems = new IntMap<>();

    protected IntMap<IExternalItemType> externalItemTypes = new IntMap<>();

    public ResourceManager() {
        this(null);
    }

    /**
     * Initialize Resource Manager with {@link ExternalTypesConfiguration} to retrieve external item types
     */
    public ResourceManager(ExternalTypesConfiguration externalTypesConfiguration) {
        if (externalTypesConfiguration != null) {
            for (IExternalItemType itemType : externalTypesConfiguration) {
                if (itemType.hasResources())
                    externalItemTypes.put(itemType.getTypeId(), itemType);
            }
        }
    }

    /**
     * Sets working resolution, please set before doing any loading
     *
     * @param resolution String resolution name, default is "orig" later use resolution names created in editor
     */
    public void setWorkingResolution(String resolution) {
        ResolutionEntryVO resolutionObject = getProjectVO().getResolution(resolution);
        if (resolutionObject != null) {
            packResolutionName = resolution;
        }
    }

    /**
     * Easy use loader
     * Iterates through all scenes and schedules all for loading
     * Prepares all the assets to be loaded that are used in scheduled scenes
     * finally loads all the prepared assets
     */
    public void initAllResources() {
        loadProjectVO();
        for (int i = 0; i < projectVO.scenes.size(); i++) {
            loadSceneVO(projectVO.scenes.get(i).sceneName);
            scheduleScene(projectVO.scenes.get(i).sceneName);
        }
        prepareAssetsToLoad();
        loadAssets();
    }

    /**
     * Initializes scene by loading it's VO data object and loading all the assets needed for this particular scene only
     *
     * @param sceneName - scene file name without ".dt" extension
     */
    public void initScene(String sceneName) {
        loadSceneVO(sceneName);
        scheduleScene(sceneName);
        prepareAssetsToLoad();
        loadAssets();
    }

    /**
     * Anloads scene from the memory, and clears all the freed assets
     *
     * @param sceneName - scene file name without ".dt" extension
     */
    public void unLoadScene(String sceneName) {
        unScheduleScene(sceneName);
        loadedSceneVOs.remove(sceneName);
        loadAssets();
    }

    /**
     * Schedules scene for later loading
     * if later prepareAssetsToLoad function will be called it will only prepare assets that are used in scheduled scene
     *
     * @param name - scene file name without ".dt" extension
     */
    public void scheduleScene(String name) {
        if (loadedSceneVOs.containsKey(name)) {
            preparedSceneNames.add(name);
        } else {
            //TODO: Throw exception that scene was not loaded to be prepared for asseting
        }

    }

    /**
     * Unschedule scene from later loading
     *
     * @param name
     */
    public void unScheduleScene(String name) {
        preparedSceneNames.remove(name);
    }


    /**
     * Creates the list of uniqe assets used in all of the scheduled scenes,
     * removes all the duplicates, and makes list of assets that are only needed.
     */
    public void prepareAssetsToLoad() {
        particleEffectNamesToLoad.clear();
        externalItemsToLoad.clear();
        spriteAnimNamesToLoad.clear();
        bitmapFontsToLoad.clear();
        fontsToLoad.clear();
        shaderPrograms.clear();
        bitmapFonts.clear();

        for (String preparedSceneName : preparedSceneNames) {
            CompositeItemVO composite = loadedSceneVOs.get(preparedSceneName).composite;
            if (composite == null) {
                continue;
            }
            //Load assets from scenes
            Array<String> particleEffects = composite.getRecursiveTypeNamesList(ParticleEffectVO.class);
            Array<String> spriteAnimations = composite.getRecursiveTypeNamesList(SpriteAnimationVO.class);
            Array<String> bitmapFonts = composite.getRecursiveTypeNamesList(LabelVO.class);
            Array<String> shaderNames = composite.getRecursiveShaderList();
            Array<FontSizePair> fonts = composite.getRecursiveFontList();

            bitmapFontsToLoad.addAll(bitmapFonts);
            particleEffectNamesToLoad.addAll(particleEffects);
            spriteAnimNamesToLoad.addAll(spriteAnimations);
            fontsToLoad.addAll(fonts);
            shaderNamesToLoad.addAll(shaderNames);

            for (IExternalItemType itemType : externalItemTypes.values()) {
                Array<String> assetsList = composite.getRecursiveTypeNamesList(itemType.getComponentFactory().getVOType());
                addExternalTypeAssetToLoad(itemType.getTypeId(), assetsList);
            }

            //Load from composite items
            for (CompositeItemVO library : projectVO.libraryItems.values()) {
                Array<FontSizePair> libFonts = library.getRecursiveFontList();
                Array<String> libEffects = library.getRecursiveTypeNamesList(ParticleEffectVO.class);
                Array<String> libShaderNames = library.getRecursiveShaderList();
                Array<String> libSpriteAnimations = library.getRecursiveTypeNamesList(SpriteAnimationVO.class);
                Array<String> libBitmapFonts = library.getRecursiveTypeNamesList(LabelVO.class);

                fontsToLoad.addAll(libFonts);
                shaderNamesToLoad.addAll(libShaderNames);
                particleEffectNamesToLoad.addAll(libEffects);
                spriteAnimNamesToLoad.addAll(libSpriteAnimations);
                bitmapFontsToLoad.addAll(libBitmapFonts);

                for (IExternalItemType itemType : externalItemTypes.values()) {
                    Array<String> assetsList = library.getRecursiveTypeNamesList(itemType.getComponentFactory().getVOType());
                    addExternalTypeAssetToLoad(itemType.getTypeId(), assetsList);
                }
            }
        }
    }

    protected void addExternalTypeAssetToLoad(int type, Array<String> names) {
        ObjectSet<String> objectSet = externalItemsToLoad.get(type);
        if (objectSet == null) {
            objectSet = new ObjectSet<>();
            externalItemsToLoad.put(type, objectSet);
        }
        objectSet.addAll(names);
    }

    protected void addExternalTypeAssetToLoad(int type, String name) {
        ObjectSet<String> objectSet = externalItemsToLoad.get(type);
        if (objectSet == null) {
            objectSet = new ObjectSet<>();
            externalItemsToLoad.put(type, objectSet);
        }
        objectSet.add(name);
    }

    protected HashMap<String, Object> getExternalItems(int type) {
        HashMap<String, Object> items = externalItems.get(type);
        if (items == null) {
            items = new HashMap<>();
            externalItems.put(type, items);
        }
        return items;
    }

    /*
        Prepare additional assets
     */
    public void prepareParticleEffect(String name) {
        particleEffectNamesToLoad.add(name);
    }

    public void prepareExternalType(int type, String name) {
        addExternalTypeAssetToLoad(type, name);
    }

    public void prepareSprite(String name) {
        spriteAnimNamesToLoad.add(name);
    }

    public void prepareFont(FontSizePair name) {
        fontsToLoad.add(name);
    }

    public void prepareShader(String name) {
        shaderNamesToLoad.add(name);
    }

    /**
     * Loads all the scheduled assets into memory including
     * main atlas pack, particle effects, sprite animations, spine animations and fonts
     */
    public void loadAssets() {
        loadAtlasPacks();
        loadParticleEffects();
        loadSpriteAnimations();
        loadFonts();
        loadBitmapFonts();
        loadShaders();
        loadExternalTypes();
    }

    @Override
    public void loadAtlasPacks() {
        for (String pack : projectVO.imagesPacks.keySet()) {
            String name = pack.equals("main") ? "pack.atlas" : pack + ".atlas";
            FileHandle packFile = Gdx.files.internal(packResolutionName + File.separator + name);
            if (packFile.exists() && atlasesPack.get(pack) == null) {
                atlasesPack.put(pack, new TextureAtlas(packFile));
            }
        }

        for (String pack : projectVO.animationsPacks.keySet()) {
            String name = pack.equals("main") ? "pack.atlas" : pack + ".atlas";
            FileHandle packFile = Gdx.files.internal(packResolutionName + File.separator + name);
            if (packFile.exists() && atlasesPack.get(pack) == null) {
                atlasesPack.put(pack, new TextureAtlas(packFile));
            }
        }

        loadReverseAtlasMap();
    }

    public void loadReverseAtlasMap() {
        for (String atlasPackName : atlasesPack.keySet()) {
            TextureAtlas atlas = atlasesPack.get(atlasPackName);
            for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
                reverseAtlasMap.put(region.name, atlasPackName);
            }
        }
    }

    @Override
    public void loadParticleEffects() {
        // empty existing ones that are not scheduled to load
        for (String key : particleEffects.keySet()) {
            if (!particleEffectNamesToLoad.contains(key)) {
                particleEffects.remove(key);
            }
        }

        // load scheduled
        for (String name : particleEffectNamesToLoad) {
            ParticleEffect effect = new ParticleEffect();
            effect.loadEmitters(Gdx.files.internal(particleEffectsPath + File.separator + name));
            for (TextureAtlas atlas : atlasesPack.values()) {
                try {
                    effect.loadEmitterImages(atlas, "");
                    ParticleEffectPool effectPool = new ParticleEffectPool(effect, 1, PARTICLE_POOL_SIZE);
                    particleEffects.put(name, effectPool);
                    break;
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    public void loadSpriteAnimations() {
        // empty existing ones that are not scheduled to load
        for (String key : spriteAnimations.keySet()) {
            if (!spriteAnimNamesToLoad.contains(key)) {
                spriteAnimations.remove(key);
            }
        }

        for (String name : spriteAnimNamesToLoad) {
            TextureAtlas atlas = atlasesPack.get(reverseAtlasMap.get(name));
            spriteAnimations.put(name, atlas.findRegions(name));
        }
    }

    @Override
    public void loadExternalTypes() {
        for (int assetType : externalItemsToLoad.keys()) {
            IExternalItemType externalItemType = externalItemTypes.get(assetType);
            ObjectSet<String> assetsToLoad = externalItemsToLoad.get(assetType);
            HashMap<String, Object> assets = getExternalItems(assetType);

            externalItemType.loadExternalTypesAsync(this, assetsToLoad, assets);
            externalItemType.loadExternalTypesSync(this, assetsToLoad, assets);
        }
    }

    @Override
    public void loadBitmapFonts() {
        // empty existing ones that are not scheduled to load
        for (String key : bitmapFonts.keySet()) {
            if (!bitmapFontsToLoad.contains(key)) {
                bitmapFonts.remove(key);
            }
        }

        // load scheduled
        for (String name : bitmapFontsToLoad) {
            BitmapFont bitmapFont = new BitmapFont(Gdx.files.internal(bitmapFontsPath + File.separator + name + ".fnt"), getTextureRegion(name));
            bitmapFont.setUseIntegerPositions(false);
            bitmapFonts.put(bitmapFont.getData().name, bitmapFont);
        }
    }

    @Override
    public void loadFonts() {
        //resolution related stuff
        ResolutionEntryVO curResolution = getProjectVO().getResolution(packResolutionName);
        resMultiplier = 1;
        if (!packResolutionName.equals("orig")) {
            if (curResolution.base == 0) {
                resMultiplier = (float) curResolution.width / (float) getProjectVO().originalResolution.width;
            } else {
                resMultiplier = (float) curResolution.height / (float) getProjectVO().originalResolution.height;
            }
        }

        // empty existing ones that are not scheduled to load
        for (FontSizePair pair : fonts.keySet()) {
            if (!fontsToLoad.contains(pair)) {
                fonts.remove(pair);
            }
        }

        for (FontSizePair pair : fontsToLoad) {
            loadFont(pair);
        }
    }

    public void loadFont(FontSizePair pair) {
        FileHandle fontFile;
        fontFile = Gdx.files.internal(fontsPath + File.separator + pair.fontName + ".ttf");
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.round(pair.fontSize * resMultiplier);
        parameter.mono = pair.monoSpace;
        BitmapFont font = generator.generateFont(parameter);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);
        if (pair.monoSpace)
            font.setFixedWidthGlyphs(FreeTypeFontGenerator.DEFAULT_CHARS);
        fonts.put(pair, font);
    }

    @Override
    public SceneVO loadSceneVO(String sceneName) {
        FileHandle file = Gdx.files.internal(scenesPath + File.separator + sceneName + ".dt");
        Json json = HyperJson.getJson();
        SceneVO sceneVO = json.fromJson(SceneVO.class, file.readString());

        loadedSceneVOs.put(sceneName, sceneVO);

        return sceneVO;
    }

    public void unLoadSceneVO(String sceneName) {
        loadedSceneVOs.remove(sceneName);
    }

    @Override
    public ProjectInfoVO loadProjectVO() {

        FileHandle file = Gdx.files.internal("project.dt");
        Json json = HyperJson.getJson();
        projectVO = json.fromJson(ProjectInfoVO.class, file.readString());

        return projectVO;
    }

    @Override
    public void loadShaders() {
        // empty existing ones that are not scheduled to load
        for (String key : shaderPrograms.keySet()) {
            if (!shaderNamesToLoad.contains(key)) {
                shaderPrograms.get(key).dispose();
                shaderPrograms.remove(key);
            }
        }

        for (String name : shaderNamesToLoad) {
            ShaderProgram shaderProgram = ShaderCompiler.compileShader(Gdx.files.internal(shadersPath + File.separator + name + ".vert"), Gdx.files.internal(shadersPath + File.separator + name + ".frag"));
            shaderPrograms.put(name, shaderProgram);
        }
    }

    /**
     * Following methods are for retriever interface, which is intended for runtime internal use
     * to retrieve any already loaded into memory asset for rendering
     */

    @Override
    public TextureRegion getTextureRegion(String name) {
        if (reverseAtlasMap.get(name) == null)
            return null;
        TextureAtlas atlas = atlasesPack.get(reverseAtlasMap.get(name));
        return atlas.findRegion(name);
    }

    @Override
    public TextureAtlas getTextureAtlas(String atlasName) {
        return atlasesPack.get(atlasName);
    }

    @Override
    public ParticleEffect getParticleEffect(String name) {
        return particleEffects.get(name).obtain();
    }

    @Override
    public Object getExternalItemType(int itemType, String name) {
        return externalItems.get(itemType).get(name);
    }

    @Override
    public Array<TextureAtlas.AtlasRegion> getSpriteAnimation(String name) {
        return spriteAnimations.get(name);
    }

    @Override
    public BitmapFont getFont(String name, int size, boolean mono) {
        return fonts.get(new FontSizePair(name, size, mono));
    }

    @Override
    public boolean hasTextureRegion(String regionName) {
        if (reverseAtlasMap.get(regionName) == null)
            return false;
        TextureAtlas atlas = atlasesPack.get(reverseAtlasMap.get(regionName));
        return atlas.findRegion(regionName) != null;
    }

    @Override
    public SceneVO getSceneVO(String sceneName) {
        return loadedSceneVOs.get(sceneName);
    }

    @Override
    public ProjectInfoVO getProjectVO() {
        return projectVO;
    }

    @Override
    public ResolutionEntryVO getLoadedResolution() {
        if (packResolutionName.equals("orig")) {
            return getProjectVO().originalResolution;
        }
        return getProjectVO().getResolution(packResolutionName);
    }

    @Override
    public void dispose() {
        for (TextureAtlas atlas : atlasesPack.values())
            atlas.dispose();

        for (BitmapFont font : fonts.values()) {
            font.dispose();
        }
    }

    @Override
    public ShaderProgram getShaderProgram(String shaderName) {
        return shaderPrograms.get(shaderName);
    }

    @Override
    public BitmapFont getBitmapFont(String name) {
        return bitmapFonts.get(name);
    }
}
