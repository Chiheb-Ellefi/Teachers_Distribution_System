package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmailRequestDTO {

    @NotNull(message = "Le type est obligatoire")
    @Pattern(regexp = "PERSONNALISE|RESPONSABLES|SURVEILLANTS",
            message = "Type invalide. Valeurs acceptées: PERSONNALISE, RESPONSABLES, SURVEILLANTS")
    private String type;

    @NotBlank(message = "L'objet du mail est obligatoire")
    private String objet;

    @NotBlank(message = "Le corps du mail est obligatoire")
    private String corps;

    // Pour type PERSONNALISE uniquement: "email1@gmail.com, email2@gmail.com"
    private String destinataires;

    // Constructeurs
    public EmailRequestDTO() {
    }

    public EmailRequestDTO(String type, String objet, String corps, String destinataires) {
        this.type = type;
        this.objet = objet;
        this.corps = corps;
        this.destinataires = destinataires;
    }

    // Getters et Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getCorps() {
        return corps;
    }

    public void setCorps(String corps) {
        this.corps = corps;
    }

    public String getDestinataires() {
        return destinataires;
    }

    public void setDestinataires(String destinataires) {
        this.destinataires = destinataires;
    }

    @Override
    public String toString() {
        return "EmailRequestDTO{" +
                "type='" + type + '\'' +
                ", objet='" + objet + '\'' +
                ", destinataires='" +
                (destinataires != null ? destinataires.substring(0, Math.min(50, destinataires.length())) : "null") +
                "...'}";
    }
}