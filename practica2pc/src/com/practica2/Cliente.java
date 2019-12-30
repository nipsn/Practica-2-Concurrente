package com.practica2;

import java.util.concurrent.CopyOnWriteArrayList;

public class Cliente {
    /*
     * ╔════════════════════════════════════════════════════╗
     * ║               «VARIABLES CONFIGURABLES»            ║
     * ╠════════════════════════════════════════════════════╣
     */
    //public static final int MAX_NUM_PRODUCTOS_POR_PEDIDO = 10;
        public static final int NUM_PRODUCTOS_ALMAZON = 1000;
        public static final int MAX_HORAS_ENTRE_PEDIDOS = 2;
        public static final int POS_PEDIDO_NO_PAGADO = 100;
    /*
     * ║                                                    ║
     * ╚════════════════════════════════════════════════════╝
     */
    public void comprar() throws InterruptedException {
        CopyOnWriteArrayList<Integer> compra = new CopyOnWriteArrayList<>();
        int nProductos;
        //boolean pagado;
        while(true){
            compra.clear();
//            nProductos = (int) (Math.random() * MAX_NUM_PRODUCTOS_POR_PEDIDO);
            nProductos = 5;
            for(int i = 0;i < nProductos;i++)
                compra.add((int) (Math.random() * NUM_PRODUCTOS_ALMAZON));

            //pagado = ((int)(Math.random() * POS_PEDIDO_NO_PAGADO)) % POS_PEDIDO_NO_PAGADO == 0;

            Almazon.pedidos.add(new Pedido(compra,true));

            if(((int)(Math.random() * 100)) % 100 == 0){
                Thread.sleep(Almazon.nSegundosSon24HorasReales/Almazon.NUM_TURNOS);
            } else {
                Thread.sleep(((int) (Math.random() * MAX_HORAS_ENTRE_PEDIDOS)) * (Almazon.nSegundosSon24HorasReales / 24));
            }
        }
    }
}
