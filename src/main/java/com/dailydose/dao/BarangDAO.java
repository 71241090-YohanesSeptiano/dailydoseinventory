package com.dailydose.dao;

import com.dailydose.model.*;
import com.dailydose.util.DatabaseHelper;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BARANG DAO
 * Menangani semua operasi database untuk tabel 'barang'.
 * Karena Barang adalah abstract class dengan 2 subclass,
 * DAO ini harus menangani keduanya.
 */
public class BarangDAO {

    private final Connection conn;

    public BarangDAO() {
        this.conn = DatabaseHelper.getInstance().getConnection();
    }

    // =========================================================
    //  CREATE
    // =========================================================
    public boolean tambahBarang(Barang barang) {
        String sql = """
            INSERT INTO barang (id_barang, nama_barang, kategori, harga, harga_beli, stok, material, tgl_kadaluarsa)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barang.getIdBarang());
            ps.setString(2, barang.getNamaBarang());
            ps.setString(3, barang.getKategori());
            ps.setDouble(4, barang.getHarga());
            ps.setDouble(5, barang.getHargaBeli());
            ps.setInt   (6, barang.getStok());

            // Isi kolom spesifik subclass
            if (barang instanceof BarangPecahBelah bp) {
                ps.setString(7, bp.getMaterial());
                ps.setNull  (8, Types.VARCHAR);
            } else if (barang instanceof BarangKonsumsi bk) {
                ps.setNull  (7, Types.VARCHAR);
                ps.setString(8, bk.getTanggalKadaluarsa().toString());
            }
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal tambah barang: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    //  READ — Semua barang
    // =========================================================
    public List<Barang> getAllBarang() {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang ORDER BY nama_barang";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToBarang(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal ambil semua barang: " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    //  READ — Cari berdasarkan nama (LIKE)
    // =========================================================
    public List<Barang> cariBarang(String keyword) {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang WHERE nama_barang LIKE ? OR id_barang LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToBarang(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal cari barang: " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    //  READ — Ambil satu barang berdasarkan ID
    // =========================================================
    public Barang getBarangById(String idBarang) {
        String sql = "SELECT * FROM barang WHERE id_barang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idBarang);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToBarang(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal ambil barang by ID: " + e.getMessage());
        }
        return null;
    }

    // =========================================================
    //  UPDATE — Edit nama & harga (stok TIDAK boleh diubah langsung)
    // =========================================================
    public boolean updateBarang(Barang barang) {
        String sql = "UPDATE barang SET nama_barang=?, harga=?, harga_beli=?, material=?, tgl_kadaluarsa=? WHERE id_barang=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barang.getNamaBarang());
            ps.setDouble(2, barang.getHarga());
            ps.setDouble(3, barang.getHargaBeli());

            if (barang instanceof BarangPecahBelah bp) {
                ps.setString(4, bp.getMaterial());
                ps.setNull  (5, Types.VARCHAR);
            } else if (barang instanceof BarangKonsumsi bk) {
                ps.setNull  (4, Types.VARCHAR);
                ps.setString(5, bk.getTanggalKadaluarsa().toString());
            }
            ps.setString(6, barang.getIdBarang());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal update barang: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    //  UPDATE STOK — Hanya dipanggil dari DAO Penjualan/Pembelian!
    //  Bukan dari UI langsung.
    // =========================================================
    public boolean updateStok(String idBarang, int stokBaru) {
        String sql = "UPDATE barang SET stok = ? WHERE id_barang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, stokBaru);
            ps.setString(2, idBarang);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal update stok: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    //  UPDATE MODAL — Dipanggil saat ada restock (Pembelian)
    // =========================================================
    public boolean updateHargaBeli(String idBarang, double hargaBeliBaru) {
        String sql = "UPDATE barang SET harga_beli = ? WHERE id_barang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, hargaBeliBaru);
            ps.setString(2, idBarang);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal update harga beli: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    //  DELETE
    // =========================================================
    public boolean deleteBarang(String idBarang) {
        String sql = "DELETE FROM barang WHERE id_barang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idBarang);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal hapus barang: " + e.getMessage());
            return false;
        }
    }

    // Id Otomatis
    public String generateNextIdBarang() {
        String sql = "SELECT id_barang FROM barang";
        int maxId = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id_barang");
                // Ambil angka dari ID yang ada
                String numberOnly = id.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty()) {
                    int num = Integer.parseInt(numberOnly);
                    if (num > maxId) {
                        maxId = num;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal generate ID: " + e.getMessage());
        }
        // Format Id baru, misal: B001, B002, B015
        return String.format("B%03d", maxId + 1);
    }

    // =========================================================
    //  HELPER — ResultSet → Barang (subclass yang sesuai)
    // =========================================================
    private Barang mapRowToBarang(ResultSet rs) throws SQLException {
        String kategori = rs.getString("kategori");
        String id       = rs.getString("id_barang");
        String nama     = rs.getString("nama_barang");
        double harga    = rs.getDouble("harga");
        double hargaBeli= rs.getDouble("harga_beli");
        int    stok     = rs.getInt("stok");

        if ("PecahBelah".equals(kategori)) {
            String material = rs.getString("material");
            return new BarangPecahBelah(id, nama, harga, hargaBeli, stok, material);
        } else {
            // Konsumsi
            String tglStr = rs.getString("tgl_kadaluarsa");
            LocalDate tgl = (tglStr != null) ? LocalDate.parse(tglStr) : LocalDate.now().plusYears(1);
            return new BarangKonsumsi(id, nama, harga, hargaBeli, stok, tgl);
        }
    }
}
