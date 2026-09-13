package com.pothole.repository;

import com.pothole.model.Notification;
import com.pothole.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    List<Notification> findByUserAndReadStatusFalseOrderByCreatedAtDesc(User user);
    long countByUserAndReadStatusFalse(User user);
}
