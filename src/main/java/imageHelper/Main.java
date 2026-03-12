package imageHelper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.Stack;

public class Main extends Application {

    Canvas canvas = new Canvas(900,600);
    GraphicsContext g = canvas.getGraphicsContext2D();
    Stack<WritableImage> history = new Stack<>();

    Image currentImage;
    double imgX,imgY,imgW,imgH;

    double lastX,lastY;

    static Stage currentStage;

    @Override
    public void start(Stage stage){

        currentStage = stage;

        Button undo = new Button("Späť");
        Button copy = new Button("Kopírovať");

        undo.setPrefSize(140,50);
        copy.setPrefSize(140,50);

        Button black = new Button();
        Button red = new Button();
        Button green = new Button();
        Button blue = new Button();

        black.setStyle("-fx-background-color:black; -fx-min-width:40; -fx-min-height:40;");
        red.setStyle("-fx-background-color:red; -fx-min-width:40; -fx-min-height:40;");
        green.setStyle("-fx-background-color:green; -fx-min-width:40; -fx-min-height:40;");
        blue.setStyle("-fx-background-color:blue; -fx-min-width:40; -fx-min-height:40;");

        black.setOnAction(e->g.setStroke(Color.BLACK));
        red.setOnAction(e->g.setStroke(Color.RED));
        green.setOnAction(e->g.setStroke(Color.GREEN));
        blue.setOnAction(e->g.setStroke(Color.BLUE));

        g.setStroke(Color.BLACK);
        g.setLineWidth(4);

        undo.setOnAction(e->undo());
        copy.setOnAction(e->copyToClipboard());

        HBox top = new HBox(10,undo,copy,black,red,green,blue);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(canvas);

        enableDrawing();
        enableDragDrop();

        Scene scene = new Scene(root,900,650);

        keyHandling(scene);

        stage.setScene(scene);
        stage.setTitle("Pomocnik pre úpravu obrázkov");
        stage.show();
    }

    void undo(){

        if(!history.isEmpty()){

            WritableImage img = history.pop();

            g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
            g.drawImage(img,0,0);

            showPopup("Späť",true);
        }
    }

    void enableDrawing(){

        canvas.setOnMousePressed(e->{

            lastX = e.getX();
            lastY = e.getY();

            saveState();
        });

        canvas.setOnMouseDragged(e->{

            g.strokeLine(lastX,lastY,e.getX(),e.getY());

            lastX = e.getX();
            lastY = e.getY();
        });
    }

    void enableDragDrop(){

        canvas.setOnDragOver(e->{

            if(e.getDragboard().hasFiles())
                e.acceptTransferModes(TransferMode.COPY);

            e.consume();
        });

        canvas.setOnDragDropped(e->{

            Dragboard db = e.getDragboard();

            if(db.hasFiles()){

                File file = db.getFiles().get(0);

                try{

                    Image img = new Image(file.toURI().toString());

                    drawImageScaled(img);

                    showPopup("Obrazok sa nahral",true);

                }catch(Exception ex){

                    showPopup("Chybny obrazok",false);
                }
            }

            e.setDropCompleted(true);
            e.consume();
        });
    }

    void drawImageScaled(Image img){

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();

        double maxSize = 600;

        double scale = Math.min(maxSize/imgWidth, maxSize/imgHeight);

        if(scale > 1)
            scale = 1;

        imgW = imgWidth * scale;
        imgH = imgHeight * scale;

        imgX = (canvas.getWidth()-imgW)/2;
        imgY = (canvas.getHeight()-imgH)/2;

        currentImage = img;

        g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
        g.drawImage(img,imgX,imgY,imgW,imgH);
    }

    void saveState(){

        WritableImage snapshot = canvas.snapshot(new SnapshotParameters(),null);

        history.push(snapshot);
    }

    void copyToClipboard(){

        if(currentImage == null){

            showPopup("Nie je obrazok",false);
            return;
        }

        Canvas temp = new Canvas(imgW,imgH);
        GraphicsContext tg = temp.getGraphicsContext2D();

        WritableImage snap = canvas.snapshot(null,null);

        tg.drawImage(snap,-imgX,-imgY);

        WritableImage cropped = temp.snapshot(null,null);

        double maxSize = 500;

        double w = cropped.getWidth();
        double h = cropped.getHeight();

        double scale = Math.min(maxSize/w,maxSize/h);

        if(scale > 1)
            scale = 1;

        double newW = w * scale;
        double newH = h * scale;

        Canvas resize = new Canvas(newW,newH);
        GraphicsContext rg = resize.getGraphicsContext2D();

        rg.drawImage(cropped,0,0,newW,newH);

        WritableImage finalImg = resize.snapshot(null,null);

        ClipboardContent content = new ClipboardContent();
        content.putImage(finalImg);

        Clipboard.getSystemClipboard().setContent(content);

        showPopup("Skopirovane",true);
    }

    void pasteFromClipboard(){

        Clipboard clipboard = Clipboard.getSystemClipboard();

        if(clipboard.hasImage()){

            Image img = clipboard.getImage();

            drawImageScaled(img);

            showPopup("Obrazok prilepeny",true);
        }
        else{

            showPopup("Nie je skopirovany ziaden obrazok",false);
        }
    }

    void keyHandling(Scene scene){

        scene.setOnKeyPressed(event -> {

            if(event.isControlDown() && event.getCode()==KeyCode.C)
                copyToClipboard();

            if(event.isControlDown() && event.getCode()==KeyCode.V)
                pasteFromClipboard();

            if(event.isControlDown() && event.getCode()==KeyCode.Z)
                undo();
        });
    }

    void showPopup(String message, boolean success){

        Label label = new Label(message);

        String color = success ? "#2ecc71" : "#e74c3c";

        label.setStyle(
                "-fx-background-color:"+color+";" +
                        "-fx-text-fill:white;" +
                        "-fx-padding:10;" +
                        "-fx-font-size:14px;" +
                        "-fx-background-radius:6;"
        );

        Stage popup = new Stage();

        popup.initOwner(currentStage);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setAlwaysOnTop(true);

        Scene scene = new Scene(new HBox(label));

        popup.setScene(scene);

        popup.setX(currentStage.getX()+currentStage.getWidth()/2-80);
        popup.setY(currentStage.getY()+currentStage.getHeight()-140);

        popup.show();

        new Thread(() -> {

            try{ Thread.sleep(1200); }catch(Exception ignored){}

            Platform.runLater(popup::close);

        }).start();
    }

    public static void main(String[] args){
        launch();
    }
}