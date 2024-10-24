package org.project.paysystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="ad")
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adId; // 동영상에 삽입된 광고 ID
//    private Long adsViews; // 광고 조회수
    private String adUrl; // 광고 URL
}
