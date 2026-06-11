package com.dailydose.dao;

import com.dailydose.model.User;
import com.dailydose.util.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * USER DAO (Data Access Object)
 * Konsep Design Pattern: DAO — memisahkan logika database dari Model & Controller.
 * Semua query SQL untuk tabel 'users' ada di sini.
 *
 * Controller tidak boleh nulis SQL sendiri — harus lewat DAO ini!
 */
public class UserDAO {
    private final Connection conn;

    public UserDAO() {
        // Ambil koneksi dari Singleton DatabaseHelper
        this.conn = DatabaseHelper.getInstance().getConnection();
    }

    // CREATE — Tambah user baru
    public boolean tambahUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal tambah user: " + e.getMessage());
            return false;
        }
    }

    // READ — Ambil semua user
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal ambil semua user: " + e.getMessage());
        }
        return list;
    }

    // LOGIN — Cari user berdasarkan username & password
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal login: " + e.getMessage());
        }
        // Return null jika tidak ditemukan trus Controller menampilkan pesan error
        return null;
    }

    // UPDATE — Edit data user
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username=?, password=?, role=? WHERE id_user=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.setInt(4, user.getIdUser());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal update user: " + e.getMessage());
            return false;
        }
    }

    // DELETE — Hapus user
    public boolean deleteUser(int idUser) {
        String sql = "DELETE FROM users WHERE id_user = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Gagal hapus user: " + e.getMessage());
            return false;
        }
    }

    // HELPER — Konversi ResultSet ke User object
    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id_user"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("role")
        );
    }
}
