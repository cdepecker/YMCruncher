package ymcruncher.core;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jdk.tools.jlink.resources.plugins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.print.BackgroundServiceLookup;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.Thread.sleep;

public class Controller {

    // Logger
    private static final Logger LOGGER = LogManager.getLogger(Controller.class.getName());

    // Model
    private final Model model = new Model();

    // View nodes
    @FXML private ComboBox<OutputPlugin> outputPlugins;
    @FXML private ListView<Chiptune> listFiles;
    @FXML private TextFlow txtInfo;
    @FXML private Menu settings;

    // FIXME ProgressBars GUI is not updated properly
    @FXML private ProgressBar totalProgressBar;
    @FXML private ProgressBar chiptuneProgressBar;

    private Stage parentStage;
    Alert info = new Alert(Alert.AlertType.NONE, ABOUT_MESSAGE, ButtonType.CLOSE);

    // TODO Moves in text file
    private static final String ABOUT_MESSAGE = "YMCruncher's initial purpose was to convert AY-3-891X chiptunes to a packed format such as AYC to allow them to be replayed on the Amstrad CPC machines.\n"
            + "Please see the Releases notes for more info about what has been done for this version\n" + "Supported input formats are :\n" + "- YM(2, 3, 3b, 5, 6)\n" + "- VTX\n" + "- MYM\n"
            + "- VGM (Sega Master System - PSG only)\n\n" + "The chiptune can be processed using one of those output formats:\n" + "- AYC (Compressed YM format originated by OVL)\n"
            + "- YM (Version 6)\n" + "- WAV (Basic Wave Output - with SpecialFX !!!)\n" + "- Fake (This one will only produce a Log file)\n" + "\n" + "Greetings :\n"
            + "- Leonard (Atari scene) : Thanks for the YM format\n" + "- Madram (OVL member - cpc scene) : Thanks for the incredible AYC compression format\n"
            + "- The VGM, MYM and VTX format creators";

    public Model getModel(){
        return model;
    }

    public void initialize(){

        //get current output plugin
        outputPlugins.getSelectionModel().select(0);

        listFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listFiles.setItems(model.getChiptuneList());

        // ProgressBars
        totalProgressBar.progressProperty().bind(model.getTotalProgress());
        chiptuneProgressBar.progressProperty().bind(OutputPlugin.getCompletionRatio());

        // info
        info.setTitle("About YMCruncher");
        info.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // Otherwise, the message gets truncated

        // Add settings menu for input plugins
        Menu inputOptions = getMenuPluginInputOptions("Input Plugins", model.getInputPlugins());
        if (!inputOptions.getItems().isEmpty()) settings.getItems().add(inputOptions);

        // Add settings menu for output plugins
        Menu outputOptions = getMenuPluginOutputOptions("Output Plugins", model.getOutuptPlugins());
        if (!outputOptions.getItems().isEmpty()) settings.getItems().add(outputOptions);
    }

    private Menu getMenuPluginInputOptions(String menuLabel, List<InputPlugin> plugins) {
        Menu menuOptions = new Menu(menuLabel);
        for (Plugin plugin: plugins){
            addMenuPluginOptions(menuOptions, plugin);
        }
        return menuOptions;
    }

    private Menu getMenuPluginOutputOptions(String menuLabel, List<OutputPlugin> plugins) {
        Menu menuOptions = new Menu(menuLabel);
        for (Plugin plugin: plugins){
            addMenuPluginOptions(menuOptions, plugin);
        }
        return menuOptions;
    }

