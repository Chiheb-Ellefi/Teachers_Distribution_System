package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequestDTO {

    // Getters et Setters
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


}