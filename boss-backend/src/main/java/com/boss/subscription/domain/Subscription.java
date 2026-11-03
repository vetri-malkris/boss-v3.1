package com.boss.subscription.domain;
import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="subscriptions") public class Subscription extends TenantScopedEntity {
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PlanType planType;
 @Column(nullable=false) private Instant startsAt; @Column(nullable=false) private Instant expiresAt;
 @Column(nullable=false) private int allocatedTokens; @Column(nullable=false) private int consumedTokens=0;
 @Column(nullable=false) private boolean active=true;
 protected Subscription(){} public Subscription(PlanType p,Instant s,Instant e,int t){planType=p;startsAt=s;expiresAt=e;allocatedTokens=t;}
 public PlanType getPlanType(){return planType;} public Instant getStartsAt(){return startsAt;} public Instant getExpiresAt(){return expiresAt;} public int getAllocatedTokens(){return allocatedTokens;} public int getConsumedTokens(){return consumedTokens;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public void consume(int n){if(n<0||consumedTokens+n>allocatedTokens)throw new IllegalStateException("Insufficient entitlement tokens");consumedTokens+=n;}
}
