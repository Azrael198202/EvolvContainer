package org.acme.evolv.forms;

import org.jboss.resteasy.reactive.RestForm;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public class PdfForm {
    @RestForm("file")
    public FileUpload file;

    @RestForm("id")
    public String id;

    @RestForm("companyid")
    public String companyid;
}
