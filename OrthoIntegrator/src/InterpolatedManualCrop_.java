/**
 *
 */

import ij.plugin.PlugIn;

import com.jug.InterpolatedCropPanel;

/**
 * @author jug
 */
public class InterpolatedManualCrop_ implements PlugIn {

	/**
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run( final String arg ) {
		InterpolatedCropPanel.main( null );
//		final InterpolatedManualCrop_Tool tool = new InterpolatedManualCrop_Tool( InterpolatedCropPanel.getInstance() );
//		tool.run( "" );
	}

	public static void main( final String[] args ) {
		final InterpolatedManualCrop_ main = new InterpolatedManualCrop_();
		main.run( "" );
	}
}
