package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller for doctor profile pages.
 */
@Slf4j
@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final ClinicalExperienceRepository clinicalExperienceRepository;

    public DoctorController(
            DoctorRepository doctorRepository,
            ClinicalExperienceRepository clinicalExperienceRepository) {
        this.doctorRepository = doctorRepository;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
    }

    @GetMapping("/{doctorId}")
    public String doctorProfile(
            @PathVariable String doctorId,
            Model model) {
        model.addAttribute("currentPage", "doctor");

        doctorRepository.findById(doctorId).ifPresentOrElse(
                doctor -> {
                    model.addAttribute("doctor", doctor);

                    // Load clinical experiences for this doctor
                    List<String> doctorIds = List.of(doctorId);
                    var experiences = clinicalExperienceRepository.findByDoctorIds(doctorIds);
                    model.addAttribute("experiences", experiences.getOrDefault(doctorId, List.of()));
                },
                () -> {
                    model.addAttribute("error", "Doctor not found: " + doctorId);
                }
        );

        return "doctors/profile";
    }
}
