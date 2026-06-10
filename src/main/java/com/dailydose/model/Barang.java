package com.dailydose.model;

/**
 * CLASS BARANG (Abstract)
 * Konsep OOP:
 *   - Encapsulation : atribut private, akses via getter/setter
 *   - Abstraction   : class ini tidak bisa di-instantiate langsung,
 *                     harus pakai subclass (BarangPecahBelah / BarangKonsumsi)
 *   - Inheritance   : subclass mewarisi semua atribut & method di sini
 */
public abstract class Barang {
    private String idBarang;
    private String namaBarang;
    private String kategori;   // "PecahBelah" atau "Konsumsi"
    private double harga;      // harga jual ke pelanggan
    private double hargaBeli;  // modal / harga beli dari supplier
    private int stok;       // Read-Only dari UI, hanya berubah via transaksi

    public Barang(String idBarang, String namaBarang, String kategori, double harga, double hargaBeli, int stok) {
        this.idBarang   = idBarang;
        this.namaBarang = namaBarang;
        this.kategori   = kategori;
        this.harga      = harga;
        this.hargaBeli  = hargaBeli;
        this.stok       = stok;
    }

    /**
     * Menambah stok ketika ada transaksi PEMBELIAN (restock dari supplier).
     * Dipanggil oleh DetailPembelian, bukan diisi manual.
     * @param jumlah harus > 0
     * @throws IllegalArgumentException jika jumlah <= 0
     */
    public void tambahStok(int jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah tambah stok harus lebih dari 0!");
        }
        this.stok += jumlah;
    }

    /**
     * Mengurangi stok ketika ada transaksi PENJUALAN.
     * Dipanggil oleh DetailPenjualan.
     * @param jumlah harus > 0 dan tidak boleh melebihi stok saat ini
     * @throws IllegalArgumentException jika stok tidak mencukupi
     */
    public void kurangiStok(int jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah kurang stok harus lebih dari 0!");
        }
        if (jumlah > this.stok) {
            // ← Ini adalah EXCEPTION HANDLING sesuai fitur no.8 di proposal
            throw new IllegalArgumentException(
                "Stok tidak mencukupi! Stok tersedia: " + this.stok + ", diminta: " + jumlah
            );
        }
        this.stok -= jumlah;
    }

    /**
     * Setiap subclass wajib mengimplementasikan ini untuk
     * menampilkan info spesifik kategorinya.
     */
    public abstract String getInfoKategori();

    public String getIdBarang() {
        return idBarang;
    }
    public void setIdBarang(String idBarang) {
        this.idBarang = idBarang;
    }

    public String getNamaBarang() {
        return namaBarang;
    }
    public void setNamaBarang(String namaBarang) {
        this.namaBarang = namaBarang;
    }

    public String getKategori() {
        return kategori;
    }
    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public double getHarga() {
        return harga;
    }
    public void setHarga(double harga) {
        this.harga = harga;
    }

    public double getHargaBeli() {
        return hargaBeli;
    }
    public void setHargaBeli(double hargaBeli) {
        this.hargaBeli = hargaBeli;
    }

    public int getStok() {
        return stok;
    }
    public void setStok(int stok) {
        this.stok = stok;
    }

    @Override
    public String toString() {
        return "[" + idBarang + "] " + namaBarang + " | Harga: " + harga + " | Stok: " + stok;
    }
}
