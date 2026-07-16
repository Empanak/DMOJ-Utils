package com.empanak.dmojutils.dmojDTO.submissions;

import java.time.Instant;

public class Submission {
    public Long id;
    public String problem;
    public String user;
    public Instant date;
    public Float points;
    public String result;
    public Contest contest;
}
