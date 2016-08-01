package com.oim.gis.veg.handler.pre;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcUserOperationsIntf;
import Thor.API.tcResultSet;
import com.oim.gis.veg.utils.Utils;
import oracle.iam.platform.Platform;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateAttributes_preprocess implements PreProcessHandler {

    private static final Logger log = Logger.getLogger("PLUGINS");
    private final String className = getClass().getName();

    private final String usrLocale = "ru-RU";

    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(className, "---*--- execute ---*--- ");
        log.log(Level.INFO, "---* Entering  EventResult of UpdateAttributes_preprocess");
        log.log(Level.INFO, "---* orchestration.getOperation() -> [" + orchestration.getOperation() + "]");

        String userKey = orchestration.getTarget().getEntityId();
        log.log(Level.FINEST, "---* userKey -> [" + userKey + "]");

        HashMap<String, Serializable> orchestrationParameters = orchestration.getParameters();
        log.log(Level.FINEST, "---* orchestrationParameters -> [" + orchestrationParameters + "]");

        Set<String> parametersSet = orchestrationParameters.keySet();
        log.log(Level.FINEST, "---* parametersSet -> [" + parametersSet + "]");

        String firstName = null;
        String lastName = null;
        String middleName = null;

        if (orchestration.getOperation().equalsIgnoreCase("MODIFY")) {
            log.log(Level.INFO, "---* Operation is MODIFY");

            boolean isUpdateAttributes = false;
            boolean isUpdateLastName = false;

            for (String key : parametersSet) {
                Serializable serializable = orchestrationParameters.get(key);

                if (key.equalsIgnoreCase("First Name")) {
                    isUpdateAttributes = true;
                    if (serializable == null) firstName = "";
                    else {
                        firstName = serializable.toString();
                        log.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
                    }
                }

                if (key.equalsIgnoreCase("Last Name")) {
                    isUpdateAttributes = true;
                    isUpdateLastName = true;
                    lastName = serializable.toString();
                    log.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
                }

                if (key.equalsIgnoreCase("Middle Name")) {
                    isUpdateAttributes = true;
                    if (serializable == null) middleName = "";
                    else {
                        middleName = serializable.toString();
                        log.log(Level.FINEST, "---* middleName -> [" + middleName + "]");
                    }
                }
            }

            if (isUpdateAttributes) {
                String response = updateDisplayNameAndInitials(userKey, firstName, lastName, middleName);
                log.log(Level.INFO, "---* response updateDisplayNameAndInitials-> [" + response + "]");
            }

            if (isUpdateLastName) {
                String response = updateLogin(userKey, firstName, lastName, middleName, false);
                log.log(Level.INFO, "---* response updateLogin-> [" + response + "]");
            }
        } else if (orchestration.getOperation().equalsIgnoreCase("CREATE")) {
            log.log(Level.INFO, "---* Operation is CREATE");

            firstName = getParameterValue(orchestrationParameters, "First Name") == null ? "" : getParameterValue(orchestrationParameters, "First Name");
            lastName = getParameterValue(orchestrationParameters, "Last Name") == null ? "" : getParameterValue(orchestrationParameters, "Last Name");
            middleName = getParameterValue(orchestrationParameters, "Middle Name") == null ? "" : getParameterValue(orchestrationParameters, "Middle Name");

            log.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
            log.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
            log.log(Level.FINEST, "---* middleName -> [" + middleName + "]");

            String displayName = null;
            String initials = null;
            if ((lastName != null) && (lastName != "")) {
                displayName = lastName;
                initials = lastName.substring(0, 1).toUpperCase();
            }
            if ((firstName != null) && (firstName != "")) {
                displayName = displayName + " " + firstName;
                initials = initials + firstName.substring(0, 1).toUpperCase();
            }
            if ((middleName != null) && (middleName != "")) {
                displayName = displayName + " " + middleName;
                initials = initials + middleName.substring(0, 1).toUpperCase();
            }
            log.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
            log.log(Level.FINEST, "---* initials -> [" + initials + "]");

            HashMap updateUser = new HashMap();
            updateUser.put("base", displayName);

            orchestrationParameters.put("Display Name", updateUser);
            orchestrationParameters.put("Initials", initials);
            orchestrationParameters.put("usr_locale", usrLocale);
        } else if (orchestration.getOperation().equalsIgnoreCase("DELETE")) {
            log.log(Level.INFO, "---* Operation is DELETE");

            String response = updateLogin(userKey, firstName, lastName, middleName, true);
            log.log(Level.INFO, "---* response updateLogin-> [" + response + "]");
        }

        log.log(Level.INFO, "---* Exiting  EventResult of UpdateAttributes_preprocess");
        log.exiting(className, "---*--- execute ---*---");
        return new EventResult();
    }

    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        log.entering(className, "---*--- execute ---*---");
        log.log(Level.INFO, "---* Entering BulkEventResult of UpdateAttributes_preprocess");
        log.log(Level.INFO, "---* bulkOrchestration.getOperation() -> [" + bulkOrchestration.getOperation() + "]");

        String userKey = bulkOrchestration.getTarget().getEntityId();
        log.log(Level.FINEST, "---* userKey -> [" + userKey + "]");

        HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
        log.log(Level.FINEST, "---* bulkParameters -> [" + bulkParameters + "]");

        String firstName = null;
        String lastName = null;
        String middleName = null;

        if (bulkOrchestration.getOperation().equalsIgnoreCase("MODIFY")) {
            log.log(Level.INFO, "---* Operation is MODIFY");

            for (HashMap<String, Serializable> bulkParameter : bulkParameters) {
                Set<String> bulkKeySet = bulkParameter.keySet();

                boolean isUpdateAttributes = false;
                boolean isUpdateLastName = false;

                for (String key : bulkKeySet) {
                    Serializable serializable = bulkParameter.get(key);

                    if (key.equalsIgnoreCase("First Name")) {
                        isUpdateAttributes = true;
                        if (serializable == null) firstName = "";
                        else {
                            firstName = serializable.toString();
                            log.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
                        }
                    }

                    if (key.equalsIgnoreCase("Last Name")) {
                        isUpdateAttributes = true;
                        isUpdateLastName = true;
                        lastName = serializable.toString();
                        log.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
                    }

                    if (key.equalsIgnoreCase("Middle Name")) {
                        isUpdateAttributes = true;
                        if (serializable == null) middleName = "";
                        else {
                            middleName = serializable.toString();
                            log.log(Level.FINEST, "---* middleName -> [" + middleName + "]");
                        }
                    }
                }
                if (isUpdateAttributes) {
                    String response = updateDisplayNameAndInitials(userKey, firstName, lastName, middleName);
                    log.log(Level.FINEST, "---* response updateDisplayNameAndInitials -> [" + response + "]");
                }

                if (isUpdateLastName) {
                    String response = updateLogin(userKey, firstName, lastName, middleName, false);
                    log.log(Level.FINEST, "---* response updateLogin -> [" + response + "]");
                }
            }
        } else if (bulkOrchestration.getOperation().equalsIgnoreCase("CREATE")) {
            log.log(Level.INFO, "---* Operation is CREATE");
            for (HashMap<String, Serializable> bulkParameter : bulkParameters) {
                firstName = getParameterValue(bulkParameter, "First Name") == null ? "" : getParameterValue(bulkParameter, "First Name");
                lastName = getParameterValue(bulkParameter, "Last Name") == null ? "" : getParameterValue(bulkParameter, "Last Name");
                middleName = getParameterValue(bulkParameter, "Middle Name") == null ? "" : getParameterValue(bulkParameter, "Middle Name");

                log.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
                log.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
                log.log(Level.FINEST, "---* middleName -> [" + middleName + "]");

                String displayName = null;
                String initials = null;
                if ((lastName != null) && (lastName != "")) {
                    displayName = lastName;
                    initials = lastName.substring(0, 1).toUpperCase();
                }
                if ((firstName != null) && (firstName != "")) {
                    displayName = displayName + " " + firstName;
                    initials = initials + firstName.substring(0, 1).toUpperCase();
                }
                if ((middleName != null) && (middleName != "")) {
                    displayName = displayName + " " + middleName;
                    initials = initials + middleName.substring(0, 1).toUpperCase();
                }
                log.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                log.log(Level.FINEST, "---* initials -> [" + initials + "]");

                HashMap updateUser = new HashMap();
                updateUser.put("base", displayName);

                bulkParameter.put("Display Name", updateUser);
                bulkParameter.put("Initials", initials);
                bulkParameter.put("usr_locale", usrLocale);
            }
        } else if (bulkOrchestration.getOperation().equalsIgnoreCase("DELETE")) {
            log.log(Level.INFO, "---* Operation is DELETE");

            String response = updateLogin(userKey, firstName, lastName, middleName, true);
            log.log(Level.INFO, "---* response updateLogin-> [" + response + "]");
        }

        log.log(Level.INFO, "---* Exiting BulkEventResult of UpdateAttributes_preprocess");
        log.exiting(className, "---*--- execute ---*---");
        return new BulkEventResult();
    }

    private String updateDisplayNameAndInitials(String userKey, String firstName, String lastName, String middleName) {

        String response = null;

        Map<String, String> phAttributeList = new HashMap<String, String>();
        phAttributeList.put("Users.Key", userKey);
        tcResultSet resultSet;

        try {
            tcUserOperationsIntf tcUserOperationsIntf = (Platform.getService(tcUserOperationsIntf.class));
            resultSet = tcUserOperationsIntf.findUsers(phAttributeList);

            int count = 0;
            count = resultSet.getRowCount();
            log.log(Level.FINEST, "---* count -> [" + count + "]");

            String displayName = null;
            String initials = null;
            String usrDisplayName = resultSet.getStringValue("Users.Display Name");

            log.log(Level.FINEST, "---* usrDisplayName -> [" + usrDisplayName + "]");

            if (!usrDisplayName.toLowerCase().contains("уволен")) {
                for (int i = 0; i < count; i++) {
                    resultSet.goToRow(i);

                    if (firstName == null) firstName = resultSet.getStringValue("Users.First Name");
                    if (lastName == null) lastName = resultSet.getStringValue("Users.Last Name");
                    if (middleName == null) middleName = resultSet.getStringValue("Users.Middle Name");

                    if (lastName != null && lastName != "") {
                        displayName = lastName;
                        initials = lastName.substring(0, 1).toUpperCase();
                    }
                    if (firstName != null && firstName != "") {
                        displayName = displayName + " " + firstName;
                        initials = initials + firstName.substring(0, 1).toUpperCase();
                    }
                    if (middleName != null && middleName != "") {
                        displayName = displayName + " " + middleName;
                        initials = initials + middleName.substring(0, 1).toUpperCase();
                    }

                    log.log(Level.FINEST, "---* displayName -> [" + displayName + "]");
                    log.log(Level.FINEST, "---* initials -> [" + initials + "]");

                    Map<String, String> updateAttributes = new HashMap<String, String>();
                    updateAttributes.put("Users.Display Name", displayName);
                    updateAttributes.put("Initials", initials);
                    tcUserOperationsIntf.updateUser(resultSet, updateAttributes);
                }
            } else log.log(Level.FINEST, "---* User is Fired");
            tcUserOperationsIntf.close();
            response = "Success updateDisplayNameAndInitials";
        } catch (tcAPIException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private String updateLogin(String userKey, String firstName, String lastName, String middleName, boolean isDeleted) {
        String response = null;

        Map<String, String> phAttributeList = new HashMap<String, String>();
        phAttributeList.put("Users.Key", userKey);
        tcResultSet resultSet;

        try {
            tcUserOperationsIntf tcUserOperationsIntf = (Platform.getService(tcUserOperationsIntf.class));
            resultSet = tcUserOperationsIntf.findUsers(phAttributeList);

            int count = 0;
            count = resultSet.getRowCount();
            log.log(Level.FINEST, "---* count -> [" + count + "]");

            String currentLogin = resultSet.getStringValue("Users.User ID");

            log.log(Level.FINEST, "---* currentLogin -> [" + currentLogin + "]");
            if (!isDeleted && !currentLogin.toLowerCase().contains("fired")) {
                for (int i = 0; i < count; i++) {
                    resultSet.goToRow(i);

                    if (firstName == null) firstName = resultSet.getStringValue("Users.First Name");
                    if (lastName == null) lastName = resultSet.getStringValue("Users.Last Name");
                    if (middleName == null) middleName = resultSet.getStringValue("Users.Middle Name");

                    String login = Utils.generateName(firstName, lastName, middleName);

                    log.log(Level.FINEST, "---* login -> [" + login + "]");

                    Map<String, String> updateLogin = new HashMap<String, String>();
                    updateLogin.put("User Login", login);
                    tcUserOperationsIntf.updateUser(resultSet, updateLogin);
                }
            } else if (isDeleted) {
                for (int i = 0; i < count; i++) {
                    resultSet.goToRow(i);

                    String newLogin = null;

                    if (currentLogin.toLowerCase().contains("fired")) {
                        if (currentLogin != null && currentLogin != "") {
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                            newLogin = "DEL_" + currentLogin.substring(currentLogin.indexOf('_') + 1, currentLogin.lastIndexOf('_')) + "_" + simpleDateFormat.format(calendar.getTime());
                        }
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        newLogin = "DEL_" + currentLogin + "_" + simpleDateFormat.format(calendar.getTime());
                    }

                    log.log(Level.FINEST, "---* newLogin -> [" + newLogin + "]");

                    Map<String, String> updateLogin = new HashMap<String, String>();
                    updateLogin.put("User Login", newLogin);
                    tcUserOperationsIntf.updateUser(resultSet, updateLogin);
                }
            }
            tcUserOperationsIntf.close();
            response = "Success updateLogin";
        } catch (tcAPIException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private String getParameterValue(HashMap<String, Serializable> parameters, String key) {
        String value = (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
        return value;
    }

    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    public void initialize(HashMap<String, String> hashMap) {

    }
}
