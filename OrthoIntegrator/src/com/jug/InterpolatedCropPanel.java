/**
 *
 */
package com.jug;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import com.jug.util.DataMover;

/**
 * @author jug
 */
public class InterpolatedCropPanel extends JPanel implements ActionListener, KeyListener, FocusListener
{

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = -8040344519142406774L;

	private static InterpolatedCropPanel main;

	private final Frame frame;

	private final ImagePlus imgPlus;

	private JTable tableROIs;

	private Vector< String > tableColumnNames;

	private DefaultTableModel tmROIs;

	private Vector< Vector< String > > tableData;

	protected JButton bAdd;

	protected JButton bDel;

	protected JButton bShowPrevious;

	protected JButton bShowNext;

	protected JButton bAuto;

	protected JButton bExport;

	protected JButton bImport;

	protected JButton bCrop;

	private ImageCanvas imgCanvas;

	/**
	 * @param imgPlus
	 */
	public InterpolatedCropPanel( final Frame frame, final ImagePlus imgPlus )
	{
		super( new BorderLayout( 5, 5 ) );
		setBorder( BorderFactory.createEmptyBorder( 10, 15, 5, 15 ) );
		this.frame = frame;
		this.imgPlus = imgPlus;

		buildGui();

		frame.setSize( 600, 400 );
	}

	private void buildGui()
	{
		final JPanel pContent = new JPanel( new BorderLayout() );
		final JPanel pControlsUpper = new JPanel();
		pControlsUpper.setLayout( new BoxLayout( pControlsUpper, BoxLayout.LINE_AXIS ) );
		final JPanel pControlsLower = new JPanel();
		pControlsLower.setLayout( new BoxLayout( pControlsLower, BoxLayout.LINE_AXIS ) );

		final JTextArea textIntro = new JTextArea( "" + "Add rectangular keyframe selections in arbitrary places.\n" + "Selection in intermediate frames will be filled in automatically.\n" + "Note that cropped stack will only contain images between first and last set keyframe." );
		textIntro.setBackground( new JButton().getBackground() );
		textIntro.setEditable( false );
		textIntro.setBorder( BorderFactory.createEmptyBorder( 0, 2, 5, 2 ) );

		bAdd = new JButton( "+" );
		bAdd.addActionListener( this );
		bDel = new JButton( "-" );
		bDel.addActionListener( this );

		bShowPrevious = new JButton( "<" );
		bShowPrevious.addActionListener( this );
		bShowNext = new JButton( ">" );
		bShowNext.addActionListener( this );

		bAuto = new JButton( "auto" );
		bAuto.addActionListener( this );
		bImport = new JButton( "load" );
		bImport.addActionListener( this );
		bExport = new JButton( "save" );
		bExport.addActionListener( this );

		bCrop = new JButton( "Crop & Go" );
		bCrop.addActionListener( this );

		tableColumnNames = new Vector< String >();
		tableColumnNames.add( "frame" );
		tableColumnNames.add( "x" );
		tableColumnNames.add( "y" );
		tableColumnNames.add( "width" );
		tableColumnNames.add( "height" );
		tableData = new Vector< Vector< String > >();
		tmROIs = new DefaultTableModel( tableData, tableColumnNames );
		tableROIs = new JTable( tmROIs )
		{

			private static final long serialVersionUID = -5757310501730411649L;

			DefaultTableCellRenderer renderRight = new DefaultTableCellRenderer();

			{ // initializer block
				renderRight.setHorizontalAlignment( SwingConstants.RIGHT );
			}

			@Override
			public TableCellRenderer getCellRenderer( final int arg0, final int arg1 )
			{
				return renderRight;

			}

			@Override
			public boolean isCellEditable( final int rowIndex, final int colIndex )
			{
				return false; // disable edit for ALL cells
			}

			@Override
			public void changeSelection( final int rowIndex, final int columnIndex, final boolean toggle, final boolean extend )
			{
				try
				{
					showFrameAndRoiFromTableData( rowIndex );
				}
				catch ( final Exception e )
				{}
				// make the selection change
				super.changeSelection( rowIndex, columnIndex, toggle, extend );
			}
		};

		add( textIntro, BorderLayout.NORTH );
		pContent.add( tableROIs.getTableHeader(), BorderLayout.NORTH );
		pContent.add( tableROIs, BorderLayout.CENTER );
		final JScrollPane scrollPane = new JScrollPane( tableROIs );
		add( scrollPane, BorderLayout.CENTER );
		pControlsUpper.add( bAdd );
		pControlsUpper.add( bDel );
		pControlsUpper.add( Box.createHorizontalGlue() );
		pControlsUpper.add( bAuto );
		pControlsUpper.add( Box.createHorizontalGlue() );
		pControlsUpper.add( bImport );
		pControlsUpper.add( bExport );

		pControlsLower.add( bShowPrevious );
		pControlsLower.add( bShowNext );
		pControlsLower.add( Box.createHorizontalGlue() );
		pControlsLower.add( bCrop );

		final JPanel controls = new JPanel( new GridLayout( 2, 1 ) );
		controls.add( pControlsUpper );
		controls.add( pControlsLower );
		add( controls, BorderLayout.SOUTH );

		// - - - - - - - - - - - - - - - - - - - - - - - -
		// KEYSTROKE SETUP (usingInput- and ActionMaps)
		// - - - - - - - - - - - - - - - - - - - - - - - -
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( 'a' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( 'd' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( '.' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( ',' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( 'A' ), "MMGUI_bindings" );

		this.getActionMap().put( "MMGUI_bindings", new AbstractAction()
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e.getActionCommand().equals( "a" ) )
				{
					bAdd.doClick();
				}
				if ( e.getActionCommand().equals( "d" ) )
				{
					bDel.doClick();
				}
				if ( e.getActionCommand().equals( "." ) )
				{
					bShowNext.doClick();
				}
				if ( e.getActionCommand().equals( "," ) )
				{
					bShowPrevious.doClick();
				}
				if ( e.getActionCommand().equals( "A" ) )
				{
					bAuto.doClick();
				}
			}
		} );

	}

