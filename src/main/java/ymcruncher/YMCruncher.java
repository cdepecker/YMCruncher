package ymcruncher;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import ymcruncher.core.Model;
import ymcruncher.core.Controller;

/**
 * TODO :
 * - Log functionnality sucks ... I need to do something clean
 * - Include Special FX in AycOutputPlugin
 * - Think about a new way to encode a mod chiptune
 * - Study SNDH format
 */

/**
 * Main Class (Launch the main.YMCruncher)
 *
 * @author F-Key/RevivaL
 */
public class YMCruncher extends Application {
    public static void main(String[] args) {
        // Model
        Model model = new Model();

        // View
//        YMC_View view = new YMC_View(model);
//        view.displayWindow();
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        // Load static view definition from fxml file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/View.fxml"));
        Parent root = loader.load();

        // Passes the view primaryStage to the controller so that he can reference to it later
        Controller controller = loader.getController();
        controller.setParentStage(primaryStage);

        // Display application Title and render the scene
        primaryStage.setTitle(Model.strApplicationName);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}