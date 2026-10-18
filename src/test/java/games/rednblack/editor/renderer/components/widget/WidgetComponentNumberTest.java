package games.rednblack.editor.renderer.components.widget;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Settings read as numbers. The systems ask for these every frame, so the answer is kept and the
 * text is only read again once somebody writes the property.
 */
public class WidgetComponentNumberTest {

    private WidgetComponent widget;

    @Before
    public void setUp() {
        widget = new WidgetComponent();
    }

    @Test
    public void aSettingIsReadAsWhatIsWrittenInIt() {
        widget.properties.put("max", " 42.5 ");

        assertEquals(42.5f, widget.number("max", 0), 0.0001f);
        assertEquals(42.5f, widget.number("max", 0), 0.0001f);
    }

    @Test
    public void aSettingNobodySetIsTheFallback() {
        assertEquals(7f, widget.number("max", 7), 0.0001f);
    }

    @Test
    public void aSettingThatIsNotANumberIsTheFallbackToo() {
        widget.properties.put("max", "quite a lot");

        assertEquals(7f, widget.number("max", 7), 0.0001f);
    }

    /** Whoever writes the property writes a string of their own, which is how the change is seen. */
    @Test
    public void writingTheSettingIsFollowed() {
        widget.properties.put("max", "10");
        assertEquals(10f, widget.number("max", 0), 0.0001f);

        widget.properties.put("max", String.valueOf(20));
        assertEquals(20f, widget.number("max", 0), 0.0001f);
    }

    /** A property taken away leaves the fallback, and putting it back is read afresh. */
    @Test
    public void removingTheSettingIsFollowedAsWell() {
        widget.properties.put("max", "10");
        widget.number("max", 0);

        widget.properties.remove("max");
        assertEquals(3f, widget.number("max", 3), 0.0001f);

        widget.properties.put("max", "30");
        assertEquals(30f, widget.number("max", 0), 0.0001f);
    }

    /** A pooled component is handed out again empty: what it once read must not come with it. */
    @Test
    public void aRecycledWidgetRemembersNothing() {
        widget.properties.put("max", "10");
        widget.number("max", 0);

        widget.reset();
        widget.properties.put("max", "10");

        assertEquals(10f, widget.number("max", 0), 0.0001f);
        assertEquals(5f, widget.number("min", 5), 0.0001f);
    }
}
