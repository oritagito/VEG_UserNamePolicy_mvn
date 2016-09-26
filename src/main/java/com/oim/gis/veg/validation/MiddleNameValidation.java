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

/**
 * проверка вводимых данных в поле отчество
 */
public class MiddleNameValidation implements ValidationHandler {
    private static final String MIDDLE_NAME_REGEX = "^[a-z,A-Z,а-я,А-Я]+|[a-z,A-Z,а-я,А-Я]+[\\s,\\-]?[a-z,A-Z,а-я,А-Я]+";
    private static final String MIDDLE_NAME_MESSAGE = "Поле 'Отчество' содержит недопустимые символы. Допустимо однократное использование ' ' и '-' в середине текста";

    public void validate(long l, long l1, Orchestration orchestration) {

        HashMap<String, Serializable> contextParams = orchestration.getParameters();
        String newMiddleName = getParamaterValue(contextParams, UserManagerConstants.AttributeName.MIDDLENAME.getId());

        if (newMiddleName != null && !newMiddleName.isEmpty()) {
            boolean isMiddleNameValidate = newMiddleName.matches(MIDDLE_NAME_REGEX);

            if (!isMiddleNameValidate) {
                throw new ValidationFailedException(MIDDLE_NAME_MESSAGE);
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
