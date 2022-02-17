package map.gui;

import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import map.gis.GeographicDataInterface;
import map.gui.components.MapPane;
import org.geotools.map.MapContent;

import java.awt.*;
import java.net.URL;
import java.util.ResourceBundle;

public class MapViewController implements Initializable {
    @FXML
    private AnchorPane mapContainer;
    private MapPane jMapPane;
    private static Stage parentStage;
    private SwingNode swingNode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initMapPane();
        parentStage.onCloseRequestProperty().addListener(observable -> {
            jMapPane.cancelRepaintTask();
        });
    }

    private void initMapPane() {
        jMapPane = new MapPane();
        jMapPane.widthProperty().bind(mapContainer.widthProperty().subtract(15));
        jMapPane.heightProperty().bind(mapContainer.heightProperty().subtract(15));
        swingNode = new SwingNode();
        swingNode.setContent(jMapPane);
        setupMapControls();
        mapContainer.getChildren().add(swingNode);
        setupContainerLayout();
    }

    private void setupContainerLayout() {
        mapContainer
                .setStyle("-fx-border-width: 2px; -fx-border-color: black; -fx-border-radius: 10px;-fx-padding: 3px;");
        AnchorPane.setTopAnchor(swingNode, 0d);
        AnchorPane.setRightAnchor(swingNode, 0d);
        AnchorPane.setLeftAnchor(swingNode, 0d);
        AnchorPane.setBottomAnchor(swingNode, 0d);
    }

    public void setBackgroundColor(Color color) {
        jMapPane.setBackground(color);
    }

    public void setMapColours(Color outline, Color fill) {
        jMapPane.changeMapColors(outline, fill);
    }

    private void setupMapControls() {
        swingNode.setOnMouseClicked(jMapPane.mouseClickedEventHandler());

        swingNode.setOnMousePressed(jMapPane.mousePressedEventHandler());

        swingNode.setOnMouseDragged(jMapPane.mouseDraggedEventHandler());

        swingNode.setOnScroll(jMapPane.scrollToZoomEventHandler());

    }

    public void loadMapLocally() {
        jMapPane.loadShapeFiles(parentStage);
    }

    public void loadMapImage() {
        jMapPane.loadImage();
    }

    public void loadWms() {
        jMapPane.loadWms();
    }

    public void loadMap() {
        jMapPane.loadMap(parentStage);
    }

    public void loadDefaultMap() {
        jMapPane.loadDefaultMap();
    }

    public void setData(GeographicDataInterface geographicDataInterface) {
        jMapPane.setGeographicDataInterface(geographicDataInterface);
    }

    public GeographicDataInterface getGeographicDataInterface() {
        return jMapPane.getGeographicDataInterface();
    }


    public static void setParentStage(Stage parentStage) {
        MapViewController.parentStage = parentStage;
    }

    public MapContent getMapContent() {
        return jMapPane.getMapContent();
    }


}
