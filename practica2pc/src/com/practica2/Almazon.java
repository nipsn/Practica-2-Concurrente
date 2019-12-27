package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Almazon {

    public static BlockingQueue<Pedido> pedidos;
    public static BlockingQueue<Pedido> pedidosErroneos;
    public static Playa[] todasPlayas;
    public static BlockingQueue<Pedido> cinta;

    private final int N_ADMINISTRATIVOS = 1;
    private final int N_RECOGEPEDIDOS = 1;
    private final int N_EMPAQUETAPEDIDOS = 1;
    private final int N_LIMPIEZA = 1;



    public static void main(String[] args) throws InterruptedException {
        pedidos = new LinkedBlockingQueue<>();
        pedidosErroneos = new LinkedBlockingQueue<>();
        cinta = new LinkedBlockingQueue<>();
        todasPlayas = new Playa[2];// no 2, sino la constante asignada
        todasPlayas[0] = new Playa();
        todasPlayas[1] = new Playa();

        new Almazon().exec();
    }

    public void exec() throws InterruptedException {
        ArrayList<Cliente> clientela = new ArrayList<>();
        ArrayList<Personal> personal = new ArrayList<>();
        clientela.add(new Cliente());

        for(int i = 0;i < 4;i++){
            personal.add(new Personal(i+1));
        }
        for(Personal p : personal){
            new Thread(() -> {
                try {
                    p.tarea();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        for(Cliente c : clientela){
            new Thread(() -> {
                try {
                    c.comprar();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
