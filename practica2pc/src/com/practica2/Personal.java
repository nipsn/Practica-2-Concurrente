package com.practica2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Personal {
    /*
     * ╔══════════════════════════════════════════════╗
     * ║              «COLORES TERMINAL»              ║
     * ╠══════════════════════════════════════════════╣
     */
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ESPACIO = " ";
    /*
     * ║                                              ║
     * ╚══════════════════════════════════════════════╝
     */
    /*
     * ╔══════════════════════════════════════════════╗
     * ║         «VARIABLES CONFIGURABLES»            ║
     * ╠══════════════════════════════════════════════╣
     */
        private final int POS_ERROR = 100; // 1 de cada 100
        private final int POS_PEDIDO_ROTO = 20; // el 5% (1 / 20)
    /*
     * ║                                              ║
     * ╚══════════════════════════════════════════════╝
     */

    public static AtomicInteger playaALimpiar;
    public static AtomicBoolean hayPedidoNuevo;
    public static AtomicBoolean limpiar;

    private static ReentrantLock mutexPlayas = new ReentrantLock();
    private static ReentrantLock mutexPedidos = new ReentrantLock();
    private static ReentrantLock mutexPedidosEnviados = new ReentrantLock();
    private static ReentrantLock mutexPedidosErroneos = new ReentrantLock();
    private static ReentrantLock mutexNotificacionLimpieza = new ReentrantLock();
    private static ReentrantLock mutexLimpiar = new ReentrantLock();

    private int tipo;
    private int turno;
    private boolean trabajo;
    private final Object canalComunicacion;
    private final Object canalComunicacion2;
    private long id;


    public Personal(int tipo, int turno, Object comunicador, Object comunicador2) {
        this.tipo = tipo;
        this.turno = turno;

        trabajo = turno == 0;

        hayPedidoNuevo = new AtomicBoolean();
        limpiar = new AtomicBoolean();
        playaALimpiar = new AtomicInteger(-1);

        canalComunicacion = comunicador;
        canalComunicacion2 = comunicador2;
    }


    public void tarea(){
        this.id = Thread.currentThread().getId();
        if (this.tipo == Almazon.T_ADMINISTRATIVO) {
            try {
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " EMPIEZA A TRABAJAR (SE DUERME)"  + ESPACIO + ANSI_RESET);
                Thread.sleep(3000);
                trabajoAdministrativo();
            }  catch (InterruptedException e){
                this.trabajo = this.turno == Almazon.turnoActual;
            }
        } else if (this.tipo == Almazon.T_RECOGEPEDIDOS) {
            try {
                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " EMPIEZA A TRABAJAR (SE DUERME)" + ESPACIO + ANSI_RESET);
                Thread.sleep(3000);
                trabajoRecogePedidos();
            }  catch (InterruptedException e){
                this.trabajo = this.turno == Almazon.turnoActual;
            }
        } else if (this.tipo == Almazon.T_EMPAQUETAPEDIDOS) {
            try {
                System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + id + " EMPIEZA A TRABAJAR (SE DUERME)"  + ESPACIO + ANSI_RESET);
                Thread.sleep(3000);
                trabajoEmpaquetaPedidos();
            }  catch (InterruptedException e){
                this.trabajo = this.turno == Almazon.turnoActual;
            }
        } else if (this.tipo == Almazon.T_LIMPIEZA) {
            try {
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + id + " EMPIEZA A TRABAJAR (SE DUERME) "  + ESPACIO + ANSI_RESET);
                Thread.sleep(3000);
                trabajoLimpieza();
            }  catch (InterruptedException e){
                this.trabajo = this.turno == Almazon.turnoActual;
            }
        } else if (this.tipo == Almazon.T_ENCARGADO) {
            try {
                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO " + Almazon.pedidos.size() + " EMPIEZA A TRABAJAR (SE DUERME)" + ESPACIO + ANSI_RESET);
                Thread.sleep(3000);
                trabajoEncargado();
            }  catch (InterruptedException e){
                try {
                    Thread.sleep(2 * Almazon.nSegundosSon24HorasReales / Almazon.NUM_TURNOS);
                } catch (InterruptedException ex) {
                    System.err.println("SE HA INTENTADO DESPERTAR A UN ENCARGADO DE SU SIESTA. ESO ESTA FEO");
                    ex.printStackTrace();
                }
            }
        }
    }

    public void trabajoAdministrativo() throws InterruptedException {
        while (true) {
            if (trabajo) {
                Pedido p = Almazon.pedidos.peek();

                if (p != null && p.isPagado()) {
                    synchronized (canalComunicacion) {
                        hayPedidoNuevo.set(true);
                        canalComunicacion.notify();
                    }

                    System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " PEDIDO CORRECTO " + p.getId() + ESPACIO + ANSI_RESET);
                } else {
                    System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " PEDIDO SIN PAGAR O NO HAY PEDIDO" + ESPACIO + ANSI_RESET);
                }

                mutexPedidosEnviados.lock();

                if (!Almazon.pedidosEnviados.isEmpty()) {

                    Pedido e = Almazon.pedidosEnviados.poll();

                    if (mutexPedidosEnviados.isHeldByCurrentThread())
                        mutexPedidosEnviados.unlock();

                    System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " ENVIA CORREO DEL PEDIDO " + e.getId() + ESPACIO + ANSI_RESET);
                } else {
                    if (mutexPedidosEnviados.isHeldByCurrentThread())
                        mutexPedidosEnviados.unlock();
                }
                Thread.sleep(Almazon.nSegundosSon24HorasReales / 24 / 60);
            } else {
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " DEJA DE TRABAJAR "  + ESPACIO + ANSI_RESET);
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }


    private Pedido tratarPedidoErroneo(Pedido mal){
        int num = (int) (Math.random() * POS_ERROR);
        if (num % POS_ERROR == 0) {
            return mal;
        }
        AtomicInteger i = new AtomicInteger();
        for(; i.get()<mal.getListaProductos().size();i.getAndIncrement()){
            if(!mal.getListaProductos().get(i.get()).equals(mal.getNotaOriginal().get(i.get()))){
                mal.getListaProductos().set(i.get(),mal.getNotaOriginal().get(i.get()));
            }
        }
        return mal;
    }

    private Pedido recogerPedidos(Pedido inicial) {
        CopyOnWriteArrayList<Integer> carrito = new CopyOnWriteArrayList<>();
        AtomicInteger i = new AtomicInteger();
        int num;
        for (Integer producto : inicial.getListaProductos()) {
            num = (int) (Math.random() * POS_ERROR);
            if (num % POS_ERROR == 0) {
                // hay error
                carrito.add(inicial.getListaProductos().get(i.get()) + 1);
            } else {
                // no hay error
                carrito.add(inicial.getListaProductos().get(i.get()));
            }
            i.getAndIncrement();
        }
        return new Pedido(carrito, inicial.getNotaOriginal(), inicial.getId());
    }


    public void trabajoRecogePedidos() throws InterruptedException {
        while (true) {
            Pedido p;

            if (trabajo) {
                Pedido nuevo;

                mutexPedidosErroneos.lock();

                if (!Almazon.pedidosErroneos.isEmpty()) {
                    nuevo = tratarPedidoErroneo(Objects.requireNonNull(Almazon.pedidosErroneos.poll()));

                    if (mutexPedidosErroneos.isHeldByCurrentThread())
                        mutexPedidosErroneos.unlock();

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " TRATANDO PEDIDO ERRONEO " + nuevo.getId() + ESPACIO + ANSI_RESET);

                    int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " PONE PEDIDO "+ nuevo.getId()+ " EN PLAYA" + ESPACIO + ANSI_RESET);
                    Almazon.todasPlayas[miPlaya].add(nuevo);

                } else {
                    if (mutexPedidosErroneos.isHeldByCurrentThread())
                        mutexPedidosErroneos.unlock();

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " ESPERA" + ESPACIO + ANSI_RESET);

                    synchronized (canalComunicacion) {
                        canalComunicacion.wait();
                    }


                    mutexPedidos.lock();

                    if (!Almazon.pedidos.isEmpty()) {
                        p = Almazon.pedidos.poll();

                        if (mutexPedidos.isHeldByCurrentThread())
                            mutexPedidos.unlock();

                        assert p != null;
                        System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " TRATA PEDIDO NUEVO " + p.getId() + ESPACIO + ANSI_RESET);

                        nuevo = recogerPedidos(p);
                        int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);


                        System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " PONE PEDIDO " + p.getId() + " EN PLAYA" + ESPACIO + ANSI_RESET);
                        Almazon.todasPlayas[miPlaya].add(nuevo);
                        Thread.sleep(Almazon.nSegundosSon24HorasReales / 24 / 60);
                    } else {
                        if (mutexPedidos.isHeldByCurrentThread())
                            mutexPedidos.unlock();
                    }
                }

            }  else {
                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " DEJA DE TRABAJAR "  + ESPACIO + ANSI_RESET);
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }


    public void trabajoEmpaquetaPedidos() throws InterruptedException {
        while (true) {
            if (trabajo) {
                int playaElegida = (int) (Math.random() * Almazon.NUM_PLAYAS);
                mutexPlayas.lock();
                if (!Almazon.todasPlayas[playaElegida].isEmpty()) {
                    if (!Almazon.todasPlayas[playaElegida].isSucia()) {
                        Pedido p = Almazon.todasPlayas[playaElegida].poll();

                        if (mutexPlayas.isHeldByCurrentThread())
                            mutexPlayas.unlock();

                        int num = (int) (Math.random() * POS_PEDIDO_ROTO);
                        boolean llamoLimpieza = (Almazon.cuentaEnviados.get() + 1) % 10 == 0;
                        if (!llamoLimpieza) playaALimpiar.set(playaElegida);

                        if (num % POS_PEDIDO_ROTO == 0 || llamoLimpieza) {
                            System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + id + " DETECTA PLAYA SUCIA " + playaALimpiar.get() + ESPACIO + ANSI_RESET);
                            mutexNotificacionLimpieza.lock();
                            Almazon.todasPlayas[playaElegida].setSucia(true);

                            synchronized (canalComunicacion) {
                                // llama y se espera hasta que limpien
                                limpiar.set(true);
                                canalComunicacion.notify();
                                if (mutexNotificacionLimpieza.isHeldByCurrentThread())
                                    mutexNotificacionLimpieza.unlock();
                            }
                            synchronized (canalComunicacion2) {
                                canalComunicacion2.wait();
                            }
                        }
                        if (p.getListaProductos().equals(p.getNotaOriginal())) {

                            Paquete paq = new Paquete(p.getListaProductos());
                            paq.setSello(true);

                            Almazon.cinta.offer(paq);
                            Almazon.pedidosEnviados.offer(p);
                            Almazon.cuentaEnviados.getAndIncrement();

                            System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + id + " ENVIA PEDIDO " + p.getId() + ESPACIO + ANSI_RESET);
                            Thread.sleep(Almazon.nSegundosSon24HorasReales / 24 / 60);
                        } else {
                            System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + Thread.currentThread().getId() + " DETECTA ERROR EN EL PEDIDO " + p.getId() + " SE MANDA A REVISAR" + ESPACIO + ANSI_RESET);
                            Almazon.pedidosErroneos.offer(p);
                            Thread.sleep(Almazon.nSegundosSon24HorasReales / 24 / 60);
                        }
                    }
                }
                if (mutexPlayas.isHeldByCurrentThread())
                    mutexPlayas.unlock();
            } else {
                System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + id + " DEJA DE TRABAJAR "  + ESPACIO + ANSI_RESET);
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }

    public void limpiarPlaya() {
        if (playaALimpiar.get()==-1) {
            for (int i = 0; i < Almazon.NUM_PLAYAS; i++) {
                Almazon.todasPlayas[i].setSucia(false);
            }
            System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO + ANSI_BLACK + "LIMPIEZA " + id + " LIMPIANDO TODAS LAS PLAYAS" + ESPACIO + ANSI_RESET);
        } else {
            if (Almazon.todasPlayas[playaALimpiar.get()].isSucia()) {
                Almazon.todasPlayas[playaALimpiar.get()].setSucia(false);
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + id + " LIMPIANDO PLAYA " + playaALimpiar.get() + ESPACIO + ANSI_RESET);
            }
        }
    }

    public void trabajoLimpieza() throws InterruptedException {
        while (true) {
            if (trabajo) {
                synchronized (canalComunicacion) {
                    canalComunicacion.wait();
                }

                mutexLimpiar.lock();//para que la funcion de limpiar sea atomica y nadie pueda interrumpir el proceso
                if (limpiar.get()) {

                    limpiarPlaya();
                    playaALimpiar.set(-1);

                    limpiar.set(false);

                    if (mutexLimpiar.isHeldByCurrentThread())
                        mutexLimpiar.unlock();
                } else {
                    if (mutexLimpiar.isHeldByCurrentThread())
                        mutexLimpiar.unlock();
                }


                synchronized (canalComunicacion2) {
                    canalComunicacion2.notify();
                }
            } else {
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO + ANSI_BLACK + "LIMPIEZA " + id + " DEJA DE TRABAJAR "  + ESPACIO + ANSI_RESET);
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }


    public void comprobarEstadoAlmazon(){
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "------------------------------------------------------" + ESPACIO + ANSI_RESET);
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: ENCARGADO REVISA:" + ESPACIO + ANSI_RESET);
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA LISTA DE PEDIDOS TIENE " + Almazon.pedidos.size() + " ELEMENTOS ACTUALMENTE" + ESPACIO + ANSI_RESET);
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA LISTA DE PEDIDOS ERRONEOS TIENE " + Almazon.pedidosErroneos.size() + " ELEMENTOS ACTUALMENTE" + ESPACIO + ANSI_RESET);
        for(int i = 0;i < Almazon.NUM_PLAYAS;i++){
            if(!Almazon.todasPlayas[i].isEmpty()) {
                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA PLAYA " + i + " ESTA EN USO CON " + Almazon.todasPlayas[i].size() + " PEDIDOS" + ESPACIO + ANSI_RESET);
            } else {
                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA PLAYA " + i + " NO ESTA EN USO" + ESPACIO + ANSI_RESET);
            }
        }

        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: SE HAN ENVIADO UN TOTAL DE " + Almazon.cinta.size() + " PEDIDOS" + ESPACIO + ANSI_RESET);
    }


    public void trabajoEncargado() throws InterruptedException {
        if(trabajo) {
            float msegundosPorTurno = Almazon.nSegundosSon24HorasReales / Almazon.NUM_TURNOS;
            float msegundosComienzoTurno = System.currentTimeMillis();
            int nRevisiones = 10;
            long periodoTurno = (long) msegundosPorTurno / nRevisiones;

            ArrayList<Thread> empleadosTrabajando = new ArrayList<>();

            Set<Thread> threadSet;
            while (true) {
                empleadosTrabajando.clear();
                comprobarEstadoAlmazon();

                threadSet = Thread.getAllStackTraces().keySet();
                for (Personal p : Almazon.personal) {
                    for (Thread t : threadSet) {
                        if (p.id == t.getId()) {
                            empleadosTrabajando.add(t);
                            System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: EL EMPLEADO " + p.id + " TIENE EL ESTADO " + t.getState().toString() + ESPACIO + ANSI_RESET);
                        }
                    }
                }

                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "------------------------------------------------------" + ESPACIO + ANSI_RESET);

//                float msegundosTranscurridos = (System.currentTimeMillis()) - msegundosComienzoTurno;
//                if (msegundosTranscurridos >= msegundosPorTurno) {
//                    System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: CAMBIO DE TURNO" + ESPACIO + ANSI_RESET);
//                    System.out.println();
//                    System.out.println();
//                    System.out.println();
//                    System.out.println();
//                    System.out.println();
//                    for (Thread t : empleadosTrabajando) {
//                        t.interrupt();
//                    }
//                    msegundosComienzoTurno = System.currentTimeMillis();
//                }
//                Almazon.turnoActual = (Almazon.turnoActual + 1) % Almazon.NUM_TURNOS;


                Thread.sleep(periodoTurno);
            }
        } else {
            System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO " + id + " DEJA DE TRABAJAR "  + ESPACIO + ANSI_RESET);
            Thread.sleep(Integer.MAX_VALUE);
        }
    }

}
