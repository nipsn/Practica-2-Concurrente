package com.practica2;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Almazon {

    public static BlockingQueue<Pedido> pedidos;
    public static BlockingQueue<Pedido> pedidosErroneos;
    public static Playa[] todasPlayas;
    public static BlockingQueue<Pedido> cinta;



    public static void main(){
        pedidos = new LinkedBlockingQueue<>();
        pedidosErroneos = new LinkedBlockingQueue<>();
        cinta = new LinkedBlockingQueue<>();
        todasPlayas = new Playa[2]; // no 2, sino la constante asignada
    }
}
