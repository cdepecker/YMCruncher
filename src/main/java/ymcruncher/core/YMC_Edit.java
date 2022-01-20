package ymcruncher.core;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

public class YMC_Edit implements Observer {

	// Static consts
	private static int WIDTH = 600;
	private static int HEIGHT = 160;
	private static int AMPLITUDE_MULTIPLICATOR = 2;
	private static int MIDDLE_LINE_OFFSET = 0xF*AMPLITUDE_MULTIPLICATOR/2;
	private static int CHANNELS_SPACE = 20 + 0xF*AMPLITUDE_MULTIPLICATOR;
	final protected byte[] arr4to8bits = { 0x0, 0x1, 0x2, 0x2, 0x4, 0x6, 0x9, 0xC, 0x11, 0x18, 0x23, 0x30, 0x48, 0x67, (byte)0xA5, (byte)0xFF };
	
	// private data 
	String fileName;
	YMC_Model model;
	Chiptune chiptune;
	int intBase = 0;
	int zoom = 1;
	Image image;
	
	// GUI
	private Display display;
	private Shell shell;
	
	public YMC_Edit(Shell pshell, final Chiptune chiptune, YMC_Model model)
	{
		// SWT Shell
		display = pshell.getDisplay();
		shell = new Shell(pshell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("Chiptune editor " + fileName);
		shell.setLayout(new RowLayout());
		
		// Init data
		this.model = model;
				
		if (chiptune != null)
		{
			// Calculate Chiptune size
			final int intVBL = chiptune.getLength();			
							
			Group grpCharts = new Group(shell, 0);
			grpCharts.setText("Voices A/B/C");
			grpCharts.setLayout(new RowLayout(SWT.HORIZONTAL));
			((RowLayout)grpCharts.getLayout()).fill = true;
			final Canvas canvas = new Canvas(grpCharts, SWT.H_SCROLL | SWT.V_SCROLL);
			canvas.setLayoutData(new RowData(WIDTH-10,HEIGHT-10));
			canvas.addPaintListener(new PaintListener() {
		        public void paintControl(PaintEvent e) {
		        	// Draw Pulses
					for(int v=0;v<3;v++)
		            {
		            	// middle line (vol == 0)
						GC gc = e.gc;
						gc.setLineStyle(SWT.LINE_DOT);
		            	gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		            	gc.drawLine(0, v*CHANNELS_SPACE + MIDDLE_LINE_OFFSET, canvas.getClientArea().width, v*CHANNELS_SPACE + MIDDLE_LINE_OFFSET);
		            	
			            // Max index in the canvas (i should always be < imax)
		            	int imax = Math.min((intVBL-intBase)*zoom, canvas.getClientArea().width);
			            
		            	// Used to draw lines between semi-periode
		            	int prevVol = v*CHANNELS_SPACE;
		            	
			            int i=0;		// graphic index
			            int k=0;		// index in chiptune
			            while(i<imax)
			            {
				            // Voice v
			            	//System.out.print(intVBL + " " + intBase + " " + k + " " + imax+ " " + i);
			            	Frame frame = chiptune.getFrame(intBase + k);
				            int vol = frame.getBytVol(v) & 0xF;
				            double period = frame.getPPeriod(v);
				            period = (period==0)?1:period;
				            
				            // do same schema till the value changes
				            double nextPeriod = period;
				            int intCurr = i;		            			            
				            double indexPeriod = 0;
				            double dbStep = (double)period/(double)zoom;
				            
				            // In this loop, we draw the same Pulse witdh (period) but at different volumes 
				            // TODO This loop is not optimal as it does 1 iteration for nothing (the first)
				            while((nextPeriod == period) && (intCurr<imax))
				            {
				            	frame = chiptune.getFrame(intBase + k);
				            	nextPeriod = frame.getPPeriod(v);
				            	vol = frame.getBytVol(v) & 0xF;
				            								            
					            // Set color
					            gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
					            gc.setLineStyle(SWT.LINE_SOLID);
					            
					            for(int z=intCurr;z<intCurr+zoom;z++)
					            {
					            	// Get graphical values for volume
					            	int volA = 0;
					            	
					            	if (indexPeriod<=(period/2))	// High state
					            		volA = v*CHANNELS_SPACE + vol*AMPLITUDE_MULTIPLICATOR;
					            	else							// Low state
					            		volA = v*CHANNELS_SPACE;	// Always 0
					            	
				            		if (prevVol != volA)
				            		{
				            			gc.drawLine(z, prevVol, z, volA);
				            			prevVol = volA;
				            		}
						            				
					            	// draw point
						            gc.drawPoint(z, volA);
						            						            
					            	// calculate next indexPeriod
					            	indexPeriod += dbStep;
					            	if (indexPeriod>period)			// Start another period
					            	{
					            		indexPeriod %= (double)period;
					            	}
					            }
					         
				            	intCurr += zoom;
					            k++;
				            }
					        i=intCurr;
				        }
			        }
		        }
		    });
			
			// Scroll bar (bottom)
			final ScrollBar hBar = canvas.getHorizontalBar ();
			hBar.setMaximum(chiptune.getLength());
			hBar.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					int hSelection = hBar.getSelection ();
					intBase = hSelection;					
					canvas.redraw();
				}
			});
			
			// Scroll bar (zoom)
			final ScrollBar vBar = canvas.getVerticalBar ();
			vBar.setMinimum(1);
			vBar.setSelection(zoom);
			vBar.setMaximum(150);
			vBar.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					zoom = vBar.getSelection();
					canvas.redraw();
				}
			});

		}
		// Pack it baby
	    shell.pack();
	}
	
	public void displayWindow()
	{
		shell.open();
	}
	
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}

}
