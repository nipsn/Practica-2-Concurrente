package com.practica2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Personal {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ESPACIO = " ";

    private int tipo;

    private final int POS_ERROR = 2; // 1 de cada 100
    private final int POS_PEDIDO_ROTO = 20; // el 5%

    public static AtomicInteger playaALimpiar;
    public static AtomicBoolean hayPedidoNuevo;
    public static AtomicBoolean limpiar;

    private static ReentrantLock mutexPlayas = new ReentrantLock();
    private static ReentrantLock mutexPedidos = new ReentrantLock();
    private static ReentrantLock mutexLimpieza = new ReentrantLock();
    private static ReentrantLock mutexPedidosEnviados = new ReentrantLock();
    private static ReentrantLock mutexPedidosErroneos = new ReentrantLock();
    private static ReentrantLock mutexNotificacionLimpieza = new ReentrantLock();


    private final Object canalComunicacion;
    private final Object canalComunicacion2;

//    // Constructor general
//    public Personal(int tipo, Object comunicador) {
//        this.tipo = tipo;
//
//        hayPedidoNuevo = new AtomicBoolean();
//        limpiar = new AtomicBoolean();
//        playaALimpiar = new AtomicInteger(-1);
//
//        canalComunicacion = comunicador;
//        canalComunicacion2 = new Object();
//        // si se inicializa con un solo object no me importa cual sea comunicador2
//    }
    public Personal(int tipo, Object comunicador, Object comunicador2) {
        this.tipo = tipo;

        hayPedidoNuevo = new AtomicBoolean();
        limpiar = new AtomicBoolean();
        playaALimpiar = new AtomicInteger(-1);

        canalComunicacion = comunicador;
        canalComunicacion2 = comunicador2;
    }


    public void tarea() throws InterruptedException {
        if (this.tipo == Almazon.T_ADMINISTRATIVO) {
            trabajoAdministrativo();
        } else if (this.tipo == Almazon.T_RECOGEPEDIDOS) {
            trabajoRecogePedidos();
        } else if (this.tipo == Almazon.T_EMPAQUETAPEDIDOS) {
            trabajoEmpaquetaPedidos();
        } else if (this.tipo == Almazon.T_LIMPIEZA) {
            trabajoLimpieza();
        } else if (this.tipo == Almazon.T_ENCARGADO) {
            //trabajoEncargado();
        }
    }

    public void trabajoAdministrativo() throws InterruptedException {
        while (true) {
            Pedido p = Almazon.pedidos.peek();
            if (p != null && p.isPagado()) {
                synchronized (canalComunicacion){
                    hayPedidoNuevo.set(true);
                    canalComunicacion.notify();
                }
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + Thread.currentThread().getId() + " PEDIDO CORRECTO" + ESPACIO + ANSI_RESET);
            } else {
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + Thread.currentThread().getId() + " PEDIDO INCORRECTO O NO HAY PEDIDO" + ESPACIO + ANSI_RESET);
            }
            Thread.sleep(1000);

            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
            mutexPedidosEnviados.lock();
            if(!Almazon.pedidosEnviados.isEmpty()){
                Pedido e = Almazon.pedidosEnviados.poll();
                if(mutexPedidosEnviados.isHeldByCurrentThread())
                    mutexPedidosEnviados.unlock();
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + Thread.currentThread().getId() + " ENVIA CORREO DEL PEDIDO " + e.getId() + ESPACIO + ANSI_RESET);
            }else{
                if(mutexPedidosEnviados.isHeldByCurrentThread())
                    mutexPedidosEnviados.unlock();
            }
        }
    }


    private Pedido tratarPedidoErroneo(Pedido mal){
        int i = 0;
        int num = (int) (Math.random() * POS_ERROR);
        if (num % POS_ERROR == 0) {
        //hay error pero da igual donde
            return mal;
        }
        for(Integer aux : mal.getListaProductos()){
            if(!mal.getListaProductos().get(i).equals(mal.getNotaOriginal().get(i))){
                aux = mal.getNotaOriginal().get(i);
            }
            i++;
        }
        return mal;
    }
    private Pedido recogerPedidos(Pedido inicial){
        ArrayList<Integer> carrito = new ArrayList<>();
        int i = 0;
        int num;
        for (Integer producto : inicial.getListaProductos()) {

            num = (int) (Math.random() * POS_ERROR);
            if (num % POS_ERROR == 0) {
                // hay error
                carrito.add(inicial.getListaProductos().get(i) + 1);
            } else {
                // no hay error
                carrito.add(inicial.getListaProductos().get(i));
            }
            i++;
            try {
                Thread.sleep((long) (Math.random() * 2) * 1000); //de 0 a 2 segundos a dormir
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return new Pedido(carrito, inicial.getNotaOriginal(), inicial.getId());
    }

    public void trabajoRecogePedidos(){
        while (true) {
            Pedido nuevo;
            mutexPedidosErroneos.lock();
            if (!Almazon.pedidosErroneos.isEmpty()) {
                nuevo = tratarPedidoErroneo(Objects.requireNonNull(Almazon.pedidosErroneos.poll()));
                if(mutexPedidosErroneos.isHeldByCurrentThread())
                    mutexPedidosErroneos.unlock();

                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + Thread.currentThread().getId() + " TRATANDO PEDIDO ERRONEO" + ESPACIO + ANSI_RESET);

                int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);
                // si la playa esta sucia me bloqueo
                while(Almazon.todasPlayas[miPlaya].isSucia());

                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + Thread.currentThread().getId() + " PONE PEDIDO EN PLAYA" + ESPACIO + ANSI_RESET);
                Almazon.todasPlayas[miPlaya].add(nuevo);

            } else {
                if(mutexPedidosErroneos.isHeldByCurrentThread())
                    mutexPedidosErroneos.unlock();

                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + Thread.currentThread().getId() + " ESPERA" + ESPACIO + ANSI_RESET);

                synchronized (canalComunicacion){
                    try {
                        canalComunicacion.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Pedido p;

                mutexPedidos.lock();
                if(!Almazon.pedidos.isEmpty()){
                    p = Almazon.pedidos.poll();
                    if(mutexPedidos.isHeldByCurrentThread())
                        mutexPedidos.unlock();

                    assert p != null;
                    Almazon.pedidosRecogidos.offer(p);

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + Thread.currentThread().getId() + " TRATA PEDIDO NUEVO" + ESPACIO + ANSI_RESET);

                    nuevo = recogerPedidos(p);
                    int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);

                    // si la playa esta sucia me bloqueo
                    while(Almazon.todasPlayas[miPlaya].isSucia());

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + Thread.currentThread().getId() + " PONE PEDIDO EN PLAYA" + ESPACIO + ANSI_RESET);
                    Almazon.todasPlayas[miPlaya].add(nuevo);

                } else {
                    if (mutexPedidos.isHeldByCurrentThread())
                        mutexPedidos.unlock();
                }
            }

        }
    }

    public boolean comprobarPedido(Pedido p) {
        Pedido encontrado = new Pedido();
        for (Pedido aux : Almazon.pedidosRecogidos) {
            if (aux.getId() == p.getId()) {
                encontrado = aux;
            }
        }

        return encontrado.getListaProductos().equals(p.getListaProductos());
    }

    public void trabajoEmpaquetaPedidos() {
        while (true) {
            int playaElegida = (int) (Math.random() * Almazon.NUM_PLAYAS);
            mutexPlayas.lock();
            if(!Almazon.todasPlayas[playaElegida].isEmpty()) {
                if (!Almazon.todasPlayas[playaElegida].isSucia()) {
                    Pedido p = Almazon.todasPlayas[playaElegida].poll();
                    if(mutexPlayas.isHeldByCurrentThread())
                        mutexPlayas.unlock();
                    int num = (int) (Math.random() * POS_PEDIDO_ROTO);
                    boolean llamoLimpieza = (Almazon.cuentaEnviados.get()+1) % 10 == 0;
                    if(!llamoLimpieza) playaALimpiar.set(playaElegida);


                    if (num % POS_PEDIDO_ROTO == 0 || llamoLimpieza) {
                        System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + Thread.currentThread().getId() + " DETECTA PLAYA SUCIA " + playaALimpiar.get() + ESPACIO + ANSI_RESET);
                        Almazon.todasPlayas[playaElegida].setSucia(true);

                        synchronized (canalComunicacion) {
                            // llama y se espera hasta que limpien
                            mutexNotificacionLimpieza.lock();
                            limpiar.set(true);
                            canalComunicacion.notify();
                            if(mutexNotificacionLimpieza.isHeldByCurrentThread()) mutexNotificacionLimpieza.unlock();
                        }
                        synchronized (canalComunicacion2) {
                            try {
                                canalComunicacion2.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (comprobarPedido(p)) {
                        Paquete paq = new Paquete(p.getListaProductos());
                        paq.setSello(true);
                        Almazon.cinta.offer(paq);
                        Almazon.pedidosEnviados.offer(p);
                        Almazon.cuentaEnviados.getAndIncrement();
                        System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + Thread.currentThread().getId() + " ENVIA PEDIDO " + p.getId() + ESPACIO + ANSI_RESET);
                    } else {
                        System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + Thread.currentThread().getId() + " DETECTA ERROR EN EL PEDIDO " + p.getId() + " SE MANDA A REVISAR" + ESPACIO + ANSI_RESET);
                        Almazon.pedidosErroneos.offer(p);
                    }
                }
            }
            if(mutexPlayas.isHeldByCurrentThread())
                mutexPlayas.unlock();
        }
    }

    public void limpiarPlaya() {
        if (playaALimpiar.get()==-1) {
            for (int i = 0; i < Almazon.NUM_PLAYAS; i++) {
                Almazon.todasPlayas[i].setSucia(false);
            }
            System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO + ANSI_BLACK + "LIMPIEZA " + Thread.currentThread().getId() + " LIMPIANDO TODAS LAS PLAYAS" + ESPACIO + ANSI_RESET);
        } else {
            if (Almazon.todasPlayas[playaALimpiar.get()].isSucia()) {
                Almazon.todasPlayas[playaALimpiar.get()].setSucia(false);
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + Thread.currentThread().getId() + " LIMPIANDO PLAYA " + playaALimpiar.get() + ESPACIO + ANSI_RESET);
            }
        }
    }

    public void trabajoLimpieza() {
        while (true) {
            synchronized (canalComunicacion){
                try {
                    canalComunicacion.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + Thread.currentThread().getId() + " HA DESPERTADO " + ESPACIO + ANSI_RESET);

            if(limpiar.get()) {
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + Thread.currentThread().getId() + " VA A LIMPIAR UNA PLAYA " + ESPACIO + ANSI_RESET);
                limpiarPlaya();
                playaALimpiar.set(-1);
                limpiar.set(false);
            }


            synchronized (canalComunicacion2){
                canalComunicacion2.notifyAll();
            }
        }
    }

    public void trabajoEncargado(){

    }

}
