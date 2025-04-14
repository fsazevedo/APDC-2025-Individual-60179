package avaliacao.util;

import com.google.cloud.datastore.Entity;

public class RegisterExtraData {
    // Constantes de roles

    private static final String ENDUSER = "ENDUSER";
    private static final String BACKOFFICE = "BACKOFFICE";
    private static final String ADMIN = "ADMIN";
    private static final String PARTNER = "PARTNER";
    
    // Constantes de estados
    private static final String STATE_ATIVADA = "ATIVADA";
    private static final String STATE_DESATIVADA = "DESATIVADA";
    private static final String STATE_SUSPENSA = "SUSPENSA";

    
    public String citizenCardNumber;
    public String role;
    public String nif;
    public String employer;
    public String job;
    public String address;
    public String employerNif;
    public String accountState;
    
    public RegisterExtraData() {
    	
    }


    public RegisterExtraData(String citizenCardNumber, String role, String nif, String employer, String job, String address, String employerNif, String accountState) {
    	this.citizenCardNumber = citizenCardNumber;
    	this.role = role;
    	this.nif = nif;
    	this.employer = employer;
    	this.job =job;
    	this.address = address;
    	this.employerNif = employerNif;
    	this.accountState = accountState;
    }

    public boolean validOptionalFields() {
        return (nonEmptyOrBlankField(role) && isValidRole(role)) ||
               (nonEmptyOrBlankField(nif) && isValidNIF(nif)) ||
               (nonEmptyOrBlankField(employerNif) && isValidNIF(employerNif)) ||
               (nonEmptyOrBlankField(citizenCardNumber) && citizenCardNumber.matches("\\d{9}")) ||
               (nonEmptyOrBlankField(employer)) ||
               (nonEmptyOrBlankField(job)) ||
               (nonEmptyOrBlankField(address)) ||
               (nonEmptyOrBlankField(accountState));
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    private boolean isValidRole(String role) {
        return role.equalsIgnoreCase(ENDUSER) || role.equalsIgnoreCase(BACKOFFICE) || role.equalsIgnoreCase(ADMIN)
                || role.equalsIgnoreCase(PARTNER);
    }

    private boolean isValidNIF(String nif) {
        return nif.matches("\\d{9}");
    }

    public void addOptionalFieldsToBuilder(Entity.Builder userBuilder) {
        if (nonEmptyOrBlankField(citizenCardNumber)) {
            userBuilder.set("citizen_card_number", citizenCardNumber);
        }
        if (nonEmptyOrBlankField(role)) {
            userBuilder.set("user_role", role);
        } else {
            userBuilder.set("user_role", ENDUSER);
        }
        
        if (nonEmptyOrBlankField(nif)) {
            userBuilder.set("user_nif", nif);
        }
        if (nonEmptyOrBlankField(employer)) {
            userBuilder.set("user_employer", employer);
        }
        if (nonEmptyOrBlankField(job)) {
            userBuilder.set("user_job", job);
        }
        if (nonEmptyOrBlankField(address)) {
            userBuilder.set("user_address", address);
        }
        if (nonEmptyOrBlankField(employerNif)) {
            userBuilder.set("user_employer_nif", employerNif);
        }
        if (nonEmptyOrBlankField(accountState)) {
            userBuilder.set("user_account_state", accountState);
        }else {
            userBuilder.set("user_account_state", STATE_ATIVADA);
        }
    }
}
