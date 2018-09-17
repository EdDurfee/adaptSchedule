package interactionFramework;

import interval.IntervalSet;

import java.awt.AlphaComposite;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import stp.Timepoint;
import dtp.DisjunctiveTemporalProblem;

public class Viz extends JPanel {
	private static String targetPath = "."; // change this to the desired default
											// path for screenshots to appear
	private static double ORANGE_CUTOFF = .7; // Ratio of time required to make
												// the UI turn orange
	private static double RED_CUTOFF = .9;
	private static int TEXT_COLUMN_SIZE = 160; // size of the left column of
												// labels (in pixels)
	private static int PIXELS_PER_HOUR = 30; //40;
	private static int HEIGHT_OF_WHISKER = 20; //16; // height of one whisker on the
												// diagram (in pixels)
	private static int SPACE_BETWEEN_WHISKERS = 6; //5; // (in pixels)
	private static int LABEL_X_POSITION = 15; //20; // size of the left margin before
												// any labels are printed
	private static double WINDOWS_ROW_OFFSET = -3.3; // used for aligning
														// tooltips in the right
														// spot vertically 
	private static double LINUX_ROW_OFFSET = -2; // used for aligning tooltips
													// in the right spot
													// vertically
	private static Color FAINT_BLUE = new Color(235, 235, 255);
	private static Color DARK_PINK = new Color(255, 140, 0);
	private static Color DARK_GREEN = new Color(50, 50, 50);
	private static int maxActivities = 45; // this is merely a default - it is
											// overwritten when a render is
											// initiated
	private static DisjunctiveTemporalProblem dtp; // the input DTP
	private static DisjunctiveTemporalProblem dtpOriginal; // the DTP before any
															// decisions were
															// made

	private static int currTime;
	private static int whichToDraw; // keeps track of which draw style the user
									// has requested, 0 (old) or 1 (improved)
	static int numImages = 0; // tracks how many images have been saved to a
								// folder so they can be named
	private static JPanel mouseClickPanel = new JPanel(); // the blue panel at
															// the bottom of the
															// screen with
															// tooltips on it
	static JLabel mouseClickLabel = new JLabel(""); // the label at the bottom
													// of the screen on
													// mouseClickPanel
	private static JPanel content; // the main graph panel
	private static CustomMouseListener listener;

	private static boolean pressed = false;// used in shading non-crucial bars
	private static int crucialWhisker = -1;
	private static String crucialName;
	
	// Drew: Skip display the early hours of the day to focus on important content
	public static int numHoursToSkipAtBeginningOfDay = 4;

	/*
	 * Ensures that the background of any Viz object will be white instead of
	 * the default grey
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(Color.white);
	}

	
	/*
	 * Overrides the default render function for a Graphics object, and allows
	 * rendering mode selection
	 */
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
			paintComponent(g2); // calls the overridden version above
			if (whichToDraw == 0)
				drawStepStyle0(g2);
			else if (whichToDraw == 1)
				drawStepStyle1(g2);
			else if (whichToDraw == 2)
				drawStepStyle2(g2);

