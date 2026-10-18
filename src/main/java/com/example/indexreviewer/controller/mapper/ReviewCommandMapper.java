package com.example.indexreviewer.controller.mapper;

import com.example.indexreviewer.config.MapStructConfig;
import com.example.indexreviewer.dto.csv.Composition;
import com.example.indexreviewer.dto.csv.SecurityData;
import com.example.indexreviewer.dto.csv.Universe;
import com.example.indexreviewer.dto.request.ConfigRequest;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.Security;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfig.class)
public interface ReviewCommandMapper {

    @Mapping(target = "indexType", source = "request.indexType")
    @Mapping(target = "cutoffDate", source = "request.cutoffDate")
    @Mapping(target = "reviewDate", source = "request.reviewDate")
    @Mapping(target = "universe", source = "universe")
    @Mapping(target = "securities", source = "securityData")
    @Mapping(target = "composition", source = "composition")
    ReviewCommand toServiceModel(ConfigRequest request, List<Universe> universe, List<SecurityData> securityData,
                                 List<Composition> composition);

    @Mapping(target = "securityId", source = "id")
    Security toSecurityRecord(SecurityData securityData);

    default Map<LocalDate, Set<Integer>> toUniverse(List<Universe> universe) {
        return universe.stream().collect(Collectors.groupingBy(Universe::getDate,
            Collectors.mapping(Universe::getId, Collectors.toSet())));
    }

    default Integer toSecurityId(Composition composition) {
        return composition.getId();
    }
}
