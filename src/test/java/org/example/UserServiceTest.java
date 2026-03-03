package org.example;


import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Utils.PasswordUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Classe de test pour vérifier que tout fonctionne
 */
public class UserServiceTest {

    @Test
    public void testPasswordHashAndCheck() {
        String plain = "Admin@123";
        String hash = PasswordUtil.hashPassword(plain);
        Assert.assertNotNull(hash);
        Assert.assertTrue(PasswordUtil.checkPassword(plain, hash));
        Assert.assertFalse(PasswordUtil.checkPassword("wrong", hash));
    }

    @Test
    public void testPasswordStrengthValidation() {
        String weak = "abc";
        String strong = "Strong@123";

        Assert.assertNotNull(PasswordUtil.getPasswordErrorMessage(weak));
        Assert.assertNull(PasswordUtil.getPasswordErrorMessage(strong));
    }

    @Test
    public void testEnumsLibelle() {
        Assert.assertEquals("Investisseur", TypeUtilisateur.INVESTISSEUR.getLibelle());
        Assert.assertEquals("Active", StatutUtilisateur.ACTIVE.getLibelle());
    }
}
