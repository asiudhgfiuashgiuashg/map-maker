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
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Scanner;
import java.util.NoSuchElementException;

public class MapGUI extends Application {
  public static void main(String[] args) {
      launch(args);
  }

	private	int tileSizeX;
	private int tileSizeY;
	private int tileCols;
	private int tileRows;
	private int windowWidth = 800;
	private int windowHeight = 600;
	private int propWidth = 400;
	private int propHeight = 250;
	private int defVisLayer = 3;
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
	private String searchString = "";
	private Image selectedTileImage;
	private Image selectedObjectImage;
	private Image testImage;
	private List<String> tileNames = new ArrayList<String>();
	private int[][] idGrid;
	private Canvas tileCanvas;
	private GraphicsContext gc;
	
	// Panes
	final ScrollPane tileScrollPane = new ScrollPane();
	final Pane tilePane = new Pane();
	final GridPane tileGrid = new GridPane();
		
	// Add horizontal slider for tiles
	final ScrollPane selectScrollPane = new ScrollPane();
	final GridPane selectGridPane = new GridPane();
	
    private int convertNameToID(String tileName) {
    	int numTileNames = tileNames.size();
    	for (int i=0; i<numTileNames; i++) {
    		if (tileNames.get(i).equals(tileName)) {
    			return i;
    		}
    	}
    	tileNames.add(tileName);
    	return numTileNames;
    }
    
    private String convertIDintToStr(int id) {
    	return String.format("%03d", id);
    }
	
	private void initCanvas() {
		tilePane.getChildren().clear();
		
		// Create new canvas
		tileCanvas = new Canvas(tileSizeX*tileCols, tileSizeY*tileRows);
		gc = tileCanvas.getGraphicsContext2D();
		tilePane.getChildren().add(tileCanvas);
		tilePane.getChildren().get(0).setId("canvas");
		
		// Set on mouse click for master canvas
		tileCanvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.PRIMARY) {
					if (selectedObjectImage != null) {
						Canvas imgCanvas = new Canvas(selectedObjectImage.getWidth(), selectedObjectImage.getHeight());
						imgCanvas.getGraphicsContext2D().drawImage(selectedObjectImage, 0, 0);
						
						// Set id of each object (canvas) to its file name and coordinates
						imgCanvas.setId(relativeSelectedObject + "," + event.getX() + "," + event.getY() + "," + defVisLayer);
						
						imgCanvas.setTranslateX(event.getX() - selectedObjectImage.getWidth() / 2);
						imgCanvas.setTranslateY(event.getY() - selectedObjectImage.getWidth() / 2);
						
						tilePane.getChildren().add(imgCanvas);
						
						// On drag, move object
						imgCanvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
							@Override
							public void handle(MouseEvent event) {
								if (event.getButton() == MouseButton.PRIMARY) {
									// Update position
									double eventXInTilePane = event.getX() + imgCanvas.getTranslateX();
									double eventYInTilePane = event.getY() + imgCanvas.getTranslateY();
									imgCanvas.setTranslateX(eventXInTilePane - selectedObjectImage.getWidth() / 2);
									imgCanvas.setTranslateY(eventYInTilePane - selectedObjectImage.getHeight() / 2);
									
									// Update position in object's id
									String[] idString = imgCanvas.getId().split(",");
									idString[1] = Double.toString(eventXInTilePane);
									idString[2] = Double.toString(eventYInTilePane);
									imgCanvas.setId(String.join(",", idString));
								}							
							}
						});
						
