package com.jug;

/**
 * Main class for the NordenTools-OrthoSlicer project.
 */

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Plot;
import ij.gui.Roi;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.imglib2.Cursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.data.Circle;
import com.jug.data.Ellipse2D;
import com.jug.data.PixelCloud2D;
import com.jug.data.PlotData;
import com.jug.data.Vector2D;
import com.jug.util.DumpImgFactory;

/**
 * @author jug
 */
public class OrthoSlicer
{

	private static OrthoSlicer main;

	static int WISH_SUM_INTENSITIES = 0;

	static int WISH_CONCENTRATION = 1;

	static int WISH_VOLUME = 2;

	static int NORMALIZE_NONE = 0;

	static int NORMALIZE_AREA = 1;

	static int NORMALIZE_CIRCUMFERENCE = 2;

	public ImageJ ij;

	public ImagePlus imgPlus = null;

	public OrthoSlicer()
	{
		main = this;
	}

	/**
	 * PROJECT MAIN ============
	 * 
	 * @param args
	 *            muh!
	 */
	public static void main( final String[] args )
	{
		ImageJ temp = IJ.getInstance();

		if ( temp == null )
		{
			temp = new ImageJ();
			// IJ.open(
			// "/Users/jug/MPI/ProjectNorden/FirstSegmentation_crop.tif" );
			IJ.open( "/Users/jug/MPI/ProjectNorden/FirstSegmentation_crop_hull_4.0.tif" );
		}

		new OrthoSlicer();

		if ( !main.loadCurrentImage() ) { return; }

		final Line lineRoi = new Line( 0, main.imgPlus.getHeight(), main.imgPlus.getWidth(), 0 );
		main.imgPlus.setRoi( lineRoi, true );

		// main.askUserForShapeExportDetails();
		main.projectConcentrationToLine( true );
	}

