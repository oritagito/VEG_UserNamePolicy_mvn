package com.oim.gis.veg.validation;

import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.ValidationException;
import oracle.iam.platform.kernel.ValidationFailedException;
import oracle.iam.platform.kernel.spi.ValidationHandler;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.Orchestration;

import java.io.Serializable;
import java.util.HashMap;

public class FirstNameValidation implements ValidationHandler {

    private static final String FIRST_NAME_REGEX = "^[a-z,A-Z,а-я,А-Я]+|[a-z,A-Z,а-я,А-Я]+[\\s,\\-]?[a-z,A-Z,а-я,А-Я]+";
    private static final String FIRST_NAME_MESSAGE = "Поле 'Имя' содержит недопустимые символы. Допустимо однократное использование ' ' и '-' в середине текста";

    public void validate(long l, long l1, Orchestration orchestration) throws ValidationException, ValidationFailedException {
        HashMap<String, Serializable> contextParams = orchestration.getParameters();
        String newFirstName = getParamaterValue(contextParams, UserManagerConstants.AttributeName.FIRSTNAME.getId());

        if (newFirstName != null && !newFirstName.equalsIgnoreCase("")) {
            boolean isFirstNameValidate = newFirstName.matches(FIRST_NAME_REGEX);

            if (!isFirstNameValidate) {
                throw new ValidationFailedException(FIRST_NAME_MESSAGE);
            }
        }
    }

    private String getParamaterValue(HashMap<String, Serializable> parameters, String key) {
        if (parameters.containsKey(key)) {
            return (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
        } else {
            return null;
        }
    }

    @Override
    public void validate(long l, long l1, BulkOrchestration bulkOrchestration) throws ValidationException, ValidationFailedException {

    }

    @Override
    public void initialize(HashMap<String, String> hashMap) {

    }
}
