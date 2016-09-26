package com.oim.gis.veg.handler.post;

import com.oim.gis.veg.utils.Utils;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserNameGenerationException;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.entitymgr.*;
import oracle.iam.platform.entitymgr.UnsupportedOperationException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The type Update attributes postprocess.
 */
public class UpdateAttributes_postprocess implements PostProcessHandler {

    private static final Logger LOG = Logger.getLogger("PLUGINS");
    private final String className = getClass().getName();

    private final String WORKING = "Working";                 // работает
    private final String VACATION = "Vacation";               // в отпуске
    private final String FIRED = "Fired";                     // уволен
    private final String MATERNITY_LEAVE = "Maternity-Leave"; // в декретном отпуске

    private final String DISABLED = "Disabled";
    private final String ACTIVE = "Active";

    /**
     * @param l
     * @param l1
     * @param orchestration
     * @return
     */
    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        LOG.entering(this.className, "---*--- entering EventResult execute ---*--- ", new Object[]{orchestration.getTarget().getType(), orchestration.getOperation()});

        String userKey = orchestration.getTarget().getEntityId();
        LOG.log(Level.FINEST, "---* userKey -> [" + userKey + "]");
        LOG.log(Level.FINEST, "---* orchestration.getParameters() -> [" + orchestration.getParameters() + "]");

        executeEvent(userKey, orchestration.getTarget().getType(), orchestration.getOperation(), orchestration.getParameters());

