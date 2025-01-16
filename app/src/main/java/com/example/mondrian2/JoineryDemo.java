package com.example.mondrian2;

import joinery.DataFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Create a new DataFrame
public class JoineryDemo {
        public static void main(String[] args) {
            // Create a new DataFrame
            DataFrame<Object> df = new DataFrame<>();
            String column = "sex";
            String column2 = "sex2";
            String column3 = "sex3";
            ArrayList newValues = new ArrayList();
            newValues.add("1");
            newValues.add("2");
            newValues.add("1");
            System.out.println(newValues);

            //System.out.println(Arrays.asList("John", "Alice", "Bob"));

            // Add data
            df.add(column.trim(), Arrays.asList("John", "Alice", "Bob"));
            df.add("Age", Arrays.asList(25, 30, 28));
            df.add("Salary", Arrays.asList(50000, 60000, 55000));
            df.add("value", newValues);

            System.out.println(df);
            df.add(column2.trim(), newValues);
            df.add(column3, newValues);
            //df.append(Arrays.asList("Bro", "2", "1"));
            System.out.println(df);





        }
}