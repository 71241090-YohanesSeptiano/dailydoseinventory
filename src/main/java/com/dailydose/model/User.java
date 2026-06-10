package com.dailydose.model;

/**
 * CLASS USER
 * Konsep OOP: Encapsulation — semua atribut private, akses via getter/setter.
 * Merepresentasikan pengguna sistem (Admin atau Staf).
 */
public class User {
    private int idUser;
    private String username;
    private String password;
    private String role; // "Admin, staf atau owner"

    /** Constructor untuk membuat User baru (belum ada ID, biar DB yang generate) */
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    /** Constructor lengkap untuk data yang sudah ada di database */
    public User(int idUser, String username, String password, String role) {
        this.idUser   = idUser;
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    //  GETTER & SETTER (Encapsulation)
    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    /** Cek apakah user ini adalah Admin */
    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(this.role);
    }

    @Override
    public String toString() {
        return "User{id=" + idUser + ", username=" + username + ", role=" + role + "}";
    }
}
