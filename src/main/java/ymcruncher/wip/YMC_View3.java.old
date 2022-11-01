package ymcruncher.core;

// SWT

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

/**
 * GUI class that observes the main.YMCruncher
 *
 * @author F-key/RevivaL
 * @version 0.5
 */
public class YMC_View3 extends Application implements Observer  {

    // Static variables
    private static final int WIDTH = 600;
    private static final String strAboutMessage = "main.YMCruncher's initial purpose was to convert AY-3-891X chiptunes to a packed format such as AYC to allow them to be replayed on the Amstrad CPC machines.\n"
            + "Please see the Releases notes for more info about what has been done for this version\n" + "Supported input formats are :\n" + "- YM(2, 3, 3b, 5, 6)\n" + "- VTX\n" + "- MYM\n"
            + "- VGM (Sega Master System - PSG only)\n\n" + "The chiptune can be processed using one of those output formats:\n" + "- AYC (Compressed YM format originated by OVL)\n"
            + "- YM (Version 6)\n" + "- WAV (Basic Wave Output - with SpecialFX !!!)\n" + "- Fake (This one will only produce a Log file)\n" + "\n" + "Greetings :\n"
            + "- Leonard (Atari scene) : Thanks for the YM format\n" + "- Madram (OVL member - cpc scene) : Thanks for the incredible AYC compression format\n"
            + "- The VGM, MYM and VTX format creators";

    // Serialization
    private static final long serialVersionUID = 1L;

    // Data Members
    private YMC_Model model = null;
    // GUI
    private Stage stage;
    private Scene scene;

    private String strPath = ".\\";

    public YMC_View3(final YMC_Model pmodel) {
        // Try to initialize the Start Path
        try {
            strPath = new File(".").getCanonicalPath();
        } catch (final IOException e) {
            System.out.println("Warning ! Cannot set the Current Working Directory");
        }

        // Initialize Private data
        model = pmodel;
        model.addObserver(this);

        // SWT Shell
//        VBox vBox = new VBox(new Label("A JavaFX Label"));
//        scene = new Scene(vBox);
//        stage = new Stage();
//        stage.setScene(scene);
//        scene = new Scene(stage, SWT.CLOSE | SWT.MIN);

//        scene.setText(YMC_Model.strApplicationName);

    }

    @Override
    public void update(Observable o, Object arg) {

    }

    @Override
    public void start(Stage stage) throws Exception {
        Circle circ = new Circle(40, 40, 30);
        Group root = new Group(circ);
        Scene scene = new Scene(root, 400, 300);

        stage.setTitle("My JavaFX Application");
        stage.setScene(scene);
        stage.show();
    }

    public YMC_View3() {}

    public void launch() {
        Application.launch();
    }
}