package map.gui.components;

import map.gis.GeographicDataInterface;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DirectLayer;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;

import java.awt.*;

public class CustomImageLayer extends DirectLayer {
    private MapViewport viewport;
    private MapContent mapContent;
    private GeographicDataInterface geographicDataInterface;

    public CustomImageLayer(GeographicDataInterface geographicDataInterface, MapContent mapContent) {
        super();
        this.geographicDataInterface = geographicDataInterface;
        this.mapContent = mapContent;
        viewport = mapContent.getViewport();
    }

    @Override
    public void draw(Graphics2D graphics2D, MapContent mapContent, MapViewport mapViewport) {
        if (mapViewport != null) {
            this.viewport = mapViewport;
        }
        if (mapContent != null) {
            this.mapContent = mapContent;
        }
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        geographicDataInterface.paintData(graphics2D, this.mapContent, null);
    }


    @Override
    public ReferencedEnvelope getBounds() {
        return viewport.getBounds();
    }
}
