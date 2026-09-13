package com.pothole.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.time.OffsetDateTime;

@Entity
@Table(name = "officer_jurisdictions")
public class OfficerJurisdiction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_profile_id", nullable = false)
    private OfficerProfile officerProfile;

    @Column(name = "jurisdiction_name", nullable = false)
    private String jurisdictionName;

    @Column(name = "office_location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point officeLocation;

    @Column(name = "radius_km", nullable = false)
    private Double radiusKm = 5.0;

    @Column(name = "boundary_polygon", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon boundaryPolygon;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public OfficerJurisdiction() {
    }

    public OfficerJurisdiction(OfficerProfile officerProfile, String jurisdictionName, Point officeLocation, Double radiusKm) {
        this.officerProfile = officerProfile;
        this.jurisdictionName = jurisdictionName;
        this.officeLocation = officeLocation;
        this.radiusKm = radiusKm;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OfficerProfile getOfficerProfile() {
        return officerProfile;
    }

    public void setOfficerProfile(OfficerProfile officerProfile) {
        this.officerProfile = officerProfile;
    }

    public String getJurisdictionName() {
        return jurisdictionName;
    }

    public void setJurisdictionName(String jurisdictionName) {
        this.jurisdictionName = jurisdictionName;
    }

    public Point getOfficeLocation() {
        return officeLocation;
    }

    public void setOfficeLocation(Point officeLocation) {
        this.officeLocation = officeLocation;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public Polygon getBoundaryPolygon() {
        return boundaryPolygon;
    }

    public void setBoundaryPolygon(Polygon boundaryPolygon) {
        this.boundaryPolygon = boundaryPolygon;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfficerJurisdiction that = (OfficerJurisdiction) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
