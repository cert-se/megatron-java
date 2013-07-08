package se.sitic.megatron.report;

import se.sitic.megatron.core.MegatronException;


/**
 * Creates report files, e.g. XML files for Flash or JavaScript graphs.
 */
public interface IReportGenerator {


    public void init() throws MegatronException;


    public void createFiles() throws MegatronException;

}
