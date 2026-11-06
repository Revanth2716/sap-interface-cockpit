package com.revzz.sap.cockpit.web;

import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.ReplayEntry;
import com.revzz.sap.cockpit.service.AuditService;
import com.revzz.sap.cockpit.service.InterfaceHealthService;
import com.revzz.sap.cockpit.service.ReplayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

/** The replay queue for failed messages. */
@Controller
@RequireRole({"OPS_ADMIN"})
public class ReplayController {

    private final ReplayService replayService;
    private final InterfaceHealthService healthService;
    private final AuditService auditService;

    public ReplayController(ReplayService replayService,
                            InterfaceHealthService healthService,
                            AuditService auditService) {
        this.replayService = replayService;
        this.healthService = healthService;
        this.auditService = auditService;
    }

    @GetMapping("/replay")
    public String replay(Model model) {
        model.addAttribute("queue", replayService.queue());
        model.addAttribute("pending", replayService.pending());

        // The failed messages that are not queued yet - the candidates.
        List<MessageRecord> candidates = new java.util.ArrayList<MessageRecord>();
        for (MessageRecord message : healthService.messagesInWindow()) {
            if (message.isError() && replayService.queue().stream()
                    .noneMatch(entry -> entry.getMessageId().equals(message.getMessageId()))) {
                candidates.add(message);
            }
        }
        model.addAttribute("candidates", candidates.size() > 20 ? candidates.subList(0, 20) : candidates);
        return "replay";
    }

    @PostMapping("/replay/queue")
    public String queue(@RequestParam String messageId, HttpSession session, Model model) {
        try {
            ReplayEntry entry = replayService.queue(messageId, CurrentUser.username(session));
            auditService.record(CurrentUser.username(session), "REPLAY_QUEUED",
                    "Message " + messageId + " queued for re-processing");
            return "redirect:/replay?queued=" + entry.getQueueId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return replay(model);
        }
    }

    @PostMapping("/replay/{queueId}/play")
    public String play(@PathVariable String queueId, HttpSession session, Model model) {
        try {
            ReplayEntry entry = replayService.play(queueId, CurrentUser.username(session));
            auditService.record(CurrentUser.username(session), "REPLAY_PLAYED",
                    "Queue entry " + queueId + " -> " + entry.getState());
            return "redirect:/replay?played=" + queueId;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return replay(model);
        }
    }
}
