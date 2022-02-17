package map.gis;

import javafx.animation.PauseTransition;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Popup;
import javafx.util.Duration;
import map.gui.components.dialogs.PopupPointDetailsController;
import org.geotools.map.MapContent;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public abstract class GeographicDataInterface implements Cloneable {
    protected Popup locationPointDataPopup;
    protected PopupPointDetailsController popupPointDetailsController;

    public abstract void paintData(Graphics2D graphics, MapContent mapContent, Container c);

    public void showPopupPointDetails(double xScene, double yScene, double xMap, double yMap, Node owner) {
        try {
            LocationPointData pointData = findLocationPointData(xMap, yMap);
            FXMLLoader loader = PopupPointDetailsController.loadPointDataDetails();
            AnchorPane content = loader.load();
            popupPointDetailsController = loader.getController();
            popupPointDetailsController.setPointData(pointData == null ? new LocationPointData(xMap, yMap) : pointData);
            if (locationPointDataPopup != null && locationPointDataPopup.isShowing()) {
                locationPointDataPopup.hide();
            } else {
                locationPointDataPopup = new Popup();
                locationPointDataPopup.getContent().add(content);
                locationPointDataPopup.show(owner, xScene, yScene);
                locationPointDataPopup.setAutoHide(true);
                PauseTransition pauseTransition = new PauseTransition(Duration.seconds(2));
                pauseTransition.setOnFinished(e -> {
                    if(locationPointDataPopup.isShowing()){
                        locationPointDataPopup.hide();
                    }
                });
                pauseTransition.play();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected LocationPointData findLocationPointData(double xMap, double yMap) {
        return null;
    }
}
