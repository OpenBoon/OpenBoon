package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * UserUpdate contains the options the user can change on their own,
 * not counting their password which is handled specially.  An entire
 * filled out object must be pushed to server.
 */
public class UserProfileUpdate {

    private String firstName;

    private String lastName;

    @NotEmpty
    @Email
    private String email;

    public String getFirstName() {
        return firstName;
    }

    public UserProfileUpdate setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserProfileUpdate setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserProfileUpdate setEmail(String email) {
        this.email = email;
        return this;
    }
}
