package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.models.requests.SwapRequest;
import org.teacherdistributionsystem.distribution_system.models.requests.TeacherSwapRequest;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResponse;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResult;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.ValidationResponse;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentPersistenceService;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentSwapService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentSwapController {

    private final AssignmentSwapService swapService;
    private final AssignmentPersistenceService assignmentPersistenceService;
    private final TeacherService teacherService;

    public AssignmentSwapController(AssignmentSwapService swapService, AssignmentPersistenceService assignmentPersistenceService, TeacherService teacherService) {
        this.swapService = swapService;
        this.assignmentPersistenceService = assignmentPersistenceService;
        this.teacherService = teacherService;
    }

    @PostMapping("/swap")
    public ResponseEntity<SwapResponse> swapAssignments(@RequestBody SwapRequest request) {
        try {
            TeacherSwapRequest t1 = request.getTeacher1();
            TeacherSwapRequest t2 = request.getTeacher2();

            Long assignmentId1 = assignmentPersistenceService.getAssignmentIdByTeacherDayAndSeance(
                    t1.getTeacherId(), t1.getDay(), t1.getSeance()
            );
            Long assignmentId2 = assignmentPersistenceService.getAssignmentIdByTeacherDayAndSeance(
                    t2.getTeacherId(), t2.getDay(), t2.getSeance()
            );

            if (assignmentId1.equals(assignmentId2)) {
                return ResponseEntity.badRequest()
                        .body(SwapResponse.error("Cannot swap an assignment with itself"));
            }



            SwapResult result = swapService.swapAssignments(
                    assignmentId1,
                   assignmentId2
            );

            if (result.isSuccess()) {


                //----------------------------------------------------------------------------sendEmail---------------------------------
                TeacherExamAssignment newAssignment1 = assignmentPersistenceService.getAssignmentById(assignmentId1);
                TeacherExamAssignment newAssignment2 = assignmentPersistenceService.getAssignmentById(assignmentId2);
                String email1=teacherService.getTeacherById(assignmentId1).getEmail();
                String email2=teacherService.getTeacherById(assignmentId2).getEmail();
                //sendEmail(newAssignment,1email1,,newAssignment2,email2)



                return ResponseEntity.ok(SwapResponse.success(result));
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(SwapResponse.failure(result));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SwapResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SwapResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }


    @GetMapping("/swap/validate")
    public ResponseEntity<ValidationResponse> validateSwap(
            @RequestParam Long assignmentId1,
            @RequestParam Long assignmentId2) {

        try {

            return ResponseEntity.ok(ValidationResponse.builder()
                    .message("Use POST /api/assignments/swap to attempt the swap")
                    .note("Validation is performed automatically during swap")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ValidationResponse.builder()
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }




}