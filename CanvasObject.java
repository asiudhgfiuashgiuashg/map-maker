import javafx.event.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.*;
import java.util.*;



public class CanvasObject extends Canvas {
		protected static final List<CanvasObject> canvasObjects = new ArrayList<>();

		// Unique Id
		private static int uidIncrementer = 0;

		// Canvas dimensions
		protected Image objectImage; //image after scaling with zoom
		protected double imgWidth; //image height after scaling with zoom
		protected double imgHeight; //image width after scaling with zoom

		// Object properties
		public String fileName;
		public double x; //the "real" position of the object aka its position at 100% zoom scaling
		public double y;
		public int visLayer;
		public Boolean collision;
		public String[] extraProps;

		// Edit properties window dimensions
		private final int propWidth = 400;
		private final int propHeight = 250;

		// Default property values
		private final int tileVisLayer = 1;
		private final int playerVisLayer = 5;
		private final int defVisLayer = 9;
		private final Boolean defCollision = true;

		private double zoomPercent; // the percentage by which the map is zoomed in the map maker.
									// we need this value to know how big to make the object's image

		public CanvasObject(Image objectImage, double imgWidth, double imgHeight, String fileName, double x, double y, double zoomPercent) {
			this.objectImage = objectImage;
			this.imgWidth = imgWidth;
			this.imgHeight = imgHeight;
			this.fileName = fileName;
			this.x = x;
			this.y = y;
			this.visLayer = defVisLayer;
			this.collision = defCollision;
			this.extraProps = null;
			CreateObject();
			setZoomPercent(zoomPercent);
		}

		public CanvasObject(Image objectImage, double imgWidth, double imgHeight, String fileName, double x, double y, double zoomPercent, int visLayer, Boolean collision, String[] extraProps) {
			this.objectImage = objectImage;
			this.imgWidth = imgWidth;
			this.imgHeight = imgHeight;
			this.fileName = fileName;
			this.x = x;
			this.y = y;
			this.visLayer = visLayer;
			this.collision = collision;
			this.extraProps = extraProps;
			CreateObject();
			setZoomPercent(zoomPercent);
		}

		public void CreateObject() {
			CanvasObject.canvasObjects.add(this);

			// Set Unique Id
			this.setId("object" + uidIncrementer);
			uidIncrementer++;


			// On drag, move object
			this.setOnMouseDragged(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if (event.getButton() == MouseButton.PRIMARY) {
						// Update position
						CanvasObject.this.x = MapGUI.fromScaled(event.getX() + CanvasObject.this.getTranslateX(), zoomPercent);
						CanvasObject.this.y = MapGUI.fromScaled(event.getY() + CanvasObject.this.getTranslateY(), zoomPercent);
						CanvasObject.this.setTranslateX(MapGUI.toScaled(CanvasObject.this.x, zoomPercent) - CanvasObject.this.imgWidth / 2);
						CanvasObject.this.setTranslateY(MapGUI.toScaled(CanvasObject.this.y, zoomPercent) - CanvasObject.this.imgHeight / 2);
					}
				}
			});

			this.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					// On right click delete image
					if (event.getButton() == MouseButton.SECONDARY) {
						((Pane) CanvasObject.this.getParent()).getChildren().remove(CanvasObject.this);
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

							// Visibility Layer
							TextField layerTextField = new TextField(Integer.toString(visLayer));

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
							if (collision) {
								collChoiceBox.getSelectionModel().selectFirst();
							} else {
								collChoiceBox.getSelectionModel().selectLast();
							}

							// Addition properties textbox
							TextField additTextField = new TextField();
							if (CanvasObject.this.extraProps != null) {
								additTextField.setText(String.join(",", CanvasObject.this.extraProps));
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
									// Make sure the text field isn't player, tile layer, or null
									if (!(layerTextField.getText().equals(Integer.toString(playerVisLayer)) || layerTextField.getText().equals(Integer.toString(tileVisLayer)) || layerTextField.getText().equals(""))) {
										// Update properties
										CanvasObject.this.visLayer = Integer.parseInt(layerTextField.getText());
										CanvasObject.this.collision = Boolean.parseBoolean(collChoiceBox.getSelectionModel().getSelectedItem());
										CanvasObject.this.extraProps = additTextField.getText().split(",");
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

		/**
		 * set the canvas which will contain object's image to be the same size as the image
		 *  and then draw the image on the canvas
		 */
		private void drawImage() {
			this.setWidth(imgWidth);
			this.setHeight(imgHeight);
			//clear the canvas for this object
			this.getGraphicsContext2D().clearRect(0, 0, this.getWidth(), this.getHeight());

			// Draw image onto the object's canvas
			this.getGraphicsContext2D().drawImage(objectImage, 0, 0);
			this.setTranslateX(MapGUI.toScaled(CanvasObject.this.x, zoomPercent) - CanvasObject.this.imgWidth / 2);
			this.setTranslateY(MapGUI.toScaled(CanvasObject.this.y, zoomPercent) - CanvasObject.this.imgHeight / 2);
		}

		/**
		 * zoom this canvasobject by some zoom percentage. This will scale the image and also translate it to the correct position
		 */
		protected void setZoomPercent(double newZoom) {
			this.zoomPercent = newZoom;

			ImageWithURL objImage = (ImageWithURL) objectImage;
			String objImageURL = objImage.getURL();

			//get the original dimensions of the object's image
			Image unzoomedImage = new Image(objImageURL);
			double unzoomedWidth = unzoomedImage.getWidth();
			double unzoomedHeight = unzoomedImage.getHeight();

			//now create a scaled (zoomed) version of the image to display as the object on the map
			ImageWithURL zoomedImage = new ImageWithURL(objImageURL, zoomPercent / 100 * unzoomedWidth, zoomPercent / 100 * unzoomedHeight, true, false, false);
			objectImage = zoomedImage;
			imgWidth = zoomedImage.getWidth();
			imgHeight = zoomedImage.getHeight();
			drawImage();
		}
	}