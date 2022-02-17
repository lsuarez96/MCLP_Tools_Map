package map.gui.components;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;
import map.gis.GeographicDataInterface;
import map.gui.components.dialogs.WMSLayerSelectionDialogController;
import mclp_tools.config.CfgMngController;
import mclp_tools.config.elements.maps.MapCfgData;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WebMapServer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.image.WorldImageFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.*;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.*;
import org.jfree.fx.FXGraphics2D;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.ContrastMethod;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static map.utils.Network.setupHostConnection;
import static org.geotools.swt.styling.SimpleStyleConfigurator.sf;

class MapCanvas extends Canvas {

    private MapContent map;
    private MapCfgData mapCfgData;
    private ScheduledService<Boolean> paintingService;
    private double baseDrageX;
    private double baseDrageY;
    private boolean repaint;
    private CoordinateReferenceSystem crs;
    private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    private GeographicDataInterface geographicDataInterface;
    private static final String INIT_SPACIAL_REF_SYS = "EPSG:3005";
    private List<File> shapes;

    public MapCanvas() {
        super();
        repaint = true;
        init();
    }

    public MapCanvas(double width, double height) {
        super(width, height);
        repaint = true;
        init();
    }

    private void init() {
        super.widthProperty().addListener(observable -> {
            repaint = true;
        });
        super.heightProperty().addListener(observable -> {
            repaint = true;
        });
        mapCfgData = CfgMngController.getInstance().getCurrentProfile().getMapCfgData();
        initMap();
        initPaintingService();
    }

    private void initMap() {
        map = new MapContent();
        loadShapeFiles();
        initMapEvents();
    }

