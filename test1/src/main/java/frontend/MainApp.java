package frontend;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load giao diện từ maintest.fxml
        Parent root = FXMLLoader.load(getClass().getResource("/maintest.fxml"));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("SNMP Browser Frontend");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
