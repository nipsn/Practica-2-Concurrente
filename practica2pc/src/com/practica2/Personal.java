package com.practica2;

public class Personal {
    private int tipo;

    private final int ADMINISTRATIVO = 1;
    private final int RECOGEPEDIDOS = 2;
    private final int EMPAQUETAPEDIDOS = 3;
    private final int LIMPIEZA = 4;
    private final int ENCARGADO = 5;
    private Object lock;

    public Personal(int a){
        this.tipo = a;
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
            trabajoEmpaquetaPedidos();
        } else if(this.tipo == LIMPIEZA){
            trabajoLimpieza();
        } else if (this.tipo == ENCARGADO) {
            trabajoEncargado();
        }
    }
    public void trabajoAdministrativo(){
        while(true){
            //replantear. puede que al peekear la segunda vez haya uno distinto
            Pedido p = Almazon.pedidos.peek();
            if(p != null && p.isPagado()) {
                Almazon.hayPedido.set(true);
                synchronized (lock) {
                    lock.notify();
                }
            }
            //ademas recibe notificacion de empaquetapedidos cuando un producto sale para mandar un correo (y hacer las gestiones oportunas)
        }
    }
    public void trabajoRecogePedidos() throws InterruptedException {
        while(true){
            synchronized (lock){
                if(!Almazon.hayPedido.get()){
                    while(!Almazon.hayPedido.get())
                        lock.wait();
                }
            }

            Pedido p = Almazon.pedidos.peek();
            //Almazon.todasPlayas[/*random*/].add(Almazon.pedidos.poll());
            Almazon.hayPedido.set(false);

        }
    }
    public void trabajoEmpaquetaPedidos(){
        while(true){
            for(int i = 0;i < 2;i++) {
                if (!Almazon.todasPlayas[i].isSucia()) {
                    Almazon.cinta.offer(Almazon.p.poll());
                }
            }
        }
    }
    public void trabajoLimpieza(){}
    public void trabajoEncargado(){}
}
