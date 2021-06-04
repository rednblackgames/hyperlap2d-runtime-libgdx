package games.rednblack.editor.renderer.resources;

import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.SceneVO;

/**
 * Created by azakhary on 9/9/2014.
 */
public interface IDataLoader {
    SceneVO loadSceneVO(String sceneName);
    ProjectInfoVO loadProjectVO();
}
