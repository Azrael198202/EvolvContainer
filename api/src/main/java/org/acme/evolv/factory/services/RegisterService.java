package org.acme.evolv.factory.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.evolv.entity.crm.*;
import org.acme.evolv.factory.repository.*;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class RegisterService {

    @Inject UserRepository userRepo;
    @Inject CompanyRepository companyRepo;
    @Inject UserCompanyRepository ucRepo;

    @Transactional
    public Map<String, Object> register(Map<String, Object> payload) {
        Map<String, Object> userData = (Map<String, Object>) payload.get("user");
        Map<String, Object> companyData = (Map<String, Object>) payload.get("company");

        if (userData == null || companyData == null) {
            throw new IllegalArgumentException("Missing user or company info");
        }

        String email = (String) userData.get("email");
        String name = (String) userData.get("name");
        String provider = (String) userData.getOrDefault("provider", "manual");
        String phone = (String) userData.get("phone");

        String companyName = (String) companyData.get("name");
        String companyAddr = (String) companyData.get("address");

        // 1. 查找或创建公司
        Company company = companyRepo.findByName(companyName);
        if (company == null) {
            company = new Company();
            company.name = companyName;
            company.address = companyAddr;
            company.persist();
        }

        // 2. 查找或创建用户
        User user = userRepo.findByEmail(email);
        if (user == null) {
            user = new User();
            user.email = email;
            user.name = name;
            user.phone = phone;
            user.provider = provider;
            user.profileCompleted = true;
            user.persist();
        }

        // 3. 建立用户-公司关系（如已存在则跳过）
        boolean exists = ucRepo.find("user.id = ?1 and company.id = ?2", user.id, company.id).firstResult() != null;
        if (!exists) {
            UserCompany uc = new UserCompany();
            uc.user = user;
            uc.company = company;
            uc.role = "owner";
            uc.persist();
        }

        // 4. 返回信息
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uuid", user.uuid.toString());
        userMap.put("name", user.name);
        userMap.put("email", user.email);
        result.put("companyUuid", company);
        result.put("user", userMap);
        return result;
    }
}