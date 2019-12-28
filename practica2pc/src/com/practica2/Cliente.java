package com.practica2;

import java.util.ArrayList;

public class Cliente {
    public void comprar() throws InterruptedException {
        ArrayList<Integer> compra = new ArrayList<>();
        compra.add(1);
        compra.add(2);
        compra.add(3);
        compra.add(234);

        while(true){
            Almazon.pedidos.add(new Pedido(compra,true));
            Thread.sleep(3000);
        }
    }
}
