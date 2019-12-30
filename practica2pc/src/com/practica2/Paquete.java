package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Paquete {
    private CopyOnWriteArrayList<Integer> listaProductos;
    private boolean sello;

    public Paquete(CopyOnWriteArrayList<Integer> productos){
        listaProductos = productos;
    }

    public CopyOnWriteArrayList<Integer> getListaProductos() {
        return listaProductos;
    }

    public void setListaProductos(CopyOnWriteArrayList<Integer> listaProductos) {
        this.listaProductos = listaProductos;
    }

    public boolean isSellado() {
        return sello;
    }

    public void setSello(boolean sello) {
        this.sello = sello;
    }
}