		if (pressed) {
			shadeRest(g);
			drawBars(g);
		}
	}
	
	/*
	 * Shades the rest of the bars, and highlight the crucial bar.
	 */
	private static void shadeRest(Graphics g){
		Graphics2D gnew = (Graphics2D) g;
		AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
		gnew.setComposite(alphaComposite);
		gnew.setColor(new Color(255, 255, 255));
		int requiredWidth = TEXT_COLUMN_SIZE + 24 * PIXELS_PER_HOUR + 50;
		int requiredHeight = dtp.getTimepoints().size() / 2 * (HEIGHT_OF_WHISKER + SPACE_BETWEEN_WHISKERS) + 140;
		gnew.fill3DRect(0, 0, requiredWidth, yCoordMaker(crucialWhisker), true);
		gnew.fill3DRect(0, yCoordMaker(crucialWhisker + 1), requiredWidth, requiredHeight, true);
		gnew.setColor(new Color(208, 125, 255));
		gnew.setStroke(new BasicStroke(5));
		gnew.drawLine(0, yCoordMaker(crucialWhisker) - 2, requiredWidth, yCoordMaker(crucialWhisker) - 2);
		gnew.drawLine(0, yCoordMaker(crucialWhisker + 1) - 2, requiredWidth, yCoordMaker(crucialWhisker + 1) - 2);
		alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
		gnew.setComposite(alphaComposite);
	}


	
	private static void drawBars(Graphics g){
		Graphics2D gnew = (Graphics2D) g;
		Vector<ConstraintPack> allTp = new Vector<ConstraintPack>();
		Set<Timepoint> timepoints = dtp.getTimepoints();
		Set<Timepoint> originalTimepoints = dtpOriginal.getTimepoints();
		Object[] originalTps = originalTimepoints.toArray();
		
		String crucialS = crucialName + "_S";
		String crucialE = crucialName + "_E";
		//if(crucialName.equals("work")){
		//	System.out.println("work!");
		//}
		int crucialEst = getEST(dtp, dtp.getTimepoint(crucialS));
		int crucialLet = getLET(dtp, dtp.getTimepoint(crucialE));
		int crucialDuration = getDuration(dtp, dtp.getTimepoint(crucialS), dtp.getTimepoint(crucialE));
		int minDuration = getMinDuration(dtp, dtp.getTimepoint(crucialS), dtp.getTimepoint(crucialE));
		int crucialTimepointID = (dtp.getTimepoint(crucialE).getAbsIndex() / 2) - 1;
		
		AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
		gnew.setComposite(alphaComposite);
		gnew.setStroke(new BasicStroke(3));
		
		gnew.setColor(DARK_PINK);
		gnew.drawLine(pxlCoord(crucialEst), yCoordMaker(crucialTimepointID), pxlCoord(crucialEst), yCoordMaker(crucialTimepointID) + HEIGHT_OF_WHISKER);
		gnew.drawLine(pxlCoord(crucialEst), yCoordMaker(crucialTimepointID), pxlCoord(crucialEst) + 3, yCoordMaker(crucialTimepointID));
		gnew.drawLine(pxlCoord(crucialEst), yCoordMaker(crucialTimepointID) + HEIGHT_OF_WHISKER, pxlCoord(crucialEst) + 3, yCoordMaker(crucialTimepointID) + HEIGHT_OF_WHISKER);
		
		int crucialEet = crucialEst + minDuration;
		gnew.setColor(DARK_GREEN);
		gnew.drawLine(pxlCoord(crucialEet), yCoordMaker(crucialTimepointID), pxlCoord(crucialEet), yCoordMaker(crucialTimepointID) + HEIGHT_OF_WHISKER);
		gnew.drawLine(pxlCoord(crucialEet), yCoordMaker(crucialTimepointID), pxlCoord(crucialEet) - 3, yCoordMaker(crucialTimepointID));
		gnew.drawLine(pxlCoord(crucialEet), yCoordMaker(crucialTimepointID) + HEIGHT_OF_WHISKER, pxlCoord(crucialEet) - 3, yCoordMaker(crucialTimepointID) + HEIGHT_OF_WHISKER);
		
		
		
		Vector<MappedPack> allRelatedPairs = dtp.getRelatedInputPairs(crucialName);
		for(MappedPack it : allRelatedPairs){
			//draw a line from value to key (key is the crucial)
			Timepoint start = dtp.getTimepoint(it.activity + "_S");
			Timepoint end = dtp.getTimepoint(it.activity + "_E");
			int EST = getEST(dtp, start);
			int LET = getLET(dtp, end);
			int EET = getEET(dtp, end);
			int duration = getMinDuration(dtp, start, end);
			duration = Math.max(duration, EET - EST);
			int timepointID = (end.getAbsIndex() / 2) - 1;
			int lineStart; 

			double rubbishTempDiff;
			if(it.keyS){
				if(it.valueS){
					lineStart = EST;
					if(it.gt){
						rubbishTempDiff = dtp.getInterval(start.getName(), crucialS).getLowerBound();
						if(rubbishTempDiff < 0)
							//rubbishTempDiff = 0;
							rubbishTempDiff = dtp.getInterval(end.getName(), crucialS).getUpperBound();
					}
					else
						rubbishTempDiff = dtp.getInterval(start.getName(), crucialS).getUpperBound();
				}
				else{
					lineStart = EST + duration;
					if(it.gt){
						rubbishTempDiff = dtp.getInterval(end.getName(), crucialS).getLowerBound();
						if(rubbishTempDiff < 0)
							rubbishTempDiff = dtp.getInterval(end.getName(), crucialS).getUpperBound();
						//	rubbishTempDiff = 0;
					}
					else{
						rubbishTempDiff = dtp.getInterval(end.getName(), crucialS).getUpperBound();
						//if(rubbishTempDiff < 0)
						//	rubbishTempDiff = 0;
					}
				}
			}
			else{
				if(it.valueS){
					lineStart = EST;
					if(it.gt){
						rubbishTempDiff = dtp.getInterval(start.getName(), crucialE).getLowerBound();
						if(rubbishTempDiff < 0)
							rubbishTempDiff = dtp.getInterval(end.getName(), crucialS).getUpperBound();
						//double rubbishTempDiffU = dtp.getInterval(start.getName(), crucialE).getUpperBound();
						//if((rubbishTempDiff * rubbishTempDiffU <= 0) && rubbishTempDiff != 0)
							//rubbishTempDiff = 0;
						//if(rubbishTempDiff < 0)
						//	rubbishTempDiff = 0;
					}
					else
						rubbishTempDiff = dtp.getInterval(start.getName(), crucialE).getUpperBound();
				}
				else{
					lineStart = EST + duration;
					if(it.gt){
						rubbishTempDiff = dtp.getInterval(end.getName(), crucialE).getLowerBound();
						if(rubbishTempDiff < 0)
							rubbishTempDiff = dtp.getInterval(end.getName(), crucialS).getUpperBound();
						//if(rubbishTempDiff < 0)
						//	rubbishTempDiff = 0;
					}
					else
						rubbishTempDiff = dtp.getInterval(end.getName(), crucialE).getUpperBound();
				}
			}
			if(it.keyS){
				gnew.setColor(DARK_PINK);
			}
			else{
				gnew.setColor(DARK_GREEN);
			}
			
			//special case for synchronized
			if(it.sync){
				lineStart = EST;
				rubbishTempDiff = 0;
				gnew.setColor(DARK_PINK);
				g.drawLine(pxlCoord(lineStart) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2, pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2);
				g.drawLine(pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID), pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER);
				g.drawString("=0", pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff) / 2)) - 6, yCoordMaker(timepointID) + 4);
				lineStart = EST + duration;
				rubbishTempDiff = 0;
				gnew.setColor(DARK_GREEN);
				g.drawLine(pxlCoord(lineStart) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2, pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2);
				g.drawLine(pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID), pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER);
				g.drawString("=0", pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff) / 2)) - 6, yCoordMaker(timepointID) + 4);			
				continue;
			}
			
			String relation;
			if(it.gt)
				relation = ">";
			else
				relation = "<";
			
			if(EST <= crucialEst){
				g.drawLine(pxlCoord(lineStart) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2, pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2);
				g.drawLine(pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID), pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER);
				g.drawString(relation + "=" + Math.abs(rubbishTempDiff), pxlCoord((int) (lineStart + Math.abs(rubbishTempDiff) / 2)) - 6, yCoordMaker(timepointID) + 4);
			}
			else{
				g.drawLine(pxlCoord(lineStart) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2, pxlCoord((int) (lineStart - Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER / 2);
				g.drawLine(pxlCoord((int) (lineStart - Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID), pxlCoord((int) (lineStart - Math.abs(rubbishTempDiff))) + 1, yCoordMaker(timepointID) + HEIGHT_OF_WHISKER);
				g.drawString(relation + "=" + Math.abs(rubbishTempDiff), pxlCoord((int) (lineStart - Math.abs(rubbishTempDiff) / 2)) - 6, yCoordMaker(timepointID) + 4);
			}
		}
	}

	
	/*
	 * Draws a diagram of the schedule on g2 with a representation of the
	 * durations and current EST/LET of each activity
	 */
	public static void drawStepStyle0(Graphics g2) {

		Set<Timepoint> timepoints = dtp.getTimepoints();
		Iterator<Timepoint> it = timepoints.iterator();
		int verticalSpace = 0;
		while (it.hasNext()) {
			Timepoint end = it.next();
			int timepointID = (end.getAbsIndex() / 2) - 1;
			if (end.getName().equals("zero"))
				break;
			Timepoint start = it.next();
			int duration = getDuration(dtp, start, end);
			listener.duration[timepointID] = duration; // informs the mouse
														// listener about the
														// details of this
														// timepoint so tooltips
														// can be provided
			int EST = getEST(dtp, start);
			listener.leftWhisker[timepointID] = EST;
			listener.leftActivity[timepointID] = EST;
			int LET = getLET(dtp, end);
			listener.rightWhisker[timepointID] = LET;
			listener.rightActivity[timepointID] = LET;
			int durationOffset = ((LET - EST) - duration) / 2;
			int leftDuration = EST + durationOffset;
			int rightDuration = LET - durationOffset;
			makeBoxWhisker(g2, timepointID, LET, leftDuration, rightDuration, EST);
			writeTag(g2, timepointID, start.getName().substring(0, start.getName().lastIndexOf('_')));
			listener.activityNames[timepointID] = start.getName().substring(0, start.getName().lastIndexOf('_'));
			verticalSpace++;
		}
		makeScale(g2, verticalSpace);
		drawCurrentTime(g2, verticalSpace);
	}

	/*
	 * Draws a diagram of the schedule on g2 with a representation of the
	 * original EST/LET, current EST/LET, and durations of each activity
	 */
	public static void drawStepStyle2(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		g2.setColor(Color.red);
		g2.drawArc(5, 5, 500, 750, 45, 90 + 45);

		Set<Timepoint> timepoints = dtp.getTimepoints();
		Set<Timepoint> originalTimepoints = dtpOriginal.getTimepoints();
		Object[] originalTps = originalTimepoints.toArray();
		Iterator<Timepoint> it = timepoints.iterator();
		int verticalSpace = 0;
		while (it.hasNext()) {
			Timepoint end = it.next();
			if (end.getName().equals("zero"))
				break;
			Timepoint start = it.next();
			int duration = getDuration(dtp, start, end);
			Timepoint originalStart = null;
			Timepoint originalEnd = null;
			int EST = getOriginalProblemEST(dtp, start);
			int LET = getOriginalProblemLET(dtp, end);
			int LST = getOriginalProblemLST(dtp, start);
			int EET = getOriginalProblemEET(dtp, start);
			if (LET > 10000 || LET == 0) {
				if (LST <= 1440 && LST != 0)
					LET = LST + duration;
				else
					LET = 1440;
			}
			if (EST == 0) {
				if (EET != 0)
					EST = EET - duration;
			}
			int space = LET - EST;
			int durationOffset = ((LET - EST) - duration) / 2;
			int leftDuration = EST + durationOffset;
			int rightDuration = LET - durationOffset;
			int timepointID = (end.getAbsIndex() / 2) - 1;
			int ESToriginal = 0;
			int LEToriginal = 0;
			int originalSpace;
			int rightMinDuration = rightDuration;
			//Seems that we are not using this style2. So I set rightMinDuration = rightDuration 
			Color boxColor = new Color(137, 147, 250);
			for (int i = 0; i < originalTps.length; i++) {
				if (((Timepoint) originalTps[i]).getName().equals(start.getName()))
					originalStart = (Timepoint) originalTps[i];
				if (((Timepoint) originalTps[i]).getName().equals(end.getName()))
					originalEnd = (Timepoint) originalTps[i];
			}

			if (!(originalStart.equals(null) || originalEnd.equals(null))) {
				ESToriginal = EST;
				LEToriginal = LET;
				originalSpace = LEToriginal - ESToriginal;
				double ratio = (double) space / originalSpace;
				if (ratio < .8)
					boxColor = new Color(204, 0, 0);
			}

			if (!(originalStart.equals(null) || originalEnd.equals(null))) {
				makeWhisker(g2, timepointID, ESToriginal, LEToriginal);
				listener.leftWhisker[timepointID] = ESToriginal; // informs the
																	// mouse
																	// listener
																	// about the
																	// details
																	// of this
																	// timepoint
																	// so
																	// tooltips
																	// can be
																	// provided
				listener.rightWhisker[timepointID] = LEToriginal;
			}

			makeNestedBox(g2, timepointID, LET, leftDuration, rightMinDuration, rightDuration, EST, boxColor);
			listener.duration[timepointID] = duration;
			listener.leftActivity[timepointID] = EST;
			listener.rightActivity[timepointID] = LET;

			writeTag(g2, timepointID, start.getName().substring(0, start.getName().lastIndexOf('_')));
			listener.activityNames[timepointID] = start.getName().substring(0, start.getName().lastIndexOf('_'));
			verticalSpace++;
		}
		makeScale(g2, verticalSpace);
		drawCurrentTime(g2, verticalSpace);

	}

	private static int getOriginalProblemEET(DisjunctiveTemporalProblem problem, Timepoint tp) {
		double EET = problem.getOriginalUpperBound(tp);
		return (int) EET * -1;
	}

	private static int getOriginalProblemLST(DisjunctiveTemporalProblem problem, Timepoint tp) {
		double LST = problem.getOriginalLowerBound(tp);
		return (int) LST;
	}

	/*
	 * Get EST and LET of the original problem
	 */
	private static int getOriginalProblemLET(DisjunctiveTemporalProblem problem, Timepoint tp) {
		double LET = problem.getOriginalLowerBound(tp);
		return (int) LET;
	}

	private static int getOriginalProblemEST(DisjunctiveTemporalProblem problem, Timepoint tp) {
		double EST = problem.getOriginalUpperBound(tp);
		return (int) EST * -1;
	}

	/*
	 * Draws a diagram of the schedule on g2 with a representation of the
	 * original problem EST/LET and durations of each activity
	 */
	public static void drawStepStyle1(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Set<Timepoint> timepoints = dtp.getTimepoints();
		Set<Timepoint> originalTimepoints = dtpOriginal.getTimepoints();
		Object[] originalTps = originalTimepoints.toArray();
		Iterator<Timepoint> it = timepoints.iterator();
		int verticalSpace = 0;
		while (it.hasNext()) {
			Timepoint end = it.next();
			AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
			if (end.name.equals("showerM_E")) {
				System.out.println("in dss1");
			}
			g2.setComposite(alphaComposite);
			if (end.getName().equals("zero"))
				break;
			Timepoint start = it.next();
			int duration = getDuration(dtp, start, end);
			int minDuration = getMinDuration(dtp, start, end);
			Timepoint originalStart = null;
			Timepoint originalEnd = null;
			int EST = getEST(dtp, start);
			int LET = getLET(dtp, end);
			int EET = getEET(dtp, end);
			int space = LET - EST;
			//int durationOffset = ((LET - EST) - duration) / 2;
			//int minDurationOffset = ((LET - EST) - minDuration) / 2;
			//int leftDuration = EST + durationOffset;
			//int rightDuration = LET - durationOffset;
			//int rightMinDuration = LET - minDurationOffset;
			int leftDuration = EST;
			int rightDuration = EST + duration;
			int rightMinDuration = Math.max(EET, EST + minDuration);
			int timepointID = (end.getAbsIndex() / 2) - 1;
			int ESToriginal = 0;
			int LEToriginal = 0;
			int originalSpace;
			//Color boxColor = new Color(137, 147, 250);
			Color boxColor = new Color(180, 180, 250);
			for (int i = 0; i < originalTps.length; i++) {
				if (((Timepoint) originalTps[i]).getName().equals(start.getName()))
					originalStart = (Timepoint) originalTps[i];
				if (((Timepoint) originalTps[i]).getName().equals(end.getName()))
					originalEnd = (Timepoint) originalTps[i];
			}

			// change the color of the box bar based on how much it has been constrained relative to original availability
			if (!(originalStart.equals(null) || originalEnd.equals(null))) {
				ESToriginal = getEST(dtpOriginal, originalStart);
				LEToriginal = getLET(dtpOriginal, originalEnd);
				originalSpace = LEToriginal - ESToriginal;
				double ratio = (double) space / originalSpace;
				if (ratio < .8)
					boxColor = new Color(204, 0, 0);
			}

			if (!(originalStart.equals(null) || originalEnd.equals(null))) {
				makeWhisker(g2, timepointID, ESToriginal, LEToriginal);
				
				
				// DREW: DEBUG code
				if (listener.leftWhisker.length < timepointID-1) {
					System.out.println("About to get out of bound error...");
				}
				
				
				
				
				listener.leftWhisker[timepointID] = ESToriginal; // informs the
																	// mouse
																	// listener
																	// about the
																	// details
																	// of this
																	// timepoint
																	// so
																	// tooltips
																	// can be
																	// provided
				listener.rightWhisker[timepointID] = LEToriginal;
			}

			makeNestedBox(g2, timepointID, LET, leftDuration, rightMinDuration, rightDuration, EST, boxColor);
			listener.duration[timepointID] = duration;
			listener.leftActivity[timepointID] = EST;
			listener.rightActivity[timepointID] = LET;

			writeTag(g2, timepointID, start.getName().substring(0, start.getName().lastIndexOf('_')));
			listener.activityNames[timepointID] = start.getName().substring(0, start.getName().lastIndexOf('_'));
			verticalSpace++;
		}
		makeScale(g2, verticalSpace);
		drawCurrentTime(g2, verticalSpace);
	}

	
	
	/*
	 * Author: Drew Davis
	 * Adapted from the drawStepStyle1 function
	 * This function is used to collect the data necessary to plot out the gantt chart using xcode side plotting
	 * Returns an ArrayList (index = agent) of Hashmaps (property name, ArrayList of values for each activity)
	 */
	public static ArrayList< HashMap< String, ArrayList< String > > > retrievePlotData(DisjunctiveTemporalProblem dtpIn, DisjunctiveTemporalProblem dtpOriginalIn,
			int timestamp) {

		dtp = dtpIn;
		dtpOriginal = dtpOriginalIn;
		currTime = timestamp;
		maxActivities = dtpIn.getTimepoints().size();
		
		Set<Timepoint> timepoints = dtp.getTimepoints();
		Set<Timepoint> originalTimepoints = dtpOriginal.getTimepoints();
		Object[] originalTps = originalTimepoints.toArray();
		Iterator<Timepoint> it = timepoints.iterator();


		ArrayList< HashMap< String, ArrayList< String > > > resultsPerAgent = new ArrayList< HashMap< String, ArrayList< String > > >( dtp.getNumAgents() );
		
		
		// for each agent index in results, initialize it to have all hashmap fields
		for (int i = 0; i < dtp.getNumAgents(); i++) {
			resultsPerAgent.add(new HashMap< String, ArrayList< String > >());
			resultsPerAgent.get(i).put("actNames", new ArrayList<String>());
			resultsPerAgent.get(i).put("actIDs", new ArrayList<String>());
			resultsPerAgent.get(i).put("actESTs", new ArrayList<String>());
			resultsPerAgent.get(i).put("actLETs", new ArrayList<String>());
			resultsPerAgent.get(i).put("actMinDurs", new ArrayList<String>());
			resultsPerAgent.get(i).put("actMaxDurs", new ArrayList<String>());
			resultsPerAgent.get(i).put("actRestricts", new ArrayList<String>());
			resultsPerAgent.get(i).put("currentTime", new ArrayList<String>());
		}
		
		
		// iterate through each end/start timepoint
		while (it.hasNext()) {
			Timepoint end = it.next();
			AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
			if (end.name.equals("showerM_E")) {
				System.out.println("in dss1");
			}
			
			if (end.getName().equals("zero"))
				break;
			Timepoint start = it.next();
			int tempAgentNum = dtp.getAgent(start.name);
			int duration = getDuration(dtp, start, end);
			int minDuration = getMinDuration(dtp, start, end);
			
			Timepoint originalStart = null;
			Timepoint originalEnd = null;
			int EST = getEST(dtp, start);
			int LET = getLET(dtp, end);
			int EET = getEET(dtp, end);
			int space = LET - EST;
			
			//int durationOffset = ((LET - EST) - duration) / 2;
			//int minDurationOffset = ((LET - EST) - minDuration) / 2;
			//int leftDuration = EST + durationOffset;
			//int rightDuration = LET - durationOffset;
			//int rightMinDuration = LET - minDurationOffset;
			int leftDuration = EST;
			int rightDuration = EST + duration;
			int rightMinDuration = Math.max(EET, EST + minDuration); // earliest legal end time
			int timepointID = (end.getAbsIndex() / 2) - 1;
			int ESToriginal = 0;
			int LEToriginal = 0;
			int originalSpace;
			//Color boxColor = new Color(137, 147, 250);
			Color boxColor = new Color(180, 180, 250);
			for (int i = 0; i < originalTps.length; i++) {
				if (((Timepoint) originalTps[i]).getName().equals(start.getName()))
					originalStart = (Timepoint) originalTps[i];
				if (((Timepoint) originalTps[i]).getName().equals(end.getName()))
					originalEnd = (Timepoint) originalTps[i];
			}

			double ratio = 1.0;
			
			// change the color of the box bar based on how much it has been constrained relative to original availability
			if (!(originalStart.equals(null) || originalEnd.equals(null))) {
				ESToriginal = getEST(dtpOriginal, originalStart);
				LEToriginal = getLET(dtpOriginal, originalEnd);
				originalSpace = LEToriginal - ESToriginal;
				
				ratio = (double) space / originalSpace;
			}
			
			resultsPerAgent.get(tempAgentNum).get("actNames").add( start.getName().substring(0, start.getName().lastIndexOf('_')) );
			resultsPerAgent.get(tempAgentNum).get("actIDs").add( String.valueOf(timepointID) );
			resultsPerAgent.get(tempAgentNum).get("actESTs").add( String.valueOf( EST ) );
			resultsPerAgent.get(tempAgentNum).get("actLETs").add( String.valueOf( LET ) );
			resultsPerAgent.get(tempAgentNum).get("actMinDurs").add( String.valueOf( minDuration ) );
//			resultsPerAgent.get(tempAgentNum).get("actMinDurs").add( String.valueOf( rightMinDuration - leftDuration ) );
			resultsPerAgent.get(tempAgentNum).get("actMaxDurs").add( String.valueOf( duration ) );
//			resultsPerAgent.get(tempAgentNum).get("actMaxDurs").add( String.valueOf( rightDuration - leftDuration ) );
			resultsPerAgent.get(tempAgentNum).get("actRestricts").add( String.valueOf(ratio) );
			
		}

		return resultsPerAgent;
	}
	
	
	/* Increases the height of the window by amount pixels */
	public static void bumpSize(Window g, int amount) {
		g.setSize(g.getWidth(), g.getHeight() + amount);
	}

	/*
	 * Converts number of minutes into the day into pixel coordinate x position
	 * on the diagram
	 */
	static int pxlCoord(int timestamp) {
		double pixPerMinute = (double) PIXELS_PER_HOUR / 60;
		double coord = pixPerMinute * timestamp - (numHoursToSkipAtBeginningOfDay * 60 * pixPerMinute);
		return (int) coord + TEXT_COLUMN_SIZE;
	}

	/* Converts the row of data on the graph into a y pixel coordinate */
	static int yCoordMaker(int whichWhisker) {
		int yCoord = (whichWhisker + 1) * (SPACE_BETWEEN_WHISKERS + HEIGHT_OF_WHISKER);
		return yCoord;
	}

	/* converts a time in minutes past midnight into a HH:mm format */
	public static String timeFormat(int timestamp) {
		int hours = timestamp / 60;
		int minutes = timestamp % 60;
		String parsable = Integer.toString(hours) + ":" + Integer.toString(minutes);
		Date date = new Date();
		try {
			date = new SimpleDateFormat("HH:mm").parse(parsable);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new SimpleDateFormat("HH:mm").format(date);
	}

	/* Creates a scale at row whichLevel, then draws row separators */
	public static void makeScale(Graphics g, int whichLevel) {
		g.setColor(Color.black);
		int yCoord = yCoordMaker(whichLevel) + 8;
		int originXCoord = pxlCoord(numHoursToSkipAtBeginningOfDay * 60 + 90);
		g.drawLine(pxlCoord(originXCoord), yCoord, pxlCoord(1440), yCoord);
		g.drawLine(pxlCoord(originXCoord), yCoordMaker(0), pxlCoord(originXCoord), yCoord);
		int pxl = 0;
		for (int i = 0 + numHoursToSkipAtBeginningOfDay; i < 25; i++) {
			pxl = pxlCoord(i * 60);
			g.drawLine(pxl, yCoord - 5, pxl, yCoord + 2);
			g.setFont(new Font("helvetica", Font.BOLD, 14));
			g.drawString(Integer.toString(i), pxl - 4, yCoord + 16);
		}
		g.setColor(FAINT_BLUE);
		g.drawLine(pxl, yCoordMaker(0), pxl, yCoord); // these are the faint
														// lines separating the
														// rows
	}

	/*
	 * Writes a label on the left side of the graph at row whichWhisker with a
	 * faint blue line beneath it
	 */
	public static void writeTag(Graphics g, int whichWhisker, String whatToSay) {
		g.setColor(FAINT_BLUE);
		g.drawLine(0, yCoordMaker(whichWhisker) + HEIGHT_OF_WHISKER + SPACE_BETWEEN_WHISKERS / 2, pxlCoord(1440),
				yCoordMaker(whichWhisker) + HEIGHT_OF_WHISKER + SPACE_BETWEEN_WHISKERS / 2);
		g.setColor(Color.black);
		Font labelFont = new Font("helvetica", Font.PLAIN, 14);
		g.setFont(labelFont);
		g.drawString(whatToSay, LABEL_X_POSITION, yCoordMaker(whichWhisker) + 16);
		g.setColor(Color.lightGray);
	}

	/*
	 * Creates a box and whisker diagram at row whichWhisker. The internal box
	 * changes color if its size is close to the size of the whiskers.
	 */
	public static void makeBoxWhisker(Graphics g, int whichWhisker, int rightBound, int leftIntBound, int rightIntBound,
			int leftBound) {
		int endTime = rightBound;
		rightBound = pxlCoord(rightBound);
		leftIntBound = pxlCoord(leftIntBound);
		rightIntBound = pxlCoord(rightIntBound);
		leftBound = pxlCoord(leftBound);
		int yCoord = yCoordMaker(whichWhisker);
		g.setColor(Color.black);
		// g.drawLine(leftBound, yCoord + HEIGHT_OF_WHISKER / 2, rightBound,
		// yCoord + HEIGHT_OF_WHISKER / 2);
		g.drawLine(leftBound, yCoord, leftBound, yCoord + HEIGHT_OF_WHISKER);
		g.drawLine(rightBound, yCoord, rightBound, yCoord + HEIGHT_OF_WHISKER);
		int boundWidth = rightBound - leftBound;
		int rectWidth = rightIntBound - leftIntBound;
		double ratio = (double) rectWidth / boundWidth;
		if (endTime <= currTime || rectWidth == 0)
			g.setColor(Color.DARK_GRAY);
		else if (ratio < ORANGE_CUTOFF)
			g.setColor(Color.blue);
		else if (ratio > ORANGE_CUTOFF && ratio < RED_CUTOFF)
			g.setColor(Color.orange);
		else if (ratio > RED_CUTOFF)
			g.setColor(Color.red);
		g.fill3DRect(leftIntBound, yCoord, rectWidth, HEIGHT_OF_WHISKER, true);
	}

	/* Draws a whisker at row whichWhisker */
	public static void makeWhisker(Graphics g, int whichWhisker, int leftBound, int rightBound) {
		rightBound = pxlCoord(rightBound);
		leftBound = pxlCoord(leftBound);
		int yCoord = yCoordMaker(whichWhisker);
		g.setColor(Color.black);
		// g.drawLine(leftBound, yCoord + HEIGHT_OF_WHISKER / 2, rightBound,
		// yCoord + HEIGHT_OF_WHISKER / 2);
		g.drawLine(leftBound, yCoord, leftBound, yCoord + HEIGHT_OF_WHISKER);
		g.drawLine(rightBound, yCoord, rightBound, yCoord + HEIGHT_OF_WHISKER);
	}

	/* Draws a nested box at row whichWhisker */
	public static void makeNestedBox(Graphics g, int whichWhisker, int rightBound, int leftIntBound,
			int rightMinIntBound, int rightIntBound, int leftBound, Color boxColor) {
		//public static void makeNestedBox(Graphics g, int whichWhisker, int rightMinBound, int minRightIntBound, int rightBound, int rightIntBound,
		//int leftBound, int leftIntBound, Color boxColor) {
		int endTime = rightBound;
		rightBound = pxlCoord(rightBound);
		leftIntBound = pxlCoord(leftIntBound);
		rightIntBound = pxlCoord(rightIntBound);
		rightMinIntBound = pxlCoord(rightMinIntBound);
		leftBound = pxlCoord(leftBound);
		int yCoord = yCoordMaker(whichWhisker);
		int boundWidth = rightBound - leftBound;
		int rectWidth = rightIntBound - leftIntBound;

		if (endTime <= currTime || rectWidth < 3) {
			boxColor = new Color(100, 100, 100, 0); // black
		}
		g.setColor(boxColor);
		g.fill3DRect(leftBound, yCoord, rightBound - leftBound, HEIGHT_OF_WHISKER, true);

		double ratio = (double) rectWidth / boundWidth;
		
		//for the maxDuration
		
		if (endTime <= currTime || rectWidth < 3)
			g.setColor(Color.DARK_GRAY);
		else if (ratio < ORANGE_CUTOFF)
			g.setColor(new Color(130, 130, 250)); // blue/purp?
		else if (ratio > ORANGE_CUTOFF && ratio < RED_CUTOFF)
			g.setColor(Color.yellow);
		else if (ratio > RED_CUTOFF)
			g.setColor(Color.pink);
		// g.fill3DRect(leftIntBound, yCoord, rectWidth, HEIGHT_OF_WHISKER,
		// true);
		g.fill3DRect(leftBound, yCoord, rectWidth, HEIGHT_OF_WHISKER, true);
		
		//for the minDuration
		
		if (endTime <= currTime || rectWidth < 3)
			g.setColor(Color.DARK_GRAY);
		else if (ratio < ORANGE_CUTOFF)
			g.setColor(Color.blue);
		else if (ratio > ORANGE_CUTOFF && ratio < RED_CUTOFF)
			g.setColor(Color.orange);
		else if (ratio > RED_CUTOFF)
			g.setColor(Color.red);
		g.fill3DRect(leftBound, yCoord, rightMinIntBound - leftIntBound, HEIGHT_OF_WHISKER, true);
	}

	/* Draws a dotted red line at the current timestamp */
	public static void drawCurrentTime(Graphics g, int depth) {
		int xCoord = pxlCoord(currTime);
		g.setColor(Color.RED);
		for (int i = 0; i <= depth * 4; i += 2) {
			g.drawLine(xCoord, yCoordMaker(i) / 4 + 32, xCoord, yCoordMaker(i - 1) / 4 + 32);
		}
	}

	/*
	 * Returns the earliest start time for an activity start timepoint in
	 * problem
	 */
	public static int getEST(DisjunctiveTemporalProblem problem, Timepoint tp) {
		String timepointName = tp.getName();
		IntervalSet startTime = problem.getInterval("zero", timepointName);
		double EST = startTime.getUpperBound();
		return (int) EST * -1; // rounding is for ease of rendering. The maximum
								// change to the final diagram is a pixel or
								// less depending on scaling.
	}

	/* Returns the latest end time for an activity end timepoint in problem */
	public static int getLET(DisjunctiveTemporalProblem problem, Timepoint tp) {
		String timepointName = tp.getName();
		IntervalSet startTime = problem.getInterval("zero", timepointName);
		double LET = startTime.getLowerBound();
		return (int) LET * -1; // rounding is for ease of rendering. The maximum
								// change to the final diagram is a pixel or
								// less depending on scaling.
	}
	
	public static int getEET(DisjunctiveTemporalProblem problem, Timepoint tp) {
		String timepointName = tp.getName();
		IntervalSet startTime = problem.getInterval("zero", timepointName);
		double LET = startTime.getUpperBound();
		return (int) LET * -1; // rounding is for ease of rendering. The maximum
								// change to the final diagram is a pixel or
								// less depending on scaling.
	}

	/*
	 * Returns the minimum duration between the start and end timepoints of an
	 * activity within problem
	 */
	public static int getDuration(DisjunctiveTemporalProblem problem, Timepoint start, Timepoint end) {
		String beginName = start.getName();
		String endName = end.getName();
		IntervalSet startTime = problem.getInterval(beginName, endName);
		double duration = startTime.getLowerBound();
		return (int) duration * -1; // rounding is for ease of rendering. The
									// maximum change to the final diagram is a
									// pixel or less depending on scaling.
	}
	
	public static int getMinDuration(DisjunctiveTemporalProblem problem, Timepoint start, Timepoint end) {
		String beginName = start.getName();
		String endName = end.getName();
		IntervalSet startTime = problem.getInterval(beginName, endName);
		double duration = startTime.getUpperBound();
		return (int) duration * -1; // rounding is for ease of rendering. The
									// maximum change to the final diagram is a
									// pixel or less depending on scaling.
	}

	/*
	 * Initiates a render of a window containing a DTP diagram. printStyle 0
	 * features the current state, while printStyle 1 features the original
	 * problem state in the whiskers.
	 */
	public static void printDTPDiagram(DisjunctiveTemporalProblem dtpIn, DisjunctiveTemporalProblem dtpOriginalIn,
			int timestamp, int printStyle) {
		dtp = dtpIn;
		dtpOriginal = dtpOriginalIn;
		currTime = timestamp;
		whichToDraw = printStyle;
		maxActivities = dtpIn.getTimepoints().size();

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				render();
			}
		});
	}
	
	/*
	 * Generate a png image of a gantt for the current dtp
	 * Author: Drew Davis
	 */
	public static void createAndSaveDTPDiagram(DisjunctiveTemporalProblem dtpIn, DisjunctiveTemporalProblem dtpOriginalIn,
			int timestamp, int printStyle) {
		dtp = dtpIn;
		dtpOriginal = dtpOriginalIn;
		currTime = timestamp;
		whichToDraw = printStyle;
		maxActivities = dtpIn.getTimepoints().size();

		JFrame j = renderAndReturn();
		
		// save image
		BufferedImage bi = new BufferedImage(j.getSize().width-1, j.getSize().height-1, BufferedImage.TYPE_INT_ARGB); 
		Graphics g = bi.createGraphics();
		j.paint(g);  //this == JComponent
		g.dispose();
		
		// Because the drawing is done on many separate threads, we need to make sure all threads finish before saving it
		// This is the simple temporary solution
		try{Thread.sleep(2000);}catch (Exception e) {System.out.println(e);}
		try{ImageIO.write(bi,"png",new File("forClient_image.png"));}catch (Exception e) {System.out.println(e);}
				

	}
	
	
	
	
	
	/*
	 * helper function for printDTPDiagram in order to make it thread safe using
	 * SwingUtilities.invokeLater
	 * This render function is same as render(f) but ALSO saves the image
	 */
	private static JFrame renderAndReturn() {
		JFrame f = new JFrame();

		f.setBackground(Color.white);
		f.setTitle("Schedule at Timestamp " + currTime);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		int requiredWidth = TEXT_COLUMN_SIZE + (24 - numHoursToSkipAtBeginningOfDay) * PIXELS_PER_HOUR + 50;
		int requiredHeight = (int) (Math.ceil(dtp.getTimepoints().size() / 2.0)) * (HEIGHT_OF_WHISKER + SPACE_BETWEEN_WHISKERS) + 60; //+ 140;
		f.setSize(requiredWidth, requiredHeight);
		f.setVisible(true);
		f.setOpacity(1);
		content = new Viz();
		content.setOpaque(true);
		content.setBackground(Color.white);
		
//		createMenuBar(f);
		content.setLayout(new BorderLayout());
		f.add(content, BorderLayout.CENTER);
		mouseClickPanel.add(mouseClickLabel);
		mouseClickPanel.setBackground(Color.white);
		f.add(mouseClickPanel, BorderLayout.SOUTH);
		listener = ((Viz) content).new CustomMouseListener();
		listener.initializeArrays();
		f.addMouseListener(listener);
		f.setVisible(true);
		
		return f;
	}
		
	

	/*
	 * helper function for printDTPDiagram in order to make it thread safe using
	 * SwingUtilities.invokeLater
	 */
	private static void render() {
		JFrame f = new JFrame();

		f.setBackground(Color.white);
		f.setTitle("Schedule at Timestamp " + currTime);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		int requiredWidth = TEXT_COLUMN_SIZE + 24 * PIXELS_PER_HOUR + 50;
		int requiredHeight = dtp.getTimepoints().size() / 2 * (HEIGHT_OF_WHISKER + SPACE_BETWEEN_WHISKERS) + 140;
		f.setSize(requiredWidth, requiredHeight);
		f.setVisible(true);
		f.setOpacity(1);
		content = new Viz();
		content.setOpaque(true);
		content.setBackground(Color.white);
		createMenuBar(f);
		content.setLayout(new BorderLayout());
		f.add(content, BorderLayout.CENTER);
		mouseClickPanel.add(mouseClickLabel);
		mouseClickPanel.setBackground(Color.white);
		f.add(mouseClickPanel, BorderLayout.SOUTH);
		listener = ((Viz) content).new CustomMouseListener();
		listener.initializeArrays();
		f.addMouseListener(listener);
		f.setVisible(true);
	}

	/* creates the menus at the top of the window */
	private static void createMenuBar(final JFrame f) {
		JMenuBar menubar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenu view = new JMenu("View");
		JMenu settings = new JMenu("Settings");

		JMenuItem saver = new JMenuItem("Save as PNG");
		JMenuItem quitter = new JMenuItem("Quit");

		final JCheckBoxMenuItem classic = new JCheckBoxMenuItem("Classic view");

		JMenuItem setPath = new JMenuItem("Set Save Path");

		saver.setToolTipText("Save a screenshot of this diagram in the project folder");
		quitter.setToolTipText("Quit vizualization");
		setPath.setToolTipText("Choose which folder to save into");
		KeyStroke controlA = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK);
		KeyStroke controlQ = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK);
		saver.setAccelerator(controlA);
		quitter.setAccelerator(controlQ);

		saver.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					if (targetPath.length() == 0)
						ScreenImage.writeImage(ScreenImage.createImage(f),
								"savedImage_" + Integer.toString(numImages) + ".png");
					else
						ScreenImage.writeImage(ScreenImage.createImage(f),
								targetPath + "//savedImage_" + Integer.toString(numImages) + ".png");

					numImages++;
				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				} catch (AWTException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		quitter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		classic.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				AbstractButton fakeButton = (AbstractButton) event.getSource();
				boolean selected = fakeButton.getModel().isSelected();
				if (selected) {
					whichToDraw = 0;
					content = new Viz();
					f.getContentPane().add(content, BorderLayout.CENTER);

					mouseClickPanel = new JPanel();
					mouseClickPanel.add(mouseClickLabel);
					mouseClickPanel.setBackground(Color.RED);
					f.getContentPane().add(mouseClickPanel, BorderLayout.SOUTH);

					// f.getContentPane().add(content, BorderLayout.CENTER);
					// f.getContentPane().add(mouseClickPanel,
					// BorderLayout.SOUTH);
				} else {

					mouseClickPanel = new JPanel();
					mouseClickPanel.add(mouseClickLabel);
					f.getContentPane().add(mouseClickPanel, BorderLayout.SOUTH);

					whichToDraw = 1;
					content = new Viz();
					f.getContentPane().add(content, BorderLayout.CENTER);

					// f.getContentPane().add(content, BorderLayout.CENTER);
					// f.getContentPane().add(mouseClickPanel,
					// BorderLayout.SOUTH);
				}

			}
		});

		setPath.addActionListener(new ActionListener() {
			class textPopup {
				String completedMessage = null;
				JFrame textWindow = null;
				JLabel promptLabel = null;
				JTextField textBox = new JTextField(20);

				public textPopup(String prompt, String confirm) {
					completedMessage = confirm;
					textBox.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent event) {
							targetPath = textBox.getText(); // this is a static
															// variable of Viz
							promptLabel.setText(completedMessage);
						}

					});

					textWindow = new JFrame();
					promptLabel = new JLabel(prompt);
					textWindow.getContentPane().setLayout(new BoxLayout(textWindow.getContentPane(), BoxLayout.Y_AXIS));
					textWindow.getContentPane().add(promptLabel);
					textWindow.getContentPane().add(textBox);
					textWindow.getContentPane().setBackground(Color.WHITE);
					textWindow.pack();
					textWindow.setVisible(true);

				}
			}

			@Override
			public void actionPerformed(ActionEvent event) {
				new textPopup("Input Path Below", "Path Set!");
			}
		});

		view.add(classic);
		file.add(saver);
		file.add(quitter);
		settings.add(setPath);
		menubar.add(file);
		menubar.add(settings);
		menubar.add(view); // This function does not satisfy layering
							// constraints yet, so I can't enable it.
		f.setJMenuBar(menubar);
	}

	/*
	 * Handles mouse clicks on the interface, and holds a secondary account of
	 * the relevant information so that it can make tooltips available when
	 * specific elements are clicked on. The DrawStepStyle1 and DrawStepStyle0
	 * classes must fill up the arrays or else the tooltip information will
	 * default to zeros and "INVALID"
	 */
	class CustomMouseListener implements MouseListener {
		int[] leftWhisker = new int[maxActivities];
		int[] rightWhisker = new int[maxActivities];
		int[] duration = new int[maxActivities];
		int[] leftActivity = new int[maxActivities];
		int[] rightActivity = new int[maxActivities];
		String[] activityNames = new String[maxActivities];

		public void initializeArrays() {
			for (int i = 0; i < maxActivities; i++) {
				leftWhisker[i] = rightWhisker[i] = duration[i] = leftActivity[i] = rightActivity[i] = 0;
				activityNames[i] = "INVALID";
			}
		}

		/* converts the current y pixel coordinate to a row coordinate */
		private double translateRow(int yPixel) {
			yPixel -= 14;// I found that the pixel is a little higher. not sure
							// if it only happens on my machine
			double ans = (double) yPixel / (SPACE_BETWEEN_WHISKERS + HEIGHT_OF_WHISKER);
			if (System.getProperty("os.name").startsWith("Windows"))
				return Math.floor(ans + WINDOWS_ROW_OFFSET);
			if (System.getProperty("os.name").startsWith("Linux"))
				return Math.floor(ans + LINUX_ROW_OFFSET);
			return -1;

		}

		/* converts the current x pixel coordinate to a time coordinate */
		private int translateHorizontalPixel(int xPixel) {

			double pixPerMinute = (double) PIXELS_PER_HOUR / 60;
			double answer = xPixel - TEXT_COLUMN_SIZE;
			answer /= pixPerMinute;
			return (int) answer - 10;
		}

		public void mouseClicked(MouseEvent e) {
			// do nothing, all relevant actions are in mousePressed so it can be
			// cleared when the mouse is released
		}

		public void mousePressed(MouseEvent e) {
			mouseClickPanel.setVisible(false);
			int rowClicked = (int) translateRow(e.getY());
			int timestampClicked = translateHorizontalPixel(e.getX());
			if (rowClicked >= 0 && timestampClicked > leftWhisker[rowClicked]
					&& timestampClicked < rightWhisker[rowClicked] && crucialWhisker != rowClicked) {
				//make mouse panel appear or shift to this one
				crucialWhisker = rowClicked;
				crucialName = activityNames[rowClicked];
				mouseClickPanel.setBackground(new Color(200, 200, 250));
				mouseClickLabel.setFont(new Font("helvetica", Font.BOLD, 12));
				mouseClickLabel.setText("\"" + activityNames[rowClicked] + "\"- Left whisker = "
						+ timeFormat(leftWhisker[rowClicked]) + ", Box Left = " + timeFormat(leftActivity[rowClicked])
						+ ", Box Right = " + timeFormat(rightActivity[rowClicked]) + ", Right whisker = "
						+ timeFormat(rightWhisker[rowClicked]) + ", Duration = " + timeFormat(duration[rowClicked])
						+ ".");

				pressed = true;
				content.repaint();
				mouseClickPanel.setVisible(true);
			}
			else{
				crucialWhisker = -1;
				mouseClickPanel.setBackground(FAINT_BLUE);
				// mouseClickLabel.setFont(new Font("helvetica", Font.ITALIC, 0));
				mouseClickPanel.setVisible(false);
				pressed = false;
				mouseClickPanel.repaint();
			}
		}

		/* when the mouse button is released, hide the tooltip again */
		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {

		}

		/* when the mouse leaves the window, hide the tooltip */
		public void mouseExited(MouseEvent e) {
			// mouseClickLabel.setFont(new Font("helvetica", Font.ITALIC, 0));
			mouseClickPanel.setVisible(false);

		}

	}

}

class ConstraintPack{
	public int tpID;
	public int tempDiff;
	public int est;
	public boolean gt;//true if greater than, false if less than
	
	public ConstraintPack(int inputTpID, int inputTempDiff, int inputEst, boolean inputGt){
		tpID = inputTpID;
		tempDiff = inputTempDiff;
		est = inputEst;
		gt = inputGt;
	}
}
