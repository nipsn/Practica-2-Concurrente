package com.practica2;

public class Pedido{
    private boolean pagado;
    private boolean roto;
    private int estado;
    private boolean atendido;

    public Pedido(){
        pagado = false;
        roto = false;
        estado = 0;
    }

    public boolean isPagado() {
        return pagado;
    }

    public boolean isRoto() {
        return roto;
    }

    public int getEstado() {
        return estado;
    }

    public boolean isAtendido() {
        return atendido;
    }
}
