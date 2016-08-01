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

public class LastNameValidation implements ValidationHandler {
    private static final String LAST_NAME_REGEX = "^[a-z,A-Z,а-я,А-Я]+|[a-z,A-Z,а-я,А-Я]+[\\s,\\-]?[a-z,A-Z,а-я,А-Я]+";
    private static final String LAST_NAME_MESSAGE = "Поле 'Фамилия' содержит недопустимые символы. Допустимо однократное использование ' ' и '-' в середине текста";

    public void validate(long l, long l1, Orchestration orchestration) {

        HashMap<String, Serializable> contextParams = orchestration.getParameters();
        String newLastName = getParamaterValue(contextParams, UserManagerConstants.AttributeName.LASTNAME.getId());

        if (newLastName != null && !newLastName.equalsIgnoreCase("")) {
            boolean isLastNameValidate = newLastName.matches(LAST_NAME_REGEX);

            if (!isLastNameValidate) {
                throw new ValidationFailedException(LAST_NAME_MESSAGE);
            }
        }
    }

    private String getParamaterValue(HashMap<String, Serializable> parameters, String key) {
        if (parameters.containsKey(key)) {
            String value = (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
            return value;
        } else {
            return null;
        }
    }

    public void validate(long l, long l1, BulkOrchestration bulkOrchestration) throws ValidationException, ValidationFailedException {
    }

    public void initialize(HashMap<String, String> hashMap) {
    }
}
