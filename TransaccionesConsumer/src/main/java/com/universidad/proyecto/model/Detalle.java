package com.universidad.proyecto.model;

public class Detalle {
	private String nombreBeneficiario;
    private String tipoTransferencia;
    private String descripcion;
    private Referencias referencias;
    
    public String getNombreBeneficiario() {
    	return nombreBeneficiario;
    }
    
    public void setNombreBeneficiario(String NombreBeneficiario) {
    	this.nombreBeneficiario = NombreBeneficiario;
    }
    
    public String getTipoTransferencia() {
    	return tipoTransferencia;
    }
    
    public void setTipoTransfarencia(String TipoTransfarencia) {
    	this.tipoTransferencia = TipoTransfarencia;
    }
    
    public String getDescripcion() {
    	return descripcion;
    }
    
    public void setDescripcion(String Descripcion) {
    	this.descripcion = Descripcion;
    }
    
    
    public Referencias getReferencias() {
    	return referencias;
    }
    
    public void setReferencias(Referencias Referencias1) {
    	this.referencias = Referencias1;
    }
}
    
    
