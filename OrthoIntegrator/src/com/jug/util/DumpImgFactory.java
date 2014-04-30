/**
 *
 */
package com.jug.util;

import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import com.jug.data.Circle;
import com.jug.data.Ellipse2D;
import com.jug.data.Pixel2D;
import com.jug.data.PixelCloud2D;
import com.jug.data.PlotData;
import com.jug.data.Point2D;
import com.jug.data.Rectangle2D;

/**
 * @author jug
 */
public class DumpImgFactory {

	/**
	 * Creates an Img<double> from a List of Lists of Pointcloud2D objects.
	 *
	 * @param clouds
	 */
	public static Img< DoubleType > dumpPointClouds( final List< List< PixelCloud2D< Double >>> clouds, final boolean interpolated ) {
		double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
		int sizeZ = 0;

		for ( final List< PixelCloud2D< Double >> frame : clouds ) {
			sizeZ = Math.max( sizeZ, frame.size() );
			for ( final PixelCloud2D< Double > slice : frame ) {
				for ( final Pixel2D<Double> point : slice.getPoints() ) {
					minX = Math.min( minX, point.getX() );
					maxX = Math.max( maxX, point.getX() );
					minY = Math.min( minY, point.getY() );
					maxY = Math.max( maxY, point.getY() );
				}
			}
		}

		final int sizeX = ( int ) ( maxX - minX + 1 );
		final int sizeY = ( int ) ( maxY - minY + 1 );

		final Img< DoubleType > ret = new ArrayImgFactory< DoubleType >().create( new long[] { sizeX, sizeY, 1, sizeZ, clouds.size() }, new DoubleType() );

		final RandomAccess< DoubleType > ra = ret.randomAccess();
		int t = 0;
		for ( final List< PixelCloud2D< Double >> frame : clouds ) {
			int z = 0;
			for ( final PixelCloud2D< Double > slice : frame ) {
				for ( final Pixel2D< Double > point : slice.getPoints() ) {
					final int x = ( int ) ( point.getX() - minX );
					final int y = ( int ) ( point.getY() - minY );

					try {
					if ( !interpolated ) {
						ra.setPosition( new long[] { x, y, 0, z, t } );
						ra.get().add( new DoubleType( point.getData().doubleValue() ) );
					} else {
						final double fracX = 1. - ( point.getX() - Math.floor( point.getX() ) );
						final double fracY = 1. - ( point.getY() - Math.floor( point.getY() ) );

						ra.setPosition( new long[] { x, y, 0, z, t } );
						ra.get().add( new DoubleType( point.getData().doubleValue() * fracX * fracY ) );

						ra.setPosition( new long[] { x + 1, y, 0, z, t } );
						ra.get().add( new DoubleType( point.getData().doubleValue() * ( 1 - fracX ) * fracY ) );

						ra.setPosition( new long[] { x, y + 1, 0, z, t } );
						ra.get().add( new DoubleType( point.getData().doubleValue() * fracX * ( 1 - fracY ) ) );

						ra.setPosition( new long[] { x + 1, y + 1, 0, z, t } );
						ra.get().add( new DoubleType( point.getData().doubleValue() * ( 1 - fracX ) * ( 1 - fracY ) ) );
					}
					} catch ( final ArrayIndexOutOfBoundsException e ) {
						System.err.println( String.format( "ArrayIndexOutOfBounds while dumping point-counds: %d, %d, 1, %d, %d -- %f", x, y, z, t, point.getData().doubleValue() ) );
					}
				}
				z++;
			}
			t++;
		}

		return ret;
	}

