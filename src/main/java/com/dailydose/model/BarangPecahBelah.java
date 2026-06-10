package com.dailydose.model;

/**
 * CLASS BARANG PECAH BELAH
 * Konsep OOP: Inheritance — mewarisi semua atribut & method dari Barang.
 * Menambahkan atribut spesifik: material (misal: "Kaca", "Keramik")
 */
public class BarangPecahBelah extends Barang {
    private String material;

    public BarangPecahBelah(String idBarang, String namaBarang, double harga, double hargaBeli, int stok, String material) {
        super(idBarang, namaBarang, "PecahBelah", harga, hargaBeli, stok);
        this.material = material;
    }

    @Override
    public String getInfoKategori() {
        return "Kategori: Barang Pecah Belah | Material: " + material;
    }

    public String getMaterial() {
        return material;
    }
    public void setMaterial(String material) {
        this.material = material;
    }
}
