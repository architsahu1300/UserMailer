package com.archit.profilemail.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CSVUtils {
    public static List<String[]> readCSVFile(MultipartFile file) throws IOException, CsvException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))){
            CSVReader csvReader = new CSVReader(reader);
            return csvReader.readAll();
        }
    }
}
