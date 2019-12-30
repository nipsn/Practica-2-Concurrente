package com.practica2;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Playa {
    /*
     * ╔══════════════════════════════════════════════╗
     * ║         «VARIABLES CONFIGURABLES»            ║
     * ╠══════════════════════════════════════════════╣
     */
                 private final int MAX = 20;
    /*
     * ║                                              ║
     * ╚══════════════════════════════════════════════╝
     */    
    private BlockingQueue<Pedido> contenido;
    private boolean sucia;

    public Playa(){
        contenido = new ArrayBlockingQueue<>(MAX);
        sucia = false;
    }

    public boolean add(Pedido p){
        return contenido.offer(p);
    }

    public Pedido poll(){
        return contenido.poll();
    }

    public Pedido peek(){
        return contenido.peek();
    }

    public void setSucia(boolean estado){
        sucia = estado;
    }

    public boolean isSucia(){
        return sucia;
    }
    public boolean isFull(){
        return contenido.size() == MAX;
    }
    public boolean isEmpty(){
        return contenido.isEmpty();
    }
}
