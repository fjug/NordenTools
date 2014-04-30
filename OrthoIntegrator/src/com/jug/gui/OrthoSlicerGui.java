/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.jug.OrthoSlicer;


/**
 * @author jug
 */
public class OrthoSlicerGui extends JPanel implements ActionListener {

	private final OrthoSlicer slicer;

	/**
	 * Construction
	 *
	 * @param mmm
	 *            the MotherMachineModel to show
	 */
	public OrthoSlicerGui( final OrthoSlicer slicer ) {
		super( new BorderLayout() );
		this.slicer = slicer;
		buildGui();
	}

	/**
	 * Builds the GUI.
	 */
	private void buildGui() {
		final JButton btnGo = new JButton( "Just do it!" );
		btnGo.addActionListener( this );
		this.add( btnGo, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		slicer.projectConcentrationToLine( true );
	}

}
