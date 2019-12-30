package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Cliente {
    int tercioInactivo;
    long tiempoComienzo;
    //TODO: Hacer compras aleatorias. Dormir tiempo aleatorio, pero leer enunciado; tienen un tiempo en el que dejan de comprar
    public void comprar() throws InterruptedException {
        tiempoComienzo = System.currentTimeMillis() / 1000; //segundo en el que comienza
        CopyOnWriteArrayList<Integer> compra = new CopyOnWriteArrayList<>();
        while(true){
            compra.clear();



            compra.add(1);
            compra.add(2);
            compra.add(3);
            compra.add(234);
            Almazon.pedidos.add(new Pedido(compra,true));
            Thread.sleep(5*Almazon.segundoConvertido);
        }
    }
}
