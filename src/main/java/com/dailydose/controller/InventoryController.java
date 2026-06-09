package com.dailydose.controller;

import com.dailydose.dao.BarangDAO;
import com.dailydose.model.*;
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
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * INVENTORY CONTROLLER
 * Menangani semua operasi CRUD untuk data barang.
 * Terhubung ke inventory.fxml via fx:controller.
 */
public class InventoryController implements Initializable {

    // =========================================================
    //  INJECT komponen FXML
    // =========================================================
    @FXML private TextField  txtIdBarang;
    @FXML private TextField  txtNamaBarang;
    @FXML private TextField  txtHarga;
    @FXML private TextField  txtHargaBeli;
    @FXML private ComboBox<String> cmbKategori;
    @FXML private Label      lblSpesifik;
    @FXML private TextField  txtSpesifik;
    @FXML private Label      lblPesan;
    @FXML private TextField  txtCari;

    // Tabel & kolom-kolomnya
    @FXML private TableView<Barang>        tabelBarang;
    @FXML private TableColumn<Barang, String> colId;
    @FXML private TableColumn<Barang, String> colNama;
    @FXML private TableColumn<Barang, String> colKategori;
    @FXML private TableColumn<Barang, Double> colHarga;
    @FXML private TableColumn<Barang, Double> colHargaBeli;
    @FXML private TableColumn<Barang, Integer> colStok;
    @FXML private TableColumn<Barang, String> colSpesifik;

    private final BarangDAO barangDAO = new BarangDAO();

    // Data yang ditampilkan di tabel (ObservableList otomatis update UI)
    private ObservableList<Barang> dataBarang = FXCollections.observableArrayList();

    // =========================================================
    //  INITIALIZE — setup tabel & dropdown saat halaman dibuka
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Isi dropdown kategori
        cmbKategori.setItems(FXCollections.observableArrayList("PecahBelah", "Konsumsi"));
        cmbKategori.setValue("PecahBelah");

        // Setup kolom tabel → hubungkan ke getter di class Barang
        colId.setCellValueFactory(new PropertyValueFactory<>("idBarang"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colKategori.setCellValueFactory(new PropertyValueFactory<>("kategori"));
        colHarga.setCellValueFactory(new PropertyValueFactory<>("harga"));
        colHargaBeli.setCellValueFactory(new PropertyValueFactory<>("hargaBeli"));
        colStok.setCellValueFactory(new PropertyValueFactory<>("stok"));

        // Kolom spesifik → tampilkan info khusus subclass
        colSpesifik.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getInfoKategori()
                    .replaceAll("Kategori: .*?\\| ", "") // singkat tampilannya
            )
        );

