package map.gui.components.dialogs;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import map.utils.Network;
import mclp_tools.config.CfgMngController;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.ReferencingFactoryFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class WMSLayerSelectionDialogController implements Initializable {

    @FXML
    private JFXListView<String> layersLv;
    @FXML
    private JFXButton selectLayerBt;

    private List<Layer> layers;
    static WebMapServer webMapServer;
    static List<Layer> layerSelection;
    static Stage stage;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.setupHostConnection();
        URL url = null;
        try {
            url = new URL(CfgMngController.getInstance().getCurrentProfile().getMapCfgData().getWmsAddress());
            webMapServer = new WebMapServer(url);
            ReferencingFactoryFinder.setAuthorityOrdering("Web", "Cartesian");
            ObservableList<String> layersNames = FXCollections.observableArrayList();
            layers = webMapServer.getCapabilities().getLayerList();
            for (Layer layer : layers) {
                layersNames.add(layer.getName());
            }
            layersLv.setItems(layersNames);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        selectLayerBt.setOnAction(event -> {
            stage.close();
        });
        layerSelection = new ArrayList<>();
        layersLv.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            List<String> selectedItems = layersLv.getSelectionModel().getSelectedItems();
            layerSelection.clear();
            for (String item : selectedItems) {
                for (Layer layer : layers) {
                    if (item.equals(layer.getName())) {
                        layerSelection.add(layer);
                    }
                }
            }
        });
    }

    public static List<Layer> showWmsLayerSelectionDialog(WebMapServer webMapServer) {
        WMSLayerSelectionDialogController.webMapServer = webMapServer;
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(
                WMSLayerSelectionDialogController.class.getResource("WMSLayerSelectionDialog.fxml"));
        try {
            AnchorPane anchorPane = loader.load();
            WMSLayerSelectionDialogController controller = loader.getController();
            Scene scene = new Scene(anchorPane);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            return layerSelection;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


}
