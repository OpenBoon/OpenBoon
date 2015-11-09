package com.zorroa.archivist.sdk.processor.export;

/**
 * Created by chambers on 11/9/15.
 */
public class TestExportProcessor extends ExportProcessor {

    public Port<String> stringInput;
    public Port<Integer> intInput;
    public Port<Float> floatInput;

    public Port<String> stringOutput;
    public Port<Integer> intOutput;
    public Port<Float> floatOutput;

    public TestExportProcessor() {
        stringInput = new Port<>("stringInput", Port.Type.Input, this);
        intInput = new Port<>("intInput", Port.Type.Input, this);
        floatInput = new Port<>("floatInput", Port.Type.Input, this);

        stringOutput = new Port<>("stringOutput", Port.Type.Output, this);
        intOutput = new Port<>("intOutput", Port.Type.Output, this);
        floatOutput = new Port<>("floatOutput", Port.Type.Output, this);
    }

    @Override
    protected void process() throws Exception {

    }
}
