package lhdt.admin.svc.cityplanning.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UpDownType {
    이상(0),
    이하(1);

    private Integer code;
}
