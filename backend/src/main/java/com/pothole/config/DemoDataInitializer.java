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
        // Seed Citizen Demo Accounts
        if (userRepository.findByEmail("citizen@test.com").isEmpty()) {
            User citizen = new User(
                    "citizen@test.com",
                    passwordEncoder.encode("password123"),
                    "Test Citizen",
                    "+91 9876543200",
                    Role.ROLE_USER
            );
            userRepository.save(citizen);
            log.info("Seeded demo citizen account: citizen@test.com");
        }

        if (userRepository.findByEmail("aman.kumar@example.com").isEmpty()) {
            User citizen = new User(
                    "aman.kumar@example.com",
                    passwordEncoder.encode("password123"),
                    "Aman Kumar",
                    "+91 9876543210",
                    Role.ROLE_USER
            );
            userRepository.save(citizen);
            log.info("Seeded demo citizen account: aman.kumar@example.com");
        }

        // Seed Officer Demo Accounts
        if (userRepository.findByEmail("officer@test.com").isEmpty()) {
            User officerUser = new User(
                    "officer@test.com",
                    passwordEncoder.encode("officerPass123"),
                    "Test Officer",
                    "+91 9876543201",
                    Role.ROLE_OFFICER
            );
            officerUser = userRepository.save(officerUser);

            OfficerProfile profile = new OfficerProfile(
                    officerUser,
                    "Delhi PWD Central Circle",
                    "PWD-DL-9999",
                    "officer-docs/test_id_card.pdf"
            );
            profile.setVerificationStatus(VerificationStatus.VERIFIED);
            profile.setVerifiedAt(OffsetDateTime.now());
            profile.setVerifiedBy("SYSTEM_AUTO_SEED");
            profile = officerProfileRepository.save(profile);

            Point officePoint = geometryFactory.createPoint(new Coordinate(77.2200, 28.6200));
            OfficerJurisdiction jurisdiction = new OfficerJurisdiction(
                    profile,
                    "Delhi Central Circle Division",
                    officePoint,
                    15.0
            );
            officerJurisdictionRepository.save(jurisdiction);
            log.info("Seeded demo verified officer account: officer@test.com");
        }

        if (userRepository.findByEmail("officer.sharma@delhipwd.gov.in").isEmpty()) {
            User officerUser = new User(
                    "officer.sharma@delhipwd.gov.in",
                    passwordEncoder.encode("officerPass123"),
                    "Officer Rajesh Sharma",
                    "+91 9876543211",
                    Role.ROLE_OFFICER
            );
            officerUser = userRepository.save(officerUser);

            OfficerProfile profile = new OfficerProfile(
                    officerUser,
                    "Delhi PWD Central Circle",
                    "PWD-DL-8891",
                    "officer-docs/demo_id_card.pdf"
            );
            profile.setVerificationStatus(VerificationStatus.VERIFIED);
            profile.setVerifiedAt(OffsetDateTime.now());
            profile.setVerifiedBy("SYSTEM_AUTO_SEED");
            profile = officerProfileRepository.save(profile);

            Point officePoint = geometryFactory.createPoint(new Coordinate(77.2200, 28.6200));
            OfficerJurisdiction jurisdiction = new OfficerJurisdiction(
                    profile,
                    "Delhi Central Circle Division",
                    officePoint,
                    10.0
            );
            officerJurisdictionRepository.save(jurisdiction);
            log.info("Seeded demo verified officer account: officer.sharma@delhipwd.gov.in");
        }
    }
}
