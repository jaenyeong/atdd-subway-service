package nextstep.subway.line.domain;

import nextstep.subway.BaseEntity;
import nextstep.subway.station.domain.Station;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
public class Line extends BaseEntity {
    private static final String ERR_TEXT_ALREADY_ADDED_SECTION = "이미 등록된 구간 입니다.";
    private static final String ERR_TEXT_CAN_NOT_ADD_SECTION = "등록할 수 없는 구간 입니다.";
    private static final int MIN_LIMIT = 1;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    private String color;

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private final List<Section> sections = new ArrayList<>();

    public Line() {
    }

    public Line(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Line(String name, String color, Station upStation, Station downStation, int distance) {
        this.name = name;
        this.color = color;
        sections.add(new Section(this, upStation, downStation, distance));
    }

    public void update(Line line) {
        this.name = line.getName();
        this.color = line.getColor();
    }

    public List<Station> getStations() {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Station> stations = new ArrayList<>();
        Station downStation = findUpStation();
        stations.add(downStation);

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                .filter(it -> it.getUpStation() == finalDownStation)
                .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations;
    }

    public Station findUpStation() {
        Station downStation = sections.get(0).getUpStation();
        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                .filter(it -> it.getDownStation() == finalDownStation)
                .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getUpStation();
        }

        return downStation;
    }

    public void addSection(final Station upStation, final Station downStation, final int distance) {
        final List<Station> stations = getStations();

        final boolean isUpStationExisted = stations.stream().anyMatch(it -> it == upStation);
        final boolean isDownStationExisted = stations.stream().anyMatch(it -> it == downStation);

        if (isUpStationExisted && isDownStationExisted) {
            throw new RuntimeException(ERR_TEXT_ALREADY_ADDED_SECTION);
        }

        if (!stations.isEmpty() && stations.stream().noneMatch(it -> it == upStation) &&
            stations.stream().noneMatch(it -> it == downStation)) {
            throw new RuntimeException(ERR_TEXT_CAN_NOT_ADD_SECTION);
        }

        if (stations.isEmpty()) {
            sections.add(new Section(this, upStation, downStation, distance));
            return;
        }

        if (isUpStationExisted) {
            sections.stream()
                .filter(it -> it.getUpStation() == upStation)
                .findFirst()
                .ifPresent(it -> it.updateUpStation(downStation, distance));

            sections.add(new Section(this, upStation, downStation, distance));
        } else if (isDownStationExisted) {
            sections.stream()
                .filter(it -> it.getDownStation() == downStation)
                .findFirst()
                .ifPresent(it -> it.updateDownStation(upStation, distance));

            sections.add(new Section(this, upStation, downStation, distance));
        } else {
            throw new RuntimeException();
        }
    }

    public void removeSection(final Station station) {
        if (sections.size() <= MIN_LIMIT) {
            throw new RuntimeException();
        }

        final Optional<Section> upLineStation = sections.stream()
            .filter(it -> it.getUpStation() == station)
            .findFirst();
        final Optional<Section> downLineStation = sections.stream()
            .filter(it -> it.getDownStation() == station)
            .findFirst();

        if (upLineStation.isPresent() && downLineStation.isPresent()) {
            Station newUpStation = downLineStation.get().getUpStation();
            Station newDownStation = upLineStation.get().getDownStation();
            int newDistance = upLineStation.get().getDistance() + downLineStation.get().getDistance();
            sections.add(new Section(this, newUpStation, newDownStation, newDistance));
        }

        upLineStation.ifPresent(sections::remove);
        downLineStation.ifPresent(sections::remove);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public List<Section> getSections() {
        return sections;
    }
}