	/**
	 * @param rowIndex
	 *            index of the row in <code>tableData</code> that should be
	 *            highlighted in <code>imgPlus</code>.
	 */
	protected void showFrameAndRoiFromTableData( final int rowIndex )
	{
		final Vector< String > row = tableData.get( rowIndex );
		final int frameNum = Integer.parseInt( row.get( 0 ) );
		final int x = Integer.parseInt( row.get( 1 ) );
		final int y = Integer.parseInt( row.get( 2 ) );
		final int w = Integer.parseInt( row.get( 3 ) );
		final int h = Integer.parseInt( row.get( 4 ) );
		imgPlus.setPosition( imgPlus.getC(), imgPlus.getSlice(), frameNum );
		imgPlus.setRoi( new Rectangle( x, y, w, h ) );
	}

	/**
	 * Shows the frame with the given <code>frameIndex</code>, in case it
	 * exists, and adds a rectangular ROI at the location given in the table (or
	 * at a linear interpolated place in case the given <code>frameIndex</code>
	 * is between two table rows.
	 * 
	 * @param channelIndex
	 *            index of the channel to be selected.
	 * @param timeIndex
	 *            index of the time to be shown in <code>imgPlus</code>.
	 */
	protected void showRoiOnFrame( final int channelIndex, final int timeIndex )
	{
		final int rowIndex = getTableEntryForFrame( timeIndex );
		if ( rowIndex >= 0 )
		{
			// No interpolation needed
			showFrameAndRoiFromTableData( rowIndex );
		}
		else
		{
			// Interpolation!!!
			imgPlus.setPosition( channelIndex, imgPlus.getSlice(), timeIndex );
			final int[][] interpolationData = getInterpolationData( timeIndex );
			if ( interpolationData[ 0 ][ 0 ] != -1 && interpolationData[ 1 ][ 0 ] != -1 )
			{
				final int[] ret = getInterpolatedROI( timeIndex );
				imgPlus.setRoi( new Rectangle( ret[ 0 ], ret[ 1 ], ret[ 2 ], ret[ 3 ] ) );
			}
			else
			{
				imgPlus.deleteRoi();
				System.out.println( "This frame is NOT within current crop-bounds!" );
			}
		}
	}

