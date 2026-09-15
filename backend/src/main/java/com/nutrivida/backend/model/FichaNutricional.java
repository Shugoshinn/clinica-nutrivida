// src/main/java/com/nutrivida/backend/model/FichaNutricional.java
package com.nutrivida.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "fichas_nutricionales")
public class FichaNutricional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El RUT del paciente es obligatorio")
    private String pacienteRut;
    private String motivoConsulta;
    private String antecedentes;
    @NotNull(message = "El peso actual es obligatorio")
    private Double pesoActual;
    private Integer estatura;
    private Double grasaCorporal;
    private Integer cintura;
    private Integer objetivoCalorico;
    private Integer proteinas;
    private Integer carbos;
    private Integer grasas;
    @Column(length = 2000)
    private String pautaDetalle;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPacienteRut() { return pacienteRut; }
    public void setPacienteRut(String pacienteRut) { this.pacienteRut = pacienteRut; }
    public String getMotivoConsulta() { return motivoConsulta; }
    public void setMotivoConsulta(String motivoConsulta) { this.motivoConsulta = motivoConsulta; }
    public String getAntecedentes() { return antecedentes; }
    public void setAntecedentes(String antecedentes) { this.antecedentes = antecedentes; }
    public Double getPesoActual() { return pesoActual; }
    public void setPesoActual(Double pesoActual) { this.pesoActual = pesoActual; }
    public Integer getEstatura() { return estatura; }
    public void setEstatura(Integer estatura) { this.estatura = estatura; }
    public Double getGrasaCorporal() { return grasaCorporal; }
    public void setGrasaCorporal(Double grasaCorporal) { this.grasaCorporal = grasaCorporal; }
    public Integer getCintura() { return cintura; }
    public void setCintura(Integer cintura) { this.cintura = cintura; }
    public Integer getObjetivoCalorico() { return objetivoCalorico; }
    public void setObjetivoCalorico(Integer objetivoCalorico) { this.objetivoCalorico = objetivoCalorico; }
    public Integer getProteinas() { return proteinas; }
    public void setProteinas(Integer proteinas) { this.proteinas = proteinas; }
    public Integer getCarbos() { return carbos; }
    public void setCarbos(Integer carbos) { this.carbos = carbos; }
    public Integer getGrasas() { return grasas; }
    public void setGrasas(Integer grasas) { this.grasas = grasas; }
    public String getPautaDetalle() { return pautaDetalle; }
    public void setPautaDetalle(String pautaDetalle) { this.pautaDetalle = pautaDetalle; }
}