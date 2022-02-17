package map.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import map.gis.GeographicDataInterface;
import mclp_tools.config.CfgMngController;
import org.geotools.map.MapContent;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Loader extends Application {
    MapViewController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage = new Stage();
        MapViewController.setParentStage(primaryStage);
        FXMLLoader loader = new FXMLLoader(MapViewController.class.getResource("MapView.fxml"));
        AnchorPane anchorPane = loader.load();
        controller = loader.getController();
        Scene scene = new Scene(anchorPane);
        primaryStage.setScene(scene);
        // primaryStage.setMaximized(true);
        primaryStage.show();
        Platform.runLater(() -> {
            controller.loadMap();
            controller.setData(new GeographicDataInterface() {
                @Override
                public void paintData(Graphics2D graphics, MapContent mapContent, Container c) {
                    double x[] = {-82.37328873611771, -82.37416081151703, -82.37139930059173, -82.37153632702933};
                    double y[] = {23.123603047596536, 23.123536981278406, 23.123803204071457, 23.124833675507585};
                    for (int i = 0; i < 4; i++) {
                        Point2D point = new Point2D.Double(x[i], y[i]);
                        Point screen = new Point();
                        mapContent.getViewport().getWorldToScreen().transform(point, screen);
                        File imageFile = CfgMngController.getInstance().getCurrentProfile().getFacilityCfgData()
                                                         .getFacilityTypes().get(0).getTypeImage();
                        try {
                            BufferedImage bfi = ImageIO.read(new FileImageInputStream(imageFile));
                            //   Graphics2D gr=bfi.createGraphics();
                            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                            //  graphics.drawImage(bfi, null, screen.x, screen.y);
                            graphics.drawImage(Toolkit.getDefaultToolkit().getImage(imageFile.getPath()), screen.x,
                                               screen.y, c);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
        });

    }
}
