package map.gui.components;

import de.javagl.svggraphics.SvgGraphics;
import de.javagl.svggraphics.SvgGraphicsWriter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import map.gis.GeographicDataInterface;
import map.gis.LocationPointData;
import map.gui.components.dialogs.*;
import map.utils.Network;
import mclp_tools.config.CfgMngController;
import mclp_tools.config.elements.maps.MapCfgData;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
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
import org.geotools.styling.*;
import org.geotools.swing.JMapPane;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.ContrastMethod;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.geotools.swt.styling.SimpleStyleConfigurator.sf;

public class MapPane extends JMapPane {
    private DoubleProperty widthProperty;
    private DoubleProperty heightProperty;
    private boolean repaint;
    private double baseDrageX;
    private double baseDrageY;
    private static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    private AffineTransform screenToWorld;
    private GeographicDataInterface geographicDataInterface;
    private static final String WMS_SPACIAL_REF_SYS = "EPSG:3005";
    private List<File> shapes;
    private MapCfgData mapCfgData;
    private ScheduledService<Boolean> paintingService;
    private Color background;
    private Color outline;
    private Color fill;
    public static double x, y;
    private boolean lockZoom = false;
    private long startZoom = 0;

    public MapPane() {
        super();
        background = Color.WHITE;
        outline = Color.BLACK;
        fill = new Color(0f, 0f, 0f, 0f);
        init();
    }

    public MapPane(Color background, Color outline, Color fill) {
        super();
        this.background = background;
        this.outline = outline;
        this.fill = fill;
        init();
    }

    private void init() {
        mapCfgData = CfgMngController.getInstance().getCurrentProfile().getMapCfgData();
        repaint = true;
        widthProperty = new SimpleDoubleProperty(this, "width");
        widthProperty.set(this.getWidth());
        heightProperty = new SimpleDoubleProperty(this, "height");
        heightProperty.set(this.getHeight());
        widthProperty.addListener(sizeChangedListener());
        heightProperty.addListener(sizeChangedListener());
        initMap();
        initPaintingService();
    }

