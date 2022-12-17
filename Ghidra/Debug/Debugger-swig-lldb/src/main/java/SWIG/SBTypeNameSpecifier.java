/* ###
 * IP: Apache License 2.0 with LLVM Exceptions
 */
/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package SWIG;

public class SBTypeNameSpecifier {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected SBTypeNameSpecifier(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SBTypeNameSpecifier obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        lldbJNI.delete_SBTypeNameSpecifier(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public SBTypeNameSpecifier() {
    this(lldbJNI.new_SBTypeNameSpecifier__SWIG_0(), true);
  }

  public SBTypeNameSpecifier(String name, boolean is_regex) {
    this(lldbJNI.new_SBTypeNameSpecifier__SWIG_1(name, is_regex), true);
  }

  public SBTypeNameSpecifier(String name) {
    this(lldbJNI.new_SBTypeNameSpecifier__SWIG_2(name), true);
  }

  public SBTypeNameSpecifier(SBType type) {
    this(lldbJNI.new_SBTypeNameSpecifier__SWIG_3(SBType.getCPtr(type), type), true);
  }

  public SBTypeNameSpecifier(SBTypeNameSpecifier rhs) {
    this(lldbJNI.new_SBTypeNameSpecifier__SWIG_4(SBTypeNameSpecifier.getCPtr(rhs), rhs), true);
  }

  public boolean IsValid() {
    return lldbJNI.SBTypeNameSpecifier_IsValid(swigCPtr, this);
  }

  public boolean IsEqualTo(SBTypeNameSpecifier rhs) {
    return lldbJNI.SBTypeNameSpecifier_IsEqualTo(swigCPtr, this, SBTypeNameSpecifier.getCPtr(rhs), rhs);
  }

  public String GetName() {
    return lldbJNI.SBTypeNameSpecifier_GetName(swigCPtr, this);
  }

  public SBType GetType() {
    return new SBType(lldbJNI.SBTypeNameSpecifier_GetType(swigCPtr, this), true);
  }

  public boolean IsRegex() {
    return lldbJNI.SBTypeNameSpecifier_IsRegex(swigCPtr, this);
  }

  public boolean GetDescription(SBStream description, DescriptionLevel description_level) {
    return lldbJNI.SBTypeNameSpecifier_GetDescription(swigCPtr, this, SBStream.getCPtr(description), description, description_level.swigValue());
  }

  public String __str__() {
    return lldbJNI.SBTypeNameSpecifier___str__(swigCPtr, this);
  }

}
