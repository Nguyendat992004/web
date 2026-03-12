package com.example.demo.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.time.Instant;

@Service
public class AuthService {

    // Sử dụng chung một thuật toán và mã bí mật (Secret Key) cho cả tạo và xác thực
    private final String SECRET_KEY = "my-secret-key";
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

    /**
     * Bước 1: Xác thực thông tin đăng nhập và Tạo Token
     */
    public String authenticate(String username, String password) {
        // Cấu hình kết nối Database (Lưu ý: Sẽ thay đổi khi triển khai lên Railway)
        String url = "jdbc:mysql://localhost:3306/simple";
        String dbUser = "root";
        String dbPass = "root";
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Nếu thông tin đúng, tiến hành tạo JWT
                    return JWT.create()
                            .withClaim("username", username) // Lưu thông tin người dùng
                            .withClaim("role", rs.getString("role")) // Lưu quyền hạn
                            .withExpiresAt(Instant.now().plusSeconds(3600)) // Hết hạn sau 1 giờ
                            .sign(algorithm); // Ký tên niêm phong
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Trả về null nếu đăng nhập thất bại
    }

    /**
     * Bước 2: Xác thực Token (Sử dụng tại "Người gác cổng" - Filter/Interceptor)
     */
    public String verifyToken(String token) {
        try {
            // Xây dựng bộ xác thực với thuật toán đã chọn
            JWTVerifier verifier = JWT.require(algorithm).build();

            // Giải mã và kiểm tra tính toàn vẹn của token
            // Nếu token sai hoặc hết hạn, hàm verify sẽ ném ra ngoại lệ (Exception)
            DecodedJWT jwt = verifier.verify(token);

            // Trích xuất lại thông tin username từ token
            return jwt.getClaim("username").asString();
        } catch (Exception e) {
            // Token không hợp lệ
            return null;
        }
    }
}