        // Format kolom harga supaya tampil Rp
        colHarga.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double harga, boolean empty) {
                super.updateItem(harga, empty);
                setText(empty || harga == null ? null :
                    "Rp " + String.format("%,.0f", harga));
            }
        });
        colHargaBeli.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double harga, boolean empty) {
                super.updateItem(harga, empty);
                setText(empty || harga == null ? null :
                    "Rp " + String.format("%,.0f", harga));
            }
        });

        // Warnai baris yang stoknya kritis (≤ 5)
        tabelBarang.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Barang item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.getStok() <= 5) {
                    setStyle("-fx-background-color: #fdecea;"); // merah muda
                } else {
                    setStyle("");
                }
            }
        });

        // Hubungkan dataBarang ke tabel
        tabelBarang.setItems(dataBarang);

        // Load data dari database
        muatSemuaBarang();
    }

    // =========================================================
    //  LOAD DATA
    // =========================================================
    private void muatSemuaBarang() {
        dataBarang.clear();
        dataBarang.addAll(barangDAO.getAllBarang());
    }

    // =========================================================
    //  EVENT: Dropdown kategori berubah → ubah label field spesifik
    // =========================================================
    @FXML
    private void handleKategoriChange() {
        String kategori = cmbKategori.getValue();
        if ("PecahBelah".equals(kategori)) {
            lblSpesifik.setText("Material:");
            txtSpesifik.setPromptText("Contoh: Kaca, Keramik");
        } else {
            lblSpesifik.setText("Tgl Kadaluarsa:");
            txtSpesifik.setPromptText("Format: YYYY-MM-DD");
        }
    }

    // =========================================================
    //  TAMBAH barang baru
    // =========================================================
    @FXML
    private void handleTambah() {
        // Validasi input
        if (!isFormValid()) return;

        try {
            Barang barangBaru = buatBarangDariForm();
            boolean berhasil = barangDAO.tambahBarang(barangBaru);

            if (berhasil) {
                tampilPesan("✅ Barang berhasil ditambahkan!", false);
                muatSemuaBarang();
                handleReset();
            } else {
                tampilPesan("❌ Gagal menambahkan barang. ID mungkin sudah ada.", true);
            }

        } catch (Exception e) {
            tampilPesan("❌ Error: " + e.getMessage(), true);
        }
    }

    // =========================================================
    //  EDIT barang yang dipilih di tabel
    // =========================================================
    @FXML
    private void handleEdit() {
        Barang dipilih = tabelBarang.getSelectionModel().getSelectedItem();

        if (dipilih == null) {
            tampilPesan("⚠️ Pilih barang di tabel dulu sebelum edit!", true);
            return;
        }
        if (!isFormValid()) return;

        try {
            Barang barangUpdate = buatBarangDariForm();
            // Stok ikut dari data lama (tidak boleh diubah manual di sini)
            barangUpdate.setStok(dipilih.getStok());

            boolean berhasil = barangDAO.updateBarang(barangUpdate);

            if (berhasil) {
                tampilPesan("✅ Barang berhasil diupdate!", false);
                muatSemuaBarang();
                handleReset();
            } else {
                tampilPesan("❌ Gagal update barang.", true);
            }

        } catch (Exception e) {
            tampilPesan("❌ Error: " + e.getMessage(), true);
        }
    }

    // =========================================================
    //  HAPUS barang yang dipilih
    // =========================================================
    @FXML
    private void handleHapus() {
        Barang dipilih = tabelBarang.getSelectionModel().getSelectedItem();

        if (dipilih == null) {
            tampilPesan("⚠️ Pilih barang di tabel dulu sebelum hapus!", true);
            return;
        }

        // Konfirmasi sebelum hapus
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText("Hapus Barang");
        konfirmasi.setContentText("Yakin ingin menghapus barang \"" + dipilih.getNamaBarang() + "\"?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean berhasil = barangDAO.deleteBarang(dipilih.getIdBarang());
                if (berhasil) {
                    tampilPesan("✅ Barang berhasil dihapus!", false);
                    muatSemuaBarang();
                    handleReset();
                } else {
                    tampilPesan("❌ Gagal hapus. Mungkin barang sudah pernah ditransaksikan.", true);
                }
            }
        });
    }

    // =========================================================
    //  CARI barang (real-time saat mengetik)
    // =========================================================
    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim();
        if (keyword.isEmpty()) {
            muatSemuaBarang();
            return;
        }
        List<Barang> hasil = barangDAO.cariBarang(keyword);
        dataBarang.clear();
        dataBarang.addAll(hasil);
    }

    @FXML
    private void handleTampilSemua() {
        txtCari.clear();
        muatSemuaBarang();
        tampilPesan("", false);
    }

    // =========================================================
    //  KLIK baris di tabel → isi form otomatis
    // =========================================================
    @FXML
    private void handlePilihBaris() {
        Barang dipilih = tabelBarang.getSelectionModel().getSelectedItem();
        if (dipilih == null) return;

        txtIdBarang.setText(dipilih.getIdBarang());
        txtIdBarang.setDisable(true); // ID tidak boleh diubah saat edit
        txtNamaBarang.setText(dipilih.getNamaBarang());
        txtHarga.setText(String.valueOf(dipilih.getHarga()));
        txtHargaBeli.setText(String.valueOf(dipilih.getHargaBeli()));
        cmbKategori.setValue(dipilih.getKategori());

        // Isi field spesifik
        if (dipilih instanceof BarangPecahBelah bp) {
            lblSpesifik.setText("Material:");
            txtSpesifik.setText(bp.getMaterial());
        } else if (dipilih instanceof BarangKonsumsi bk) {
            lblSpesifik.setText("Tgl Kadaluarsa:");
            txtSpesifik.setText(bk.getTanggalKadaluarsa().toString());
        }

        tampilPesan("ℹ️ Data dimuat. Stok: " + dipilih.getStok() +
                    " (hanya berubah via Transaksi)", false);
    }

    // =========================================================
    //  RESET form
    // =========================================================
    @FXML
    private void handleReset() {
        txtIdBarang.clear();
        txtIdBarang.setDisable(false);
        txtNamaBarang.clear();
        txtHarga.clear();
        txtHargaBeli.clear();
        txtSpesifik.clear();
        cmbKategori.setValue("PecahBelah");
        lblSpesifik.setText("Material:");
        lblPesan.setText("");
        tabelBarang.getSelectionModel().clearSelection();
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
            Stage stage = (Stage) tabelBarang.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("DailyDose Inventory — Dashboard");
        } catch (IOException e) {
            tampilPesan("❌ Gagal kembali ke dashboard.", true);
        }
    }

    // =========================================================
    //  HELPER — Buat objek Barang dari isian form
    // =========================================================
    private Barang buatBarangDariForm() {
        String id       = txtIdBarang.getText().trim();
        String nama     = txtNamaBarang.getText().trim();
        double harga    = Double.parseDouble(txtHarga.getText().trim());
        double hargaBeli= Double.parseDouble(txtHargaBeli.getText().trim());
        String kategori = cmbKategori.getValue();
        String spesifik = txtSpesifik.getText().trim();

        if ("PecahBelah".equals(kategori)) {
            return new BarangPecahBelah(id, nama, harga, hargaBeli, 0, spesifik);
        } else {
            LocalDate tgl = LocalDate.parse(spesifik); // format YYYY-MM-DD
            return new BarangKonsumsi(id, nama, harga, hargaBeli, 0, tgl);
        }
    }

    // =========================================================
    //  HELPER — Validasi form sebelum proses
    // =========================================================
    private boolean isFormValid() {
        if (txtIdBarang.getText().trim().isEmpty()) {
            tampilPesan("⚠️ ID Barang tidak boleh kosong!", true);
            return false;
        }
        if (txtNamaBarang.getText().trim().isEmpty()) {
            tampilPesan("⚠️ Nama Barang tidak boleh kosong!", true);
            return false;
        }
        try {
            double harga = Double.parseDouble(txtHarga.getText().trim());
            if (harga <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tampilPesan("⚠️ Harga harus berupa angka positif!", true);
            return false;
        }
        try {
            double modal = Double.parseDouble(txtHargaBeli.getText().trim());
            if (modal < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tampilPesan("⚠️ Modal (Harga Beli) harus berupa angka positif!", true);
            return false;
        }
        if (txtSpesifik.getText().trim().isEmpty()) {
            tampilPesan("⚠️ Field " + lblSpesifik.getText() + " tidak boleh kosong!", true);
            return false;
        }
        // Validasi format tanggal kalau kategori Konsumsi
        if ("Konsumsi".equals(cmbKategori.getValue())) {
            try {
                LocalDate.parse(txtSpesifik.getText().trim());
            } catch (Exception e) {
                tampilPesan("⚠️ Format tanggal salah! Gunakan YYYY-MM-DD. Contoh: 2025-12-31", true);
                return false;
            }
        }
        return true;
    }

    // =========================================================
    //  HELPER — Tampilkan pesan di label
    // =========================================================
    private void tampilPesan(String pesan, boolean isError) {
        lblPesan.setText(pesan);
        lblPesan.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
