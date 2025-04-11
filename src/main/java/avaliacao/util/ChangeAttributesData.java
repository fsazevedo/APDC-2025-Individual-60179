package avaliacao.util;

public class ChangeAttributesData {
    public String targetUsername;
    public String name;
    public String email;
    public String phoneNumber;
    public String address;
    public String job;
    public String employer;
    public String employerNif;
    public String nif;
    public String profileType;
    public String role;
    public String accountState;
	public String newUsername;

    public ChangeAttributesData() {}

    public boolean hasInvalidControlFieldsForEnduser() {
        return role != null || accountState != null || name != null;
    }

    public boolean isEmpty() {
        return name == null && phoneNumber == null && address == null && job == null &&
               employer == null && employerNif == null && nif == null && profileType == null &&
               role == null && accountState == null;
    }
} 
