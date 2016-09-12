package com.oim.gis.veg.handler.post;

import com.oim.gis.veg.utils.Utils;
import oracle.iam.identity.exception.*;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateAttributes_postprocess implements PostProcessHandler {

    private static final Logger LOG = Logger.getLogger("PLUGINS");
    private final String className = getClass().getName();

    private final String WORKING = "Working";                 // Работает
    private final String VACATION = "Vacation";               // В Отпуске
    private final String FIRED = "Fired";                     // Уволен
    private final String MATERNITY_LEAVE = "Maternity-Leave"; // Декретный Отпуск

    private final String ACTIVE = "Active";
    private final String DISABLED = "Disabled";

    private final Locale LOCALE = new Locale("ru", "RU");

    public EventResult execute(long l, long l1, Orchestration orchestration) {
        LOG.entering(className, "---*--- execute ---*--- ");
        LOG.log(Level.INFO, "---* Entering  EventResult of UpdateAttributes_postprocess");
        LOG.log(Level.INFO, "---* orchestration.getOperation() -> [" + orchestration.getOperation() + "]");

        String userKey = orchestration.getTarget().getEntityId();
        LOG.log(Level.FINEST, "---* userKey -> [" + userKey + "]");
        LOG.log(Level.FINEST, "---* orchestration.getParameters() -> [" + orchestration.getParameters() + "]");

        executeEvent(userKey, orchestration.getOperation(), orchestration.getParameters());

        LOG.log(Level.INFO, "---* Exiting  EventResult of UpdateAttributes_postprocess");
        LOG.exiting(className, "---*--- execute ---*---");

        return new EventResult();
    }

    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        LOG.entering(className, "---*--- execute ---*---");
        LOG.log(Level.INFO, "---* Entering BulkEventResult of UpdateAttributes_postprocess");
        LOG.log(Level.INFO, "---* bulkOrchestration.getOperation() -> [" + bulkOrchestration.getOperation() + "]");

        HashMap[] bulkParameters = bulkOrchestration.getBulkParameters();

        String[] userKey = bulkOrchestration.getTarget().getAllEntityId();

        for (int i = 0; i < bulkParameters.length; i++) {
            LOG.log(Level.FINEST, "---* userKey -> [" + userKey[i] + "]");
            LOG.log(Level.FINEST, "---* bulkParameters -> [" + bulkParameters[i] + "]");
            executeEvent(userKey[i], bulkOrchestration.getOperation(), bulkParameters[i]);
        }

        LOG.log(Level.INFO, "---* Exiting BulkEventResult of UpdateAttributes_postprocess");
        LOG.exiting(className, "---*--- execute ---*---");
        return new BulkEventResult();
    }

    private void executeEvent(String userKey, String operation, HashMap parameters) {
        LOG.entering(this.className, "---* executeEvent", new Object[]{operation, userKey});

        String empStatus = null;
        String firstName = null;
        String lastName = null;
        String middleName = null;

        String response;

        if (operation.equalsIgnoreCase("MODIFY")) {
            LOG.log(Level.INFO, "---* Operation is MODIFY");

            boolean isUpdateAttributes = false;
            boolean isUpdateLastName = false;

            if (parameters.containsKey("emp_status")) {
                isUpdateAttributes = true;
                empStatus = getParameterValue(parameters, "emp_status") == null ? "" : getParameterValue(parameters, "emp_status");
                LOG.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");
            }

            if (parameters.containsKey("First Name")) {
                isUpdateAttributes = true;
                firstName = getParameterValue(parameters, "First Name") == null ? "" : getParameterValue(parameters, "First Name");
                LOG.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
            }

            if (parameters.containsKey("Last Name")) {
                isUpdateAttributes = true;
                isUpdateLastName = true;
                lastName = getParameterValue(parameters, "Last Name") == null ? "" : getParameterValue(parameters, "Last Name");
                LOG.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
            }

            if (parameters.containsKey("Middle Name")) {
                isUpdateAttributes = true;
                middleName = getParameterValue(parameters, "Middle Name") == null ? "" : getParameterValue(parameters, "Middle Name");
                LOG.log(Level.FINEST, "---* middleName -> [" + middleName + "]");
            }

            if (isUpdateLastName) {
                response = updateLogin(userKey, firstName, lastName, middleName, false);
                LOG.log(Level.INFO, "---* response updateLogin-> [" + response + "]");
            }

            if (isUpdateAttributes) {
                response = updateDisplayNameAndInitials(userKey, firstName, lastName, middleName);
                LOG.log(Level.INFO, "---* response updateDisplayNameAndInitials-> [" + response + "]");

                response = updateEmpStatusAndLocale(userKey, empStatus, true);
                LOG.log(Level.INFO, "---* response updateEmpStatusAndLocale-> [" + response + "]");
            }

        } else if (operation.equalsIgnoreCase("CREATE")) {
            LOG.log(Level.INFO, "---* Operation is CREATE");

            empStatus = getParameterValue(parameters, "emp_status") == null ? "" : getParameterValue(parameters, "emp_status");
            LOG.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");

            firstName = getParameterValue(parameters, "First Name") == null ? "" : getParameterValue(parameters, "First Name");
            lastName = getParameterValue(parameters, "Last Name") == null ? "" : getParameterValue(parameters, "Last Name");
            middleName = getParameterValue(parameters, "Middle Name") == null ? "" : getParameterValue(parameters, "Middle Name");

            LOG.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
            LOG.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
            LOG.log(Level.FINEST, "---* middleName -> [" + middleName + "]");

            response = updateDisplayNameAndInitials(userKey, firstName, lastName, middleName);
            LOG.log(Level.INFO, "---* response updateDisplayNameAndInitials-> [" + response + "]");

            response = updateEmpStatusAndLocale(userKey, empStatus, false);
            LOG.log(Level.INFO, "---* response updateEmpStatusAndLocale-> [" + response + "]");
        }

        LOG.exiting(this.className, "---* executeEvent");
    }

    private String updateEmpStatusAndLocale(String userKey, String empStatus, boolean isUpdate) {

        String response = null;

        try {
            User user = Utils.getUserManager().getDetails(userKey, null, false);

            String usrLogin = user.getLogin();
            String usrStatus = user.getStatus();
            String usrDisplayName = user.getDisplayName();
            String usrFirstName = user.getFirstName();
            String usrLastName = user.getLastName();
            String usrMiddleName = user.getMiddleName();

            User updateUser = new User(userKey);

            if (!isUpdate) {
                updateUser.setLocale(LOCALE);
            }

            if (empStatus != null && !empStatus.isEmpty()) {

                if (empStatus.equalsIgnoreCase(WORKING)) {
                    if (!usrStatus.equalsIgnoreCase(ACTIVE)) {
                        Utils.getUserManager().enable(userKey, false);
                        LOG.log(Level.FINEST, "---* Enabled user -> [" + userKey + "] Successfully");
                    }
                    if (usrDisplayName.toLowerCase().contains("уволен")) {
                        updateUser.setDisplayName(generateDisplayName(usrFirstName, usrLastName, usrMiddleName, false));
                    }
                    if (usrLogin.toLowerCase().contains("fired")) {
                        updateUser.setLogin(Utils.generateName(usrFirstName, usrLastName, usrMiddleName));
                    }
                }

                if (empStatus.equalsIgnoreCase(VACATION)) {
                    if (!usrStatus.equalsIgnoreCase(DISABLED)) {
                        Utils.getUserManager().disable(userKey, false);
                        LOG.log(Level.FINEST, "---* Disabled user -> [" + userKey + "] Successfully");
                    }
                    if (usrDisplayName.toLowerCase().contains("уволен")) {
                        updateUser.setDisplayName(generateDisplayName(usrFirstName, usrLastName, usrMiddleName, false));
                    }
                    if (usrLogin.toLowerCase().contains("fired")) {
                        updateUser.setLogin(Utils.generateName(usrFirstName, usrLastName, usrMiddleName));
                    }
                }

                if (empStatus.equalsIgnoreCase(FIRED)) {
                    if (!usrStatus.equalsIgnoreCase(DISABLED)) {
                        Utils.getUserManager().disable(userKey, false);
                        LOG.log(Level.FINEST, "---* Disabled user -> [" + userKey + "] Successfully");
                    }
                    if (!usrDisplayName.toLowerCase().contains("уволен")) {
                        updateUser.setDisplayName(generateDisplayName(usrFirstName, usrLastName, usrMiddleName, true));
                    }
                    if (!usrLogin.toLowerCase().contains("fired")) {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        updateUser.setLogin("FIRED_" + usrLogin + "_" + simpleDateFormat.format(calendar.getTime()));
                    }
                }

                if (empStatus.equalsIgnoreCase(MATERNITY_LEAVE)) {
                    if (!usrStatus.equalsIgnoreCase(DISABLED)) {
                        Utils.getUserManager().disable(userKey, false);
                        LOG.log(Level.FINEST, "---* Disabled user -> [" + userKey + "] Successfully");
                    }
                    if (usrDisplayName.toLowerCase().contains("уволен")) {
                        updateUser.setDisplayName(generateDisplayName(usrFirstName, usrLastName, usrMiddleName, false));
                    }
                    if (usrLogin.toLowerCase().contains("fired")) {
                        updateUser.setLogin(Utils.generateName(usrFirstName, usrLastName, usrMiddleName));
                    }
                }
            }

            Utils.getUserManager().modify(updateUser);

            response = "Success updateEmpStatusAndLocale";

        } catch (NoSuchUserException e) {
            LOG.log(Level.WARNING, "---* NoSuchUserException", e);
        } catch (UserLookupException e) {
            LOG.log(Level.WARNING, "---* UserLookupException", e);
        } catch (ValidationFailedException e) {
            LOG.log(Level.WARNING, "---* ValidationFailedException", e);
        } catch (UserModifyException e) {
            LOG.log(Level.WARNING, "---* UserModifyException", e);
        } catch (UserNameGenerationException e) {
            LOG.log(Level.WARNING, "---* UserNameGenerationException", e);
        } catch (UserEnableException e) {
            LOG.log(Level.WARNING, "---* UserEnableException", e);
        } catch (UserDisableException e) {
            LOG.log(Level.WARNING, "---* UserDisableException", e);
        }

        return response;
    }

    private String updateDisplayNameAndInitials(String userKey, String firstName, String lastName, String middleName) {

        String response = null;

        try {
            User user = Utils.getUserManager().getDetails(userKey, null, false);

            String usrDisplayName = user.getDisplayName();
            LOG.log(Level.FINEST, "---* usrDisplayName -> [" + usrDisplayName + "]");

            if (!usrDisplayName.toLowerCase().contains("уволен")) {

                User updateUser = new User(userKey);

                if (firstName == null) firstName = user.getFirstName();
                if (lastName == null) lastName = user.getLastName();
                if (middleName == null) middleName = user.getMiddleName();

                String displayName = generateDisplayName(firstName, lastName, middleName, false);
                String initials = generateInitials(firstName, lastName, middleName);

                LOG.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                LOG.log(Level.FINEST, "---* initials -> [" + initials + "]");

                updateUser.setDisplayName(displayName);
                updateUser.setAttribute("Initials", initials);
                Utils.getUserManager().modify(updateUser);

            } else LOG.log(Level.FINEST, "---* User is Fired");

            response = "Success updateDisplayNameAndInitials";

        } catch (NoSuchUserException e) {
            LOG.log(Level.WARNING, "---* NoSuchUserException", e);
        } catch (UserLookupException e) {
            LOG.log(Level.WARNING, "---* UserLookupException", e);
        } catch (ValidationFailedException e) {
            LOG.log(Level.WARNING, "---* ValidationFailedException", e);
        } catch (UserModifyException e) {
            LOG.log(Level.WARNING, "---* UserModifyException", e);
        }

        return response;
    }

    private String updateLogin(String userKey, String firstName, String lastName, String middleName, boolean isDeleted) {

        String response = null;

        try {
            User user = Utils.getUserManager().getDetails(userKey, null, false);

            String currentLogin = user.getLogin();
            LOG.log(Level.FINEST, "---* currentLogin -> [" + currentLogin + "]");

            User updateUser = new User(userKey);

            if (!isDeleted && !currentLogin.toLowerCase().contains("fired")) {

                if (firstName == null) firstName = user.getFirstName();
                if (lastName == null) lastName = user.getLastName();
                if (middleName == null) middleName = user.getMiddleName();

                String login = Utils.generateName(firstName, lastName, middleName);
                LOG.log(Level.FINEST, "---* login -> [" + login + "]");

                updateUser.setLogin(login);

            } else if (isDeleted) {

                String login = null;

                if (currentLogin.toLowerCase().contains("fired")) {
                    if (currentLogin != null && currentLogin != "") {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        login = "DEL_" + currentLogin.substring(currentLogin.indexOf('_') + 1, currentLogin.lastIndexOf('_')) + "_" + simpleDateFormat.format(calendar.getTime());
                    }
                } else {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    login = "DEL_" + currentLogin + "_" + simpleDateFormat.format(calendar.getTime());
                }

                LOG.log(Level.FINEST, "---* login -> [" + login + "]");

                updateUser.setLogin(login);
            }

            Utils.getUserManager().modify(updateUser);

            response = "Success updateLogin";

        } catch (NoSuchUserException e) {
            LOG.log(Level.WARNING, "---* NoSuchUserException", e);
        } catch (UserLookupException e) {
            LOG.log(Level.WARNING, "---* UserLookupException", e);
        } catch (UserNameGenerationException e) {
            LOG.log(Level.WARNING, "---* UserNameGenerationException", e);
        } catch (UserModifyException e) {
            LOG.log(Level.WARNING, "---* UserModifyException", e);
        } catch (ValidationFailedException e) {
            LOG.log(Level.WARNING, "---* ValidationFailedException", e);
        }

        return response;
    }

    private String generateDisplayName(String firstName, String lastName, String middleName, boolean isFired) {
        String usrDisplayName = null;

        if (!isFired) {
            if (lastName != null && lastName != "") usrDisplayName = lastName;
            if (firstName != null && firstName != "") usrDisplayName = usrDisplayName + " " + firstName;
            if (middleName != null && middleName != "") usrDisplayName = usrDisplayName + " " + middleName;
        } else {
            if (lastName != null && lastName != "") usrDisplayName = lastName;
            if (firstName != null && firstName != "") usrDisplayName = usrDisplayName + " " + firstName;
            if (middleName != null && middleName != "") usrDisplayName = usrDisplayName + " " + middleName;
            usrDisplayName = "Уволен " + usrDisplayName;
        }
        return usrDisplayName;
    }

    private String generateInitials(String firstName, String lastName, String middleName) {

        String initials = null;

        if (lastName != null && lastName != "") {
            initials = lastName.substring(0, 1).toUpperCase();
        }
        if (firstName != null && firstName != "") {
            initials = initials + firstName.substring(0, 1).toUpperCase();
        }
        if (middleName != null && middleName != "") {
            initials = initials + middleName.substring(0, 1).toUpperCase();
        }

        return initials;
    }

    private String getParameterValue(HashMap<String, Serializable> parameters, String key) {
        return (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
    }

    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    public void initialize(HashMap<String, String> hashMap) {

    }
}
