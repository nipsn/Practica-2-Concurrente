package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Personal {
    private int tipo;

    private final int ADMINISTRATIVO = 1;
    private final int RECOGEPEDIDOS = 2;
    private final int EMPAQUETAPEDIDOS = 3;
    private final int LIMPIEZA = 4;
    private final int ENCARGADO = 5;
    private final int POS_ERROR = 100; // 1 de cada 10
    private Object lock;
    public AtomicBoolean hayPedido;

    public Personal(int tipo){
        this.tipo = tipo;
        hayPedido.set(false);
    }

    public void tarea(){
        if(this.tipo == ADMINISTRATIVO){
            trabajoAdministrativo();
        } else if (this.tipo == RECOGEPEDIDOS){
            try {
                trabajoRecogePedidos();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (this.tipo == EMPAQUETAPEDIDOS){
            //trabajoEmpaquetaPedidos();
        } else if(this.tipo == LIMPIEZA){
            //trabajoLimpieza();
        } else if (this.tipo == ENCARGADO) {
            trabajoEncargado();
        }
    }
    public void trabajoAdministrativo(){
        while(true){
            Pedido p = Almazon.pedidos.peek();
            if(p != null && p.isPagado()) {
                hayPedido.set(true);
                synchronized (lock) {
                    lock.notify();
                }
            }
            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
        }
    }

    private Pedido tratarPedido(Pedido inicial) throws InterruptedException {
        ArrayList<Integer> carrito = new ArrayList<>();
        for (Integer i: inicial.getListaProductos()) {
            int num;
            num = (int) (Math.random() * POS_ERROR);
            if(num % POS_ERROR == 0){
                // hay error
                carrito.add(inicial.getListaProductos().get(i) + 1);
            } else {
                // no hay error
                carrito.add(inicial.getListaProductos().get(i));
            }
            Thread.sleep((long) (Math.random() * 2)); //de 0 a 2 segundos a dormir
        }
        return new Pedido(carrito,inicial.getId());
    }

    public void trabajoRecogePedidos() throws InterruptedException {
        while(true){
            Pedido nuevo;
            if(!Almazon.pedidosErroneos.isEmpty()){
                System.out.println("RECOGEPEDIDOS TRATANDO PEDIDO ERRONEO");
                nuevo = tratarPedido(Almazon.pedidosErroneos.poll());
            } else {
                synchronized (lock) {
                    if (!hayPedido.get()) {
                        while (!hayPedido.get())
                            System.out.println("RECOGEPEDIDOS SE BLOQUEA");
                            lock.wait();
                    }
                }
                System.out.println("RECOGEPEDIDOS TRATA PEDIDO NUEVO");
                //pillo el pedido
                Pedido p = Almazon.pedidos.poll();
                hayPedido.set(false);
                nuevo = tratarPedido(p);
            }
            int miPlaya = (int) (Math.random() * 2);// max playas
            Almazon.todasPlayas[miPlaya].add(nuevo);
        }
    }
//    public void trabajoEmpaquetaPedidos(){
//        while(true){
//            for(int i = 0;i < 2;i++) {
//                if (!Almazon.todasPlayas[i].isSucia()) {
//                    Almazon.cinta.offer(Almazon.p.poll());
//                }
//            }
//        }
//    }
//    public void trabajoLimpieza(){}
    public void trabajoEncargado(){}
}
