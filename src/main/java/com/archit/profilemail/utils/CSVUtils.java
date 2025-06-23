package com.archit.profilemail.utils;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.ProfileRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CSVUtils {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    public List<Profile> extractValidProfiles(String csvContent, UserAccount owner) {
        String[] rows = csvContent.split("\n");
        if (rows.length < 2) return Collections.emptyList();

        String[] columns = rows[0].split(",");
        Profile[] allProfiles = createProfiles(columns, rows, owner);

        List<Profile> validProfiles = new ArrayList<>();
        for (int i = 0; i < allProfiles.length; i++) {
            Profile profile = allProfiles[i];
            if (profile == null) continue;
            String email = profile.getEmail();
            if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                System.out.println("Invalid email skipped at index " + i + ": " + email);
                continue;
            }

            validProfiles.add(profile);
        }

        return validProfiles;
    }

    public List<Profile> removeDuplicates(
            List<Profile> profiles,
            UserAccount owner,
            ProfileRepository profileRepository
    ) {
        List<String> emails = profiles.stream()
                .map(Profile::getEmail)
                .collect(Collectors.toList());

        List<Profile> existingProfiles = profileRepository.findByEmailInAndOwner(emails, owner);
        Set<String> existingEmails = existingProfiles.stream()
                .map(Profile::getEmail)
                .collect(Collectors.toSet());

        profiles.stream()
                .filter(p -> existingEmails.contains(p.getEmail()))
                .forEach(p -> System.out.println("Duplicate email skipped for " + owner.getEmail() + ": " + p.getEmail()));

        return profiles.stream()
                .filter(p -> !existingEmails.contains(p.getEmail()))
                .collect(Collectors.toList());
    }

    private Profile[] createProfiles(String[] columns, String[] rows, UserAccount owner){
        Profile[] profiles = new Profile[rows.length-1];
        for(int i=1;i<rows.length;i++){
            String[] rowData = rows[i].split(",");
            Profile temp = new Profile();
            temp.setEmail(rowData[0]);
            temp.setOwner(owner);
            setProperties(rowData,columns,temp);
            profiles[i-1]=temp;
        }
        return profiles;
    }
    private void setProperties(String[] rowData, String[] columns, Profile profile){
        List<ProfileProperty> props = new ArrayList<>();
        for(int i=1;i<columns.length;i++){
            ProfileProperty temp = new ProfileProperty();
            temp.setKey(columns[i]);
            temp.setValue(rowData[i]);
            temp.setProfile(profile);
            props.add(temp);
        }
        profile.setProperties(props);
    }
}
