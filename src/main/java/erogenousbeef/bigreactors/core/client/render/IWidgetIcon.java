package erogenousbeef.bigreactors.core.client.render;

import javax.annotation.Nullable;

public interface IWidgetIcon {

    int getX();

    int getY();

    int getWidth();

    int getHeight();

    @Nullable
    IWidgetIcon getOverlay();

    IWidgetMap getMap();
}
