package to.etc.el.node;

import java.math.*;

import javax.servlet.jsp.el.*;

public class NdMultiply extends NdBinOp {
	/**
	 * @param a
	 * @param b
	 */
	public NdMultiply(NdBase a, NdBase b) {
		super(a, b);
	}

	@Override
	protected Object apply(BigDecimal a, BigDecimal b) throws ELException {
		return a.multiply(b);
	}

	@Override
	protected Object apply(BigInteger a, BigInteger b) throws ELException {
		return a.multiply(b);
	}

	@Override
	protected double apply(double a, double b) throws ELException {
		return a * b;
	}

	@Override
	protected long apply(long a, long b) throws ELException {
		return a * b;
	}

	@Override
	protected String getOperator() {
		return "*";
	}

}
