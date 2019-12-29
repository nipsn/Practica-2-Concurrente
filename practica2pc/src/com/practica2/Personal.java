package com.practica2;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Personal {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ESPACIO = " ";


    private final int POS_ERROR = 100; // 1 de cada 100
    private final int POS_PEDIDO_ROTO = 20; // el 5%

    public static AtomicInteger playaALimpiar;
    public static AtomicBoolean hayPedidoNuevo;
    public static AtomicBoolean limpiar;


    private static ReentrantLock mutexPlayas = new ReentrantLock();
    private static ReentrantLock mutexPedidos = new ReentrantLock();
    private static ReentrantLock mutexPedidosEnviados = new ReentrantLock();
    private static ReentrantLock mutexPedidosErroneos = new ReentrantLock();
    private static ReentrantLock mutexNotificacionLimpieza = new ReentrantLock();

    private int tipo;
    private boolean trabajo;
    private final Object canalComunicacion;
    private final Object canalComunicacion2;
    private long id;
    private Thread.State estado;

    public Personal(int tipo, Object comunicador, Object comunicador2) {
        this.tipo = tipo;

        hayPedidoNuevo = new AtomicBoolean();
        limpiar = new AtomicBoolean();
        playaALimpiar = new AtomicInteger(-1);

        canalComunicacion = comunicador;
        canalComunicacion2 = comunicador2;
    }


    public void tarea() throws InterruptedException {
        this.id = Thread.currentThread().getId();

        if (this.tipo == Almazon.T_ADMINISTRATIVO) {
            try {
                trabajoAdministrativo();
            } catch (InterruptedException e){
                // pongo la variable
                // TODO: IMPLEMENTAR MANEJADOR DE INTERRUPCION (BOOLEANO TRABAJA/NO TRABAJA)
                // TODO: CAMBIAR LOS BUCLES WHILE?? QUIZA DENTRO DEL WHILE DISTINGUIR SI TRABAJA O NO
                // TODO: LA INTERRUPCION DONDE VA?? EN CADA trabajo() O PUEDE IR EN tarea() ??
            }
        } else if (this.tipo == Almazon.T_RECOGEPEDIDOS) {
            trabajoRecogePedidos();
        } else if (this.tipo == Almazon.T_EMPAQUETAPEDIDOS) {
            trabajoEmpaquetaPedidos();
        } else if (this.tipo == Almazon.T_LIMPIEZA) {
            trabajoLimpieza();
        } else if (this.tipo == Almazon.T_ENCARGADO) {
            trabajoEncargado();
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
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " PEDIDO CORRECTO "  + p.getId() + ESPACIO + ANSI_RESET);
            } else {

                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " PEDIDO INCORRECTO O NO HAY PEDIDO" + ESPACIO + ANSI_RESET);
            }
            Thread.sleep(1000);

            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
            mutexPedidosEnviados.lock();

            if(!Almazon.pedidosEnviados.isEmpty()){
                Pedido e = Almazon.pedidosEnviados.poll();
                if(mutexPedidosEnviados.isHeldByCurrentThread())
                    mutexPedidosEnviados.unlock();
                System.out.println(ANSI_PURPLE_BACKGROUND + ESPACIO + ANSI_BLACK + "ADMINISTRATIVO " + id + " ENVIA CORREO DEL PEDIDO " + e.getId() + ESPACIO + ANSI_RESET);
            }else{
                if(mutexPedidosEnviados.isHeldByCurrentThread())
                    mutexPedidosEnviados.unlock();
            }
        }
    }


    private Pedido tratarPedidoErroneo(Pedido mal){

        int num = (int) (Math.random() * POS_ERROR);
        if (num % POS_ERROR == 0) {
        //hay error pero da igual donde
            return mal;
        }
       for(int i=0; i<mal.getListaProductos().size();i++){
            if(!mal.getListaProductos().get(i).equals(mal.getNotaOriginal().get(i))){
                mal.getListaProductos().set(i,mal.getNotaOriginal().get(i));
            }
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

                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " TRATANDO PEDIDO ERRONEO" + ESPACIO + ANSI_RESET);

                int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);
                // si la playa esta sucia me bloqueo
                while(Almazon.todasPlayas[miPlaya].isSucia());

                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " PONE PEDIDO EN PLAYA" + ESPACIO + ANSI_RESET);
                Almazon.todasPlayas[miPlaya].add(nuevo);

            } else {
                if(mutexPedidosErroneos.isHeldByCurrentThread())
                    mutexPedidosErroneos.unlock();

                System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " ESPERA" + ESPACIO + ANSI_RESET);

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

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " TRATA PEDIDO NUEVO" + ESPACIO + ANSI_RESET);

                    nuevo = recogerPedidos(p);
                    int miPlaya = (int) (Math.random() * Almazon.NUM_PLAYAS);

                    // si la playa esta sucia me bloqueo
                    while(Almazon.todasPlayas[miPlaya].isSucia());

                    System.out.println(ANSI_GREEN_BACKGROUND + ESPACIO + ANSI_BLACK + "RECOGEPEDIDOS " + id + " PONE PEDIDO EN PLAYA" + ESPACIO + ANSI_RESET);
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
                        System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + id + " DETECTA PLAYA SUCIA " + playaALimpiar.get() + ESPACIO + ANSI_RESET);
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
                        System.out.println(ANSI_BLUE_BACKGROUND + ESPACIO + ANSI_BLACK + "EMPAQUETAPEDIDOS " + id + " ENVIA PEDIDO " + p.getId() + ESPACIO + ANSI_RESET);
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
            System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO + ANSI_BLACK + "LIMPIEZA " + id + " LIMPIANDO TODAS LAS PLAYAS" + ESPACIO + ANSI_RESET);
        } else {
            if (Almazon.todasPlayas[playaALimpiar.get()].isSucia()) {
                Almazon.todasPlayas[playaALimpiar.get()].setSucia(false);
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + id + " LIMPIANDO PLAYA " + playaALimpiar.get() + ESPACIO + ANSI_RESET);
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
            System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + id + " HA DESPERTADO " + ESPACIO + ANSI_RESET);

            if(limpiar.get()) {
                System.out.println(ANSI_YELLOW_BACKGROUND + ESPACIO +  ANSI_BLACK + "LIMPIEZA " + id + " VA A LIMPIAR UNA PLAYA " + ESPACIO + ANSI_RESET);
                limpiarPlaya();
                playaALimpiar.set(-1);
                limpiar.set(false);
            }


            synchronized (canalComunicacion2){
                canalComunicacion2.notifyAll();
            }
        }
    }


    public void comprobarEstadoAlmazon(){
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "------------------------------------------------------" + ESPACIO + ANSI_RESET);
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: ENCARGADO REVISA:" + ESPACIO + ANSI_RESET);
        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA LISTA DE PEDIDOS TIENE " + Almazon.pedidos.size() + " ELEMENTOS ACTUALMENTE" + ESPACIO + ANSI_RESET);
        System.out.println("ENCARGADO: LA LISTA DE PEDIDOS ERRONEOS TIENE " + Almazon.pedidosErroneos.size() + " ELEMENTOS ACTUALMENTE");
        for(int i = 0;i < Almazon.NUM_PLAYAS;i++){
            if(!Almazon.todasPlayas[i].isEmpty()) {
                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA PLAYA " + i + " ESTA EN USO" + ESPACIO + ANSI_RESET);
            } else {
                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: LA PLAYA " + i + " NO ESTA EN USO" + ESPACIO + ANSI_RESET);
            }
        }

        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: SE HAN ENVIADO UN TOTAL DE " + Almazon.cinta.size() + " PEDIDOS" + ESPACIO + ANSI_RESET);
    }

    /*
    * TODO: BORRAR ESTO ANTES DE ENTREGAR
    * Que coño hace esto? Buena pregunta.
    * Tenemos una lista de hilos que estan trabajando actualmente (empleadosTrabajando)
    * Se mira la lista del personal, disponible en Almazon. Cada Personal guarda su Thread id.
    * Teniendo en cuenta esto, se puede conseguir un conjunto de todos los hilos y comprobar los Thread id. Si coinciden, es que
    * el hilo en cuestion esta trabajando y se mete en la lista.
    * Esta lista luego se usa para interrumpir a todos los que trabajan para decir que su turno ha terminado o acabado
    *
    * TODO: Implementar lo que hacen las interrupciones (metodo tarea())
    */
    public void trabajoEncargado(){
        float segundosPorTurno = 24 / Almazon.NUM_TURNOS;
        float segundoComienzoTurno = System.currentTimeMillis() / 1000;
        int nRevisiones = 10;
        long periodoTurno = (long) segundosPorTurno / nRevisiones;

        ArrayList<Thread> empleadosTrabajando = new ArrayList<>();

        while(true){
            empleadosTrabajando.clear();
            comprobarEstadoAlmazon();

            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            for(Personal p : Almazon.personal){
                for(Thread t : threadSet){
                    if(p.id == t.getId()){
                        empleadosTrabajando.add(t);
                        System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: EL EMPLEADO " + p.id + " TIENE EL ESTADO " + t.getState().toString() + ESPACIO + ANSI_RESET);
                    }
                }
            }

            System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "------------------------------------------------------" + ESPACIO + ANSI_RESET);

            float segundosTranscurridos = (System.currentTimeMillis() / 1000) - segundoComienzoTurno;
            if(segundosTranscurridos >= segundosPorTurno){
                System.out.println(ANSI_RED_BACKGROUND + ESPACIO + ANSI_BLACK + "ENCARGADO: CAMBIO DE TURNO" + ESPACIO + ANSI_RESET);
                for(Thread t : empleadosTrabajando){
                    t.interrupt();
                }
                segundoComienzoTurno = System.currentTimeMillis() / 1000;
            }


            try {
                Thread.sleep(periodoTurno * Almazon.segundoConvertido);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
