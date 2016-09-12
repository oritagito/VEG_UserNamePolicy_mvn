package com.oim.gis.veg.utils;

import oracle.iam.identity.exception.UserNameGenerationException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.utils.UserNameGenerationUtil;
import oracle.iam.identity.usermgmt.utils.UserNamePolicyUtil;
import oracle.iam.platform.Platform;

import java.util.List;

public class Utils {

    private static UserManager userManager;

    private static String cyrillicToLatinMapping(char c) {
        switch (c) {
            case 'А':
                return "A";
            case 'Б':
                return "B";
            case 'В':
                return "V";
            case 'Г':
                return "G";
            case 'Д':
                return "D";
            case 'Е':
                return "E";
            case 'Ё':
                return "E";
            case 'Ж':
                return "ZH";
            case 'З':
                return "Z";
            case 'И':
                return "I";
            case 'Й':
                return "Y";
            case 'К':
                return "K";
            case 'Л':
                return "L";
            case 'М':
                return "M";
            case 'Н':
                return "N";
            case 'О':
                return "O";
            case 'П':
                return "P";
            case 'Р':
                return "R";
            case 'С':
                return "S";
            case 'Т':
                return "T";
            case 'У':
                return "U";
            case 'Ф':
                return "F";
            case 'Х':
                return "H";
            case 'Ц':
                return "C";
            case 'Ч':
                return "CH";
            case 'Ш':
                return "SH";
            case 'Щ':
                return "SH";
            case 'Ъ':
                return "";
            case 'Ы':
                return "Y";
            case 'Ь':
                return "";
            case 'Э':
                return "E";
            case 'Ю':
                return "YU";
            case 'Я':
                return "YA";
            default:
                return String.valueOf(c);
        }
    }

    private static String cyrillicToLatinResult(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);

        if (s.toLowerCase().matches(".+(ий)$")) {
            StringBuilder sbr = new StringBuilder(s);
            sbr.deleteCharAt(sbr.length() - 2);
            s = sbr.toString();
        }
        for (char c : s.toCharArray()) {
            sb.append(cyrillicToLatinMapping(c));
        }
        return sb.toString();
    }

    private static String generateNextName(String lastName, String firstInitial, String middleInitial, int index) {
        if (index == 1) {
            if (firstInitial != null && middleInitial != null)
                return (firstInitial.concat(middleInitial)).concat(lastName);
            if (firstInitial != null && middleInitial == null)
                return (firstInitial.concat(lastName)).concat(Integer.toString(index));
            if (firstInitial == null && middleInitial != null)
                return (middleInitial.concat(lastName)).concat(Integer.toString(index));
            else
                return lastName.concat(Integer.toString(index));
        } else if (index == 2) {
            return lastName;
        } else {
            if (firstInitial != null)
                return (firstInitial.concat(lastName)).concat(Integer.toString(index - 2));
            else
                return lastName.concat(Integer.toString(index - 2));
        }
    }

    public static boolean isContainInvalidCharacters(String inputString) {

        String NAME_REGEX = "^[a-z,A-Z,а-я,А-Я]+|[a-z,A-Z,а-я,А-Я]+[\\s,\\-]?[a-z,A-Z,а-я,А-Я]+";

        boolean isNameValid = false;

        if (inputString != null && !inputString.equalsIgnoreCase("")) {
            isNameValid = !inputString.matches(NAME_REGEX);
        }

        return isNameValid;
    }

    public static String generateName(String firstName, String lastName, String middleName) throws UserNameGenerationException {

        String login;

        String firstInitial = null;
        String middleInitial = null;

        boolean isFirstName = !(firstName == null || firstName.length() == 0);
        boolean isMiddleName = !(middleName == null || middleName.length() == 0);

        if (isFirstName) {
            if (!Utils.isContainInvalidCharacters(firstName))
                firstInitial = cyrillicToLatinResult((firstName.substring(0, 1)).toUpperCase());
            else
                isFirstName = false;
        }

        if (isMiddleName) {
            if (!Utils.isContainInvalidCharacters(middleName))
                middleInitial = cyrillicToLatinResult((middleName.substring(0, 1)).toUpperCase());
        }

        lastName = cyrillicToLatinResult(lastName.toUpperCase());

        if (isFirstName) {
            login = firstInitial.concat(lastName);
            login = UserNameGenerationUtil.trimWhiteSpaces(login);
        } else {
            login = lastName;
            login = UserNameGenerationUtil.trimWhiteSpaces(login);
        }

        if (UserNamePolicyUtil.isUserExists(login) || (UserNamePolicyUtil.isUserNameReserved(login))) {
            boolean userNameGenerated = false;
            for (int j = 1; j < 100; j++) {
                login = generateNextName(lastName, firstInitial, middleInitial, j);
                if (UserNameGenerationUtil.isUserNameExistingOrReserved(login))
                    continue;
                userNameGenerated = true;
                break;
            }
            if (!userNameGenerated)
                throw new UserNameGenerationException("Failed To Generate User Name", "GenerateUserNameFailed");
        }
        return login;
    }

    public static String generateNameTest(String firstName, String lastName, String middleName, List<String> userExists) throws UserNameGenerationException {

        String login;

        String firstInitial = null;
        String middleInitial = null;

        boolean isFirstName = !(firstName == null || firstName.length() == 0);
        boolean isMiddleName = !(middleName == null || middleName.length() == 0);

        if (isFirstName) {
            if (!Utils.isContainInvalidCharacters(firstName))
                firstInitial = cyrillicToLatinResult((firstName.substring(0, 1)).toUpperCase());
            else
                isFirstName = false;
        }

        if (isMiddleName) {
            if (!Utils.isContainInvalidCharacters(middleName))
                middleInitial = cyrillicToLatinResult((middleName.substring(0, 1)).toUpperCase());
        }

        if (!Utils.isContainInvalidCharacters(lastName))
            lastName = cyrillicToLatinResult(lastName.toUpperCase());
        else
            return "FAILED";

        if (isFirstName) {
            login = firstInitial.concat(lastName);
            login = UserNameGenerationUtil.trimWhiteSpaces(login);
        } else {
            login = lastName;
            login = UserNameGenerationUtil.trimWhiteSpaces(login);
        }

        if (userExists.contains(login)) {
            boolean userNameGenerated = false;
            for (int j = 1; j < 100; j++) {
                login = generateNextName(lastName, firstInitial, middleInitial, j);
                if (userExists.contains(login))
                    continue;
                userNameGenerated = true;
                break;
            }
            if (!userNameGenerated)
                return "FAILED";
        }

        return login;
    }

    public static UserManager getUserManager() {
        if (userManager == null) {
            userManager = (UserManager) Platform.getService(UserManager.class);
        }
        return userManager;
    }

}