    private void addMenuPluginOptions(Menu menuOptions, Plugin plugin) {
        if (plugin.blnHasOptions()){
            MenuItem opMenu = new MenuItem(plugin.getMenuLabel());
            menuOptions.getItems().add(opMenu);

            opMenu.setOnAction(e -> {
                String name = ((MenuItem) e.getTarget()).getText();
                LOGGER.debug(name + " selected");

                // New window (Stage/ScrollPane)
                ScrollPane sp = new ScrollPane();
                Scene scene = new Scene(sp);
                scene.getStylesheets().add("css/styles.css");
                Stage opSettingsStage = new Stage();
                opSettingsStage.setScene(scene);
                opSettingsStage.setTitle(plugin.getMenuLabel());

                // Set default settings
                VBox vbox = new VBox();
                sp.setContent(vbox);
                vbox.setAlignment(Pos.CENTER);
                vbox.setMaxWidth(Double.valueOf(Tools.properties.getProperty("settings_width")));
                vbox.setMaxHeight(Double.valueOf(Tools.properties.getProperty("settings_height")));
//                    vbox.setStyle("-fx-background-color: #336699;");

                // Add List of Boolean options
                VBox vbox3 = new VBox();
                vbox3.setAlignment(Pos.CENTER);
                vbox3.getStyleClass().add("settings_options");
                vbox.getChildren().add(vbox3);
                if (plugin.blnHasBooleanOptions()) {
                    for (Map.Entry<String, Boolean> option: plugin.getBooleanOptionList()){
                        vbox3.getChildren().add(new CheckBox(option.getKey()));
                    }
                }

                // Add List of either combo-boxes or radio buttons
                if (plugin.blnHasListOptions()){

                    GridPane gp = new GridPane();
                    vbox.getChildren().add(gp);
                    gp.setHgap(10);
                    gp.setVgap(10);
                    gp.setAlignment(Pos.CENTER);
                    gp.setPadding(new Insets(0, 10, 0, 10));
                    int gp_row=0;
                    int gp_col=0;

                    for (Map.Entry<String, OutputPlugin.OptionList> option: plugin.getListOptionList()){

                        final String key = option.getKey();
                        final Plugin.OptionList optionList = plugin.getListOptionArray(key);

                        Label lbl = new Label(option.getKey());
                        HBox hbox = new HBox();
                        hbox.setAlignment(Pos.CENTER);

                        if (plugin.isListOptionRadioType(key)) { // Radio buttons

                            VBox vbox2 = new VBox();
                            vbox2.setAlignment(Pos.CENTER);
                            vbox2.getStyleClass().add("settings_options");
//                                vbox.getChildren().add(vbox2);
                            gp.add(vbox2, gp_col, gp_row);
                            vbox2.getChildren().add(lbl);
                            vbox2.getChildren().add(hbox);
                            ToggleGroup group = new ToggleGroup();
                            for (Object optionItem: optionList.getItems()) {
                                RadioButton rb = new RadioButton();
                                rb.setText(optionItem.);
                                rb.setToggleGroup(group);
                                hbox.getChildren().add(rb);
                            }
                        } else { // Combo Boxes
//                                vbox.getChildren().add(hbox);
                            gp.add(hbox, gp_col, gp_row);
                            hbox.getChildren().add(lbl);
                            hbox.getStyleClass().add("settings_options");

                            ComboBox cb = new ComboBox();
                            hbox.getChildren().add(cb);
                            if (arrList != null) {
                                cb.getItems().addAll(arrList);
                            }
                        }

                        // Grid row/col increase
                        if (++gp_col>1){
                            gp_col=0;
                            gp_row++;
                        }

                    }
                }

                opSettingsStage.show();
            });
        }
    }

    /**
     * Add files to the to be crunched list
     * @param actionEvent
     */
    public void addFiles(ActionEvent actionEvent) {
        // Get File from FileChooser Object
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(model.getSourceFolder());
        List<File> files = chooser.showOpenMultipleDialog(parentStage);

        if (files != null) {

            // Add File in View and Model
            for (File file : files) {
                model.addChiptune(new Chiptune(file));
                LOGGER.debug("Adding file " + file.getName());
                LOGGER.info("Size = " + model.getChiptuneList().size());
            }

            // Override the source Folder
            Optional sourceFolder = files.stream().findFirst();
            if (sourceFolder.isPresent()) {
                model.setSourceFolder(files.stream().findFirst().get().getParentFile());
                LOGGER.debug("Source folder changed to: " + model.getSourceFolder());
            }
        }
    }

    public void setParentStage(Stage primaryStage) {
        this.parentStage = primaryStage;
    }

    public void removeFiles(ActionEvent actionEvent) {
        ObservableList<Chiptune> selectedItems = listFiles.getSelectionModel().getSelectedItems();
        selectedItems.forEach(chiptune -> LOGGER.debug("Removing file " + chiptune.getFileName()));
        model.removeAllChiptunes(selectedItems);
        LOGGER.info("Size = " + model.getChiptuneList().size());
    }

    public void infoFile(ActionEvent actionEvent) {
        txtInfo.getChildren().clear();
        ObservableList<Chiptune> selectedItems = listFiles.getSelectionModel().getSelectedItems();
        selectedItems.forEach(chiptune -> {
            chiptune.load(model.getInputPlugins());
            txtInfo.getChildren().add(new Text(chiptune.getInfo() + "\n"));
            LOGGER.debug("Getting info for file " + chiptune.getFileName());
        });
    }

    public void crunchFiles(ActionEvent actionEvent) {
        LOGGER.debug("Crunch started at " + new Date());
        new Thread(new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                model.crunchList();
                return true;
            }
        }).start();
        LOGGER.debug("Crunch finished at " + new Date());
    }

    public void toggleVerbose(ActionEvent actionEvent) {
        // TODO Use proper javafx binding
        Tools.setBlnVerbose(((CheckMenuItem)actionEvent.getSource()).isSelected());
        LOGGER.debug("Verbosity changed to " + Tools.isVerbose());
    }

    /**
     * Display About Alert
     * @param actionEvent
     */
    public void showInformation(ActionEvent actionEvent) {
        info.show();
    }

    /**
     * CHoose the destination folder for the crunched chiptunes
     * @param actionEvent
     */
    public void chooseDestinationFolder(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(model.getDestinationPath()));
        File destinationFolder = directoryChooser.showDialog(parentStage);
        if (destinationFolder != null) {
            model.setDestinationPath(destinationFolder.getAbsolutePath());
            LOGGER.debug("Destination folder changed to: " + model.getDestinationPath());
        }
    }

    public void testProgressBar(ActionEvent actionEvent) {
        Task task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                for (int i=0;i<10;i++){
                    sleep(1000);
                    this.updateProgress(i,10);
                }
                return true;
            }
        };
        totalProgressBar.progressProperty().unbind();
        totalProgressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
        LOGGER.debug("Crunch finished at " + new Date());
    }
}