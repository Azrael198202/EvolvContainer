package org.acme.evolv.factory.db;

import org.acme.evolv.DTO.ChatConfig;
import org.acme.evolv.interfaces.ChatConfigRepo;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.sql.*;
import java.util.UUID;

@ApplicationScoped
@Named("jdbcRepo")
public class ChatConfigRepoJdbc implements ChatConfigRepo {
    @Inject
    AgroalDataSource dataSource;

    @Override
    public ChatConfig findByCompanyId(String companyId) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("select * from chat_config where company_id=?")) {

            UUID id = UUID.fromString(companyId);
            ps.setObject(1, id, Types.OTHER);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return null;
            return new ChatConfig(
                    rs.getString("company_id"),
                    rs.getString("api_url"),
                    rs.getString("message_text"),
                    rs.getString("message_icon_url"),
                    rs.getString("header_icon_url"),
                    rs.getString("theme_primary"),
                    rs.getString("content_bg"),
                    rs.getString("footer_bg"),
                    rs.getString("text_color"),
                    rs.getString("bubble_user"),
                    rs.getString("bubble_bot"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
