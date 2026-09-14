package br.com.fiap.nous_sight.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.nous_sight.entity.Meeting;
import br.com.fiap.nous_sight.repository.MeetingRepository;

@Service
public class MeetingService {

    private final MeetingRepository repository;

    public MeetingService(MeetingRepository repository) {
        this.repository = repository;
    }

    public List<Meeting> list() {
        return repository.findAll();
    }

    public Meeting search(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found: " + id));
    }

    public Meeting save(Meeting meeting) {
        validate(meeting);
        return repository.save(meeting);
    }

    public Meeting update(Long id, Meeting meeting) {
        Meeting existing = search(id);
        existing.setTitle(meeting.getTitle());
        existing.setStartDateTime(meeting.getStartDateTime());
        existing.setEndDateTime(meeting.getEndDateTime());
        validate(existing);
        return repository.save(existing);
    }

    public void delete(Long id) {
        search(id);
        repository.deleteById(id);
    }

    private void validate(Meeting meeting) {
        validateEndAfterStart(meeting);
    }

    private void validateEndAfterStart(Meeting meeting) {
        if (meeting.getEndDateTime() != null && !meeting.getEndDateTime().isAfter(meeting.getStartDateTime())) {
            throw new RuntimeException("endDateTime must be after startDateTime");
        }
    }
}
