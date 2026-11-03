package com.boss.subscription.controller;
import com.boss.common.security.TenantContext; import com.boss.subscription.domain.*; import com.boss.subscription.repository.SubscriptionRepository; import org.springframework.web.bind.annotation.*; import java.time.*;
@RestController @RequestMapping("/api/v1/subscription") public class SubscriptionController { private final SubscriptionRepository repo; public SubscriptionController(SubscriptionRepository r){repo=r;}
 @GetMapping public Object current(){return repo.findFirstByTenantIdAndActiveTrueOrderByExpiresAtDesc(TenantContext.requireTenantId()).orElse(null);}
 @PostMapping public Subscription create(@RequestParam PlanType plan){var now=Instant.now();var end=plan==PlanType.MONTHLY?now.plus(30,java.time.temporal.ChronoUnit.DAYS):now.plus(365,java.time.temporal.ChronoUnit.DAYS);var s=new Subscription(plan,now,end,plan==PlanType.MONTHLY?5:75);s.setTenantId(TenantContext.requireTenantId());return repo.save(s);}
}
