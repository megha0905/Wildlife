package com.wildlife.patrol.service;

import com.wildlife.patrol.client.ResourceClient;
import com.wildlife.patrol.client.StaffRefDto;
import com.wildlife.patrol.dto.PageResponse;
import com.wildlife.patrol.dto.TeamRequest;
import com.wildlife.patrol.dto.TeamResponse;
import com.wildlife.patrol.entity.PatrolTeam;
import com.wildlife.patrol.entity.TeamMember;
import com.wildlife.patrol.exception.BusinessRuleException;
import com.wildlife.patrol.exception.ConflictException;
import com.wildlife.patrol.exception.ResourceNotFoundException;
import com.wildlife.patrol.repository.ScheduleRepository;
import com.wildlife.patrol.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final ScheduleRepository scheduleRepository;
    private final ResourceClient resourceClient;

    public TeamService(TeamRepository teamRepository, ScheduleRepository scheduleRepository,
                       ResourceClient resourceClient) {
        this.teamRepository = teamRepository;
        this.scheduleRepository = scheduleRepository;
        this.resourceClient = resourceClient;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> search(Long areaId, Boolean active, Pageable pageable) {
        Specification<PatrolTeam> spec = Specification.where(null);
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("areaId"), areaId));
        if (active != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        return PageResponse.of(teamRepository.findAll(spec, pageable), t -> PatrolMapper.toTeam(t));
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long id) {
        return PatrolMapper.toTeam(find(id));
    }

    public TeamResponse create(TeamRequest req) {
        if (teamRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new ConflictException("A team named '" + req.name().trim() + "' already exists");
        }
        PatrolTeam team = new PatrolTeam();
        List<String> warnings = apply(team, req);
        return PatrolMapper.toTeam(teamRepository.save(team), warnings);
    }

    public TeamResponse update(Long id, TeamRequest req) {
        PatrolTeam team = find(id);
        if (teamRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ConflictException("A team named '" + req.name().trim() + "' already exists");
        }
        List<String> warnings = apply(team, req);
        return PatrolMapper.toTeam(team, warnings);
    }

    public void delete(Long id) {
        PatrolTeam team = find(id);
        if (scheduleRepository.countByTeamId(id) > 0) {
            throw new ConflictException("Team " + team.getName() + " has patrol schedules; deactivate it instead");
        }
        teamRepository.delete(team);
    }

    public PatrolTeam find(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patrol team", id));
    }

    /**
     * Looks up leader and members in resource-service.
     * Unknown staff -> 422. resource-service unreachable -> saved with a warning and no names.
     */
    private List<String> apply(PatrolTeam team, TeamRequest req) {
        Set<Long> memberIds = new LinkedHashSet<>(req.memberStaffIds());
        memberIds.add(req.leaderStaffId()); // the leader is always part of the team
        List<String> warnings = new ArrayList<>();

        Map<Long, StaffRefDto> found = new HashMap<>();
        try {
            List<Long> all = new ArrayList<>(memberIds);
            for (StaffRefDto s : resourceClient.lookupStaff(all)) {
                found.put(s.id(), s);
            }
            List<Long> missing = all.stream().filter(id -> !found.containsKey(id)).toList();
            if (!missing.isEmpty()) {
                throw new BusinessRuleException("Unknown staff id(s) in resource-service: " + missing);
            }
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.warn("resource-service unavailable while checking team staff: {}", e.getMessage());
            warnings.add("Staff could not be verified with resource-service; member names omitted.");
        }

        team.setName(req.name().trim());
        team.setAreaId(req.areaId());
        team.setLeaderStaffId(req.leaderStaffId());
        StaffRefDto leader = found.get(req.leaderStaffId());
        team.setLeaderName(leader != null ? leader.fullName() : null);
        team.getMembers().clear();
        for (Long id : memberIds) {
            StaffRefDto s = found.get(id);
            team.getMembers().add(new TeamMember(id, s != null ? s.fullName() : null, s != null ? s.designation() : null));
        }
        team.setActive(req.active() == null || req.active());
        return warnings;
    }
}
