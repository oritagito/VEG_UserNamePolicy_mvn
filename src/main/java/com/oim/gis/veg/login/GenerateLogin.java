package com.oim.gis.veg.login;

import com.oim.gis.veg.utils.Utils;
import oracle.iam.identity.exception.UserNameGenerationException;
import oracle.iam.identity.usermgmt.api.UserNamePolicy;

import java.util.Locale;
import java.util.Map;

public class GenerateLogin implements UserNamePolicy {

    public String getUserNameFromPolicy(Map<String, String> reqData) throws UserNameGenerationException {

        String userName;

        userName = Utils.generateName(reqData.get("First Name"), reqData.get("Last Name"), reqData.get("Middle Name"));

        return userName;
    }

    public boolean isUserNameValid(String userName, Map<String, String> reqData) {

        boolean isUserNameValid = true;

        if ((userName == null) || (userName.length() == 0)) {
            isUserNameValid = false;
        }

        String firstName = reqData.get("First Name");
        if (!(firstName == null || firstName.length() == 0)) {
            if (Utils.isContainInvalidCharacters(firstName))
                isUserNameValid = false;
        }

        String middleName = reqData.get("Middle Name");
        if (!(middleName == null || middleName.length() == 0)) {
            if (Utils.isContainInvalidCharacters(middleName))
                isUserNameValid = false;
        }

        String lastName = reqData.get("Last Name");
        if (lastName == null || lastName.length() == 0 || Utils.isContainInvalidCharacters(lastName))
            isUserNameValid = false;

        return isUserNameValid;
    }

    public String getDescription(Locale locale) {
        return "Custom UFA UserNamePolicy (firstInitial lastName)";
    }

}
