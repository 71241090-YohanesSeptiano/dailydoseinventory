package com.dailydose.controller;

import com.dailydose.dao.BarangDAO;
import com.dailydose.dao.PenjualanDAO;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * KASIR CONTROLLER
 * Menangani logika transaksi penjualan (barang keluar ke pelanggan).
 * Konsep MVC: Controller ini menghubungkan kasir.fxml (View)
 * dengan PenjualanDAO (Model).
 *
 * Konsep Exception Handling:
 *   - Saat proses penjualan, Barang.kurangiStok() dipanggil.
 *   - Jika stok tidak mencukupi, method itu throw IllegalArgumentException.
 *   - Controller menangkap exception ini dan menampilkan pesan error ke user.
 */
public class KasirController implements Initializable {

    // =========================================================
    //  INJECT komponen FXML
    // =========================================================
    @FXML private ComboBox<Barang> cmbBarang;
    @FXML private Label            lblStokTersedia;
    @FXML private Label            lblHargaJual;
    @FXML private TextField        txtJumlah;
    @FXML private Label            lblPesan;
    @FXML private Label            lblTotal;

    @FXML private TableView<DetailPenjualan>              tabelKeranjang;
    @FXML private TableColumn<DetailPenjualan, String>    colIdBarang;
    @FXML private TableColumn<DetailPenjualan, String>    colNamaBarang;
    @FXML private TableColumn<DetailPenjualan, Integer>   colJumlah;
    @FXML private TableColumn<DetailPenjualan, Double>    colHarga;
    @FXML private TableColumn<DetailPenjualan, Double>    colSubtotal;

    private final BarangDAO    barangDAO    = new BarangDAO();
    private final PenjualanDAO penjualanDAO = new PenjualanDAO();

    // Data keranjang belanja
    private ObservableList<DetailPenjualan> keranjang = FXCollections.observableArrayList();

    // =========================================================
    //  INITIALIZE
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        muatDaftarBarang();

        // Listener: pilih barang → tampilkan stok & harga jual
        cmbBarang.setOnAction(e -> {
            Barang dipilih = cmbBarang.getValue();
            if (dipilih != null) {
                lblStokTersedia.setText(String.valueOf(dipilih.getStok()));
                lblHargaJual.setText("Rp " + String.format("%,.0f", dipilih.getHarga()));
            }
        });

        // Setup kolom tabel
        colIdBarang.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getIdBarang()));
        colNamaBarang.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getNamaBarang()));
        colJumlah.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().getJumlah()).asObject());
        colHarga.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getSubtotal() / d.getValue().getJumlah()).asObject());
        colSubtotal.setCellValueFactory(d ->
            new SimpleDoubleProperty(d.getValue().getSubtotal()).asObject());

        // Format rupiah
        colHarga.setCellFactory(col -> new TableCell<>() {
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
    //  LOAD daftar barang ke ComboBox (hanya yang stok > 0)
    // =========================================================
    private void muatDaftarBarang() {
        List<Barang> semua = barangDAO.getAllBarang();
        cmbBarang.setItems(FXCollections.observableArrayList(semua));

        cmbBarang.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) {
                    setText(null);
                } else {
                    setText("[" + b.getIdBarang() + "] " + b.getNamaBarang()
                        + " (stok: " + b.getStok() + ")");
                }
            }
        });
        cmbBarang.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) {
                    setText(null);
                } else {
                    setText("[" + b.getIdBarang() + "] " + b.getNamaBarang());
                }
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

        // Cek stok cukup (validasi awal di controller — validasi final di DAO)
        if (jumlah > barang.getStok()) {
            tampilPesan("⚠️ Stok tidak mencukupi! Tersedia: " + barang.getStok(), true);
            return;
        }

        // Buat detail penjualan (ID transaksi sementara, di-generate saat proses)
        DetailPenjualan detail = new DetailPenjualan(
            "TEMP",
            barang.getIdBarang(),
            barang.getNamaBarang(),
            jumlah,
            barang.getHarga(), // harga jual
            barang.getHargaBeli() // modal
        );
        keranjang.add(detail);

        updateTotal();
        tampilPesan("✅ " + barang.getNamaBarang() + " x" + jumlah + " ditambahkan.", false);
        txtJumlah.clear();
    }

    // =========================================================
    //  HAPUS item dari keranjang
    // =========================================================
    @FXML
    private void handleHapusItem() {
        DetailPenjualan dipilih = tabelKeranjang.getSelectionModel().getSelectedItem();
        if (dipilih == null) {
            tampilPesan("⚠️ Pilih item di keranjang dulu!", true);
            return;
        }
        keranjang.remove(dipilih);
        updateTotal();
        tampilPesan("✅ Item dihapus dari keranjang.", false);
    }

    // =========================================================
    //  PROSES PENJUALAN — simpan ke database + kurangi stok
    //  Konsep: Exception Handling (fitur no.8 proposal)
    // =========================================================
    @FXML
    private void handleProses() {
        if (keranjang.isEmpty()) {
            tampilPesan("⚠️ Keranjang kosong! Tambahkan item dulu.", true);
            return;
        }

        // Konfirmasi
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Penjualan");
        konfirmasi.setHeaderText("Proses Penjualan");
        konfirmasi.setContentText("Total bayar: " + lblTotal.getText() +
            "\nLanjutkan proses penjualan?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                prosesTransaksi();
            }
        });
    }

    private void prosesTransaksi() {
        // Generate ID transaksi unik: TRX-yyyyMMdd-HHmmss
        String idTransaksi = "TRX-" + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        int idUser = SessionManager.getInstance().getCurrentUser().getIdUser();
        Penjualan penjualan = new Penjualan(idTransaksi, idUser);

        // Re-create details dengan ID transaksi yang benar
        for (DetailPenjualan d : keranjang) {
            DetailPenjualan real = new DetailPenjualan(
                idTransaksi,
                d.getIdBarang(),
                d.getNamaBarang(),
                d.getJumlah(),
                d.getSubtotal() / d.getJumlah(),  // harga satuan
                d.getHargaBeli()                  // modal
            );
            penjualan.tambahDetail(real);
        }
        penjualan.hitungTotal();

        try {
            // simpanPenjualan() memanggil Barang.kurangiStok() secara internal
            // Jika stok tidak cukup → throw IllegalArgumentException
            boolean berhasil = penjualanDAO.simpanPenjualan(penjualan);

            if (berhasil) {
                tampilPesan("✅ Penjualan berhasil! ID: " + idTransaksi, false);
                keranjang.clear();
                updateTotal();
                muatDaftarBarang(); // refresh stok di ComboBox
            } else {
                tampilPesan("❌ Gagal memproses penjualan.", true);
            }

        } catch (IllegalArgumentException e) {
            // ← Exception dari Barang.kurangiStok() — stok tidak mencukupi
            tampilPesan("❌ " + e.getMessage(), true);
        }
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
            .mapToDouble(DetailPenjualan::getSubtotal)
            .sum();
        lblTotal.setText("Rp " + String.format("%,.0f", total));
    }

    private void tampilPesan(String pesan, boolean isError) {
        lblPesan.setText(pesan);
        lblPesan.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
