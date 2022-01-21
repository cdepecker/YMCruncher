package ymcruncher.core;

// SWT

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import ymcruncher.plugins.SpecialFXType;

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
public class YMC_View implements Observer {

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
    private final YMC_Model model;
    // GUI
    private final Display display;
    private final Shell shell;
    private final ProgressBar pbProgressCrunch;
    private final ProgressBar pbProgressSingleCrunch;
    private final Tree lstSourceFiles;
    private final Label lblFileProcessed;
    private final Group grpList;
    private final Group grpOptions;
    private final Menu menuConf;
    private String strPath = ".\\";

    public YMC_View(final YMC_Model pmodel) {
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
        display = new Display();
        shell = new Shell(display, SWT.CLOSE | SWT.MIN);
        shell.setText(YMC_Model.strApplicationName);

        // Menu Verbosity
        menuConf = new Menu(shell, SWT.DROP_DOWN);
        final MenuItem menuItemVerbose = new MenuItem(menuConf, SWT.CHECK);
        menuItemVerbose.setText("&Verbose");
        menuItemVerbose.setSelection(YMC_Tools.isVerbose());
        menuItemVerbose.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                YMC_Tools.ToggleVerbosity();
            }
        });

        // Menu OutputPlugins
        // Get List of available Output main.Plugins and add them in the Menu
        final java.util.List<OutputPlugin> arrOutputPlugins = model.getOutuptPlugins();
        for (byte b = 0; b < arrOutputPlugins.size(); b++) {
            // OutputPlugin
            final OutputPlugin op = arrOutputPlugins.get(b);

            // Set Menu
            final MenuItem menuItemOP = new MenuItem(menuConf, SWT.RADIO);
            menuItemOP.setText(op.getMenuLabel());
            menuItemOP.addListener(SWT.Selection, new Listener() {

                public void handleEvent(final Event event) {
                    final MenuItem btnOrigin = (MenuItem) event.widget;
                    if (btnOrigin.getSelection()) {
                        // Registering as an Observer of the plugin
                        model.setCurrent_OutputPlugin(op);
                        op.addObserver(YMC_View.this);
                    }
                }
            });

            // Select first in list
            if (b == 0) {
                menuItemOP.setSelection(true);

                // Registering as an Observer of the plugin
                final OutputPlugin outputPlugin = model.getCurrent_OutputPlugin();
                outputPlugin.addObserver(YMC_View.this);
            }
        }

        // Other Menu Items
        final MenuItem menuItemPath = new MenuItem(menuConf, SWT.PUSH);
        menuItemPath.setText("&Change Destination Folder");
        menuItemPath.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                // Open Directory Dialog
                final DirectoryDialog dirDialog = new DirectoryDialog(shell, SWT.OPEN);
                dirDialog.setFilterPath(strPath);
                dirDialog.setText("Change Folder Dialog");
                dirDialog.setMessage("Please select a directory");
                final String strChangedPath = dirDialog.open();
                if (strChangedPath != null) {
                    strPath = strChangedPath;
                }
            }
        });

        // Settings
        final Menu menuMain = new Menu(shell, SWT.BAR);
        final MenuItem menuItemConf = new MenuItem(menuMain, SWT.CASCADE);
        menuItemConf.setText("&Settings");
        menuItemConf.setMenu(menuConf);

        // About
        final MenuItem menuItemAbout = new MenuItem(menuMain, SWT.PUSH);
        menuItemAbout.setText("&About");
        menuItemAbout.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                // Dialob Box About
                final MessageBox mb = new MessageBox(shell, SWT.OK);
                mb.setText("About main.YMCruncher");
                mb.setMessage(YMC_View.strAboutMessage);
                mb.open();
            }
        });

        // Main Layout
        shell.setLayout(new FillLayout());

        /**
         * Tabs
         * 1 - Main Tab
         * 2 - Output Plugin Settings
         * * - Options for Output main.Plugins
         */

        final TabFolder tabFolderMain = new TabFolder(shell, SWT.NONE);

        // Main Tab Layout
        final Composite compMain = new Composite(tabFolderMain, SWT.NONE);
        final RowLayout lytMain = new RowLayout(SWT.VERTICAL);
        lytMain.fill = true;
        compMain.setLayout(lytMain);
        final TabItem tabMain = new TabItem(tabFolderMain, SWT.NONE);
        tabMain.setText("Main");
        tabMain.setControl(compMain);

        /**********************************************************************
         * OPTION TABS
         **********************************************************************/

        for (final OutputPlugin op : arrOutputPlugins) {
            if (op.blnHasOptions()) {
                final Composite compOP = new Composite(tabFolderMain, SWT.NONE);
                compOP.setLayout(new RowLayout(SWT.VERTICAL));
                final TabItem tabOP = new TabItem(tabFolderMain, SWT.NONE);
                tabOP.setText(op.getMenuLabel());
                tabOP.setControl(compOP);

                // Booleans
                if (op.blnHasBooleanOptions()) {
                    final Group grpBooleanParams = new Group(compOP, SWT.NONE);
                    final RowLayout lytBooleanOptions = new RowLayout(SWT.VERTICAL);
                    lytBooleanOptions.fill = true;
                    grpBooleanParams.setLayout(lytBooleanOptions);
                    grpBooleanParams.setText("Boolean Parameters");
                    for (final Iterator it = op.getBooleanOptionList().iterator(); it.hasNext(); ) {
                        final Entry entryOpt = (Entry) it.next();
                        final String key = (String) entryOpt.getKey();
                        final boolean blnValue = op.getBooleanOption(key);

                        final Button cbParam = new Button(grpBooleanParams, SWT.CHECK);
                        cbParam.setText(key);
                        cbParam.setSelection(blnValue);
                        cbParam.addListener(SWT.Selection, new Listener() {

                            public void handleEvent(final Event event) {
                                final boolean blnValue = op.getBooleanOption(key);
                                op.setBooleanOption(key, !blnValue);
                            }
                        });
                    }
                }

                // Lists
                if (op.blnHasListOptions()) {
                    final Group grpListParams = new Group(compOP, SWT.FILL);
                    final GridLayout lytListOptions = new GridLayout(4, false);
                    lytListOptions.horizontalSpacing = 20;
                    grpListParams.setLayout(lytListOptions);
                    grpListParams.setText("List Parameters");
                    for (final Iterator it = op.getListOptionList().iterator(); it.hasNext(); ) {
                        final Entry entryOpt = (Entry) it.next();
                        final String key = (String) entryOpt.getKey();
                        final String[] arrList = op.getListOptionArray(key);
                        final int intSelected = op.getListOptionIndex(key);

                        // Trick to space options
                        if (arrList == null) {
                            new Composite(grpListParams, SWT.NULL);
                            new Composite(grpListParams, SWT.NULL);
                            continue;
                        }

                        final Label lbList = new Label(grpListParams, SWT.NONE);
                        lbList.setText(key);
                        if (op.isListOptionRadioType(key)) {
                            final Composite composite = new Composite(grpListParams, SWT.NULL | SWT.BORDER);
                            composite.setLayout(new RowLayout());

                            for (int i = 0; i < arrList.length; i++) {
                                final int k = i;
                                final Button radioButton = new Button(composite, SWT.RADIO);
                                radioButton.setText(arrList[i]);
                                if (i == intSelected) {
                                    radioButton.setSelection(true);
                                }
                                radioButton.addListener(SWT.Selection, new Listener() {

                                    public void handleEvent(final Event event) {
                                        op.setListOptionIndex(key, k);
                                    }
                                });
                            }
                        } else {
                            final List lstParam = new List(grpListParams, SWT.V_SCROLL | SWT.BORDER);

                            lstParam.setItems(arrList);
                            lstParam.setSelection(intSelected);
                            lstParam.addListener(SWT.Selection, new Listener() {

                                public void handleEvent(final Event event) {
                                    op.setListOptionIndex(key, lstParam.getSelectionIndex());
                                }
                            });
                        }
                    }
                }
            }
        }

        /**********************************************************************
         * MAIN TAB
         **********************************************************************/

        // List Layout
        grpList = new Group(compMain, 0);
        final RowLayout lytList = new RowLayout(SWT.VERTICAL);
        lytList.fill = true;
        grpList.setLayout(lytList);
        final StringBuffer sb = new StringBuffer("Files to Crunch ");