    private InvalidationListener sizeChangedListener() {
        return observable -> {
            try {
                int width = widthProperty.intValue();
                int height = heightProperty.intValue();
                if (width > 0 && height > 0) {
                    setSize(width, height);
                    getMapContent().getViewport()
                                   .setScreenArea(new Rectangle(width, height));
                    this.setDisplayArea(mapContent.getMaxBounds());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            repaint = true;
        };
    }

    private void setDisplayArea(ReferencedEnvelope maxBounds) {
        mapContent.getViewport().setBounds(maxBounds);
    }

    private void initMap() {
        MapContent mapContent = new MapContent();
        setMapContent(mapContent);
        setBackground(background);
        initMapEvents();
    }

    private void initMapEvents() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() > 1) {
                    mapContent.getViewport()
                              .setScreenArea(new Rectangle((int) widthProperty().get(), (int) heightProperty().get()));
                    setDisplayArea(mapContent.getMaxBounds());
                    //drawData = true;
                } else {
                    baseDrageX = e.getX();
                    baseDrageY = e.getY();
                }
                e.consume();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent event) {
                baseDrageX = event.getX();
                baseDrageY = event.getY();
                event.consume();
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                double difX = evt.getX() - baseDrageX;
                double difY = evt.getY() - baseDrageY;
                baseDrageX = evt.getX();
                baseDrageY = evt.getY();
                DirectPosition2D newPos = new DirectPosition2D(difX, difY);
                DirectPosition2D result = new DirectPosition2D();
                mapContent.getViewport().getScreenToWorld().transform(newPos, result);
                ReferencedEnvelope envelope = new ReferencedEnvelope(mapContent.getViewport().getBounds());
                envelope.translate(envelope.getMinimum(0) - result.x, envelope.getMaximum(1) - result.y);
                setDisplayArea(envelope);
                //drawData = true;
                evt.consume();
            }
        });


        this.addMouseWheelListener(evt -> {
            ReferencedEnvelope envelope = mapContent.getViewport().getBounds();
            double percent = evt.getPreciseWheelRotation() / widthProperty().get();
            double width = envelope.getWidth();
            double height = envelope.getHeight();
            double deltaW = width * percent;
            double deltaH = height * percent;
            envelope.expandBy(deltaW, deltaH);
            setDisplayArea(envelope);
            // drawData = true;
            evt.consume();
        });


    }


    public void loadShapeFiles(final Stage initStage) {
        List<Layer> currentLayers = new ArrayList<>(mapContent.layers());
        mapContent.layers().clear();
        MapContent mapContent = new MapContent();
        List<Layer> layers = createLocalFileLayersList(outline, fill);
        List<Layer> layerSelection = ShapeFileSelectionDialogController
                .showShapeLayerSelectionDialog(layers, currentLayers, initStage);
        for (Layer layer : layerSelection) {
            mapContent.addLayer(layer);
        }
        setMapContent(mapContent);
        if (!currentLayers.isEmpty()) {
            repaint = true;
        }
    }

    public void loadImage() {
        if (mapCfgData.isHasMapImage()) {
//            List<Layer> layers = mapContent.layers();
//            for (Layer l : layers) {
//                mapContent.removeLayer(l);
//            }
            AbstractGridFormat gridFormat = new WorldImageFormat();
            File mapImage = mapCfgData.getMapImagePath();
            GridCoverage2DReader reader = gridFormat.getReader(mapImage);
            Style style = createRGBStyle(reader);
            Layer layer = new GridReaderLayer(reader, style);
            layer.setTitle("Map image layer");
            mapContent.addLayer(layer);
            repaint = true;
        }
    }

    static Style createRGBStyle(GridCoverage2DReader reader) {
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
            List<Layer> mapLayers = mapContent.layers();
            for (Layer l : mapLayers) {
                mapContent.removeLayer(l);
            }
            Network.setupHostConnection();
            MapContent mapContent = new MapContent();
            try {
                URL url = new URL(mapCfgData.getWmsAddress());
                WebMapServer webMapServer = new WebMapServer(url);
                ReferencingFactoryFinder.setAuthorityOrdering("Web", "Cartesian");
                List<org.geotools.data.ows.Layer> layers = WMSLayerSelectionDialogController
                        .showWmsLayerSelectionDialog(webMapServer);
                List<WMSLayer> wmsLayers = new ArrayList<>();
                if (!layers.isEmpty()) {
                    for (org.geotools.data.ows.Layer layer : layers) {
                        WMSLayer displayLayer = new WMSLayer(webMapServer, layer);
                        wmsLayers.add(displayLayer);
                    }
                }
                for (WMSLayer layer : wmsLayers) {
                    mapContent.addLayer(layer);
                }
                setMapContent(mapContent);
                repaint = true;
            } catch (ServiceException | IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void loadMap(final Stage initialStage) {
        List<Layer> currentLayers = new ArrayList<>(mapContent.layers());
        mapContent.layers().clear();
        MapContent mapContent = new MapContent();
        List<Layer> layers = createLocalFileLayersList(outline, fill);
        MapSelectionResult msr = ShapeWMSSelectionController.showMapSelectionView(initialStage, layers, currentLayers);
        if (msr.localFileLayers != null && !msr.localFileLayers.isEmpty()) {
            List<Layer> layerSelection = msr.localFileLayers;
            for (Layer layer : layerSelection) {
                mapContent.addLayer(layer);
            }
            setMapContent(mapContent);
            if (!currentLayers.isEmpty()) {
                repaint = true;
            }
        } else if (msr.wmsLayers != null && !msr.wmsLayers.isEmpty()) {
            List<Layer> mapLayers = mapContent.layers();
            for (Layer l : mapLayers) {
                mapContent.removeLayer(l);
            }
            List<org.geotools.data.ows.Layer> layersWms = msr.wmsLayers;
            List<WMSLayer> wmsLayers = new ArrayList<>();
            for (org.geotools.data.ows.Layer layer : layersWms) {
                WMSLayer displayLayer = new WMSLayer(msr.webMapServer, layer);
                wmsLayers.add(displayLayer);
            }

            for (WMSLayer layer : wmsLayers) {
                mapContent.addLayer(layer);
            }
            setMapContent(mapContent);
            repaint = true;
        }
    }

    public void loadDefaultMap() {
        List<Layer> currentLayers = new ArrayList<>(mapContent.layers());
        mapContent.layers().clear();
        MapContent mapContent = new MapContent();
        try {
            File file = CfgMngController.getInstance().getCurrentProfile().getMapCfgData().getShapeFilePath();
            if (file != null && file.exists() && file.getName().endsWith("shp")) {
                FileDataStore store = FileDataStoreFinder.getDataStore(file);
                SimpleFeatureSource featureSource = store.getFeatureSource();
                dataStores.add(store);
                Style style = SLD.createPolygonStyle(outline, fill, 1.0f);
                FeatureLayer layer = new FeatureLayer(featureSource, style);

                layer.setTitle(file.getName().replace(".shp", ""));
                mapContent.addLayer(layer);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error cargando el mapa por defecto");
                alert.setContentText(
                        "La definici\u00F3n del mapa no pudo ser encontrada en la ruta especificada en la "
                        + "configuración: "
                        + CfgMngController.getInstance().getCurrentProfile().getMapCfgData().getShapeFilePath()
                                          .toString());
                alert.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        setMapContent(mapContent);
        if (!currentLayers.isEmpty()) {
            repaint = true;
        }
    }

    public void changeMapColors(Color outline, Color fill) {
        this.outline = outline;
        this.fill = fill;
        repaint = true;
    }

    List<FileDataStore> dataStores = new ArrayList<>();
    Feature feature;

    private List<Layer> createLocalFileLayersList(Color outline, Color fill) {
        if (shapes == null || shapes.isEmpty()) {
            shapes = getShapeFiles();
        }
        List<Layer> layers = new ArrayList<>();
        for (File file : shapes) {
            try {
                FileDataStore store = FileDataStoreFinder.getDataStore(
                        file);
                SimpleFeatureSource featureSource = store.getFeatureSource();
                dataStores.add(store);
                Style style = SLD.createPolygonStyle(outline, fill, 1.0f);
                FeatureLayer layer = new FeatureLayer(featureSource, style);

                layer.setTitle(file.getName().replace(".shp", ""));
                layers.add(layer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return layers;
    }

    private List<File> getShapeFiles() {
        File baseFolder = mapCfgData.getShapeFilesDirectory();
        if (baseFolder.isFile()) {
            baseFolder = baseFolder.getParentFile();
        }
        File[] shapeFiles = baseFolder.listFiles(pathname -> {
            if (pathname.isFile()) {
                return pathname.getName()
                               .endsWith("shp");//startsWith("shp", pathname.getName().length() - 3);
            }
            return false;
        });
        return shapeFiles != null ? Arrays.asList(shapeFiles) : new ArrayList<>();
    }


    public DoubleProperty widthProperty() {
        return widthProperty;
    }


    public DoubleProperty heightProperty() {
        return heightProperty;
    }

    private void initPaintingService() {
        paintingService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        if (repaint) {
                            repaint = false;
                            repaint(new Rectangle(widthProperty.intValue(), heightProperty.intValue()));
                        }
                        if ((System.currentTimeMillis() - startZoom) >= 100) {
                            lockZoom = false;
                        }
                        return true;
                    }
                };
            }
        };
        paintingService.setPeriod(Duration.millis(1000 / 50));
        paintingService.start();
    }


    public void setGeographicDataInterface(GeographicDataInterface geographicDataInterface) {
        this.geographicDataInterface = geographicDataInterface;
        repaint = true;

    }

    public GeographicDataInterface getGeographicDataInterface() {
        return geographicDataInterface;
    }

    public void cancelRepaintTask() {
        super.paneTaskExecutor.shutdownNow();
        paintingService.cancel();
        mapContent.dispose();

    }


    public void setRepaint(boolean repaint) {
        this.repaint = repaint;
    }

    public EventHandler<ScrollEvent> scrollToZoomEventHandler() {
        return evt -> {
            try {
                if (!lockZoom) {
                    ReferencedEnvelope envelope = getMapContent().getViewport().getBounds();
                    double percent = evt.getDeltaY() * 2 / widthProperty().get();
                    double width = envelope.getWidth();
                    double height = envelope.getHeight();
                    double deltaW = width * percent;
                    double deltaH = height * percent;
                    envelope.expandBy(deltaW, deltaH);
                    super.setDisplayArea(envelope);
                    startZoom = System.currentTimeMillis();
                    lockZoom = true;
                    evt.consume();
                }
            } catch (Exception e) {
            }
            evt.consume();
        };
    }

    private ContextMenu contextMenu;
    private boolean mouseWasDraggedBefore = false;

    public EventHandler<MouseEvent> mouseClickedEventHandler() {
        return event -> {
            if (event.getClickCount() > 1) {
                getMapContent().getViewport()
                               .setScreenArea(
                                       new Rectangle((int) widthProperty().get(), (int) heightProperty().get
                                               ()));
                super.setDisplayArea(getMapContent().getMaxBounds());
                if (contextMenu != null) {
                    contextMenu.hide();
                }
            } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = (Node) event.getSource();
                if (contextMenu == null) {
                    contextMenu = new ContextMenu();
                    MenuItem reload = new MenuItem("Recargar");
                    MenuItem saveImage = new MenuItem("Guardar imagen");
                    saveImage.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.IMAGE));
                    reload.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.REFRESH));
                    reload.setOnAction(evt -> {
                        try {
                            this.reset();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    saveImage.setOnAction(evt -> {
//                        Platform.runLater(() -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Guardar imagen");
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("Imagenes vectoriales", "*.svg"));
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("Imagenes png", "*.png"));
                        File file = fileChooser.showSaveDialog(null);

                        if (file != null) {
                            if (file.getName().endsWith("svg")) {
                                DOMImplementation dom = GenericDOMImplementation.getDOMImplementation();
                                // Create an instance of org.w3c.dom.Document.
                                String svgNS = "http://www.w3.org/2000/svg";
                                Document document = dom.createDocument(svgNS, "svg", null);
                                SVGGeneratorContext ctx1 = SVGGeneratorContext.createDefault(document);
                                // Create an instance of the SVG Generator.
                                SVGGraphics2D svgGenerator = new SVGGraphics2D(ctx1, true);
                                getRenderer().setJava2DHints(new RenderingHints(
                                        RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON));
                                paint(svgGenerator);
                                drawData(svgGenerator);
                                try {
                                    OutputStream out = new FileOutputStream(file);

                                    OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                                    svgGenerator.stream(osw);
                                    out.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else if (file.getName().endsWith("png")) {
                                    BufferedImage bi = new BufferedImage(this.getSize().width, this.getSize().height,
                                                                         BufferedImage.TYPE_INT_ARGB);
                                    Graphics2D g = bi.createGraphics();
                                    this.paint(g);  //this == JComponent
                                    drawData(g);
                                    g.dispose();
                                    try {
                                        ImageIO.write(bi, "png", file);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
//                                SwingNode node1 = (SwingNode) node;
//                                WritableImage writableImage =
//                                        new WritableImage((int) node1.getContent().getWidth(), (int) node1.getContent().getHeight());
//                                node1.snapshot(new SnapshotParameters(), writableImage);
//                                try {
//                                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
//                                    System.out.println("snapshot saved: " + file.getAbsolutePath());
//                                } catch (IOException ex) {
////                                    Logger.getLogger(JavaFXSnapshot.class.getName()).log(Level.SEVERE, null, ex);
//                                }
                            }
                        }
//                        });


                    });
                    contextMenu.getItems().add(reload);
                    contextMenu.getItems().add(saveImage);

                } else {
                    contextMenu.hide();
                }
                contextMenu.setX(event.getSceneX());
                contextMenu.setY(event.getSceneY());
                contextMenu.setAutoHide(true);
                contextMenu.show(node, event.getScreenX(), event.getScreenY());
            } else if (!mouseWasDraggedBefore) {
                if (contextMenu != null) {
                    contextMenu.hide();
                }
                double xScene = event.getX();
                double yScene = event.getY();
                screenToWorld = mapContent.getViewport().getScreenToWorld();
                Point2D point2D = new DirectPosition2D();
                point2D.setLocation(xScene, yScene);
                Point2D posInMap = new DirectPosition2D();
                screenToWorld.transform(point2D, posInMap);
                x = ((DirectPosition2D) posInMap).x;
                y = ((DirectPosition2D) posInMap).y;
                if (geographicDataInterface != null) {
                    geographicDataInterface
                            .showPopupPointDetails(event.getScreenX(), event.getScreenY(),
                                                   ((DirectPosition2D) posInMap).x,
                                                   ((DirectPosition2D) posInMap).y,
                                                   (Node) event.getSource());
                } else {
                    showPointDetails(event.getScreenX(), event.getScreenY(), ((DirectPosition2D) posInMap).x,
                                     ((DirectPosition2D) posInMap).y,
                                     (Node) event.getSource());
                }

            }

            mouseWasDraggedBefore = false;
            event.consume();
        };
    }

    private Popup locationPointDataPopup = null;

    private void showPointDetails(double xScene, double yScene, double xMap, double yMap, Node owner) {
        System.out.println("Screen X " + xScene + " Y " + yScene);
        try {
            PopupPointDetailsController popupPointDetailsController;
            FXMLLoader loader = PopupPointDetailsController.loadPointDataDetails();
            AnchorPane content = loader.load();
            popupPointDetailsController = loader.getController();
            popupPointDetailsController.setPointData(new LocationPointData(xMap, yMap));

            if (locationPointDataPopup != null && locationPointDataPopup.isShowing()) {
                locationPointDataPopup.hide();
            } else {
                locationPointDataPopup = new Popup();
                locationPointDataPopup.getContent().add(content);
                locationPointDataPopup.show(owner, xScene, yScene);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public EventHandler<MouseEvent> mousePressedEventHandler() {
        return event -> {
            baseDrageX = event.getSceneX();
            baseDrageY = event.getSceneY();
            event.consume();
        };
    }

    public EventHandler<MouseEvent> mouseDraggedEventHandler() {
        return evt -> {
            int difX = (int) (evt.getSceneX() - baseDrageX);
            int difY = (int) (evt.getSceneY() - baseDrageY);
            baseDrageX = evt.getSceneX();
            baseDrageY = evt.getSceneY();
            mouseWasDraggedBefore = true;
            try {
                moveImage(difX, difY);
            } catch (Exception e) {

            }
            evt.consume();
        };
    }

    /**
     * Devuelve el menu contextual del mapa que se muestra al presionar el boton derecho del mouse.
     * La opcion por defecto es la de recargar el mapa. Pero es posible agregar mas opciones.
     *
     * @return
     */
    public ContextMenu getContextMenu() {
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
        }
        return contextMenu;
    }

    private void drawData(Graphics2D graphics2D) {
        if (geographicDataInterface != null && !mapContent.layers().isEmpty()) {
            geographicDataInterface.paintData(graphics2D, mapContent, this);
        } else {
            repaint = true;
        }
    }

    private void drawData() {
        if (geographicDataInterface != null && !mapContent.layers().isEmpty()) {
            Graphics2D graphics2D = (Graphics2D) getGraphics();
            geographicDataInterface.paintData(graphics2D, mapContent, this);
        } else {
            repaint = true;
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        drawData();
    }

    @Override
    public void repaint(Rectangle rectangle) {
        try {
            super.repaint(rectangle);
            drawData();
        } catch (Exception e) {

        }
    }

    @Override
    public void repaint() {
        try {
            super.repaint();
            drawData();
        } catch (Exception e) {

        }
    }
}
