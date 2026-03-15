package EasyGui;
import Simulation.CreateLogFile;
import Simulation.WorldEnviroment;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GuiWindow extends Application {

    @Override
    public void start(Stage primaryStage){

        Button startButton = new Button("Start Simulate");
        startButton.setOnAction(event -> {
            WorldEnviroment.resetSimulation();
            WorldEnviroment.uiStartSimulation();   // 启动，速度 1.0x
        });
        StackPane root = new StackPane();
        root.getChildren().add(startButton);
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
