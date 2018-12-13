package org.myspring.aop;

public interface IntroductionAdvisor extends Advisor, IntroductionInfo  {

    ClassFilter getClassFilter();

    void validateInterfaces() throws IllegalArgumentException;

}