	/**
	 * Computes and returns the interpolated ROI at a given time.
	 * 
	 * @param timeIndex
	 *            the time
	 * @return an 1d int-array containing (x,y,w,h), being the interpolated ROI
	 *         for the requested time-point.
	 */
	private int[] getInterpolatedROI( final int timeIndex )
	{
		final int[][] interpolationData = getInterpolationData( timeIndex );
		final int[] ret = new int[ 4 ];
		if ( interpolationData[ 0 ][ 0 ] != -1 && interpolationData[ 1 ][ 0 ] != -1 )
		{
			final float interpolationFraction = ( ( float ) ( timeIndex - interpolationData[ 0 ][ 0 ] ) ) / ( interpolationData[ 1 ][ 0 ] - interpolationData[ 0 ][ 0 ] );
			ret[ 0 ] = interpolationData[ 0 ][ 1 ] + Math.round( ( interpolationData[ 1 ][ 1 ] - interpolationData[ 0 ][ 1 ] ) * interpolationFraction );
			ret[ 1 ] = interpolationData[ 0 ][ 2 ] + Math.round( ( interpolationData[ 1 ][ 2 ] - interpolationData[ 0 ][ 2 ] ) * interpolationFraction );
			ret[ 2 ] = interpolationData[ 0 ][ 3 ] + Math.round( ( interpolationData[ 1 ][ 3 ] - interpolationData[ 0 ][ 3 ] ) * interpolationFraction );
			ret[ 3 ] = interpolationData[ 0 ][ 4 ] + Math.round( ( interpolationData[ 1 ][ 4 ] - interpolationData[ 0 ][ 4 ] ) * interpolationFraction );
		}
		else
		{
			ret[ 0 ] = 0;
			ret[ 1 ] = 0;
			ret[ 2 ] = imgPlus.getWidth();
			ret[ 3 ] = imgPlus.getHeight();
		}
		return ret;
	}

	/**
	 * @param timeIndex
	 * @return
	 */
	private int getTableEntryForFrame( final int timeIndex )
	{
		int i = 0;
		for ( final Vector< String > row : tableData )
		{
			final int rowEntry = Integer.parseInt( row.get( 0 ) );
			if ( timeIndex == rowEntry ) { return i; }
			if ( rowEntry > timeIndex )
			{
				break;
			}
			i++;
		}
		return -1;
	}

