package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/harness-runs")
public class HarnessRunsWebController {

    private final HarnessWorkflowRunQueryService runQueryService;
    private final HarnessWorkflowCheckpointService checkpointService;

    public HarnessRunsWebController(
            HarnessWorkflowRunQueryService runQueryService,
            HarnessWorkflowCheckpointService checkpointService) {
        this.runQueryService = runQueryService;
        this.checkpointService = checkpointService;
    }

    @GetMapping
    public String harnessRunsPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        List<Map<String, Object>> runs = runQueryService.listAwaitingHumanReview(50);
        model.addAttribute("currentPage", "harness-runs");
        model.addAttribute("runs", runs);
        return "admin/harness-runs";
    }

    @PostMapping("/checkpoint")
    public String checkpoint(
            WebRequest request,
            @RequestParam String runId,
            @RequestParam String resumeToken,
            @RequestParam String decision,
            RedirectAttributes redirectAttributes) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        try {
            HarnessWorkflowCheckpointService.CheckpointAction action =
                    HarnessWorkflowCheckpointService.CheckpointAction.valueOf(decision.toUpperCase());
            checkpointService.checkpoint(runId, new HarnessWorkflowCheckpointService.CheckpointDecision(action, resumeToken));
            redirectAttributes.addFlashAttribute("successMessage", "Checkpoint " + action.name() + " for run " + runId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/harness-runs?user=admin";
    }
}
