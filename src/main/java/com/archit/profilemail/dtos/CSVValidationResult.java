package com.archit.profilemail.dtos;

import com.archit.profilemail.model.Profile;
import lombok.Builder;

import java.util.List;
@Builder
public class CSVValidationResult {
    private List<Profile> validProfiles;
    private List<Integer> invalidRows;

    public List<Profile> getValidProfiles() {
        return validProfiles;
    }
    public List<Integer> getInvalidRows(){
        return invalidRows;
    }

}
