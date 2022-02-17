package map.gui.components.dialogs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.CheckListView;
import org.geotools.map.Layer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ShapeFileSelectionDialogController implements Initializable {


    @FXML
    private CheckListView<Layer> layersListView;

    static ObservableList<Layer> availableLayers;
    static List<Layer> representedLayers;
    static List<Layer> selectedLayer;
    static Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        layersListView.setItems(availableLayers);
        layersListView.setCellFactory(lv -> new CheckBoxListCell<Layer>(layersListView::getItemBooleanProperty) {
            @Override
            public void updateItem(Layer item, boolean empty) {
                super.updateItem(item, empty);
                String text = "";
                if (item != null) {
                    text = item.getTitle();
                }
                setText(text);
            }
        });
        for (int i = 0; i < availableLayers.size(); i++) {
            for (int j = 0; j < representedLayers.size(); j++) {
                if (availableLayers.get(i).getTitle().equals(representedLayers.get(j).getTitle())) {
                    layersListView.getCheckModel().check(i);
                }
            }
        }

    }

    @FXML
    void reorderLayerUp() {
        Layer selected = layersListView.getSelectionModel().getSelectedItem();
        int selectedIndex = layersListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            int newPos = selectedIndex - 1;
            Layer inPos = layersListView.getItems().get(newPos);
            layersListView.getItems().set(newPos, selected);
            layersListView.getItems().set(selectedIndex, inPos);
        }
    }

    @FXML
    void reorderLayerDown() {
        Layer selected = layersListView.getSelectionModel().getSelectedItem();
        int selectedIndex = layersListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < layersListView.getItems().size() - 1) {
            int newPos = selectedIndex + 1;
            Layer inPos = layersListView.getItems().get(newPos);
            layersListView.getItems().set(newPos, selected);
            layersListView.getItems().set(selectedIndex, inPos);
        }
    }

    @FXML
    void finishSelection() {
        List<Integer> checkedIndexes = layersListView.getCheckModel().getCheckedIndices();
        selectedLayer=new ArrayList<>();
        for (Integer idx : checkedIndexes) {
            selectedLayer.add(layersListView.getItems().get(idx));
        }
        stage.close();
    }

    public static List<Layer> showShapeLayerSelectionDialog(List<Layer> layers, List<Layer> represented,
                                                            Stage initStage) {
        selectedLayer = new ArrayList<>();
        representedLayers = represented;
        availableLayers = FXCollections.observableArrayList(layers);
        try {

            Stage stage = new Stage();
            if (initStage != null) {
                stage.initOwner(initStage);
            }
            FXMLLoader loader = new FXMLLoader(
                    ShapeFileSelectionDialogController.class.getResource("ShapeFileSelectionDialog.fxml"));
            AnchorPane root = loader.load();
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Seleccione las capas a aplicar");
            ShapeFileSelectionDialogController.stage = stage;
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return selectedLayer;
    }
}
