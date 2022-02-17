package map.gis;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;

public class LocationPointData {
    protected SimpleStringProperty place;
    protected SimpleDoubleProperty x;
    protected SimpleDoubleProperty y;
    private SimpleDoubleProperty activeParameterValue;
    private SimpleStringProperty activeParameterName;
    private SimpleStringProperty title;
    private Node representationImage;

    public LocationPointData(double x,
                             double y) {

        this("","", x, y, 0, "", new FontAwesomeIconView(FontAwesomeIcon.MAP_MARKER));
    }

    public LocationPointData(String title,String place, double x,
                             double y, double activeParameterValue,
                             String activeParameterName, Node representationImage) {
        this.title=new SimpleStringProperty(this,"title",title);
        this.place = new SimpleStringProperty(this, "place", place);
        this.x = new SimpleDoubleProperty(this, "x", x);
        this.y = new SimpleDoubleProperty(this, "y", y);
        this.activeParameterValue = new SimpleDoubleProperty(this, "activeParameterValue", activeParameterValue);
        this.activeParameterName = new SimpleStringProperty(this, "activeParameterName", activeParameterName);
        this.representationImage = representationImage;
        if(title.isEmpty()){
            this.representationImage.setStyle("-fx-fill: red");
            this.representationImage.prefWidth(30);
            this.representationImage.prefHeight(30);
            this.representationImage.minWidth(30);
            this.representationImage.minHeight(30);
            this.representationImage.maxWidth(30);
            this.representationImage.maxHeight(30);
        }else {
            this.representationImage.prefWidth(17);
            this.representationImage.prefHeight(17);
            this.representationImage.minWidth(17);
            this.representationImage.minHeight(17);
            this.representationImage.maxWidth(17);
            this.representationImage.maxHeight(17);
        }
        this.title.addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                this.representationImage = new FontAwesomeIconView(FontAwesomeIcon.MAP_MARKER);
                this.activeParameterValue.set(0);
                this.place.set("");
                this.representationImage.setStyle("-fx-text-fill: red");
            }
        });

    }

    public String getTitle() {
        return title.get();
    }

    public SimpleStringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public String getPlace() {
        return place.get();
    }

    public SimpleStringProperty placeProperty() {
        return place;
    }

    public void setPlace(String place) {
        this.place.set(place);
    }

    public double getX() {
        return x.get();
    }

    public SimpleDoubleProperty xProperty() {
        return x;
    }

    public void setX(double x) {
        this.x.set(x);
    }

    public double getY() {
        return y.get();
    }

    public SimpleDoubleProperty yProperty() {
        return y;
    }

    public void setY(double y) {
        this.y.set(y);
    }

    public double getActiveParameterValue() {
        return activeParameterValue.get();
    }

    public SimpleDoubleProperty activeParameterValueProperty() {
        return activeParameterValue;
    }

    public void setActiveParameterValue(double activeParameterValue) {
        this.activeParameterValue.set(activeParameterValue);
    }

    public String getActiveParameterName() {
        return activeParameterName.get();
    }

    public SimpleStringProperty activeParameterNameProperty() {
        return activeParameterName;
    }

    public void setActiveParameterName(String activeParameterName) {
        this.activeParameterName.set(activeParameterName);
    }

    public Node getRepresentationImage() {
        return representationImage;
    }

    public void setRepresentationImage(Node representationImage) {
        this.representationImage = representationImage;
    }
}
