package com.practica2;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Almazon {

    public static BlockingQueue<Pedido> pedidos;
    public static AtomicBoolean hayPedido;
    public static Playa[] todasPlayas = new Playa[2];
    public static BlockingQueue<Pedido> cinta;



    public static void main(){
        pedidos = new ArrayBlockingQueue<Pedido>(Integer.MAX_VALUE);
    }
}
