package com.boss.subscription.domain;
import com.boss.common.domain.TenantScopedEntity; import jakarta.persistence.*;
@Entity @Table(name="token_transactions") public class TokenTransaction extends TenantScopedEntity {
 @Column(nullable=false) private int amount; @Column(nullable=false,length=100) private String action; @Column(nullable=false,length=500) private String reason;
 protected TokenTransaction(){} public TokenTransaction(int amount,String action,String reason){this.amount=amount;this.action=action;this.reason=reason;} public int getAmount(){return amount;} public String getAction(){return action;} public String getReason(){return reason;}
}
