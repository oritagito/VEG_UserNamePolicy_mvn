package com.oim.gis.veg;

import com.oim.gis.veg.utils.Utils;
import oracle.iam.identity.exception.UserNameGenerationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

@RunWith(Parameterized.class)
public class AppTest {

    private String firstName;
    private String lastName;
    private String middleName;
    private String login;

    public AppTest(String firstName, String lastName, String middleName, String login) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.login = login;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Петр", "Петров", "Петрович", "PPPETROV"},
                {"Евгений", "Васильев", "Геннадьевич", "EGVASILEV"},
                {"", "Иванов", "Петрович", "IVANOV"},
                {"Петр", "Василий", "Петочин", "PVASILY"},
                {"", "Нагаев", "", "NAGAEV2"},
                {"", "NAGAEV", "", "NAGAEV2"},
                {"", "Иванко", "", "IVANKO1"},
                {"", "Игнатий", "", "IGNATY"},
                {"Павло", "Палий", "", "PPALY1"},
                {"", "^&", "", "FAILED"},
                {"-", "F-", "", "FAILED"},
                {"Fs-", "Павлов", "", "PAVLOV"},
                {"Игнат-", "Игнатов", "И-", "IGNATOV1"},

        });
    }

    @Test
    public void testGeneratelogin() throws UserNameGenerationException {

        // Exists
        List<String> userExists = new LinkedList();
        userExists.add("PETROV");
        userExists.add("PPETROV");
        userExists.add("EVASILEV");
        userExists.add("IIVANOV");
        userExists.add("IVANKO");
        userExists.add("PETROV");
        userExists.add("NAGAEV");
        userExists.add("NAGAEV1");
        userExists.add("PPALY");
        userExists.add("IGNATOV");

        Utils testGenerateLogin = new Utils();
        assertEquals("Test #1", login, Utils.generateNameTest(firstName, lastName, middleName, userExists));
    }
}
