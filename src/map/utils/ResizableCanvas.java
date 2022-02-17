package map.utils;

import javafx.scene.canvas.Canvas;
import org.jfree.fx.FXGraphics2D;

import java.awt.*;

public class ResizableCanvas extends Canvas {

    public ResizableCanvas() {
        draw();
    }

    public ResizableCanvas(double width, double height) {
        super(width, height);
        draw();
    }

    private void draw(){
        FXGraphics2D graphics2D = new FXGraphics2D(this.getGraphicsContext2D());
        float[]hsb=Color.RGBtoHSB(79, 79, 79, null);
        Color hsbColor = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);//dark gray
        graphics2D.setBackground(hsbColor);
        graphics2D.clearRect(0, 0, (int) this.getWidth(), (int) this.getHeight());
    }

    @Override
    public double minWidth(double height) {
        return 1;
    }

    @Override
    public double minHeight(double width) {
        return 1;
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double prefHeight(double width) {
        return super.heightProperty().get();
    }

    @Override
    public double prefWidth(double height) {
        return super.widthProperty().get();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        draw();
    }
}
