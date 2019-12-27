package com.practica2;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Pedido{
    private static int numPedidos = 0;
    private int id;
    private boolean pagado;
    private ArrayList<Integer> listaProductos;

    public Pedido(ArrayList<Integer> productos, boolean pagado){
        id = numPedidos;
        numPedidos++;
        this.pagado = pagado;
        listaProductos = productos;
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
}
