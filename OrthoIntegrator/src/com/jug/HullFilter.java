/**
 *
 */
package com.jug;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import com.jug.data.Pixel2D;
import com.jug.data.PixelCloud2D;
import com.jug.data.Point2D;

/**
 * @author jug
 */
public class HullFilter {

	private static HullFilter main;

	public ImageJ ij;
	public ImagePlus imgPlus = null;

	private JFrame gui;

	public HullFilter() {
		main = this;
		buildGui();
	}

	/**
	 * PROJECT MAIN
	 * ============
	 *
	 * @param args
	 *            muh!
	 */
	public static void main( final String[] args ) {
		ImageJ temp = IJ.getInstance();

		if ( temp == null ) {
			temp = new ImageJ();
			IJ.open( "/Users/jug/MPI/ProjectNorden/FirstSegmentation_crop.tif" );
		}

		main = new HullFilter();

		if ( !main.loadCurrentImage() ) { return; }

		final Line lineRoi = new Line( 0, main.imgPlus.getHeight(), main.imgPlus.getWidth(), 0 );
		main.imgPlus.setRoi( lineRoi, true );

		main.askUserForDetails();
	}

	/**
	 *
	 */
	private void buildGui() {
		gui = new JFrame( "Select Options..." );

		final boolean onlyCurrent = true;

		final JPanel mainPanel = new JPanel( new GridBagLayout() );
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		final JCheckBox cbOnlyCurrentFrame = new JCheckBox( "only current timepoint", onlyCurrent );
		mainPanel.add( cbOnlyCurrentFrame, gbc );

		final JLabel lblShapes = new JLabel( "x/y voxel margin:" );
		lblShapes.setBorder( BorderFactory.createEmptyBorder( 10, 0, 0, 0 ) );
		mainPanel.add( lblShapes, gbc );
		final JTextField txtMargin = new JTextField( "4.0", 5 );
		mainPanel.add( txtMargin, gbc );

		final JPanel buttons = new JPanel();
		buttons.setLayout( new BoxLayout( buttons, BoxLayout.LINE_AXIS ) );
		final JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener( new ActionListener() {
		    @Override
		    public void actionPerformed(final ActionEvent evt) {
				gui.dispose();
		    }
		} );
		final JButton btnOk = new JButton( "Ok" );
		btnOk.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent evt ) {
				// check soundness of inputs...
				double margin = 1.0;
				try {
					margin = new Double( txtMargin.getText() );
				} catch ( final NumberFormatException e ) {
					IJ.showMessage( "Format error!", "Please enter a proper real number!" );
					return;
				}

				gui.dispose();

				int showFrameNum = -1; // this means 'show all'
				if ( cbOnlyCurrentFrame.isSelected() ) {
					showFrameNum = imgPlus.getFrame() - 1; // one-based index... (puke)
				}

				//call the shit
				runHullFilter( showFrameNum, margin );
			}
		} );
		buttons.add( Box.createHorizontalGlue() );
		buttons.add( btnCancel );
		buttons.add( btnOk );
		mainPanel.add( buttons );
		gui.getContentPane().add( mainPanel, BorderLayout.CENTER );
		gui.getContentPane().add( buttons, BorderLayout.SOUTH );
		gui.pack();
		gui.setSize( ( int ) mainPanel.getPreferredSize().getWidth() + 20, ( int ) mainPanel.getPreferredSize().getHeight() + 70 );
		gui.setLocation( ( IJ.getScreenSize().width - gui.getWidth() ) / 2, ( IJ.getScreenSize().height - gui.getHeight() ) / 2 );
		gui.getRootPane().setDefaultButton( btnOk );
	}

	/**
	 *
	 */
	public void askUserForDetails() {
		this.gui.setVisible( true );
	}

	/**
	 * Runs the main procedure.
	 *
	 * @param frameNumToShow
	 *            the frame number to be worked on. If '-1' all frames will be
	 *            filtered.
	 * @param margin
	 *            the thickness of the margin to retain.
	 */
	private void runHullFilter( final int frameNumToShow, final double margin ) {
		// check if all or only one frame has to be worked on...
		int start = frameNumToShow;
		int endPlusOne = frameNumToShow + 1;
		if ( frameNumToShow == -1 ) {
			start = 0;
			endPlusOne = imgPlus.getNFrames();
		}

		IJ.showStatus( "Preparing Image-Containers..." );

		// prepare source image
		final Img< UnsignedShortType > srcImg = ImagePlusAdapter.wrap( imgPlus );

		// prepare target image
		final ImgFactory< RealType > imgFactory = new ArrayImgFactory();
		final Img< RealType > targetImg = imgFactory.create( new int[] { imgPlus.getWidth(), imgPlus.getHeight(), 1, imgPlus.getNSlices(), endPlusOne - start }, srcImg.firstElement() );

		// idea: for each voxel in the source we check for the first and last non-zero voxel per pixel-row.
		final List< List< List < Long >>> firstRowPos = new ArrayList< List< List < Long >>>();
		final List< List< List < Long >>> lastRowPos = new ArrayList< List< List < Long >>>();
		// idea: for each voxel in the source we check for the first and last non-zero voxel per pixel-column.
		final List< List< List < Long >>> firstColPos = new ArrayList< List< List < Long >>>();
		final List< List< List < Long >>> lastColPos = new ArrayList< List< List < Long >>>();
		// later we will add all those coordinates into a list and filter only those voxels (second sweep)
		// that are close to at least one of thos 'border-voxels'.

		// Initialize all fields...
		int targetIndex = 0;
		for ( int i = start; i < endPlusOne; i++ ) {
			firstRowPos.add( new ArrayList< List< Long >>() );
			lastRowPos.add( new ArrayList< List< Long >>() );
			firstColPos.add( new ArrayList< List< Long >>() );
			lastColPos.add( new ArrayList< List< Long >>() );

			for ( int zpos = 0; zpos<imgPlus.getNSlices(); zpos++ ) {
				firstRowPos.get( targetIndex ).add( new ArrayList< Long >() );
				for (int rowIdx = 0; rowIdx < imgPlus.getHeight(); rowIdx++) {
					firstRowPos.get( targetIndex ).get( zpos ).add( Long.MAX_VALUE );
				}
				lastRowPos.get( targetIndex ).add( new ArrayList< Long >() );
				for (int rowIdx = 0; rowIdx < imgPlus.getHeight(); rowIdx++) {
					lastRowPos.get( targetIndex ).get( zpos ).add( Long.MIN_VALUE );
				}
				firstColPos.get( targetIndex ).add( new ArrayList< Long >() );
				for (int colIdx = 0; colIdx < imgPlus.getWidth(); colIdx++) {
					firstColPos.get( targetIndex ).get( zpos ).add( Long.MAX_VALUE );
				}
				lastColPos.get( targetIndex ).add( new ArrayList< Long >() );
				for (int colIdx = 0; colIdx < imgPlus.getWidth(); colIdx++) {
					lastColPos.get( targetIndex ).get( zpos ).add( Long.MIN_VALUE );
				}
			}
			targetIndex++;
		}

		IJ.showStatus( "Scanning for border-pixels..." );

		// Now iterate over source and update corresponding fields...
		targetIndex = 0;
		for ( int i = start; i < endPlusOne; i++ ) {
			final Cursor< UnsignedShortType > cursor = Views.iterable( Views.hyperSlice( srcImg, 3, i ) ).cursor();

			while ( cursor.hasNext() ) {
				final double pixel_value = cursor.next().get();
				if (pixel_value != 0.0) {
					final int xpos = cursor.getIntPosition( 0 );
					final int ypos = cursor.getIntPosition( 1 );
					final int zpos = cursor.getIntPosition( 2 );

					if ( firstRowPos.get( targetIndex ).get( zpos ).get( ypos ).longValue() > xpos ) {
						firstRowPos.get( targetIndex ).get( zpos ).set( ypos, new Long( xpos ) );
					}
					if ( lastRowPos.get( targetIndex ).get( zpos ).get( ypos ).longValue() < xpos ) {
						lastRowPos.get( targetIndex ).get( zpos ).set( ypos, new Long( xpos ) );
					}

					if ( firstColPos.get( targetIndex ).get( zpos ).get( xpos ).longValue() > ypos ) {
						firstColPos.get( targetIndex ).get( zpos ).set( xpos, new Long( ypos ) );
					}
					if ( lastColPos.get( targetIndex ).get( zpos ).get( xpos ).longValue() < ypos ) {
						lastColPos.get( targetIndex ).get( zpos ).set( xpos, new Long( ypos ) );
					}
				}
			}
			targetIndex++;
		}

		IJ.showStatus( "Building Point-Clouds for Margin-Filter..." );

		// Iterate over all fields and build a PointCloud for each slice in each frame...
		final List< List< PixelCloud2D< Double > >> borderVoxels = new ArrayList< List< PixelCloud2D< Double >>>();
		targetIndex = 0;

		for ( int i = start; i < endPlusOne; i++ ) {
			borderVoxels.add( new ArrayList< PixelCloud2D< Double >>() );
			for ( int zpos = 0; zpos < imgPlus.getNSlices(); zpos++ ) {
				borderVoxels.get( targetIndex ).add( new PixelCloud2D< Double >() );
			}
			targetIndex++;
		}
		targetIndex = 0;
		for ( int i = start; i < endPlusOne; i++ ) {
			for ( int zpos = 0; zpos < imgPlus.getNSlices(); zpos++ ) {
				for ( int ypos = 0; ypos < imgPlus.getHeight(); ypos++ ) {
					final long first = firstRowPos.get( targetIndex ).get( zpos ).get( ypos ).longValue();
					final long last = lastRowPos.get( targetIndex ).get( zpos ).get( ypos ).longValue();
					// do adding business only if anything was found at all
					if ( first < Long.MAX_VALUE ) {
						// add first for sure
						borderVoxels.get( targetIndex ).get( zpos ).addPoint( new Pixel2D< Double >( first, ypos, 1.0 ) );
						// add last if >first
						if ( last > first ) {
							borderVoxels.get( targetIndex ).get( zpos ).addPoint( new Pixel2D< Double >( last, ypos, 1.0 ) );
						}
					}
				}
				for ( int xpos = 0; xpos < imgPlus.getWidth(); xpos++ ) {
					final long first = firstColPos.get( targetIndex ).get( zpos ).get( xpos ).longValue();
					final long last = lastColPos.get( targetIndex ).get( zpos ).get( xpos ).longValue();
					// do adding business only if anything was found at all
					if ( first < Long.MAX_VALUE ) {
						// add first for sure
						borderVoxels.get( targetIndex ).get( zpos ).addPoint( new Pixel2D< Double >( xpos, first, 1.0 ) );
						// add last if >first
						if ( last > first ) {
							borderVoxels.get( targetIndex ).get( zpos ).addPoint( new Pixel2D< Double >( xpos, last, 1.0 ) );
						}
					}
				}
			}
			targetIndex++;
		}

		// Debug-dump...
//		IJ.showStatus( "Dumping Point-Clouds (for debugging reasons)..." );
//		final Img< DoubleType > dump = DumpImgFactory.dumpPointClouds( borderVoxels, false );
//		ImageJFunctions.show( dump, "Dumped PointClouds" );

		// Now we make a final sweep and take over all voxels that are close enough to any of the points in the clouds...
		IJ.showStatus( "Final step: creating new image..." );
		targetIndex = 0;
		final RandomAccess< RealType > raTarget = targetImg.randomAccess();
		for ( int i = start; i < endPlusOne; i++ ) {
			final Cursor< UnsignedShortType > cursor = Views.iterable( Views.hyperSlice( srcImg, 3, i ) ).cursor();

			while ( cursor.hasNext() ) {
				final double pixel_value = cursor.next().get();
				if ( pixel_value != 0.0 ) {
					final int xpos = cursor.getIntPosition( 0 );
					final int ypos = cursor.getIntPosition( 1 );
					final int zpos = cursor.getIntPosition( 2 );
					final Point2D cur = new Point2D(xpos, ypos);
					final Point2D closest = borderVoxels.get( targetIndex ).get( zpos ).getClosestPoint(cur);
					if ( Point2D.distance( closest, cur ) < margin ) {
						raTarget.setPosition( new long[] { cursor.getLongPosition( 0 ), cursor.getLongPosition( 1 ), 0, cursor.getLongPosition( 2 ), targetIndex } );
						raTarget.get().set( cursor.get() );
					}
				}
			}
			targetIndex++;
		}
		ImageJFunctions.show( targetImg, imgPlus.getTitle() + String.format( "_HullMargin_%.1f", margin ) );

		IJ.showStatus( "...done!" );
	}

	/**
	 * Loads current IJ image into the OrthoSlicer
	 */
	public boolean loadCurrentImage() {
		imgPlus = WindowManager.getCurrentImage();
		if ( imgPlus == null ) {
			IJ.error( "There must be an active, open window!" );
			return false;
		}
//		final int[] dims = imgPlus.getDimensions(); // width, height, nChannels, nSlizes, nFrames
//		if ( dims[ 3 ] > 1 || dims[ 4 ] < 1 ) {
//			IJ.error( "The active, open window must contain an image with multiple frames, but no slizes!" );
//			return;
//		}
		return true;
	}

}
