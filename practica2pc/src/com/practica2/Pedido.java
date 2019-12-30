package com.practica2;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Pedido{
    private int id;
    private boolean pagado;
    private boolean recogido;
    private CopyOnWriteArrayList<Integer> listaProductos;
    private CopyOnWriteArrayList<Integer> notaOriginal;

    public Pedido(CopyOnWriteArrayList<Integer> productos, boolean pagado){
        id = Almazon.numPedidos.getAndIncrement();
        this.pagado = pagado;
        listaProductos = productos;
        notaOriginal = productos;
        recogido = false;
    }
    public Pedido(){
        id=-1;
        pagado=false;
        listaProductos=new CopyOnWriteArrayList<>();
    }
    public Pedido(CopyOnWriteArrayList<Integer> productos, CopyOnWriteArrayList<Integer> notaOriginal, int id){
        this.id = id;
        pagado = false; // no me importa
        listaProductos = productos;
        this.notaOriginal = notaOriginal;
    }

    public boolean isPagado() {
        return pagado;
    }

    public int getId() {
        return id;
    }

    public CopyOnWriteArrayList<Integer> getListaProductos() {
        return listaProductos;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setListaProductos(CopyOnWriteArrayList<Integer> listaProductos) {
        this.listaProductos = listaProductos;
    }

    public CopyOnWriteArrayList<Integer> getNotaOriginal() {
        return notaOriginal;
    }

    public boolean isRecogido() {
        return recogido;
    }
}
