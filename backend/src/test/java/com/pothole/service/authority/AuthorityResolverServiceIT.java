package com.pothole.service.authority;

import com.pothole.model.CivicAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class AuthorityResolverServiceIT {

    @Container
    static PostgreSQLContainer<?> postgisContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("potholedb")
            .withUsername("pothole_user")
            .withPassword("pothole_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        if (postgisContainer.isRunning()) {
            registry.add("spring.datasource.url", postgisContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgisContainer::getUsername);
            registry.add("spring.datasource.password", postgisContainer::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }

    @Autowired
    private AuthorityResolverService authorityResolverService;

    @Test
    @DisplayName("1. Point directly on / near PWD road network resolves to PWD")
    void testPointOnPwdRoadNetwork() {
        // PWD line is MULTILINESTRING((77.2000 28.6000, 77.2200 28.6200, 77.2400 28.6400))
        // Point exactly on PWD line: (77.2200, 28.6200) -> Lat: 28.6200, Lon: 77.2200
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(28.6200, 77.2200);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.authority()).isNotNull();
        assertThat(result.authority().getCode()).isEqualTo("DEMO_PWD_ARTERIAL");
        assertThat(result.authority().getDepartmentType()).isEqualTo("PWD");
        assertThat(result.authorityCode()).isEqualTo("DEMO_PWD_ARTERIAL");
    }

    @Test
    @DisplayName("2. Point directly on / near NHAI highway road network resolves to NHAI")
    void testPointOnNhaiHighwayRoadNetwork() {
        // NHAI line is MULTILINESTRING((77.1200 28.7000, 77.1300 28.7200, 77.1400 28.7400))
        // Point on NHAI line: (77.1300, 28.7200) -> Lat: 28.7200, Lon: 77.1300
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(28.7200, 77.1300);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.authority()).isNotNull();
        assertThat(result.authority().getCode()).isEqualTo("DEMO_NHAI_HIGHWAY");
        assertThat(result.authority().getDepartmentType()).isEqualTo("HIGHWAY");
        assertThat(result.authorityCode()).isEqualTo("DEMO_NHAI_HIGHWAY");
    }

    @Test
    @DisplayName("3. Point not near road network but inside municipal polygon resolves to Municipal Authority")
    void testPointInsideMunicipalBoundary() {
        // NDMC polygon: POLYGON((77.1800 28.5800, 77.2600 28.5800, 77.2600 28.6600, 77.1800 28.6600, 77.1800 28.5800))
        // Point at (77.2500, 28.5900) is far (>500m) from PWD line but inside NDMC polygon
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(28.5900, 77.2500);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.authority()).isNotNull();
        assertThat(result.authority().getCode()).isEqualTo("DEMO_NDMC_CENTRAL");
        assertThat(result.authority().getDepartmentType()).isEqualTo("MUNICIPAL");
        assertThat(result.authorityCode()).isEqualTo("DEMO_NDMC_CENTRAL");
    }

    @Test
    @DisplayName("4. Point on polygon boundary edge resolves to Municipal Authority via ST_Covers")
    void testPointOnMunicipalPolygonBoundary() {
        // NDMC polygon boundary bottom-left corner: (77.1800, 28.5800) -> Lat: 28.5800, Lon: 77.1800
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(28.5800, 77.1800);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.authority()).isNotNull();
        assertThat(result.authority().getCode()).isEqualTo("DEMO_NDMC_CENTRAL");
    }

    @Test
    @DisplayName("5. Point outside all known geometries resolves to UNKNOWN_AUTHORITY")
    void testPointOutsideAllGeometries() {
        // Point far outside Delhi (e.g. Lat: 12.9716, Lon: 77.5946 - Bengaluru)
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(12.9716, 77.5946);

        assertThat(result.isResolved()).isFalse();
        assertThat(result.authority()).isNull();
        assertThat(result.authorityCode()).isEqualTo(AuthorityResolutionResult.UNKNOWN_AUTHORITY_CODE);
    }

    @Test
    @DisplayName("6. Dedicated road authority takes precedence over municipal boundary when point is on road")
    void testRoadTakesPrecedenceOverMunicipal() {
        // PWD line at (77.2200, 28.6200) is inside NDMC polygon (77.18..77.26, 28.58..28.66)
        // Road hierarchy MUST return PWD, not NDMC!
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(28.6200, 77.2200);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.authority().getCode()).isEqualTo("DEMO_PWD_ARTERIAL");
    }

    @Test
    @DisplayName("7. 15-meter road matching behaves in metric distance")
    void testFifteenMeterProximityInMeters() {
        // PWD line point: Lat 28.6200, Lon 77.2200
        // At latitude 28.62 deg, 1 second of latitude is ~30.8 meters.
        // 0.00008 degrees latitude is ~8.9 meters -> Should match PWD (within 15m)
        AuthorityResolutionResult closeResult = authorityResolverService.resolveAuthority(28.62008, 77.2200);
        assertThat(closeResult.isResolved()).isTrue();
        assertThat(closeResult.authority().getCode()).isEqualTo("DEMO_PWD_ARTERIAL");

        // 0.0006 degrees latitude is ~66.5 meters -> Exceeds 15m road buffer, should fall back to NDMC municipal boundary
        AuthorityResolutionResult farResult = authorityResolverService.resolveAuthority(28.6206, 77.2200);
        assertThat(farResult.isResolved()).isTrue();
        assertThat(farResult.authority().getCode()).isEqualTo("DEMO_NDMC_CENTRAL");
    }

    @Test
    @DisplayName("8. Coordinate ordering (Lat, Lon vs Lon, Lat) is handled correctly")
    void testCoordinateOrdering() {
        // Ensure passing latitude (28.7200) and longitude (77.1300) correctly queries (Lon: 77.1300, Lat: 28.7200)
        AuthorityResolutionResult result = authorityResolverService.resolveAuthority(28.7200, 77.1300);
        assertThat(result.authority().getCode()).isEqualTo("DEMO_NHAI_HIGHWAY");
    }
}