        LOG.exiting(this.className, "---*--- exiting EventResult execute ---*---");
        return new EventResult();
    }

    /**
     * @param l
     * @param l1
     * @param bulkOrchestration
     * @return
     */
    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        LOG.entering(this.className, "---*--- entering BulkEventResult execute ---*---", new Object[]{bulkOrchestration.getTarget().getType(), bulkOrchestration.getOperation()});

        HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();

        String[] userKey = bulkOrchestration.getTarget().getAllEntityId();

        for (int i = 0; i < bulkParameters.length; i++) {
            LOG.log(Level.FINEST, "---* userKey -> [" + userKey[i] + "]");
            LOG.log(Level.FINEST, "---* bulkParameters -> [" + bulkParameters[i] + "]");
            executeEvent(userKey[i], bulkOrchestration.getTarget().getType(), bulkOrchestration.getOperation(), bulkParameters[i]);
        }

        LOG.exiting(this.className, "---*--- exiting BulkEventResult execute ---*---");
        return new BulkEventResult();
    }

    /**
     * выполняет обновление атрибутов при создании/изменении
     *
     * @param userKey    - уникальный идентификатор пользователя
     * @param type       - тип операции. необходим для коммита
     * @param operation  - вид операции
     * @param parameters - список параметров
     */
    private void executeEvent(String userKey, String type, String operation, HashMap<String, Serializable> parameters) {
        LOG.entering(this.className, "---* entering executeEvent", new Object[]{userKey, type, operation});

        if (operation.equalsIgnoreCase("MODIFY")) {

            HashMap<String, Object> updateUser = new HashMap<String, Object>();

            boolean isUpdateAttributes = false;
            boolean isUpdateLastName = false;
            boolean isUpdateEmpStatus = false;

            String empStatus = null;
            String firstName = null;
            String lastName = null;
            String middleName = null;

            if (parameters.containsKey("emp_status")) {
                isUpdateEmpStatus = true;
                empStatus = getParameterValue(parameters, "emp_status");
            }

            if (parameters.containsKey("First Name")) {
                isUpdateAttributes = true;
                firstName = getParameterValue(parameters, "First Name");
            }

            if (parameters.containsKey("Last Name")) {
                isUpdateAttributes = true;
                isUpdateLastName = true;
                lastName = getParameterValue(parameters, "Last Name");
            }

            if (parameters.containsKey("Middle Name")) {
                isUpdateAttributes = true;
                middleName = getParameterValue(parameters, "Middle Name");
            }

            // если обновились атрибуты
            if (isUpdateAttributes || isUpdateEmpStatus) {

                User user = getUser(userKey);

                if (isUpdateAttributes) {
                    LOG.log(Level.INFO, "---* isUpdateAttributes -> [" + isUpdateAttributes + "]");

                    // обновить displayName
                    String displayName = getDisplayName(userKey, firstName, lastName, middleName, user);
                    if (displayName != null) {
                        updateUser.put(UserManagerConstants.AttributeName.DISPLAYNAME.getName(), displayNameMap(displayName));
                    }
                    // обновить initials
                    String initials = getInitials(userKey, firstName, lastName, middleName, user);
                    if (initials != null) {
                        updateUser.put(UserManagerConstants.AttributeName.INITIALS.getName(), initials);
                    }
                    // если обновилась фамилия, обновить login
                    if (isUpdateLastName) {
                        LOG.log(Level.INFO, "---* isUpdateLastName -> [" + isUpdateLastName + "]");
                        String login = getLogin(userKey, firstName, lastName, middleName, user);
                        if (login != null) {
                            updateUser.put(UserManagerConstants.AttributeName.USER_LOGIN.getName(), login);
                        }
                    }
                    // если были измененные атрибуты, выполнить коммит
                    if (!updateUser.isEmpty()) updateUser(type, userKey, updateUser);
                }

                // выполнить обновление status/displayName/login в зависимости от полученного статуса
                // если были измененные атрибуты, выполнить коммит
                if (isUpdateEmpStatus) {
                    updateEmpStatus(type, userKey, empStatus);
                }
            }

        } else if (operation.equalsIgnoreCase("CREATE")) {

            HashMap<String, Object> updateUser = new HashMap<String, Object>();

            String empStatus = getParameterValue(parameters, "emp_status");
            LOG.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");

            String firstName = getParameterValue(parameters, "First Name");
            LOG.log(Level.FINEST, "---* firstName -> [" + firstName + "]");

            String lastName = getParameterValue(parameters, "Last Name");
            LOG.log(Level.FINEST, "---* lastName -> [" + lastName + "]");

            String middleName = getParameterValue(parameters, "Middle Name");
            LOG.log(Level.FINEST, "---* middleName -> [" + middleName + "]");

            // обновить locale
            updateUser.put(UserManagerConstants.AttributeName.LOCALE.getName(), getLocale(userKey));

            // обновить displayName
            String displayName = generateDisplayName(firstName, lastName, middleName, false);
            if (displayName != null) {
                updateUser.put(UserManagerConstants.AttributeName.DISPLAYNAME.getName(), displayNameMap(displayName));
            }

            // обновить initials
            String initials = generateInitials(firstName, lastName, middleName);
            if (initials != null) {
                updateUser.put(UserManagerConstants.AttributeName.INITIALS.getName(), initials);
            }

            // если были измененные атрибуты, выполнить коммит
            if (!updateUser.isEmpty()) updateUser(type, userKey, updateUser);

            // выполнить обновление status/displayName/login в зависимости от полученного статуса
            // если были измененные атрибуты, выполнить коммит
            updateEmpStatus(type, userKey, empStatus);
        }

        LOG.exiting(this.className, "---* exiting executeEvent");
    }

    /**
     * обновляет атрибуты в зависимости от полученного emp_status
     *
     * @param type      - тип операции. необходим для коммита
     * @param userKey   - уникальный идентификатор пользователя
     * @param empStatus - полученный emp_status
     */
    private void updateEmpStatus(String type, String userKey, String empStatus) {
        LOG.entering(this.className, "---* entering updateEmpStatus", new Object[]{userKey, empStatus, type});
        try {
            // если emp_status не пуст
            if (empStatus != null && !empStatus.isEmpty()) {

                User user = getUser(userKey);

                HashMap<String, Object> updateUser = new HashMap<String, Object>();

                // если emp_status еквивалентен "Working"
                if (empStatus.equalsIgnoreCase(this.WORKING)) {
                    // если пользователь заблокированн - разблокировать
                    if (!user.getStatus().equalsIgnoreCase(this.ACTIVE)) {
                        updateUser.put(UserManagerConstants.AttributeName.STATUS.getName(), this.ACTIVE);
                    }
                    // если displayName начинается с "уволен" - обновить displayName. убрать уволен
                    if (user.getDisplayName().toLowerCase().startsWith("уволен")) {
                        String displayName = generateDisplayName(user.getFirstName(), user.getLastName(), user.getMiddleName(), false);
                        LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                        if (displayName != null) {
                            updateUser.put(UserManagerConstants.AttributeName.DISPLAYNAME.getName(), displayNameMap(displayName));
                        }
                    }
                    // если login начинается с "fired" - обновить login. убрать fired
                    if (user.getLogin().toLowerCase().startsWith("fired")) {
                        String login = Utils.generateName(user.getFirstName(), user.getLastName(), user.getMiddleName());
                        LOG.log(Level.FINEST, "---* login -> [" + login + "]");
                        updateUser.put(UserManagerConstants.AttributeName.USER_LOGIN.getName(), login);
                    }
                }

                // если emp_status еквивалентен "Fired"
                if (empStatus.equalsIgnoreCase(this.FIRED)) {
                    // если пользователь разблокированн - заблокировать
                    if (!user.getStatus().equalsIgnoreCase(this.DISABLED)) {
                        updateUser.put(UserManagerConstants.AttributeName.STATUS.getName(), this.DISABLED);
                    }
                    // если displayName НЕ начинается с "уволен" - обновить displayName. добавить уволен
                    if (!user.getDisplayName().toLowerCase().startsWith("уволен")) {
                        String displayName = generateDisplayName(user.getFirstName(), user.getLastName(), user.getMiddleName(), true);
                        LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                        if (displayName != null) {
                            updateUser.put(UserManagerConstants.AttributeName.DISPLAYNAME.getName(), displayNameMap(displayName));
                        }
                    }
                    // если login НЕ начинается с "fired" - обновить login. добавить fired с текущей датой
                    if (!user.getLogin().toLowerCase().startsWith("fired")) {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        String login = "FIRED_" + user.getLogin() + "_" + simpleDateFormat.format(calendar.getTime());
                        LOG.log(Level.FINEST, "---* login -> [" + login + "]");
                        updateUser.put(UserManagerConstants.AttributeName.USER_LOGIN.getName(), login);
                    }
                }

                // если emp_status еквивалентен "Vacation"
                if (empStatus.equalsIgnoreCase(this.VACATION)) {
                    // если пользователь разблокированн - заблокировать
                    if (!user.getStatus().equalsIgnoreCase(this.DISABLED)) {
                        updateUser.put(UserManagerConstants.AttributeName.STATUS.getName(), this.DISABLED);
                    }
                    // если displayName начинается с "уволен" - обновить displayName. убрать уволен
                    if (user.getDisplayName().toLowerCase().startsWith("уволен")) {
                        String displayName = generateDisplayName(user.getFirstName(), user.getLastName(), user.getMiddleName(), false);
                        LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                        if (displayName != null) {
                            updateUser.put(UserManagerConstants.AttributeName.DISPLAYNAME.getName(), displayNameMap(displayName));
                        }
                    }
                    // если login начинается с "fired" - обновить login. убрать fired
                    if (user.getLogin().toLowerCase().startsWith("fired")) {
                        String login = Utils.generateName(user.getFirstName(), user.getLastName(), user.getMiddleName());
                        LOG.log(Level.FINEST, "---* login -> [" + login + "]");
                        updateUser.put(UserManagerConstants.AttributeName.USER_LOGIN.getName(), login);
                    }
                }

                // если emp_status еквивалентен "Maternity-Leave"
                if (empStatus.equalsIgnoreCase(this.MATERNITY_LEAVE)) {
                    // если пользователь разблокированн - заблокировать
                    if (!user.getStatus().equalsIgnoreCase(this.DISABLED)) {
                        updateUser.put(UserManagerConstants.AttributeName.STATUS.getName(), this.DISABLED);
                    }
                    // если displayName начинается с "уволен" - обновить displayName. убрать уволен
                    if (user.getDisplayName().toLowerCase().startsWith("уволен")) {
                        String displayName = generateDisplayName(user.getFirstName(), user.getLastName(), user.getMiddleName(), false);
                        LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                        if (displayName != null) {
                            updateUser.put(UserManagerConstants.AttributeName.DISPLAYNAME.getName(), displayNameMap(displayName));
                        }
                    }
                    // если login начинается с "fired" - обновить login. убрать fired
                    if (user.getLogin().toLowerCase().startsWith("fired")) {
                        String login = Utils.generateName(user.getFirstName(), user.getLastName(), user.getMiddleName());
                        LOG.log(Level.FINEST, "---* login -> [" + login + "]");
                        updateUser.put(UserManagerConstants.AttributeName.USER_LOGIN.getName(), login);
                    }
                }
                // если были измененные атрибуты, выполнить коммит
                if (!updateUser.isEmpty()) updateUser(type, userKey, updateUser);
            } else LOG.log(Level.FINEST, "---* empStatus is empty");
        } catch (UserNameGenerationException e) {
            LOG.log(Level.SEVERE, "---* UserNameGenerationException", e.getMessage());
            e.printStackTrace();
        }
        LOG.exiting(this.className, "---* exiting updateEmpStatus");
    }

    /**
     * выполняет коммит
     *
     * @param type       - тип операции. необходим для коммита
     * @param userKey    - уникальный идентификатор пользователя
     * @param updateUser - список ключей - атрибутов для обновления
     */
    private void updateUser(String type, String userKey, HashMap<String, Object> updateUser) {
        LOG.entering(this.className, "---* entering updateUser", new Object[]{type, userKey, updateUser});
        EntityManager entityManager = Utils.getEntityManager();
        try {
            entityManager.modifyEntity(type, userKey, updateUser);
        } catch (InvalidDataTypeException e) {
            LOG.log(Level.SEVERE, "---* InvalidDataTypeException", e.getMessage());
            e.printStackTrace();
        } catch (InvalidDataFormatException e) {
            LOG.log(Level.SEVERE, "---* InvalidDataFormatException", e.getMessage());
            e.printStackTrace();
        } catch (NoSuchEntityException e) {
            LOG.log(Level.SEVERE, "---* NoSuchEntityException", e.getMessage());
            e.printStackTrace();
        } catch (StaleEntityException e) {
            LOG.log(Level.SEVERE, "---* StaleEntityException", e.getMessage());
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            LOG.log(Level.SEVERE, "---* UnsupportedOperationException", e.getMessage());
            e.printStackTrace();
        } catch (UnknownAttributeException e) {
            LOG.log(Level.SEVERE, "---* UnknownAttributeException", e.getMessage());
            e.printStackTrace();
        } catch (ProviderException e) {
            LOG.log(Level.SEVERE, "---* ProviderException", e.getMessage());
            e.printStackTrace();
        }
        LOG.exiting(this.className, "---* exiting updateUser");
    }

    /**
     * получить список ключей - атрибутов пользователя
     *
     * @param userKey - уникальный идентификатор пользователя
     * @return
     */
    private User getUser(String userKey) {
        User user = null;
        try {
            user = Utils.getUserManager().getDetails(userKey, null, false);
        } catch (NoSuchUserException e) {
            LOG.log(Level.SEVERE, "---* NoSuchUserException", e.getMessage());
            e.printStackTrace();
        } catch (UserLookupException e) {
            LOG.log(Level.SEVERE, "---* UserLookupException", e.getMessage());
            e.printStackTrace();
        }
        return user;
    }

    /**
     * для обновления displayName необходимо подставлять "Map"
     * упрощение читаемости
     *
     * @param displayName
     * @return - возвращает корректный "Map" для обновления
     */
    private Map displayNameMap(String displayName) {
        Map displayNameMap = new HashMap();
        displayNameMap.put("base", displayName);
        return displayNameMap;
    }

    /**
     * @param userKey - уникальный идентификатор пользователя
     * @retur - возвращает locale пользователя по умолчанию (ru-RU)
     */
    private String getLocale(String userKey) {
        LOG.entering(this.className, "---* entering getLocale", new Object[]{userKey});

        String locale = "ru-RU";
        LOG.log(Level.FINEST, "---* locale -> [" + locale + "]");

        LOG.exiting(this.className, "---* exiting getLocale");
        return locale;
    }

    /**
     * получает обновленный displayName
     * проверяет на null полученные атрибуты
     * если null, берет текущие
     *
     * @param userKey    - уникальный идентификатор пользователя
     * @param firstName  - имя
     * @param lastName   - фамилия
     * @param middleName - отчество
     * @param user       - текущие атрибуты пользователя
     * @return - возвращает обновленный displayName
     */
    private String getDisplayName(String userKey, String firstName, String lastName, String middleName, User user) {
        LOG.entering(this.className, "---* entering getDisplayName", new Object[]{userKey, firstName, lastName, middleName});

        String displayName = null;

        if (!user.getDisplayName().toLowerCase().startsWith("уволен")) {
            if (firstName == null) {
                firstName = user.getFirstName();
                LOG.log(Level.FINEST, "---* firstName is null... get current value -> [" + firstName + "]");
            }
            if (lastName == null) {
                lastName = user.getLastName();
                LOG.log(Level.FINEST, "---* lastName is null... get current value -> [" + lastName + "]");
            }
            if (middleName == null) {
                middleName = user.getMiddleName();
                LOG.log(Level.FINEST, "---* middleName is null... get current value -> [" + middleName + "]");
            }
            displayName = generateDisplayName(firstName, lastName, middleName, false);
            LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
        } else LOG.log(Level.FINEST, "---* user is Fired");

        LOG.exiting(this.className, "---* exiting getDisplayName");
        return displayName;
    }

    /**
     * получает обновленные initials
     * проверяет на null полученные атрибуты
     * если null, берет текущие
     *
     * @param userKey    - уникальный идентификатор пользователя
     * @param firstName  - имя
     * @param lastName   - фамилия
     * @param middleName - отчество
     * @param user       - текущие атрибуты пользователя
     * @return - возвращает обновленные initials
     */
    private String getInitials(String userKey, String firstName, String lastName, String middleName, User user) {
        LOG.entering(this.className, "---* entering getInitials", new Object[]{userKey, firstName, lastName, middleName});

        String initials = null;

        if (!user.getDisplayName().toLowerCase().startsWith("уволен")) {
            if (firstName == null) {
                firstName = user.getFirstName();
                LOG.log(Level.FINEST, "---* firstName is null... get current value -> [" + firstName + "]");
            }
            if (lastName == null) {
                lastName = user.getLastName();
                LOG.log(Level.FINEST, "---* lastName is null... get current value -> [" + lastName + "]");
            }
            if (middleName == null) {
                middleName = user.getMiddleName();
                LOG.log(Level.FINEST, "---* middleName is null... get current value -> [" + middleName + "]");
            }

            initials = generateInitials(firstName, lastName, middleName);
            LOG.log(Level.FINEST, "---* initials -> [" + initials + "]");
        } else LOG.log(Level.FINEST, "---* User is Fired");

        LOG.exiting(this.className, "---* exiting getInitials");
        return initials;
    }

    /**
     * получает обновленный login
     * проверяет на null полученные атрибуты
     * если null, берет текущие
     *
     * @param userKey    - уникальный идентификатор пользователя
     * @param firstName  - имя
     * @param lastName   - фамилия
     * @param middleName - отчество
     * @param user       - текущие атрибуты пользователя
     * @return - возвращает обновленные login
     */
    private String getLogin(String userKey, String firstName, String lastName, String middleName, User user) {
        LOG.entering(this.className, "---* entering getLogin", new Object[]{userKey, firstName, lastName, middleName});

        String login = null;
        try {
            LOG.log(Level.FINEST, "---* Current login -> [" + user.getLogin() + "]");
            if (!user.getLogin().toLowerCase().startsWith("fired")) {
                if (firstName == null) {
                    firstName = user.getFirstName();
                    LOG.log(Level.FINEST, "---* firstName is null... get current value -> [" + firstName + "]");
                }
                if (lastName == null) {
                    lastName = user.getLastName();
                    LOG.log(Level.FINEST, "---* lastName is null... get current value -> [" + lastName + "]");
                }
                if (middleName == null) {
                    middleName = user.getMiddleName();
                    LOG.log(Level.FINEST, "---* middleName is null... get current value -> [" + middleName + "]");
                }
                login = Utils.generateName(firstName, lastName, middleName);
                LOG.log(Level.FINEST, "---* login -> [" + login + "]");
            } else LOG.log(Level.FINEST, "---* user is Fired");
        } catch (UserNameGenerationException e) {
            LOG.log(Level.SEVERE, "---* UserNameGenerationException", e.getMessage());
            e.printStackTrace();
        }
        LOG.exiting(this.className, "---* exiting getLogin");
        return login;
    }

    /**
     * генерация login
     * с проверкой на статус пользователя (уволенный - не уволенный)
     *
     * @param firstName  - имя
     * @param lastName   - фамилия
     * @param middleName - отчество
     * @param isFired    - true - обновить displayName по обычному правилу (фамилия - имя - отчество)
     *                   - false - обновить displayName по правилу увольнения (уволен - фамилия - имя - отчество)
     * @return - возвращает сгенерированный displayName
     */
    private String generateDisplayName(String firstName, String lastName, String middleName, boolean isFired) {
        LOG.entering(this.className, "---* entering generateDisplayName", new Object[]{firstName, lastName, middleName, isFired});
        String displayName = null;
        if (!isFired) {
            if (lastName != null && !lastName.isEmpty()) displayName = lastName;
            if (firstName != null && !firstName.isEmpty()) displayName = displayName + " " + firstName;
            if (middleName != null && !middleName.isEmpty()) displayName = displayName + " " + middleName;
        } else {
            if (lastName != null && !lastName.isEmpty()) displayName = lastName;
            if (firstName != null && !firstName.isEmpty()) displayName = displayName + " " + firstName;
            if (middleName != null && !middleName.isEmpty()) displayName = displayName + " " + middleName;
            displayName = "Уволен " + displayName;
        }
        LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
        LOG.exiting(this.className, "---* exiting generateDisplayName");
        return displayName;
    }

    /**
     * генерация initials
     *
     * @param firstName - имя
     * @param lastName - фамилия
     * @param middleName - отчество
     * @return - возвращает сгенерированные initials
     */
    private String generateInitials(String firstName, String lastName, String middleName) {
        LOG.entering(this.className, "---* entering generateInitials", new Object[]{firstName, lastName, middleName});
        String initials = null;
        if (lastName != null && !lastName.isEmpty()) initials = lastName.substring(0, 1).toUpperCase();
        if (firstName != null && !firstName.isEmpty()) initials = initials + firstName.substring(0, 1).toUpperCase();
        if (middleName != null && !middleName.isEmpty()) initials = initials + middleName.substring(0, 1).toUpperCase();
        LOG.log(Level.FINEST, "---* initials -> [" + initials + "]");
        LOG.exiting(this.className, "---* exiting generateInitials");
        return initials;
    }

    private String getParameterValue(HashMap<String, Serializable> parameters, String key) {
        return (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    @Override
    public void initialize(HashMap<String, String> hashMap) {

    }
}
