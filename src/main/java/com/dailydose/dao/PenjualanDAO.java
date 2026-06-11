package com.dailydose.dao;

import com.dailydose.model.*;
import com.dailydose.util.DatabaseHelper;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * PENJUALAN DAO (Data Access Object)
 * Menangani operasi database untuk tabel 'penjualan' dan 'detail_penjualan'.
 * Saat menyimpan penjualan, method ini memanggil Barang.kurangiStok()
 * untuk memvalidasi dan memperbarui stok secara otomatis.
 *
 * Konsep: Exception Handling — jika stok tidak mencukupi,
 *         kurangiStok() akan throw IllegalArgumentException
 *         dan seluruh transaksi di-ROLLBACK.
 */
public class PenjualanDAO {

    private final Connection conn;
    private final BarangDAO barangDAO;

    public PenjualanDAO() {
        this.conn      = DatabaseHelper.getInstance().getConnection();
        this.barangDAO = new BarangDAO();
    }

    // =========================================================
    //  SIMPAN PENJUALAN + KURANGI STOK (Transactional)
    // =========================================================
    public boolean simpanPenjualan(Penjualan penjualan) throws IllegalArgumentException {
        try {
            conn.setAutoCommit(false);

            // 1. INSERT header penjualan
            String sqlHeader = """
                INSERT INTO penjualan (id_transaksi, tanggal, total_bayar, total_keuntungan, id_user)
                VALUES (?, ?, ?, ?, ?)
            """;
            PreparedStatement psHeader = conn.prepareStatement(sqlHeader);
            psHeader.setString(1, penjualan.getIdTransaksi());
            psHeader.setString(2, penjualan.getTanggal().toString());
            psHeader.setDouble(3, penjualan.getTotalBayar());
            psHeader.setDouble(4, penjualan.getTotalKeuntungan());
            psHeader.setInt   (5, penjualan.getIdUser());
            psHeader.executeUpdate();

            // 2. INSERT setiap detail + KURANGI stok
            String sqlDetail = """
                INSERT INTO detail_penjualan (id_transaksi, id_barang, jumlah, subtotal, keuntungan)
                VALUES (?, ?, ?, ?, ?)
            """;
            for (DetailPenjualan d : penjualan.getListDetail()) {
                // 2a. Insert detail
                PreparedStatement psDetail = conn.prepareStatement(sqlDetail);
                psDetail.setString(1, d.getIdTransaksi());
                psDetail.setString(2, d.getIdBarang());
                psDetail.setInt   (3, d.getJumlah());
                psDetail.setDouble(4, d.getSubtotal());
                psDetail.setDouble(5, d.getKeuntungan());
                psDetail.executeUpdate();

                // 2b. Load barang → kurangiStok() → simpan
                //     kurangiStok() akan THROW EXCEPTION jika stok tidak cukup!
                //     → Exception Handling (fitur no.8 proposal)
                Barang barang = barangDAO.getBarangById(d.getIdBarang());
                if (barang != null) {
                    barang.kurangiStok(d.getJumlah());  // ← bisa throw IllegalArgumentException!
                    barangDAO.updateStok(barang.getIdBarang(), barang.getStok());
                }
            }

            // 3. COMMIT
            conn.commit();
            System.out.println("✅ Penjualan berhasil. ID: " + penjualan.getIdTransaksi());
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("❌ Gagal simpan penjualan: " + e.getMessage());
            return false;

        } catch (IllegalArgumentException e) {
            // Ini dari kurangiStok() → stok tidak mencukupi
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;  // ← lempar ke Controller agar bisa tampilkan pesan ke user

        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // =========================================================
    //  READ — Semua riwayat penjualan (untuk halaman Riwayat)
    // =========================================================
    public List<Penjualan> getAllPenjualan() {
        List<Penjualan> list = new ArrayList<>();
        String sql = """
            SELECT p.*, u.username
            FROM penjualan p
            JOIN users u ON p.id_user = u.id_user
            ORDER BY p.tanggal DESC
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Penjualan p = new Penjualan(
                    rs.getString("id_transaksi"),
                    rs.getInt("id_user")
                );
                p.setTotalBayar(rs.getDouble("total_bayar"));
                p.setTotalKeuntungan(rs.getDouble("total_keuntungan"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal ambil riwayat penjualan: " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    //  READ — Detail item dari satu transaksi tertentu
    // =========================================================
    public List<DetailPenjualan> getDetailByTransaksi(String idTransaksi) {
        List<DetailPenjualan> list = new ArrayList<>();
        String sql = """
            SELECT dp.*, b.nama_barang, b.harga_beli
            FROM detail_penjualan dp
            JOIN barang b ON dp.id_barang = b.id_barang
            WHERE dp.id_transaksi = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idTransaksi);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new DetailPenjualan(
                    rs.getInt("id_detail"),
                    rs.getString("id_transaksi"),
                    rs.getString("id_barang"),
                    rs.getString("nama_barang"),
                    rs.getInt("jumlah"),
                    rs.getDouble("subtotal"),
                    rs.getDouble("harga_beli"),
                    rs.getDouble("keuntungan")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal ambil detail penjualan: " + e.getMessage());
        }
        return list;
    }
}
