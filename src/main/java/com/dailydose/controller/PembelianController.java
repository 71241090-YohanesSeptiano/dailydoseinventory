package com.dailydose.controller;

import com.dailydose.dao.BarangDAO;
import com.dailydose.dao.PembelianDAO;
import com.dailydose.model.*;
import com.dailydose.util.SessionManager;
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
 * PEMBELIAN CONTROLLER
 * Menangani logika transaksi pembelian (restock barang dari supplier).
 * Konsep MVC: Controller ini menghubungkan pembelian.fxml (View)
 * dengan PembelianDAO (Model).
 *
 * Alur: Pilih barang → isi jumlah & harga beli → tambah ke keranjang
 *       → proses → stok otomatis bertambah via Barang.tambahStok()
 */
public class PembelianController implements Initializable {

    // =========================================================
    //  INJECT komponen FXML
    // =========================================================
    @FXML private ComboBox<Barang> cmbBarang;
    @FXML private Label            lblStokSaatIni;
    @FXML private TextField        txtJumlah;
    @FXML private TextField        txtHargaBeli;
    @FXML private TextField        txtNamaToko;
    @FXML private Label            lblPesan;
    @FXML private Label            lblTotal;

    @FXML private TableView<DetailPembelian>              tabelKeranjang;
    @FXML private TableColumn<DetailPembelian, String>    colIdBarang;
    @FXML private TableColumn<DetailPembelian, String>    colNamaBarang;
    @FXML private TableColumn<DetailPembelian, Integer>   colJumlah;
    @FXML private TableColumn<DetailPembelian, Double>    colHargaBeli;
    @FXML private TableColumn<DetailPembelian, Double>    colSubtotal;

    private final BarangDAO    barangDAO    = new BarangDAO();
    private final PembelianDAO pembelianDAO = new PembelianDAO();

    // Data keranjang (belum disimpan ke DB)
    private ObservableList<DetailPembelian> keranjang = FXCollections.observableArrayList();

    // =========================================================
    //  INITIALIZE
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load semua barang ke ComboBox
        muatDaftarBarang();

        // Listener: saat pilih barang di ComboBox → tampilkan stok saat ini
        cmbBarang.setOnAction(e -> {
            Barang dipilih = cmbBarang.getValue();
            if (dipilih != null) {
                lblStokSaatIni.setText(String.valueOf(dipilih.getStok()));
            }
        });

        // Setup kolom tabel keranjang
        colIdBarang.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getIdBarang()));
        colNamaBarang.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getNamaBarang()));
        colJumlah.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().getJumlah()).asObject());
        colHargaBeli.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getHargaBeli()).asObject());
        colSubtotal.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getSubtotal()).asObject());

        // Format kolom harga
        colHargaBeli.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "Rp " + String.format("%,.0f", val));
            }
        });
        colSubtotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "Rp " + String.format("%,.0f", val));
            }
        });

        tabelKeranjang.setItems(keranjang);
    }

    // =========================================================
    //  LOAD daftar barang ke ComboBox
    // =========================================================
    private void muatDaftarBarang() {
        List<Barang> semua = barangDAO.getAllBarang();
        cmbBarang.setItems(FXCollections.observableArrayList(semua));

        // Custom display: tampilkan "[ID] Nama" di dropdown
        cmbBarang.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? null :
                    "[" + b.getIdBarang() + "] " + b.getNamaBarang());
            }
        });
        cmbBarang.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? null :
                    "[" + b.getIdBarang() + "] " + b.getNamaBarang());
            }
        });
    }

    // =========================================================
    //  TAMBAH item ke keranjang
    // =========================================================
    @FXML
    private void handleTambahKeKeranjang() {
        Barang barang = cmbBarang.getValue();
        if (barang == null) {
            tampilPesan("⚠️ Pilih barang terlebih dahulu!", true);
            return;
        }

        // Validasi jumlah
        int jumlah;
        try {
            jumlah = Integer.parseInt(txtJumlah.getText().trim());
            if (jumlah <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tampilPesan("⚠️ Jumlah harus berupa angka positif!", true);
            return;
        }

        // Validasi harga beli
        double hargaBeli;
        try {
            hargaBeli = Double.parseDouble(txtHargaBeli.getText().trim());
            if (hargaBeli <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tampilPesan("⚠️ Harga beli harus berupa angka positif!", true);
            return;
        }

        // Buat detail dan tambah ke keranjang
        DetailPembelian detail = new DetailPembelian(
            0, // idPembelian belum ada (di-generate saat simpan)
            barang.getIdBarang(),
            barang.getNamaBarang(),
            jumlah,
            hargaBeli
        );
        keranjang.add(detail);

        // Update total
        updateTotal();
        tampilPesan("✅ " + barang.getNamaBarang() + " x" + jumlah + " ditambahkan.", false);

        // Reset input
        txtJumlah.clear();
        txtHargaBeli.clear();
    }

    // =========================================================
    //  HAPUS item dari keranjang
    // =========================================================
    @FXML
    private void handleHapusItem() {
        DetailPembelian dipilih = tabelKeranjang.getSelectionModel().getSelectedItem();
        if (dipilih == null) {
            tampilPesan("⚠️ Pilih item di keranjang dulu!", true);
            return;
        }
        keranjang.remove(dipilih);
        updateTotal();
        tampilPesan("✅ Item dihapus dari keranjang.", false);
    }

    // =========================================================
    //  PROSES PEMBELIAN — simpan ke database + update stok
    // =========================================================
    @FXML
    private void handleProses() {
        if (keranjang.isEmpty()) {
            tampilPesan("⚠️ Keranjang kosong! Tambahkan item dulu.", true);
            return;
        }

        // Konfirmasi
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Pembelian");
        konfirmasi.setHeaderText("Proses Pembelian");
        konfirmasi.setContentText("Total pembayaran: " + lblTotal.getText() +
            "\nLanjutkan proses pembelian?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Buat objek Pembelian
                int idUser = SessionManager.getInstance().getCurrentUser().getIdUser();
                String namaToko = txtNamaToko.getText().trim();
                
                if (namaToko.isEmpty()) {
                    tampilPesan("⚠️ Nama Toko/Supplier tidak boleh kosong!", true);
                    return;
                }

                Pembelian pembelian = new Pembelian(idUser, namaToko);

                // Tambahkan semua detail
                for (DetailPembelian d : keranjang) {
                    pembelian.tambahDetail(d);
                }
                pembelian.hitungTotalBeli();

                // Simpan ke database (DAO akan otomatis update stok via tambahStok())
                boolean berhasil = pembelianDAO.simpanPembelian(pembelian);

                if (berhasil) {
                    tampilPesan("✅ Pembelian berhasil diproses! Stok sudah diperbarui.", false);
                    keranjang.clear();
                    updateTotal();
                    txtNamaToko.clear();
                    muatDaftarBarang(); // refresh stok di ComboBox
                } else {
                    tampilPesan("❌ Gagal memproses pembelian.", true);
                }
            }
        });
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
            Stage stage = (Stage) tabelKeranjang.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("DailyDose Inventory — Dashboard");
        } catch (IOException e) {
            tampilPesan("❌ Gagal kembali ke dashboard.", true);
        }
    }

    // =========================================================
    //  HELPER
    // =========================================================
    private void updateTotal() {
        double total = keranjang.stream()
            .mapToDouble(DetailPembelian::getSubtotal)
            .sum();
        lblTotal.setText("Rp " + String.format("%,.0f", total));
    }

    private void tampilPesan(String pesan, boolean isError) {
        lblPesan.setText(pesan);
        lblPesan.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
