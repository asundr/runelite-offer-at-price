package org.asundr;

import java.util.function.Function;

public enum RoundingRule
{
    NEAREST(val -> (long)Math.round(val)),
    DOWN(val -> (long)Math.floor(val)),
    UP(val -> (long)Math.ceil(val));
    final public Function<Float, Long> method;
    RoundingRule(final Function<Float, Long> method) { this.method = method; }
}
