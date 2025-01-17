package com.optum.admiral.type;

public class VerifiedPathVariable {
    public String name;
    public ENFORCEMENT trailing_file_separator;
    public String description;
    public String markerfile_name;
    public String markerfile_md5;

    public enum ENFORCEMENT {
        REQUIRED,
        FORBIDDEN;
    }
}
