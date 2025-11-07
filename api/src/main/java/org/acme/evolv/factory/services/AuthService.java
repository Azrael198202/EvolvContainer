package org.acme.evolv.factory.services;

import java.util.HashMap;
import java.util.Map;

import org.acme.evolv.factory.repository.UserRepository;
import org.acme.evolv.utils.HashUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.evolv.entity.crm.*;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepo;

    public Map<String, Object> login(Map<String, Object> payload) {

        Map<String, Object> userData = (Map<String, Object>) payload.get("user");

        String email = (String) userData.get("email");
        String pwd = (String) userData.get("pwd");

        User user = userRepo.findByEmail(email);
        Map<String, Object> result = new HashMap<>();

        if (user == null) {
            result.put("result", "ng");
            result.put("message_code", "010");
        } else {
            if (!HashUtils.checkCryp(pwd, user.pwd)) {
                result.put("result", "ng");
                result.put("message_code", "011");
            }
        }

        result.put("result", "ok");
        result.put("message_code", "000");

        return result;
    }
}
