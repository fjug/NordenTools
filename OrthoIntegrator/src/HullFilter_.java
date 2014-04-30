/**
 *
 */

import ij.plugin.PlugIn;

import com.jug.HullFilter;

/**
 * @author jug
 */
public class HullFilter_ implements PlugIn {

	/**
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run( final String arg ) {
		final HullFilter main = new HullFilter();
		if ( main.loadCurrentImage() ) {
			main.askUserForDetails();
		}
	}

}
