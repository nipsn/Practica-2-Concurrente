package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.*;

public class Almazon {

    public static BlockingQueue<Pedido> pedidos;
    public static BlockingQueue<Pedido> pedidosErroneos;
    public static Playa[] todasPlayas;
    public static BlockingQueue<Pedido> cinta;

    private static Object lockAdminRecogep;
    private static Object lockLimpiezaEmpaquetaP;

    private final int N_ADMINISTRATIVOS = 1;
    private final int N_RECOGEPEDIDOS = 1;
    private final int N_EMPAQUETAPEDIDOS = 1;
    private final int N_LIMPIEZA = 1;

    public static final int T_ADMINISTRATIVO = 1;
    public static final int T_RECOGEPEDIDOS = 2;
    public static final int T_EMPAQUETAPEDIDOS = 3;
    public static final int T_LIMPIEZA = 4;
    public static final int T_ENCARGADO = 5;

    public static void main(String[] args) throws InterruptedException {
        pedidos = new LinkedBlockingQueue<>();
        pedidosErroneos = new LinkedBlockingQueue<>();
        cinta = new LinkedBlockingQueue<>();
        todasPlayas = new Playa[2];// no 2, sino la constante asignada
        todasPlayas[0] = new Playa();
        todasPlayas[1] = new Playa();
//        canal1 = new Exchanger<>();
        lockAdminRecogep = new Object();
        lockLimpiezaEmpaquetaP = new Object();

        new Almazon().exec();
    }

    public void exec() throws InterruptedException {
        ArrayList<Cliente> clientela = new ArrayList<>();
        ArrayList<Personal> personal = new ArrayList<>();
        clientela.add(new Cliente());

        for(int i = 0;i < N_ADMINISTRATIVOS;i++){
            personal.add(new Personal(T_ADMINISTRATIVO, lockAdminRecogep));
        }
        for(int i = 0;i < N_RECOGEPEDIDOS;i++){
            personal.add(new Personal(T_RECOGEPEDIDOS, lockAdminRecogep));
        }
        for(int i = 0;i < N_EMPAQUETAPEDIDOS;i++){
            personal.add(new Personal(T_EMPAQUETAPEDIDOS, lockLimpiezaEmpaquetaP));
        }
        for(int i = 0;i < N_LIMPIEZA;i++){
            personal.add(new Personal(T_LIMPIEZA, lockLimpiezaEmpaquetaP));
        }

//        for(int i = 0;i < 4;i++){
//            personal.add(new Personal(i+1, canal1));
//        }
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
