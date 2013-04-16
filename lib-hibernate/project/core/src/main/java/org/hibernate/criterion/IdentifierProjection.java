/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.criterion;

import org.hibernate.*;
import org.hibernate.type.*;
import org.hibernate.util.*;

/**
 * A property value, or grouped property value
 * @author Gavin King
 */
public class IdentifierProjection extends SimpleProjection {

	private boolean grouped;

	protected IdentifierProjection(boolean grouped) {
		this.grouped = grouped;
	}

	protected IdentifierProjection() {
		this(false);
	}

	@Override
	public String toString() {
		return "id";
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return new Type[] { criteriaQuery.getIdentifierType(criteria) };
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery)
	throws HibernateException {
		StringBuffer buf = new StringBuffer();
		String[] cols = criteriaQuery.getIdentifierColumns(criteria);
		for ( int i=0; i<cols.length; i++ ) {
			//-- jal 20110815 bugfix: compound ID properties do not have a ',' between their fields.
			if(i > 0)
				buf.append(',');
			//-- jal 20110815 end bugfix
			buf.append( cols[i] )
				.append(" as y")
				.append(position + i)
				.append('_');
		}
		return buf.toString();
	}

	@Override
	public boolean isGrouped() {
		return grouped;
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		if (!grouped) {
			return super.toGroupSqlString(criteria, criteriaQuery);
		}
		else {
			return StringHelper.join( ", ", criteriaQuery.getIdentifierColumns(criteria) );
		}
	}

}