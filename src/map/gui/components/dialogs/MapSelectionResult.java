package map.gui.components.dialogs;

import org.geotools.data.wms.WebMapServer;
import org.geotools.map.Layer;

import java.util.ArrayList;
import java.util.List;

public class MapSelectionResult {
    public List<Layer> localFileLayers;
    public List<org.geotools.data.ows.Layer> wmsLayers;
    public WebMapServer webMapServer;

    public MapSelectionResult() {
        localFileLayers = new ArrayList<>();
        wmsLayers = new ArrayList<>();
    }
}
