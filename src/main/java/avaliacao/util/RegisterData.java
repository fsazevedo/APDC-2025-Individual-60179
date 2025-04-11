package avaliacao.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class RegisterData {
	
    private static final String PRIVATE = "privado";
    private static final String PUBLIC = "publico";
    
    public String username;
    public String password;
    public String confirmation;
    public String email;
    public String name;    
    public String phoneNumber;
    public String profileType;
    public RegisterExtraData extraData;
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String password, String confirmation, String email, String name, String phoneNumber, String profileType, RegisterExtraData extraData) {
        this.username = username;
        this.password = password;
        this.confirmation = confirmation;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.profileType = profileType;
        this.extraData = extraData;
	}
	
    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }
    
    public boolean validRegistration() {
        return nonEmptyOrBlankField(username) &&
               nonEmptyOrBlankField(password) &&
               nonEmptyOrBlankField(email) &&
               nonEmptyOrBlankField(name) &&        // Check non empty
               nonEmptyOrBlankField(phoneNumber) && // Check for phone number
               nonEmptyOrBlankField(profileType) && // Check for profile type
               validEmailFormat(email) &&           // Email format check
               validPasswordFormat(password) &&     // Password format check
               validPhoneNumber(phoneNumber) &&     // Phone number check
               validProfileType(profileType) &&     // Profile type check
               password.equals(confirmation) &&     // Password confirmation check
               (extraData == null || extraData.validOptionalFields()); // Validate optional fields if provided
    }

    private boolean validProfileType(String profileType) {
        return PUBLIC.equalsIgnoreCase(profileType) || PRIVATE.equalsIgnoreCase(profileType);
    }

    private boolean validPhoneNumber(String phoneNumber) {
        String phoneRegex = "^\\+\\d{9,15}$";
        Pattern pattern = Pattern.compile(phoneRegex);
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    private boolean validPasswordFormat(String password) {
        // Uppercase, lowercase, numbers, special characters, 8-16 characters
        String passwordRegex = "(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()-+]).{8,16}";
        Pattern pattern = Pattern.compile(passwordRegex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private boolean validEmailFormat(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }
}
