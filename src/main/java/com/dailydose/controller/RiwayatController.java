package com.dailydose.controller;

import com.dailydose.dao.PenjualanDAO;
import com.dailydose.model.DetailPenjualan;
import com.dailydose.model.Penjualan;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * RIWAYAT CONTROLLER
 * Menampilkan riwayat semua transaksi penjualan (read-only).
 * Konsep MVC: Controller ini menghubungkan riwayat.fxml (View)
 * dengan PenjualanDAO (Model).
 *
 * Klik transaksi di tabel atas → detail item muncul di tabel bawah.
 */
public class RiwayatController implements Initializable {

    // =========================================================
    //  INJECT komponen FXML
    // =========================================================
    @FXML private TableView<Penjualan>               tabelPenjualan;
    @FXML private TableColumn<Penjualan, String>     colIdTransaksi;
    @FXML private TableColumn<Penjualan, String>     colTanggal;
    @FXML private TableColumn<Penjualan, Double>     colTotal;
    @FXML private TableColumn<Penjualan, Double>     colTotalKeuntungan;
    @FXML private TableColumn<Penjualan, Integer>    colIdUser;

    @FXML private Label                               lblDetailHeader;
    @FXML private TableView<DetailPenjualan>          tabelDetail;
    @FXML private TableColumn<DetailPenjualan, String>  colDetailBarang;
    @FXML private TableColumn<DetailPenjualan, String>  colDetailNama;
    @FXML private TableColumn<DetailPenjualan, Integer> colDetailJumlah;
    @FXML private TableColumn<DetailPenjualan, Double>  colDetailSubtotal;
    @FXML private TableColumn<DetailPenjualan, Double>  colDetailKeuntungan;

    private final PenjualanDAO penjualanDAO = new PenjualanDAO();

    private ObservableList<Penjualan>       dataPenjualan = FXCollections.observableArrayList();
    private ObservableList<DetailPenjualan> dataDetail    = FXCollections.observableArrayList();

    // =========================================================
    //  INITIALIZE
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Setup kolom tabel header penjualan
        colIdTransaksi.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getIdTransaksi()));
        colTanggal.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getTanggal().toString()));
        colTotal.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getTotalBayar()).asObject());
        colTotalKeuntungan.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getTotalKeuntungan()).asObject());
        colIdUser.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().getIdUser()).asObject());

        // Format rupiah untuk kolom total
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "Rp " + String.format("%,.0f", val));
            }
        });
        colTotalKeuntungan.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "Rp " + String.format("%,.0f", val));
            }
        });

        tabelPenjualan.setItems(dataPenjualan);

        // Setup kolom tabel detail
        colDetailBarang.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getIdBarang()));
        colDetailNama.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getNamaBarang()));
        colDetailJumlah.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().getJumlah()).asObject());
        colDetailSubtotal.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getSubtotal()).asObject());
        colDetailKeuntungan.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getKeuntungan()).asObject());

        // Format rupiah
        colDetailSubtotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "Rp " + String.format("%,.0f", val));
            }
        });
        colDetailKeuntungan.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "Rp " + String.format("%,.0f", val));
            }
        });

        tabelDetail.setItems(dataDetail);

        // Load semua riwayat penjualan
        muatRiwayat();
    }

    // =========================================================
    //  LOAD riwayat dari database
    // =========================================================
    private void muatRiwayat() {
        dataPenjualan.clear();
        dataPenjualan.addAll(penjualanDAO.getAllPenjualan());
    }

    // =========================================================
    //  KLIK transaksi → tampilkan detail item
    // =========================================================
    @FXML
    private void handlePilihTransaksi() {
        Penjualan dipilih = tabelPenjualan.getSelectionModel().getSelectedItem();
        if (dipilih == null) return;

        lblDetailHeader.setText("Detail Transaksi — " + dipilih.getIdTransaksi());

        // Load detail dari database
        dataDetail.clear();
        List<DetailPenjualan> details = penjualanDAO.getDetailByTransaksi(
            dipilih.getIdTransaksi()
        );
        dataDetail.addAll(details);
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
            Stage stage = (Stage) tabelPenjualan.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("DailyDose Inventory — Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