						// On right click delete image, on double click edit properties
						imgCanvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
							@Override
							public void handle(MouseEvent event) {
								if (event.getButton() == MouseButton.SECONDARY) {
									tilePane.getChildren().remove(imgCanvas);
								}
								if (event.getButton() == MouseButton.PRIMARY) {
									if (event.getClickCount() == 2) {
										Stage propStage = new Stage();
										propStage.setTitle("Edit Properties - " + imgCanvas.getId().split(",")[0]);
										
										Scene propScene = new Scene(new VBox(), propWidth, propHeight);
										propStage.setScene(propScene);
										
										Label layerLabel = new Label("Visibility Layer:");
										
										// Get current visibility layer
										String curVisLayer = imgCanvas.getId().split(",")[3];
										TextField layerTextField = new TextField(curVisLayer);
										
										// Make sure character is numeric
										layerTextField.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
								            @Override
								            public void handle(KeyEvent e) {
								                if (layerTextField.getText().length() >= 1 || !Character.isDigit(e.getCharacter().charAt(0))) {
								                    e.consume();
								                }
								            }
								        });
										
										Button propCloseBtn = new Button("OK");
										propCloseBtn.setOnAction(new EventHandler<ActionEvent>() {
								    		@Override
								    		public void handle(ActionEvent e) {
												String[] idString = imgCanvas.getId().split(",");
												idString[3] = layerTextField.getText();
												imgCanvas.setId(String.join(",", idString));

												System.out.println(imgCanvas.getId());
								    			propStage.close();
								    		}
								    	});
										
										GridPane propGridPane = new GridPane();
										propGridPane.add(layerLabel, 0, 0);
										propGridPane.add(layerTextField, 1, 0);
										propGridPane.add(propCloseBtn, 0, 1);
										
										((VBox) propScene.getRoot()).getChildren().addAll(propGridPane);
										propStage.showAndWait();
									}
								}
							}
						});
					}
				}
			}
		});  
		
		// Add grid
    	tileScrollPane.setContent(tilePane);
    	tilePane.getChildren().add(tileGrid);
    	tilePane.getChildren().get(1).setId("grid");
		
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
				selectGridPane.add(selectImageView, childCount-1, 0);
				
				Text selectImageName = new Text(relativePath);
				selectGridPane.add(selectImageName, childCount-1, 1);
				
				// 	On mouse click of asset tiles
				selectImageView.setId(Integer.toString(childCount-1));
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
						selectGridPane.add(selectImageView, childCount-1, 0);
						
						Text selectImageName = new Text(relativePath);
						selectGridPane.add(selectImageName, childCount-1, 1);
						
						// 	On mouse click of asset tiles
						selectImageView.setId(Integer.toString(childCount-1));
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
    	
    	
    	//------------------------//
    	
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
    				
    				// Create canvas of grid size
    				initCanvas();
    				drawSelectionTiles();
    				
    				// Get image and fill grid with default tile
    				Image defTileImage = null;
    				try {
    					defTileImage = new Image("file:" + workToTileAsset + defTilePath);
    				} catch (IllegalArgumentException e) {
    					System.out.println("Default tile file not found or file out of map-maker directory");
    					System.exit(0);
    				}
    				tileNames.clear();
    				tileNames.add(defTilePath);
    				idGrid = new int[tileCols][tileRows];
    				for(int i=0; i<tileRows; i++){
    					for(int j=0; j<tileCols; j++){
    						idGrid[j][i] = convertNameToID(defTilePath);
    						ImageView tileImageView = new ImageView();
    						tileImageView.setImage(defTileImage);
    						tileGrid.add(tileImageView, j, i);
    						
    						// On mouse click of grid tiles
    						tileImageView.setId(Integer.toString(i) + ":" + Integer.toString(j));
    						tileImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
    							@Override
    							public void handle(MouseEvent event) {
    								if (event.getButton() == MouseButton.PRIMARY) {
    									String idNumbers [] = tileImageView.getId().split(":");
    									int x = Integer.parseInt(idNumbers[0]);
    									int y = Integer.parseInt(idNumbers[1]);
    									
    									if (selectedTileImage != null) {
    										idGrid[y][x] = convertNameToID(relativeSelectedTile);
    										tileImageView.setImage(selectedTileImage);
    									}
    								}
    							}
    						});  
    						
    					}
    				}
    			}
    		}
    	});
    	
    	////////////////////////
    	// Open - menu button //
    	////////////////////////
    	MenuItem openBtn = new MenuItem("Open");
    	openBtn.setOnAction(new EventHandler<ActionEvent>() {
    		public void handle(ActionEvent t) {
    			// load in tiles
    			File openMapFile = openMapChooser.showOpenDialog(primaryStage);
    			if (openMapFile != null) {
    				tileNames.clear();
    				tileGrid.getChildren().clear();
    				try {
    					Scanner mapFileScanner = new Scanner(openMapFile);
    					tileRows = Integer.parseInt(mapFileScanner.nextLine());
    					tileCols = Integer.parseInt(mapFileScanner.nextLine());
    					
        				// Create canvas of grid size
        				initCanvas();
        				drawSelectionTiles();
    					
    					idGrid = new int[tileCols][tileRows];
    					for (int i=0; i<tileRows; i++) {
    						String readRow = mapFileScanner.nextLine();
    						String splitIDs [] = readRow.split(" ");
    						
    						// First extract the idGrid data
    						for (int j=0; j<tileCols; j++) {
    							idGrid[j][i] = Integer.parseInt(splitIDs[j]);
    						}
    					}	
    					// Now extract the tileNames data
    					String kRow = mapFileScanner.nextLine();
    					if (kRow != null) {
    						while (kRow != null){
    							String kSplit [] = kRow.split(":");
    							tileNames.add(kSplit[0]);
    							try {
    								kRow = mapFileScanner.nextLine();
    							} catch (NoSuchElementException e) {
    								kRow = null;
    							}
    						}
    					}
    					// Now fill in grid
        				for(int i=0; i<tileRows; i++){
        					for(int j=0; j<tileCols; j++){
        						
        						Image inpTileImage = null;
        						try {
        							inpTileImage = new Image("file:" + workToTileAsset + tileNames.get(idGrid[j][i]));
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
        								String idNumbers [] = tileImageView.getId().split(":");
        								int x = Integer.parseInt(idNumbers[0]);
        								int y = Integer.parseInt(idNumbers[1]);
        								
        								if (selectedTileImage != null) {
        									idGrid[y][x] = convertNameToID(relativeSelectedTile);
        									tileImageView.setImage(selectedTileImage);
        								}
        							}
        						}); 
        					}
        				}
        				
    				} catch (FileNotFoundException e) {
    					System.out.println("File not found");
    					System.exit(0);
    				}
    			}
    	}});
    	
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
    					saveWriter.println(tileRows);
    					saveWriter.println(tileCols);
    					for (int i=0; i<tileRows; i++) {
    						String rowStr = "";
    						for (int j=0; j<tileCols; j++) {
    							rowStr = rowStr + convertIDintToStr(idGrid[j][i]) + " ";
    						}
    						saveWriter.println(rowStr);
    					}
    					int numTileNames = tileNames.size();
    					for (int i=0; i<numTileNames; i++) {
    						String keyStr = tileNames.get(i) + ":" + convertIDintToStr(i);
    						saveWriter.println(keyStr);
    					}
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
    	MenuItem tileBtn = new MenuItem("Tiles");
    	tileBtn.setOnAction(new EventHandler<ActionEvent>() {
    		public void handle(ActionEvent t) {
    			ObservableList<Node> children = FXCollections.observableArrayList(tilePane.getChildren());
    			if (children.size() > 1) {
    				if (!children.get(children.size()-1).getId().equals("grid")) {
    					// Switch to tiles
    					Collections.swap(children, 0, children.size()-1);
    					tilePane.getChildren().setAll(children);
    					drawSelectionTiles();
    				}
    			}
    		}
    	});
    	
    	MenuItem objBtn = new MenuItem("Objects");
    	objBtn.setOnAction(new EventHandler<ActionEvent>() {
    		public void handle(ActionEvent t) {
    			ObservableList<Node> children = FXCollections.observableArrayList(tilePane.getChildren());
    			if (children.size() > 1) {
    				if (children.get(children.size()-1).getId().equals("grid")) {
    					// Switch to objects
    					Collections.swap(children, 0, children.size()-1);
    					tilePane.getChildren().setAll(children);
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
    	
    	//----------------------------------------------------------------------//
    	///////////////
    	// File Menu //
    	///////////////
    	
    	Menu menuFile = new Menu("File");
    	createFileMenu(primaryStage, menuFile);
    	
    	//----------------------------------------------------------------------//
    	///////////////
    	// Edit Menu //
    	///////////////
    	
    	Menu menuEdit = new Menu("Edit");
    	createEditMenu(primaryStage, menuEdit);
    	
    	//----------------------------------------------------------------------//
    	////////////////////////
    	// Tile Selection Bar //
    	////////////////////////
  	
    	selectScrollPane.setContent(selectGridPane);
    	drawSelectionTiles();
    	selectScrollPane.setMinHeight(tileSizeY + 35);
    	
    	//----------------------------------------------------------------------//
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
						if (tilePane.getChildren().get(tilePane.getChildren().size()-1).getId() == "grid") {
							drawSelectionTiles();
						} else {
							drawSelectionObjects();
						}
					}
				}
			});  
		    	
    	//----------------------------------------------------------------------//
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