package com.dailydose.model;

import java.time.LocalDate;

/**
 * CLASS BARANG KONSUMSI
 * Konsep OOP: Inheritance — mewarisi semua atribut & method dari Barang.
 * Menambahkan atribut spesifik: tanggalKadaluarsa
 */
public class BarangKonsumsi extends Barang {
    private LocalDate tanggalKadaluarsa;

    public BarangKonsumsi(String idBarang, String namaBarang, double harga, double hargaBeli, int stok, LocalDate tanggalKadaluarsa) {
        super(idBarang, namaBarang, "Konsumsi", harga, hargaBeli, stok);
        this.tanggalKadaluarsa = tanggalKadaluarsa;
    }

    @Override
    public String getInfoKategori() {
        return "Kategori: Barang Konsumsi | Kadaluarsa: " + tanggalKadaluarsa;
    }

    public boolean isSudahKadaluarsa() {
        return LocalDate.now().isAfter(tanggalKadaluarsa);
    }

    public LocalDate getTanggalKadaluarsa() {
        return tanggalKadaluarsa;
    }

    public void setTanggalKadaluarsa(LocalDate tanggalKadaluarsa) {
        this.tanggalKadaluarsa = tanggalKadaluarsa;
    }
}
