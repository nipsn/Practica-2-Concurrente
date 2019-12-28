package com.practica2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Personal {
    private int tipo;

    private final int POS_ERROR = 100; // 1 de cada 100
    private final int POS_PEDIDO_ROTO = 20;// el 5%

    private AtomicInteger playaALimpiar;
    public AtomicBoolean hayPedidoNuevo;
    public AtomicBoolean pedidoEnviado;
    public AtomicBoolean limpiar;

    private Exchanger<Pedido> canalAdminRecogeP;

    private final Object canalComunicacion;

    // Constructor general
    public Personal(int tipo, Object comunicador) {
        this.tipo = tipo;

        pedidoEnviado = new AtomicBoolean();
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
            //trabajoLimpieza();
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
            Thread.sleep(5000);
            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
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
            if (!Almazon.pedidosErroneos.isEmpty()) {
                System.out.println("RECOGEPEDIDOS TRATANDO PEDIDO ERRONEO");
                nuevo = tratarPedido(Objects.requireNonNull(Almazon.pedidosErroneos.poll()));
            } else {
                System.out.println("RECOGEPEDIDOS ESPERA");
                synchronized (canalComunicacion){
                    try {
                        canalComunicacion.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Pedido p = Almazon.pedidos.peek();
                System.out.println("RECOGEPEDIDOS TRATA PEDIDO NUEVO");
                assert p != null;
                nuevo = tratarPedido(p);
            }
            int miPlaya = (int) (Math.random() * 2);// max playas
            // si la playa esta sucia me bloqueo
            while(Almazon.todasPlayas[miPlaya].isSucia());

            System.out.println("RECOGEPEDIDOS PONE PEDIDO EN PLAYA");
            Almazon.todasPlayas[miPlaya].add(nuevo);
        }
    }

    public boolean comprobarPedido(Pedido p) {
        Pedido encontrado = new Pedido();
        for (Pedido aux : Almazon.pedidos) {
            if (aux.getId() == p.getId()) {
                System.out.println("EMPAQUETAPEDIDOS HA ENCONTRADO EL PEDIDO ORIGINAL");
                encontrado = aux;
            }
        }

        return encontrado.getListaProductos().equals(p.getListaProductos());
    }

    public void trabajoEmpaquetaPedidos() {
        while (true) {
            int playaElegida = (int) (Math.random() * 2);
            if(!Almazon.todasPlayas[playaElegida].isEmpty()) {
                if (!Almazon.todasPlayas[playaElegida].isSucia()) {
                    Pedido p = Almazon.todasPlayas[playaElegida].poll();

                    int num = (int) (Math.random() * POS_PEDIDO_ROTO);
                    boolean llamoLimpieza = p.getId() % 10 == 0;

                    if(!llamoLimpieza) playaALimpiar.set(playaElegida);

                    if (num % POS_PEDIDO_ROTO == 0 || llamoLimpieza) {
                        synchronized (canalComunicacion) {
                            // llama y se espera hasta que limpien
                            canalComunicacion.notify();
                            try {
                                canalComunicacion.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    System.out.println("EMPAQUETAPEDIDOS COMPROBANDO PEDIDO");
                    if (comprobarPedido(p)) {
                        System.out.println("EMPAQUETAPEDIDOS PEDIDO CORRECTO, SE ENVÍA");
                        Almazon.cinta.offer(p);
                        pedidoEnviado.set(true);//variable que controla si algún pedido ha sido envíado
//                           synchronized (lock1) {
//                               lock1.notify();//hay que poner la espera al administrativo para mandar el mensaje
//                           }
                    } else {
                        System.out.println("EMPAQUETAPEDIDOS ERROR EN EL PEDIDO, SE MANDA A REVISAR");
                        Almazon.pedidosErroneos.offer(p);
                    }
                }
            }
        }
    }

    public void limpiarPlaya(int playaSucia) {
        if (playaSucia==-1) {
            System.out.println("LIMPIEZA, LIMPIANDO TODAS LAS PLAYAS");
            for (int i = 0; i < 2; i++) {
                Almazon.todasPlayas[i].setSucia(false);
            }
        } else {
            if (Almazon.todasPlayas[playaSucia].isSucia()) {
                System.out.println("LIMPIEZA, LIMPIANDO PLAYA " + playaSucia);
                Almazon.todasPlayas[playaSucia].setSucia(false);
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

            limpiarPlaya(playaALimpiar.get());
            playaALimpiar.set(-1);

            synchronized (canalComunicacion){
                canalComunicacion.notifyAll();
            }

        }
    }

}
   // public void trabajoEncargado(){}
//}
