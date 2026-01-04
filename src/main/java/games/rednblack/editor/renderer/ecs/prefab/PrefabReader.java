package games.rednblack.editor.renderer.ecs.prefab;

import games.rednblack.editor.renderer.ecs.annotations.PrefabData;

/**
 * <p>Reads prefab data, receiving the <code>path</code>
 * from the prefab's {@link PrefabData}.</p>
 *
 * <p>The <code>artemis-odb-serializer-json-libgdx</code> and
 * <code>artemis-odb-serializer-json</code> artifacts bundle a default
 * <code>JsonValuePrefabReader</code>, which covers typical usage.</p>
 *
 * @see BasePrefab
 * @see PrefabData
 * @param <DATA> source data type.
 */
public interface PrefabReader<DATA> {
	void initialize(String path);
	DATA getData();
}
