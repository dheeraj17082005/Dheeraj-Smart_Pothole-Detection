package com.pothole.config;

import com.pothole.model.*;
import com.pothole.model.enums.Role;
import com.pothole.model.enums.VerificationStatus;
import com.pothole.repository.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final OfficerProfileRepository officerProfileRepository;
    private final OfficerJurisdictionRepository officerJurisdictionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public DemoDataInitializer(
            UserRepository userRepository,
            OfficerProfileRepository officerProfileRepository,
            OfficerJurisdictionRepository officerJurisdictionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.officerProfileRepository = officerProfileRepository;
        this.officerJurisdictionRepository = officerJurisdictionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed or update Citizen Demo Accounts
        seedOrUpdateCitizen("aman.kumar@example.com", "password123", "Aman Kumar", "+91 9876543210");
        seedOrUpdateCitizen("citizen@test.com", "password123", "Test Citizen", "+91 9876543200");

        // 2. Seed or update Officer Demo Accounts
        seedOrUpdateOfficer("officer.sharma@delhipwd.gov.in", "officerPass123", "Officer Rajesh Sharma", "+91 9876543211", "PWD-DL-8891");
        seedOrUpdateOfficer("officer@test.com", "officerPass123", "Test Officer", "+91 9876543201", "PWD-DL-9999");
    }

    private void seedOrUpdateCitizen(String email, String rawPassword, String name, String phone) {
        User citizen = userRepository.findByEmail(email).orElseGet(() -> new User(
                email,
                passwordEncoder.encode(rawPassword),
                name,
                phone,
                Role.ROLE_USER
        ));
        citizen.setPasswordHash(passwordEncoder.encode(rawPassword));
        citizen.setRole(Role.ROLE_USER);
        userRepository.save(citizen);
        log.info("Ensured demo citizen account: {} with password: {}", email, rawPassword);
    }

    private void seedOrUpdateOfficer(String email, String rawPassword, String name, String phone, String badgeNumber) {
        User officerUser = userRepository.findByEmail(email).orElseGet(() -> new User(
                email,
                passwordEncoder.encode(rawPassword),
                name,
                phone,
                Role.ROLE_OFFICER
        ));
        officerUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        officerUser.setRole(Role.ROLE_OFFICER);
        officerUser = userRepository.save(officerUser);

        final User finalUser = officerUser;
        OfficerProfile profile = officerProfileRepository.findByUser(officerUser).orElseGet(() -> {
            OfficerProfile p = new OfficerProfile(
                    finalUser,
                    "Delhi PWD Central Circle",
                    badgeNumber,
                    "officer-docs/demo_id_card.pdf"
            );
            p.setVerificationStatus(VerificationStatus.VERIFIED);
            p.setVerifiedAt(OffsetDateTime.now());
            p.setVerifiedBy("SYSTEM_AUTO_SEED");
            return officerProfileRepository.save(p);
        });
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        if (profile.getVerifiedAt() == null) {
            profile.setVerifiedAt(OffsetDateTime.now());
        }
        profile = officerProfileRepository.save(profile);

        if (officerJurisdictionRepository.findByOfficerProfileAndActiveTrue(profile).isEmpty()) {
            Point officePoint = geometryFactory.createPoint(new Coordinate(77.2200, 28.6200));
            OfficerJurisdiction jurisdiction = new OfficerJurisdiction(
                    profile,
                    "Delhi Central Circle Division",
                    officePoint,
                    15.0
            );
            officerJurisdictionRepository.save(jurisdiction);
        }
        log.info("Ensured demo verified officer account: {} with password: {}", email, rawPassword);
    }
}
