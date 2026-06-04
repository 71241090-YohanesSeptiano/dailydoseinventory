package com.dailydose.controller;

import com.dailydose.dao.UserDAO;
import com.dailydose.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * USER CONTROLLER
 * Menangani operasi CRUD untuk data User.
 * Konsep MVC: Controller ini menghubungkan user.fxml (View) dengan UserDAO (Model).
 * Hanya Admin yang bisa mengakses halaman ini (guard di DashboardController).
 */
public class UserController implements Initializable {

    // =========================================================
    //  INJECT komponen FXML
    // =========================================================
    @FXML private TextField       txtUsername;
    @FXML private TextField       txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label           lblPesan;

    @FXML private TableView<User>            tabelUser;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;

    private final UserDAO userDAO = new UserDAO();
    private ObservableList<User> dataUser = FXCollections.observableArrayList();

    // ID user yang sedang di-edit (0 = mode tambah baru)
    private int editingUserId = 0;

    // =========================================================
    //  INITIALIZE
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Isi dropdown role
        cmbRole.setItems(FXCollections.observableArrayList("Admin", "Staf"));
        cmbRole.setValue("Staf");

        // Setup kolom tabel
        colId.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(d -> {
            String role = d.getValue().getRole();
            if (d.getValue().getUsername().equalsIgnoreCase("admin")) {
                role = "Owner";
            }
            return new javafx.beans.property.SimpleStringProperty(role);
        });

        tabelUser.setItems(dataUser);
        muatSemuaUser();
    }

    // =========================================================
    //  LOAD DATA
    // =========================================================
    private void muatSemuaUser() {
        dataUser.clear();
        dataUser.addAll(userDAO.getAllUsers());
    }

    // =========================================================
    //  TAMBAH user baru
    // =========================================================
    @FXML
    private void handleTambah() {
        if (!isFormValid()) return;

        User userBaru = new User(
            txtUsername.getText().trim(),
            txtPassword.getText().trim(),
            cmbRole.getValue()
        );

        boolean berhasil = userDAO.tambahUser(userBaru);
        if (berhasil) {
            tampilPesan("✅ User berhasil ditambahkan!", false);
            muatSemuaUser();
            handleReset();
        } else {
            tampilPesan("❌ Gagal menambahkan user. Username mungkin sudah ada.", true);
        }
    }

    // =========================================================
    //  EDIT user yang dipilih
    // =========================================================
    @FXML
    private void handleEdit() {
        if (editingUserId == 0) {
            tampilPesan("⚠️ Pilih user di tabel dulu sebelum edit!", true);
            return;
        }
        if (!isFormValid()) return;

        User userUpdate = new User(
            editingUserId,
            txtUsername.getText().trim(),
            txtPassword.getText().trim(),
            cmbRole.getValue()
        );

        boolean berhasil = userDAO.updateUser(userUpdate);
        if (berhasil) {
            tampilPesan("✅ User berhasil diupdate!", false);
            muatSemuaUser();
            handleReset();
        } else {
            tampilPesan("❌ Gagal update user.", true);
        }
    }

    // =========================================================
    //  HAPUS user yang dipilih
    // =========================================================
    @FXML
    private void handleHapus() {
        User dipilih = tabelUser.getSelectionModel().getSelectedItem();
        if (dipilih == null) {
            tampilPesan("⚠️ Pilih user di tabel dulu sebelum hapus!", true);
            return;
        }

        User currentUser = com.dailydose.util.SessionManager.getInstance().getCurrentUser();

        // Jangan hapus diri sendiri
        if (dipilih.getIdUser() == currentUser.getIdUser()) {
            tampilPesan("❌ Tidak bisa menghapus akun sendiri!", true);
            return;
        }

        // Mencegah menghapus Super Admin bawaan
        if (dipilih.getUsername().equalsIgnoreCase("admin")) {
            tampilPesan("❌ Akun Super Admin bawaan tidak boleh dihapus!", true);
            return;
        }

        // Mencegah admin biasa menghapus admin lain
        if (dipilih.getRole().equals("Admin") && !currentUser.getUsername().equalsIgnoreCase("admin")) {
            tampilPesan("❌ Hanya Super Admin (admin) yang berhak menghapus Admin lain!", true);
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText("Hapus User");
        konfirmasi.setContentText("Yakin ingin menghapus user \"" + dipilih.getUsername() + "\"?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean berhasil = userDAO.deleteUser(dipilih.getIdUser());
                if (berhasil) {
                    tampilPesan("✅ User berhasil dihapus!", false);
                    muatSemuaUser();
                    handleReset();
                } else {
                    tampilPesan("❌ Gagal hapus user.", true);
                }
            }
        });
    }

    // =========================================================
    //  KLIK baris tabel → isi form
    // =========================================================
    @FXML
    private void handlePilihBaris() {
        User dipilih = tabelUser.getSelectionModel().getSelectedItem();
        if (dipilih == null) return;

        editingUserId = dipilih.getIdUser();
        txtUsername.setText(dipilih.getUsername());
        txtPassword.setText(dipilih.getPassword());
        cmbRole.setValue(dipilih.getRole());

        tampilPesan("ℹ️ Data dimuat untuk editing. ID: " + dipilih.getIdUser(), false);
    }

    // =========================================================
    //  RESET form
    // =========================================================
    @FXML
    private void handleReset() {
        editingUserId = 0;
        txtUsername.clear();
        txtPassword.clear();
        cmbRole.setValue("Staf");
        lblPesan.setText("");
        tabelUser.getSelectionModel().clearSelection();
    }

    // =========================================================
    //  KEMBALI ke Dashboard
    // =========================================================
    @FXML
    private void kembaliDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/dailydose/view/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tabelUser.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("DailyDose Inventory — Dashboard");
        } catch (IOException e) {
            tampilPesan("❌ Gagal kembali ke dashboard.", true);
        }
    }

    // =========================================================
    //  VALIDASI form
    // =========================================================
    private boolean isFormValid() {
        if (txtUsername.getText().trim().isEmpty()) {
            tampilPesan("⚠️ Username tidak boleh kosong!", true);
            return false;
        }
        if (txtPassword.getText().trim().isEmpty()) {
            tampilPesan("⚠️ Password tidak boleh kosong!", true);
            return false;
        }
        if (txtPassword.getText().trim().length() < 4) {
            tampilPesan("⚠️ Password minimal 4 karakter!", true);
            return false;
        }
        return true;
    }

    // =========================================================
    //  HELPER
    // =========================================================
    private void tampilPesan(String pesan, boolean isError) {
        lblPesan.setText(pesan);
        lblPesan.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
