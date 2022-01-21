package ymcruncher;

import ymcruncher.core.YMC_Model;
import ymcruncher.core.YMC_View;

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
public class YMCruncher {
    public static void main(String[] args) {
        // Model
        YMC_Model model = new YMC_Model();

        // View
        YMC_View view = new YMC_View(model);
        view.displayWindow();
//        view.launch();
    }
}