    private void initMapEvents() {
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() > 1) {
                map.getViewport()
                   .setScreenArea(new Rectangle((int) widthProperty().get(), (int) heightProperty().get()));
                setDisplayArea(map.getMaxBounds());
            } else {
                baseDrageX = event.getScreenX();
                baseDrageY = event.getScreenY();
            }
            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            baseDrageX = event.getSceneX();
            baseDrageY = event.getSceneY();
            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, evt -> {
            double difX = evt.getSceneX() - baseDrageX;
            double difY = evt.getSceneY() - baseDrageY;
            baseDrageX = evt.getSceneX();
            baseDrageY = evt.getSceneY();
            DirectPosition2D newPos = new DirectPosition2D(difX, difY);
            DirectPosition2D result = new DirectPosition2D();
            map.getViewport().getScreenToWorld().transform(newPos, result);
            ReferencedEnvelope envelope = new ReferencedEnvelope(map.getViewport().getBounds());
            envelope.translate(envelope.getMinimum(0) - result.x, envelope.getMaximum(1) - result.y);
            setDisplayArea(envelope);
            evt.consume();
        });

        addEventHandler(ScrollEvent.SCROLL, evt -> {
            ReferencedEnvelope envelope = map.getViewport().getBounds();
            double percent = evt.getDeltaY() / widthProperty().get();
            double width = envelope.getWidth();
            double height = envelope.getHeight();
            double deltaW = width * percent;
            double deltaH = height * percent;
            envelope.expandBy(deltaW, deltaH);
            setDisplayArea(envelope);
            evt.consume();
        });
    }

    public void setDisplayArea(ReferencedEnvelope maxBounds) {
        map.getViewport().setBounds(maxBounds);
        repaint = true;
    }

    public void loadShapeFiles() {
        if (shapes == null || shapes.isEmpty()) {
            shapes = getShapeFiles();
        }
        map.layers().clear();
        for (File file : shapes) {
            addShapeFileLayer(file);
            //  break;
        }
        crs = map.getCoordinateReferenceSystem();
    }

    private List<File> getShapeFiles() {
        File baseFolder = mapCfgData.getShapeFilePath();
        File[] shapeFiles = baseFolder.listFiles(pathname -> {
            if (pathname.isFile()) {
                return pathname.getName().substring(pathname.getName().length() - 3, pathname.getName().length())
                               .equals("shp");
            }
            return false;
        });
        return shapeFiles != null ? Arrays.asList(shapeFiles) : new ArrayList<>();
    }

    private void addShapeFileLayer(File shapeFile) {
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(
                    shapeFile);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            float[] hsb = Color.RGBtoHSB(79, 79, 79, null);
            Color hsbColor = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);//dark gray
            Style style = SLD.createPolygonStyle(Color.WHITE, Color.BLACK, 1.0f);
            FeatureLayer layer = new FeatureLayer(featureSource, style);
            layer.setTitle("Shape file layer " + shapeFile.getName());
            map.addLayer(layer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadImage() {
        if (mapCfgData.isHasMapImage()) {
            AbstractGridFormat gridFormat = new WorldImageFormat();
            File mapImage = mapCfgData.getMapImagePath();
            GridCoverage2DReader reader = gridFormat.getReader(mapImage);
            Style style = createRGBStyle(reader);
            Layer layer = new GridReaderLayer(reader, style);
            layer.setTitle("Map image layer");
            map.addLayer(layer);
        }
    }

    private Style createRGBStyle(GridCoverage2DReader reader) {
        GridCoverage2D cov = null;
        try {
            cov = reader.read(null);
        } catch (IOException giveUp) {
            throw new RuntimeException(giveUp);
        }
        int numBands = cov.getNumSampleDimensions();
        if (numBands < 3) {
            return null;
        }
        String[] sampleDimensionNames = new String[numBands];
        for (int i = 0; i < numBands; i++) {
            GridSampleDimension dim = cov.getSampleDimension(i);
            sampleDimensionNames[i] = dim.getDescription().toString();
        }
        final int RED = 0, GREEN = 1, BLUE = 2;
        int[] channelNum = {-1, -1, -1};
        for (int i = 0; i < numBands; i++) {
            String name = sampleDimensionNames[i].toLowerCase();
            if (name.matches("red.*")) {
                channelNum[RED] = i + 1;
            } else if (name.matches("green.*")) {
                channelNum[GREEN] = i + 1;
            } else if (name.matches("blue.*")) {
                channelNum[BLUE] = i + 1;
            }
        }
        if (channelNum[RED] < 0 || channelNum[GREEN] < 0 || channelNum[BLUE] < 0) {
            channelNum[RED] = 1;
            channelNum[GREEN] = 2;
            channelNum[BLUE] = 3;
        }
        SelectedChannelType[] sct = new SelectedChannelType[cov.getNumSampleDimensions()];
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        for (int i = 0; i < 3; i++) {
            sct[i] = sf.createSelectedChannelType(String.valueOf(channelNum[i]), ce);
        }
        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct[RED], sct[GREEN], sct[BLUE]);
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }

    public void loadWms() {
        if (mapCfgData.isHasWms()) {
            setupHostConnection();
            try {
                URL url = new URL(mapCfgData.getWmsAddress());
                WebMapServer webMapServer = new WebMapServer(url);
                ReferencingFactoryFinder.setAuthorityOrdering("Web", "Cartesian");
                List<org.geotools.data.ows.Layer> layers = WMSLayerSelectionDialogController
                        .showWmsLayerSelectionDialog(webMapServer);
                List<WMSLayer> wmsLayers = new ArrayList<>();
                if (!layers.isEmpty()) {
                    map.layers().clear();
                    for (org.geotools.data.ows.Layer layer : layers) {
                        WMSLayer displayLayer = new WMSLayer(webMapServer, layer);
                        wmsLayers.add(displayLayer);
                    }
                }
                for (WMSLayer layer : wmsLayers) {
                    map.addLayer(layer);
                }

            } catch (ServiceException | IOException e) {
                e.printStackTrace();
            }

        }
    }


    private static final double PAINT_FRECUENCY = 50;

    private void initPaintingService() {
        paintingService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        Platform.runLater(MapCanvas.this::draw);
                        return true;
                    }
                };
            }
        };
        paintingService.setPeriod(Duration.millis(10000 / PAINT_FRECUENCY));
        paintingService.start();
    }

    private void draw() {
        try {
            if (repaint) {
                repaint = false;
                GraphicsContext graphicsContext = getGraphicsContext2D();
                StreamingRenderer drawer = new StreamingRenderer();
                Rectangle rectangle = new Rectangle((int) widthProperty().get(),
                                                    (int) heightProperty().get());
                map.getViewport().setScreenArea(rectangle);
                crs = map.getViewport().getBounds().getCoordinateReferenceSystem();
                drawer.setMapContent(map);
                FXGraphics2D graphics2D = new FXGraphics2D(graphicsContext);
                float[] hsb = Color.RGBtoHSB(79, 79, 79, null);
                Color hsbColor = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);//dark gray
                graphics2D.setBackground(hsbColor);
                graphics2D.clearRect(0, 0, (int) widthProperty().get(), (int) heightProperty().get());
                drawer.paint(graphics2D, rectangle, map.getViewport().getBounds());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelRepaintTask() {
        paintingService.cancel();
        map.dispose();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public double prefHeight(double width) {
        return heightProperty().get();
    }

    @Override
    public double prefWidth(double height) {
        return widthProperty().get();
    }

    @Override
    public void resize(double width, double height) {
        widthProperty().setValue(width);
        heightProperty().setValue(height);
        //repaint = true;
    }

    @Override
    public void resizeRelocate(double x, double y, double width, double height) {
        resize(width, height);
        relocate(x, y);
    }
}
