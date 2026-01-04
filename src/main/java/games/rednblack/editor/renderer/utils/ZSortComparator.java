package games.rednblack.editor.renderer.utils;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.components.ZIndexComponent;

public class ZSortComparator {
    private ComponentMapper<ZIndexComponent> zIndexMapper;

    public void setzIndexMapper(ComponentMapper<ZIndexComponent> zIndexMapper) {
        this.zIndexMapper = zIndexMapper;
    }

    public int compare(int e1, int e2) {
        ZIndexComponent zIndexComponent1 = zIndexMapper.get(e1);
        ZIndexComponent zIndexComponent2 = zIndexMapper.get(e2);
        return zIndexComponent1.layerIndex == zIndexComponent2.layerIndex ? Integer.signum(zIndexComponent1.getZIndex() - zIndexComponent2.getZIndex()) : Integer.signum(zIndexComponent1.layerIndex - zIndexComponent2.layerIndex);
    }

    public void quickSort(int[] array, int length) {
        if (array == null || length == 0) {
            return;
        }
        quickSort(array, 0, length - 1);
    }

    private void quickSort(int[] array, int low, int high) {
        if (low < high) {
            int partitionIndex = partition(array, low, high);

            // Recursively sort elements before and after the partition index
            quickSort(array, low, partitionIndex - 1);
            quickSort(array, partitionIndex + 1, high);
        }
    }

    private int partition(int[] array, int low, int high) {
        int pivot = array[high];  // Choose the rightmost element as the pivot
        int i = low - 1;  // Index of smaller element

        for (int j = low; j < high; j++) {
            // If the current element is smaller than or equal to the pivot
            if (compare(array[j], pivot) <= 0) {
                i++;

                // Swap array[i] and array[j]
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }

        // Swap array[i+1] and array[high] (pivot)
        int temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;

        return i + 1;
    }
}
