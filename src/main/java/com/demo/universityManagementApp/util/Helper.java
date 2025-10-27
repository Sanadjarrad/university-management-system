package com.demo.universityManagementApp.util;

import lombok.experimental.UtilityClass;

public final class Helper {

    private Helper() {}

    public static class Validate {

        public static void validatePagination(final int pageNo, final int pageSize) throws IllegalArgumentException {
            if (pageNo < 0) throw new IllegalArgumentException("Page number must be >= 0");
            if (pageSize <= 0) throw new IllegalArgumentException("Page size must be > 0");
        }

        public static <T> T validateNotNull(final T value, final String fieldName) {
            if (value == null) throw new IllegalArgumentException(String.format("%s cannot be null",fieldName));

            return value;
        }

    }

    public static class Generate {

        public static String generateStudentEmail(final String name, final int enrollmentYear) {
            //Validate.validateStringNotNull(name, "name");
            String[] nameParts = name.trim().split("\\s+");
            String firstName = nameParts[0];
            String lastName = nameParts[nameParts.length - 1];

            char firstInitial = Character.toLowerCase(firstName.charAt(0));
            char middleInitial = (nameParts.length > 2) ? Character.toLowerCase(nameParts[1].charAt(0)) : firstInitial;

            String lowerLastName = lastName.toLowerCase();
            String yearSuffix = String.valueOf(enrollmentYear).substring(2);

            return String.format("%c%c%s%s@DigitinaryUniversity.com", firstInitial, middleInitial, lowerLastName, yearSuffix);
        }

        public static String generateLecturerEmail(final String name) {
            //Validate.validateStringNotNull(name, "name");
            String[] nameParts = name.trim().split("\\s+");
            String firstName = nameParts[0].toLowerCase();
            String lastName = nameParts[nameParts.length - 1].toLowerCase();

            return String.format("%s.%s@DigitinaryUniversity.com", firstName, lastName);
        }

    }
}
