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

import com.dmsl.anyplace.algorithms.CreateLookAhead;
import com.dmsl.anyplace.algorithms.blocks.*;
import com.dmsl.anyplace.algorithms.DatasetCreator;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimulationFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private OptionsFrame parent;
	private JPanel panel;
	private JPanel bottomPanel;
	private JLabel lblN;
	private JLabel lblConnectivity;
	private JLabel lblalgorithm;
	private JLabel lblSeed;
	private JLabel lblMaxSignalStrength;
	private JTextField txtN;
	private JTextField txtConnectivity;
	private JTextField txtAlgorithm;
	private JTextField txtSeed;
	private JTextField txtMaxSignalStrength;
	
	private JButton bttnLoadSimulation;
	private JButton bttnRunSimulation;

	private JList<Integer> paths;

	public int selectedPathIndex = -1;
	public List<Integer> selectedPath;
	public ArrayList<List<Integer>> downloadedVertices;
	private DatasetCreator dataset;



	public SimulationFrame(OptionsFrame parent, String title) {
		super(title);
		this.parent = parent;
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				parent.SimulationFrameVisibility = !parent.SimulationFrameVisibility;
				SimulationFrame.this.setVisible(parent.SimulationFrameVisibility);
			}
		});

		panel = new JPanel();
		panel.setLayout(new GridLayout(6, 2));

		bottomPanel = new JPanel();

		addButtons();
		this.add(panel, BorderLayout.CENTER);

		paths = new JList<>();
		paths.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		paths.addListSelectionListener(new ListListener());
		JScrollPane listScroller = new JScrollPane(paths);
		bottomPanel.add(listScroller);
		this.add(bottomPanel, BorderLayout.SOUTH);
	}

	private void addButtons() {
		lblN = new JLabel("#of Blocks");
		panel.add(lblN);
		txtN = new JTextField();
		txtN.setText("15");
		panel.add(txtN);

		lblConnectivity = new JLabel("Signal Lost Prob");
		panel.add(lblConnectivity);
		txtConnectivity = new JTextField();
		txtConnectivity.setText("40");
		panel.add(txtConnectivity);

		lblalgorithm = new JLabel("Algorithm");
		panel.add(lblalgorithm);
		txtAlgorithm = new JTextField();
		txtAlgorithm.setText("2");
		panel.add(txtAlgorithm);

		lblSeed = new JLabel("Seed");
		panel.add(lblSeed);
		txtSeed = new JTextField();
		txtSeed.setText("1456737260395");
		panel.add(txtSeed);

		lblMaxSignalStrength = new JLabel("Max Signal Strength(Db)");
		panel.add(lblMaxSignalStrength);
		txtMaxSignalStrength = new JTextField();
		txtMaxSignalStrength.setText("40");
		panel.add(txtMaxSignalStrength);
		
		bttnLoadSimulation = new JButton("Load Simulation");
		bttnLoadSimulation.addActionListener(new LoadSimulationFromFile());
		panel.add(bttnLoadSimulation);
		bttnRunSimulation = new JButton("Run Simulation");
		bttnRunSimulation.addActionListener(new RunSimulation());
		panel.add(bttnRunSimulation);

	}

	class ListListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (paths == null)
				return;
			if (e.getValueIsAdjusting() == false) {
				Integer x = paths.getSelectedValue();
				if (x == null){
					selectedPath=null;
					return;
					
				}
				selectedPathIndex = x;
				selectedPath = dataset.dataset.get(selectedPathIndex);
				System.out.println(selectedPath);
			}
		}

	}

	/**
	 * Runs a simulation
	 * 
	 * @author zgeorg03
	 *
	 */
	class RunSimulation implements ActionListener {

		private int N;
		private long seed;
		private float prob;
		private int algo;
		private int maxSignalStrength;
		
		@Override
		public void actionPerformed(ActionEvent e) {

			// ParseDetails
			try {
				parseDetails();
			} catch (Exception e2) {
				System.err.println("Details are not valid");
				return;
			}

			CleanBuilding building = SimulationFrame.this.parent.parent.building;
			
			dataset = null;

			try {
				dataset = new DatasetCreator(building.getBuid(), seed, true);
				building.parseFpFile();
			} catch (Exception e1) {
				e1.printStackTrace();
				return;
			}
			AlgorithmBlocks algorithm = null;
			switch (algo) {

			case 0:
				algorithm = new RandomBlocks(building.getVertices(), N, dataset.random, prob);
				break;
			case 1:
				algorithm = new BFSBlocks(building.getVertices(), N, dataset.random, prob);
				break;
			case 2:// Random
				algorithm = new ME2(building.getVertices(), N, dataset.random, prob,building);
				break;
			case 3:// Random
				algorithm = new ME1a(building.getVertices(), N, dataset.random, prob);
				break;
			case 4:// Random
				algorithm = new ME1b(building.getVertices(), N, dataset.random, prob);
				break;
			case 6:
				algorithm = new ME4a(building.getVertices(), N, dataset.random, prob, 3, 3,false,building);
				break;
			default:
				System.err.println("Algorithm doesn't exist");
				return;

			}
			
			CreateLookAhead lookahead = new CreateLookAhead(DataConnector.FINGERPRINTS_PATH
					+ building.getBuid() + "/all.pajek", maxSignalStrength);
			
			SimulationBlocks sim = new SimulationBlocks(N, building, dataset, algorithm,lookahead.getLookAhead());
			sim.runSimulation();
			try {
				sim.writeToFile();
				sim.writeNodesToFile();
			} catch (IOException e1) {

				e1.printStackTrace();
			}

			// Hide
			parent.SimulationFrameVisibility = !parent.SimulationFrameVisibility;
			SimulationFrame.this.setVisible(parent.SimulationFrameVisibility);

		}

		void parseDetails() throws Exception {
			N = Integer.parseInt(txtN.getText());
			prob = Float.parseFloat(txtConnectivity.getText());
			algo = Integer.parseInt(txtAlgorithm.getText());
			seed = Long.parseLong(txtSeed.getText());
			maxSignalStrength = Integer.parseInt(txtMaxSignalStrength.getText());
		}

	}

	/**
	 * Loads a simulation from file
	 * 
	 */
	class LoadSimulationFromFile implements ActionListener {
		private int N;
		private long seed;
		private float prob;
		private int algo;
		private int maxSignalStrength;
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				parseDetails();
			} catch (Exception e2) {
				System.err.println("Details are not valid");
				return;
			}
			dataset = null;
			CleanBuilding building = SimulationFrame.this.parent.parent.building;
			try {
				dataset = new DatasetCreator(building.getBuid(), seed, false);
				building.parseFpFile();
			} catch (Exception e1) {
				e1.printStackTrace();
				return;
			}

			if (dataset.dataset == null) {
				return;
			}

			Integer array[] = new Integer[dataset.dataset.size()];
			for (int i = 0; i < array.length; i++)
				array[i] = i;

			paths.setListData(array);
			
			AlgorithmBlocks algorithm = null;
			switch (algo) {

			case 0:
				algorithm = new RandomBlocks(building.getVertices(), N, dataset.random, prob);
				break;
			case 1:
				algorithm = new BFSBlocks(building.getVertices(), N, dataset.random, prob);
				break;
			case 2:// ME1
				algorithm = new ME2(building.getVertices(), N, dataset.random, prob,building);
				break;
			case 3:// Random
				algorithm = new ME1a(building.getVertices(), N, dataset.random, prob);
				break;
			case 4:// Random
				algorithm = new ME1b(building.getVertices(), N, dataset.random, prob);
				break;
			case 6:
				algorithm = new ME4a(building.getVertices(), N, dataset.random, prob, 3, 3,true,building);
				break;
			default:
				System.err.println("Algorithm doesn't exist");
				return;

			}
			
			CreateLookAhead lookahead = new CreateLookAhead(DataConnector.FINGERPRINTS_PATH
					+ building.getBuid() + "/all.pajek", maxSignalStrength);
			
			SimulationBlocks sim = new SimulationBlocks(N, building, dataset, algorithm,lookahead.getLookAhead());
		
			try {
				downloadedVertices = sim.parseFile();
			} catch (Exception e1) {

				System.out.println("[Info] File doesn't exist");

			}

		}

		void parseDetails() throws Exception {
			N = Integer.parseInt(txtN.getText());
			prob = Float.parseFloat(txtConnectivity.getText());
			algo = Integer.parseInt(txtAlgorithm.getText());
			seed = Long.parseLong(txtSeed.getText());
			maxSignalStrength = Integer.parseInt(txtMaxSignalStrength.getText());
			
		}

	}
}
