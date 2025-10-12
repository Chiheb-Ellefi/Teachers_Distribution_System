package org.teacherdistributionsystem.distribution_system.models.keys;

public record TeacherKey(String nom, String prenom) {
    public TeacherKey(String nom, String prenom) {
        this.nom = nom.toLowerCase().trim();
        this.prenom = prenom.toLowerCase().trim();
    }
}