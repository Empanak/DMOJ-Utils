package com.empanak.dmojutils.dmojDTO.contest;

import java.time.Instant;
import java.util.List;

public class Contest {
    public String key;
    public String name;
    public Instant start_time;
    public Instant end_time;
    public List<Ranking> rankings;
    public List<Problem> problems;
}
