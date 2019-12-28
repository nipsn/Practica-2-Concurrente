package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Almazon {

    public static BlockingQueue<Pedido> pedidos;
    public static BlockingQueue<Pedido> pedidosRecogidos;
    public static BlockingQueue<Pedido> pedidosErroneos;
    public static BlockingQueue<Paquete> cinta;
    public static BlockingQueue<Pedido> pedidosEnviados;
    public static Playa[] todasPlayas;

    private static Object lockAdminRecogep;
    private static Object lockLimpiezaEmpaquetaP;

    private final int N_CLIENTES = 3;
    private final int N_ADMINISTRATIVOS = 2;
    private final int N_RECOGEPEDIDOS = 3;
    private final int N_EMPAQUETAPEDIDOS = 3;
    private final int N_LIMPIEZA = 2;
    private final int N_ENCARGADOS = 1;

    public static final int NUM_PLAYAS = 3;

    public static final int T_ADMINISTRATIVO = 1;
    public static final int T_RECOGEPEDIDOS = 2;
    public static final int T_EMPAQUETAPEDIDOS = 3;
    public static final int T_LIMPIEZA = 4;
    public static final int T_ENCARGADO = 5;

    public static AtomicInteger numPedidos;
    public static AtomicInteger cuentaEnviados;

    public static void main(String[] args) throws InterruptedException {
        pedidos = new LinkedBlockingQueue<>();
        pedidosRecogidos = new LinkedBlockingQueue<>();
        pedidosErroneos = new LinkedBlockingQueue<>();
        cinta = new LinkedBlockingQueue<>();
        pedidosEnviados = new LinkedBlockingQueue<>();

        todasPlayas = new Playa[NUM_PLAYAS];// todo numero de playas dinamico

        for(int i = 0;i < NUM_PLAYAS;i++)
            todasPlayas[i] = new Playa();

        lockAdminRecogep = new Object();
        lockLimpiezaEmpaquetaP = new Object();

        numPedidos = new AtomicInteger();
        cuentaEnviados = new AtomicInteger();

        new Almazon().exec();
    }

    public void exec() throws InterruptedException {
        ArrayList<Cliente> clientela = new ArrayList<>();
        ArrayList<Personal> personal = new ArrayList<>();

        for(int i = 0;i < N_CLIENTES;i++)
            clientela.add(new Cliente());

        for(int i = 0;i < N_ADMINISTRATIVOS;i++)
            personal.add(new Personal(T_ADMINISTRATIVO, lockAdminRecogep));

        for(int i = 0;i < N_RECOGEPEDIDOS;i++)
            personal.add(new Personal(T_RECOGEPEDIDOS, lockAdminRecogep));

        for(int i = 0;i < N_EMPAQUETAPEDIDOS;i++)
            personal.add(new Personal(T_EMPAQUETAPEDIDOS, lockLimpiezaEmpaquetaP));

        for(int i = 0;i < N_LIMPIEZA;i++)
            personal.add(new Personal(T_LIMPIEZA, lockLimpiezaEmpaquetaP));


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
