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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.ArrayList;
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
	private int windowWidth = 400;
	private int windowHeight = 250;
	private String defTilePath;
	private String workingDir = System.getProperty("user.dir");
	private String workToAsset = "../tile-game/core/assets/tileart/";
	private String assetDir = workingDir + "/" + workToAsset;
	private String selectedTile;
	private String relativeSelectedTile;
	private String searchString = "";
	private Image selectedTileImage;
	private List<String> tileNames = new ArrayList<String>();
	private int[][] idGrid;
	
	// Scroll pane and grid pane for tiles
	final ScrollPane tileScrollPane = new ScrollPane();
	final GridPane tileGrid = new GridPane();
		
	// Add horizontal slider for tiles
	final ScrollPane selectScrollPane = new ScrollPane();
	final GridPane selectGridPane = new GridPane();
	
    public int convertNameToID(String tileName) {
    	int numTileNames = tileNames.size();
    	for (int i=0; i<numTileNames; i++) {
    		if (tileNames.get(i).equals(tileName)) {
    			return i;
    		}
    	}
    	tileNames.add(tileName);
    	return numTileNames;
    }
    
    public String convertIDintToStr(int id) {
    	return String.format("%03d", id);
    }

    @Override
    public void start(Stage primaryStage) {
    	primaryStage.setTitle("MapGUI");
    	Scene primaryScene = new Scene(new VBox(), windowWidth, windowHeight);
    	primaryScene.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				//System.out.println(event.getSceneX());
			}
		});  
    	
    	tileScrollPane.setContent(tileGrid);
    	
    	//----------------------------------------------------------------------//
    	///////////////
    	// File Menu //
    	///////////////
    	
    	Menu menuFile = new Menu("File");
    	
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
    	defTileChooser.setInitialDirectory(new File(assetDir));
    	FileChooser.ExtensionFilter imgFilter = new FileChooser.ExtensionFilter("Images (*.png, *.jpg)", "*.png", "*.jpg");
    	defTileChooser.getExtensionFilters().add(imgFilter);
    	
    	Button defTileChooserBtn = new Button("Open");
    	defTileChooserBtn.setOnAction(new EventHandler<ActionEvent>() {
    		@Override
    		public void handle(ActionEvent e) {
    			File defTileFile = defTileChooser.showOpenDialog(primaryStage);
    			if (defTileFile != null) {
    				// Convert absolute image location to relative location
    				String relativePath = new File(assetDir).toURI().relativize(defTileFile.toURI()).getPath();
    				
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
    	    	// Return input data
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
    				 
    				// Get image and fill grid with default tile
    				Image defTileImage = null;
    				try {
    					defTileImage = new Image("file:" + workToAsset + defTilePath);
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
    								String idNumbers [] = tileImageView.getId().split(":");
    								int x = Integer.parseInt(idNumbers[0]);
    								int y = Integer.parseInt(idNumbers[1]);
    								
    								idGrid[y][x] = convertNameToID(relativeSelectedTile);
    								tileImageView.setImage(selectedTileImage);
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
        							inpTileImage = new Image("file:" + workToAsset + tileNames.get(idGrid[j][i]));
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
        								
        								idGrid[y][x] = convertNameToID(relativeSelectedTile);
        								tileImageView.setImage(selectedTileImage);
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
    	
    	//----------------------------------------------------------------------//
    	////////////////////////
    	// Tile Selection Bar //
    	////////////////////////
  	
    	selectScrollPane.setContent(selectGridPane);
    	drawSelectionTiles();
    	selectScrollPane.setMinHeight(tileSizeY + 20);
    	
    	//----------------------------------------------------------------------//
    	/////////////////////
    	// Tile search bar //
    	/////////////////////
    	
    	TextField tileSearchBox = new TextField();
    	tileSearchBox.setOnKeyReleased(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					searchString = tileSearchBox.getText();
					drawSelectionTiles();
				}
			});  
    	
    	//----------------------------------------------------------------------//
    	//////////////////////
    	// almost done boiz //
    	//////////////////////
    	
    	// Menu Bar    	
    	MenuBar menuBar = new MenuBar();
    	menuBar.getMenus().addAll(menuFile);
    	
    	// Add elements to scene
    	((VBox) primaryScene.getRoot()).getChildren().addAll(menuBar, tileScrollPane, selectScrollPane, tileSearchBox);
    	
    	// Show GUI
    	primaryStage.setScene(primaryScene);
    	primaryStage.show();
    }

    public void drawSelectionTiles() {
    	
		// Count tile art in directory
		File tileArtDirFile = new File(assetDir);
		File[] tileArtDirList = tileArtDirFile.listFiles();
		selectGridPane.getChildren().clear();
		int childCount = 0;
		for (File child : tileArtDirList) {			
			childCount += 1;
			if (child.toString().contains(searchString)) {
				
				String tileArtPath = child.toString();
				String relativePath = new File(assetDir).toURI().relativize(child.toURI()).getPath();
				
				Image tileArtImage = null;
				try {
					tileArtImage = new Image("file:" + workToAsset + relativePath);
				} catch (IllegalArgumentException e) {
					System.out.println("Tile file not found or something...");
					System.exit(0);
				}
				
				if (childCount == 1) {
					tileSizeX = (int) tileArtImage.getWidth();
					tileSizeY = (int) tileArtImage.getHeight();
				}
				
				ImageView selectImageView = new ImageView();
				selectImageView.setImage(tileArtImage);
				selectGridPane.add(selectImageView, childCount-1, 0);
				
			// 	On mouse click of asset tiles
				selectImageView.setId(Integer.toString(childCount-1));
				selectImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent event) {
						int id = Integer.parseInt(selectImageView.getId());
						selectedTile = tileArtDirList[id].toString();
						relativeSelectedTile = new File(assetDir).toURI().relativize(tileArtDirList[id].toURI()).getPath();
						try {
							selectedTileImage = new Image("file:" + workToAsset + relativeSelectedTile);
						} catch (IllegalArgumentException e) {
							System.out.println("Tile file not found or something...");
							System.exit(0);
						}
					}
				});  
			}
		}
	}
}