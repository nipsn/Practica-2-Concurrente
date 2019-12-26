package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class Paquete {
    private ArrayList<Integer> listaProductos;
    private boolean sello;

    public Paquete(ArrayList<Integer> productos){
        listaProductos = new ArrayList<>(20); // hay que hacer una constante
    }

    public ArrayList<Integer> getListaProductos() {
        return listaProductos;
    }

    public void setListaProductos(ArrayList<Integer> listaProductos) {
        this.listaProductos = listaProductos;
    }

    public boolean isSellado() {
        return sello;
    }

    public void setSello(boolean sello) {
        this.sello = sello;
    }
}
