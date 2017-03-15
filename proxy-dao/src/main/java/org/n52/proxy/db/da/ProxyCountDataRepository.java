/*
 * Copyright (C) 2013-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.proxy.db.da;

import org.hibernate.Session;
import org.n52.io.response.dataset.count.CountData;
import org.n52.io.response.dataset.count.CountValue;
import org.n52.proxy.connector.AbstractSosConnector;
import org.n52.proxy.db.beans.ProxyServiceEntity;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.CountDatasetEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.dao.DbQuery;

import java.util.Map;

public class ProxyCountDataRepository extends org.n52.series.db.da.CountDataRepository
        implements ProxyDataRepository<CountDatasetEntity, CountValue> {

    private Map<String, AbstractSosConnector> connectorMap;

    @Override
    public void setConnectorMap(Map<String, AbstractSosConnector> connectorMap) {
        this.connectorMap = connectorMap;
    }

    @Override
    public CountValue getFirstValue(CountDatasetEntity entity, Session session, DbQuery query) {
        DataEntity firstObs = this.getConnector(entity).getFirstObservation(entity).orElse(null);
        return createSeriesValueFor((CountDataEntity) firstObs, entity, query);
    }

    @Override
    public CountValue getLastValue(CountDatasetEntity entity, Session session, DbQuery query) {
        DataEntity lastObs = this.getConnector(entity).getLastObservation(entity).orElse(null);
        return createSeriesValueFor((CountDataEntity) lastObs, entity, query);
    }

    @Override
    protected CountData assembleData(CountDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        CountData result = new CountData();
        this.getConnector(seriesEntity)
                .getObservations(seriesEntity, query).stream()
                .map((entry) -> createSeriesValueFor((CountDataEntity) entry, seriesEntity, query))
                .forEach(entry -> result.addNewValue(entry));
        return result;
    }

    private AbstractSosConnector getConnector(CountDatasetEntity entity) {
        String connectorName = ((ProxyServiceEntity) entity.getService()).getConnector();
        return this.connectorMap.get(connectorName);
    }

}
