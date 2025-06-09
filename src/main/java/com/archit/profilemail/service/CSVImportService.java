package com.archit.profilemail.service;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.repository.ProfileRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CSVImportService {

    @Autowired
    private ProfileRepository profileRepository;

    public void importCSV(MultipartFile file) throws FileNotFoundException {
        if(file.isEmpty()){
            throw new FileNotFoundException("Empty file");
        }
        try{
            byte[] bytes = file.getBytes();
            String completeData = new String(bytes);
            String[] rows = completeData.split("\n");
            String[] columns = rows[0].split(",");
            Profile[] profiles = createProfile(columns,rows);
            profileRepository.saveAll(Arrays.asList(profiles));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private Profile[] createProfile(String[] columns, String[] rows){
        Profile[] profiles = new Profile[rows.length-1];
        for(int i=1;i<rows.length;i++){
            String[] rowData = rows[i].split(",");
            Profile temp = new Profile();
            temp.setEmail(rowData[0]);
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
