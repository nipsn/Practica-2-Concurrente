package com.practica2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Personal {
    private int tipo;

    private final int ADMINISTRATIVO = 1;
    private final int RECOGEPEDIDOS = 2;
    private final int EMPAQUETAPEDIDOS = 3;
    private final int LIMPIEZA = 4;
    private final int ENCARGADO = 5;
    private final int POS_ERROR = 100; // 1 de cada 100
    private final int POS_PEDIDO_ROTO = 20;// el 5%
    private AtomicInteger playaALimpiar;
    public AtomicBoolean pedidoEnviado;
    public AtomicBoolean limpiar;


    private Exchanger<Pedido> canalAdminRecogeP;

    public Personal(int tipo, Exchanger ex) {
        this.tipo = tipo;
        pedidoEnviado = new AtomicBoolean();
        limpiar = new AtomicBoolean();
        playaALimpiar = new AtomicInteger(-1);
        this.canalAdminRecogeP = ex;
    }

    public void tarea() throws InterruptedException {
        if (this.tipo == ADMINISTRATIVO) {
            trabajoAdministrativo();
        } else if (this.tipo == RECOGEPEDIDOS) {
            trabajoRecogePedidos();
        } else if (this.tipo == EMPAQUETAPEDIDOS) {
            trabajoEmpaquetaPedidos();
        } else if (this.tipo == LIMPIEZA) {
            trabajoLimpieza();
        } else if (this.tipo == ENCARGADO) {
            //trabajoEncargado();
        }
    }

    public void trabajoAdministrativo() throws InterruptedException {
        while (true) {
            Pedido p = Almazon.pedidos.peek();
            System.out.println("ADMINISTRATIVO COMPRUEBA PEDIDO");
            if (p != null && p.isPagado()) {
                canalAdminRecogeP.exchange(p);
                System.out.println("ADMINISTRATIVO PEDIDO CORRECTO");

            }
            Thread.sleep(1000);
            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
        }
    }

    private Pedido tratarPedido(Pedido inicial) throws InterruptedException {
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
            Thread.sleep((long) (Math.random() * 2) * 1000); //de 0 a 2 segundos a dormir
        }
        return new Pedido(carrito, inicial.getId());
    }

    public void trabajoRecogePedidos() throws InterruptedException {
        while (true) {
            Pedido nuevo;
            System.out.println("RECOGEPEDIDOS TRABAJA");
            if (!Almazon.pedidosErroneos.isEmpty()) {
                System.out.println("RECOGEPEDIDOS TRATANDO PEDIDO ERRONEO");
                nuevo = tratarPedido(Objects.requireNonNull(Almazon.pedidosErroneos.poll()));
            } else {
                Pedido p = canalAdminRecogeP.exchange(null);
                //hayPedido.set(false);
                System.out.println("RECOGEPEDIDOS TRATA PEDIDO NUEVO");
                //pillo el pedido
//                hayPedido.set(false);
                assert p != null;
                nuevo = tratarPedido(p);
            }
            int miPlaya = (int) (Math.random() * 2);// max playas
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
                    int num;
                    num = (int) (Math.random() * POS_PEDIDO_ROTO);
                    if (num % POS_PEDIDO_ROTO == 0) {
                        playaALimpiar.set(playaElegida);
                        limpiar.set(true);
//                        synchronized (lock2) {
//                            lock2.notify();
//                        }
                    }
                        System.out.println("EMPAQUETAPEDIDOS COMPROBANDO PEDIDO");
                        if (comprobarPedido(p)) {
                            System.out.println("EMPAQUETAPEDIDOS PEDIDO CORRECTO, SE ENVÍA");
                            Almazon.cinta.offer(p);
                            pedidoEnviado.set(true);//variable que controla si algún pedido ha sido envíado
//                            synchronized (lock1) {
//                                lock1.notify();//hay que poner la espera al administrativo para mandar el mensaje
//                            }

                        } else {
                            System.out.println("EMPAQUETAPEDIDOS ERROR EN EL PEDIDO, SE MANDA A REVISAR");
                            Almazon.pedidosErroneos.offer(p);
                        }
                    }

            }
        }
    }

    public void limpiarPlayas(int playaSucia) {
        
//        if (playaSucia==-1) {//cuando nos pasan una playa especifica a limpiar
//            for (int i = 0; i < 2; i++) {
//                System.out.println("LIMPIEZA, LIMPIANDO TODAS LAS PLAYAS");
//                Almazon.todasPlayas[i].setSucia(false);
//            }
//        } else {//cuando pase cierto numero pedidos y se limpien todas las playas
//            if (Almazon.todasPlayas[playaSucia].isSucia()) {//cuando nos pasan una playa especifica a limpiar
//                System.out.println("LIMPIEZA, LIMPIANDO PLAYA " + playaSucia);
//                Almazon.todasPlayas[playaSucia].setSucia(false); //no se como acceder a cada posiscion para borrar
//            }
//
//        }
    }


    public void trabajoLimpieza() throws InterruptedException {
        while (true) {
//            synchronized (lock2) {
//                while (!limpiar.get()) {
//                    lock2.wait();
//                }
//            }
//
//            limpiarPlayas(playaALimpiar.get());
//            playaALimpiar.set(-1);

        }
    }

}
   // public void trabajoEncargado(){}
//}
