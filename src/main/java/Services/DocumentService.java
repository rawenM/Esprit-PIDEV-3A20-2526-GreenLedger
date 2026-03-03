package Services;

import DataBase.MyConnection;
import Models.ProjectDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DocumentService {

    public String ensureProjectFolder(int projectId) throws IOException {
        Path base = Paths.get("uploads", "projects", String.valueOf(projectId));
        Files.createDirectories(base);
        return base.toString();
    }

    public ProjectDocument saveFile(int projectId, File file) throws IOException, SQLException {
        String folder = ensureProjectFolder(projectId);

        String originalName = file.getName();
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) ext = originalName.substring(dot);

        String storedName = UUID.randomUUID() + ext;
        Path target = Paths.get(folder, storedName);

        Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        String mime = Files.probeContentType(target);
        long size = Files.size(target);
        if (mime == null) mime = "application/octet-stream";
        boolean isImage = mime.startsWith("image/");

        ProjectDocument doc = new ProjectDocument();
        doc.setIdProjet(projectId);
        doc.setFileName(originalName);
        doc.setStoredName(storedName);
        doc.setFilePath(target.toString());
        doc.setMimeType(mime);
        doc.setFileSize(size);
        doc.setImage(isImage);

        try (Connection cnx = MyConnection.getConnection()) {
            insertMeta(cnx, doc);
        }
        return doc;
    }

    private void insertMeta(Connection cnx, ProjectDocument doc) throws SQLException {
        String sql =
                "INSERT INTO project_document (id_projet, file_name, stored_name, file_path, mime_type, file_size, is_image) " +
                        "VALUES (?,?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, doc.getIdProjet());
            ps.setString(2, doc.getFileName());
            ps.setString(3, doc.getStoredName());
            ps.setString(4, doc.getFilePath());
            ps.setString(5, doc.getMimeType());
            ps.setLong(6, doc.getFileSize());
            ps.setBoolean(7, doc.isImage());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) doc.setIdDocument(rs.getInt(1));
            }
        }
    }

    public List<ProjectDocument> getByProject(int projectId) {
        List<ProjectDocument> list = new ArrayList<>();
        String sql = "SELECT * FROM project_document WHERE id_projet=? ORDER BY uploaded_at DESC";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProjectDocument d = new ProjectDocument();
                    d.setIdDocument(rs.getInt("id_document"));
                    d.setIdProjet(rs.getInt("id_projet"));
                    d.setFileName(rs.getString("file_name"));
                    d.setStoredName(rs.getString("stored_name"));
                    d.setFilePath(rs.getString("file_path"));
                    d.setMimeType(rs.getString("mime_type"));
                    d.setFileSize(rs.getLong("file_size"));
                    d.setImage(rs.getBoolean("is_image"));
                    d.setUploadedAt(rs.getTimestamp("uploaded_at"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            System.out.println("DocumentService.getByProject error: " + e.getMessage());
        }
        return list;
    }
}