	/**
	 * Returns the to table entries embracing the given time point.
	 * 
	 * @param timeIndex
	 *            the time point to get the data for
	 * @return a two dimensional int array. Dimension one is of length 2,
	 *         containing the roi-data (x,z,w,h) of the adjacent earlier table
	 *         entry at 0 and the roi-data of the adjacent later table entry at
	 *         1. If one or both such entries do not exist the array contains
	 *         -1's.
	 */
	private int[][] getInterpolationData( final int timeIndex )
	{
		final int[][] ret = { { -1, -1, -1, -1, -1 }, { -1, -1, -1, -1, -1 } };

		int i = 0;
		for ( ; i < tableData.size(); i++ )
		{
			final int frame = Integer.parseInt( tableData.get( i ).get( 0 ) ) - 1;
			if ( frame <= timeIndex )
			{
				ret[ 0 ][ 0 ] = frame;
				for ( int j = 0; j < 5; j++ )
				{
					ret[ 0 ][ j ] = Integer.parseInt( tableData.get( i ).get( j ) );
				}
			}
			if ( frame >= timeIndex )
			{
				ret[ 1 ][ 0 ] = frame;
				for ( int j = 0; j < 5; j++ )
				{
					ret[ 1 ][ j ] = Integer.parseInt( tableData.get( i ).get( j ) );
				}
				break;
			}
		}

		return ret;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e )
	{
		// --- ADD ---
		if ( e.getSource().equals( bAdd ) )
		{
			final Roi roi = this.imgPlus.getRoi();
			if ( roi == null )
			{
				IJ.error( "Please use ImageJ to select a ROI containing your cell of interest." );
				return;
			}

			// if ROI for this frame exists -- remove it first
			final int rowId = getFrameInTable( imgPlus.getT() );
			if ( rowId >= 0 )
			{
				tableData.remove( rowId );
			}

			final Rectangle rect = roi.getBounds();
			final Vector< String > row = new Vector< String >();
			row.add( "" + imgPlus.getT() );
			row.add( "" + rect.x );
			row.add( "" + rect.y );
			row.add( "" + rect.width );
			row.add( "" + rect.height );
			tableData.add( row );

			Collections.sort( tableData, new Comparator< Vector< String >>()
			{
				@Override
				public int compare( final Vector< String > v1, final Vector< String > v2 )
				{
					final int n1 = Integer.parseInt( v1.get( 0 ) );
					final int n2 = Integer.parseInt( v2.get( 0 ) );
					return ( n1 - n2 );
				}
			} );

			tmROIs.setDataVector( tableData, tableColumnNames );

			// --- DEL ---
		}
		else if ( e.getSource().equals( bDel ) )
		{
			int rowId = tableROIs.getSelectedRow();
			if ( rowId >= 0 )
			{
				tableData.remove( rowId );
				tmROIs.setDataVector( tableData, tableColumnNames );
			}
			else
			{
				rowId = getFrameInTable( imgPlus.getT() );
				if ( rowId >= 0 )
				{
					tableData.remove( rowId );
				}
				tmROIs.setDataVector( tableData, tableColumnNames );
			}

			// --- SHOW PREVIOUS ---
		}
		else if ( e.getSource().equals( bShowPrevious ) )
		{
			int t = imgPlus.getT();
			t--;
			if ( t > 0 )
			{
				showRoiOnFrame( imgPlus.getC(), t );
			}
			else
			{
				System.out.println( "There is no previous frame..." );
			}

			// --- SHOW NEXT ---
		}
		else if ( e.getSource().equals( bShowNext ) )
		{
			int t = imgPlus.getT();
			t++;
			if ( t <= imgPlus.getDimensions()[ 4 ] )
			{
				showRoiOnFrame( imgPlus.getC(), t );
			}
			else
			{
				System.out.println( "There is no next frame..." );
			}

			// --- AUTO CROP ---
		}
		else if ( e.getSource().equals( bAuto ) )
		{
			final boolean b = IJ.showMessageWithCancel( "Question", "This function will automatically crop pixels that are black (0).\nNote that this will replace all table entries... do you want to start the automatic crop?" );
			if ( b )
			{
				performAutoCrop();
			}

			// --- EXPORT ---
		}
		else if ( e.getSource().equals( bExport ) )
		{
			final JFileChooser chooser = new JFileChooser();

			chooser.setCurrentDirectory( new File( imgPlus.getOriginalFileInfo().directory ) );
			chooser.setDialogTitle( "Save keyframe-data to csv-file..." );
			chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
			chooser.setAcceptAllFileFilterUsed( false );
			chooser.setMultiSelectionEnabled( false );
			chooser.setFileFilter( new FileFilter()
			{

				@Override
				public final boolean accept( final File file )
				{
					return ( file.canWrite() && file.getName().endsWith( ".csv" ) );
				}

				@Override
				public String getDescription()
				{
					return ".csv - Comma Seperated Values File";
				}
			} );

			if ( chooser.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
			{
				File file = chooser.getSelectedFile();
				final String file_name = file.toString();
				if ( !file_name.endsWith( ".csv" ) )
					file = new File( file_name + ".csv" );
				this.exportTableData( file );
			}

			// --- IMPORT ---
		}
		else if ( e.getSource().equals( bImport ) )
		{
			final JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory( new File( imgPlus.getOriginalFileInfo().directory ) );
			chooser.setDialogTitle( "Load keyframe-data to csv-file..." );
			chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
			chooser.setAcceptAllFileFilterUsed( false );
			chooser.setMultiSelectionEnabled( false );
			chooser.setFileFilter( new FileFilter()
			{

				@Override
				public final boolean accept( final File file )
				{
					return ( file.canRead() && file.getName().endsWith( ".csv" ) );
				}

				@Override
				public String getDescription()
				{
					return ".csv - Comma Seperated Values File";
				}
			} );

			if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
			{
				this.importTableData( chooser.getSelectedFile() );
			}

			// --- CROP ---
		}
		else if ( e.getSource().equals( bCrop ) )
		{
			final Img< RealType > croppedImg = getCroppedImg();
			ImageJFunctions.show( croppedImg );
			frame.dispose();
		}
	}

	/**
	 *
	 */
	@SuppressWarnings( "unchecked" )
	private void performAutoCrop()
	{
		final Img< RealType > srcImg = ImageJFunctions.wrapReal( imgPlus );

		final int frames = imgPlus.getDimensions()[ 4 ];
		final int slices = imgPlus.getDimensions()[ 3 ];
		final int channels = imgPlus.getDimensions()[ 2 ];
		final int height = imgPlus.getDimensions()[ 1 ];
		final int width = imgPlus.getDimensions()[ 0 ];

		final Cursor< RealType > srcCursor = srcImg.localizingCursor();

		int minFrame = frames;
		int maxFrame = 0;

		final int[] minX = new int[ frames ];
		final int[] maxX = new int[ frames ];
		final int[] minY = new int[ frames ];
		final int[] maxY = new int[ frames ];

		for ( int i = 0; i < frames; i++ )
		{
			minX[ i ] = width;
			minY[ i ] = height;
			maxX[ i ] = 0;
			maxY[ i ] = 0;
		}

		// iterate over the input cursor
		while ( srcCursor.hasNext() )
		{
			srcCursor.next();
			if ( srcCursor.get().getRealDouble() > 0.000001 )
			{
				final int f = srcCursor.getIntPosition( srcCursor.numDimensions() - 1 );
				minFrame = Math.min( minFrame, f );
				maxFrame = Math.max( maxFrame, f );
				final int x = srcCursor.getIntPosition( 0 );
				final int y = srcCursor.getIntPosition( 1 );
				minX[ f ] = Math.min( minX[ f ], x );
				minY[ f ] = Math.min( minY[ f ], y );
				maxX[ f ] = Math.max( maxX[ f ], x );
				maxY[ f ] = Math.max( maxY[ f ], y );
			}
		}

		// write new table entries
		this.tableData = new Vector< Vector< String >>();
		for ( int f = minFrame; f <= maxFrame; f++ )
		{
			final Vector< String > row = new Vector< String >();
			row.add( Integer.toString( f + 1 ) );
			row.add( Integer.toString( minX[ f ] ) );
			row.add( Integer.toString( minY[ f ] ) );
			row.add( Integer.toString( maxX[ f ] - minX[ f ] + 1 ) );
			row.add( Integer.toString( maxY[ f ] - minY[ f ] + 1 ) );
			tableData.add( row );
		}
		tmROIs.setDataVector( tableData, tableColumnNames );
	}

	/**
	 * @param frameId
	 *            the index of the active frame.
	 * @return the index of the corresponding row in the table-data, or -1 if it
	 *         is not contained.
	 */
	private int getFrameInTable( final int frameId )
	{
		int ret = 0;
		for ( final Vector< String > row : tableData )
		{
			if ( row.get( 0 ).equals( "" + frameId ) ) { return ret; }
			ret++;
		}
		return -1;
	}

	/**
	 * @param filename
	 *            file to save the table to
	 */
	private void exportTableData( final File file )
	{
		try
		{
			final FileOutputStream fos = new FileOutputStream( file );
			final OutputStreamWriter out = new OutputStreamWriter( fos );

			for ( final Vector< String > row : tableData )
			{
				for ( final String col : row )
				{
					out.write( col + ", " );
				}
				out.write( '\n' );
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

	private void importTableData( final File file )
	{
		try
		{
			final BufferedReader reader = new BufferedReader( new FileReader( file ) );
			String line = null;
			this.tableData = new Vector< Vector< String >>();
			while ( ( line = reader.readLine() ) != null )
			{
				final Vector< String > row = new Vector< String >();
				for ( final String col : line.split( "," ) )
				{
					row.add( col.trim() );
				}
				tableData.add( row );
			}
			tmROIs.setDataVector( tableData, tableColumnNames );
			reader.close();
		}
		catch ( final FileNotFoundException e )
		{
			IJ.error( "File '" + file.getAbsolutePath() + "' could not be opened!" );
		}
		catch ( final IOException e )
		{
			IJ.error( "Could not read from file '" + file.getAbsolutePath() + "'!" );
		}
	}

	/**
	 * @return the 'crop'-button... in order to know if an external event was
	 *         caused by this button...
	 */
	public JButton getButtonCrop()
	{
		return bCrop;
	}

	/**
	 * @return an ImgLib2 <code>Img</code> of <code>RealType</code> containing
	 *         the cropped area.
	 */
	public Img< RealType > getCroppedImg()
	{
		// determine size
		int startFrame;
		int slices, frames;
		int maxW = 0, maxH = 0;
		if ( tableROIs.getRowCount() == 0 )
		{
			startFrame = 0;
			slices = imgPlus.getDimensions()[ 3 ];
			frames = imgPlus.getDimensions()[ 4 ];
			maxW = imgPlus.getDimensions()[ 0 ];
			maxH = imgPlus.getDimensions()[ 1 ];
		}
		else if ( tableROIs.getRowCount() == 1 )
		{
			startFrame = Integer.parseInt( tableData.get( 0 ).get( 0 ) ) - 1;
			slices = imgPlus.getDimensions()[ 3 ];
			frames = 1;
			maxW = Integer.parseInt( tableData.get( 0 ).get( 3 ) );
			maxH = Integer.parseInt( tableData.get( 0 ).get( 4 ) );
		}
		else
		{
			startFrame = Integer.parseInt( tableData.get( 0 ).get( 0 ) ) - 1;
			slices = imgPlus.getDimensions()[ 3 ];
			frames = Integer.parseInt( tableData.get( tableData.size() - 1 ).get( 0 ) );
			frames -= Integer.parseInt( tableData.get( 0 ).get( 0 ) ) - 1;
			for ( final Vector< String > row : tableData )
			{
				final int w = Integer.parseInt( row.get( 3 ) );
				final int h = Integer.parseInt( row.get( 4 ) );
				maxW = Math.max( maxW, w );
				maxH = Math.max( maxH, h );
			}

		}

		// wrap source-image
		final Img< RealType > srcImg = ImageJFunctions.wrapReal( imgPlus );

		// create empty Img
		final ImgFactory< RealType > imgFactory = new ArrayImgFactory();
		final Img< RealType > ret = imgFactory.create( new int[] { maxW, maxH, 1, slices, frames }, srcImg.firstElement() );

		// copying data
		for ( int i = 0; i < frames; i++ )
		{
			for ( int z = 0; z < slices; z++ )
			{
				final int[] roi = getInterpolatedROI( startFrame + i );
				final int deltaW = maxW - roi[ 2 ];
				final int deltaH = maxH - roi[ 3 ];
				final long x = roi[ 0 ] - deltaW / 2;
				final long y = roi[ 1 ] - deltaH / 2;

				final IntervalView< RealType > src = Views.hyperSlice( Views.hyperSlice( srcImg, 3, i ), 2, z );
				final IntervalView< RealType > target = Views.hyperSlice( Views.hyperSlice( Views.hyperSlice( ret, 4, i ), 3, z ), 2, 0 );
				DataMover.copy( Views.offset( src, x, y ), target );
			}
		}

		// tadaaaa!
		return ret;
	}

	public static InterpolatedCropPanel getInstance()
	{
		return main;
	}

	public void clickAdd()
	{
		bAdd.doClick();
	}

	public void clickDel()
	{
		bDel.doClick();
	}

	public void clickShowNext()
	{
		bShowNext.doClick();
	}

	public void clickShowPrevious()
	{
		bShowPrevious.doClick();
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
			IJ.open( "/Users/jug/MPI/ProjectNorden/FirstSegmentation_crop.tif" );
		}

		final ImagePlus imgPlus = WindowManager.getCurrentImage();
		if ( imgPlus == null )
		{
			IJ.error( "There must be an active, open window!" );
			// System.exit( 1 );
			return;
		}

		final JFrame guiFrame = new JFrame( "NordenTools -- Crop" );
		main = new InterpolatedCropPanel( guiFrame, imgPlus );

		IJ.setTool( "Rectangle Tool" );

		main.imgCanvas = imgPlus.getCanvas();

		final FocusListener[] focusListener = main.imgCanvas.getFocusListeners();
		for ( final FocusListener l : focusListener )
		{
			main.imgCanvas.removeFocusListener( l );
		}
		main.imgCanvas.addFocusListener( main );

		final KeyListener[] keyListerners = main.imgCanvas.getKeyListeners();
		for ( final KeyListener l : keyListerners )
		{
			main.imgCanvas.removeKeyListener( l );
		}
		main.imgCanvas.addKeyListener( main );

		guiFrame.add( main );
		guiFrame.setVisible( true );
	}

	/**
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyTyped( final KeyEvent e )
	{
		e.consume(); // prevent ImageJ from handling this event
	}

	/**
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyPressed( final KeyEvent e )
	{
		if ( KeyEvent.getKeyText( e.getKeyCode() ).equals( "A" ) )
		{
			this.clickAdd();
		}
		if ( KeyEvent.getKeyText( e.getKeyCode() ).equals( "D" ) )
		{
			this.clickDel();
		}
		if ( KeyEvent.getKeyText( e.getKeyCode() ).equals( "." ) )
		{
			this.clickShowNext();
		}
		if ( KeyEvent.getKeyText( e.getKeyCode() ).equals( "," ) )
		{
			this.clickShowPrevious();
		}
		e.consume(); // prevent ImageJ from handling this event
	}

	/**
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyReleased( final KeyEvent e )
	{
		e.consume(); // prevent ImageJ from handling this event
	}

	/**
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	@Override
	public void focusGained( final FocusEvent e )
	{
		final KeyListener[] keyListerners = imgCanvas.getKeyListeners();
		for ( final KeyListener l : keyListerners )
		{
			imgCanvas.removeKeyListener( l );
		}
		imgCanvas.addKeyListener( main );
	}

	/**
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	@Override
	public void focusLost( final FocusEvent e )
	{}
}
