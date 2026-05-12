package Services;

import DataBase.MyConnection;
import Models.Budget;
import Models.Projet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService {

    private final Connection cnx;

    public ProjetService() {
        this.cnx = MyConnection.getConnection();
    }

    public List<Projet> afficher() {
        try (Connection cnx = MyConnection.getConnection()) {
            String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, p.statut, p.score_esg, " +
                    "       p.company_address, p.company_email, p.company_phone, " +
                    "       p.secteur, p.type_projet, p.localisation, p.date_creation, " +
<<<<<<< HEAD
=======
                    "       p.latitude, p.longitude, p.geocoded_at, p.air_quality_index, " +
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                    "       p.consommation_energie, p.unite_energie, p.distance_transport, " +
                    "       p.type_transport, p.type_materiau, p.quantite_materiau, " +
                    "       p.consommation_eau, p.dechets_generes, p.emissions_estimees, p.source_emissions, " +
                    "       p.fraud_risk_score, p.fraud_flag, p.fraud_reasons, " +
                    "       p.baseline_tco2, p.actual_tco2, p.avoided_tco2, " +
<<<<<<< HEAD
                    "       p.dispatched_green_credits, p.statut_financement, p.montant_demande, " +
=======
                    "       p.dispatched_green_credits, p.statut_financement, p.montant_demande, p.funded_at, " +
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                    "       b.id_budget, b.montant, b.raison, b.devise " +
                    "FROM projet p " +
                    "LEFT JOIN budget b ON b.id_projet = p.id " +
                    "ORDER BY p.date_creation DESC";

            List<Projet> list = new ArrayList<>();
            try (PreparedStatement ps = cnx.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProjetFull(rs));
                }
            } catch (SQLException e) {
                System.out.println("Erreur afficher projets: " + e.getMessage());
            }
            return list;
        } catch (SQLException e) {
            System.out.println("Erreur afficher projets (connexion): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Projet> getByEntreprise(int entrepriseId) {
        try (Connection cnx = MyConnection.getConnection()) {
            String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, p.statut, p.score_esg, " +
                    "       p.company_address, p.company_email, p.company_phone, " +
                    "       p.secteur, p.type_projet, p.localisation, p.date_creation, " +
<<<<<<< HEAD
=======
                    "       p.latitude, p.longitude, p.geocoded_at, p.air_quality_index, " +
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                    "       p.consommation_energie, p.unite_energie, p.distance_transport, " +
                    "       p.type_transport, p.type_materiau, p.quantite_materiau, " +
                    "       p.consommation_eau, p.dechets_generes, p.emissions_estimees, p.source_emissions, " +
                    "       p.fraud_risk_score, p.fraud_flag, p.fraud_reasons, " +
                    "       p.baseline_tco2, p.actual_tco2, p.avoided_tco2, " +
<<<<<<< HEAD
                    "       p.dispatched_green_credits, p.statut_financement, p.montant_demande, " +
=======
                    "       p.dispatched_green_credits, p.statut_financement, p.montant_demande, p.funded_at, " +
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                    "       b.id_budget, b.montant, b.raison, b.devise " +
                    "FROM projet p " +
                    "LEFT JOIN budget b ON b.id_projet = p.id " +
                    "WHERE p.entreprise_id=? " +
                    "ORDER BY p.date_creation DESC";

            List<Projet> list = new ArrayList<>();

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, entrepriseId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapProjetFull(rs));
                    }
                }

            } catch (SQLException e) {
                System.out.println("Erreur getByEntreprise: " + e.getMessage());
            }

            return list;
        } catch (SQLException e) {
            System.out.println("Erreur getByEntreprise (connexion): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Projet> getByEntreprise(Integer entrepriseId) {
        if (entrepriseId == null) return new ArrayList<>();
        return getByEntreprise(entrepriseId.intValue());
    }

    public Projet getById(int idProjet) {
        String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, p.statut, p.score_esg, " +
                "       p.company_address, p.company_email, p.company_phone, " +
                "       p.secteur, p.type_projet, p.localisation, p.date_creation, " +
<<<<<<< HEAD
=======
                "       p.latitude, p.longitude, p.geocoded_at, p.air_quality_index, " +
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                "       p.consommation_energie, p.unite_energie, p.distance_transport, " +
                "       p.type_transport, p.type_materiau, p.quantite_materiau, " +
                "       p.consommation_eau, p.dechets_generes, p.emissions_estimees, p.source_emissions, " +
                "       p.fraud_risk_score, p.fraud_flag, p.fraud_reasons, " +
                "       p.baseline_tco2, p.actual_tco2, p.avoided_tco2, " +
<<<<<<< HEAD
                "       p.dispatched_green_credits, p.statut_financement, p.montant_demande, " +
=======
                "       p.dispatched_green_credits, p.statut_financement, p.montant_demande, p.funded_at, " +
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                "       b.id_budget, b.montant, b.raison, b.devise " +
                "FROM projet p " +
                "LEFT JOIN budget b ON b.id_projet = p.id " +
                "WHERE p.id=?";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProjetFull(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur getById: " + e.getMessage());
        }
        return null;
    }

    public Projet getById(Integer idProjet) {
        if (idProjet == null) return null;
        return getById(idProjet.intValue());
    }

    public void insert(Projet p) {
        insertAndReturnId(p);
    }

    public int insertAndReturnId(Projet p) {
        if (p == null) return -1;

        // Only the fields visible in the creation form — no score_esg.
        // The database still requires `roi`, so we persist a neutral default (0).
        String sqlProjet =
                "INSERT INTO projet (entreprise_id, titre, description, statut, " +
                "company_address, company_email, company_phone, " +
                "secteur, type_projet, localisation, " +
                "consommation_energie, unite_energie, distance_transport, type_transport, " +
                "type_materiau, quantite_materiau, consommation_eau, dechets_generes, " +
                "emissions_estimees, source_emissions, montant_demande, roi, description_projet, " +
                "statut_financement, date_creation) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'NON_APPLICABLE',NOW())";

        // budget: roi defaults to 0
        String sqlBudget =
                "INSERT INTO budget (montant, raison, devise, id_projet, roi) VALUES (?,?,?,?,0)";

        try (Connection conn = MyConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = conn.prepareStatement(sqlProjet, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, p.getEntrepriseId());
                    ps.setString(2, p.getTitre());
                    ps.setString(3, p.getDescription());
                    ps.setString(4, safeStatut(p));
                    ps.setString(5, p.getCompanyAddress());
                    ps.setString(6, p.getCompanyEmail());
                    ps.setString(7, p.getCompanyPhone());
                    ps.setString(8,  p.getSecteur());
                    ps.setString(9,  p.getTypeProjet());
                    ps.setString(10, p.getLocalisation());
                    setDoubleOrNull(ps, 11, p.getConsommationEnergie());
                    ps.setString(12, p.getUniteEnergie());
                    setDoubleOrNull(ps, 13, p.getDistanceTransport());
                    ps.setString(14, p.getTypeTransport());
                    ps.setString(15, p.getTypeMateriau());
                    setDoubleOrNull(ps, 16, p.getQuantiteMateriau());
                    setDoubleOrNull(ps, 17, p.getConsommationEau());
                    setDoubleOrNull(ps, 18, p.getDechetsGeneres());
                    setDoubleOrNull(ps, 19, p.getEmissionsEstimees());
                    ps.setString(20, p.getSourceEmissions());
                    setDoubleOrNull(ps, 21, p.getMontantDemande());
                    ps.setDouble(22, 0.0);
                    ps.setString(23, p.getDescriptionProjet());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Insertion projet: aucune cle generee");
                        newId = keys.getInt(1);
                    }
                }

                // Try to insert budget — skip gracefully if table has extra NOT NULL columns
                try (PreparedStatement psB = conn.prepareStatement(sqlBudget)) {
                    Budget b = extractBudgetSafe(p);
                    psB.setDouble(1, b.getMontant());
                    psB.setString(2, b.getRaison());
                    psB.setString(3, normalizeDevise(b.getDevise()));
                    psB.setInt(4, newId);
                    psB.executeUpdate();
                } catch (SQLException budgetEx) {
                    // Budget insert failed (e.g. extra NOT NULL columns) — log and continue
                    // The project was created successfully; budget is optional
                    System.err.println("[ProjetService] Budget insert skipped: " + budgetEx.getMessage());
                }

                conn.commit();
                return newId;

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Erreur insertAndReturnId: " + e.getMessage());
                return -1;
            }
        } catch (SQLException e) {
            System.out.println("Erreur insertAndReturnId (connexion): " + e.getMessage());
            return -1;
        }
    }
    public void update(Projet p) {
        if (p == null) return;

        String current = getStatutById(p.getId());
        if (current == null) current = safeStatut(p);

        // Règles : si DRAFT => tout modifiable
        // sinon => seulement description + infos entreprise
        if ("DRAFT".equalsIgnoreCase(current)) {
            updateDraft(p);
        } else {
            updateDescriptionOnly(p.getId(),
                    p.getDescription(),
                    p.getCompanyAddress(),
                    p.getCompanyEmail(),
                    p.getCompanyPhone()
            );
        }
    }

    private void updateDraft(Projet p) {
        Budget b = extractBudgetSafe(p);

        String sqlProjet =
                "UPDATE projet SET titre=?, description=?, company_address=?, company_email=?, company_phone=?, " +
                "secteur=?, type_projet=?, localisation=?, " +
                "consommation_energie=?, unite_energie=?, distance_transport=?, type_transport=?, " +
                "type_materiau=?, quantite_materiau=?, consommation_eau=?, dechets_generes=?, " +
                "emissions_estimees=?, source_emissions=?, montant_demande=?, description_projet=? " +
                "WHERE id=?";

        String sqlBudget =
                "UPDATE budget SET montant=?, raison=?, devise=? WHERE id_projet=?";

        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement ps = cnx.prepareStatement(sqlProjet)) {
                ps.setString(1, p.getTitre());
                ps.setString(2, p.getDescription());
                ps.setString(3, p.getCompanyAddress());
                ps.setString(4, p.getCompanyEmail());
                ps.setString(5, p.getCompanyPhone());
                ps.setString(6, p.getSecteur());
                ps.setString(7, p.getTypeProjet());
                ps.setString(8, p.getLocalisation());
                setDoubleOrNull(ps, 9,  p.getConsommationEnergie());
                ps.setString(10, p.getUniteEnergie());
                setDoubleOrNull(ps, 11, p.getDistanceTransport());
                ps.setString(12, p.getTypeTransport());
                ps.setString(13, p.getTypeMateriau());
                setDoubleOrNull(ps, 14, p.getQuantiteMateriau());
                setDoubleOrNull(ps, 15, p.getConsommationEau());
                setDoubleOrNull(ps, 16, p.getDechetsGeneres());
                setDoubleOrNull(ps, 17, p.getEmissionsEstimees());
                ps.setString(18, p.getSourceEmissions());
                setDoubleOrNull(ps, 19, p.getMontantDemande());
                ps.setString(20, p.getDescriptionProjet());
                ps.setInt(21, p.getId());
                ps.executeUpdate();
            }

            int updated;
            try (PreparedStatement psB = cnx.prepareStatement(sqlBudget)) {
                psB.setDouble(1, b.getMontant());
                psB.setString(2, b.getRaison());
                psB.setString(3, normalizeDevise(b.getDevise()));
                psB.setInt(4, p.getId());
                updated = psB.executeUpdate();
            }

            if (updated == 0) {
                String ins = "INSERT INTO budget (montant, raison, devise, id_projet, roi) VALUES (?,?,?,?,0)";
                try (PreparedStatement psIns = cnx.prepareStatement(ins)) {
                    psIns.setDouble(1, b.getMontant());
                    psIns.setString(2, b.getRaison());
                    psIns.setString(3, normalizeDevise(b.getDevise()));
                    psIns.setInt(4, p.getId());
                    psIns.executeUpdate();
                }
            }

            cnx.commit();

        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("Erreur updateDraft: " + e.getMessage());
        } finally {
            setAutoCommitQuietly(true);
        }
    }


    public void updateDescriptionOnly(int id, String description, String address, String email, String phone) {
        String sql = "UPDATE projet SET description=?, company_address=?, company_email=?, company_phone=? WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setString(2, address);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateDescriptionOnly: " + e.getMessage());
        }
    }


    public void delete(int id) {
        String sql = "DELETE FROM projet WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur delete projet: " + e.getMessage());
        }
    }

    public void cancel(int id) {
        String statut = getStatutById(id);
        if (statut == null) statut = "";

        // Règle métier: si DRAFT => delete, sinon => CANCELLED
        if ("DRAFT".equalsIgnoreCase(statut)) {
            delete(id);
            return;
        }

        String sql = "UPDATE projet SET statut='CANCELLED' WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur cancel projet: " + e.getMessage());
        }
    }


    public boolean updateStatut(int idProjet, String statut) {
        // Bloquer SUBMITTED -> DRAFT
        String current = getStatutById(idProjet);
        if (current != null && "SUBMITTED".equalsIgnoreCase(current) && "DRAFT".equalsIgnoreCase(statut)) {
            return false;
        }

        String sql = "UPDATE projet SET statut=? WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, idProjet);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur updateStatut: " + e.getMessage());
            return false;
        }
    }

    public String getStatutById(int idProjet) {
        String sql = "SELECT statut FROM projet WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("statut");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getStatutById: " + e.getMessage());
        }
        return null;
    }

    // ✅ Overload attendu (Integer)
    public String getStatutById(Integer idProjet) {
        if (idProjet == null) return null;
        return getStatutById(idProjet.intValue());
    }


    private Projet mapProjetFull(ResultSet rs) throws SQLException {
        Projet p = new Projet();
        p.setId(rs.getInt("id"));
        p.setEntrepriseId(rs.getInt("entreprise_id"));
        p.setTitre(rs.getString("titre"));
        p.setDescription(rs.getString("description"));
        p.setStatutEvaluation(rs.getString("statut"));
        p.setScoreEsg((Integer) rs.getObject("score_esg"));
        p.setCompanyAddress(rs.getString("company_address"));
        p.setCompanyEmail(rs.getString("company_email"));
        p.setCompanyPhone(rs.getString("company_phone"));

        // Location & metadata
        safeSet(() -> p.setSecteur(rs.getString("secteur")));
        safeSet(() -> p.setTypeProjet(rs.getString("type_projet")));
        safeSet(() -> p.setLocalisation(rs.getString("localisation")));
        // date_creation — TIMESTAMP column, always present
        try {
            Timestamp ts = rs.getTimestamp("date_creation");
            if (ts != null) p.setDateCreation(ts.toLocalDateTime());
        } catch (Exception e) {
            System.err.println("[ProjetService] date_creation read failed: " + e.getMessage());
        }
<<<<<<< HEAD
=======
        // Geocoding fields
        safeSet(() -> p.setLatitude(getDoubleOrNull(rs, "latitude")));
        safeSet(() -> p.setLongitude(getDoubleOrNull(rs, "longitude")));
        safeSet(() -> {
            Timestamp gts = rs.getTimestamp("geocoded_at");
            if (gts != null) p.setGeocodedAt(gts.toLocalDateTime());
        });
        safeSet(() -> {
            Object aqi = rs.getObject("air_quality_index");
            if (aqi != null) p.setAirQualityIndex(((Number) aqi).intValue());
        });
        // funded_at
        safeSet(() -> {
            Timestamp fts = rs.getTimestamp("funded_at");
            if (fts != null) p.setFundedAt(fts.toLocalDateTime());
        });
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44

        // Environmental data
        safeSet(() -> p.setConsommationEnergie(getDoubleOrNull(rs, "consommation_energie")));
        safeSet(() -> p.setUniteEnergie(rs.getString("unite_energie")));
        safeSet(() -> p.setDistanceTransport(getDoubleOrNull(rs, "distance_transport")));
        safeSet(() -> p.setTypeTransport(rs.getString("type_transport")));
        safeSet(() -> p.setTypeMateriau(rs.getString("type_materiau")));
        safeSet(() -> p.setQuantiteMateriau(getDoubleOrNull(rs, "quantite_materiau")));
        safeSet(() -> p.setConsommationEau(getDoubleOrNull(rs, "consommation_eau")));
        safeSet(() -> p.setDechetsGeneres(getDoubleOrNull(rs, "dechets_generes")));
        safeSet(() -> p.setEmissionsEstimees(getDoubleOrNull(rs, "emissions_estimees")));
        safeSet(() -> p.setSourceEmissions(rs.getString("source_emissions")));

        // Fraud detection
        safeSet(() -> p.setFraudRiskScore(getDoubleOrNull(rs, "fraud_risk_score")));
        safeSet(() -> {
            Object flag = rs.getObject("fraud_flag");
            if (flag != null) p.setFraudFlag(((Number) flag).intValue() == 1);
        });
        safeSet(() -> p.setFraudReasons(rs.getString("fraud_reasons")));

        // Carbon metrics
        safeSet(() -> p.setBaselineTco2(getDoubleOrNull(rs, "baseline_tco2")));
        safeSet(() -> p.setActualTco2(getDoubleOrNull(rs, "actual_tco2")));
        safeSet(() -> p.setAvoidedTco2(getDoubleOrNull(rs, "avoided_tco2")));

        // Green credits
        safeSet(() -> p.setDispatchedGreenCredits(getDoubleOrNull(rs, "dispatched_green_credits")));

        // Financing
        safeSet(() -> p.setStatutFinancement(rs.getString("statut_financement")));
        safeSet(() -> p.setMontantDemande(getDoubleOrNull(rs, "montant_demande")));

        // Budget (LEFT JOIN)
        Object idBudget = rs.getObject("id_budget");
        if (idBudget != null) {
            Budget b = new Budget();
            b.setIdBudget(rs.getInt("id_budget"));
            b.setMontant(rs.getDouble("montant"));
            b.setRaison(rs.getString("raison"));
            b.setDevise(rs.getString("devise"));
            b.setIdProjet(p.getId());
            p.setBudget(b);
            try { p.setBudget(b.getMontant()); } catch (Exception ignored) {}
        }

        return p;
    }

    /** Silently ignore columns that don't exist in older DB schemas */
    private void safeSet(SqlRunnable r) {
        try { r.run(); } catch (Exception ignored) {}
    }

    private Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    @FunctionalInterface
    private interface SqlRunnable { void run() throws Exception; }

    private Projet mapProjetWithBudget(ResultSet rs) throws SQLException {
        Integer score = (Integer) rs.getObject("score_esg");

        Projet p = new Projet();
        p.setId(rs.getInt("id"));
        p.setEntrepriseId(rs.getInt("entreprise_id"));
        p.setTitre(rs.getString("titre"));
        p.setDescription(rs.getString("description"));
        p.setStatutEvaluation(rs.getString("statut"));
        p.setScoreEsg(score);
        p.setCompanyAddress(rs.getString("company_address"));
        p.setCompanyEmail(rs.getString("company_email"));
        p.setCompanyPhone(rs.getString("company_phone"));

        // budget (LEFT JOIN)
        Object idBudget = rs.getObject("id_budget");
        if (idBudget != null) {
            Budget b = new Budget();
            b.setIdBudget(rs.getInt("id_budget"));
            b.setMontant(rs.getDouble("montant"));
            b.setRaison(rs.getString("raison"));
            b.setDevise(rs.getString("devise"));
            b.setIdProjet(rs.getInt("id"));
            p.setBudget(b);

            // compat si tu as encore budget double dans Projet
            try { p.setBudget(b.getMontant()); } catch (Exception ignored) {}
        } else {
            p.setBudget(null);
            try { p.setBudget(0); } catch (Exception ignored) {}
        }

        return p;
    }

    private Budget extractBudgetSafe(Projet p) {
        Budget b = null;
        try { b = p.getBudgetObj(); } catch (Exception ignored) {}

        if (b == null) {
            b = new Budget();
            double montant = 0;
            try { montant = p.getBudget(); } catch (Exception ignored) {}
            b.setMontant(montant);
            b.setRaison("Budget");
            b.setDevise("TND");
        }

        if (b.getRaison() == null || b.getRaison().trim().isEmpty()) b.setRaison("Budget");
        if (b.getDevise() == null || b.getDevise().trim().isEmpty()) b.setDevise("TND");
        b.setDevise(normalizeDevise(b.getDevise()));
        return b;
    }

    private String safeStatut(Projet p) {
        String s = null;
        try { s = p.getStatutEvaluation(); } catch (Exception ignored) {}
        if (s == null) {
            try { s = p.getStatut(); } catch (Exception ignored) {}
        }
        if (s == null || s.trim().isEmpty()) return "DRAFT";
        return s.trim().toUpperCase();
    }

    private String normalizeDevise(String d) {
        if (d == null) return "TND";
        String v = d.trim().toUpperCase();
        if (v.equals("TND") || v.equals("EUR") || v.equals("USD")) return v;
        return "TND";
    }

    private void rollbackQuietly() {
        try { cnx.rollback(); } catch (Exception ignored) {}
    }

    private void setAutoCommitQuietly(boolean value) {
        try { cnx.setAutoCommit(value); } catch (Exception ignored) {}
    }

    private void setDoubleOrNull(PreparedStatement ps, int idx, Double v) throws java.sql.SQLException {
        if (v == null) ps.setNull(idx, Types.DOUBLE);
        else           ps.setDouble(idx, v);
    }
}