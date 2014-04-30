/**
 *
 */

import ij.IJ;
import ij.gui.Line;
import ij.plugin.PlugIn;

import com.jug.OrthoSlicer;

/**
 * @author jug
 */
public class SectionAreaAlongLine_ implements PlugIn {

	/**
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run( final String arg ) {
		final OrthoSlicer main = new OrthoSlicer();
		main.loadCurrentImage();

		try {
			final Line roi = ( Line ) main.imgPlus.getRoi();
			if ( roi == null ) {
				final Line lineRoi = new Line( 0, main.imgPlus.getHeight(), main.imgPlus.getWidth(), 0 );
				main.imgPlus.setRoi( lineRoi, true );
			}
		} catch ( final ClassCastException e ) {
			IJ.error( "Found a non-line ROI... please use the line-tool to indicate the slicing direction!" );
			return;
		}

		main.projectSectionAreaToLine( true );
	}

}
