package com.practica2;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Pedido{
    private int id;
    private boolean pagado;
    private boolean recogido;
    private ArrayList<Integer> listaProductos;

    public Pedido(ArrayList<Integer> productos, boolean pagado){
        id = Almazon.numPedidos.getAndIncrement();
        this.pagado = pagado;
        listaProductos = productos;
        recogido = false;
    }
    public Pedido(){
        id=-1;
        pagado=false;
        listaProductos=new ArrayList<>();
    }
    public Pedido(ArrayList<Integer> productos, int id){
        this.id = id;
        pagado = false; // no me importa
        listaProductos = productos;
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

    public boolean isRecogido() {
        return recogido;
    }
}
