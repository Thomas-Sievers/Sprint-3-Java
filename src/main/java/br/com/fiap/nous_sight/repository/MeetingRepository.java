package br.com.fiap.nous_sight.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.nous_sight.entity.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}
