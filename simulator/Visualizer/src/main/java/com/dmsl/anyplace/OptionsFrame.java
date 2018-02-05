/**
 * Anyplace Simulator:  A trace-driven evaluation and visualization of IoT Data Prefetching in Indoor Navigation SOAs
 *
 * Author(s): Zacharias Georgiou, Panagiotis Irakleous

 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * Copyright (c) 2017 Data Management Systems Laboratory, University of Cyprus
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/
 */
package com.dmsl.anyplace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class OptionsFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SwingVisualization parent;
	public JPanel panel;
	public JButton bttnshowIsolatedNodes;

	public JButton bttnShowSimulationFrame;
	public boolean SimulationFrameVisibility;
	public SimulationFrame simulationFrame;

	public OptionsFrame(SwingVisualization parent, String title) {
		super(title);
		this.parent = parent;
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				parent.optionsVisibility = !parent.optionsVisibility;
				OptionsFrame.this.setVisible(parent.optionsVisibility);
			}
		});

		// Create Simulation Frame
		simulationFrame = new SimulationFrame(this, "Simulation");
		simulationFrame.setAlwaysOnTop(true);
		simulationFrame.setSize(300, 280);
		simulationFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		simulationFrame.setVisible(SimulationFrameVisibility);

		panel = new JPanel();
		panel.setLayout(new GridLayout(5, 1));

		addButtons();
		this.add(panel);
	}

	void addButtons() {
		bttnshowIsolatedNodes = new JButton("Find Isolated Nodes");
		bttnshowIsolatedNodes.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String nodes = parent.building.findIsolatedNodes();
				System.out.println("Isolated nodes:");
				System.out.println(nodes);
			}
		});
		panel.add(bttnshowIsolatedNodes);

		bttnShowSimulationFrame = new JButton("Run simulation");
		bttnShowSimulationFrame.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SimulationFrameVisibility = !SimulationFrameVisibility;
				simulationFrame.setLocationRelativeTo(OptionsFrame.this);
				simulationFrame.setVisible(SimulationFrameVisibility);

			}
		});
		panel.add(bttnShowSimulationFrame);
	}


}
