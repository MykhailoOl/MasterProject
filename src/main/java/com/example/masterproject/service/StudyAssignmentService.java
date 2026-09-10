package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.StudyCondition;
import com.example.masterproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyAssignmentService {
    private final UserRepository users;
    private final UserContextService currentUser;
    private final AppLog log;
    public StudyAssignmentService(UserRepository users, UserContextService currentUser, AppLog log) {
        this.users = users; this.currentUser = currentUser; this.log = log;
    }
    @Transactional(readOnly = true)
    public StudyCondition assignment(Long id) {
        currentUser.requireAdmin();
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found")).getStudyCondition();
    }
    @Transactional
    public void assign(Long id, StudyCondition condition) {
        var admin = currentUser.requireAdmin();
        if (condition == null) throw new IllegalArgumentException("Choose an interview condition.");
        var user = users.findForUpdate(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if ("RANDOMIZED".equals(user.getAssignmentMethod())) {
            throw new IllegalStateException("A randomized assignment is fixed and cannot be overwritten.");
        }
        user.setStudyCondition(condition);
        user.setAssignmentMethod("MANUAL");
        log.info("STUDY", "event=assignment user=" + id + " condition=" + condition + " assigned_by=" + admin.getId());
    }
    @Transactional
    public void randomize(Long id) {
        var admin = currentUser.requireAdmin();
        var user = users.findForUpdate(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStudyCondition() != null) throw new IllegalStateException("This participant already has an assignment.");
        user.setStudyCondition(new java.security.SecureRandom().nextBoolean() ? StudyCondition.GUIDED : StudyCondition.BASELINE);
        user.setAssignmentMethod("RANDOMIZED");
        log.info("STUDY", "event=enrollment method=RANDOMIZED user=" + id + " condition=" + user.getStudyCondition()
                + " assigned_by=" + admin.getId());
    }
}