//        for (final String element : InputPlugin.INPUT_PLUGINS) {
//            sb.append("/" + element.substring(0, element.length() - "InputPlugin".length()));
//        }
        grpList.setText(sb.toString());

        // List of Files & Buttons
        lstSourceFiles = new Tree(grpList, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        lstSourceFiles.setLayoutData(new RowData(YMC_View.WIDTH, 100));

        /** Expned Listener */
        lstSourceFiles.addTreeListener(new TreeListener() {

            public void treeCollapsed(final TreeEvent e) {
            }

            public void treeExpanded(final TreeEvent e) {
                final TreeItem item = (TreeItem) e.item;
                final String strFullPath = item.getItem(0).getText();
                final Chiptune chiptune = model.getChiptune(strFullPath);

                // Add branch only if it hasn't been expanded yet
                if (!chiptune.isLoaded() && item.getItemCount() <= 1) {
                    // Frequency
                    final TreeItem subitemError = new TreeItem(item, SWT.NONE);
                    subitemError.setText(new String[]{"Error, unable to load chiptune."});
                } else if (item.getItemCount() <= 1) {
                    // Frequency
                    final TreeItem subitemAuthor = new TreeItem(item, SWT.NONE);
                    subitemAuthor.setText(new String[]{"Author = " + chiptune.getStrAuthorName()});

                    // Play Rate
                    final TreeItem subitemPlayRate = new TreeItem(item, SWT.NONE);
                    subitemPlayRate.setText(new String[]{"Play Rate = " + chiptune.getPlayRate()});

                    // Number of samples
                    final TreeItem subitemNbSamples = new TreeItem(item, SWT.NONE);
                    subitemNbSamples.setText(new String[]{"Nb samples = " + chiptune.getNbSamples()});
                }
            }

        });

        // TODO Context Menu
        final Menu popUpMenu = new Menu(shell, SWT.POP_UP);
        lstSourceFiles.setMenu(popUpMenu);
        final MenuItem editItem = new MenuItem(popUpMenu, SWT.PUSH);
        editItem.setText("Edit Chiptune");
        editItem.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                // Dialog Box Edit
                if (lstSourceFiles.getSelectionCount() == 1) {
                    final TreeItem item = lstSourceFiles.getSelection()[0];
                    final String strFullPath = item.getItem(0).getText();
                    final Chiptune chiptune = model.getChiptune(strFullPath);
                    final YMC_Edit editFrame = new YMC_Edit(shell, chiptune, model);
                    editFrame.displayWindow();
                }
            }
        });

        /**
         * Drag and drop section
         */
        final DropTarget target = new DropTarget(lstSourceFiles, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);

        // Receive data in Text or File format
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        final Transfer[] types = new Transfer[]{fileTransfer};
        target.setTransfer(types);

        target.addDropListener(new DropTargetListener() {

            public void dragEnter(final DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if ((event.operations & DND.DROP_COPY) != 0) {
                        event.detail = DND.DROP_COPY;
                    } else {
                        event.detail = DND.DROP_NONE;
                    }
                }
                // will accept text but prefer to have files dropped
                for (final TransferData dataType : event.dataTypes) {
                    if (fileTransfer.isSupportedType(dataType)) {
                        event.currentDataType = dataType;
                        // files should only be copied
                        if (event.detail != DND.DROP_COPY) {
                            event.detail = DND.DROP_NONE;
                        }
                        break;
                    }
                }
            }

            public void dragOver(final DropTargetEvent event) {
            }

            public void dragOperationChanged(final DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if ((event.operations & DND.DROP_COPY) != 0) {
                        event.detail = DND.DROP_COPY;
                    } else {
                        event.detail = DND.DROP_NONE;
                    }
                }
                // allow text to be moved but files should only be copied
                if (fileTransfer.isSupportedType(event.currentDataType)) {
                    if (event.detail != DND.DROP_COPY) {
                        event.detail = DND.DROP_NONE;
                    }
                }
            }

            public void dragLeave(final DropTargetEvent event) {
            }

            public void dropAccept(final DropTargetEvent event) {
            }

            public void drop(final DropTargetEvent event) {
                if (fileTransfer.isSupportedType(event.currentDataType)) {
                    final String[] files = (String[]) event.data;
                    for (final String strFileName : files) {

                        // Add entry in model
                        model.addChiptune(strFileName);

                        final String strREFileNameOnly = "^.*\\" + File.separatorChar;
                        final String strFileNameOnly = strFileName.replaceFirst(strREFileNameOnly, "");

                        final TreeItem item = new TreeItem(lstSourceFiles, SWT.NONE);
                        item.setText(new String[]{strFileNameOnly});
                        final TreeItem subitem = new TreeItem(item, SWT.NONE);
                        subitem.setText(new String[]{strFileName});

                    }
                }
            }
        });

        // Button Layout
        final Composite cmpButtons = new Composite(grpList, 0);
        final RowLayout lytButtons = new RowLayout(SWT.HORIZONTAL);
        lytButtons.justify = true;
        cmpButtons.setLayout(lytButtons);

        final Button btnAddFile = new Button(cmpButtons, SWT.PUSH);
        btnAddFile.setText("Add File(s)");
        btnAddFile.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                final FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
                dialog.setFilterPath(strPath);
                dialog.open();
                final String[] arrFileName = dialog.getFileNames();
                for (final String element : arrFileName) {
                    final String strFullPath = dialog.getFilterPath() + File.separatorChar + element;

                    // Add entry in model
                    model.addChiptune(strFullPath);

                    // Add GUI branch
                    // Short Name
                    final TreeItem item = new TreeItem(lstSourceFiles, SWT.NONE);
                    item.setText(new String[]{element});
                    // Full name
                    final TreeItem subitem = new TreeItem(item, SWT.NONE);
                    subitem.setText(new String[]{strFullPath});
                }
            }
        });
        final Button btnRemFile = new Button(cmpButtons, SWT.PUSH);
        btnRemFile.setText("Remove File(s)");
        btnRemFile.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                final TreeItem arrTI[] = lstSourceFiles.getSelection();
                for (int i = 0; i < arrTI.length; i++) {
                    if (!arrTI[i].isDisposed() && arrTI[i].getParentItem() == null) // Act only on main branch
                    {
                        // Remove entry from model
                        model.delChiptune(arrTI[i].getItem(0).getText());

                        // Del GUI branch
                        arrTI[i].dispose();
                    }
                }
            }
        });

        // Button - Crunch Entire List
        final Button btnCrunchEntireList = new Button(cmpButtons, SWT.PUSH);
        btnCrunchEntireList.setText("Crunch Entire List");

        final Listener doCrunchEntireListListener = new Listener() {

            public void handleEvent(final Event event) {
                final ProcessCrunch process = new ProcessCrunch();
                process.start();
            }
        };

        btnCrunchEntireList.addListener(SWT.Selection, doCrunchEntireListListener);

        // Options Part
        grpOptions = new Group(compMain, SWT.NONE);
        final RowLayout lytOptions = new RowLayout(SWT.VERTICAL);
        lytOptions.fill = true;
        grpOptions.setLayout(lytOptions);
        grpOptions.setText("Debug - Enable or Disable Sound/Noise/Eveloppe for each Channel ");

        // Filters
        final Composite panReg7 = new Composite(grpOptions, 0);
        panReg7.setLayout(new GridLayout(3, false));
        final Button[][] arrFilters = new Button[3][3];
        for (byte b = 0; b < 3; b++) {
            final byte final_b = b;
            arrFilters[0][b] = new Button(panReg7, SWT.CHECK);
            arrFilters[0][b].setText("Voice " + b);
            arrFilters[0][b].setSelection(true);
            arrFilters[0][b].addListener(SWT.Selection, new Listener() {

                public void handleEvent(final Event event) {
                    model.toggleFilterVoice(final_b);
                }
            });

            arrFilters[1][b] = new Button(panReg7, SWT.CHECK);
            arrFilters[1][b].setText("Noise " + b);
            arrFilters[1][b].setSelection(true);
            arrFilters[1][b].addListener(SWT.Selection, new Listener() {

                public void handleEvent(final Event event) {
                    model.toggleFilterVoice(final_b + 3);
                }
            });

            arrFilters[2][b] = new Button(panReg7, SWT.CHECK);
            arrFilters[2][b].setText("Env " + b);
            arrFilters[2][b].setSelection(true);
            arrFilters[2][b].addListener(SWT.Selection, new Listener() {

                public void handleEvent(final Event event) {
                    model.toggleFilterEnv(final_b);
                }
            });
        }

        // SpecialFX filter
        final SpecialFXType specialFXs[] = SpecialFXType.filterValues();
        final Button arrSpecialFXFilters[] = new Button[specialFXs.length];
        final Composite panSpecialFXFilters = new Composite(grpOptions, 0);
        panSpecialFXFilters.setLayout(new RowLayout(SWT.HORIZONTAL));
        for (byte b = 0; b < specialFXs.length; b++) {
            final SpecialFXType final_b = specialFXs[b];
            arrSpecialFXFilters[b] = new Button(panSpecialFXFilters, SWT.CHECK);
            arrSpecialFXFilters[b].setText(specialFXs[b].name());
            arrSpecialFXFilters[b].setSelection(true);
            arrSpecialFXFilters[b].addListener(SWT.Selection, new Listener() {

                public void handleEvent(final Event event) {
                    model.toggleSpecialFXFilter(final_b);
                }
            });
        }

        // try to convert replay Frequency ?
        final Composite panOptions = new Composite(grpOptions, 0);
        panOptions.setLayout(new RowLayout(SWT.VERTICAL));
        final Button btnRF = new Button(panOptions, SWT.CHECK);
        btnRF.setText("Convert tune to CPC PSG frequency (1000000Hz)");
        btnRF.setSelection(true);
        btnRF.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                model.ToggleFrequencyConvert();
            }
        });

        /*
         * Button btnAF = new Button(panOptions, SWT.CHECK);
         * btnAF.setText("Try to Adjust Frequency");
         * btnAF.setSelection(false);
         * btnAF.addListener(SWT.Selection,
         * new Listener() {
         * public void handleEvent(Event event) {
         * model.toggleAdjustFrequeny();
         * }});
         */

        final Button btnFF = new Button(panOptions, SWT.CHECK);
        btnFF.setText("Force replay frequency to 50hz (EXPERIMENTAL)");
        btnFF.setSelection(false);
        btnFF.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                model.ToggleReplayFrequencyConvert();
            }
        });

        final Button btnNP = new Button(panOptions, SWT.CHECK);
        btnNP.setText("Null Period disables channel mixing");
        btnNP.setSelection(false);
        btnNP.setEnabled(false);
        btnNP.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event event) {
                model.ToggleNullPeriodDisableChannel();
            }
        });

        // Last Part
        final Group grpProgressBar = new Group(compMain, 0);
        final GridLayout lytProgress = new GridLayout(2, false);
        grpProgressBar.setLayout(lytProgress);

        // File processed
        final Label lblFile = new Label(grpProgressBar, 0);
        lblFile.setText("File processed :");
        lblFileProcessed = new Label(grpProgressBar, 0);
        lblFileProcessed.setText("none");

        // Progress Bar
        final Label lblProgressBar = new Label(grpProgressBar, 0);
        lblProgressBar.setText("Current process");
        pbProgressSingleCrunch = new ProgressBar(grpProgressBar, 0);
        pbProgressSingleCrunch.setMinimum(0);
        pbProgressSingleCrunch.setMaximum(100);
        pbProgressSingleCrunch.setToolTipText("Progress Bar");
        pbProgressSingleCrunch.setLayoutData(new GridData(YMC_View.WIDTH - 70, 10));

        // Progress Bar - Total Process
        final Label lblProgressCrunch = new Label(grpProgressBar, 0);
        lblProgressCrunch.setText("Total process");
        pbProgressCrunch = new ProgressBar(grpProgressBar, 0);
        pbProgressCrunch.setMinimum(0);
        pbProgressCrunch.setMaximum(100);
        pbProgressCrunch.setToolTipText("Total Process");
        pbProgressCrunch.setLayoutData(new GridData(YMC_View.WIDTH - 70, 10));

        // Pack it baby
        shell.setMenuBar(menuMain);
        shell.pack();
    }

    private void forceDisplay(final Control control) {
        control.pack();
        control.redraw();
        control.update();
    }

    public void displayWindow() {
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    // Implemented from Interface Observer
    public void update(final Observable o, final Object arg) {
        if (o == model) {
            // Update Label
            final String strFileName = model.getStrChiptuneName();
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (pbProgressCrunch.isDisposed()) {
                        return;
                    }
                    lblFileProcessed.setText(strFileName);
                    forceDisplay(lblFileProcessed);
                }
            });

            // Update the ProgressBar
            final int total = model.getHmChiptuneSize();
            final int intProgress = total == 0 ? 0 : model.getHmChiptuneindex() * 100 / model.getHmChiptuneSize();

            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (pbProgressCrunch.isDisposed()) {
                        return;
                    }
                    pbProgressCrunch.setSelection(intProgress);
                }
            });
        } else {
            // Completion Status
            final OutputPlugin op = (OutputPlugin) o;
            final int ratio = op.getCompletionRatio();

            // Update Current Process bar
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (pbProgressSingleCrunch.isDisposed()) {
                        return;
                    }
                    pbProgressSingleCrunch.setSelection(ratio);
                }
            });
        }
    }

    // Thread for Processing (Long process)
    // needs to be done in a separate thread
    class ProcessCrunch extends Thread {

        private void updateProgressBar(final int intValue) {
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (pbProgressCrunch.isDisposed()) {
                        return;
                    }
                    pbProgressCrunch.setSelection(intValue);
                }
            });
        }

        private void updateLabel(final String strFileName) {
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (pbProgressCrunch.isDisposed()) {
                        return;
                    }
                    lblFileProcessed.setText(strFileName);
                    forceDisplay(lblFileProcessed);
                }
            });
        }

        private void enableButtons(final boolean blnEnabled) {
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (grpList.isDisposed() || menuConf.isDisposed() || grpOptions.isDisposed()) {
                        return;
                    }
                    grpList.setEnabled(blnEnabled);
                    menuConf.setEnabled(blnEnabled);
                    grpOptions.setEnabled(blnEnabled);
                }
            });
        }

        @Override
        public void run() {
            // Disable Buttons
            enableButtons(false);

            // Perform Crunch operation
            model.crunchList(strPath/* , outputPlugin */);

            // Enable Buttons
            updateLabel("finished.");
            updateProgressBar(0);
            enableButtons(true);
        }
    }
}