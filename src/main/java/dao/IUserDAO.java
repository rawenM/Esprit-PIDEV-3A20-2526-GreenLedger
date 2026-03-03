package dao;


import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;

import java.util.List;
import java.util.Optional;

/**
 * Interface définissant les opérations CRUD pour User
 */
public interface IUserDAO {

    // ========== CRUD de base ==========

    /**
     * Créer un nouvel utilisateur
     */
    User save(User user);

    /**
     * Trouver un utilisateur par ID
     */
    Optional<User> findById(Long id);

    /**
     * Récupérer tous les utilisateurs
     */
    List<User> findAll();

    /**
     * Mettre à jour un utilisateur
     */
    User update(User user);

    /**
     * Supprimer un utilisateur par ID
     */
    boolean delete(Long id);

    // ========== Méthodes de recherche spécifiques ==========

    /**
     * Trouver un utilisateur par email
     */
    Optional<User> findByEmail(String email);

    /**
     * Trouver un utilisateur par telephone
     */
    Optional<User> findByTelephone(String telephone);

    Optional<User> findByToken(String token);
    /**
     * Trouver les utilisateurs par type
     */
    List<User> findByType(TypeUtilisateur type);

    /**
     * Trouver les utilisateurs par statut
     */
    List<User> findByStatut(StatutUtilisateur statut);

    /**
     * Vérifier si un email existe déjà
     */
    boolean emailExists(String email);

    // ========== Statistiques ==========

    /**
     * Compter le nombre total d'utilisateurs
     */
    long count();

    /**
     * Compter les utilisateurs par statut
     */
    long countByStatut(StatutUtilisateur statut);

    /**
     * Compter les utilisateurs par type
     */
    long countByType(TypeUtilisateur type);
}
