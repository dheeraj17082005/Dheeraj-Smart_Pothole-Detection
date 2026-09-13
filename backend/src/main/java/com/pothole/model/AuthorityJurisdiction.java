package com.pothole.model;

import com.pothole.model.enums.JurisdictionType;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Geometry;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "authority_jurisdictions")
public class AuthorityJurisdiction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authority_id", nullable = false)
    private CivicAuthority authority;

    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction_type", nullable = false, length = 30)
    private JurisdictionType jurisdictionType;

    @Column(name = "geometry", columnDefinition = "geometry(Geometry, 4326)", nullable = false)
    private Geometry geometry;

    public AuthorityJurisdiction() {
    }

    public AuthorityJurisdiction(CivicAuthority authority, JurisdictionType jurisdictionType, Geometry geometry) {
        this.authority = authority;
        this.jurisdictionType = jurisdictionType;
        this.geometry = geometry;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CivicAuthority getAuthority() {
        return authority;
    }

    public void setAuthority(CivicAuthority authority) {
        this.authority = authority;
    }

    public JurisdictionType getJurisdictionType() {
        return jurisdictionType;
    }

    public void setJurisdictionType(JurisdictionType jurisdictionType) {
        this.jurisdictionType = jurisdictionType;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorityJurisdiction that = (AuthorityJurisdiction) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AuthorityJurisdiction{" +
                "id=" + id +
                ", jurisdictionType=" + jurisdictionType +
                '}';
    }
}
