package com.oim.gis.veg.utils;

import oracle.iam.identity.exception.UserNameGenerationException;
import oracle.iam.identity.usermgmt.utils.UserNameGenerationUtil;
import oracle.iam.identity.usermgmt.utils.UserNamePolicyUtil;

public class Utils {

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
                return "YO";
            case 'Ж':
                return "ZH";
            case 'З':
                return "Z";
            case 'И':
                return "I";
            case 'Й':
                return "J";
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
                return "SHH";
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
        } else {
            if (firstInitial != null)
                return (firstInitial.concat(lastName)).concat(Integer.toString(index - 1));
            else
                return lastName.concat(Integer.toString(index - 1));
        }
    }

    public static boolean isContainInvalidCharacters(String inputString) {

        if (inputString.startsWith("-") || inputString.startsWith(" "))
            return true;

        // Valid only "-" and " "
        String[] invalidCharacters = {"{", "}", "[", "]",
                ":", "\"", ";", "№", "'", "+", "=",
                "<", ">", "?", ",", ".", "/", "_",
                "!", "@", "#", "$", "%", "^", "&", "*", "(", ")"};

        for (String s : invalidCharacters) {
            if (inputString.contains(s))
                return true;
        }

        return false;
    }

    public static String generateName(String firstName, String lastName, String middleName) throws UserNameGenerationException {

        String login;

        String firstInitial = null;
        String middleInitial = null;

        boolean isFirstName = !(firstName == null || firstName.length() == 0);
        boolean isMiddleName = !(middleName == null || middleName.length() == 0);

        if (isFirstName)
            firstInitial = cyrillicToLatinResult((firstName.substring(0, 1)).toUpperCase());

        if (isMiddleName)
            middleInitial = cyrillicToLatinResult((middleName.substring(0, 1)).toUpperCase());

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
}
