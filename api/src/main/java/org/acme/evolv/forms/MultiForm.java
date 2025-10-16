package org.acme.evolv.forms;

import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class MultiForm {
    @RestForm("files")
    public List<FileUpload> files;

    @RestForm("question")
    public String question;

    @RestForm("companyid")
    public String companyid;

    @RestForm("id")
    public String id;

    @RestForm("model")
    public String model; 
}
