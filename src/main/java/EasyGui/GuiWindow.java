package EasyGui;
import TimeControl.CreateLogFile;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GuiWindow extends Application {

    @Override
    public void start(Stage primaryStage){

        Button button = new Button();
        button.setText("Start Simulate");//设置按钮文本
        button.setOnAction(event -> {
            System.out.println("Simulate start");

        });
        StackPane root = new StackPane();
        root.getChildren().add(button);
        Scene scene = new Scene(root);
        primaryStage.setTitle("EasyGui");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
        CreateLogFile.getInstance().log("[GUI]:Create window 'EasyGui' Successfully");
        CreateLogFile.getInstance().flush();
    }
}
