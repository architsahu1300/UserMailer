package com.archit.profilemail.utils;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.model.UserAccount;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CSVUtils {
    public Profile[] createProfile(String[] columns, String[] rows, UserAccount owner){
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
