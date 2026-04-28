package healthcare.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class UserPassword {

    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;
}
