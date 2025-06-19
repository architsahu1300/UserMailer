package com.archit.profilemail.service;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.ProfileRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.utils.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CSVImportService {
    @Autowired
    private CSVUtils csvUtils;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    public void importCSV(MultipartFile file, String username) throws FileNotFoundException {
        if(file.isEmpty()){
            throw new FileNotFoundException("Empty file");
        }
        UserAccount owner = userAccountRepository.findByEmail(username);
        try{
            byte[] bytes = file.getBytes();
            String completeData = new String(bytes);
            String[] rows = completeData.split("\n");
            String[] columns = rows[0].split(",");
            Profile[] profiles = csvUtils.createProfile(columns, rows, owner);
            List<String> emails = Arrays.stream(profiles)
                    .map(Profile::getEmail)
                    .collect(Collectors.toList());
            List<Profile> existingProfiles = profileRepository.findByEmailInAndOwner(emails, owner);
            Set<String> existingEmails = existingProfiles.stream()
                    .map(Profile::getEmail)
                    .collect(Collectors.toSet());
            List<Profile> toSave = Arrays.stream(profiles)
                    .filter(p -> !existingEmails.contains(p.getEmail()))
                    .collect(Collectors.toList());
            emails.stream()
                    .filter(existingEmails::contains)
                    .forEach(email -> System.out.println("Duplicate email skipped for " + owner.getEmail() + ": " + email));

            profileRepository.saveAll(toSave);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
