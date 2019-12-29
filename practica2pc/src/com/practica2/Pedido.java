package com.practica2;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Pedido{
    private int id;
    private boolean pagado;
    private boolean recogido;
    private ArrayList<Integer> listaProductos;
    private ArrayList<Integer> notaOriginal;

    public Pedido(ArrayList<Integer> productos, boolean pagado){
        id = Almazon.numPedidos.getAndIncrement();
        this.pagado = pagado;
        listaProductos = productos;
        notaOriginal = productos;
        recogido = false;
    }
    public Pedido(){
        id=-1;
        pagado=false;
        listaProductos=new ArrayList<>();
    }
    public Pedido(ArrayList<Integer> productos, ArrayList<Integer> notaOriginal, int id){
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

    public ArrayList<Integer> getListaProductos() {
        return listaProductos;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<Integer> getNotaOriginal() {
        return notaOriginal;
    }

    public boolean isRecogido() {
        return recogido;
    }
}
