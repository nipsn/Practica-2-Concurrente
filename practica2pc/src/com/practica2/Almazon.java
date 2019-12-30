package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Almazon {

    public static final int nMinutosSon24HorasReales = 12 * 3600;
    public static final int NUM_TURNOS = 3;
    public static int segundoConvertido;

    public static ArrayList<Cliente> clientela;
    public static ArrayList<Personal> personal;

    public static BlockingQueue<Pedido> pedidos;
    public static BlockingQueue<Pedido> pedidosRecogidos;
    public static BlockingQueue<Pedido> pedidosErroneos;
    public static BlockingQueue<Paquete> cinta;
    public static BlockingQueue<Pedido> pedidosEnviados;
    public static Playa[] todasPlayas;

    private static Object lockAdminRecogep;
    private static Object lockLimpiezaEmpaquetaP;
    private static Object lockLimpiezaEmpaquetaPVuelta;

    private final int N_CLIENTES = 6;
    private final int N_ADMINISTRATIVOS = 4;
    private final int N_RECOGEPEDIDOS = 8;
    private final int N_EMPAQUETAPEDIDOS = 6;
    private final int N_LIMPIEZA = 4;
    private final int N_ENCARGADOS = 2;
    // todo: no tiene sentido poner mas de un encargado por turno (poner en la memoria)

    public static final int NUM_PLAYAS = 50;

    public static final int T_ADMINISTRATIVO = 1;
    public static final int T_RECOGEPEDIDOS = 2;
    public static final int T_EMPAQUETAPEDIDOS = 3;
    public static final int T_LIMPIEZA = 4;
    public static final int T_ENCARGADO = 5;

    public static AtomicInteger numPedidos;
    public static AtomicInteger cuentaEnviados;
    public static int turnoActual;
    public static void main(String[] args) throws InterruptedException {

        segundoConvertido = nMinutosSon24HorasReales * 60 / 86400;

        pedidos = new LinkedBlockingQueue<>();
        pedidosRecogidos = new LinkedBlockingQueue<>();
        pedidosErroneos = new LinkedBlockingQueue<>();
        cinta = new LinkedBlockingQueue<>();
        pedidosEnviados = new LinkedBlockingQueue<>();

        todasPlayas = new Playa[NUM_PLAYAS];
        for(int i = 0;i < NUM_PLAYAS;i++)
            todasPlayas[i] = new Playa();

        lockAdminRecogep = new Object();
        lockLimpiezaEmpaquetaP = new Object();
        lockLimpiezaEmpaquetaPVuelta = new Object();

        numPedidos = new AtomicInteger();
        cuentaEnviados = new AtomicInteger();

        turnoActual = 0;
        new Almazon().exec();
    }

    public void exec() throws InterruptedException {
        clientela = new ArrayList<>();
        personal = new ArrayList<>();

        for(int i = 0;i < N_CLIENTES;i++)
            clientela.add(new Cliente());

        for(int i = 0;i < N_ADMINISTRATIVOS * (NUM_TURNOS - 1);i++) {
            if(i % (NUM_TURNOS - 1) == 0) {
                personal.add(new Personal(T_ADMINISTRATIVO, 0 , lockAdminRecogep, new Object()));
            } else {
                personal.add(new Personal(T_ADMINISTRATIVO, 1 , lockAdminRecogep, new Object()));
            }
        }
        for(int i = 0;i < N_RECOGEPEDIDOS;i++) {
            if(i % (NUM_TURNOS - 1) == 0) {
                personal.add(new Personal(T_RECOGEPEDIDOS,0, lockAdminRecogep, new Object()));
            } else {
                personal.add(new Personal(T_RECOGEPEDIDOS,1, lockAdminRecogep, new Object()));
            }
        }
        for(int i = 0;i < N_EMPAQUETAPEDIDOS;i++) {
            if(i % (NUM_TURNOS - 1) == 0) {
                personal.add(new Personal(T_EMPAQUETAPEDIDOS, 0,lockLimpiezaEmpaquetaP, lockLimpiezaEmpaquetaPVuelta));
            } else {
                personal.add(new Personal(T_EMPAQUETAPEDIDOS, 1,lockLimpiezaEmpaquetaP, lockLimpiezaEmpaquetaPVuelta));
            }
        }
        for(int i = 0;i < N_LIMPIEZA;i++) {
            if(i % (NUM_TURNOS - 1) == 0){
                personal.add(new Personal(T_LIMPIEZA, 0, lockLimpiezaEmpaquetaP, lockLimpiezaEmpaquetaPVuelta));
            } else {
                personal.add(new Personal(T_LIMPIEZA, 1, lockLimpiezaEmpaquetaP, lockLimpiezaEmpaquetaPVuelta));
            }
        }
        for(int i = 0;i < N_ENCARGADOS;i++) {
            if(i % (NUM_TURNOS - 1) == 0){
                personal.add(new Personal(T_ENCARGADO, 0, new Object(), new Object()));
            } else {
                personal.add(new Personal(T_ENCARGADO, 1, new Object(), new Object()));
            }
        }

        for(Personal p : personal){
            new Thread(() -> {
                    p.tarea();
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
