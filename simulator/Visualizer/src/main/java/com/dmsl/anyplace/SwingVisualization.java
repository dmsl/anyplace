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

import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SwingVisualization extends JPanel implements Runnable {
	private static final long serialVersionUID = 1L;

	public static int WIDTH = 720;
	public static int HEIGHT = 540;
	public static float SENSITIVITY = 0.5f;
	public static float zoom = 0.1f;
	public static int centerX = 0;
	public static int centerY = 0;

	// This is a frame with different optionswriteToFile
	public OptionsFrame options;
	public boolean optionsVisibility;

	public ArrayList<String> fileNames = new ArrayList<>();
	public CleanBuilding building;
	public int MIN_FLOOR = -1;
	public int MAX_FLOOR = 2;
	public ArrayList<CleanPoi> vertices;

	public JFrame parent;
	public JMenuBar menuBar;

	public JPanel bottomPanel;
	public JComboBox<Object> comboboxFiles;
	public JButton bttnFind;
	public JTextField txtFind;
	public JButton bttnMoreOptions;

	// Top Menu
	public JMenuItem plusFloor;
	public JMenuItem minusFloor;
	public JMenuItem bttnFindPath;

	public JCheckBox chkShowPREdges;
	public JCheckBox chkShowIDs;
	public JCheckBox chkShowPR;
	public JCheckBox chkShowSignficanceLevel;
	public boolean showPREdges;
	public boolean showSignificanceLevel;
	public boolean showIDs;
	public boolean showPR;

	public JTextField txtPath;

	public JLabel labelFloor;
	Font font14 = new Font("Helvetica", Font.LAYOUT_LEFT_TO_RIGHT, 14);
	Font font12 = new Font("Helvetica", Font.LAYOUT_LEFT_TO_RIGHT, 12);

	public int currentFloor;

	/*
	 * 
	 */

	int selectedNode = -1;
	int oldSelected = -1;

	// PATH
	List<Integer> path;

	public SwingVisualization(JFrame parent, String buid) throws Exception {
		loadFiles(DataConnector.GRAPH_PATH);

		if(fileNames.size()==0)
			throw new Exception(DataConnector.GRAPH_PATH +" doesn't contain the graphs. Make sure you run DataConnector");
		this.building = new CleanBuilding(fileNames.get(0), false);
		this.parent = parent;

		menuBar = new JMenuBar();
		createMenu();
		parent.setJMenuBar(menuBar);
		bottomPanel = new JPanel();
		parent.add(bottomPanel, BorderLayout.SOUTH);
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

		// Drag
		MouseMotionListener mouseMotionListener = new MouseMotionListener() {
			Point lp = new Point();
			boolean isFirst = true;

			@Override
			public void mouseMoved(MouseEvent e) {
				isFirst = true;
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
		};
		this.addMouseMotionListener(mouseMotionListener);
	
		// Add FileOption
		comboboxFiles = new JComboBox<>(fileNames.toArray());
		comboboxFiles.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox<Object> cb = (JComboBox<Object>) e.getSource();
				String filename = (String) cb.getSelectedItem();
				System.out.println(filename);
				try {
					SwingVisualization.this.building = new CleanBuilding(filename, false);
				} catch (Exception e1) {

				}

			}
		});
		bottomPanel.add(comboboxFiles);

		// Add Search
		addSearch();
		bttnMoreOptions = new JButton("More options");
		bttnMoreOptions.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				optionsVisibility = !optionsVisibility;
				options.setVisible(optionsVisibility);

			}
		});
		bottomPanel.add(bttnMoreOptions);

		options = new OptionsFrame(this, "Options");
		options.setSize(200, 400);
		options.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		options.setAlwaysOnTop(true);
		options.setVisible(optionsVisibility);

		new Thread(this).start();
	}

	private void addSearch() {
		bttnFind = new JButton("Find");
		txtFind = new JTextField(5);
		txtFind.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n')
					bttnFind.doClick();

			}

			@Override
			public void keyReleased(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		});

		bttnFind.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String f = txtFind.getText();

				Integer id = 0;
				try {
					id = Integer.parseInt(f);
				} catch (NumberFormatException ex) {
					System.out.println("Insert a number");
					return;
				}

				CleanPoi p[] = building.getVertices();
				Integer floor = Integer.parseInt(p[id].getFloor());
				currentFloor = floor;
				labelFloor.setText("Floor: " + currentFloor);
				vertices = building.getVerticesByFloor(currentFloor + "");
				int tid = findVertexID(id);
				selectedNode = tid;
				oldSelected = tid;
				System.out.println("PID:" + findVertex(id).getPid());

			}
		});

		bottomPanel.add(txtFind);
		bottomPanel.add(bttnFind);

	}

	private void createMenu() {

		minusFloor = new JMenuItem("", UIManager.getIcon("Table.descendingSortIcon"));
		minusFloor.setMnemonic(KeyEvent.VK_J);
		minusFloor.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Lower floor");
				if (currentFloor == MIN_FLOOR)
					return;
				currentFloor--;
				labelFloor.setText("Floor: " + currentFloor);
				selectedNode = -1;
				oldSelected = -1;

			}
		});

		menuBar.add(minusFloor);

		plusFloor = new JMenuItem("", UIManager.getIcon("Table.ascendingSortIcon"));
		plusFloor.setMnemonic(KeyEvent.VK_K);
		plusFloor.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (currentFloor == MAX_FLOOR)
					return;
				currentFloor++;
				labelFloor.setText("Floor: " + currentFloor);
				selectedNode = -1;
				oldSelected = -1;
			}
		});

		menuBar.add(plusFloor);

		JMenu menu = new JMenu("Random Path");
		bttnFindPath = new JMenuItem("Find");

		bttnFindPath.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (oldSelected < 0) {
					System.err.println("Select a node");
					return;
				}
				int num = 0;
				try {
					num = Integer.parseInt(txtPath.getText().trim());
				} catch (NumberFormatException ex) {
					System.err.println("Insert an integer");
					return;
				}

				int realId = vertices.get(oldSelected).getId();
				ArrayList<List<Integer>> paths = SwingVisualization.this.building.BFS(realId, num);

				if (paths.size() == 0) {
					System.err.println("Path with " + num + " nodes doesn't exist");
					path = null;
					return;
				}
				int rand = (int) (Math.random() * paths.size());
				path = paths.get(rand);

			}
		});

		txtPath = new JTextField(3);
		txtPath.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {

				if (e.getKeyChar() == '\n')
					bttnFindPath.doClick();
			}

		});
		txtPath.setText("10");
		menu.add(txtPath);
		menu.add(bttnFindPath);

		menuBar.add(menu);

		// Checkboxes
		chkShowPR = new JCheckBox("PR");
		chkShowPR.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showPR = !showPR;

			}
		});
		menuBar.add(chkShowPR);

		// Checkboxes
		chkShowPREdges = new JCheckBox("Weights");
		chkShowPREdges.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showPREdges = !showPREdges;

			}
		});
		menuBar.add(chkShowPREdges);

		chkShowSignficanceLevel = new JCheckBox("Significance Level");
		chkShowSignficanceLevel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showSignificanceLevel = !showSignificanceLevel;

			}
		});
		menuBar.add(chkShowSignficanceLevel);

		chkShowIDs = new JCheckBox("ID's");
		chkShowIDs.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showIDs = !showIDs;

			}
		});
		menuBar.add(chkShowIDs);

		labelFloor = new JLabel("Floor:" + currentFloor);
		menuBar.add(labelFloor);

	}

	public void paintComponent(Graphics g) {
		g.setFont(font12);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());

		paintVertices(g);
		paintEdges(g);

		paintText(g);

	}

	private void paintText(Graphics g) {
		g.setFont(font14);
		g.setColor(Color.WHITE);
		g.drawString("Selected:", 4, 16);
		if (oldSelected < 0)
			g.drawString("None", 4 + 80, +16);
		else {
			CleanPoi v = vertices.get(oldSelected);
			g.drawString("(" + v.getId() + ") " + v.getName(), 4 + 80, +16);
		}
	}

	private void paintEdges(Graphics g) {

		int i = 0;
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

				if (containedInRandomPath(idU, idV) || containedInSelectedPath(idU, idV))
					g.setColor(Color.BLUE);
				else
					g.setColor(Color.GREEN);
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
			i++;

		}
	}

	private boolean containedInRandomPath(int cU, int cV) {
		if (path == null)
			return false;
		for (int i = 0; i < path.size() - 1; i++) {
			if (path.get(i) == cU && path.get(i + 1) == cV)
				return true;
			if (path.get(i) == cV && path.get(i + 1) == cU)
				return true;
		}
		return false;
	}

	private boolean containedInSelectedPath(int cU, int cV) {
		path = options.simulationFrame.selectedPath;
		if (path == null)
			return false;
		for (int i = 0; i < path.size() - 1; i++) {
			if (path.get(i) == cU && path.get(i + 1) == cV)
				return true;
			if (path.get(i) == cV && path.get(i + 1) == cU)
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

	private void paintVertices(Graphics g) {

		vertices = building.getVerticesByFloor(currentFloor + "");
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

			int index = options.simulationFrame.selectedPathIndex;
			if (options.simulationFrame.downloadedVertices != null && index != -1) {

				if (i == oldSelected) {
					g.setColor(Color.YELLOW);
					g.fillOval((int) (xx), (int) (yy), 12, 12);
				} else if (options.simulationFrame.downloadedVertices.get(index).contains(p.getId())) {
					g.setColor(Color.PINK);
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

	@Override
	public void run() {
		while (true) {

			repaint();
			try {
				Thread.sleep(10);

			} catch (InterruptedException e) {

			}
		}
	}

	public void loadFiles(String filename) {

		File directory = new File(filename);

		// get all the files from a directory
		File[] fList = directory.listFiles();
		if(fList==null)
			return;
		for (File file : fList) {
			if (file.isFile()) {
				if (file.getName().contains(".typeDescription"))
					continue;
				String toks[] = file.getName().split("\\." + DataConnector.FILE_FORMAT);
				fileNames.add(toks[0]);
			} else if (file.isDirectory()) {
				loadFiles(file.getAbsolutePath());
			}
		}
	}

	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {

				JFrame frame = new JFrame("Anyplace-Graph");
				frame.setSize(WIDTH, HEIGHT);
				frame.setLocationRelativeTo(null);
				try {
					frame.add(new SwingVisualization(frame, null));
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setVisible(true);
				} catch (Exception e) {
					System.err.println(e.getLocalizedMessage());
				}

			}
		});

	}

}
