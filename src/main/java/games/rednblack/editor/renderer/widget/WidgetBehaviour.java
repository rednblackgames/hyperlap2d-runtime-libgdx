package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.widget.WidgetComponent;

/**
 * A behaviour component that starts from the widget's settings. It is set up the moment it is
 * attached, as the scene loads, so game code acting right after the load is never undone by the
 * settings on the first frame: the systems only follow a setting when it changes afterwards.
 */
public interface WidgetBehaviour {

    void initialise(WidgetComponent widget);
}
