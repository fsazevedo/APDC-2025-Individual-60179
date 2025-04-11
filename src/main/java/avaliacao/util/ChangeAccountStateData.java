package avaliacao.util;

public class ChangeAccountStateData {
    public String username;
    public String newState;

    public ChangeAccountStateData() {}

    public ChangeAccountStateData(String username, String newState) {
        this.username = username;
        this.newState = newState;
    }
}
