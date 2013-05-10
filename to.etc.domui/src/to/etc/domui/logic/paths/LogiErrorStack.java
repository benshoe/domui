package to.etc.domui.logic.paths;

import java.util.*;

import javax.annotation.*;

/**
 * This collects errors as encountered in business logic, with the ability to
 * report those errors on the proper location.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Feb 5, 2013
 */
public class LogiErrorStack {
	@Nonnull
	private List<IPropertyPathElement< ? >> m_currentPath = new ArrayList<IPropertyPathElement< ? >>();

	public <T> void push(@Nonnull String propertyPath, @Nonnull T instance) {
		m_currentPath.add(new PropertyPathProperty<T>(instance, propertyPath));
	}

	public <T> void push(int index, @Nonnull T instance) {
		m_currentPath.add(new PropertyPathIndex<T>(index, instance));
	}

	public void pop() {
		if(m_currentPath.size() == 0)
			throw new IllegalStateException("Error path stack underflow: too many popped() items");
		m_currentPath.remove(m_currentPath.size() - 1);
	}

	@Nonnull
	public PropertyPath getPath() {
		return new PropertyPath(m_currentPath);
	}
}
