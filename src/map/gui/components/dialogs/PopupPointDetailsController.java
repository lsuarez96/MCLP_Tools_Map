package map.gui.components.dialogs;

import com.jfoenix.controls.JFXButton;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import map.gis.LocationPointData;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PopupPointDetailsController implements Initializable {

    @FXML
    private Label titleLb;
    @FXML
    HBox titleContainerHb;
    @FXML
    private Label xTextLb;
    @FXML
    private Label yTextLb;
    @FXML
    private VBox dataContainer;
    private SimpleBooleanProperty moreDetails;
    private JFXButton moreDetailsBtn;
    private LocationPointData pointData;
    private static Popup popupStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setMoreDetails(false);
        this.moreDetails.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                moreDetailsBtn = new JFXButton("Mas detalles...");
                dataContainer.getChildren().addAll(moreDetailsBtn);
            } else {
                dataContainer.getChildren().remove(moreDetailsBtn);
            }
        });
    }

    public VBox getDataContainer() {
        return dataContainer;
    }

    public LocationPointData getPointData() {
        return pointData;
    }

    public void setPointData(LocationPointData pointData) {
        this.pointData = pointData;
        if (pointData != null) {
            dataContainer.getChildren().remove(moreDetailsBtn);
            titleContainerHb.getChildren().add(pointData.getRepresentationImage());
            if (pointData.getTitle().isEmpty()) {
                titleContainerHb.getChildren().remove(titleLb);
            } else {
                titleLb.textProperty().bind(pointData.titleProperty());
            }

            xTextLb.textProperty().bind(pointData.xProperty().asString());
            yTextLb.textProperty().bind(pointData.yProperty().asString());
            if (!pointData.getPlace().isEmpty()) {
                Label placeLb = new Label(pointData.getPlace());
                dataContainer.getChildren().add(placeLb);
            }
            if (!pointData.getActiveParameterName().isEmpty()) {
                Label activeParameterLb = new Label(pointData.getActiveParameterName() + ":");
                Label activeParameterValue = new Label(String.valueOf(pointData.getActiveParameterValue()));

                HBox hBox = new HBox(5, activeParameterLb, activeParameterValue);
                dataContainer.getChildren().add(hBox);
            }
            if (moreDetailsBtn != null && moreDetails.get()) {
                moreDetailsBtn = new JFXButton("Mas detalles...");
                dataContainer.getChildren().addAll(moreDetailsBtn);
            } else if (moreDetails.get()) {
                moreDetailsBtn = new JFXButton("Mas detalles...");
                dataContainer.getChildren().addAll(moreDetailsBtn);
            }
        }
    }

    public JFXButton getMoreDetailsBtn() {
        return moreDetailsBtn;
    }

    public void setMoreDetailsBtn(JFXButton moreDetailsBtn) {
        this.moreDetailsBtn = moreDetailsBtn;
    }

    public boolean isMoreDetails() {
        return moreDetails.get();
    }

    public SimpleBooleanProperty moreDetailsProperty() {
        return moreDetails;
    }

    public void setMoreDetails(boolean moreDetails) {
        if(this.moreDetails==null){
            this.moreDetails=new SimpleBooleanProperty();
        }
        this.moreDetails.set(moreDetails);

        if (moreDetails) {
            moreDetailsBtn = new JFXButton("Mas detalles...");
            dataContainer.getChildren().addAll(moreDetailsBtn);
        } else {
            dataContainer.getChildren().remove(moreDetailsBtn);
        }
    }

    public static FXMLLoader loadPointDataDetails() {
        return new FXMLLoader(PopupPointDetailsController.class.getResource("PopupPointDetails.fxml"));

    }


}
