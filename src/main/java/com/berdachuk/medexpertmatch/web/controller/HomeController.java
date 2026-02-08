package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the home page (dashboard).
 */
@Slf4j
@Controller
public class HomeController {

    private final DoctorRepository doctorRepository;
    private final MedicalCaseRepository medicalCaseRepository;
    private final ConsultationMatchRepository consultationMatchRepository;

    public HomeController(
            DoctorRepository doctorRepository,
            MedicalCaseRepository medicalCaseRepository,
            ConsultationMatchRepository consultationMatchRepository) {
        this.doctorRepository = doctorRepository;
        this.medicalCaseRepository = medicalCaseRepository;
        this.consultationMatchRepository = consultationMatchRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentPage", "index");

        // Get statistics
        int doctorCount = doctorRepository.findAllIds(0).size();
        int caseCount = medicalCaseRepository.findAllIds(0).size();

        // Count pending consultation requests (cases with CONSULT_REQUEST type)
        // Use Integer.MAX_VALUE to get all results when counting
        int pendingConsultsCount = medicalCaseRepository.findByCaseType(
                CaseType.CONSULT_REQUEST.name(), Integer.MAX_VALUE).size();

        int matchesCount = (int) consultationMatchRepository.count();

        model.addAttribute("doctorCount", doctorCount);
        model.addAttribute("caseCount", caseCount);
        model.addAttribute("pendingConsultsCount", pendingConsultsCount);
        model.addAttribute("matchesCount", matchesCount);

        return "index";
    }
}
