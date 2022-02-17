package map.gui.components.dialogs;

import com.jfoenix.controls.JFXTabPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mclp_tools.config.CfgMngController;
import org.geotools.data.wms.WebMapServer;
import org.geotools.map.Layer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ShapeWMSSelectionController implements Initializable {

    @FXML
    private JFXTabPane layerTypeTp;
    @FXML
    private Tab localMapTab;
    @FXML
    private Tab wmsMapTab;

    @FXML
    private AnchorPane localMapAp;
    @FXML
    private AnchorPane wmsMapAp;

    private Initializable mapSelectionController;
    //  static WebMapServer webMapServer;
    static ObservableList<Layer> availableLayers;
    static List<Layer> representedLayers;
    static Stage stage;
    static MapTypeSelection mapTypeSelection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!CfgMngController.getInstance().getCurrentProfile().getMapCfgData().isHasWms()) {
            layerTypeTp.getTabs().remove(wmsMapTab);
        }
        layerTypeTp.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == localMapTab) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ShapeFileSelectionDialog.fxml"));
                try {
                    ShapeFileSelectionDialogController.stage = stage;
                    ShapeFileSelectionDialogController.availableLayers = availableLayers;
                    ShapeFileSelectionDialogController.representedLayers = representedLayers;
                    AnchorPane anchorPane = loader.load();
                    mapSelectionController = loader.getController();
                    localMapAp.getChildren().add(anchorPane);
                    AnchorPane.setRightAnchor(anchorPane, 0d);
                    AnchorPane.setLeftAnchor(anchorPane, 0d);
                    AnchorPane.setTopAnchor(anchorPane, 0d);
                    AnchorPane.setBottomAnchor(anchorPane, 0d);
                    mapTypeSelection = MapTypeSelection.LOCAL;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (newValue == wmsMapTab) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("WMSLayerSelectionDialog.fxml"));
                try {
                    WMSLayerSelectionDialogController.stage = stage;
                    // WMSLayerSelectionDialogController.webMapServer = webMapServer;
                    AnchorPane anchorPane = loader.load();
                    mapSelectionController = loader.getController();
                    wmsMapAp.getChildren().add(anchorPane);
                    AnchorPane.setRightAnchor(anchorPane, 0d);
                    AnchorPane.setLeftAnchor(anchorPane, 0d);
                    AnchorPane.setTopAnchor(anchorPane, 0d);
                    AnchorPane.setBottomAnchor(anchorPane, 0d);
                    mapTypeSelection = MapTypeSelection.WMS;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        layerTypeTp.getSelectionModel().clearSelection();
        layerTypeTp.getSelectionModel().select(localMapTab);
    }

    public static MapSelectionResult showMapSelectionView(Stage homeStage, List<Layer> layers,
                                                          List<Layer> currentLayers) {
        stage = new Stage();
        representedLayers = currentLayers;
        availableLayers = FXCollections.observableArrayList(layers);
        //ShapeWMSSelectionController.webMapServer = webMapServer;
        FXMLLoader loader = new FXMLLoader(ShapeWMSSelectionController.class.getResource("Shape_WMSSelection.fxml"));
        try {
            AnchorPane ap = loader.load();
            Scene scene = new Scene(ap);
            if (homeStage != null) {
                stage.initOwner(homeStage);
            }
            stage.setScene(scene);
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            MapSelectionResult mapSelectionResult = new MapSelectionResult();
            if (mapTypeSelection.equals(MapTypeSelection.LOCAL)) {
                mapSelectionResult.localFileLayers = ShapeFileSelectionDialogController.selectedLayer;
            } else {
                mapSelectionResult.wmsLayers = WMSLayerSelectionDialogController.layerSelection;
                mapSelectionResult.webMapServer = WMSLayerSelectionDialogController.webMapServer;
            }
            return mapSelectionResult;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new MapSelectionResult();
    }

    private enum MapTypeSelection {
        LOCAL,
        WMS
    }

}
