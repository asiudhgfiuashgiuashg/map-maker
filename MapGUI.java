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
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;


public class MapGUI extends Application {
    public static void main(String[] args) {
        launch(args);
    }

	private	int tileSizeX;
	private int tileSizeY;
	private int tileCols;
	private int tileRows;
	private String defTilePath;
	private String workingDir = System.getProperty("user.dir");
	private String workToAsset = "../tile-game/core/assets/tileart/";
	private String assetDir = workingDir + "/" + workToAsset;
	private String selectedTile;
	private String relativeSelectedTile;
	private Image selectedTileImage;
	private List<String> tileNames = new ArrayList<String>();
	private int[][] idGrid;
	
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
    	Scene scene = new Scene(new VBox(), 400, 250);
    	
    	// Scroll pane and grid pane for tiles
    	final ScrollPane sp = new ScrollPane();
    	final GridPane gp = new GridPane();
    	sp.setContent(gp);
    	
    	//----------------------------------------------------------------------//
    	
    	//////////////////////
    	// New - dialog box //
    	//////////////////////
    	Dialog<String[]> newMapDialog = new Dialog<>();
    	newMapDialog.setTitle("Create New Map");
    	newMapDialog.setHeaderText(null);
    	
    	ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    	newMapDialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
    	
    	GridPane grid = new GridPane();
    	grid.setHgap(10);
    	grid.setVgap(10);
    	grid.setPadding(new Insets(20, 150, 10, 10));

    	TextField tileNumRows = new TextField("10");
    	TextField tileNumCols = new TextField("10");

    	grid.add(new Label("Rows:"), 0, 0);
    	grid.add(tileNumRows, 1, 0);
    	grid.add(new Label("Columns:"), 0, 1);
    	grid.add(tileNumCols, 1, 1);
    	
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
    	    	    	
    	grid.add(new Label("Choose default tile"), 0, 2);
    	grid.add(defTileText, 1, 2);
    	grid.add(defTileChooserBtn, 2, 2);
    	
    	newMapDialog.getDialogPane().setContent(grid);
    	
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
    	
    	
    	//----------------------------------------------------------------------//
    	
    	// File Menu    	
    	Menu menuFile = new Menu("File");
    	
    	///////////////////////
    	// New - menu button //
    	///////////////////////
    	MenuItem newBtn = new MenuItem("New");
    	newBtn.setOnAction(new EventHandler<ActionEvent>() {
    		public void handle(ActionEvent t) {
    			Optional<String[]> newMapResult = newMapDialog.showAndWait();
    			if (newMapResult.isPresent()) {
    				// Clear grid
    				gp.getChildren().clear();
    				
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
    						ImageView tempImageView = new ImageView();
    						tempImageView.setImage(defTileImage);
    						gp.add(tempImageView, j, i);
    						
    						// On mouse click of grid tiles
    						tempImageView.setId(Integer.toString(i) + ":" + Integer.toString(j));
    						tempImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
    							@Override
    							public void handle(MouseEvent event) {
    								String idNumbers [] = tempImageView.getId().split(":");
    								int x = Integer.parseInt(idNumbers[0]);
    								int y = Integer.parseInt(idNumbers[1]);
    								
    								System.out.println(relativeSelectedTile);
    								idGrid[y][x] = convertNameToID(relativeSelectedTile);
    								tempImageView.setImage(selectedTileImage);
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
    			/*Scanner mapFileScanner = new Scanner(openMapFile);
    			mapTitle = mapFileScanner.nextLine();
    	        inputRows = Integer.parseInt(mapFileScanner.nextLine());
    	        inputCols = Integer.parseInt(mapFileScanner.nextLine());
    	        */
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
    						String keyStr = tileNames.get(i) + " " + convertIDintToStr(i);
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
    	
    	//----------------------------------------------------------------------//
    	
    	// Add buttons to File dropdown
    	menuFile.getItems().addAll(newBtn, openBtn, saveBtn);

    	// Menu Bar    	
    	MenuBar menuBar = new MenuBar();
    	menuBar.getMenus().add(menuFile);

    	//----------------------------------------------------------------------//
    	
    	// Add horizontal slider for tiles
    	final ScrollPane hsp = new ScrollPane();
    	final GridPane hgp = new GridPane();
    	hsp.setContent(hgp);
    	
    	// Count tile art in directory
    	File tileArtDirFile = new File(assetDir);
    	//int numTileArt = tileArtDirFile.list().length;
    	File[] tileArtDirList = tileArtDirFile.listFiles();
    	int childCount = 0;
    	for (File child : tileArtDirList) {
   		
    		childCount += 1;
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
			
			ImageView hImageView = new ImageView();
			hImageView.setImage(tileArtImage);
			hgp.add(hImageView, childCount-1, 0);
			
			// On mouse click of asset tiles
			hImageView.setId(Integer.toString(childCount-1));
			hImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					int id = Integer.parseInt(hImageView.getId());
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
    	
    	hsp.setMinHeight(tileSizeY + 20);
    	
    	// Add elements to scene
    	((VBox) scene.getRoot()).getChildren().addAll(menuBar, sp, hsp);
    	    	
    	// Show GUI
    	primaryStage.setScene(scene);
    	primaryStage.show();
    }
}