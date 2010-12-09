// (c) Copyright 2010 Odiago, Inc.

package com.odiago.rtengine.lang;

import java.util.ArrayList;
import java.util.List;

import com.odiago.rtengine.util.StringUtils;

/**
 * A representation of an abstract type which can be unified to a specific
 * concrete type on a per-expression basis.
 *
 * <p>This is used by functions to specify arguments and return values of
 * variable type. These are only valid as argument and return types of
 * FnCallExprs.</p>
 *
 * <p>As an expression is typechecked, UniversalType instances will be
 * compared against the actual types of arguments to the function; constraints
 * generated by these comparisons will result in the UniversalType being
 * replaced in the official expression by a concrete type.</p>
 *
 * <p>UniversalType instances may carry constraints that restrict the
 * set of concrete types to which they may be unified. Each constraint
 * is a <i>promotesTo</i> relationship. e.g., adding a constraint of
 * <tt>Type.TypeName.TYPECLASS_NUMERIC</tt> asserts that the final type
 * this takes on must promote to TYPECLASS_NUMERIC.</p>
 *
 * <p>Note that the equals() method operates on the alias and the constraints;
 * if two instances of a UniversalType have the same alias and the same
 * constraints (or no constraints at all), they will be judged "equal;" if a
 * function has two unbound argument types that are unrelated, they should
 * be instantiated with different aliases, such as "'a" and "'b".
 * </p>
 */
public class UniversalType extends Type {
  /**
   * The set of type(classes) which constrain the set of values this type can
   * take on.
   */
  private List<Type> mConstraints;

  /**
   * A human-readable alias to distinguish this from other type variables in an expression.
   */
  private String mAlias;

  public UniversalType(String alias) {
    super(TypeName.UNIVERSAL);
    mAlias = alias;
    mConstraints = new ArrayList<Type>();
  }

  /**
   * Adds a type to the list of constraints for this type variable.
   */
  public void addConstraint(Type t) {
    mConstraints.add(t);
  }

  @Override
  public TypeName getPrimitiveTypeName() {
    return null;
  }

  public List<Type> getConstraints() {
    return mConstraints;
  }

  /**
   * We are a primitive type iff one of our constraints forces us to be.
   */
  @Override
  public boolean isPrimitive() {
    for (Type t : mConstraints) {
      if (t.isPrimitive()) {
        return true;
      }
    }

    return false;
  }

  /**
   * We are a numeric type iff one of our constraints forces us to be.
   */
  @Override
  public boolean isNumeric() {
    for (Type t : mConstraints) {
      if (t.isNumeric()) {
        return true;
      }
    }

    return false;
  }

  /**
   * We are a nullable type iff one of our constraints forces us to be.
   */
  @Override
  public boolean isNullable() {
    for (Type t : mConstraints) {
      if (t.isNullable()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("var(");
    sb.append(mAlias);
    if (mConstraints.size() > 0) {
      sb.append(", constraints={");
      StringUtils.formatList(sb, mConstraints);
      sb.append("}");
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * <p>Given a set of constraints 'actualConstraints' imposed by a specific
   * expression context for this type variable, determine the narrowest
   * type that satisfies all of mConstraints and actualConstraints.</p>
   * <p>All constraints in actualConstraints should be real types (e.g., INT)
   * and not abstract typeclasses, etc.</p>
   *
   * @return the narrowest type to satisfy all such constraints.
   * @throws TypeCheckException if no such type can be found.
   */
  public Type getRuntimeType(List<Type> actualConstraints) throws TypeCheckException {
    if (actualConstraints.size() == 0) {
      // We need a real type to start with.
      throw new TypeCheckException("Cannot make a concrete type from a "
          + "type variable without a binding constraint");
    }

    // Get the narrowest type that satisfies all the actualConstraints.
    Type candidate = actualConstraints.get(0);
    for (int i = 1; i < actualConstraints.size(); i++) {
      Type constraint = actualConstraints.get(i);
      boolean candidatePromotes = candidate.promotesTo(constraint);
      boolean constraintPromotes = constraint.promotesTo(candidate);
      if (candidatePromotes && !constraintPromotes) {
        // candidate satisfies constraint, but not the other way around.
        // Widen our type to the next constraint type.
        candidate = constraint;
      } else if (!candidatePromotes && !constraintPromotes) {
        // We can't go in either direction.
        throw new TypeCheckException("Actual constraints " + candidate + " and " + constraint
            + " are incompatible.");
      }
      // In both other cases, we stay with the candidate constraint as-is.
    }

    // Now that we've found the narrowest real type we can use,
    // make sure it handles all our built-in constraints. These may be real
    // or abstract types.
    for (Type constraint : mConstraints) {
      if (!candidate.promotesTo(constraint)) {
        throw new TypeCheckException("Candidate type " + candidate
            + " cannot satisfy constraint: " + constraint);
      }
    }

    // The candidate type passed all our tests.
    return candidate;
  }

  @Override
  public int hashCode() {
    int hash = mAlias.hashCode();
    for (Type constraint : mConstraints) {
      hash ^= constraint.hashCode();
    }

    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else if (!other.getClass().equals(getClass())) {
      return false;
    }

    UniversalType otherType = (UniversalType) other;
    if (!mAlias.equals(otherType.mAlias)) {
      return false;
    }

    if (mConstraints.size() != otherType.mConstraints.size()) {
      return false;
    }

    for (int i = 0; i < mConstraints.size(); i++) {
      if (!mConstraints.get(i).equals(otherType.mConstraints.get(i))) {
        return false;
      }
    }

    return true;
  }
}
