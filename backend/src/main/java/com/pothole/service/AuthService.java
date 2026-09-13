package com.pothole.service;

import com.pothole.dto.*;
import com.pothole.model.*;
import com.pothole.model.enums.Role;
import com.pothole.model.enums.VerificationStatus;
import com.pothole.repository.*;
import com.pothole.security.JwtTokenProvider;
import com.pothole.service.storage.MinioStorageService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OfficerProfileRepository officerProfileRepository;
    private final OfficerJurisdictionRepository officerJurisdictionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final MinioStorageService minioStorageService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public static final String OFFICER_DOCS_BUCKET = "pothole-officer-docs";

    public AuthService(
            UserRepository userRepository,
            OfficerProfileRepository officerProfileRepository,
            OfficerJurisdictionRepository officerJurisdictionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            MinioStorageService minioStorageService
    ) {
        this.userRepository = userRepository;
        this.officerProfileRepository = officerProfileRepository;
        this.officerJurisdictionRepository = officerJurisdictionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.minioStorageService = minioStorageService;
    }

    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                Role.ROLE_USER
        );
        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), null);
    }

    @Transactional
    public AuthResponse registerOfficer(OfficerRegisterRequest request, MultipartFile idCardFile) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        if (idCardFile == null || idCardFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Officer ID card document is required");
        }

        String idCardKey = "id-cards/" + UUID.randomUUID() + "-" + idCardFile.getOriginalFilename();
        try {
            minioStorageService.ensureBucketExists(OFFICER_DOCS_BUCKET);
            minioStorageService.putObject(
                    OFFICER_DOCS_BUCKET,
                    idCardKey,
                    idCardFile.getInputStream(),
                    idCardFile.getSize(),
                    idCardFile.getContentType()
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload ID document", e);
        }

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                Role.ROLE_OFFICER
        );
        user = userRepository.save(user);

        OfficerProfile profile = new OfficerProfile(
                user,
                request.getDepartment(),
                request.getOfficerIdCode(),
                idCardKey
        );
        profile = officerProfileRepository.save(profile);

        Point officeLocation = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
        OfficerJurisdiction jurisdiction = new OfficerJurisdiction(
                profile,
                request.getJurisdictionName(),
                officeLocation,
                request.getRadiusKm() != null ? request.getRadiusKm() : 5.0
        );
        officerJurisdictionRepository.save(jurisdiction);

        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), profile.getVerificationStatus());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        VerificationStatus verificationStatus = null;
        if (user.getRole() == Role.ROLE_OFFICER) {
            verificationStatus = officerProfileRepository.findByUser(user)
                    .map(OfficerProfile::getVerificationStatus)
                    .orElse(VerificationStatus.PENDING_VERIFICATION);
        }

        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), verificationStatus);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
