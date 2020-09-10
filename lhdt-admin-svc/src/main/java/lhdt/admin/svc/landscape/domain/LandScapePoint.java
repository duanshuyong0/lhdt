package lhdt.admin.svc.landscape.domain;

import com.sun.istack.NotNull;
import lhdt.admin.svc.landscape.type.LandScapeAnalsType;
import lhdt.admin.svc.landscape.type.LSPointActionType;
import lhdt.ds.common.domain.DsDomain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.geo.Point;

import javax.persistence.*;

@Entity
@Table(name="landscape_anals")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LandScapePoint extends DsDomain {
    @NotNull
    @Column(name = "land_scape_point_name")
    private String landScapePointName;

    @NotNull
    @Column(name = "start_land_scape")
    private Point startLandScapePos;

    @Column(name = "end_land_scape")
    private Point endLandScapePos;

    @Column(name = "start_alt")
    private Double startAltitude;

    @Column(name = "end_alt")
    private Double endAltitude;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    LandScapeAnalsType landScapePointType;

    LSPointActionType LSPointActionType;
}