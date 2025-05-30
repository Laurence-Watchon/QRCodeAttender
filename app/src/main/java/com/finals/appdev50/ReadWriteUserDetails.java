package com.finals.appdev50;

public class ReadWriteUserDetails {
    public String firstname, middlename, lastname, dateofbirth, gender, mobilenumber, address, role, email;

    public ReadWriteUserDetails(String fname, String mname, String lname, String dateofbirthtext, String gendertext, String mobilenumberText, String addresstext, String roletext, String emailtext){
        this.firstname = fname;
        this.middlename = mname;
        this.lastname = lname;
        this.dateofbirth = dateofbirthtext;
        this.gender = gendertext;
        this.mobilenumber = mobilenumberText;
        this.address = addresstext;
        this.role = roletext;
        this.email = emailtext;
    }
}
