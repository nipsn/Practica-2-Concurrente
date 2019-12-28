package com.practica2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Personal {
    private int tipo;

    private final int POS_ERROR = 100; // 1 de cada 100
    private final int POS_PEDIDO_ROTO = 20;// el 5%

    public static AtomicInteger playaALimpiar;
    public static AtomicBoolean hayPedidoNuevo;
    public static AtomicBoolean limpiar;

    private static ReentrantLock mutexPlayas = new ReentrantLock();
    private static ReentrantLock mutexPedidos = new ReentrantLock();
    private static ReentrantLock mutexPedidosEnviados = new ReentrantLock();
    private static ReentrantLock mutexPedidosErroneos = new ReentrantLock();
    private static ReentrantLock mutexNotificacionLimpieza = new ReentrantLock();


    private final Object canalComunicacion;

    // Constructor general
    public Personal(int tipo, Object comunicador) {
        this.tipo = tipo;

        hayPedidoNuevo = new AtomicBoolean();
        limpiar = new AtomicBoolean();
        playaALimpiar = new AtomicInteger(-1);

        canalComunicacion = comunicador;
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
            System.out.println("ADMINISTRATIVO COMPRUEBA PEDIDO");
            if (p != null && p.isPagado()) {
                synchronized (canalComunicacion){
                    hayPedidoNuevo.set(true);
                    canalComunicacion.notify();
                }
                System.out.println("ADMINISTRATIVO PEDIDO CORRECTO");
            }
            Thread.sleep(1000);

            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
            mutexPedidosEnviados.lock();
            if(!Almazon.pedidosEnviados.isEmpty()){
                Pedido e = Almazon.pedidosEnviados.poll();
                if(mutexPedidosEnviados.isHeldByCurrentThread())
                    mutexPedidosEnviados.unlock();
                System.out.println("    ADMINISTRATIVO ENVIA CORREO DEL PEDIDO " + e.getId());
            }else{
                if(mutexPedidosEnviados.isHeldByCurrentThread())
                    mutexPedidosEnviados.unlock();
            }
        }
    }

    private Pedido tratarPedido(Pedido inicial){
        ArrayList<Integer> carrito = new ArrayList<>();
        int i = 0;
        for (Integer producto : inicial.getListaProductos()) {
            int num;

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
        return new Pedido(carrito, inicial.getId());
    }

    public void trabajoRecogePedidos(){
        while (true) {
            Pedido nuevo;
            mutexPedidosErroneos.lock();
            if (!Almazon.pedidosErroneos.isEmpty()) {
                nuevo = tratarPedido(Objects.requireNonNull(Almazon.pedidosErroneos.poll()));
                if(mutexPedidosErroneos.isHeldByCurrentThread())
                    mutexPedidosErroneos.unlock();
                System.out.println("RECOGEPEDIDOS TRATANDO PEDIDO ERRONEO");
                int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);
                // si la playa esta sucia me bloqueo
                while(Almazon.todasPlayas[miPlaya].isSucia());

                System.out.println("RECOGEPEDIDOS PONE PEDIDO EN PLAYA");
                Almazon.todasPlayas[miPlaya].add(nuevo);
            } else {
                if(mutexPedidosErroneos.isHeldByCurrentThread())
                    mutexPedidosErroneos.unlock();
                System.out.println("RECOGEPEDIDOS ESPERA");
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
                    System.out.println("RECOGEPEDIDOS TRATA PEDIDO NUEVO");
                    nuevo = tratarPedido(p);
                    int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);
                    // si la playa esta sucia me bloqueo
                    while(Almazon.todasPlayas[miPlaya].isSucia());

                    System.out.println("RECOGEPEDIDOS PONE PEDIDO EN PLAYA");
                    Almazon.todasPlayas[miPlaya].add(nuevo);
                }
                else {
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
                System.out.println("EMPAQUETAPEDIDOS HA ENCONTRADO EL PEDIDO ORIGINAL");
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
                        System.out.println("EMPAQUETAPEDIDOS, PLAYA SUCIA " + playaALimpiar.get());
                        Almazon.todasPlayas[playaElegida].setSucia(true);
                        synchronized (canalComunicacion) {
                            // llama y se espera hasta que limpien
                            mutexNotificacionLimpieza.lock();

                            canalComunicacion.notify();

                            if(mutexNotificacionLimpieza.isHeldByCurrentThread())
                                mutexNotificacionLimpieza.unlock();
                            try {
                                canalComunicacion.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    System.out.println("EMPAQUETAPEDIDOS COMPROBANDO PEDIDO");
                    if (comprobarPedido(p)) {
                        Paquete paq = new Paquete(p.getListaProductos());
                        paq.setSello(true);
                        Almazon.cinta.offer(paq);
                        Almazon.pedidosEnviados.offer(p);
                        Almazon.cuentaEnviados.getAndIncrement();
                        System.out.println("EMPAQUETAPEDIDOS ENVIA PEDIDO " + p.getId());
                    } else {
                        System.out.println("EMPAQUETAPEDIDOS ERROR EN EL PEDIDO, SE MANDA A REVISAR");
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
            System.out.println("                    LIMPIEZA, LIMPIANDO TODAS LAS PLAYAS");
            for (int i = 0; i < Almazon.NUM_PLAYAS; i++) {
                Almazon.todasPlayas[i].setSucia(false);
            }
        } else {
            if (Almazon.todasPlayas[playaALimpiar.get()].isSucia()) {
                System.out.println("LIMPIEZA, LIMPIANDO PLAYA " + playaALimpiar.get());
                
                Almazon.todasPlayas[playaALimpiar.get()].setSucia(false);
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
            limpiarPlaya();
            playaALimpiar.set(-1);

            synchronized (canalComunicacion){
                canalComunicacion.notifyAll();
            }
        }
    }

    public void trabajoEncargado(){

    }

}