	/**
	 * Creates an Img<double> from a List of Lists of Ellipse2D objects.
	 *
	 * @param allConcentrations
	 *
	 * @param clouds
	 */
	public static Img< DoubleType > dumpCircularShapes( final List< List< Circle >> shapes, final List< PlotData > allConcentrations ) {
		double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
		int sizeZ = 0;

		for ( final List< Circle > frame : shapes ) {
			sizeZ = Math.max( sizeZ, frame.size() );
			for ( final Circle slice : frame ) {
				if ( !slice.isZeroCircle() ) {
					final Rectangle2D box = slice.getBoundingBox();
					minX = Math.min( minX, box.getX() );
					maxX = Math.max( maxX, box.getX() + box.getWidth() );
					minY = Math.min( minY, box.getY() );
					maxY = Math.max( maxY, box.getY() + box.getHeight() );
				}
			}
		}

		final int sizeX = ( int ) ( maxX - minX + 1 );
		final int sizeY = ( int ) ( maxY - minY + 1 );

		final Img< DoubleType > ret = new ArrayImgFactory< DoubleType >().create( new long[] { sizeX, sizeY, 1, sizeZ, shapes.size() }, new DoubleType() );

//		final RandomAccess< DoubleType > ra = ret.randomAccess();
		int t = 0;
		for ( final List< Circle > frame : shapes ) {
			int z = 0;
			for ( final Circle c : frame ) {
//				for ( double i = 0.0; !slice.isZeroEllipse() && i < Math.PI * 2; i += 0.1 ) {
//					final Point2D p = slice.getPointAt( i );
//					ra.setPosition( new long[] { ( long ) ( p.getX() - minX ), ( long ) ( p.getY() - minY ), 0, z, t } );
//					ra.get().set( 1.0 );
//				}
				final IntervalView< DoubleType > imgSlice = Views.hyperSlice( Views.hyperSlice( ret, 4, t ), 3, z );
				final Cursor< DoubleType > cursor = Views.iterable( imgSlice ).cursor();
				while ( cursor.hasNext() ) {
					final DoubleType pixel = cursor.next();
					final int x = cursor.getIntPosition( 0 );
					final int y = cursor.getIntPosition( 1 );
					if ( c.contains( new Point2D( x + minX, y + minY ) ) ) {
//						pixel.set( 1.0 );
						pixel.set( allConcentrations.get( t ).getValueAt( z ) );
//					} else {
//						pixel.set( 0.0 );
					}
				}
				z++;
			}
			t++;
		}

		return ret;
	}

	/**
	 * Creates an Img<double> from a List of Lists of Ellipse2D objects.
	 *
	 * @param allConcentrations
	 *
	 * @param clouds
	 */
	public static Img< DoubleType > dumpEllipticalShapes( final List< List< Ellipse2D >> shapes, final List< PlotData > allConcentrations ) {
		double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
		int sizeZ = 0;

		for ( final List< Ellipse2D > frame : shapes ) {
			sizeZ = Math.max( sizeZ, frame.size() );
			for ( final Ellipse2D slice : frame ) {
				if ( !slice.isZeroEllipse() ) {
					final Rectangle2D box = slice.getBoundingBox();
					minX = Math.min( minX, box.getX() );
					maxX = Math.max( maxX, box.getX() + box.getWidth() );
					minY = Math.min( minY, box.getY() );
					maxY = Math.max( maxY, box.getY() + box.getHeight() );
//					if ( Double.isNaN( minX ) || Double.isNaN( minY ) ) {
//						System.out.println( "NaN" );
//					}
				}
			}
		}

		final int sizeX = ( int ) ( maxX - minX + 1 );
		final int sizeY = ( int ) ( maxY - minY + 1 );

		final Img< DoubleType > ret = new ArrayImgFactory< DoubleType >().create( new long[] { sizeX, sizeY, 1, sizeZ, shapes.size() }, new DoubleType() );

//		final RandomAccess< DoubleType > ra = ret.randomAccess();
		int t = 0;
		for ( final List< Ellipse2D > frame : shapes ) {
			int z = 0;
			for ( final Ellipse2D ellipse : frame ) {
//				for ( double i = 0.0; !slice.isZeroEllipse() && i < Math.PI * 2; i += 0.1 ) {
//					final Point2D p = slice.getPointAt( i );
//					ra.setPosition( new long[] { ( long ) ( p.getX() - minX ), ( long ) ( p.getY() - minY ), 0, z, t } );
//					ra.get().set( 1.0 );
//				}
				final IntervalView< DoubleType > imgSlice = Views.hyperSlice( Views.hyperSlice( ret, 4, t ), 3, z );
				final Cursor< DoubleType > cursor = Views.iterable( imgSlice ).cursor();
				while ( cursor.hasNext() ) {
					final DoubleType pixel = cursor.next();
					final int x = cursor.getIntPosition( 0 );
					final int y = cursor.getIntPosition( 1 );
					if ( ellipse.contains( new Point2D( x + minX, y + minY ) ) ) {
//						pixel.set( 1.0 );
						pixel.set( allConcentrations.get( t ).getValueAt( z ) );
//					} else {
//						pixel.set( 0.0 );
					}
				}
				z++;
			}
			t++;
		}

		return ret;
	}
}
