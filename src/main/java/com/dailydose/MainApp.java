package com.dailydose;

import com.dailydose.util.DatabaseHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MAIN APP — Entry point aplikasi JavaFX
 * Urutan eksekusi:
 *   1. main() → memanggil launch()
 *   2. launch() → memanggil start()
 *   3. start() → inisialisasi database & tampilkan halaman Login
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inisialisasi database (buat file .db + semua tabel jika belum ada)
        DatabaseHelper.getInstance();

        // Load halaman Login dari FXML
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/dailydose/view/login.fxml")
        );
        Parent root = loader.load();

        // Tampilkan window
        primaryStage.setTitle("DailyDose Inventory — Login");
        primaryStage.setScene(new Scene(root, 450, 350));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