	/**
	 * @param line
	 */
	public void askUserForShapeExportDetails()
	{

		final boolean onlyCurrent = true;
		final boolean exportCSVData = false;
		final boolean dumpPointClouds = false;
		final boolean exportCircles = false;
		final boolean exportEllipses = true;
		final int toNormalize = NORMALIZE_NONE;

		imgPlus = WindowManager.getCurrentImage();
		if ( imgPlus == null )
		{
			IJ.error( "There must be an active, open image window!" );
			return;
		}

		Line roi = null;
		try
		{
			roi = ( Line ) imgPlus.getRoi();
			if ( roi == null )
			{
				IJ.error( "A line-ROI must be drawn in active image window." );
				return;
			}
		}
		catch ( final ClassCastException e )
		{
			e.printStackTrace();
			IJ.error( "Found a non-line ROI... please use the line-tool to indicate the slicing direction!" );
		}
		final Line line = roi;

		final JFrame frame = new JFrame( "Select Options..." );
		final JPanel gui = new JPanel( new GridBagLayout() );
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		final JCheckBox cbS = new JCheckBox( "only current timepoint", onlyCurrent );
		final JCheckBox cbD = new JCheckBox( "export CSV-data", exportCSVData );
		final JCheckBox cbP = new JCheckBox( "dump point-clouds", dumpPointClouds );
		gui.add( cbS, gbc );
		gui.add( cbD, gbc );
		gui.add( cbP, gbc );

		final JLabel lblShapes = new JLabel( "Shape approximations:" );
		lblShapes.setBorder( BorderFactory.createEmptyBorder( 10, 0, 0, 0 ) );
		gui.add( lblShapes, gbc );
		final JCheckBox cbC = new JCheckBox( "circular", exportCircles );
		final JCheckBox cbE = new JCheckBox( "elliptical", exportEllipses );
		gui.add( cbC, gbc );
		gui.add( cbE, gbc );

		final JLabel lblNorm = new JLabel( "Normalisation:" );
		lblNorm.setBorder( BorderFactory.createEmptyBorder( 10, 0, 0, 0 ) );
		gui.add( lblNorm, gbc );
		final ButtonGroup bg = new ButtonGroup();
		final JRadioButton rbNormNone = new JRadioButton( "only basic fit", ( toNormalize == NORMALIZE_NONE ) );
		final JRadioButton rbNormArea = new JRadioButton( "norm slice area", ( toNormalize == NORMALIZE_AREA ) );
		final JRadioButton rbNormCirc = new JRadioButton( "norm slice circumference", ( toNormalize == NORMALIZE_CIRCUMFERENCE ) );
		bg.add( rbNormNone );
		bg.add( rbNormArea );
		bg.add( rbNormCirc );
		gui.add( rbNormNone, gbc );
		gui.add( rbNormArea, gbc );
		gui.add( rbNormCirc, gbc );

		final JPanel buttons = new JPanel();
		buttons.setLayout( new BoxLayout( buttons, BoxLayout.LINE_AXIS ) );
		final JButton btnCancel = new JButton( "Cancel" );
		btnCancel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent evt )
			{
				frame.dispose();
			}
		} );
		final JButton btnOk = new JButton( "Ok" );
		btnOk.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent evt )
			{
				frame.dispose();

				File path = null; // output folder for CSV-data...
				if ( cbD.isSelected() )
				{
					final JFileChooser chooser = new JFileChooser();

					chooser.setCurrentDirectory( new File( imgPlus.getOriginalFileInfo().directory ) );
					chooser.setDialogTitle( "Choose export folder..." );
					chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
					chooser.setAcceptAllFileFilterUsed( false );
					chooser.setMultiSelectionEnabled( false );

					if ( chooser.showSaveDialog( IJ.getInstance() ) == JFileChooser.APPROVE_OPTION )
					{
						path = chooser.getSelectedFile();
					}
				}

				int n = 0;
				if ( rbNormNone.isSelected() )
				{
					n = NORMALIZE_NONE;
				}
				else if ( rbNormArea.isSelected() )
				{
					n = NORMALIZE_AREA;
				}
				else
				{
					n = NORMALIZE_CIRCUMFERENCE;
				}

				int showFrameNum = -1; // this means 'show all'
				if ( cbS.isSelected() )
				{
					showFrameNum = imgPlus.getFrame() - 1; // one-based index...
															// (puke)
				}

				fitShapesToOrthogonalSections( showFrameNum, line, cbP.isSelected(), cbC.isSelected(), cbE.isSelected(), n, path );
			}
		} );
		buttons.add( Box.createHorizontalGlue() );
		buttons.add( btnCancel );
		buttons.add( btnOk );
		gui.add( buttons );
		frame.getContentPane().add( gui, BorderLayout.CENTER );
		frame.getContentPane().add( buttons, BorderLayout.SOUTH );
		frame.pack();
		frame.setSize( ( int ) gui.getPreferredSize().getWidth() + 20, ( int ) gui.getPreferredSize().getHeight() + 70 );
		frame.setLocation( ( IJ.getScreenSize().width - frame.getWidth() ) / 2, ( IJ.getScreenSize().height - frame.getHeight() ) / 2 );
		frame.getRootPane().setDefaultButton( btnOk );
		frame.setVisible( true );
	}

	public void fitShapesToOrthogonalSections( final int frameNumToShow, final Line line, final boolean exportPointClouds, final boolean exportCircles, final boolean exportEllipses, final int toNormalize, final File path )
	{

		int progressIndex = 1;
		final int stepsToDo = 7;

		// extract point (voxel) coulds along line-ROI)
		IJ.showStatus( "Generating Point-Clouds along line-ROI..." );
		IJ.showProgress( progressIndex++, stepsToDo );
		final double voxel_depth = this.imgPlus.getCalibration().getZ( 1.0 );
		final List< List< PixelCloud2D< Double >>> allPointClouds = getOrthogonalPointClouds( line, voxel_depth );

		// create new Img<Double> showing those points
		if ( exportPointClouds )
		{
			IJ.showStatus( "Dumping Point-Cloud..." );
			IJ.showProgress( progressIndex++, stepsToDo );
			final Img< DoubleType > dump = DumpImgFactory.dumpPointClouds( allPointClouds, false );
			ImageJFunctions.show( dump, "Dumped PointClouds" );

		}

		if ( exportCircles || exportEllipses )
		{
			// extract section-areas and concentration data
			IJ.showStatus( "Evaluating section areas..." );
			IJ.showProgress( progressIndex++, stepsToDo );
			final List< PlotData > allSectionAreas = valueOfDesireAlongLine( line, WISH_VOLUME );
			IJ.showStatus( "Evaluating section concentrations..." );
			IJ.showProgress( progressIndex++, stepsToDo );
			final List< PlotData > allConcentrations = valueOfDesireAlongLine( line, WISH_CONCENTRATION );

			// check if all or only one frame has to be worked on...
			int start = frameNumToShow;
			int endPlusOne = frameNumToShow + 1;
			if ( frameNumToShow == -1 )
			{
				start = 0;
				endPlusOne = allPointClouds.size();
			}

			String normString = "noNorm";

			if ( exportCircles )
			{
				IJ.showStatus( "Generating circular shape approximation(s)..." );
				IJ.showProgress( progressIndex++, stepsToDo );
				ArrayList< Circle > circularShape = null;
				final List< List< Circle >> circularShapes = new ArrayList< List< Circle >>();
				for ( int i = start; i < endPlusOne; i++ )
				{
					IJ.showProgress( i - start + 1, endPlusOne - start );
					final List< PixelCloud2D< Double >> pointClouds = allPointClouds.get( i );
					if ( toNormalize == NORMALIZE_NONE )
					{
						circularShape = fitCircles( pointClouds, null, true );
					}
					else if ( toNormalize == NORMALIZE_AREA )
					{
						normString = "areaNorm";
						circularShape = fitCircles( pointClouds, allSectionAreas.get( i ), true );
					}
					else
					{
						normString = "circNorm";
						circularShape = fitCircles( pointClouds, allSectionAreas.get( i ), false );
					}
					circularShapes.add( circularShape );
					if ( path != null )
					{
						saveCircularShapeToFile( circularShape, path, String.format( "circular_shape_t%03d_%s.csv", i + 1, normString ) );
					}
				}

				// create new Img<Double> showing the approximated ellipses
				final Img< DoubleType > dump = DumpImgFactory.dumpCircularShapes( circularShapes, allConcentrations );
				ImageJFunctions.show( dump, "Dumped Circles" );
			}

			if ( exportEllipses )
			{
				IJ.showStatus( "Generating elliptical shape approximation(s)..." );
				IJ.showProgress( progressIndex++, stepsToDo );
				ArrayList< Ellipse2D > ellipticalShape = null;
				final List< List< Ellipse2D >> ellipticalShapes = new ArrayList< List< Ellipse2D >>();
				for ( int i = start; i < endPlusOne; i++ )
				{
					IJ.showProgress( i - start + 1, endPlusOne - start );
					final List< PixelCloud2D< Double >> pointClouds = allPointClouds.get( i );
					if ( toNormalize == NORMALIZE_NONE )
					{
						ellipticalShape = fitEllipses( pointClouds, null, true );
					}
					else if ( toNormalize == NORMALIZE_AREA )
					{
						normString = "areaNorm";
						ellipticalShape = fitEllipses( pointClouds, allSectionAreas.get( i ), true );
					}
					else
					{
						normString = "circNorm";
						ellipticalShape = fitEllipses( pointClouds, allSectionAreas.get( i ), false );
					}
					ellipticalShapes.add( ellipticalShape );
					if ( path != null )
					{
						saveEllipticalShapeToFile( ellipticalShape, path, String.format( "elliptical_shape_t%03d_%s.csv", i + 1, normString ) );
					}
				}

				// create new Img<Double> showing the approximated ellipses
				final Img< DoubleType > dump = DumpImgFactory.dumpEllipticalShapes( ellipticalShapes, allConcentrations );
				ImageJFunctions.show( dump, "Dumped Ellipses" );
			}

			IJ.showProgress( stepsToDo, stepsToDo );
		}
	}

	public static OrthoSlicer getInstance()
	{
		return main;
	}

	/**
	 * Loads current IJ image into the OrthoSlicer
	 */
	public boolean loadCurrentImage()
	{
		imgPlus = WindowManager.getCurrentImage();
		if ( imgPlus == null )
		{
			IJ.error( "There must be an active, open window!" );
			return false;
		}
		// final int[] dims = imgPlus.getDimensions(); // width, height,
		// nChannels, nSlizes, nFrames
		// if ( dims[ 3 ] > 1 || dims[ 4 ] < 1 ) {
		// IJ.error(
		// "The active, open window must contain an image with multiple frames, but no slizes!"
		// );
		// return;
		// }
		return true;
	}

	/**
	 * Projects, for each time point, all voxels in orthogonal direction to the
	 * current line-ROI.
	 * 
	 * @param showPlots
	 *            if true, plots acquired data into a sequence of ImageJ
	 *            PlotWindows.
	 */
	public void projectConcentrationToLine( final boolean showPlots )
	{
		final Roi roi = this.imgPlus.getRoi();
		if ( roi == null )
		{
			IJ.error( "Use the imagej line-tool to draw a line anling which you want to sum ortho-slices." );
			return;
		}

		final JFileChooser chooser = new JFileChooser();

		chooser.setCurrentDirectory( new File( imgPlus.getOriginalFileInfo().directory ) );
		chooser.setDialogTitle( "Choose folder to export plot data to..." );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		chooser.setAcceptAllFileFilterUsed( false );
		chooser.setMultiSelectionEnabled( false );

		File path = null; // output folder for CSV-data...
		if ( chooser.showSaveDialog( IJ.getInstance() ) == JFileChooser.APPROVE_OPTION )
		{
			path = chooser.getSelectedFile();
		}

		try
		{
			final Line lineRoi = ( Line ) roi;
			final List< PlotData > data = valueOfDesireAlongLine( lineRoi, WISH_CONCENTRATION );

			// Export plot data and show plots
			int i = 0;
			for ( final PlotData d : data )
			{
				if ( path != null )
				{
					d.saveToFile( path, String.format( "concentration_plot_data_t%03d.csv", i + 1 ) );
				}

				if ( showPlots )
				{
					plotStripped( "Frame: " + ( i + 1 ), "x", "myosin/volume", data.get( i ).getXData(), data.get( i ).getYData() );
				}
				i++;
			}

		}
		catch ( final ClassCastException e )
		{
			e.printStackTrace();
			IJ.error( "Found a non-line ROI... please use the line-tool to indicate the slicing direction!" );
		}
	}

	/**
	 * TODO is not yet really giving the section area but something like the
	 * section volume.
	 * 
	 * @param showPlots
	 *            if true, plots acquired data into a sequence of ImageJ
	 *            PlotWindows.
	 */
	public void projectSectionAreaToLine( final boolean showPlots )
	{
		final Roi roi = this.imgPlus.getRoi();
		if ( roi == null )
		{
			IJ.error( "Use the imagej line-tool to draw a line anling which you want to sum ortho-slices." );
			return;
		}

		final JFileChooser chooser = new JFileChooser();

		chooser.setCurrentDirectory( new File( imgPlus.getOriginalFileInfo().directory ) );
		chooser.setDialogTitle( "Choose folder to export plot data to..." );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		chooser.setAcceptAllFileFilterUsed( false );
		chooser.setMultiSelectionEnabled( false );

		File path = null; // output folder for CSV-data...
		if ( chooser.showSaveDialog( IJ.getInstance() ) == JFileChooser.APPROVE_OPTION )
		{
			path = chooser.getSelectedFile();
		}

		try
		{
			final Line lineRoi = ( Line ) roi;
			final List< PlotData > data = valueOfDesireAlongLine( lineRoi, WISH_VOLUME );

			// Export plot data and show plots
			int i = 0;
			for ( final PlotData d : data )
			{
				if ( path != null )
				{
					d.saveToFile( path, String.format( "area_plot_data_t%03d.csv", i + 1 ) );
				}

				if ( showPlots )
				{
					plotStripped( "Frame: " + ( i + 1 ), "x", "area", d.getXData(), d.getYData() );
				}
				i++;
			}
		}
		catch ( final ClassCastException e )
		{
			e.printStackTrace();
			IJ.error( "Found a non-line ROI... please use the line-tool to indicate the slicing direction!" );
		}
	}

	/**
	 * Plots a data series given as double arrays but strips away all leading
	 * and trailing sequences of 0-values.
	 * 
	 * @param title
	 *            title of the plot.
	 * @param xlabel
	 *            x-axis label.
	 * @param ylabel
	 *            y-axis label.
	 * @param xData
	 *            double array of x-axis data.
	 * @param yData
	 *            double array of y-axis data.
	 * @return instance of ImageJ <code>Plot</code>.
	 */
	private Plot plotStripped( final String title, final String xlabel, final String ylabel, final double[] xData, final double[] yData )
	{
		int start, end;
		for ( start = 0; start < xData.length && yData[ start ] < 0.0000001; start++ );
		for ( end = xData.length - 1; end >= 0 && yData[ end ] < 0.0000001; end-- );

		double[] x = new double[ 0 ];
		double[] y = new double[ 0 ];
		if ( end >= start )
		{
			x = new double[ end - start + 1 ];
			y = new double[ end - start + 1 ];
			for ( int i = 0; i < end - start + 1; i++ )
			{
				x[ i ] = xData[ i ];
				y[ i ] = yData[ i ];
			}
		}

		final Plot plot = new Plot( title, xlabel, ylabel, x, y );
		plot.show();

		return plot;
	}

	/**
	 * Returns a measured value of desire projected to a given line-ROI.
	 * 
	 * @param line
	 *            a line-ROI.
	 * @param which_one_my_master
	 *            one of the static values WISH_CONCENTRATION,
	 *            WISH_SUM_INTERNSITIES, or WISH_VOLUME.
	 * @return
	 */
	private List< PlotData > valueOfDesireAlongLine( final Line line, final int which_one_my_master )
	{
		final Img< UnsignedShortType > img = ImagePlusAdapter.wrap( imgPlus );
		final Cursor< UnsignedShortType > cursor = img.cursor();

		// get 'line' as vector based at 0
		final double vec_x = line.x2 - line.x1;
		final double vec_y = line.y2 - line.y1;
		// direction of 'line' as normalized vecter at 0
		final Vector2D vec_line_direction = new Vector2D( vec_x, vec_y );

		final int frames = imgPlus.getDimensions()[ 4 ]; // 4 happens to be the
															// time-dimension...
		final List< PlotData > summed_intensities = new ArrayList< PlotData >();
		final List< PlotData > volume = new ArrayList< PlotData >();
		final List< PlotData > concentrations = new ArrayList< PlotData >();

		for ( int i = 0; i < frames; i++ )
		{
			summed_intensities.add( new PlotData( ( int ) Math.floor( line.getLength() ) ) );
			volume.add( new PlotData( ( int ) Math.floor( line.getLength() ) ) );
			concentrations.add( new PlotData( ( int ) Math.floor( line.getLength() ) ) );
		}

		double pixel_value;
		final double voxel_depth = this.imgPlus.getCalibration().getZ( 1.0 );
		while ( cursor.hasNext() )
		{
			pixel_value = 0.0;
			try
			{
				pixel_value = cursor.next().get();
			}
			catch ( final ClassCastException e )
			{
				e.printStackTrace();
				IJ.error( "ClassCastException", "Please make source image 16bit!" );
			}
			final double xpos = cursor.getIntPosition( 0 ) - line.x1;
			final double ypos = cursor.getIntPosition( 1 ) - line.y1;
			// final int zpos = cursor.getIntPosition( 2 );
			final int tpos = cursor.getIntPosition( 3 );

			// vector to current pixel
			final Vector2D vec_pos = new Vector2D( xpos, ypos );

			// compute projection onto line-ROI
			final Vector2D lineProjection = vec_line_direction.project( vec_pos );
			final double fractional_slice_num = lineProjection.getLength();
			// don't do anything in case voxel is below or above line
			if ( lineProjection.dot( vec_line_direction ) < 0 || fractional_slice_num > vec_line_direction.getLength() )
			{
				continue;
			}

			// add the shit
			if ( pixel_value > 0.0 )
			{
				summed_intensities.get( tpos ).addValueToXValue( fractional_slice_num, pixel_value );
				volume.get( tpos ).addValueToXValue( fractional_slice_num, voxel_depth );
			}
		}

		// compute concentrations (iterate over summed intensities and divide by
		// volume)
		for ( int t = 0; t < summed_intensities.size(); t++ )
		{
			for ( int i = 0; i < summed_intensities.get( t ).getXData().length; i++ )
			{
				double concentration = summed_intensities.get( t ).getYData()[ i ] / volume.get( t ).getYData()[ i ];
				if ( Double.isNaN( concentration ) )
					concentration = 0.0;
				concentrations.get( t ).addValueToXValue( summed_intensities.get( t ).getXData()[ i ], concentration );
			}
		}

		if ( which_one_my_master == WISH_CONCENTRATION )
		{
			return concentrations;
		}
		else if ( which_one_my_master == WISH_SUM_INTENSITIES )
		{
			return summed_intensities;
		}
		else
		{
			return volume;
		}
	}

	/**
	 * 
	 * @param sectionAreasOrCircumference
	 * @return
	 */
	public ArrayList< Circle > fitCircles( final List< PixelCloud2D< Double >> pointClouds, final PlotData sectionAreasOrCircumference, final boolean normalizeArea )
	{
		final ArrayList< Circle > newCircularShape = new ArrayList< Circle >();
		int z = 1;
		for ( final PixelCloud2D< Double > cloud : pointClouds )
		{
			if ( cloud.isEmpty() )
			{
				newCircularShape.add( new Circle() );
			}
			else
			{
				final Circle c = cloud.getCircularApproximation();
				if ( sectionAreasOrCircumference != null )
				{
					final double desiredValue = sectionAreasOrCircumference.getValueAt( z );
					if ( normalizeArea )
					{
						c.setRadius( Math.sqrt( desiredValue / Math.PI ) );
					}
					else
					{
						c.setRadius( desiredValue / ( 2 * Math.PI ) );
					}
				}
				newCircularShape.add( c );
			}
			z++;
		}

		return newCircularShape;
	}

	/**
	 * @param showPlots
	 * @return
	 */
	public ArrayList< Ellipse2D > fitEllipses( final List< PixelCloud2D< Double >> pointClouds, final PlotData sectionAreasOrCircumference, final boolean normalizeArea )
	{
		final ArrayList< Ellipse2D > newShape = new ArrayList< Ellipse2D >();
		int z = 0;
		for ( final PixelCloud2D< Double > cloud : pointClouds )
		{
			if ( cloud.isEmpty() )
			{
				newShape.add( new Ellipse2D() );
			}
			else
			{
				final Ellipse2D ellipse = cloud.getEllipticalApproximation();
				if ( sectionAreasOrCircumference != null )
				{
					final double desiredValue = sectionAreasOrCircumference.getValueAt( z );
					if ( normalizeArea )
					{
						double c = desiredValue / ( Math.PI * Math.max( 1.0, ellipse.getA() ) * Math.max( 1.0, ellipse.getB() ) );
						// System.out.println( "z=" + z + " desVal=" +
						// desiredValue + " c=" + c + " sqrt=" + Math.sqrt( c )
						// );
						// System.out.println( "\ta=" + ellipse.getA() + " b=" +
						// ellipse.getB() );
						if ( c == 0 )
							c = 1;
						ellipse.setA( ellipse.getA() * Math.sqrt( c ) );
						ellipse.setB( ellipse.getB() * Math.sqrt( c ) );
						// System.out.println( "\ta=" + ellipse.getA() + " b=" +
						// ellipse.getB() );
					}
					else
					{
						final double a = ellipse.getA();
						final double b = ellipse.getB();
						double c = ( a * a + b * b ) * ( 2 * Math.PI * Math.PI ) / ( desiredValue * desiredValue );
						if ( c == 0 )
							c = 1;
						ellipse.setA( Math.sqrt( a / c ) );
						ellipse.setB( Math.sqrt( b / c ) );
					}
				}
				newShape.add( ellipse );
			}
			z++;
		}

		return newShape;
	}

	/**
	 * @param newShape
	 * @param path
	 * @param format
	 */
	private void saveCircularShapeToFile( final ArrayList< Circle > shape, final File path, final String filename )
	{
		final File file = new File( path, filename );
		try
		{
			final FileOutputStream fos = new FileOutputStream( file );
			final OutputStreamWriter out = new OutputStreamWriter( fos );

			out.write( "# Each row one circle, parametrized by: x, y, radius" );
			for ( final Circle c : shape )
			{
				out.write( String.format( "%f, %f, %f\n", c.getCenter().getX(), c.getCenter().getY(), c.getRadius() ) );
			}
			out.close();
			fos.close();
		}
		catch ( final FileNotFoundException e )
		{
			IJ.error( "File '" + file.getAbsolutePath() + "' could not be opened!" );
		}
		catch ( final IOException e )
		{
			IJ.error( "Could not write to file '" + file.getAbsolutePath() + "'!" );
		}
	}

	/**
	 * @param newShape
	 * @param path
	 * @param format
	 */
	private void saveEllipticalShapeToFile( final ArrayList< Ellipse2D > shape, final File path, final String filename )
	{
		final File file = new File( path, filename );
		try
		{
			final FileOutputStream fos = new FileOutputStream( file );
			final OutputStreamWriter out = new OutputStreamWriter( fos );

			out.write( "# Each row one ellipse, parametrized as: x, y, angle, a, b" );
			for ( final Ellipse2D e : shape )
			{
				out.write( String.format( "%f, %f, %f, %f, %f\n", e.getCenter().getX(), e.getCenter().getY(), e.getAngle(), e.getA(), e.getB() ) );
			}
			out.close();
			fos.close();
		}
		catch ( final FileNotFoundException e )
		{
			IJ.error( "File '" + file.getAbsolutePath() + "' could not be opened!" );
		}
		catch ( final IOException e )
		{
			IJ.error( "Could not write to file '" + file.getAbsolutePath() + "'!" );
		}
	}

	/**
	 * Iterates over imgPlus, projects each voxel onto given line and adds two
	 * copies of that voxel into PointClouds2D-objects. Two copies since
	 * projection will lie in between to ortho-slizes along line. Each copy will
	 * also split the voxel intensity based on the relative distance of the
	 * voxel center to the two ortho-slizes.
	 * 
	 * @param line
	 *            Line ROI onto which to project. (We imagine this line to be at
	 *            z=0!)
	 * @return
	 */
	private List< List< PixelCloud2D< Double >>> getOrthogonalPointClouds( final Line line, final double factorVoxelDepth )
	{
		final int num_frames = imgPlus.getDimensions()[ 4 ]; // happens to be
																// the time
																// dimension
		final int num_slices = ( int ) line.getLength();
		final List< List< PixelCloud2D< Double >>> ret = new ArrayList< List< PixelCloud2D< Double >>>();

		// Add new PointCloud2D for each ortho-slice
		for ( int i = 0; i < num_frames; i++ )
		{
			ret.add( new ArrayList< PixelCloud2D< Double >>() );
			for ( int j = 0; j < num_slices; j++ )
			{
				ret.get( i ).add( new PixelCloud2D< Double >() );
			}
		}

		// Wrap imgPlus into Img
		final Img< UnsignedShortType > img = ImagePlusAdapter.wrap( imgPlus );
		final Cursor< UnsignedShortType > cursor = img.cursor();

		// get 'line' as vector based at 0
		final double vec_x = line.x2 - line.x1;
		final double vec_y = line.y2 - line.y1;
		// direction of 'line' as normalized vecter at 0
		final Vector2D vec_line_direction = new Vector2D( vec_x, vec_y );
		final Vector2D vec_orthogonal_direction = new Vector2D( vec_y, -vec_x );

		// Iterate over each voxel in Img
		double pixel_value;
		while ( cursor.hasNext() )
		{
			pixel_value = cursor.next().get();
			final double xpos = cursor.getIntPosition( 0 ) - line.x1;
			final double ypos = cursor.getIntPosition( 1 ) - line.y1;
			final int zpos = cursor.getIntPosition( 2 );
			final double zdepth = factorVoxelDepth * zpos;
			final int tpos = cursor.getIntPosition( 3 );

			// vector to current pixel
			final Vector2D vec_pos = new Vector2D( xpos, ypos );

			// compute projection onto line-ROI
			final Vector2D lineProjection = vec_line_direction.project( vec_pos );
			final double fractional_slice_num = lineProjection.getLength();
			// don't do anything in case voxel is below or above line
			if ( lineProjection.dot( vec_line_direction ) < 0 || fractional_slice_num > vec_line_direction.getLength() )
			{
				continue;
			}

			// compute distance to line-ROI
			final Vector2D orthoProjection = vec_orthogonal_direction.project( vec_pos );
			double dist_to_line = orthoProjection.getLength();
			// and mind the sign! (Is the pixel on the left or right of the
			// line-ROI?)
			if ( orthoProjection.dot( vec_orthogonal_direction ) > 0 )
			{
				dist_to_line *= -1;
			}

			// add the shit
			if ( fractional_slice_num >= 0 && fractional_slice_num <= Math.floor( line.getLength() ) )
			{
				if ( pixel_value > 0 )
				{
					final double p = 1.0 - ( fractional_slice_num - Math.floor( fractional_slice_num ) );
					ret.get( tpos ).get( ( int ) fractional_slice_num ).addPoint( dist_to_line, zdepth, p * pixel_value );
					if ( ( ( int ) fractional_slice_num + 1 ) < ret.get( tpos ).size() )
					{
						ret.get( tpos ).get( ( int ) fractional_slice_num + 1 ).addPoint( dist_to_line, zdepth, ( 1.0 - p ) * pixel_value );
					}
				}
			}
		}

		return ret;
	}
}
