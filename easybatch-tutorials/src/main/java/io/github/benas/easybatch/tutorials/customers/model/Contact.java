package io.github.benas.easybatch.tutorials.customers.model;

/**
 * JavaBean representing a customer's contact.
 *
 * @author benas (md.benhassine@gmail.com)
 */
public class Contact {

    private String email;

    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
