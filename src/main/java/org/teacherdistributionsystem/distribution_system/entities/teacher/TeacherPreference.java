package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;




@Entity
@Table(name = "teacher_preferences")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;


    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", length = 50, nullable = false)
    private PreferenceType preferenceType = PreferenceType.NOTHING;

    @Column(name = "priority_weight", columnDefinition = "INTEGER DEFAULT 0")
    private Integer priorityWeight = 0;

}
