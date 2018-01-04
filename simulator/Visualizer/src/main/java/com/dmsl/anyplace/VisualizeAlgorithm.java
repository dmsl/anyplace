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

import com.dmsl.anyplace.algorithms.DatasetCreator;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VisualizeAlgorithm {
	public static final Logger LOGGER = Logger.getGlobal();

	private static final long RANDOM_SEED = 1456737260395L;
	private static final Random RANDOM = new Random(RANDOM_SEED);
	private static List<Integer> selectedPath;
	private static int pathIndex;
	private static VSME1 algo;

	private static boolean isSimulationRunning;

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {

		// Start the frame in a new thread
		EventQueue.invokeAndWait(() -> {
			JFrame frame = new JFrame();

			frame.setResizable(false);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			final JPanel rootPanel = new JPanel(new GridLayout(1, 2));
			final JPanel leftPanel = new JPanel(new BorderLayout());

			final JPanel leftPanelTop = new JPanel();
			final JPanel leftPanelBottom = new JPanel();

			final JPanel rightPanel = new JPanel();
			// Left Panel Top
			leftPanelTop.setBorder(BorderFactory.createTitledBorder("Controller"));
			leftPanelTop.add(ControlPanel.INSTANCE, BorderLayout.CENTER);

			leftPanelBottom.setBorder(BorderFactory.createTitledBorder("Map"));
			leftPanelBottom.add(MapPanel.INSTANCE, BorderLayout.CENTER);

			leftPanel.add(leftPanelTop, BorderLayout.NORTH);
			leftPanel.add(leftPanelBottom, BorderLayout.CENTER);

			rootPanel.add(leftPanel);

			// Right Panel
			rightPanel.setBorder(BorderFactory.createTitledBorder("Terminal"));
			rightPanel.add(TerminalPanel.INSTANCE, BorderLayout.CENTER);

			rootPanel.add(rightPanel);

			frame.add(rootPanel);
			frame.setSize(720, 480);
			frame.pack();

			frame.setVisible(true);

		});
	}

	/**
	 * This panel is the controller
	 * 
	 * @author zgeorg03
	 *
	 */
	static class ControlPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final JPanel rootPanel = new JPanel();
		private static final JPanel pairsPanel = new JPanel(new GridLayout(6, 2));
		private static final JPanel bottomPanel = new JPanel();

		// Control Parameters
		private CleanBuilding building;
		private DatasetCreator dataset;

		public int MIN_FLOOR = -1; // TODO Fix this min and max floors
		public int MAX_FLOOR = 2;

		// Buildings
		private final JLabel lblSelectedBuilding = new JLabel("Building ID:");
		private final JComboBox<Object> comboboxFiles;

		// Paths
		private final JLabel lblSelectedPath = new JLabel("Path ID:");
		private JComboBox<Object> comboboxPath;

		// Algorithm
		private final JLabel lblAlgorithm = new JLabel("Algorithm:");
		private final JComboBox<Object> comboboxAlgorithm = new JComboBox<>(new Object[] { "ME1", });

		// Num of Blocks

		private final JLabel lblNumBlocks = new JLabel("Num Blocks:");
		private final JTextField txtNumBlocks = new JTextField();

		// Param 1
		private final JLabel lblParam1 = new JLabel("Param 1:");
		private final JTextField txtParam1 = new JTextField();

		// Param 2
		private final JLabel lblParam2 = new JLabel("Param 2:");
		private final JTextField txtParam2 = new JTextField();

		private final JButton bttnClearAll = new JButton("Clear All");
		private final JButton bttnStartSimulation = new JButton("Start Sim");
		private final JButton bttnResetSimulation = new JButton("Reset Sim");
		private final JButton bttnNextStep = new JButton("Next Step");

		public static final ControlPanel INSTANCE = new ControlPanel();

		private ControlPanel() {
			rootPanel.setPreferredSize(new Dimension(440, 160));
			final ArrayList<String> fileNames = new ArrayList<>();
			VSUtil.loadFiles(DataConnector.GRAPH_PATH, fileNames);

			LOGGER.info(fileNames.size() + " building found");
			comboboxFiles = new JComboBox<>(fileNames.toArray());
			// comboboxFiles.setPreferredSize(new Dimension(80, 20));
			try {
				ControlPanel.this.building = new CleanBuilding((String) (comboboxFiles.getSelectedItem()), false);

			} catch (Exception e) {
				LOGGER.severe((e.getMessage()));
			}

			try {
				dataset = new DatasetCreator(building.getBuid(), RANDOM_SEED, true);
				Integer temp[] = new Integer[dataset.getDataset().size()];
				for (int i = 0; i < temp.length; i++)
					temp[i] = i;
				comboboxPath = new JComboBox<>(temp);
			} catch (Exception e) {
				LOGGER.severe((e.getMessage()));
			}

			pairsPanel.setPreferredSize(new Dimension(400, 120));

			// Building selection
			pairsPanel.add(lblSelectedBuilding);
			pairsPanel.add(comboboxFiles);

			// Path selection
			pairsPanel.add(lblSelectedPath);
			pairsPanel.add(comboboxPath);

			// Algorithm selection
			pairsPanel.add(lblAlgorithm);
			pairsPanel.add(comboboxAlgorithm);

			pairsPanel.add(lblNumBlocks);
			txtNumBlocks.setText("" + 3);
			pairsPanel.add(txtNumBlocks);

			// Param 1
			pairsPanel.add(lblParam1);
			txtParam1.setText("0.5");
			pairsPanel.add(txtParam1);

			// Param 12
			pairsPanel.add(lblParam2);
			txtParam2.setText("0.5");
			pairsPanel.add(txtParam2);

			// Clear all
			rootPanel.add(pairsPanel, BorderLayout.CENTER);

			bottomPanel.add(bttnClearAll);
			bottomPanel.add(bttnStartSimulation);
			bttnResetSimulation.setEnabled(false);
			bottomPanel.add(bttnResetSimulation);
			bttnNextStep.setEnabled(false);
			bottomPanel.add(bttnNextStep);

			rootPanel.add(bottomPanel, BorderLayout.SOUTH);

			addListeners();
			this.add(rootPanel);
		}

		private void selectPath() {
			Integer selected = (Integer) comboboxPath.getModel().getSelectedItem();
			selectedPath = dataset.getDataset().get(selected);
			final String path = selectedPath.stream().map(x -> x.toString())
					.collect(Collectors.joining(" -> ", "Path : {", "}"));
			TerminalPanel.INSTANCE.writeln(path);
			TerminalPanel.INSTANCE.separate();
			MapPanel.INSTANCE.selectedPathIndex = selected;

			selected = selectedPath.get(0);

			CleanPoi p[] = building.getVertices();
			Integer floor = Integer.parseInt(p[selected].getFloor());
			MapPanel.INSTANCE.currentFloor = floor;

			MapPanel.INSTANCE.vertices = ControlPanel.INSTANCE.building.getVerticesByFloor(floor + "");

			int tid = MapPanel.INSTANCE.findVertexID(selected);
			MapPanel.INSTANCE.selectedNode = tid;
			MapPanel.INSTANCE.oldSelected = tid;

		}

		private void addListeners() {
			comboboxFiles.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					@SuppressWarnings("unchecked")
					JComboBox<String> cb = (JComboBox<String>) e.getSource();
					String filename = (String) cb.getSelectedItem();

					LOGGER.info("Changed building to " + filename);
					MapPanel.INSTANCE.vertices = null;
					try {
						ControlPanel.this.building = new CleanBuilding(filename, false);
						dataset = new DatasetCreator(building.getBuid(), 1456737260395L, true);
						pairsPanel.remove(comboboxPath);
						Integer temp[] = new Integer[dataset.getDataset().size()];
						for (int i = 0; i < temp.length; i++)
							temp[i] = i;
						comboboxPath = new JComboBox<>(temp);
						pairsPanel.add(comboboxPath, 3);
						pairsPanel.updateUI();
					} catch (Exception e1) {
						LOGGER.severe((e1.getMessage()));
					}

				}
			});

			comboboxPath.addActionListener(event -> selectPath());

			bttnClearAll.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					TerminalPanel.txtArea.setText("");
					algo.reset(selectedPath.get(pathIndex));
					algo.toReturn=null;
					algo=null;
					pathIndex=0;
					
				}
			});

			bttnStartSimulation.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int nofblocks = 0;
					float a1, a2;
					try {
						nofblocks = Integer.parseInt(txtNumBlocks.getText().trim());
					} catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(null, "Number of blocks should be an integer");
						return;
					}
					try {
						a1 = (float) Double.parseDouble(txtParam1.getText().trim());
						a2 = (float) Double.parseDouble(txtParam2.getText().trim());
					} catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(null, "Parameters should be float");
						return;
					}
					txtNumBlocks.setEnabled(false);
					txtParam1.setEnabled(false);
					txtParam2.setEnabled(false);
					comboboxFiles.setEnabled(false);
					bttnNextStep.setEnabled(true);
					comboboxPath.setEnabled(false);
					comboboxAlgorithm.setEnabled(false);
					bttnResetSimulation.setEnabled(true);
					bttnStartSimulation.setEnabled(false);
					selectPath();
					isSimulationRunning = true;

					int startNode = selectedPath.get(0);
					int endNode = selectedPath.get(selectedPath.size() - 1);

					String algoname = comboboxAlgorithm.getSelectedItem().toString();

					if (algoname.equalsIgnoreCase("ME1")) {
						algo = new VSME1(building.getVertices(), nofblocks, RANDOM, a1, a2, startNode, endNode);
					}
					TerminalPanel.INSTANCE.writeln("Simulation started");
					TerminalPanel.INSTANCE.writeln("Step: " + (pathIndex + 1));
					TerminalPanel.INSTANCE.writeln(algo.getCurrentState(selectedPath.get(pathIndex)));

				}
			});

			bttnResetSimulation.addActionListener(event -> {
				txtNumBlocks.setEnabled(true);
				txtParam1.setEnabled(true);
				txtParam2.setEnabled(true);
				bttnNextStep.setEnabled(false);
				bttnResetSimulation.setEnabled(false);
				bttnStartSimulation.setEnabled(true);
				comboboxFiles.setEnabled(true);
				comboboxAlgorithm.setEnabled(true);
				comboboxPath.setEnabled(true);
				isSimulationRunning = false;
			});

			bttnNextStep.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (!algo.nextStep()) {
						pathIndex++;

						TerminalPanel.INSTANCE.writeln("Step: " + (pathIndex + 1));
						TerminalPanel.INSTANCE.writeln(algo.getCurrentState(selectedPath.get(pathIndex)));

						if (pathIndex + 1  == selectedPath.size()) {
							bttnResetSimulation.doClick();
							pathIndex=0;
						}
						algo.reset(selectedPath.get(pathIndex));
					}
					MapPanel.INSTANCE.oldSelected = MapPanel.INSTANCE.findVertexID(algo.currentNode.u);

				}
			});
		}
	}

	/**
	 * This panel is the terminal to show information
	 * 
	 * @author zgeorg03
	 *
	 */
	static class TerminalPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int MAX_ROWS = 40;
		private static final JPanel rootPanel = new JPanel();

		private static final JTextArea txtArea = new JTextArea(MAX_ROWS, 40);
		private static final JScrollPane areaScrollPane = new JScrollPane(txtArea);

		private TerminalPanel() {

			((DefaultCaret) txtArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			txtArea.setLineWrap(true);
			txtArea.setEditable(false);
			rootPanel.add(areaScrollPane);

			this.add(rootPanel, BorderLayout.CENTER);
		}

		public static final TerminalPanel INSTANCE = new TerminalPanel();

		public void write(String txt) {
			txtArea.append(txt);
		}

		public void writeln(String txt) {
			txtArea.append(txt + "\n");
		}

		public void separate() {
			TerminalPanel.INSTANCE.writeln("--------------------------------------------"
					+ "------------------------------------------" + "--------------------");

		}

	}

	/**
	 * This panel shows the graph
	 * 
	 * @author zgeorg03
	 *
	 */
	static class MapPanel extends JPanel implements Runnable {
		private static final long serialVersionUID = 1L;

		// Selected Path
		public int selectedPathIndex;

		public ArrayList<CleanPoi> vertices;// The vertices to paint
		public int currentFloor; // Current floor
		public int lastFloor;
		public static int WIDTH = 480;
		public static int HEIGHT = 480;
		public static float SENSITIVITY = 0.1f;
		public static float zoom = 0.1f;
		public static int centerX = 0;
		public static int centerY = 0;
		public boolean showIDs;
		public boolean showPR;
		public boolean showPREdges;
		public boolean showSignificanceLevel;
		int selectedNode = -1;
		int oldSelected = -1;

		class ShowOptionsRightClick extends JPopupMenu {
			private static final long serialVersionUID = 1L;

			JMenuItem showInformation;
			JMenuItem showIds;
			JMenuItem showPREdges;

			public ShowOptionsRightClick() {

				showInformation = new JMenuItem("Show Info");
				showInformation.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {

						final CleanPoi selected = vertices.get(oldSelected);
						final CleanPoi[] allVertices = ControlPanel.INSTANCE.building.getVertices();

						List<String> mypairs = new ArrayList<>();
						String midFormat = String.format("%3d", selected.getId());
						String mprFormat = String.format("%.4f", selected.getPagerank());
						mypairs.add(VSUtil.getPair("ID", midFormat));
						mypairs.add(VSUtil.getPair("PR", mprFormat));

						TerminalPanel.INSTANCE.writeln("Node:\t" + VSUtil.likeJSON(mypairs));
						TerminalPanel.INSTANCE.writeln("Neighbours:");
						for (Pair p : selected.getNeighbours()) {

							CleanPoi neigh = allVertices[p.getTo()];
							List<String> pairs = new ArrayList<>();
							String weight = String.format("%.4f", p.getWeight());
							String idFormat = String.format("%3d", neigh.getId());
							String prFormat = String.format("%.4f", neigh.getPagerank());
							pairs.add(VSUtil.getPair("ID", idFormat));
							pairs.add(VSUtil.getPair("PR", prFormat));
							pairs.add(VSUtil.getPair("WEIGHT", weight));
							TerminalPanel.INSTANCE.writeln("\t" + VSUtil.likeJSON(pairs));
						}
						TerminalPanel.INSTANCE.separate();
					}

				});

				if (showIDs)
					showIds = new JMenuItem("Hide ID's");
				else
					showIds = new JMenuItem("Show ID's");

				showIds.addActionListener(event -> showIDs = !showIDs);

				if (oldSelected == -1)
					showInformation.setEnabled(false);

				if (MapPanel.this.showPREdges)
					showPREdges = new JMenuItem("Hide weights");
				else
					showPREdges = new JMenuItem("Show weights");

				showPREdges.addActionListener(event -> MapPanel.this.showPREdges = !MapPanel.this.showPREdges);

				add(showInformation);
				add(showIds);
				add(showPREdges);
			}
		}

		private MapPanel() {
			setPreferredSize(new Dimension(480, 480));
			this.addMouseWheelListener(new MouseWheelListener() {

				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					int notches = e.getWheelRotation();

					if (zoom + ((float) notches * -0.005f) > 1)
						return;
					if (zoom + ((float) notches * -0.005f) < 0.1f)
						return;
					zoom += (float) notches * -0.005f;
				}
			});
			this.addMouseListener(new MouseAdapter() {

				private void doPop(MouseEvent e) {
					ShowOptionsRightClick menu = new ShowOptionsRightClick();
					menu.show(e.getComponent(), e.getX(), e.getY());
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger())
						doPop(e);
				}

				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger())
						doPop(e);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// JOptionPane.showMessageDialog(null, "Entered Panel");
					MapPanel.this.requestFocus();
				}

			});

			// Drag
			this.addMouseMotionListener(new MouseMotionListener() {
				Point lp = new Point();
				boolean isFirst = true;

				@Override
				public void mouseMoved(MouseEvent e) {

					isFirst = true;
					if (VisualizeAlgorithm.isSimulationRunning)
						return;
					selectedNode = -1;

					int x = e.getX();
					int y = e.getY();

					int i = 0;

					for (CleanPoi p : vertices) {
						if (p == null)
							continue;
						// Change POI
						if (Math.abs(p.getX() + 5 - x) <= 5 && Math.abs(p.getY() + 5 - y) <= 5) {
							if (i == oldSelected)
								return;

							selectedNode = i;
							// System.out.println(vertices.get(selectedNode).getPid());
						}
						i++;
					}
					if (selectedNode != -1)
						oldSelected = selectedNode;

				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (isFirst) {
						lp = e.getPoint();
						isFirst = false;
					}
					Point p = e.getPoint();
					float diffX = (float) (p.getX() - lp.getX());
					float diffY = (float) (p.getY() - lp.getY());
					centerX += (diffX * (1 / (zoom))) * SENSITIVITY;
					centerY += (diffY * (1 / (zoom))) * SENSITIVITY;
					lp = p;

				}
			});

			this.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(KeyEvent e) {

					switch (e.getKeyCode()) {
					case KeyEvent.VK_RIGHT:
						centerX += (-30 * (1 / (zoom))) * SENSITIVITY;
						break;
					case KeyEvent.VK_LEFT:
						centerX += (30 * (1 / (zoom))) * SENSITIVITY;
						break;
					case KeyEvent.VK_UP:
						centerY += (30 * (1 / (zoom))) * SENSITIVITY;
						break;
					case KeyEvent.VK_DOWN:
						centerY += (-30 * (1 / (zoom))) * SENSITIVITY;
						break;

					case KeyEvent.VK_MINUS:
					case KeyEvent.VK_SUBTRACT:

						if (zoom + ((float) 1 * -0.005f) < 0.09)
							return;
						zoom += (float) 1 * -0.005f;
						break;
					case KeyEvent.VK_ADD:
					case KeyEvent.VK_PLUS:

						if (zoom + ((float) -1 * -0.005f) < 0.1f)
							return;
						zoom += (float) -1 * -0.005f;
						break;

					}

				}

				@Override
				public void keyReleased(KeyEvent e) {

					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						selectedNode = -1;
						oldSelected = -1;
						return;
					}
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						if (currentFloor == ControlPanel.INSTANCE.MAX_FLOOR)
							return;
						lastFloor = currentFloor;
						currentFloor++;

						selectedNode = -1;
						oldSelected = -1;
						LOGGER.info("Changed to floor " + currentFloor);
						return;
					}

					if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						if (currentFloor == ControlPanel.INSTANCE.MIN_FLOOR)
							return;
						lastFloor = currentFloor;
						currentFloor--;

						selectedNode = -1;
						oldSelected = -1;
						LOGGER.info("Changed to floor " + currentFloor);
					}
				}
			});
			new Thread(this).start();
		}

		public static final MapPanel INSTANCE = new MapPanel();

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			paintVertices(g);
			paintEdges(g);

			g.setColor(Color.WHITE);
			g.drawString("Floor: " + currentFloor, 0, getHeight() - 10);
		}

		private void paintVertices(Graphics g) {

			if (vertices == null || lastFloor != currentFloor)
				vertices = ControlPanel.INSTANCE.building.getVerticesByFloor(currentFloor + "");

			double x[] = new double[vertices.size()];
			double y[] = new double[vertices.size()];

			double minLat = 1000000000;
			double minLon = 1000000000;
			double maxLat = -100000000;
			double maxLon = -100000000;

			int i = 0;
			for (CleanPoi p : vertices) {
				if (p == null)
					continue;
				double lat = Double.parseDouble(p.getLat());
				double lon = Double.parseDouble(p.getLon());
				x[i] = lat;
				y[i] = lon;
				if (lat < minLat)
					minLat = lat;
				if (lat > maxLat)
					maxLat = lat;
				if (lon < minLon)
					minLon = lon;
				if (lon > maxLon)
					maxLon = lon;
				i++;
			}
			double diffLat = maxLat - minLat;
			double diffLon = maxLon - minLon;
			i = 0;
			for (CleanPoi p : vertices) {
				if (p == null)
					continue;
				double lat = x[i];
				double lon = y[i];

				int xx = centerX + (int) ((lat - minLat) / diffLat * WIDTH * 10 * zoom);
				int yy = centerY + (int) ((lon - minLon) / diffLon * HEIGHT * 10 * zoom);
				p.setX(xx);
				p.setY(yy);

				if (VisualizeAlgorithm.algo != null && VisualizeAlgorithm.algo.toReturn != null) {

					if (i == oldSelected) {
						g.setColor(Color.YELLOW);
						g.fillOval((int) (xx), (int) (yy), 12, 12);
					} else if (VisualizeAlgorithm.algo.isContainedInReturned(p.getId())) {
						g.setColor(Color.BLUE);
						g.fillOval((int) (xx), (int) (yy), 10, 10);
					} else {
						g.setColor(Color.RED);
						g.fillOval(xx, yy, 10, 10);
					}
				} else {
					if (i == oldSelected) {
						g.setColor(Color.YELLOW);
						g.fillOval((int) (xx), (int) (yy), 12, 12);
					} else {
						g.setColor(Color.RED);
						g.fillOval(xx, yy, 10, 10);
					}
				}
				if (showIDs) {

					g.setColor(Color.WHITE);
					g.drawString(p.getId() + "", xx - 2, yy + 7);

				}
				if (showPR) {
					g.setColor(Color.WHITE);
					g.drawString(String.format("%.3f", p.getPagerank()), (xx - 2), yy + 7);
				}
				i++;
			}

		}

		private void paintEdges(Graphics g) {

			for (CleanPoi p : vertices) {
				if (p == null)
					continue;
				int xs = (int) p.getX();
				int ys = (int) p.getY();
				for (Pair neigh : p.getNeighbours()) {

					CleanPoi v = findVertex(neigh.getTo());
					// Other floor
					if (v == null) {
						continue;
					}
					int xf = (int) v.getX();
					int yf = (int) v.getY();
					float prob = (float) neigh.getWeight() * 100;
					String w = String.format("%.0f", prob);

					int idU = p.getId();
					int idV = v.getId();

					// if (containedInRandomPath(idU, idV) ||
					// containedInSelectedPath(idU, idV))
					// g.setColor(Color.BLUE);
					// else

					if (containedInSelectedPath(idU, idV)) {
						g.setColor(Color.BLUE);
					} else {
						g.setColor(Color.GREEN);
					}
					g.drawLine(xs + 5, ys + 5, xf + 5, yf + 5);

					if (showPREdges) {
						g.setColor(Color.WHITE);
						g.drawString(w, (xf - xs) / 3 + xs, (yf - ys) / 3 + ys);
					}
					if (showSignificanceLevel) {
						g.setColor(Color.WHITE);
						int sigLevel = neigh.getSigLevel();
						g.drawString(sigLevel + "", (xf - xs) / 3 + xs, (yf - ys) / 3 + ys);
					}
				}

			}
		}

		private boolean containedInSelectedPath(int cU, int cV) {

			if (selectedPath == null)
				return false;
			for (int i = 0; i < selectedPath.size() - 1; i++) {
				if (selectedPath.get(i) == cU && selectedPath.get(i + 1) == cV)
					return true;
				if (selectedPath.get(i) == cV && selectedPath.get(i + 1) == cU)
					return true;
			}
			return false;
		}

		public CleanPoi findVertex(int id) {
			for (CleanPoi p : vertices) {
				if (p == null)
					continue;
				if (p.getId() == id)
					return p;
			}
			return null;
		}

		public int findVertexID(int id) {
			int i = 0;
			for (CleanPoi p : vertices) {

				if (p.getId() == id)
					return i;
				i++;
			}
			return i;
		}

		@Override
		public void run() {

			while (true) {

				repaint();
				try {
					TimeUnit.MILLISECONDS.sleep(20);
				} catch (InterruptedException e) {

				}
			}

		}

	}

}
