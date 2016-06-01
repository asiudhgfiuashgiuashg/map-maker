import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Scanner;
import java.util.NoSuchElementException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

public class MapGUI extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	private int tileSizeX;
	private int tileSizeY;
	private int tileCols;
	private int tileRows;

	private final int windowWidth = 800;
	private final int windowHeight = 600;

	private String defTilePath;
	private String workingDir = System.getProperty("user.dir");
	private String workToTileAsset = "../tile-game/core/assets/tileart/";
	private static String workToObjectAsset = "../tile-game/core/assets/objectart/";
	private String tileAssetDir = workingDir + "/" + workToTileAsset;
	private String objectAssetDir = workingDir + "/" + workToObjectAsset;

	private String selectedTile;
	private String relativeSelectedTile;
	private String selectedObject;
	private String relativeSelectedObject;
	private ImageWithURL selectedTileImage;
	private ImageWithURL selectedObjectImage;

	private String searchString = "";

	private String[][] idGrid;
	private Canvas tileCanvas;
	private GraphicsContext gc;
	private ImageView[][] tileImageViewGrid;

	// Panes
	final ScrollPane tileScrollPane = new ScrollPane();
	static final Pane tilePane = new Pane();
	GridPane tileGrid;

	// Add horizontal slider for tiles
	final ScrollPane selectScrollPane = new ScrollPane();
	final GridPane selectGridPane = new GridPane();

	private double zoomPercent;
	private TextField zoomField;
	

	private void updateTileCanvasDimensions() {
		tileCanvas.setWidth(tileSizeX * tileCols * (zoomPercent / 100));
		tileCanvas.setHeight(tileSizeY * tileRows * (zoomPercent / 100));
	}

	private void initCanvas() {
		tilePane.getChildren().clear();
		tileGrid = new GridPane();

		// Create new canvas
		tileCanvas = new Canvas();
		updateTileCanvasDimensions();
		gc = tileCanvas.getGraphicsContext2D();
		tilePane.getChildren().add(tileCanvas);
		tilePane.getChildren().get(0).setId("canvas");

		// On mouse click of master canvas, create new object
		tileCanvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.PRIMARY) {
					if (selectedObjectImage != null) {
						// Create object Image
						ImageWithURL objectImage = null;
						try {
							objectImage = new ImageWithURL("file:" + workToObjectAsset + relativeSelectedObject);
							double imgWidth = objectImage.getWidth();
							double imgHeight = objectImage.getHeight();
							objectImage = new ImageWithURL("file:" + workToObjectAsset + relativeSelectedObject, zoomPercent / 100 * imgWidth, zoomPercent / 100 * imgHeight, true, false, false);
							tilePane.getChildren().add(new CanvasObject(objectImage, imgWidth * (zoomPercent / 100), imgHeight * (zoomPercent / 100), relativeSelectedObject, fromScaled(event.getX(), zoomPercent), fromScaled(event.getY(), zoomPercent), zoomPercent));
						} catch (IllegalArgumentException e) {
							System.out.println("object file not found or file out of map-maker directory");
							System.exit(0);
						}
					}
				}
			}
		});

		



		// Add grid
		tileScrollPane.setContent(tilePane);
		tilePane.getChildren().add(tileGrid);
		tilePane.getChildren().get(1).setId("grid");

	}

	private void swapTilePaneChildren() {
		/*
		 *  Swaps the first and last items in tilePane to 
		 *  switch between tile and object editing
		 *  
		 *  In object edit mode, tilePane children order is
		 *  grid, canvas, object1, object2, ... objectN
		 *  so that the objects may be edited and the canvas be 
		 *  clicked on to add new objects
		 *  
		 *  In tile edit mode, grid and objectN are temporarily
		 *  switched so that the user can edit the tiles
		 */
		ObservableList<Node> children = FXCollections.observableArrayList(tilePane.getChildren());
		if (children.size() > 1) {
			Collections.swap(children, 0, children.size() - 1);
			tilePane.getChildren().setAll(children);
		}
	}

	private void drawSelectionTiles() {
		selectGridPane.getChildren().clear();

		// Count tile art in directory
		File tileArtDirFile = new File(tileAssetDir);
		File[] tileArtDirList = tileArtDirFile.listFiles();
		int childCount = 0;
		for (File child : tileArtDirList) {
			childCount += 1;
			String tileArtPath = child.toString();
			String relativePath = new File(tileAssetDir).toURI().relativize(child.toURI()).getPath();
			if (relativePath.contains(searchString)) {

				Image tileArtImage = null;
				try {
					tileArtImage = new ImageWithURL("file:" + workToTileAsset + relativePath);
				} catch (IllegalArgumentException e) {
					System.out.println("Tile file not found or something...");
					System.exit(0);
				}

				// First image in the directory sets the tile dimensions
				if (childCount == 1) {
					tileSizeX = (int) tileArtImage.getWidth();
					tileSizeY = (int) tileArtImage.getHeight();
				}

				ImageView selectImageView = new ImageView();
				selectImageView.setImage(tileArtImage);
				selectGridPane.add(selectImageView, childCount - 1, 0);

				Text selectImageName = new Text(relativePath);
				selectGridPane.add(selectImageName, childCount - 1, 1);

				// On mouse click of asset tiles
				selectImageView.setId(Integer.toString(childCount - 1));
				selectImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent event) {
						if (event.getButton() == MouseButton.PRIMARY) {
							int id = Integer.parseInt(selectImageView.getId());
							selectedTile = tileArtDirList[id].toString();
							relativeSelectedTile = new File(tileAssetDir).toURI().relativize(tileArtDirList[id].toURI()).getPath();
							try {
								selectedTileImage = new ImageWithURL("file:" + workToTileAsset + relativeSelectedTile);
							} catch (IllegalArgumentException e) {
								System.out.println("Tile file not found or something...");
								System.exit(0);
							}
						}
					}
				});
			}
		}
	}

	private void drawSelectionObjects() {
		selectGridPane.getChildren().clear();

		// Count tile art in directory
		File objectArtDirFile = new File(objectAssetDir);
		File[] objectArtDirList = objectArtDirFile.listFiles();
		int childCount = 0;
		for (File child : objectArtDirList) {
			childCount += 1;
			String objectArtPath = child.toString();
			String relativePath = new File(objectAssetDir).toURI().relativize(child.toURI()).getPath();
			if (relativePath.contains(searchString)) {

				Image objectArtImage = null;
				try {
					objectArtImage = new ImageWithURL("file:" + workToObjectAsset + relativePath);
				} catch (IllegalArgumentException e) {
					System.out.println("Object file not found or something...");
					System.exit(0);
				}

				ImageView selectImageView = new ImageView();
				selectImageView.setImage(objectArtImage);
				selectGridPane.add(selectImageView, childCount - 1, 0);

				Text selectImageName = new Text(relativePath);
				selectGridPane.add(selectImageName, childCount - 1, 1);

				// On mouse click of asset tiles
				selectImageView.setId(Integer.toString(childCount - 1));
				selectImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent event) {
						if (event.getButton() == MouseButton.PRIMARY) {
							int id = Integer.parseInt(selectImageView.getId());
							selectedObject = objectArtDirList[id].toString();
							relativeSelectedObject = new File(objectAssetDir).toURI().relativize(objectArtDirList[id].toURI()).getPath();
							try {
								selectedObjectImage = new ImageWithURL("file:" + workToObjectAsset + relativeSelectedObject);
							} catch (IllegalArgumentException e) {
								System.out.println("Object file not found or something...");
								System.exit(0);
							}
						}
					}
				});
			}
		}
	}

	private void fillTileGrid() {
		// Create canvas of grid size
		initCanvas();
		drawSelectionTiles();

		// Now fill in grid with tiles
		tileImageViewGrid = new ImageView[tileCols][tileRows];
		for (int i = 0; i < tileRows; i++) {
			for (int j = 0; j < tileCols; j++) {

				Image inpTileImage = null;
				try {
					inpTileImage = new ImageWithURL("file:" + workToTileAsset + idGrid[j][i], zoomPercent / 100 * tileSizeX, zoomPercent / 100 * tileSizeY, true, false, true);

				} catch (IllegalArgumentException e) {
					System.out.println("tile file not found or file out of map-maker directory");
					System.exit(0);
				}

				ImageView tileImageView = new ImageView();



				tileImageView.setImage(inpTileImage);
				tileGrid.add(tileImageView, j, i);
				tileImageViewGrid[j][i] = tileImageView;

				// On mouse click of grid tiles
				tileImageView.setId(Integer.toString(i) + ":" + Integer.toString(j));
				tileImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent event) {
						String idNumbers[] = tileImageView.getId().split(":");
						int x = Integer.parseInt(idNumbers[0]);
						int y = Integer.parseInt(idNumbers[1]);

						if (selectedTileImage != null) {
							//create a tile image scaled on the zoom percentage to place on the grid
							String selectedTileURL = selectedTileImage.getURL();
							ImageWithURL zoomedSelectedTileImage = new ImageWithURL(selectedTileURL, zoomPercent / 100 * tileSizeX, zoomPercent / 100 * tileSizeY, true, false, true);

							idGrid[y][x] = relativeSelectedTile;
							tileImageView.setImage(zoomedSelectedTileImage);
						}
					}
				});
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void createFileMenu(Stage primaryStage, Menu menuFile) {
		//////////////////////
		// New - dialog box //
		//////////////////////
		Dialog<String[]> newMapDialog = new Dialog<>();
		newMapDialog.setTitle("Create New Map");
		newMapDialog.setHeaderText(null);

		ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		newMapDialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

		GridPane newGrid = new GridPane();
		newGrid.setHgap(10);
		newGrid.setVgap(10);
		newGrid.setPadding(new Insets(20, 150, 10, 10));

		TextField tileNumRows = new TextField("10");
		TextField tileNumCols = new TextField("10");

		newGrid.add(new Label("Rows:"), 0, 0);
		newGrid.add(tileNumRows, 1, 0);
		newGrid.add(new Label("Columns:"), 0, 1);
		newGrid.add(tileNumCols, 1, 1);

		Text defTileText = new Text();
		defTileText.setWrappingWidth(200);
		defTileText.setTextAlignment(TextAlignment.JUSTIFY);
		defTileText.setText("no selection made");
		
		// Disable button until tile selection is made
		newMapDialog.getDialogPane().lookupButton(okButtonType).setDisable(true);

		FileChooser defTileChooser = new FileChooser();
		defTileChooser.setTitle("Choose default tile");
		defTileChooser.setInitialDirectory(new File(tileAssetDir));
		FileChooser.ExtensionFilter imgFilter = new FileChooser.ExtensionFilter("Images (*.png, *.jpg)", "*.png", "*.jpg");
		defTileChooser.getExtensionFilters().add(imgFilter);

		Button defTileChooserBtn = new Button("Open");
		defTileChooserBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				File defTileFile = defTileChooser.showOpenDialog(primaryStage);
				if (defTileFile != null) {
					// Convert absolute image location to relative location
					String relativePath = new File(tileAssetDir).toURI().relativize(defTileFile.toURI()).getPath();

					// Re-enable OK button
					newMapDialog.getDialogPane().lookupButton(okButtonType).setDisable(false);
					defTileText.setText(relativePath);
				}
			}
		});

		newGrid.add(new Label("Choose default tile"), 0, 2);
		newGrid.add(defTileText, 1, 2);
		newGrid.add(defTileChooserBtn, 2, 2);

		newMapDialog.getDialogPane().setContent(newGrid);

		newMapDialog.setResultConverter(dialogButton -> {
			if (dialogButton == okButtonType) {
				// input data
				String[] returnStr = new String[3];
				returnStr[0] = tileNumRows.getText();
				returnStr[1] = tileNumCols.getText();
				returnStr[2] = defTileText.getText();
				return returnStr;
			}
			return null;
		});

		//////////////////////
		// Open - dialog box //
		//////////////////////
		FileChooser openMapChooser = new FileChooser();
		openMapChooser.setTitle("Open Map");
		openMapChooser.setInitialDirectory(new File(workingDir));
		FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
		openMapChooser.getExtensionFilters().add(txtFilter);

		//////////////////////
		// Save - dialog box //
		//////////////////////
		FileChooser saveMapChooser = new FileChooser();
		saveMapChooser.setTitle("Save Map");
		saveMapChooser.setInitialDirectory(new File(workingDir));
		saveMapChooser.getExtensionFilters().add(txtFilter);

		// --------------------------------------------------------------------//

		/*                                 JSON Hierarchy
		 * 
		 *                                  jsonFileObj
		 *           tiles                                          objects
		 *        array of rows                          array of properties for each obj
		 * array of tile names in row              base props                         extra props
		 *                                  fileName, x, y, visLayer, collision    array of extra props
		 */

		///////////////////////
		// New - menu button //
		///////////////////////
		MenuItem newBtn = new MenuItem("New");
		newBtn.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
		newBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				Optional<String[]> newMapResult = newMapDialog.showAndWait();
				if (newMapResult.isPresent()) {
					// Clear grid
					//tileGrid.getChildren().clear();

					// Get inputs from the create map dialog box
					tileRows = Integer.parseInt(newMapResult.get()[0]);
					tileCols = Integer.parseInt(newMapResult.get()[1]);
					defTilePath = newMapResult.get()[2];

					// Reset idGrid
					idGrid = new String[tileCols][tileRows];

					// Fill idGrid with default tile
					for (String[] row : idGrid) {
						Arrays.fill(row, defTilePath);
					}

					fillTileGrid();
				}
			}
		});

		////////////////////////
		// Open - menu button //
		////////////////////////
		MenuItem openBtn = new MenuItem("Open");
		openBtn.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		openBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				File openMapFile = openMapChooser.showOpenDialog(primaryStage);
				if (openMapFile != null) {
					if (null != tileGrid) {
						tileGrid.getChildren().clear();
					}
					
					try {
						Scanner mapFileScanner = new Scanner(openMapFile);
						Object jsonString = JSONValue.parse(mapFileScanner.nextLine());
						JSONObject jsonFileObj = (JSONObject) jsonString;

						//////////////////
						// Import tiles //
						//////////////////

						JSONArray jsonTileArray = (JSONArray) jsonFileObj.get("tiles");
						tileRows = jsonTileArray.size();
						tileCols = ((JSONArray) jsonTileArray.get(0)).size();
						idGrid = new String[tileCols][tileRows];

						for (int i = 0; i < tileRows; i++) {
							JSONArray rowArray = (JSONArray) jsonTileArray.get(i);
							for (int j = 0; j < tileCols; j++) {
								idGrid[j][i] = rowArray.get(j).toString();
							}
						}

						fillTileGrid();

						////////////////////
						// Import objects //
						////////////////////

						// Temporary change to object editing mode
						swapTilePaneChildren();

						JSONArray jsonObjArray = (JSONArray) jsonFileObj.get("objects");

						// Load properties from JSON format
						for (Object propObj : jsonObjArray) {
							JSONObject properties = (JSONObject) propObj;
							JSONObject baseProps = (JSONObject) properties.get("baseProperties");
							JSONArray jsonExtraProps = (JSONArray) properties.get("extraProperties");

							String fileName = (String) baseProps.get("fileName");
							double x = (Double) baseProps.get("x");
							double y = (Double) baseProps.get("y");
							int visLayer = ((Long) baseProps.get("visLayer")).intValue();
							Boolean collision = (Boolean) baseProps.get("collision");

							List<String> extraPropsList = (ArrayList) jsonExtraProps;
							String[] extraProps = new String[extraPropsList.size()];
							extraProps = extraPropsList.toArray(extraProps);

							// Create object
							Image objectImage = null;
							try {
								objectImage = new ImageWithURL("file:" + workToObjectAsset + fileName, false);
								double imgWidth = objectImage.getWidth();
								double imgHeight = objectImage.getHeight();
								tilePane.getChildren().add(new CanvasObject(objectImage, imgWidth, imgHeight, fileName, x, y, zoomPercent, visLayer, collision, extraProps));
							} catch (IllegalArgumentException e) {
								System.out.println("object file not found or file out of map-maker directory");
								System.exit(0);
							}
						}

						// Return back to tile editing mode
						swapTilePaneChildren();

					} catch (FileNotFoundException e) {
						System.out.println("File not found");
						System.exit(0);
					}
				}
			}
		});

		////////////////////////
		// Save - menu button //
		////////////////////////
		MenuItem saveBtn = new MenuItem("Save");
		saveBtn.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
		saveBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				File saveMapFile = saveMapChooser.showSaveDialog(primaryStage);
				if (saveMapFile != null) {
					try {
						PrintWriter saveWriter = new PrintWriter(saveMapFile.toString(), "UTF-8");

						// JSON Object for the entire file
						JSONObject jsonFileObj = new JSONObject();

						// Write tiles in JSON format as a list of lists of tilenames
						List<List<String>> idGridRows = new ArrayList<List<String>>();
						int m = tileRows;
						int n = tileCols;
						for (int i = 0; i < m; i++) {
							List<String> rowList = new ArrayList<String>();
							for (int j = 0; j < n; j++) {
								rowList.add(idGrid[j][i]);
							}
							idGridRows.add(rowList);
						}

						// Put tiles in FileObj
						jsonFileObj.put("tiles", idGridRows);

						// put width and height of map in tile units
						jsonFileObj.put("width", tileCols);
						jsonFileObj.put("height", tileRows);

						// Write objects in JSON format
						JSONArray jsonObjArray = new JSONArray();
						for (CanvasObject canvasObj: CanvasObject.canvasObjects) {
								// JSON Object that holds base and extra props
								JSONObject properties = new JSONObject();

								// First entry: base properties as obj
								JSONObject jsonObj = new JSONObject();
								jsonObj.put("fileName", canvasObj.fileName);
								jsonObj.put("x", canvasObj.x);
								jsonObj.put("y", canvasObj.y);
								jsonObj.put("visLayer", canvasObj.visLayer);
								jsonObj.put("collision", canvasObj.collision);
								properties.put("baseProperties", jsonObj);

								// Second entry: extra properties as array
								JSONArray extraList = new JSONArray();
								// NOTE: Couldn't find a method to do this for me
								if (canvasObj.extraProps != null) {
									for (String prop : canvasObj.extraProps) {
										extraList.add(prop);
									}
								}
								properties.put("extraProperties", extraList);

								// Add properties object to Object Array
								jsonObjArray.add(properties);
						}
						// Put array of all property objects into FileObj
						jsonFileObj.put("objects", jsonObjArray);

						// Write to file
						saveWriter.println(jsonFileObj.toJSONString());
						saveWriter.close();
					} catch (FileNotFoundException e) {
						System.out.println("File not found");
						System.exit(0);
					} catch (UnsupportedEncodingException e) {
						System.out.println("Unsupported encoding");
						System.exit(0);
					}
				}
			}
		});

		// Add buttons to File dropdown
		menuFile.getItems().addAll(newBtn, openBtn, saveBtn);
	}

	private void createEditMenu(Stage primaryStage, Menu menuEdit) {
		ObservableList<Node> layers = tilePane.getChildren();

		MenuItem tileBtn = new MenuItem("Tiles");
		tileBtn.setAccelerator(KeyCombination.keyCombination("Ctrl+1"));
		tileBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				if (layers.size() > 1) {
					if (!layers.get(layers.size() - 1).getId().equals("grid")) {
						// Switch to tiles
						swapTilePaneChildren();
						drawSelectionTiles();
					}
				}
			}
		});

		MenuItem objBtn = new MenuItem("Objects");
		objBtn.setAccelerator(KeyCombination.keyCombination("Ctrl+2"));
		objBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				if (layers.size() > 1) {
					if (layers.get(layers.size() - 1).getId().equals("grid")) {
						// Switch to objects
						swapTilePaneChildren();
						drawSelectionObjects();
					}
				}
			}
		});

		menuEdit.getItems().addAll(tileBtn, objBtn);
	}

	@Override
	public void start(Stage primaryStage) {

		tileScrollPane.setPannable(true);
		zoomPercent = 100;

		/**
		 * use the scroll wheel to zoom in and out
		 */
		tilePane.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				zoomPercent += event.getDeltaY();
				zoomField.setText(String.valueOf(zoomPercent));
				zoom();
			}
		});

		primaryStage.setTitle("MapGUI");
		Scene primaryScene = new Scene(new GridPane(), windowWidth, windowHeight);

		tileScrollPane.setPrefWidth(800);
		tileScrollPane.setPrefHeight(600);

		// ----------------------------------------------------------------------//
		///////////////
		// File Menu //
		///////////////

		Menu menuFile = new Menu("File");
		createFileMenu(primaryStage, menuFile);

		// ----------------------------------------------------------------------//
		///////////////
		// Edit Menu //
		///////////////

		Menu menuEdit = new Menu("Edit");
		createEditMenu(primaryStage, menuEdit);

		// ---------------------------------------------------------------------//
		///////////////////
		// Zoom amt field//
		///////////////////
		zoomField = new TextField("100");
		zoomField.setPromptText("Zoom %");
		zoomField.setMinWidth(80);
		zoomField.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent kEvent) {
				if (kEvent.getCode() == KeyCode.ENTER) {
					zoomPercent = Double.parseDouble(zoomField.getText());
					zoom();
/*					fillTileGrid();
					swapTilePaneChildren();
					for (CanvasObject canvasObject: CanvasObject.canvasObjects) {
						tilePane.getChildren().add(canvasObject);
					}*/
				}
			}
		});

		// ----------------------------------------------------------------------//
		////////////////////////
		// Tile Selection Bar //
		////////////////////////

		selectScrollPane.setContent(selectGridPane);
		drawSelectionTiles();
		selectScrollPane.setMinHeight(tileSizeY + 35);

		// ----------------------------------------------------------------------//
		/////////////////////
		// Tile search bar //
		/////////////////////

		TextField tileSearchBox = new TextField();
		tileSearchBox.setPromptText("Search");
		tileSearchBox.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				searchString = tileSearchBox.getText();
				if (tilePane.getChildren().size() > 1) {
					if (tilePane.getChildren().get(tilePane.getChildren().size() - 1).getId() == "grid") {
						drawSelectionTiles();
					} else {
						drawSelectionObjects();
					}
				}
			}
		});

		// ----------------------------------------------------------------------//
		//////////////////////
		// almost done boiz //
		//////////////////////
		//kek
		
		// Menu Bar
		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuFile, menuEdit);

		// Add elements to scene
		//((GridPane) primaryScene.getRoot()).getChildren().addAll(menuBar, tileScrollPane, selectScrollPane, tileSearchBox, zoomField);
		GridPane gPane = ((GridPane) primaryScene.getRoot());
		gPane.add(menuBar, 0, 0);
		gPane.add(tileScrollPane, 0, 1);
		gPane.add(selectScrollPane, 0, 2);
		gPane.add(tileSearchBox, 0, 3);
		gPane.add(zoomField, 1, 3);



		// Show GUI
		primaryStage.setScene(primaryScene);
		primaryStage.show();
	}

	/**
	 * scale the tile grid and objects on the grid
	 */
	private void zoom() {

		//update canvas size to contain all of the tiles
		updateTileCanvasDimensions();

		//scale(zoom) all of the tiles
		for (int col = 0; col < tileImageViewGrid.length; col++) {
			for (int row = 0; row < tileImageViewGrid[0].length; row++) {
				ImageView tileImageView = tileImageViewGrid[col][row];
				ImageWithURL tileImage = (ImageWithURL) tileImageView.getImage();
				String tileImageURL = tileImage.getURL();
				ImageWithURL zoomedImage = new ImageWithURL(tileImageURL, zoomPercent / 100 * tileSizeX, zoomPercent / 100 * tileSizeY, true, false, false);
				tileImageView.setImage(zoomedImage);
			}
		}


		for (CanvasObject canvasObject: CanvasObject.canvasObjects) {
			canvasObject.setZoomPercent(zoomPercent);
		}
	}

	/**
	 * convert a scaled coordinate to 100% scale
	 * example: if the zoom percentage is 200%, this will return half of what you pass in
	 */
	protected static double fromScaled(double scaledCoord, double zoomPercent) {
		return scaledCoord / (zoomPercent / 100);
	}

	/**
	 * scale a coordinate by a percentage
	 * @param toScale what you want to scale
	 * @param zoomPercent the percentage by which to scale the coordinate value
	 */
	protected static double toScaled(double toScale, double zoomPercent) {
		return toScale * (zoomPercent / 100);
	}
}