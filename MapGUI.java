import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

	private int windowWidth = 800;
	private int windowHeight = 600;
	private int propWidth = 400;
	private int propHeight = 250;

	private int tileVisLayer = 1;
	private int playerVisLayer = 5;
	private int defVisLayer = 9;
	private Boolean defCollision = true;

	private String defTilePath;
	private String workingDir = System.getProperty("user.dir");
	private String workToTileAsset = "../tile-game/core/assets/tileart/";
	private String workToObjectAsset = "../tile-game/core/assets/objectart/";
	private String tileAssetDir = workingDir + "/" + workToTileAsset;
	private String objectAssetDir = workingDir + "/" + workToObjectAsset;

	private String selectedTile;
	private String relativeSelectedTile;
	private String selectedObject;
	private String relativeSelectedObject;
	private Image selectedTileImage;
	private Image selectedObjectImage;

	private String searchString = "";

	private String[][] idGrid;
	private Canvas tileCanvas;
	private GraphicsContext gc;

	// Panes
	final ScrollPane tileScrollPane = new ScrollPane();
	final Pane tilePane = new Pane();
	final GridPane tileGrid = new GridPane();

	// Add horizontal slider for tiles
	final ScrollPane selectScrollPane = new ScrollPane();
	final GridPane selectGridPane = new GridPane();

	private void initCanvas() {
		tilePane.getChildren().clear();

		// Create new canvas
		tileCanvas = new Canvas(tileSizeX * tileCols, tileSizeY * tileRows);
		gc = tileCanvas.getGraphicsContext2D();
		tilePane.getChildren().add(tileCanvas);
		tilePane.getChildren().get(0).setId("canvas");

		// On mouse click of master canvas, create new object
		tileCanvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.PRIMARY) {
					if (selectedObjectImage != null) {
						String objString = relativeSelectedObject + "," + event.getX() + "," + event.getY() + "," + defVisLayer + "," + defCollision + ",";
						createObject(objString);
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

	private void createObject(String objPropString) {

		String[] inpObjProps = objPropString.split(",");
		String inpObject = inpObjProps[0];
		Image inpObjectImage = null;
		try {
			inpObjectImage = new Image("file:" + workToObjectAsset + inpObject);
		} catch (IllegalArgumentException e) {
			System.out.println("object file not found or file out of map-maker directory");
			System.exit(0);
		}
		double x = Double.parseDouble(inpObjProps[1]);
		double y = Double.parseDouble(inpObjProps[2]);

		double imgWidth = inpObjectImage.getWidth();
		double imgHeight = inpObjectImage.getHeight();

		Canvas imgCanvas = new Canvas(imgWidth, imgHeight);
		imgCanvas.getGraphicsContext2D().drawImage(inpObjectImage, 0, 0);

		// Set id of each object (canvas) to its file name and coordinates
		imgCanvas.setId(objPropString);

		imgCanvas.setTranslateX(x - imgWidth / 2);
		imgCanvas.setTranslateY(y - imgHeight / 2);

		tilePane.getChildren().add(imgCanvas);

		// On drag, move object
		imgCanvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.PRIMARY) {
					// Update position
					double eventXInTilePane = event.getX() + imgCanvas.getTranslateX();
					double eventYInTilePane = event.getY() + imgCanvas.getTranslateY();
					imgCanvas.setTranslateX(eventXInTilePane - imgWidth / 2);
					imgCanvas.setTranslateY(eventYInTilePane - imgHeight / 2);

					// Update position in object's id
					String[] objProps = imgCanvas.getId().split(",");
					objProps[1] = Double.toString(eventXInTilePane);
					objProps[2] = Double.toString(eventYInTilePane);
					imgCanvas.setId(String.join(",", objProps));
				}
			}
		});
		
		imgCanvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				String[] objProps = imgCanvas.getId().split(",");
				
				// On right click delete image
				if (event.getButton() == MouseButton.SECONDARY) {
					tilePane.getChildren().remove(imgCanvas);
				}
				
				// On double click edit properties
				if (event.getButton() == MouseButton.PRIMARY) {
					if (event.getClickCount() == 2) {
						Dialog<String> editDialog = new Dialog<>();
						editDialog.setTitle("Edit Object Properties");
						editDialog.setHeaderText("Visibility Layers:\nInvisible = 0, Tiles = " + tileVisLayer + ", Player = " + playerVisLayer + ", Default = " + defVisLayer + "\n(Additional properties delimited by comma)");

						ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
						editDialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

						Label layerLabel = new Label("Visibility Layer:");
						Label collLabel = new Label("Collisions:");
						Label additLabel = new Label("Additional Properties:");

						// Get current visibility layer
						String curVisLayer = objProps[3];
						TextField layerTextField = new TextField(curVisLayer);

						// Make sure character is numeric
						layerTextField.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
							@Override
							public void handle(KeyEvent e) {
								if (!Character.isDigit(e.getCharacter().charAt(0))) {
									e.consume();
								}
							}
						});

						// Make sure layer is not player, tile layer, or null
						layerTextField.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
							@Override
							public void handle(KeyEvent e) {
								Node okButton = editDialog.getDialogPane().lookupButton(okButtonType);
								if (layerTextField.getText().equals(Integer.toString(playerVisLayer)) || layerTextField.getText().equals(Integer.toString(tileVisLayer)) || layerTextField.getText().equals("")) {
									okButton.setDisable(true);
								} else {
									okButton.setDisable(false);
								}
							}
						});

						// Collision choice box
						ChoiceBox<String> collChoiceBox = new ChoiceBox<String>();
						collChoiceBox.getItems().addAll("true", "false");
						if (objProps[4].equals("true")) {
							collChoiceBox.getSelectionModel().selectFirst();
						} else {
							collChoiceBox.getSelectionModel().selectLast();
						}

						// Addition properties textbox
						TextField additTextField = new TextField();
						if (objProps.length > 5) {
							// Get additional properties and list them in textbox
							String additPropString = "";
							for (int i=5; i<objProps.length; i++) {
								additPropString += (objProps[i] + ",");
							}
							additTextField.setText(additPropString);
						}
						
						// Grid pane for dialog box
						GridPane propGridPane = new GridPane();
						propGridPane.setHgap(10);
						propGridPane.setVgap(10);
						propGridPane.add(layerLabel, 0, 0);
						propGridPane.add(layerTextField, 1, 0);
						propGridPane.add(collLabel, 0, 1);
						propGridPane.add(collChoiceBox, 1, 1);
						propGridPane.add(additLabel, 0, 2);
						propGridPane.add(additTextField, 1, 2);

						editDialog.setResultConverter(dialogButton -> {
							if (dialogButton == okButtonType) {
								objProps[3] = layerTextField.getText();
								objProps[4] = collChoiceBox.getSelectionModel().getSelectedItem();
								
								// Construct base properties for id
								String basePropString = "";
								for (int i=0; i<5; i++) {
									basePropString += (objProps[i] + ",");
								}
								
								// Now add additional properties to the id
								String additPropString = additTextField.getText();
								String propString = basePropString + additPropString;
																
								// Make sure the text field isn't player, tile layer, or null
								if (!(layerTextField.getText().equals(Integer.toString(playerVisLayer)) || layerTextField.getText().equals(Integer.toString(tileVisLayer)) || layerTextField.getText().equals(""))) {
									imgCanvas.setId(propString);
								}
							}
							return null;
						});

						editDialog.getDialogPane().setContent(propGridPane);
						editDialog.showAndWait();
					}
				}
			}
		});
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
					tileArtImage = new Image("file:" + workToTileAsset + relativePath);
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
								selectedTileImage = new Image("file:" + workToTileAsset + relativeSelectedTile);
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
					objectArtImage = new Image("file:" + workToObjectAsset + relativePath);
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
								selectedObjectImage = new Image("file:" + workToObjectAsset + relativeSelectedObject);
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
		for (int i = 0; i < tileRows; i++) {
			for (int j = 0; j < tileCols; j++) {

				Image inpTileImage = null;
				try {
					inpTileImage = new Image("file:" + workToTileAsset + idGrid[j][i]);
				} catch (IllegalArgumentException e) {
					System.out.println("tile file not found or file out of map-maker directory");
					System.exit(0);
				}

				ImageView tileImageView = new ImageView();
				tileImageView.setImage(inpTileImage);
				tileGrid.add(tileImageView, j, i);

				// On mouse click of grid tiles
				tileImageView.setId(Integer.toString(i) + ":" + Integer.toString(j));
				tileImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent event) {
						String idNumbers[] = tileImageView.getId().split(":");
						int x = Integer.parseInt(idNumbers[0]);
						int y = Integer.parseInt(idNumbers[1]);

						if (selectedTileImage != null) {
							idGrid[y][x] = relativeSelectedTile;
							tileImageView.setImage(selectedTileImage);
						}
					}
				});
			}
		}
	}

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
		FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		openMapChooser.getExtensionFilters().add(txtFilter);

		//////////////////////
		// Save - dialog box //
		//////////////////////
		FileChooser saveMapChooser = new FileChooser();
		saveMapChooser.setTitle("Save Map");
		saveMapChooser.setInitialDirectory(new File(workingDir));
		saveMapChooser.getExtensionFilters().add(txtFilter);

		// ------------------------//

		///////////////////////
		// New - menu button //
		///////////////////////
		MenuItem newBtn = new MenuItem("New");
		newBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				Optional<String[]> newMapResult = newMapDialog.showAndWait();
				if (newMapResult.isPresent()) {
					// Clear grid
					tileGrid.getChildren().clear();

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
		openBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				File openMapFile = openMapChooser.showOpenDialog(primaryStage);
				if (openMapFile != null) {
					tileGrid.getChildren().clear();
					try {
						Scanner mapFileScanner = new Scanner(openMapFile);

						//////////////////
						// Import tiles //
						//////////////////

						Object jsonTileLine = JSONValue.parse(mapFileScanner.nextLine());
						JSONArray jsonTileArray = (JSONArray) jsonTileLine;
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

						Object jsonObjLine = JSONValue.parse(mapFileScanner.nextLine());
						JSONArray jsonObjArray = (JSONArray) jsonObjLine;
						for (int i = 0; i < jsonObjArray.size(); i++) {
							createObject((String) jsonObjArray.get(i));
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
		saveBtn.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				File saveMapFile = saveMapChooser.showSaveDialog(primaryStage);
				if (saveMapFile != null) {
					try {
						PrintWriter saveWriter = new PrintWriter(saveMapFile.toString(), "UTF-8");

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
						String jsonGrid = JSONValue.toJSONString(idGridRows);
						saveWriter.println(jsonGrid);

						// Write objects in JSON format as a list of object id's
						List<String> objList = new ArrayList<String>();
						for (Node obj : tilePane.getChildren()) {
							if (!obj.getId().equals("canvas") && !obj.getId().equals("grid")) {
								objList.add(obj.getId());
							}
						}
						String jsonObjects = JSONValue.toJSONString(objList);
						saveWriter.println(jsonObjects);

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
		primaryStage.setTitle("MapGUI");
		Scene primaryScene = new Scene(new VBox(), windowWidth, windowHeight);

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

		// Menu Bar
		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuFile, menuEdit);

		// Add elements to scene
		((VBox) primaryScene.getRoot()).getChildren().addAll(menuBar, tileScrollPane, selectScrollPane, tileSearchBox);

		// Show GUI
		primaryStage.setScene(primaryScene);
		primaryStage.show();
	}
}