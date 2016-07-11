package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * UserUpdate contains the options the user can change on their own,
 * not counting their password which is handled specially.  An entire
 * filled out object must be pushed to server.
 */
public class UserUpdate {

    private String firstName;

    private String lastName;

    @NotEmpty
    private String email;

    public String getFirstName() {
        return firstName;
    }

    public UserUpdate setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserUpdate setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserUpdate setEmail(String email) {
        this.email = email;
        return this;
    }
}
