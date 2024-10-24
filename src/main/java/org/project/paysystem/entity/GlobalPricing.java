package org.project.paysystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="globalPricing")
public class GlobalPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int unitPrice;  // 광고 단가 (금액)

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private MediaTypeEnum type;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CurrencyEnum currency;  // 화폐 단위

    private Long minViews;  // 최소 조회수
    private Long maxViews; // 최대 조회수

    private LocalDate createdAt; // 단가가 설정된 날짜
    private LocalDate updatedAt; // 마지막으로 단가가 변경된 날짜
}